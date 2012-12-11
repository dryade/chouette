/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */
package fr.certu.chouette.exchange.gtfs.importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

import org.apache.log4j.Logger;

import fr.certu.chouette.model.neptune.AccessLink;
import fr.certu.chouette.model.neptune.AccessPoint;
import fr.certu.chouette.model.neptune.AreaCentroid;
import fr.certu.chouette.model.neptune.Company;
import fr.certu.chouette.model.neptune.ConnectionLink;
import fr.certu.chouette.model.neptune.Facility;
import fr.certu.chouette.model.neptune.GroupOfLine;
import fr.certu.chouette.model.neptune.JourneyPattern;
import fr.certu.chouette.model.neptune.Line;
import fr.certu.chouette.model.neptune.NeptuneIdentifiedObject;
import fr.certu.chouette.model.neptune.PTLink;
import fr.certu.chouette.model.neptune.PTNetwork;
import fr.certu.chouette.model.neptune.Route;
import fr.certu.chouette.model.neptune.StopArea;
import fr.certu.chouette.model.neptune.StopPoint;
import fr.certu.chouette.model.neptune.TimeSlot;
import fr.certu.chouette.model.neptune.Timetable;
import fr.certu.chouette.model.neptune.VehicleJourney;
import fr.certu.chouette.model.neptune.VehicleJourneyAtStop;

/**
 * @author michel
 *
 */
public class ModelAssembler 
{
	private static Logger logger = Logger.getLogger(ModelAssembler.class);

	@Getter @Setter private List<Line> lines;
	@Getter @Setter private List<Route> routes;
	@Getter @Setter private List<Company> companies;
	@Getter @Setter private PTNetwork ptNetwork;
	@Getter @Setter private List<JourneyPattern> journeyPatterns;
	@Getter @Setter private List<VehicleJourney> vehicleJourneys;
	@Getter @Setter private List<StopPoint> stopPoints;
	@Getter @Setter private List<StopArea> stopAreas;
	@Getter @Setter private List<Timetable> timetables;
	@Getter @Setter private List<ConnectionLink> connectionLinks;

	private Map<Class<? extends NeptuneIdentifiedObject>, Map<String,? extends NeptuneIdentifiedObject>> populatedDictionaries = new HashMap<Class<? extends NeptuneIdentifiedObject>, Map<String,? extends NeptuneIdentifiedObject>>();
	private Map<String, Company> companiesDictionary = new HashMap<String, Company>();
//	private Map<String, Line> linesDictionary = new HashMap<String, Line>();
//	private Map<String, Route> routesDictionary = new HashMap<String, Route>();
//	private Map<String, JourneyPattern> journeyPatternsDictionary = new HashMap<String, JourneyPattern>();
//	private Map<String, VehicleJourney> vehicleJourneysDictionary = new HashMap<String, VehicleJourney>();
//	private Map<String, StopPoint> stopPointsDictionary = new HashMap<String, StopPoint>();
//	private Map<String, StopArea> stopAreasDictionary = new HashMap<String, StopArea>();
//	private Map<String, Timetable> timetablesDictionary = new HashMap<String, Timetable>();

	public void connect()
	{
		populateDictionaries();
		connectLines();
		connectRoutes();
		connectCompanies();
		connectPTNetwork();
		connectJourneyPatterns();
		connectVehicleJourneys();
		connectStopPoints();
		connectStopAreas();
		connectTimetables();
	}


	private void populateDictionaries()
	{

		populateDictionnary(companies, companiesDictionary);
//		populateDictionnary(lines, linesDictionary);
//		populateDictionnary(routes, routesDictionary);
//		populateDictionnary(journeyPatterns, journeyPatternsDictionary);
//		populateDictionnary(vehicleJourneys, vehicleJourneysDictionary);
//		populateDictionnary(stopPoints, stopPointsDictionary);
//		populateDictionnary(stopAreas, stopAreasDictionary);
//		populateDictionnary(timetables, timetablesDictionary);
	}

	private <T extends NeptuneIdentifiedObject> void populateDictionnary(List<T> list, Map<String,T> dictionnary)
	{

		for(T item : list)
		{
			if(item != null && item.getObjectId() != null)
			{
				dictionnary.put(item.getObjectId(), item);
			}
		}
		if(list.size() > 0)
		{
			populatedDictionaries.put(list.get(0).getClass(), dictionnary);
		}
	}

