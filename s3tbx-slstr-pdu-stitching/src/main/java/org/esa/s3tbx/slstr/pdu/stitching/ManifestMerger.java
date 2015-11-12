package org.esa.s3tbx.slstr.pdu.stitching;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.converters.DateFormatConverter;
import com.sun.org.apache.xerces.internal.dom.DeferredTextImpl;
import com.sun.org.apache.xerces.internal.dom.TextImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Tonio Fincke
 */
class ManifestMerger {

    private static final String[] discerningAttributesNames = {"ID", "name", "grid", "view", "element", "type", "role"};
    private static final DateFormatConverter SLSTR_DATE_FORMAT_CONVERTER =
            new DateFormatConverter(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));

    static Document mergeManifests(File[] manifestFiles) throws IOException, ParserConfigurationException, PDUStitchingException {
        List<Node> manifestList = new ArrayList<>();
        for (File manifestFile : manifestFiles) {
            manifestList.add(createXmlDocument(new FileInputStream(manifestFile)));
        }
        final DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document manifest = documentBuilder.newDocument();
        manifest.setXmlStandalone(true);
        mergeDefaultNodes(manifestList, manifest, manifest);
        return manifest;
    }

    private static boolean hasIdenticalChild(Node node, Node newNode) {
        if (newNode.getNodeName().equals("sentinel-safe:ephemeris")) {
            return false;
        }
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            final Node nodeToBeChecked = node.getChildNodes().item(i);
            if (nodeToBeChecked.getNodeName().equals(newNode.getNodeName())) {
                final NamedNodeMap nodeToBeCheckedAttributes = nodeToBeChecked.getAttributes();
                final NamedNodeMap attributes = newNode.getAttributes();
                if (nodeToBeCheckedAttributes != null && attributes != null) {
                    for (String name : discerningAttributesNames) {
                        final Node attributeToBeChecked = nodeToBeCheckedAttributes.getNamedItem(name);
                        final Node attribute = attributes.getNamedItem(name);
                        if (attributeToBeChecked != null && attribute != null &&
                                !attributeToBeChecked.getNodeValue().equals(attribute.getNodeValue())) {
                            return false;
                        }
                    }
                    return true;
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    private static void mergeDefaultNodes(List<Node> fromParents, Node toParent, Document toDocument) throws PDUStitchingException {
        NodeList[] childNodeLists = new NodeList[fromParents.size()];
        for (int i = 0; i < childNodeLists.length; i++) {
            childNodeLists[i] = fromParents.get(i).getChildNodes();
        }
        for (int j = 0; j < fromParents.size(); j++) {
            for (int i = 0; i < childNodeLists[j].getLength(); i++) {
                final Node child = childNodeLists[j].item(i);
                if (child instanceof TextImpl && child.getTextContent().contains("\n")) {
                    final Node lastChild = toParent.getLastChild();
                    if (!(lastChild instanceof TextImpl)) {
                        final String textContent = child.getTextContent();
                        final Text textNode = toDocument.createTextNode(textContent);
                        toParent.appendChild(textNode);
                    } else if (!lastChild.getTextContent().contains("\n")) {
                        final String textContent = child.getTextContent();
                        final Text textNode = toDocument.createTextNode(textContent);
                        toParent.appendChild(textNode);
                    }
                } else {
                    if (!hasIdenticalChild(toParent, child)) {
                        final String nodeValue = child.getNodeValue();
                        List<Node> childNodes = collectChildNodes(child, childNodeLists, j);
                        if (child instanceof DeferredTextImpl) {
                            final String textContent = child.getTextContent();
                            final Text textNode = toDocument.createTextNode(textContent);
                            toParent.appendChild(textNode);
                        } else {
                            final Element manifestElement = toDocument.createElement(child.getNodeName());
                            manifestElement.setNodeValue(nodeValue);
                            copyAttributes(childNodes, manifestElement);
                            toParent.appendChild(manifestElement);
                            mergeChildNodes(childNodes, manifestElement, toDocument);
                        }
                    }
                }
            }
        }
    }

    static void mergeChildNodes(List<Node> fromParents, Element toParent, Document toDocument) throws PDUStitchingException {
        if (toParent.getNodeName().equals("checksum")) {
            setChecksum(toParent);
        } else if (toParent.getNodeName().equals("slstr:nadirImageSize") ||
                toParent.getNodeName().equals("slstr:obliqueImageSize")) {
            mergeImageSizeNodes(fromParents, toParent, toDocument);
        } else if (toParent.getNodeName().equals("sentinel-safe:startTime")) {
            mergeStartTimeNodes(fromParents, toParent, toDocument);
        } else if (toParent.getNodeName().equals("sentinel-safe:stopTime")) {
            mergeStopTimeNodes(fromParents, toParent, toDocument);
        } else if (toParent.getNodeName().equals("slstr:classificationSummary")) {
            //todo implement
        } else if (toParent.getNodeName().equals("slstr:pixelQualitySummary")) {
            //todo implement
        } else if (toParent.getNodeName().equals("slstr:missingElements")) {
            //todo implement
        } else if (toParent.getNodeName().equals("sentinel-safe:footPrint")) {
            //todo implement
        } else if (toParent.getNodeName().equals("sentinel3:creationTime")) {
            //todo implement
        } else if (toParent.getNodeName().equals("sentinel3:granuleNumber")) {
            //todo implement
        } else if (toParent.getNodeName().equals("sentinel3:productName")) {
            //todo implement
        } else if (toParent.getNodeName().equals("sentinel3:receivingStartTime")) {
            //todo implement
        } else if (toParent.getNodeName().equals("sentinel3:receivingStopTime")) {
            //todo implement
        } else if (toParent.getNodeName().equals("slstr:classificationSummary")) {
            mergeSlstrClassificationSummaryNodes(fromParents, toParent, toDocument);
        } else {
            mergeDefaultNodes(fromParents, toParent, toDocument);
        }
    }

    static void setChecksum(Node toParent) throws PDUStitchingException {
        //todo get full path to file
//        Node sibling = toParent;
//        try {
//            while (!sibling.getNodeName().equals("fileLocation")) {
//                sibling = sibling.getPreviousSibling();
//
//            }
//            final NamedNodeMap attributes = sibling.getAttributes();
//            if (attributes != null) {
//                final Node hrefAttribute = attributes.getNamedItem("href");
//                if (hrefAttribute != null) {
//                    final String fileName = hrefAttribute.getNodeValue();
//                    final MessageDigest md5 = MessageDigest.getInstance("MD5");
//                    final DigestInputStream digestInputStream = new DigestInputStream(Files.newInputStream(Paths.get(fileName)), md5);
//                    digestInputStream.read();
//                    toParent.setNodeValue(new String(md5.digest()));
//                }
//            }
//        } catch (NullPointerException | NoSuchAlgorithmException | IOException npe) {
//            throw new PDUStitchingException("Could not create checksum");
//        }
        toParent.setNodeValue("");
    }

    //package local for testing
    static void mergeStartTimeNodes(List<Node> fromParents, Element toParent, Document toDocument) throws PDUStitchingException {
        String earliestDateAsNodeValue = fromParents.get(0).getFirstChild().getNodeValue();
        Date earliestDate = parseDate(earliestDateAsNodeValue);
        if (fromParents.size() > 1) {
            for (int i = 1; i < fromParents.size(); i++) {
                final String nodeValue = fromParents.get(i).getFirstChild().getNodeValue();
                final Date date = parseDate(nodeValue);
                if (date.before(earliestDate)) {
                    earliestDateAsNodeValue = nodeValue;
                    earliestDate = date;
                }
            }
        }
        final Text textNode = toDocument.createTextNode(earliestDateAsNodeValue);
        toParent.appendChild(textNode);
    }

    //package local for testing
    static void mergeStopTimeNodes(List<Node> fromParents, Element toParent, Document toDocument) throws PDUStitchingException {
        String latetDateAsNodeValue = fromParents.get(0).getFirstChild().getNodeValue();
        Date latestDate = parseDate(latetDateAsNodeValue);
        if (fromParents.size() > 1) {
            for (int i = 1; i < fromParents.size(); i++) {
                final String nodeValue = fromParents.get(i).getFirstChild().getNodeValue();
                final Date date = parseDate(nodeValue);
                if (date.after(latestDate)) {
                    latetDateAsNodeValue = nodeValue;
                    latestDate = date;
                }
            }
        }
        final Text textNode = toDocument.createTextNode(latetDateAsNodeValue);
        toParent.appendChild(textNode);
    }

    //package local for testing
    static void mergeImageSizeNodes(List<Node> fromParents, Element toParent, Document toDocument) throws PDUStitchingException {
        ImageSize[] imageSizes = new ImageSize[fromParents.size()];
        for (int i = 0; i < imageSizes.length; i++) {
            imageSizes[i] = ImageSizeHandler.extractImageSizeFromNode(fromParents.get(i), "");
        }
        final ImageSize targetImageSize = ImageSizeHandler.createTargetImageSize(imageSizes);
        final String grid = fromParents.get(0).getAttributes().item(0).getNodeValue();
        toParent.setAttribute("grid", grid);
        toParent.appendChild(toDocument.createTextNode("\n"));
        final Element startOffsetElement = toDocument.createElement("sentinel3:startOffset");
        final Text startOffsetTextNode = toDocument.createTextNode("" + targetImageSize.getStartOffset());
        startOffsetElement.appendChild(startOffsetTextNode);
        toParent.appendChild(startOffsetElement);
        toParent.appendChild(toDocument.createTextNode("\n"));
        final Element trackOffsetElement = toDocument.createElement("sentinel3:trackOffset");
        final Text trackOffsetTextNode = toDocument.createTextNode("" + targetImageSize.getTrackOffset());
        trackOffsetElement.appendChild(trackOffsetTextNode);
        toParent.appendChild(trackOffsetElement);
        toParent.appendChild(toDocument.createTextNode("\n"));
        final Element rowsElement = toDocument.createElement("sentinel3:rows");
        final Text rowsTextNode = toDocument.createTextNode("" + targetImageSize.getRows());
        rowsElement.appendChild(rowsTextNode);
        toParent.appendChild(rowsElement);
        toParent.appendChild(toDocument.createTextNode("\n"));
        final Element columnsElement = toDocument.createElement("sentinel3:columns");
        final Text columnsTextNode = toDocument.createTextNode("" + targetImageSize.getColumns());
        columnsElement.appendChild(columnsTextNode);
        toParent.appendChild(columnsElement);
        toParent.appendChild(toDocument.createTextNode("\n"));
    }

    static void mergeSlstrClassificationSummaryNodes(List<Node> fromParents, Node toParent, Document toDocument) throws PDUStitchingException {
        //todo implement
        mergeDefaultNodes(fromParents, toParent, toDocument);
    }

    private static List<Node> collectChildNodes(Node child, NodeList[] childNodeLists, int indexOfCurrentParent)
            throws PDUStitchingException {
        List<Node> itemNodes = new ArrayList<>();
        itemNodes.add(child);
        //todo handle this more gracefully
        if (child.getNodeName().equals("sentinel-safe:orbitReference")) {
            return itemNodes;
        }
        final String nodeValue = child.getNodeValue();
        if (indexOfCurrentParent < childNodeLists.length - 1) {
            for (int k = indexOfCurrentParent + 1; k < childNodeLists.length; k++) {
                for (int l = 0; l < childNodeLists[k].getLength(); l++) {
                    if (childNodeLists[k].item(l).getNodeName().equals(child.getNodeName())) {
                        boolean discerningAttributesAreDifferent = false;
                        final NamedNodeMap attributes = child.getAttributes();
                        final NamedNodeMap otherAttributes = childNodeLists[k].item(l).getAttributes();
                        if (attributes != null && otherAttributes != null) {
                            for (String name : discerningAttributesNames) {
                                final Node attributeToBeChecked = attributes.getNamedItem(name);
                                final Node attribute = otherAttributes.getNamedItem(name);
                                if (attributeToBeChecked != null && attribute != null &&
                                        !attributeToBeChecked.getNodeValue().equals(attribute.getNodeValue())) {
                                    discerningAttributesAreDifferent = true;
                                }
                            }
                        }
                        if (!discerningAttributesAreDifferent) {
                            final String otherNodeValue = childNodeLists[k].item(l).getNodeValue();
                            if ((otherNodeValue != null && nodeValue == null) ||
                                    (otherNodeValue == null && nodeValue != null) ||
                                    (otherNodeValue != null && !otherNodeValue.trim().equals(nodeValue.trim()))) {
                                throw new PDUStitchingException("Different values for node " + child.getParentNode().getNodeName() + ": "
                                                                        + otherNodeValue + ", " + nodeValue);
                            } else {
                                itemNodes.add(childNodeLists[k].item(l));
                                break;
                            }
                        }
                    }
                }
            }
        }
        return itemNodes;
    }

    private static void copyAttributes(List<Node> itemNodes, Element manifestElement) throws PDUStitchingException {
        for (int n = 0; n < itemNodes.size(); n++) {
            final Node currentItem = itemNodes.get(n);
            final NamedNodeMap attributes = currentItem.getAttributes();
            if (attributes != null) {
                for (int k = 0; k < attributes.getLength(); k++) {
                    final Node attribute = attributes.item(k);
                    final String attributeName = attribute.getNodeName();
                    if (!manifestElement.hasAttribute(attributeName)) {
                        String attributeValue = attribute.getNodeValue();
                        if (n < itemNodes.size() - 1) {
                            for (int m = n + 1; m < itemNodes.size(); m++) {
                                final NamedNodeMap otherItemAttributes = itemNodes.get(m).getAttributes();
                                final Node otherAttribute = otherItemAttributes.getNamedItem(attributeName);
                                if (otherAttribute != null) {
                                    final String otherAttributeValue = otherAttribute.getNodeValue();
                                    if (attributeName.equals("start")) {
                                        final Date startValue = parseDate(attributeValue);
                                        final Date otherStartValue = parseDate(otherAttributeValue);
                                        if (otherStartValue.before(startValue)) {
                                            attributeValue = otherAttributeValue;
                                        }
                                    } else if (attributeName.equals("stop")) {
                                        final Date startValue = parseDate(attributeValue);
                                        final Date otherStartValue = parseDate(otherAttributeValue);
                                        if (otherStartValue.after(startValue)) {
                                            attributeValue = otherAttributeValue;
                                        }
                                    } else if (!otherAttributeValue.equals(attributeValue)) {
                                        throw new PDUStitchingException("Different values for attribute " + attributeName +
                                                                                " of node " + currentItem.getNodeName());
                                    }
                                }
                            }
                        }
                        manifestElement.setAttribute(attributeName, attributeValue.trim());
                    }
                }
            }
        }
    }

    private static Date parseDate(String text) throws PDUStitchingException {
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

    private static Document createXmlDocument(InputStream inputStream) throws IOException {
        final String msg = "Cannot create document from manifest XML file.";
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException(msg, e);
        }
    }

}
