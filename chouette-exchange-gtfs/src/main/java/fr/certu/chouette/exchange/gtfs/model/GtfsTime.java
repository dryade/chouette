/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

package fr.certu.chouette.exchange.gtfs.model;

import java.sql.Time;
import java.text.MessageFormat;
import java.util.Calendar;

import lombok.Getter;
import lombok.Setter;

/**
 * @author michel
 *
 */

public class GtfsTime implements Comparable<GtfsTime>
{
	private static final String mfHoraireHMS = "{0,number,00}:{1,number,00}:{2,number,00}";
	@Getter @Setter private Time time;
	@Getter @Setter private boolean tomorrow = false;

	public GtfsTime(String gtfsTimeFormat)
	{
		if (gtfsTimeFormat.contains(":"))
		{
		String[] token = gtfsTimeFormat.split(":");
		long h = Long.parseLong(token[0]);
		long m = Long.parseLong(token[1]);
		long s = Long.parseLong(token[2]);

		if (h >=24 ) 
		{
			tomorrow = true;
			h -= 24;
		}

		long t = (h*3600+m*60+s)*1000;
		time = new Time(t);
		}
		else
		{
			long s = Long.parseLong(gtfsTimeFormat);
			long t = s*1000;
			time = new Time(t);
		}
	}

	public GtfsTime(Time time,boolean tomorrow)
	{
		this.time = time;
		this.tomorrow = tomorrow;
	}

	public String toString()
	{
		// This will use default JVM timezone
		Calendar cal = Calendar.getInstance();
		cal.setTime(time);
		long h = cal.get(Calendar.HOUR_OF_DAY);
		long m = cal.get(Calendar.MINUTE);
		long s = cal.get(Calendar.SECOND);
		if (tomorrow)
			h+=24;
		return MessageFormat.format(mfHoraireHMS,h,m,s);
	}

    public String toSeconds()
    {
    	long s = time.getTime()/1000;
    	return Long.toString(s);
    }

	@Override
	public int compareTo(GtfsTime other) 
	{
		if (this.tomorrow == other.tomorrow)
		{
			return this.time.compareTo(other.getTime());
		}
		if (this.tomorrow) return 1;
		return -1;
	}



	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() 
	{
		return toString().hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof GtfsTime)) {
			return false;
		}
		GtfsTime other = (GtfsTime) obj;

		return toString().equals(other.toString());
	}
}
