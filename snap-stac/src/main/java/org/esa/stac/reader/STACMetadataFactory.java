package org.esa.stac.reader;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.stac.internal.StacItem;
import org.json.simple.JSONObject;

public class STACMetadataFactory {

    private static MetadataElement convertJSONToSNAPMetadata(JSONObject json, MetadataElement curElement){

        return null;
    }
    public static MetadataElement convertJSONToSNAPMetadata(JSONObject json){
        MetadataElement root = new MetadataElement("root");
        for (Object o : json.keySet()){
            if (o instanceof JSONObject){
                
            }
        }
        return null;
    }

    private final StacItem item;
    public STACMetadataFactory(StacItem item){
        this.item = item;
    }

    // Creates a metadata element formatted as the abstract metadata root
    // from a StacItem.
    public MetadataElement generate(){
        MetadataElement root = new MetadataElement(AbstractMetadata.ABSTRACT_METADATA_ROOT);
        AbstractMetadata.setAttribute(root, "PRODUCT", item.getId());
        return root;
    }
}
