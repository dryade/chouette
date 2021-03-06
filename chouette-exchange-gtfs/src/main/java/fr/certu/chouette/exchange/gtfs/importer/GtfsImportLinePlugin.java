package fr.certu.chouette.exchange.gtfs.importer;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import lombok.Setter;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import fr.certu.chouette.common.ChouetteException;
import fr.certu.chouette.exchange.gtfs.model.GtfsRoute;
import fr.certu.chouette.model.neptune.Line;
import fr.certu.chouette.plugin.exchange.FormatDescription;
import fr.certu.chouette.plugin.exchange.IImportPlugin;
import fr.certu.chouette.plugin.exchange.ParameterDescription;
import fr.certu.chouette.plugin.exchange.ParameterValue;
import fr.certu.chouette.plugin.exchange.SimpleParameterValue;
import fr.certu.chouette.plugin.exchange.report.ExchangeReport;
import fr.certu.chouette.plugin.exchange.report.ExchangeReportItem;
import fr.certu.chouette.plugin.exchange.report.LimitedExchangeReportItem;
import fr.certu.chouette.plugin.report.Report;
import fr.certu.chouette.plugin.report.ReportHolder;
import fr.certu.chouette.plugin.report.ReportItem;

public class GtfsImportLinePlugin implements IImportPlugin<Line>
{
	private static final Logger logger = Logger.getLogger(GtfsImportLinePlugin.class);
	private FormatDescription description;

	@Setter private NeptuneConverter converter; 
	@Setter private String dbDirectory = "/tmp";

	private List<String>        allowedExtensions = Arrays.asList(new String[] { "zip" });

	public GtfsImportLinePlugin()
	{
		description = new FormatDescription(this.getClass().getName());
		description.setName("GTFS");
		List<ParameterDescription> params = new ArrayList<ParameterDescription>();
		{
			ParameterDescription param = new ParameterDescription("inputFile", ParameterDescription.TYPE.FILEPATH, false, true);
			param.setAllowedExtensions(Arrays.asList(new String[]{"zip"}));
			params.add(param);
		}
		{
			ParameterDescription param = new ParameterDescription("fileFormat", ParameterDescription.TYPE.STRING, false,
					"file extension");
			param.setAllowedExtensions(Arrays.asList(new String[] { "zip" }));
			params.add(param);
		}
		{
			ParameterDescription param = new ParameterDescription("objectIdPrefix", ParameterDescription.TYPE.STRING, false, true);
			params.add(param);
		}
		{
			ParameterDescription param = new ParameterDescription("incremental", ParameterDescription.TYPE.STRING,false , false);
			params.add(param);
		}
		{
			ParameterDescription param = new ParameterDescription("mergeRouteByShortName", ParameterDescription.TYPE.BOOLEAN,false , "false");
			params.add(param);
		}
		{
			ParameterDescription param = new ParameterDescription("maxDistanceForCommercial", ParameterDescription.TYPE.INTEGER,false , "10");
			params.add(param);
		}
		{
			ParameterDescription param = new ParameterDescription("ignoreLastWord", ParameterDescription.TYPE.BOOLEAN, false, "false");
			params.add(param);
		}
		{
			ParameterDescription param = new ParameterDescription("ignoreEndChars", ParameterDescription.TYPE.INTEGER, false, "0");
			params.add(param);
		}
		{
			ParameterDescription param = new ParameterDescription("maxDistanceForConnectionLink", ParameterDescription.TYPE.INTEGER,false , "50");
			params.add(param);
		}
		{
			ParameterDescription param = new ParameterDescription("colorFile", ParameterDescription.TYPE.FILEPATH,false , false);
			params.add(param);
		}
		{
			ParameterDescription param = new ParameterDescription("optimizeMemory", ParameterDescription.TYPE.BOOLEAN,false , "false");
			params.add(param);
		}



		description.setParameterDescriptions(params);
	}

	@Override
	public FormatDescription getDescription() 
	{
		return description;
	}

