package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import com.sun.org.apache.xerces.internal.dom.DeferredTextImpl;
import com.sun.org.apache.xerces.internal.dom.TextImpl;
import org.esa.s3tbx.slstr.pdu.stitching.ImageSize;
import org.esa.s3tbx.slstr.pdu.stitching.ImageSizeHandler;
import org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Tonio Fincke
 */
public class ManifestMerger {

    private Date creationTime;
    private static DefaultMerger defaultMerger;
    private File productDir;
    private long productSize;

    private ImageSize[][] imageSizes;
    private static final String[] discerningAttributesNames = {"ID", "name", "grid", "view", "element", "type", "role"};

    public File createMergedManifest(File[] manifestFiles, Date creationTime, File productDir, long productSize)
            throws IOException, TransformerException, PDUStitchingException, ParserConfigurationException {
        final Document document = mergeManifests(manifestFiles, creationTime, productDir, productSize);
        final File manifestFile = new File(productDir, "xfdumanifest.xml");
        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute("indent-number", 2);
        final Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        final DOMSource domSource = new DOMSource(document);
        final StringWriter stringWriter = new StringWriter();
        final StreamResult streamResult = new StreamResult(stringWriter);
        transformer.transform(domSource, streamResult);
        String docAsString = stringWriter.toString();
        docAsString = docAsString.replace(" standalone=\"no\"", "");
        final FileWriter fileWriter = new FileWriter(manifestFile);
        fileWriter.write(docAsString);
        fileWriter.close();
        return manifestFile;
    }

    private Document mergeManifests(File[] manifestFiles, Date creationTime, File productDir, long productSize) throws IOException, PDUStitchingException, ParserConfigurationException {
        this.creationTime = creationTime;
        this.productDir = productDir;
        this.productSize = productSize;
        List<Node> manifestList = new ArrayList<>();
        imageSizes = new ImageSize[manifestFiles.length][];
        for (int i = 0; i < manifestFiles.length; i++) {
            File manifestFile = manifestFiles[i];
            final Document xmlDocument = createXmlDocument(new FileInputStream(manifestFile));
            imageSizes[i] = ImageSizeHandler.extractImageSizes(xmlDocument);
            manifestList.add(xmlDocument);
        }
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        defaultMerger = new DefaultMerger();
        defaultMerger.mergeNodes(manifestList, document, document);
        return document;
    }

    private static Document createXmlDocument(InputStream inputStream) throws IOException {
        final String msg = "Cannot create document from manifest XML file.";
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException(msg, e);
        }
    }

    private ElementMerger getElementMerger(String elementName) throws PDUStitchingException {
        switch (elementName) {
            case "dataObject":
                return new DataObjectMerger(productDir.getAbsolutePath());
            case "slstr:nadirImageSize":
            case "slstr:obliqueImageSize":
                return new ImageSizesMerger();
            case "sentinel-safe:startTime":
                return new StartTimesMerger();
            case "sentinel-safe:stopTime":
                return new StopTimesMerger();
            case "slstr:classificationSummary":
                return new ClassificationSummaryMerger(imageSizes);
            case "slstr:pixelQualitySummary":
                return new PixelQualitySummaryMerger();
            case "slstr:missingElements":
                return new MissingElementsMerger();
            case "sentinel-safe:footPrint":
                return new FootprintMerger(productDir);
            case "sentinel3:creationTime":
                return new CreationTimeMerger(creationTime);
            case "sentinel3:productName":
                return new ProductNameMerger(productDir.getName());
            case "sentinel3:productSize":
                return new ProductSizeMerger(productSize);
            case "slstr:min":
                return new MinMerger();
            case "slstr:max":
                return new MaxMerger();
            case "sentinel3:dumpInformation":
                return new DumpInformationMerger();
            case "sentinel-safe:orbitReference":
                return new OrbitReferenceMerger();
            case "sentinel3:duration":
                return new DurationMerger();
            case "sentinel3:alongtrackCoordinate":
                return new AlongTrackCoordinateMerger();
        }
        return defaultMerger;
    }


    private class DefaultMerger extends AbstractElementMerger {

        @Override
        public void mergeNodes(List<Node> fromParents, Element toParent, Document toDocument) throws PDUStitchingException {
            mergeNodes(fromParents, (Node) toParent, toDocument);
        }

        public void mergeNodes(List<Node> fromParents, Node toParent, Document toDocument) throws PDUStitchingException {
            NodeList[] childNodeLists = new NodeList[fromParents.size()];
            for (int i = 0; i < childNodeLists.length; i++) {
                childNodeLists[i] = fromParents.get(i).getChildNodes();
            }
            for (int j = 0; j < fromParents.size(); j++) {
                for (int i = 0; i < childNodeLists[j].getLength(); i++) {
                    final Node child = childNodeLists[j].item(i);
                    if (!(child instanceof TextImpl) || !child.getTextContent().contains("\n")) {
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

        //package local for testing
        void mergeChildNodes(List<Node> fromParents, Element toParent, Document toDocument) throws PDUStitchingException {
            final ElementMerger elementMerger = getElementMerger(toParent.getNodeName());
            elementMerger.mergeNodes(fromParents, toParent, toDocument);
        }

        private List<Node> collectChildNodes(Node child, NodeList[] childNodeLists, int indexOfCurrentParent)
                throws PDUStitchingException {
            List<Node> itemNodes = new ArrayList<>();
            itemNodes.add(child);
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
                                            !attributeToBeChecked.getNodeValue().trim().equals(attribute.getNodeValue().trim())) {
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

        private boolean hasIdenticalChild(Node node, Node newNode) {
            for (int i = 0; i < node.getChildNodes().getLength(); i++) {
                final Node nodeToBeChecked = node.getChildNodes().item(i);
                if (nodeToBeChecked.getNodeName().equals(newNode.getNodeName())) {
                    final NamedNodeMap nodeToBeCheckedAttributes = nodeToBeChecked.getAttributes();
                    final NamedNodeMap attributes = newNode.getAttributes();
                    if (nodeToBeCheckedAttributes != null && attributes != null) {
                        boolean atLeastOneDiscerningAttributeIsDifferent = false;
                        for (String name : discerningAttributesNames) {
                            final Node attributeToBeChecked = nodeToBeCheckedAttributes.getNamedItem(name);
                            final Node attribute = attributes.getNamedItem(name);
                            if (attributeToBeChecked != null && attribute != null &&
                                    !attributeToBeChecked.getNodeValue().trim().equals(attribute.getNodeValue().trim())) {
                                atLeastOneDiscerningAttributeIsDifferent = true;
                                break;
                            }
                        }
                        if (!atLeastOneDiscerningAttributeIsDifferent) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            }
            return false;
        }

    }

}
