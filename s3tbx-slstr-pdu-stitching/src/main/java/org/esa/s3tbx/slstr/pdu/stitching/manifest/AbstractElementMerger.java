package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.converters.DateFormatConverter;
import org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Tonio Fincke
 */
public abstract class AbstractElementMerger implements ElementMerger {

    protected static final DateFormatConverter SLSTR_DATE_FORMAT_CONVERTER =
            new DateFormatConverter(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));

    protected void addTextToNode(Node node, String text, Document toDocument) throws PDUStitchingException {
        final Text textNode = toDocument.createTextNode(text);
        node.appendChild(textNode);
    }

    protected static Date parseDate(String text) throws PDUStitchingException {
        String subDate = text;
        if (text.endsWith("Z")) {
            subDate = text.substring(0, 23) + "Z";
        }
        try {
            return SLSTR_DATE_FORMAT_CONVERTER.parse(subDate);
        } catch (ConversionException e) {
            throw new PDUStitchingException("Error while parsing start time: " + e.getMessage());
        }
    }

}
