package fr.certu.chouette.model.neptune;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import fr.certu.chouette.model.neptune.type.AccessPointTypeEnum;

/**
 * Neptune AccessPoint  
 * <p/>
 * Note for fields comment : <br/>
 * when readable is added to comment, a implicit getter is available <br/>
 * when writable is added to comment, a implicit setter is available
 * 
 */
public class AccessPoint extends NeptuneLocalizedObject{
	private static final long serialVersionUID = 7520070228185917225L;
	
	/**
	 * Comment
	 * <br/><i>readable/writable</i>
	 */
	@Getter @Setter private String comment;
	/**
	 * ObjectId of container
	 * <br/><i>readable/writable</i>
	 */
	@Getter @Setter private String containedInStopArea;
	/**
	 * Container
	 * <br/><i>readable/writable</i>
	 */
	@Getter @Setter private StopArea containedIn;
	/**
	 * List of linked StopArea informations
	 * <br/><i>readable/writable</i>
	 */
	@Getter @Setter private List<AccessLink> accessLinks;
	/**
	 * Time for opening the AccessPoint
	 * <br/><i>readable/writable</i>
	 */
	@Getter @Setter private Time openingTime;
	/**
	 * Time for closing the AccessPoint
	 * <br/><i>readable/writable</i>
	 */
	@Getter @Setter private Time closingTime;
	/**
	 * access type (In,Out,InOut)
	 * <br/><i>readable/writable</i>
	 */
	@Getter @Setter private AccessPointTypeEnum type;
	/**
	 * Indicate if a Lift is available 
	 * <br/><i>readable/writable</i>
	 */
	@Getter @Setter private boolean liftAvailable;
	/**
	 * indicate if the link is equipped for mobility restricted persons 
	 * <br/><i>readable/writable</i>
	 */
	@Getter @Setter private boolean mobilityRestrictedSuitable;
	/**
	 * indicate if stairs are present on the link 
	 * <br/><i>readable/writable</i>
	 */
	@Getter @Setter private boolean stairsAvailable;

	/**
	 * add an AccessLink to AccesPoint if not already present
	 * <br/> no control is made on AccessLink's start and end links
	 * 
	 * @param accessLink link to add
	 */
	public void addAccessLink(AccessLink accessLink)
	{
		if (accessLinks == null) accessLinks = new ArrayList<AccessLink>();
		if (accessLink != null && !accessLinks.contains(accessLink)) 
		{
			accessLinks.add(accessLink);
			accessLink.setAccessPoint(this);
		}
	}

	/**
	 * add a collection of AccessLinks to AccesPoint if not already presents
	 * <br/> no control is made on AccessLink's start and end links
	 * 
	 * @param accessLinkCollection links to add
	 */
	public void addAccessLinks(Collection<AccessLink> accessLinkCollection)
	{
		if (accessLinks == null) accessLinks = new ArrayList<AccessLink>();

		for (AccessLink accessLink : accessLinkCollection) 
		{
			if (accessLink != null && !accessLinks.contains(accessLink)) 
			{
				accessLinks.add(accessLink);
				accessLink.setAccessPoint(this);
			}
		}

	}

	/**
	 * remove an AccessLink from AccesPoint if present
	 * 
	 * @param accessLink to remove
	 */
	public void removeAccessLink(AccessLink accessLink)
	{
		if (accessLinks == null) accessLinks = new ArrayList<AccessLink>();
		if (accessLinks.contains(accessLink)) accessLinks.remove(accessLink);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.certu.chouette.model.neptune.NeptuneIdentifiedObject#toString(java.
	 * lang.String, int)
	 */
	@Override
	public String toString(String indent, int level)
	{
		StringBuilder sb = new StringBuilder(super.toString(indent, level));
		sb.append("\n").append(indent).append("  comment = ").append(comment);
		sb.append("\n").append(indent).append("  liftAvailable = ").append(liftAvailable);
		sb.append("\n").append(indent).append("  mobilityRestrictedSuitable = ").append(mobilityRestrictedSuitable);
		sb.append("\n").append(indent).append("  stairsAvailable = ").append(stairsAvailable);
		return sb.toString();
	}

	
	@Override
	public void complete() 
	{
		if (isCompleted()) return;
		super.complete();
		if (getContainedIn() != null)
		{
			containedInStopArea = getContainedIn().getObjectId();
			getContainedIn().addAccessPoint(this);
		}
		else
		{
			containedInStopArea = "NEPTUNE:StopArea:UnusedField";
		}
	}




}
