/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */
package fr.certu.chouette.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.apache.log4j.Logger;

import fr.certu.chouette.common.ChouetteException;
import fr.certu.chouette.core.CoreException;
import fr.certu.chouette.core.CoreExceptionCode;
import fr.certu.chouette.core.CoreRuntimeException;
import fr.certu.chouette.dao.IDaoTemplate;
import fr.certu.chouette.filter.DetailLevelEnum;
import fr.certu.chouette.filter.Filter;
import fr.certu.chouette.model.neptune.NeptuneIdentifiedObject;
import fr.certu.chouette.model.user.User;
import fr.certu.chouette.plugin.exchange.FormatDescription;
import fr.certu.chouette.plugin.exchange.IExportPlugin;
import fr.certu.chouette.plugin.exchange.IImportPlugin;
import fr.certu.chouette.plugin.exchange.ParameterValue;
import fr.certu.chouette.plugin.report.Report;
import fr.certu.chouette.plugin.report.ReportHolder;
import fr.certu.chouette.plugin.report.ReportItem;
import fr.certu.chouette.plugin.validation.IValidationPlugin;
import fr.certu.chouette.plugin.validation.ValidationClassReportItem;
import fr.certu.chouette.plugin.validation.ValidationParameters;
import fr.certu.chouette.plugin.validation.ValidationReport;
import fr.certu.chouette.plugin.validation.ValidationStepDescription;

/**
 * @author michel
 *
 * @param <T>
 */
public abstract class AbstractNeptuneManager<T extends NeptuneIdentifiedObject> implements INeptuneManager<T>
{
	// data storage access by hibernate
	@Getter @Setter private IDaoTemplate<T> dao; 
	
	// data storage access by jdbc
	@Getter @Setter private IDaoTemplate<T> jdbcDao;


	private Map<String,IImportPlugin<T>> importPluginMap = new HashMap<String, IImportPlugin<T>>();
	private Map<String,IExportPlugin<T>> exportPluginMap = new HashMap<String, IExportPlugin<T>>();
	private Map<String,IExportPlugin<T>> exportDeletionPluginMap = new HashMap<String, IExportPlugin<T>>();
	private List<IValidationPlugin<T>> validationPluginList = new ArrayList<IValidationPlugin<T>>();

	private static Map<Class<?>,INeptuneManager<?>> managers = new HashMap<Class<?>, INeptuneManager<?>>();

	protected abstract Logger getLogger();
	/**
	 * 
	 */
	private Class<?> neptuneType ;


	public AbstractNeptuneManager(Class<?> neptuneType) 
	{
		managers.put(neptuneType, this);
		this.neptuneType = neptuneType;
	}



	@SuppressWarnings("unchecked")
	public T getNewInstance(User user) throws ChouetteException 
	{
		try 
		{
			return (T) neptuneType.getConstructor(new Class[0]).newInstance(new Object[0]);
		} 
		catch (Exception e) 
		{
			throw new CoreRuntimeException(CoreExceptionCode.FATAL, e);
		} 
	}

	/**
	 * @param neptuneType
	 * @return
	 */
	public static INeptuneManager<? extends NeptuneIdentifiedObject> getManager(Class<? extends NeptuneIdentifiedObject> neptuneType)
	{
		return managers.get(neptuneType);
	}

	/* (non-Javadoc)
	 * @see fr.certu.chouette.manager.INeptuneManager#addImportPlugin(fr.certu.chouette.plugin.exchange.IImportPlugin)
	 */
	@Override
	public void addImportPlugin(IImportPlugin<T> plugin)
	{
		importPluginMap.put(plugin.getDescription().getName(),plugin);
	}

	/* (non-Javadoc)
	 * @see fr.certu.chouette.manager.INeptuneManager#addExportPlugin(fr.certu.chouette.plugin.exchange.IExportPlugin)
	 */
	@Override
	public void addExportPlugin(IExportPlugin<T> plugin)
	{
		exportPluginMap.put(plugin.getDescription().getName(),plugin);
	}
	/* (non-Javadoc)
	 * @see fr.certu.chouette.manager.INeptuneManager#addExportDeletionPlugin(fr.certu.chouette.plugin.exchange.IExportPlugin)
	 */
	@Override
	public void addExportDeletionPlugin(IExportPlugin<T> plugin)
	{
		exportDeletionPluginMap.put(plugin.getDescription().getName(),plugin);
	}

	/* (non-Javadoc)
	 * @see fr.certu.chouette.manager.INeptuneManager#addValidationPlugin(fr.certu.chouette.plugin.validation.IValidationPlugin)
	 */
	@Override
	public void addValidationPlugin(IValidationPlugin<T> plugin)
	{
		validationPluginList.add(plugin);
	}

