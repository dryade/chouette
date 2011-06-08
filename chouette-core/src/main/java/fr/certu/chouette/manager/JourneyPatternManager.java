package fr.certu.chouette.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import fr.certu.chouette.common.ChouetteException;
import fr.certu.chouette.filter.DetailLevelEnum;
import fr.certu.chouette.filter.Filter;
import fr.certu.chouette.model.neptune.JourneyPattern;
import fr.certu.chouette.model.neptune.Line;
import fr.certu.chouette.model.neptune.Route;
import fr.certu.chouette.model.neptune.StopPoint;
import fr.certu.chouette.model.neptune.VehicleJourney;
import fr.certu.chouette.model.neptune.VehicleJourneyAtStop;
import fr.certu.chouette.model.user.User;
import fr.certu.chouette.plugin.report.Report;
import fr.certu.chouette.plugin.validation.ValidationParameters;
import fr.certu.chouette.plugin.validation.ValidationReport;

@SuppressWarnings("unchecked")
public class JourneyPatternManager extends AbstractNeptuneManager<JourneyPattern> 
{
	public JourneyPatternManager() 
	{
		super(JourneyPattern.class);
	}

	@Override
	protected Report propagateValidation(User user, List<JourneyPattern> beans,
			ValidationParameters parameters,boolean propagate) 
	throws ChouetteException 
	{
		Report globalReport = new ValidationReport();

		// aggregate dependent objects for validation
		Set<StopPoint> stopPoints = new HashSet<StopPoint>();
		List<VehicleJourney> vehicleJourneys = new ArrayList<VehicleJourney>();
		for (JourneyPattern bean : beans) 
		{
			if (bean.getStopPoints() != null)
				stopPoints.addAll(bean.getStopPoints());
			if (bean.getVehicleJourneys() != null)
				vehicleJourneys.addAll(bean.getVehicleJourneys());
		}

		// propagate validation on StopPoints
		if (stopPoints.size() > 0)
		{
			Report report = null;
			AbstractNeptuneManager<StopPoint> manager = (AbstractNeptuneManager<StopPoint>) getManager(StopPoint.class);
			if (manager.canValidate())
			{
				report = manager.validate(user, Arrays.asList(stopPoints.toArray(new StopPoint[0])), parameters,propagate);
			}
			else
			{
				report = manager.propagateValidation(user, Arrays.asList(stopPoints.toArray(new StopPoint[0])), parameters,propagate);
			}
			if (report != null)
			{
				globalReport.addAll(report.getItems());
				globalReport.updateStatus(report.getStatus());
			}
		}

		// propagate validation on journey patterns
		if (vehicleJourneys.size() > 0)
		{
			Report report = null;
			AbstractNeptuneManager<VehicleJourney> manager = (AbstractNeptuneManager<VehicleJourney>) getManager(VehicleJourney.class);
			if (manager.canValidate())
			{
				report = manager.validate(user, vehicleJourneys, parameters,propagate);
			}
			else
			{
				report = manager.propagateValidation(user, vehicleJourneys, parameters,propagate);
			}
			if (report != null)
			{
				globalReport.addAll(report.getItems());
				globalReport.updateStatus(report.getStatus());
			}
		}		

		return globalReport;
	}
	@Override
	public void remove(User user,JourneyPattern journeyPattern,boolean propagate) throws ChouetteException{
		INeptuneManager<VehicleJourney> vjManager = (INeptuneManager<VehicleJourney>) getManager(VehicleJourney.class);
		Filter filter = Filter.getNewEqualsFilter("journeyPattern.id", journeyPattern.getId());
		DetailLevelEnum level = DetailLevelEnum.ATTRIBUTE;
		List<VehicleJourney> vehicleJourneys = vjManager.getAll(user, filter, level);
		if(vehicleJourneys != null && !vehicleJourneys.isEmpty())
			vjManager.removeAll(user, vehicleJourneys,propagate);
		super.remove(user, journeyPattern,propagate);
	}

	@Override
	protected Logger getLogger() {
		return null;
	}

	@Override
	public void completeObject(User user, JourneyPattern journeyPattern) throws ChouetteException {
		Route route = journeyPattern.getRoute();
		if(route != null){
			Line line = route.getLine();
			if(line != null)
				journeyPattern.setLineIdShortcut(line.getObjectId());
		}
		List<StopPoint> stopPoints = journeyPattern.getStopPoints();
		List<VehicleJourney> vjs = journeyPattern.getVehicleJourneys();
		if (vjs != null && !vjs.isEmpty())
		{
			// complete StopPoints
			if (stopPoints == null || stopPoints.isEmpty())
			{
				VehicleJourney vj = vjs.get(0);
				for (VehicleJourneyAtStop vjas : vj.getVehicleJourneyAtStops()) 
				{
					journeyPattern.addStopPoint(vjas.getStopPoint());
				}
			}
			// complete VJ
			INeptuneManager<VehicleJourney> vjManager = (INeptuneManager<VehicleJourney>) getManager(VehicleJourney.class);
			for (VehicleJourney vehicleJourney : vjs) 
			{
				vjManager.completeObject(user, vehicleJourney);
			}
		}

	}

	@Override
	public void saveAll(User user, List<JourneyPattern> journeyPatterns, boolean propagate) throws ChouetteException 
	{
		super.saveAll(user, journeyPatterns,propagate);

		if(propagate)
		{
			INeptuneManager<VehicleJourney> vjManager = (INeptuneManager<VehicleJourney>) getManager(VehicleJourney.class);
			List<VehicleJourney> vehicleJourneys = new ArrayList<VehicleJourney>();
			for (JourneyPattern journeyPattern : journeyPatterns) 
			{
				if(journeyPattern.getVehicleJourneys() != null && !vehicleJourneys.containsAll(journeyPattern.getVehicleJourneys()))	
					vehicleJourneys.addAll(journeyPattern.getVehicleJourneys());	
			}

			if(!vehicleJourneys.isEmpty())
				vjManager.saveAll(user, vehicleJourneys, propagate);

		}
	}
}
