package org.esa.s3tbx.slstr.pdu.stitching;

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
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tonio Fincke
 */
class ManifestMerger {

    static Document mergeManifests(File[] manifestFiles) throws IOException, ParserConfigurationException {
//        XPathHelper xPathHelper = new XPathHelper(XPathFactory.newInstance().newXPath());
//        Document[] manifestDocuments = new Document[manifestFiles.length];
        List<Node> manifestList = new ArrayList<>();
        for (File manifestFile : manifestFiles) {
//            manifestDocuments[i] = createXmlDocument(new FileInputStream(manifestFiles[i]));
            manifestList.add(createXmlDocument(new FileInputStream(manifestFile)));
        }
        final DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document manifest = documentBuilder.newDocument();
        manifest.setXmlStandalone(true);
//        final Text newLineNode = manifest.createTextNode("\n");
//        manifest.appendChild(newLineNode);
//        copyNode(manifestDocuments[0], manifest, manifest);
        copyNode(manifestList, manifest, manifest);
//        xPathHelper.getNode()
        return manifest;
    }

    private static boolean hasChildOfName(Node node, Node newNode) {
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            final Node nodeToBeChecked = node.getChildNodes().item(i);
            if (nodeToBeChecked.getNodeName().equals(newNode.getNodeName())) {
                final NamedNodeMap nodeToBeCheckedAttributes = nodeToBeChecked.getAttributes();
                final NamedNodeMap attributes = newNode.getAttributes();
                if (nodeToBeCheckedAttributes != null && attributes != null) {
                    final Node idAttributeToBeChecked = nodeToBeCheckedAttributes.getNamedItem("ID");
                    final Node nameAttributeToBeChecked = nodeToBeCheckedAttributes.getNamedItem("name");
                    final Node idAttribute = attributes.getNamedItem("ID");
                    final Node nameAttribute = attributes.getNamedItem("name");
                    if (idAttributeToBeChecked != null && idAttribute != null) {
                        return idAttributeToBeChecked.getNodeValue().equals(idAttribute.getNodeValue());
                    } else if (nameAttributeToBeChecked != null && nameAttribute != null) {
                        return nameAttributeToBeChecked.getNodeValue().equals(nameAttribute.getNodeValue());
                    }
                    {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    private static void copyNode(List<Node> fromParents, Node toParent, Document toDocument) {
        NodeList[] childNodeLists = new NodeList[fromParents.size()];
        for (int i = 0; i < childNodeLists.length; i++) {
            childNodeLists[i] = fromParents.get(i).getChildNodes();
        }
        for (int j = 0; j < fromParents.size(); j++) {
            for (int i = 0; i < childNodeLists[j].getLength(); i++) {
                final Node item = childNodeLists[j].item(i);
                if (item instanceof TextImpl && item.getTextContent().contains("\n")) {
                    final Node lastChild = toParent.getLastChild();
                    if (!(lastChild instanceof TextImpl)) {
                        final String textContent = item.getTextContent();
                        final Text textNode = toDocument.createTextNode(textContent);
                        toParent.appendChild(textNode);
                    } else if (!lastChild.getTextContent().contains("\n")) {
                        final String textContent = item.getTextContent();
                        final Text textNode = toDocument.createTextNode(textContent);
                        toParent.appendChild(textNode);
                    }
                } else {
                    if (!hasChildOfName(toParent, item)) {
                        List<Node> itemNodes = new ArrayList<>();
                        itemNodes.add(item);
                        final String nodeValue = item.getNodeValue();
                        if (i < childNodeLists.length - 1) {
                            for (int k = j + 1; k < childNodeLists.length; k++) {
                                for (int l = 0; l < childNodeLists[k].getLength(); l++) {
                                    if (childNodeLists[k].item(l).getNodeName().equals(item.getNodeName())) {
                                        final String otherNodeValue = childNodeLists[k].item(l).getNodeValue();
                                        if ((otherNodeValue != null && nodeValue == null) ||
                                                (otherNodeValue == null && nodeValue != null) ||
                                                (otherNodeValue != null && !otherNodeValue.trim().equals(nodeValue.trim()))) {
                                            //todo throw Exception when no problems excepted
                                            System.out.println("Different values for node " + item.getParentNode().getNodeName() + ": "
                                                                       + otherNodeValue + ", " + nodeValue);
                                        } else {
                                            itemNodes.add(childNodeLists[k].item(l));
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                        if (item instanceof DeferredTextImpl) {
                            final String textContent = item.getTextContent();
                            final Text textNode = toDocument.createTextNode(textContent);
                            toParent.appendChild(textNode);
                        } else {
                            final Element manifestElement = toDocument.createElement(item.getNodeName());
                            manifestElement.setNodeValue(nodeValue);
                            for (int n = 0; n < itemNodes.size(); n++) {
                                final Node currentItem = itemNodes.get(n);
                                final NamedNodeMap attributes = currentItem.getAttributes();

                                if (attributes != null) {
                                    for (int k = 0; k < attributes.getLength(); k++) {
                                        final Node attribute = attributes.item(k);
                                        if (!manifestElement.hasAttribute(attribute.getNodeName())) {
                                            if (n < itemNodes.size() - 1) {
                                                for (int m = n + 1; m < itemNodes.size(); m++) {
                                                    final NamedNodeMap otherItemAttributes = itemNodes.get(m).getAttributes();
                                                    final Node otherAttribute = otherItemAttributes.getNamedItem(attribute.getNodeName());
                                                    if (otherAttribute != null && !otherAttribute.getNodeValue().equals(attribute.getNodeValue())) {
                                                        //todo throw Exception when no problems excepted
                                                        System.out.println("Different values for attribute " + attribute.getNodeName() +
                                                                                   " of node " + currentItem.getNodeName());
                                                    }
                                                }
                                            }

                                            manifestElement.setAttribute(attribute.getNodeName(), attribute.getNodeValue().trim());
                                        }
                                    }
                                }
                            }
                            toParent.appendChild(manifestElement);
                            copyNode(itemNodes, manifestElement, toDocument);
                        }
                    }
                }
            }
        }
    }

//    private static boolean equalsNodesFromOtherLists(Node node, NodeList[] childNodeLists, int indexOfCurrentList) {
//        for (int i = indexOfCurrentList; i < childNodeLists.length; ++i) {
//            for (int j = 0; j < childNodeLists[i].getLength(); j++) {
//                final Node toCompare = childNodeLists[i].item(j);
//                if (toCompare.getNodeName().equals(node.getNodeName())) {
//                    if (!areEqual(toCompare, node)) {
//                        return false;
//                    }
//                }
//            }
//        }
//        return true;
//    }

//    private static boolean areEqual(Node node1, Node node2) {
//        if (!node1.getNodeValue().equals(node2.getNodeValue())) {
//            return false;
//        }
//        final NamedNodeMap node1Attributes = node1.getAttributes();
//        final NamedNodeMap node2Attributes = node2.getAttributes();
//
//    }

    private static void copyNode(Node fromParent, Node toParent, Document toDocument) {
        for (int i = 0; i < fromParent.getChildNodes().getLength(); i++) {
            final Node item = fromParent.getChildNodes().item(i);
            if (item instanceof DeferredTextImpl) {
                final Text textNode = toDocument.createTextNode(item.getTextContent());
                toParent.appendChild(textNode);
            } else {
                final Element manifestElement = toDocument.createElement(item.getNodeName());
                manifestElement.setNodeValue(item.getNodeValue());
                final NamedNodeMap attributes = item.getAttributes();
                for (int j = 0; j < attributes.getLength(); j++) {
                    final Node attribute = attributes.item(j);
                    manifestElement.setAttribute(attribute.getNodeName(), attribute.getNodeValue());
                }
                toParent.appendChild(manifestElement);
                copyNode(item, manifestElement, toDocument);
            }
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