	private void connectLines()
	{
		for (Line line : lines) 
		{
			line.setPtNetwork(ptNetwork);
//			line.setRoutes(getObjectsFromIds(line.getRouteIds(), Route.class));
			line.setCompany(getObjectFromId(line.getComment(), Company.class));
			line.setComment(null);
		}
	}


	private void connectRoutes()
	{
	}


	private void connectCompanies()
	{
	}

	private void connectPTNetwork() 
	{
		ptNetwork.setLines(lines);
	}

	private void connectJourneyPatterns() 
	{
	}


	private void connectVehicleJourneys()
	{
//		for(VehicleJourney vehicleJourney : vehicleJourneys)
//		{
//			vehicleJourney.setCompany(getObjectFromId(vehicleJourney.getCompanyId(), Company.class));
//			JourneyPattern journeyPattern = getObjectFromId(vehicleJourney.getJourneyPatternId(), JourneyPattern.class);
			// vehicleJourney.setJourneyPattern(journeyPattern);
//			if (journeyPattern != null)
//				journeyPattern.addVehicleJourney(vehicleJourney);
			// vehicleJourney.setRoute(getObjectFromId(vehicleJourney.getRouteId(), Route.class));
//			for(VehicleJourneyAtStop vehicleJourneyAtStop : vehicleJourney.getVehicleJourneyAtStops()){
//				vehicleJourneyAtStop.setStopPoint(getObjectFromId(vehicleJourneyAtStop.getStopPointId(), StopPoint.class));
//				vehicleJourneyAtStop.setVehicleJourney(vehicleJourney);
//			}
//			vehicleJourney.setTimeSlot(getObjectFromId(vehicleJourney.getTimeSlotId(), TimeSlot.class));
//		}
	}

	private void connectStopPoints() 
	{
//		for(StopPoint stopPoint : stopPoints)
//		{
//			stopPoint.setContainedInStopArea(getObjectFromId(stopPoint.getContainedInStopAreaId(), StopArea.class));
//		}
	}


	private void connectStopAreas() 
	{
//		for(StopArea stopArea : stopAreas)
//		{
//			stopArea.setContainedStopAreas(getObjectsFromIds(stopArea.getContainedStopIds(), StopArea.class));
//			if(stopArea.getContainedStopAreas() != null)
//			{
//				for(StopArea childStopArea : stopArea.getContainedStopAreas())
//				{
//					childStopArea.setParentStopArea(stopArea);
//				}
//			}
//			stopArea.setContainedStopPoints(getObjectsFromIds(stopArea.getContainedStopIds(), StopPoint.class));
//
//		}
	}

	private void connectTimetables() 
	{
//		for(Timetable timetable : timetables)
//		{
//			timetable.setVehicleJourneys(getObjectsFromIds(timetable.getVehicleJourneyIds(), VehicleJourney.class));
//			if(timetable.getVehicleJourneys() != null)
//			{
//				for(VehicleJourney vehicleJourney : timetable.getVehicleJourneys())
//				{
//					vehicleJourney.addTimetable(timetable);
//				}
//			}
//		}
	}


	@SuppressWarnings("unchecked")
	private <T extends NeptuneIdentifiedObject> List<T> getObjectsFromIds(List<String> ids, Class<T> dictionaryClass)
	{
		Map<String, ? extends NeptuneIdentifiedObject> dictionary =  populatedDictionaries.get(dictionaryClass);
		List<T> objects = new ArrayList<T>();

		if(dictionary != null && ids != null)
		{
			for(String id : ids)
			{
				T object = (T)dictionary.get(id);
				if(object != null)
				{
					objects.add(object);
				}
			}
		}

		if(objects.size() == 0)
		{
			objects = null;
		}

		return objects;
	}

	@SuppressWarnings("unchecked")
	private <T extends NeptuneIdentifiedObject> T getObjectFromId(String id, Class<T> dictionaryClass)
	{

		Map<String, ? extends NeptuneIdentifiedObject> dictionary =  populatedDictionaries.get(dictionaryClass);
		T object = null;

		if (dictionary != null)
			object = (T)dictionary.get(id);

		return object;
	}
}
