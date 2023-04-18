
package org.esa.snap.core.dataio.geometry;

import com.bc.ceres.binding.Converter;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.converters.JavaTypeConverter;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// todo - use new CsvWriter here (nf, 2012-04-12)

/**
 * A writer for VectorDataNodes.
 *
 * @author Norman
 */
public class WriterBasedVectorDataNodeWriter {

    private static long id = System.nanoTime();

    public void write(VectorDataNode vectorDataNode, Writer writer) throws IOException {
        writeNodeProperties(vectorDataNode, writer);
        writeFeatures(vectorDataNode.getFeatureCollection(), writer);
    }

    public void writeFeatures(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection, Writer writer) throws IOException {
        SimpleFeatureType simpleFeatureType = featureCollection.getSchema();
        writeFeatureType(simpleFeatureType, writer);
        writeFeatures0(featureCollection, writer);
    }

    public void writeProperties(Map<String, String> properties, Writer writer) throws IOException {
        Set<Map.Entry<String, String>> entries = properties.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            writer.write("#" + entry.getKey() + "=" + entry.getValue() + "\n");
        }
    }

    void writeNodeProperties(VectorDataNode vectorDataNode, Writer writer) throws IOException {
        HashMap<String, String> properties = new LinkedHashMap<>();
        final Map<Object, Object> userData = vectorDataNode.getFeatureType().getUserData();
        final Set<Map.Entry<Object, Object>> entries = userData.entrySet();
        for (Map.Entry<Object, Object> entry : entries) {
            if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                properties.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        String description = vectorDataNode.getDescription();
        if (StringUtils.isNotNullAndNotEmpty(description)) {
            properties.put(ProductNode.PROPERTY_NAME_DESCRIPTION, description);
        }
        String defaultCSS = vectorDataNode.getDefaultStyleCss();
        if (StringUtils.isNotNullAndNotEmpty(defaultCSS)) {
            properties.put(VectorDataNodeIO.PROPERTY_NAME_DEFAULT_CSS, defaultCSS);
        }
        writeProperties(properties, writer);
    }

    private void writeFeatures0(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection, Writer writer) throws IOException {
        Converter[] converters = VectorDataNodeIO.getConverters(featureCollection.getSchema());
        try (FeatureIterator<SimpleFeature> features = featureCollection.features()) {
            while (features.hasNext()) {
                SimpleFeature simpleFeature = features.next();

                String fid = simpleFeature.getID();
                if (fid == null || fid.isEmpty()) {
                    fid = String.format("FID%s", Long.toHexString(id++));
                }
                writer.write(fid);

                List<Object> attributes = simpleFeature.getAttributes();
                for (int i = 0; i < attributes.size(); i++) {
                    Object value = attributes.get(i);
                    String text = VectorDataNodeIO.NULL_TEXT;
                    if (value != null) {
                        Converter converter = converters[i];
                        text = converter.format(value);
                        text = VectorDataNodeIO.encodeTabString(text);
                    }
                    writer.write(VectorDataNodeIO.DEFAULT_DELIMITER_CHAR);
                    writer.write(text);
                }
                writer.write('\n');
            }
        }
    }

    private void writeFeatureType(SimpleFeatureType simpleFeatureType, Writer writer) throws IOException {

        writer.write(simpleFeatureType.getTypeName());

        List<AttributeDescriptor> attributeDescriptors = simpleFeatureType.getAttributeDescriptors();
        JavaTypeConverter typeConverter = new JavaTypeConverter();
        for (AttributeDescriptor attributeDescriptor : attributeDescriptors) {
            Class<?> binding = attributeDescriptor.getType().getBinding();
            String name = attributeDescriptor.getLocalName();
            String type = typeConverter.format(binding);

            writer.write(VectorDataNodeIO.DEFAULT_DELIMITER_CHAR);
            writer.write(name);
            writer.write(':');
            writer.write(type);
        }
        writer.write('\n');
    }
}
