package org.esa.s3tbx.slstr.pdu.stitching.ui;

import org.esa.snap.core.datamodel.GeoPos;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tonio Fincke
 */
class PDUBoundariesProvider {

    private List<String> names;
    private List<Object> inputs;
    private List<GeoPos[]> geoBoundaries;
    private List<Boolean> selected;

    PDUBoundariesProvider() {
        names = new ArrayList<>();
        inputs = new ArrayList<>();
        geoBoundaries = new ArrayList<>();
        selected = new ArrayList<>();
    }

    int getNumberOfElements() {
        return names.size();
    }

    void clear() {
        names.clear();
        inputs.clear();
        geoBoundaries.clear();
        selected.clear();
    }

    void extractBoundaryFromFile(File file, Object input, boolean isSelected) {
        final String fileName = file.getName();
        String directoryName;
        File metadataFile;
        if (!fileName.equals("xfdumanifest.xml")) {
            directoryName = fileName;
            metadataFile = new File(file.getAbsolutePath(), "xfdumanifest.xml");
        } else {
            directoryName = file.getParentFile().getName();
            metadataFile = file;
        }
        if (!metadataFile.exists() || !SlstrL1bFileNameValidator.isValidSlstrL1BFile(metadataFile)) {
            return;
        }
        try {
            final InputStream inputStream = new FileInputStream(metadataFile);
            final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
            final NodeList posListElements = document.getElementsByTagName("gml:posList");
            if (posListElements.getLength() != 1) {
                return;
            }
            final String[] geoPositions = posListElements.item(0).getTextContent().split(" ");
            if (geoPositions.length % 2 != 0) {
                return;
            }
            final GeoPos[] geoBoundary = new GeoPos[geoPositions.length / 2];
            for (int i = 0; i < geoPositions.length / 2; i++) {
                geoBoundary[i] = new GeoPos(Double.parseDouble(geoPositions[2 * i]), Double.parseDouble(geoPositions[(2 * i) + 1]));
            }
            names.add(directoryName);
            geoBoundaries.add(geoBoundary);
            selected.add(isSelected);
            inputs.add(input);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            //do not extract then
        }
    }

    String getName(int index) {
        return names.get(index);
    }

    GeoPos[] getGeoBoundary(int index) {
        return geoBoundaries.get(index);
    }

    boolean isSelected(int index) {
        return selected.get(index);
    }

    void setSelected(List selectedValuesList) {
        for (int i = 0; i < selected.size(); i++) {
            selected.set(i, selectedValuesList.contains(inputs.get(i)));
        }
    }
}
