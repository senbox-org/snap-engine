package org.esa.snap.core.metadata;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for XML metadata inspectors.
 *
 * @author Denisa Stefanescu
 */
@Deprecated
public abstract class XmlMetadataInspector implements MetadataInspector {

    private Document xmlDocument;
    private XPath xPath;
    protected Logger logger = Logger.getLogger(getClass().getName());

    public XmlMetadataInspector() { }

    protected void readDocument(Path documentPath) throws ParserConfigurationException, IOException, SAXException, XMLStreamException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        try (InputStream inputStream = Files.newInputStream(documentPath)) {
            this.xmlDocument = builder.parse(inputStream);
        }
        this.xPath = XPathFactory.newInstance().newXPath();
        try (InputStream inputStream = Files.newInputStream(documentPath)) {
            XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(inputStream);
            while (reader.hasNext()) {
                XMLEvent xmlEvent = reader.nextEvent();
                if (xmlEvent.isStartElement()) {
                    this.xPath.setNamespaceContext(((StartElement) xmlEvent).getNamespaceContext());
                    break;
                }
            }
        }

    }

    protected String getValue(String xPathExpression) {
        String value = null;
        if (this.xPath != null) {
            try {
                value = (String) this.xPath.compile(xPathExpression).evaluate(this.xmlDocument, XPathConstants.STRING);
            } catch (XPathExpressionException e) {
                logger.log(Level.WARNING,String.format("Failed to get value '%s'", xPathExpression), e);
            }
        }
        return value;
    }

    protected Set<String> getValues(String xPathExpression) {
        Set<String> values = null;
        if (this.xPath != null) {
            try {
                NodeList nodes = (NodeList) this.xPath.compile(xPathExpression).evaluate(this.xmlDocument, XPathConstants.NODESET);
                if (nodes != null) {
                    values = new HashSet<>();
                    final int length = nodes.getLength();
                    for (int i = 0; i < length; i++) {
                        values.add(nodes.item(i).getNodeValue());
                    }
                }
            } catch (XPathExpressionException e) {
                logger.log(Level.WARNING,String.format("Failed to get values '%s'", xPathExpression), e);
            }
        }
        return values;
    }

    protected boolean existElement(String xPathExpression){
        return xmlDocument.getElementsByTagName(xPathExpression).getLength()>0;
    }

}
