package fr.certu.chouette.echange.comparator;

import java.util.Map;

import fr.certu.chouette.echange.ILectureEchange;
import fr.certu.chouette.modele.Transporteur;
import fr.certu.chouette.service.commun.ServiceException;

public class CompanyComparator extends AbstractChouetteDataComparator 
{  
  
  public boolean compareData(IExchangeableLineComparator master) throws ServiceException
  {
      this.master = master;
    ILectureEchange source = master.getSource();
    ILectureEchange target = master.getTarget();
    
    Transporteur sourceData = source.getTransporteur();
    Transporteur targetData = target.getTransporteur();
    
    // there is only one company for a line, so we can map the id immediately
    master.addMappingIds(sourceData.getObjectId(),targetData.getObjectId());
    
    // check the attributes 
    ChouetteObjectState objectState = new ChouetteObjectState(getMappingKey(),sourceData.getObjectId(),targetData.getObjectId());

    objectState.addAttributeState("name",sourceData.getName(),targetData.getName());
    
    master.addObjectState(objectState);
    
    return objectState.isIdentical();
  }

@Override
public Map<String, ChouetteObjectState> getStateMap()
{
    // TODO Auto-generated method stub
    return null;
}

}