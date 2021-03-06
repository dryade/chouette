/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */
package fr.certu.chouette.model.neptune;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;
import fr.certu.chouette.model.neptune.type.LongLatTypeEnum;
import fr.certu.chouette.service.geographic.IGeographicService;

/**
 * Abstract object used for all Localized Neptune Object
 * <p/>
 * Note for fields comment : <br/>
 * when readable is added to comment, a implicit getter is available <br/>
 * when writable is added to comment, a implicit setter is available
 */
@SuppressWarnings("serial")
public abstract class NeptuneLocalizedObject extends NeptuneIdentifiedObject
{
	// geographic tool for coordinate conversion
	@Setter @Getter private static IGeographicService geographicService;
	// constant for persistence fields
	public static final String LONGITUDE ="longitude"; 
	public static final String LATITUDE ="latitude"; 
	public static final String LONGLAT_TYPE="longLatType"; 
	public static final String COUNTRY_CODE="countryCode"; 
	public static final String STREET_NAME="streetName"; 
	public static final String X="x"; 
	public static final String Y="y"; 
	public static final String PROJECTION_TYPE="projectionType"; 

	/**
	 * Spatial Referential Type (actually only WGS84 is valid)  
	 * <br/><i>readable/writable</i>
	 */
	@Getter @Setter private LongLatTypeEnum longLatType;
	/**
	 * Latitude position of area 
	 * <br/><i>readable/writable</i>
	 */
	@Getter @Setter private BigDecimal latitude;
	/**
	 * Longitude position of area
	 * <br/><i>readable/writable</i>
	 */
	@Getter @Setter private BigDecimal longitude;
	/**
	 * address street name 
	 * <br/><i>readable/writable</i>
	 */
	@Getter @Setter private String streetName;
	/**
	 * address city or district code
	 * <br/><i>readable/writable</i>
	 */
	@Getter @Setter private String countryCode;

	/**
	 * x coordinate
	 * <br/><i>readable/writable</i>
	 */
	@Getter @Setter private BigDecimal x;
	/**
	 * y coordinate
	 * <br/><i>readable/writable</i>
	 */
	@Getter @Setter private BigDecimal y;
	/**
	 * projection system name (f.e. : epgs:27578)
	 * <br/><i>readable/writable</i>
	 */
	@Getter @Setter private String projectionType;

	public boolean hasCoordinates()
	{
		return longitude != null && latitude != null && longLatType != null;
	}

	public boolean hasAddress()
	{
		return notEmptyString(countryCode) || notEmptyString(streetName);
	}

	public boolean hasProjection()
	{
		return x != null && x != null &&  notEmptyString(projectionType);
	}

	private boolean notEmptyString(String data)
	{
		return data != null && !data.isEmpty();
	}
	/* (non-Javadoc)
	 * @see fr.certu.chouette.model.neptune.NeptuneObject#toString(java.lang.String, int)
	 */
	@Override
	public String toString(String indent, int level)
	{
		String s = super.toString(indent,level);
		StringBuffer sb = new StringBuffer(s);
		if (streetName != null && !streetName.isEmpty())
			sb.append("\n").append(indent).append("  streetName = ").append(streetName);
		if (countryCode != null && !countryCode.isEmpty())
			sb.append("\n").append(indent).append("  countryCode = ").append(countryCode);
		sb.append("\n").append(indent).append("  longLatType = ").append(longLatType);
		sb.append("\n").append(indent).append("  latitude = ").append(latitude);
		sb.append("\n").append(indent).append("  longitude = ").append(longitude);
		if (x != null)
			sb.append("\n").append(indent).append("  x = ").append(x);
		if (y != null)
			sb.append("\n").append(indent).append("  y = ").append(y);
		if (projectionType != null && !projectionType.isEmpty())
			sb.append("\n").append(indent).append("  projection = ").append(projectionType);

		return sb.toString();
	}
	
	@Override
	public void complete() 
	{
		super.complete();
		if (!hasCoordinates()) return;
		geographicService.convertToProjection(this);
	}

    public void toLatLong()
    {
    	if (!hasProjection()) return;
    	geographicService.convertToWGS84(this);
    	
    }

}
