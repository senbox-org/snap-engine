package org.esa.s3tbx.dataio.s3;

import org.esa.s3tbx.dataio.util.XPathHelper;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.ArrayUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathFactory;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tonio Fincke
 */
public class XfduManifest implements Manifest {

    protected static final String MANIFEST_FILE_NAME = "xfdumanifest.xml";
    private final Document doc;
    private final XPathHelper xPathHelper;
    private MetadataElement manifestElement;

    public static Manifest createManifest(Document manifestDocument) {
        return new XfduManifest(manifestDocument);
    }

    private XfduManifest(Document manifestDocument) {
        doc = manifestDocument;
        xPathHelper = new XPathHelper(XPathFactory.newInstance().newXPath());
    }

    @Override
    public String getProductName() {
        final Node gpi = xPathHelper.getNode("/XFDU/metadataSection/metadataObject[@ID='generalProductInformation']", doc);
        return  xPathHelper.getString("//metadataWrap/xmlData/generalProductInformation/productName", gpi);
    }

    @Override
    public String getProductType() {
        final Node gpi = xPathHelper.getNode("/XFDU/metadataSection/metadataObject[@ID='generalProductInformation']", doc);
        String typeString = xPathHelper.getString("//metadataWrap/xmlData/generalProductInformation/productType", gpi);
        return removeUnderbarsAtEnd(typeString);
    }

    @Override
    public String getDescription() {
        return xPathHelper.getString("/XFDU/informationPackageMap/contentUnit/@textInfo", doc);
    }

    @Override
    public ProductData.UTC getStartTime() {
        return getTime("startTime");
    }

    @Override
    public ProductData.UTC getStopTime() {
        return getTime("stopTime");
    }

    @Override
    public List<String> getFileNames(final String schema) {
        final List<String> fileNameList = new ArrayList<>();

        getFileNames("dataObjectSection/dataObject", fileNameList);
        getFileNames("metadataSection/metadataObject", fileNameList);

        return fileNameList;
    }

    @Override
    public List<String> getFileNames(String[] excluded) {
        final ArrayList<String> fileNameList = new ArrayList<>();
        final NodeList nodeList = xPathHelper.getNodeList("/XFDU/dataObjectSection/dataObject", doc);
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node item = nodeList.item(i);
            final NamedNodeMap attributes = item.getAttributes();
            if (attributes != null) {
                Attr attr = (Attr) (attributes.getNamedItem("ID"));
                String id = (attr == null) ? "" : attr.getValue();

                if (!ArrayUtils.isMemberOf(id, excluded)) {
                    final String fileName = xPathHelper.getString("./byteStream/fileLocation/@href", item);
                    if (!fileNameList.contains(fileName)) {
                        fileNameList.add(fileName);
                    }
                }
            }
        }

        return fileNameList;
    }

    @Override
    public MetadataElement getMetadata() {
        if (manifestElement == null) {
            manifestElement = new MetadataElement("Manifest");
            Node node = xPathHelper.getNode("//metadataSection", doc);
            manifestElement.addElement(convertNodeToMetadataElement(node, new MetadataElement(node.getNodeName())));
        }
        return manifestElement;
    }

    private static String removeNamespace(String withNamespace) {
        if (!withNamespace.contains(":")) {
            return withNamespace;
        }
        return withNamespace.split(":")[1];
    }

    private MetadataElement convertNodeToMetadataElement(Node rootNode, MetadataElement rootMetadata) {
        NodeList childNodes = rootNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (node.getNodeName().contains(":")) {
                    String nodeName = removeNamespace(node.getNodeName());
                    if (hasElementChildNodes(node)) {
                        MetadataElement element = new MetadataElement(nodeName);
                        rootMetadata.addElement(element);
                        addAttributesToElement(node, element);
                        convertNodeToMetadataElement(node, element);
                    } else if (hasAttributeChildNodes(node)) {
                        MetadataElement element = new MetadataElement(nodeName);
                        rootMetadata.addElement(element);
                        final String textContent = node.getTextContent().trim();
                        if (!textContent.equals("")) {
                            element.setAttributeString(nodeName, textContent);
                        }
                        addAttributesToElement(node, element);
                    } else {
                        String nodevalue = node.getTextContent().trim();
                        ProductData textContent = ProductData.createInstance(nodevalue);
                        rootMetadata.addAttribute(new MetadataAttribute(nodeName, textContent, true));
                    }
                } else {
                    convertNodeToMetadataElement(node, rootMetadata);
                }
            }
        }
        return rootMetadata;
    }

    private void addAttributesToElement(Node node, MetadataElement element) {
        final NamedNodeMap attributes = node.getAttributes();
        for (int j = 0; j < attributes.getLength(); j++) {
            final Node nodeAttribute = attributes.item(j);
            String nodeAttributeValue = nodeAttribute.getTextContent();
            ProductData attributeTextContent = ProductData.createInstance(nodeAttributeValue);
            String attributeNodeName = removeNamespace(nodeAttribute.getNodeName());
            final MetadataAttribute attribute = new MetadataAttribute(attributeNodeName,
                                                                      attributeTextContent, true);
            element.addAttribute(attribute);
        }
    }

    private static boolean hasElementChildNodes(Node rootNode) {
        NodeList childNodes = rootNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAttributeChildNodes(Node rootNode) {
        final NamedNodeMap attributeNodes = rootNode.getAttributes();
        for (int i = 0; i < attributeNodes.getLength(); i++) {
            Node node = attributeNodes.item(i);
            if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
                return true;
            }
        }
        return false;
    }

    private List<String> getFileNames(String objectPath, List<String> fileNameList) {
        final NodeList nodeList = xPathHelper.getNodeList(
                "/XFDU/" + objectPath, doc);
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node item = nodeList.item(i);
            final String fileName = xPathHelper.getString("./byteStream/fileLocation/@href", item);
            if (!fileNameList.contains(fileName)) {
                fileNameList.add(fileName);
            }
        }

        return fileNameList;
    }

    private ProductData.UTC getTime(final String name) {
        final Node period = xPathHelper.getNode("/XFDU/metadataSection/metadataObject[@ID='acquisitionPeriod']", doc);
        String time = xPathHelper.getString("//metadataWrap/xmlData/acquisitionPeriod/" + name, period);
        try {
            if (!Character.isDigit(time.charAt(time.length() - 1))) {
                time = time.substring(0, time.length() - 1);
            }
            return ProductData.UTC.parse(time, "yyyy-MM-dd'T'HH:mm:ss");
        } catch (ParseException ignored) {
            return null;
        }
    }


    private String removeUnderbarsAtEnd(String typeString) {
        char[] chars = typeString.toCharArray();
        int endIndex = chars.length;
        for (int i = chars.length - 1; i >= 0; i--) {
            if (chars[i] != '_') {
                endIndex = i;
                break;
            }
        }
        return typeString.substring(0, endIndex+1);
    }

}