	@Override
	public List<Line> doImport(List<ParameterValue> parameters, ReportHolder reportContainer) throws ChouetteException 
	{
		String filePath = null;
		String colorPath = null;
		String objectIdPrefix = null;
		double maxDistanceForCommercialStop = 10;
		double maxDistanceForConnectionLink = 50;
		boolean ignoreLastWord = false;;
		int ignoreEndCharacters = 0;
		boolean mergeRouteByShortName = false;
		boolean optimizeMemory = false;
		String incrementalPrefix = "";
		String extension = "file extension";
		boolean agencyFound = false;
		boolean stopFound = false;
		boolean stopTimeFound = false;
		boolean tripFound = false;
		boolean routeFound = false;
		boolean calendarFound = false;
		for (ParameterValue value : parameters) 
		{
			if (value instanceof SimpleParameterValue) 
			{
				SimpleParameterValue svalue = (SimpleParameterValue)value;
				if (svalue.getName().equalsIgnoreCase("inputFile")) 
				{
					filePath = svalue.getFilepathValue();
				} 
				else if (svalue.getName().equals("fileFormat"))
				{
					extension = svalue.getStringValue().toLowerCase();
				}
				else if (svalue.getName().equalsIgnoreCase("objectIdPrefix")) 
				{
					objectIdPrefix = svalue.getStringValue().toLowerCase();
				} 
				else if (svalue.getName().equalsIgnoreCase("incremental")) 
				{
					incrementalPrefix = svalue.getStringValue();
				} 
				else if (svalue.getName().equalsIgnoreCase("mergeRouteByShortName")) 
				{
					mergeRouteByShortName =  svalue.getBooleanValue();
				} 
				else if (svalue.getName().equalsIgnoreCase("maxDistanceForCommercial")) 
				{
					maxDistanceForCommercialStop = (double) svalue.getIntegerValue().doubleValue();
				} 
				else if (svalue.getName().equalsIgnoreCase("ignoreLastWord")) 
				{
					ignoreLastWord = svalue.getBooleanValue().booleanValue();
				}
				else if (svalue.getName().equalsIgnoreCase("optimizeMemory")) 
				{
					optimizeMemory = svalue.getBooleanValue().booleanValue();
				}
				else if (svalue.getName().equalsIgnoreCase("ignoreEndChars")) 
				{
					ignoreEndCharacters = svalue.getIntegerValue().intValue();
				}
				else if (svalue.getName().equalsIgnoreCase("maxDistanceForConnectionLink")) 
				{
					maxDistanceForConnectionLink = (double) svalue.getIntegerValue().doubleValue();
				} 
				else if (svalue.getName().equalsIgnoreCase("colorFile")) 
				{
					colorPath = svalue.getFilepathValue();
				} 
				else 
				{
					throw new IllegalArgumentException("unexpected argument " + svalue.getName());
				}
			}
			else 
			{
				throw new IllegalArgumentException("unexpected argument " + value.getName());
			}
		}
		if (filePath == null) 
		{
			logger.error("missing argument zipFile");
			throw new IllegalArgumentException("zipFile required");
		}

		if (objectIdPrefix == null) 
		{
			logger.error("missing argument objectIdPrefix");
			throw new IllegalArgumentException("objectIdPrefix required");
		}

		if (extension.equals("file extension"))
		{
			extension = FilenameUtils.getExtension(filePath).toLowerCase();
		}
		if (!allowedExtensions.contains(extension))
		{
			logger.error("invalid argument inputFile " + filePath + ", allowed format : "
					+ Arrays.toString(allowedExtensions.toArray()));
			throw new IllegalArgumentException("invalid file type : " + extension);
		}

		Report report = new ExchangeReport(ExchangeReport.KEY.IMPORT, description.getName());
		report.setStatus(Report.STATE.OK);
		reportContainer.setReport(report);
		
		ZipFile zip = null;
		try 
		{
			zip = new ZipFile(filePath);
		} 
		catch (IOException e) 
		{
			ReportItem item = new ExchangeReportItem(ExchangeReportItem.KEY.FILE_ERROR,Report.STATE.ERROR,filePath,e.getLocalizedMessage());
			report.addItem(item);
			report.setStatus(Report.STATE.FATAL);
			logger.error("zip import failed (cannot open zip)" + e.getLocalizedMessage());
			return null;
		}
		try
		{
			GtfsData data = new GtfsData(objectIdPrefix,dbDirectory,optimizeMemory);
			data.loadNetwork(objectIdPrefix);
			boolean ok = true;
			for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements() && ok;) 
			{
				ZipEntry entry = entries.nextElement();

				// ignore directory
				if (entry.isDirectory()) continue;

				String entryName = entry.getName();

				logger.info("analyzing "+entryName) ;
				if (entryName.endsWith("agency.txt"))
				{
					agencyFound=true;
					try 
					{
						ReportItem item = new LimitedExchangeReportItem(ExchangeReportItem.KEY.ZIP_ENTRY,Report.STATE.OK,entryName);
						data.loadAgencies(zip.getInputStream(entry),item);
						report.addItem(item);
					} 
					catch (Exception e) 
					{
						ReportItem item = new ExchangeReportItem(ExchangeReportItem.KEY.ZIP_ERROR,Report.STATE.ERROR,"agency.txt",filePath,e.getLocalizedMessage());
						report.addItem(item);
						report.setStatus(Report.STATE.ERROR);
						logger.error("zip import failed (cannot read agency.txt)" + e.getLocalizedMessage(),e);
						ok = false;
					}
				}
				else if (entryName.endsWith("calendar.txt"))
				{
					calendarFound=true;
					try 
					{
						ReportItem item = new LimitedExchangeReportItem(ExchangeReportItem.KEY.ZIP_ENTRY,Report.STATE.OK,entryName);
						data.loadCalendars(zip.getInputStream(entry),item);
						report.addItem(item);
					} 
					catch (Exception e) 
					{
						ReportItem item = new ExchangeReportItem(ExchangeReportItem.KEY.ZIP_ERROR,Report.STATE.ERROR,"calendar.txt",filePath,e.getLocalizedMessage());
						report.addItem(item);
						report.setStatus(Report.STATE.ERROR);
						logger.error("zip import failed (cannot read calendar.txt)" + e.getLocalizedMessage(),e);
						ok = false;
					}
				}
				else if (entryName.endsWith("calendar_dates.txt"))
				{
					calendarFound=true;
					try 
					{
						ReportItem item = new LimitedExchangeReportItem(ExchangeReportItem.KEY.ZIP_ENTRY,Report.STATE.OK,entryName);
						data.loadCalendarDates(zip.getInputStream(entry),item);
						report.addItem(item);
					} 
					catch (Exception e) 
					{
						ReportItem item = new ExchangeReportItem(ExchangeReportItem.KEY.ZIP_ERROR,Report.STATE.ERROR,"calendar_dates.txt",filePath,e.getLocalizedMessage());
						report.addItem(item);
						report.setStatus(Report.STATE.ERROR);
						logger.error("zip import failed (cannot read calendar_dates.txt )" + e.getLocalizedMessage(),e);
						ok = false;
					}
				}
				else if (entryName.endsWith("frequencies.txt"))
				{
					try 
					{
						ReportItem item = new LimitedExchangeReportItem(ExchangeReportItem.KEY.ZIP_ENTRY,Report.STATE.OK,entryName);
						data.loadFrequencies(zip.getInputStream(entry),item);
						report.addItem(item);
					} 
					catch (Exception e) 
					{
						ReportItem item = new ExchangeReportItem(ExchangeReportItem.KEY.ZIP_ERROR,Report.STATE.ERROR,"frequencies.txt",filePath,e.getLocalizedMessage());
						report.addItem(item);
						report.setStatus(Report.STATE.ERROR);
						logger.error("zip import failed (cannot read frequencies.txt)" + e.getLocalizedMessage(),e);
						ok = false;
					}
				}
				else if (entryName.endsWith("routes.txt"))
				{
					routeFound=true;
					try 
					{
						ReportItem item = new LimitedExchangeReportItem(ExchangeReportItem.KEY.ZIP_ENTRY,Report.STATE.OK,entryName);
						data.loadRoutes(zip.getInputStream(entry),item);
						report.addItem(item);
					} 
					catch (Exception e) 
					{
						ReportItem item = new ExchangeReportItem(ExchangeReportItem.KEY.ZIP_ERROR,Report.STATE.ERROR,"routes.txt",filePath,e.getLocalizedMessage());
						report.addItem(item);
						report.setStatus(Report.STATE.ERROR);
						logger.error("zip import failed (cannot read routes.txt)" + e.getLocalizedMessage(),e);
						ok = false;
					}
				}
				else if (entryName.endsWith("stops.txt"))
				{
					stopFound=true;
					try 
					{
						ReportItem item = new LimitedExchangeReportItem(ExchangeReportItem.KEY.ZIP_ENTRY,Report.STATE.OK,entryName);
						data.loadStops(zip.getInputStream(entry),item);
						report.addItem(item);
					} 
					catch (Exception e) 
					{
						ReportItem item = new ExchangeReportItem(ExchangeReportItem.KEY.ZIP_ERROR,Report.STATE.ERROR,"stops.txt",filePath,e.getLocalizedMessage());
						report.addItem(item);
						report.setStatus(Report.STATE.ERROR);
					logger.error("zip import failed (cannot read stops.txt)" + e.getLocalizedMessage(),e);
						ok = false;
					}
				}
				else if (entryName.endsWith("stop_times.txt"))
				{
					stopTimeFound=true;
					try 
					{
						ReportItem item = new LimitedExchangeReportItem(ExchangeReportItem.KEY.ZIP_ENTRY,Report.STATE.OK,entryName);
						data.loadStopTimes(zip.getInputStream(entry),item);
						report.addItem(item);
					} 
					catch (Exception e) 
					{
						ReportItem item = new ExchangeReportItem(ExchangeReportItem.KEY.ZIP_ERROR,Report.STATE.ERROR,"stop_times.txt",filePath,e.getLocalizedMessage());
						report.addItem(item);
						report.setStatus(Report.STATE.ERROR);
						logger.error("zip import failed (cannot read stop_times.txt)" + e.getLocalizedMessage(),e);
						ok = false;
					}
				}
				else if (entryName.endsWith("trips.txt"))
				{
					tripFound=true;
					try 
					{
						ReportItem item = new LimitedExchangeReportItem(ExchangeReportItem.KEY.ZIP_ENTRY,Report.STATE.OK,entryName);
						data.loadTrips(zip.getInputStream(entry),item);
						report.addItem(item);
					} 
					catch (Exception e) 
					{
						ReportItem item = new ExchangeReportItem(ExchangeReportItem.KEY.ZIP_ERROR,Report.STATE.ERROR,"trips.txt",filePath,e.getLocalizedMessage());
						report.addItem(item);
						report.setStatus(Report.STATE.ERROR);
						logger.error("zip import failed (cannot read trips.txt)" + e.getLocalizedMessage(),e);
						ok = false;
					}
				}
//				else if (entryName.endsWith("shapes.txt"))
//				{
//					try 
//					{
//						data.loadShapes(zip.getInputStream(entry));
//					} 
//					catch (Exception e) 
//					{
//						logger.error("zip import failed (cannot read shapes.txt)" + e.getLocalizedMessage(),e);
//						ok = false;
//					}
//				}
				else if (entryName.endsWith("transfers.txt"))
				{
					try 
					{
						ReportItem item = new LimitedExchangeReportItem(ExchangeReportItem.KEY.ZIP_ENTRY,Report.STATE.OK,entryName);
						data.loadTransfers(zip.getInputStream(entry),item);
						report.addItem(item);
					} 
					catch (Exception e) 
					{
						ReportItem item = new ExchangeReportItem(ExchangeReportItem.KEY.ZIP_ERROR,Report.STATE.ERROR,"transfers.txt",filePath,e.getLocalizedMessage());
						report.addItem(item);
						report.setStatus(Report.STATE.ERROR);
						logger.error("zip import failed (cannot read transfers.txt)" + e.getLocalizedMessage(),e);
						ok = false;
					}
				}
				else
				{
					ReportItem item = new ExchangeReportItem(ExchangeReportItem.KEY.FILE_IGNORED,Report.STATE.OK,entryName);
					report.addItem(item);
					logger.info("entry "+entryName+" unused");
				}
			}
			// check mandatory entries
			if (!agencyFound)
			{
				ReportItem item = new ExchangeReportItem(ExchangeReportItem.KEY.ZIP_MISSING_ENTRY,Report.STATE.ERROR,"agency.txt",filePath);
				report.addItem(item);
				report.setStatus(Report.STATE.ERROR);
				logger.error("zip import failed (missing entry agency.txt)");
				ok = false;				
			}
			if (!routeFound)
			{
				ReportItem item = new ExchangeReportItem(ExchangeReportItem.KEY.ZIP_MISSING_ENTRY,Report.STATE.ERROR,"routes.txt",filePath);
				report.addItem(item);
				report.setStatus(Report.STATE.ERROR);
				logger.error("zip import failed (missing entry routes.txt)");
				ok = false;				
			}
			if (!stopFound)
			{
				ReportItem item = new ExchangeReportItem(ExchangeReportItem.KEY.ZIP_MISSING_ENTRY,Report.STATE.ERROR,"stops.txt",filePath);
				report.addItem(item);
				report.setStatus(Report.STATE.ERROR);
				logger.error("zip import failed (missing entry stops.txt)");
				ok = false;				
			}
			if (!stopTimeFound)
			{
				ReportItem item = new ExchangeReportItem(ExchangeReportItem.KEY.ZIP_MISSING_ENTRY,Report.STATE.ERROR,"stop_times.txt",filePath);
				report.addItem(item);
				report.setStatus(Report.STATE.ERROR);
				logger.error("zip import failed (missing entry stop_times.txt)");
				ok = false;				
			}
			if (!calendarFound)
			{
				ReportItem item = new ExchangeReportItem(ExchangeReportItem.KEY.ZIP_MISSING_ENTRY,Report.STATE.ERROR,"calendars.txt, calendar_dates.txt",filePath);
				report.addItem(item);
				report.setStatus(Report.STATE.ERROR);
				logger.error("zip import failed (missing entry calendars.txt, calendar_dates.txt)");
				ok = false;				
			}
			if (!tripFound)
			{
				ReportItem item = new ExchangeReportItem(ExchangeReportItem.KEY.ZIP_MISSING_ENTRY,Report.STATE.ERROR,"trips.txt",filePath);
				report.addItem(item);
				report.setStatus(Report.STATE.ERROR);
				logger.error("zip import failed (missing entry trips.txt)");
				ok = false;				
			}
			
			if (ok && data.connect(report))
			{
				System.gc();
				ModelAssembler assembler = converter.convert(optimizeMemory, objectIdPrefix, incrementalPrefix, data, maxDistanceForCommercialStop,  ignoreLastWord,  ignoreEndCharacters, maxDistanceForConnectionLink, mergeRouteByShortName,report);
				assembler.connect(report);
				if (colorPath != null) produceColorFile(colorPath,data,assembler);
				// report objects count
				{
					ExchangeReportItem countItem = new ExchangeReportItem(ExchangeReportItem.KEY.LINE_COUNT,Report.STATE.OK,assembler.getLines().size());
					report.addItem(countItem);
					countItem = new ExchangeReportItem(ExchangeReportItem.KEY.ROUTE_COUNT,Report.STATE.OK,assembler.getRoutes().size());
					report.addItem(countItem);
					countItem = new ExchangeReportItem(ExchangeReportItem.KEY.JOURNEY_PATTERN_COUNT,Report.STATE.OK,assembler.getJourneyPatterns().size());
					report.addItem(countItem);
					countItem = new ExchangeReportItem(ExchangeReportItem.KEY.VEHICLE_JOURNEY_COUNT,Report.STATE.OK,assembler.getVehicleJourneys().size());
					report.addItem(countItem);
					countItem = new ExchangeReportItem(ExchangeReportItem.KEY.STOP_AREA_COUNT,Report.STATE.OK,assembler.getStopAreas().size());
					report.addItem(countItem);
					countItem = new ExchangeReportItem(ExchangeReportItem.KEY.CONNECTION_LINK_COUNT,Report.STATE.OK,assembler.getConnectionLinks().size());
					report.addItem(countItem);
					countItem = new ExchangeReportItem(ExchangeReportItem.KEY.TIME_TABLE_COUNT,Report.STATE.OK,assembler.getTimetables().size());
					report.addItem(countItem);
				}
				return assembler.getLines();
			}
			else
			{
				return new ArrayList<Line>();
			}
		}
		finally
		{
			try {
				zip.close();
			} catch (IOException e) {
				logger.warn("cannot close zip file");
			}

		}

	}

	private void produceColorFile(String colorPath, GtfsData data, ModelAssembler assembler) 
	{
		File file = new File(colorPath);
		try {
			PrintWriter fw = new PrintWriter(file);
			List<Line> lines = assembler.getLines();
			List<GtfsRoute> gtfsRoutes = data.getRoutes().getAll();
			for (Line line : lines) 
			{
				String objectId = line.getObjectId();
				String routeId = line.getRoutes().get(0).getObjectId().split(":")[2];
				routeId = routeId.substring(0,routeId.lastIndexOf("_"));
				Color c = null;
				for (GtfsRoute gtfsRoute : gtfsRoutes) 
				{
					if (gtfsRoute.getRouteId().equals(routeId))
					{
						c = gtfsRoute.getRouteColor();
						break;
					}
				}
				if (c != null)
				{
					fw.print(objectId);
					fw.print("=");
					fw.println(toColorString(c));
				}
			}
			fw.close();


		}
		catch (Exception e) 
		{
			logger.error("cannot create color file "+e.getMessage() );
		}

	}

	private String toColorString(Color c) 
	{
		String str = Integer.toHexString(c.getRGB()).substring(2);
		while (str.length() < 8) str = "0"+str;
		str = "ff"+str.substring(2);
		return str;
	}


}
