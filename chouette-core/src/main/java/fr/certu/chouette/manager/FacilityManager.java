package fr.certu.chouette.manager;

import org.apache.log4j.Logger;

import fr.certu.chouette.model.neptune.Facility;
import fr.certu.chouette.model.neptune.NeptuneLocalizedObject;

/**
 * 
 * @author mamadou keira
 *
 */
public class FacilityManager extends AbstractNeptuneManager<Facility>
{
	private static final Logger logger = Logger.getLogger(FacilityManager.class); 
	
	public FacilityManager() 
	{
		super(Facility.class,Facility.FACILITY_KEY);
	}

	public void init()
	{
		NeptuneLocalizedObject.setGeographicService(getGeographicService());
	}
	
	@Override
	protected Logger getLogger() 
	{
		return logger;
	}

}
