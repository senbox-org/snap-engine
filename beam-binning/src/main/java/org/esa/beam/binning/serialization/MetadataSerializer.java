package org.esa.beam.binning.serialization;

import com.thoughtworks.xstream.XStream;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.util.StringUtils;

public class MetadataSerializer {

    private final XStream xStream;

    public MetadataSerializer() {
        xStream = new XStream();
    }

    public String toXml(MetadataElement metadataElement) {
        if (metadataElement == null) {
            return "";
        }
        return xStream.toXML(metadataElement);
    }

    public MetadataElement fromXml(String xml) {
        if (StringUtils.isNullOrEmpty(xml)) {
            return null;
        }
        return (MetadataElement) xStream.fromXML(xml);
    }
}
