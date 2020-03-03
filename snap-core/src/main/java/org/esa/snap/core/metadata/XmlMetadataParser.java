/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2014-2015 CS-Romania (office@c-s.ro)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.metadata;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * SAX parser for XML metadata. This is because DOM parsing would consume more time and resources
 * for large metadata files.
 *
 * @author Cosmin Cara
 */
public class XmlMetadataParser<T extends GenericXmlMetadata> {

    protected Class fileClass;
    protected String[] schemaLocations;
    protected String schemaBasePath = null;

    /**
     * Tries to infer the type of the element, based on the available XSD schema definition.
     * If no schema definition exist, the type will always be <code>ProductData.ASCII</code>.
     *
     * @param elementName   The name of the XML element.
     * @param value         The value of the XML element.
     * @return      An instance of <code>ProductData</code> wrapping the element value.
     */
    protected ProductData inferType(String elementName, String value) {
        return ProductData.ASCII.createInstance(value);
    }

    /**
     * Constructs an instance of <code>XmlMetadataParser</code> for the given metadata class.
     *
     * @param metadataClass    The class of metadata (it should be derived from <code>XmlMetadata</code>).
     */
    public XmlMetadataParser(Class metadataClass) {
        this.fileClass = metadataClass;
    }

    /**
     * Tries to parse the given <code>InputStream</code> (which may be a string or a stream over a file).
     *
     * @param inputStream   The input stream
     * @return  If successful, it returns an instance of a class extending <code>XmlMetadata</code>.
     * @throws ParserConfigurationException     Exception is thrown by the underlying SAX mechanism.
     * @throws SAXException                     Exception is thrown if the XML is not well formed.
     * @throws IOException                      Exception is thrown if there is a problem reading the input stream.
     */
    public T parse(InputStream inputStream) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        if (schemaLocations != null && shouldValidateSchema()) {
            SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            ClassLoader classLoader = this.getClass().getClassLoader();
            if(schemaBasePath != null) {
                schemaFactory.setResourceResolver(new ResourceResolver(schemaBasePath, classLoader));
            }
            List<StreamSource> streamSourceList = new Vector<>();
            for (String schemaLocation : schemaLocations) {
                InputStream is = classLoader.getResourceAsStream(schemaLocation);
                StreamSource streamSource = new StreamSource(is);
                streamSourceList.add(streamSource);
            }
            StreamSource sources[] = new StreamSource[streamSourceList.size()];
            Schema schema = schemaFactory.newSchema(streamSourceList.toArray(sources));
            factory.setSchema(schema);
            factory.setNamespaceAware(true);
            factory.setValidating(true);
        }
        SAXParser parser = factory.newSAXParser();
        MetadataHandler handler = new MetadataHandler();
        parser.parse(inputStream, handler);