	/* (non-Javadoc)
	 * @see fr.certu.chouette.manager.INeptuneManager#addNew(fr.certu.chouette.model.user.User, fr.certu.chouette.model.neptune.NeptuneObject)
	 */
	@Override
	public T addNew(User user, T bean) throws ChouetteException 
	{
		if (getDao() == null) throw new CoreException(CoreExceptionCode.NO_DAO_AVAILABLE,"unavailable resource");
		// TODO : check user access
		getDao().save(bean);
		return bean;
	}

	/* (non-Javadoc)
	 * @see fr.certu.chouette.manager.INeptuneManager#exists(fr.certu.chouette.model.user.User, fr.certu.chouette.model.neptune.NeptuneObject)
	 */
	@Override
	public boolean exists(User user, T bean) throws ChouetteException
	{
		if (getDao() == null) throw new CoreException(CoreExceptionCode.NO_DAO_AVAILABLE,"unavailable resource");
		// TODO : check user access
		if (bean.getId() != null)
		{
			return getDao().exists(bean.getId());
		}
		if (bean.getObjectId() != null)
		{
			return getDao().exists(bean.getObjectId());
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see fr.certu.chouette.manager.INeptuneManager#get(fr.certu.chouette.model.user.User, fr.certu.chouette.filter.Filter, fr.certu.chouette.filter.DetailLevelEnum)
	 */
	@Override
	public T get(User user, Filter filter, DetailLevelEnum level) throws ChouetteException
	{
		if (getDao() == null) throw new CoreException(CoreExceptionCode.NO_DAO_AVAILABLE,"unavailable resource");
		// TODO : check user access
		if (filter.getType().equals(Filter.Type.EQUALS))
		{
			T bean = null;
			if (filter.getAttribute().equals("id"))
			{
				bean = getDao().get((Long) filter.getFirstValue());
			}
			if (filter.getAttribute().equals("objectId"))
			{
				bean = getDao().getByObjectId((String) filter.getFirstValue());
			}
			//bean.expand(level);
			return bean;
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see fr.certu.chouette.manager.INeptuneManager#getAll(fr.certu.chouette.model.user.User, fr.certu.chouette.filter.Filter, fr.certu.chouette.filter.DetailLevelEnum)
	 */
	@Override
	public List<T> getAll(User user, Filter filter, DetailLevelEnum level) throws ChouetteException
	{
		if (getDao() == null) throw new CoreException(CoreExceptionCode.NO_DAO_AVAILABLE,"unavailable resource");
		// TODO : check user access
		List<T> beans =  getDao().select(filter);
		/*
		for (T bean : beans)
		{
			bean.expand(level);
		}
		 */

		return beans;
	}





	@Override
	public List<T> getAll(User user) throws ChouetteException {
		// TODO check User access
		if (getDao() == null) throw new CoreException(CoreExceptionCode.NO_DAO_AVAILABLE,"unavailable resource");
		return getDao().getAll();
	}
	/* (non-Javadoc)
	 * @see fr.certu.chouette.manager.NeptuneBeanManager#update(fr.certu.chouette.model.user.User, fr.certu.chouette.model.neptune.NeptuneBean)
	 */
	@Override
	public void update(User user, T bean) throws ChouetteException
	{
		if (getDao() == null) throw new CoreException(CoreExceptionCode.NO_DAO_AVAILABLE,"unavailable resource");
		// TODO Auto-generated method stub
		getDao().update(bean);
	}

	/* (non-Javadoc)
	 * @see fr.certu.chouette.manager.NeptuneBeanManager#update(fr.certu.chouette.model.user.User, fr.certu.chouette.model.neptune.NeptuneBean, fr.certu.chouette.manager.NeptuneBeanManager.DETAIL_LEVEL)
	 */
	@Override
	public void update(User user, T bean, DetailLevelEnum level)
	throws ChouetteException
	{
		if (getDao() == null) throw new CoreException(CoreExceptionCode.NO_DAO_AVAILABLE,"unavailable resource");
		// TODO Auto-generated method stub
		getDao().update(bean);
	}

	/* (non-Javadoc)
	 * @see fr.certu.chouette.manager.NeptuneBeanManager#isRemovable(fr.certu.chouette.model.user.User, fr.certu.chouette.model.neptune.NeptuneBean)
	 */
	@Override
	public boolean isRemovable(User user, T bean) throws ChouetteException
	{
		if (getDao() == null) throw new CoreException(CoreExceptionCode.NO_DAO_AVAILABLE,"unavailable resource");
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see fr.certu.chouette.manager.NeptuneBeanManager#remove(fr.certu.chouette.model.user.User, fr.certu.chouette.model.neptune.NeptuneBean)
	 */
	@Override
	public void remove(User user, T bean,boolean propagate) throws ChouetteException
	{
		if (getDao() == null) throw new CoreException(CoreExceptionCode.NO_DAO_AVAILABLE,"unavailable resource");
		// TODO Auto-generated method stub
		getDao().remove(bean.getId());
	}

	/* (non-Javadoc)
	 * @see fr.certu.chouette.manager.NeptuneBeanManager#removeAll(fr.certu.chouette.model.user.User, fr.certu.chouette.manager.Filter)
	 */
	@Override
	public void removeAll(User user, Collection<T> objects,boolean propagate) throws ChouetteException
	{
		if (getDao() == null) throw new CoreException(CoreExceptionCode.NO_DAO_AVAILABLE,"unavailable resource");
		for (T t : objects) {
			remove(user, t, propagate);
		}
		//getDao().removeAll(objects);
	}

	@Override
	public int removeAll(User user,Filter filter) throws ChouetteException
	{
		if (getDao() == null) throw new CoreException(CoreExceptionCode.NO_DAO_AVAILABLE,"unavailable resource");
		return 0;
	}	

	/* (non-Javadoc)
	 * @see fr.certu.chouette.manager.INeptuneManager#getImportFormats(fr.certu.chouette.model.user.User)
	 */
	@Override
	public List<FormatDescription> getImportFormats(User user)
	throws ChouetteException 
	{
		List<FormatDescription> formats = new ArrayList<FormatDescription>();

		for (IImportPlugin<T> plugin : importPluginMap.values()) 
		{
			formats.add(plugin.getDescription());
		}

		return formats;
	}

	@Override
	public List<T> doImport(User user, String formatDescriptor,List<ParameterValue> parameters,ReportHolder report) throws ChouetteException 
	{
		IImportPlugin<T> plugin = importPluginMap.get(formatDescriptor);
		if (plugin == null) throw new CoreException(CoreExceptionCode.NO_PLUGIN_AVAILABLE,"unknown format :"+formatDescriptor);

		return plugin.doImport(parameters,report);


	}

	@Override
	public void save(User user, T object ,boolean propagate) throws ChouetteException
	{
		if (getDao() == null) throw new CoreException(CoreExceptionCode.NO_DAO_AVAILABLE,"unavailable resource");
		getDao().save(object);
	}
	/* (non-Javadoc)
	 * @see fr.certu.chouette.manager.INeptuneManager#saveAll(fr.certu.chouette.model.user.User, java.util.List)
	 */
	@Override
	public void saveAll(User user, List<T> beans,boolean propagate) throws ChouetteException 
	{
//		if (getDao() == null) throw new CoreException(CoreExceptionCode.NO_DAO_AVAILABLE,"unavailable resource");
//
//		for (T t : beans) {
//			save(user,t, propagate);
//		}
		if(getJdbcDao() == null)
			throw new CoreException(CoreExceptionCode.NO_JDBC_DAO_AVAILABLE, "unavailable resource");
		
		getJdbcDao().saveOrUpdateAll(beans);
	}

	/* (non-Javadoc)
	 * @see fr.certu.chouette.manager.NeptuneBeanManager#saveOrUpdateAll(fr.certu.chouette.model.user.User, java.util.List)
	 */
	@Override
	public void saveOrUpdateAll(User user, List<T> beans) throws ChouetteException
	{
		if (getDao() == null) throw new CoreException(CoreExceptionCode.NO_DAO_AVAILABLE,"unavailable resource");
		getDao().saveOrUpdateAll(beans);
	}


	/* (non-Javadoc)
	 * @see fr.certu.chouette.manager.INeptuneManager#getExportFormats(fr.certu.chouette.model.user.User)
	 */
	@Override
	public List<FormatDescription> getExportFormats(User user)
	throws ChouetteException 
	{
		List<FormatDescription> formats = new ArrayList<FormatDescription>();

		for (IExportPlugin<T> plugin : exportPluginMap.values()) 
		{
			formats.add(plugin.getDescription());
		}
		return formats;
	}

	/* (non-Javadoc)
	 * @see fr.certu.chouette.manager.INeptuneManager#doExport(fr.certu.chouette.model.user.User, java.util.List, java.lang.String, java.util.List, fr.certu.chouette.plugin.report.ReportHolder)
	 */
	@Override
	public void doExport(User user, List<T> beans, String formatDescriptor,
			List<ParameterValue> parameters,ReportHolder report) 
	throws ChouetteException 
	{
		IExportPlugin<T> plugin = exportPluginMap.get(formatDescriptor);
		if (plugin == null) throw new CoreException(CoreExceptionCode.NO_PLUGIN_AVAILABLE,"unknown format :"+formatDescriptor);

		plugin.doExport(beans,parameters,report);

	}

	/* (non-Javadoc)
	 * @see fr.certu.chouette.manager.INeptuneManager#getDeleteExportFormats(fr.certu.chouette.model.user.User)
	 */
	@Override
	public List<FormatDescription> getDeleteExportFormats(User user)
	throws ChouetteException 
	{
		List<FormatDescription> formats = new ArrayList<FormatDescription>();

		for (IExportPlugin<T> plugin : exportDeletionPluginMap.values()) 
		{
			formats.add(plugin.getDescription());
		}
		return formats;
	}

	/* (non-Javadoc)
	 * @see fr.certu.chouette.manager.INeptuneManager#doExportDeleted(fr.certu.chouette.model.user.User, java.util.List, java.lang.String, java.util.List, fr.certu.chouette.plugin.report.ReportHolder)
	 */
	@Override
	public void doExportDeleted(User user, List<T> beans,
			String formatDescriptor, List<ParameterValue> parameters,ReportHolder report)
	throws ChouetteException 
	{
		IExportPlugin<T> plugin = exportDeletionPluginMap.get(formatDescriptor);
		if (plugin == null) throw new CoreException(CoreExceptionCode.NO_PLUGIN_AVAILABLE,"unknown format :"+formatDescriptor);

		plugin.doExport(beans,parameters,report);

	}

	/* (non-Javadoc)
	 * @see fr.certu.chouette.manager.INeptuneManager#canValidate()
	 */
	@Override
	public boolean canValidate()
	{
		return validationPluginList.size() > 0;

	}


	/* (non-Javadoc)
	 * @see fr.certu.chouette.manager.INeptuneManager#validate(fr.certu.chouette.model.user.User, java.util.List, fr.certu.chouette.plugin.validation.ValidationParameters)
	 */
	@Override
	public Report validate(User user, List<T> beans, ValidationParameters parameters, Boolean... propagate) throws ChouetteException 
	{
		if (validationPluginList.size() == 0) throw new CoreException(CoreExceptionCode.NO_VALIDATION_PLUGIN_AVAILABLE,"");

		boolean hasToPropagate = true;
		if (propagate.length > 0) 
		{
			hasToPropagate=propagate[0].booleanValue();
		}

		Report r = new ValidationReport();
		ValidationClassReportItem[] validationClasses = new ValidationClassReportItem[ValidationClassReportItem.CLASS.values().length]; // see how manage max enum
		for (int i = 0; i < validationClasses.length; i++)
		{
			validationClasses[i] = new ValidationClassReportItem(ValidationClassReportItem.CLASS.values()[i]);
		}

		for (IValidationPlugin<T> plugin : validationPluginList)
		{
			List<ValidationClassReportItem> stepItems = plugin.doValidate(beans,parameters);

			for (ValidationClassReportItem item : stepItems) 
			{
				int rank=item.getValidationClass().ordinal();
				validationClasses[rank].addAll(item.getItems());
			}

		}

		if (hasToPropagate)
		{
			Report propagationReport = propagateValidation(user, beans, parameters,hasToPropagate);
			if (propagationReport != null && propagationReport.getItems() != null)
			{
				for (ReportItem item : propagationReport.getItems())
				{
					ValidationClassReportItem classItem = (ValidationClassReportItem) item;
					validationClasses[classItem.getValidationClass().ordinal()].addAll(classItem.getItems());
					validationClasses[classItem.getValidationClass().ordinal()].updateStatus(classItem.getStatus());
				}
			}
		}

		for (ValidationClassReportItem item : validationClasses) 
		{
			if (item.getItems() != null && !item.getItems().isEmpty())
			{
				item.sortItems();
				r.addItem(item);
			}
		}
		return r;
	}

	protected Report propagateValidation(User user, List<T> beans, ValidationParameters parameters,boolean propagate) throws ChouetteException 
	{
		return null;
	}


	/* (non-Javadoc)
	 * @see fr.certu.chouette.manager.INeptuneManager#getValidationSteps(fr.certu.chouette.model.user.User)
	 */
	@Override
	public List<ValidationStepDescription> getValidationSteps(User user)
	throws ChouetteException 
	{
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fr.certu.chouette.manager.INeptuneManager#validateStep(fr.certu.chouette.model.user.User, java.util.List, java.lang.String, fr.certu.chouette.plugin.validation.ValidationParameters)
	 */
	@Override
	public ReportItem validateStep(User user, List<T> beans, String stepDescriptor, ValidationParameters parameters)
	throws ChouetteException 
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void completeObject(User user, T bean) throws ChouetteException 
	{
	}
	
	@Override
	public T getByObjectId(String objectId) throws CoreException
	{
		if (getDao() == null) throw new CoreException(CoreExceptionCode.NO_DAO_AVAILABLE,"unavailable resource");
		
		return getDao().getByObjectId(objectId);		
	}
}
