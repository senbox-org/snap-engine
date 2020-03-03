package org.esa.snap.dataio.bigtiff;

import org.esa.snap.core.datamodel.Band;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Utility class for BigGeoTiff I/O
 *
 * @author olafd
 */
class BigGeoTiffUtils {
    /**
     * Parses a GDAL metadata string and takes the information to set up the bands for a target product with a
     * maximum of information. Acutally implemented and tested for the purpose of improved reading of
     * Proba-V S* GeoTiff products (see SIIITBX-85).
     * TODO: this is duplicated from standard GeoTiff reader. Try to make GDAL support more generic.
     *
     * @param gdalMetadataXmlString - GDAL metadata XLM string
     * @param productDataType - product data type
     * @param width - product width
     * @param height - product height
     *
     * @return array of bands
     * @throws Exception -
     */
    static Band[] setupBandsFromGdalMetadata(String gdalMetadataXmlString,
                                             int productDataType,
                                             int width, int height) throws Exception {
        final DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(gdalMetadataXmlString));

        final Document doc = db.parse(is);
        final NodeList nodes = doc.getElementsByTagName("GDALMetadata");
        final Element element = (Element) nodes.item(0);

        ArrayList bandnameList = new ArrayList();
        ArrayList descrList = new ArrayList();
        ArrayList unitsList = new ArrayList();
        ArrayList nodataList = new ArrayList();
        ArrayList offsetList = new ArrayList();
        ArrayList scaleList = new ArrayList();
        for (int i = 0; i < element.getElementsByTagName("Item").getLength(); i++) {
            final Node node = element.getElementsByTagName("Item").item(i);
            final Node child = node.getFirstChild();
            final CharacterData cd = (CharacterData) child;
            if (node.hasAttributes()) {
                for (int j = 0; j < node.getAttributes().getLength(); j++) {
                    final Node attr = node.getAttributes().item(j);
                    if (attr.getNodeName().equals("name")) {
                        switch (attr.getNodeValue()) {
                            case "BAND":
                                bandnameList.add(cd.getData());
                                break;
                            case "DESCRIPTION":
                                descrList.add(cd.getData());
                                break;
                            case "UNITS":
                                unitsList.add(cd.getData());
                                break;
                            case "NODATA":
                                nodataList.add(cd.getData());
                                break;
                            case "OFFSET":
                                offsetList.add(cd.getData());
                                break;
                            case "SCALE":
                                scaleList.add(cd.getData());
                                break;
                        }
                    }
                }
            }
        }

        Iterator descrListIterator = descrList.iterator();
        Iterator unitsListIterator = unitsList.iterator();
        Iterator nodataListIterator = nodataList.iterator();
        Iterator offsetListIterator = offsetList.iterator();
        Iterator scaleListIterator = scaleList.iterator();

        Band[] bands = new Band[bandnameList.size()];
        for (int i = 0; i < bandnameList.size(); i++) {
            bands[i] = new Band((String) bandnameList.get(i), productDataType, width, height);
            if (descrListIterator.hasNext()) {
                bands[i].setDescription((String) descrListIterator.next());
            }
            if (unitsListIterator.hasNext()) {
                bands[i].setUnit((String) unitsListIterator.next());
            }
            if (nodataListIterator.hasNext()) {
                final String nodataValString = (String) nodataListIterator.next();
                if (nodataValString != null) {
                    final double nodataVal = Double.parseDouble(nodataValString);
                    bands[i].setNoDataValue(nodataVal);
                    bands[i].setNoDataValueUsed(true);
                }
            }
            if (offsetListIterator.hasNext()) {
                final String offsetValString = (String) offsetListIterator.next();
                if (offsetValString != null) {
                    final double offsetVal = Double.parseDouble(offsetValString);
                    bands[i].setScalingOffset(offsetVal);
                }
            }
            if (scaleListIterator.hasNext()) {
                final String scaleValString = (String) scaleListIterator.next();
                if (scaleValString != null) {
                    final double scaleVal = Double.parseDouble(scaleValString);
                    bands[i].setScalingFactor(scaleVal);
                }
            }
        }

        return bands;
    }
}