        return handler.getResult();
    }

    public MetadataElement parse(Path file, Set<String> excludes) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        if (schemaLocations != null && shouldValidateSchema()) {
            SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            ClassLoader classLoader = this.getClass().getClassLoader();
            if(schemaBasePath != null) {
                schemaFactory.setResourceResolver(new ResourceResolver(schemaBasePath, classLoader));
            }
            List<StreamSource> streamSourceList = new Vector<>();
            for (String schemaLocation : schemaLocations) {
                InputStream is = classLoader.getResourceAsStream(schemaLocation);
                StreamSource streamSource = new StreamSource(is);
                streamSourceList.add(streamSource);
            }
            StreamSource sources[] = new StreamSource[streamSourceList.size()];
            Schema schema = schemaFactory.newSchema(streamSourceList.toArray(sources));
            factory.setSchema(schema);
            factory.setNamespaceAware(true);
            factory.setValidating(true);
        }
        SAXParser parser = factory.newSAXParser();
        SimpleMetadataHandler handler = new SimpleMetadataHandler(excludes);
        try (InputStream inputStream = Files.newInputStream(file)) {
            parser.parse(inputStream, handler);
        }
        return handler.getResult();
    }

    /**
     * Indicates if the XSD validation should be performed.
     * Override this in derived classes to enable schema validation.
     * @return  The default implementation always returns <code>false</code>.
     *          In a derived class, <code>true</code> would mean that the XML
     *          schema validation should be performed.
     */
    protected boolean shouldValidateSchema() {
        return false;
    }

    /**
     * Sets the location of the schema base path that should be used for XSD
     * schema validation.
     *
     * @param schemaBasePath   The schema base path.
     */
    protected void setSchemaBasePath(String schemaBasePath) {
        this.schemaBasePath = schemaBasePath;
    }


    /**
     * Sets the location(s) of the XSD schema(s) that should be used for XSD
     * schema validation.
     *
     * @param schemaLocations   An array of schema locations.
     */
    protected void setSchemaLocations(String[] schemaLocations) {
        this.schemaLocations = schemaLocations;
    }

    /**
     * Actual document handler implementation
     */
    protected class MetadataHandler extends DefaultHandler {
        private T result;
        private StringBuilder buffer = new StringBuilder(512);
        private String currentPath;
        private Stack<MetadataElement> elementStack;
        private Logger systemLogger;

        public T getResult() {
            return result;
        }

        @Override
        public void startDocument() throws SAXException {
            systemLogger = Logger.getLogger(XmlMetadataParser.class.getName());
            elementStack = new Stack<>();
            try {
                @SuppressWarnings("unchecked") Constructor<T> ctor = fileClass.getConstructor(String.class);
                result = ctor.newInstance("Metadata");
                currentPath = "/";
            } catch (Exception e1) {
                try {
                    @SuppressWarnings("unchecked") Constructor<T> ctor = fileClass.getDeclaredConstructor(String.class);
                    result = ctor.newInstance("Metadata");
                    currentPath = "/";
                } catch (Exception e) {
                    systemLogger.severe(e.getMessage());
                }
            }
        }

        @Override
        public void endDocument() throws SAXException {
            super.endDocument();
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            //characters() may be called several times for chunks of one element by a SAX parser
            buffer.append(ch, start, length);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            // strip any namespace prefix
            if (qName.indexOf(":") > 0) {
                qName = qName.substring(qName.indexOf(":") + 1);
            }
            MetadataElement element = new MetadataElement(qName);
            buffer.setLength(0);
            currentPath += qName + "/";
            if (attributes != null) {
                for (int i = 0; i < attributes.getLength(); i++) {
                    MetadataAttribute attribute = new MetadataAttribute(attributes.getQName(i), ProductData.ASCII.createInstance(attributes.getValue(i)), false);
                    element.addAttribute(attribute);
                    result.indexAttribute(currentPath, attribute);
                }
            }
            elementStack.push(element);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            MetadataElement closingElement =  elementStack.pop();
            if (!elementStack.empty())
            {
                //if (buffer != null && !buffer.isEmpty() && !buffer.startsWith("\n")) {
                if (buffer.length() > 0 && buffer.charAt(0) != '\n') { // TODO: Unclear what the purpose of the second condition is. Should be dropped.
                    MetadataAttribute attribute = new MetadataAttribute(closingElement.getName(), inferType(qName, buffer.toString()), false);
                    elementStack.peek().addAttribute(attribute);
                    currentPath = removeClosingElement(currentPath,closingElement.getName());
                    result.indexAttribute(currentPath, attribute);
                    buffer.setLength(0);
                } else {
                    elementStack.peek().addElement(closingElement);
                    currentPath = removeClosingElement(currentPath,closingElement.getName());
                }
            } else {
                XmlMetadata.CopyChildElements(closingElement, result.getRootElement());
                result.getRootElement().setName("Metadata");
                currentPath = removeClosingElement(currentPath,closingElement.getName());
            }
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            String error = e.getMessage();
            if (!(error.contains("Dimap_Document") || error.contains("no grammar found")))
                systemLogger.warning(e.getMessage());
        }

        private String removeClosingElement (String path, String closingElementName) {
            int lastIndex = path.lastIndexOf(closingElementName +"/");
            if(lastIndex == -1) {
                return path;
            }
            return path.substring(0, lastIndex);
        }
    }

    protected class SimpleMetadataHandler extends DefaultHandler {
        private MetadataElement rootElement;
        private Set<String> excludedElements;
        private StringBuilder buffer = new StringBuilder(512);
        private Stack<MetadataElement> elementStack;
        private Logger systemLogger;
        private String unit;

        public SimpleMetadataHandler(Set<String> excludes) {
            super();
            this.excludedElements = excludes;
        }

        public MetadataElement getResult() {
            return rootElement;
        }

        @Override
        public void startDocument() throws SAXException {
            systemLogger = Logger.getLogger(XmlMetadataParser.class.getName());
            elementStack = new Stack<>();
            rootElement = new MetadataElement("Metadata");
        }

        @Override
        public void endDocument() throws SAXException {
            super.endDocument();
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            //characters() may be called several times for chunks of one element by a SAX parser
            buffer.append(ch, start, length);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            // strip any namespace prefix
            if (qName.indexOf(":") > 0) {
                qName = qName.substring(qName.indexOf(":") + 1);
            }
            if (this.excludedElements == null || !this.excludedElements.contains(qName)) {
                MetadataElement currentElement = new MetadataElement(qName);
                buffer.setLength(0);
                for (int i = 0; i < attributes.getLength(); i++) {
                    MetadataAttribute attribute = new MetadataAttribute(attributes.getQName(i).toUpperCase(), ProductData.ASCII.createInstance(attributes.getValue(i)), false);
                    currentElement.addAttribute(attribute);
                    if ("unit".equals(attributes.getQName(i))) {
                        unit = attributes.getValue(i);
                    }
                }
                elementStack.push(currentElement);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (this.excludedElements == null || !this.excludedElements.contains(qName)) {
                MetadataElement closingElement = elementStack.pop();
                if (!elementStack.empty()) {
                    //if (buffer != null && !buffer.isEmpty() && !buffer.startsWith("\n")) {
                    if (buffer.length() > 0 && buffer.charAt(0) != '\n') {  // TODO see above
                        MetadataAttribute attribute = new MetadataAttribute(closingElement.getName().toUpperCase(), inferType(qName, buffer.toString()), false);
                        if (unit != null) {
                            attribute.setUnit(unit);
                        }
                        elementStack.peek().addAttribute(attribute);
                        buffer.setLength(0);
                    } else {
                        elementStack.peek().addElement(closingElement);
                    }
                } else {
                    XmlMetadata.CopyChildElements(closingElement, rootElement);
                //result.getRootElement().setName("Metadata");
                //currentPath = currentPath.replace(closingElement.getName() + "/", "");
                }
            }
            unit = null;
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            String error = e.getMessage();
            if (!error.contains("no grammar found"))
                systemLogger.warning(e.getMessage());
        }
    }
}
