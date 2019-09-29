package org.esa.snap.remote.execution.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;
import org.esa.snap.remote.execution.RemoteExecutionOp;

/**
 * Created by jcoravu on 4/1/2019.
 */
public class SourceProductFilesConverter implements Converter<String[]> {

    public SourceProductFilesConverter() {
    }

    @Override
    public Class<? extends String[]> getValueType() {
        return String[].class;
    }

    @Override
    public String[] parse(String text) throws ConversionException {
        // <file path='\\\\192.168.240.65\\shared\\3_8bit_components_srgb.tif'/><file path='\\\\192.168.240.65\\shared\\deimos.dm/>
        StringBuilder xml = new StringBuilder();
        xml.append("<source-product-files>")
           .append(text)
           .append("</source-product-files>");
        XppDom rootNode = RemoteExecutionOp.buildDom(xml.toString());
        if (rootNode.getName().equalsIgnoreCase("source-product-files")) {
            int childCount = rootNode.getChildCount();
            String[] result = new String[childCount];
            for (int i=0; i<childCount; i++) {
                XppDom childNote = rootNode.getChild(i);
                if (childNote.getName().equalsIgnoreCase("file")) {
                    String filePath = childNote.getAttribute("path");
                    result[i] = filePath;
                } else {
                    throw new IllegalArgumentException("Unknown child tag name '"+childNote.getName()+"'.");
                }
            }
            return result;
        } else {
            throw new IllegalArgumentException("Unknown root tag name '"+rootNode.getName()+"'.");
        }
    }

    @Override
    public String format(String[] value) {
        StringBuilder xml = new StringBuilder();
        for (int i=0; i<value.length; i++) {
            xml.append("<file path='")
                    .append(value[i])
                    .append("' />");
        }
        return xml.toString();
    }
}
