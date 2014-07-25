/*
 * Copyright (c) 2012. Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA
 */

package org.esa.beam.dataio.s3;

import org.esa.beam.dataio.util.XPathHelper;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class encapsulating the manifest file of an Olci Level 1b product.
 *
 * @author Marco Peters
 * @since 1.0
 */
class EarthExplorerManifest implements Manifest {

    private final Document doc;
    private final XPathHelper xPathHelper;

    static Manifest createManifest(Document manifestDocument) {
        return new EarthExplorerManifest(manifestDocument);
    }

    /**
     * Creates an instance of this class by using the given W3C document.
     *
     * @param manifestDocument the W3C manifest document.
     */
    private EarthExplorerManifest(Document manifestDocument) {
        doc = manifestDocument;
        XPath xPath = XPathFactory.newInstance().newXPath();
        xPathHelper = new XPathHelper(xPath);
    }

    @Override
    public final String getDescription() {
        return xPathHelper.getString("//File_Description", doc);
    }

    @Override
    public final ProductData.UTC getStartTime() {
        final String utcString = xPathHelper.getString("//Start_Time", doc);
        try {
            return ProductData.UTC.parse(utcString, "'UTC='yyyy-MM-dd'T'HH:mm:ss");
        } catch (ParseException ignored) {
            return null;
        }
    }

    @Override
    public final ProductData.UTC getStopTime() {
        final String utcString = xPathHelper.getString("//Stop_Time", doc);
        try {
            return ProductData.UTC.parse(utcString, "'UTC='yyyy-MM-dd'T'HH:mm:ss");
        } catch (ParseException ignored) {
            return null;
        }
    }

    @Override
    public final List<String> getFileNames(String schema) {
        final String xPath = String.format("//Data_Object_Descriptor[Type='%s']", schema);
        final NodeList nodeList = xPathHelper.getNodeList(xPath, doc);
        final List<String> fileNames = new ArrayList<String>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node dataObjectDescriptorNode = nodeList.item(i);
            fileNames.add(xPathHelper.getString("Filename", dataObjectDescriptorNode));
        }
        return fileNames;
    }

    @Override
    public List<String> getFileNames(String[] excluded) {
        return null;
    }

    @Override
    public MetadataElement getMetadata() {
        final MetadataElement manifestElement = new MetadataElement("Manifest");
        final Node node = xPathHelper.getNode("//Earth_Explorer_Header", doc);

        manifestElement.addElement(convertNodeToMetadataElement(node, new MetadataElement(node.getNodeName())));

        return manifestElement;
    }

    private static MetadataElement convertNodeToMetadataElement(Node sourceNode, MetadataElement targetElement) {
        final NodeList childNodes = sourceNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            final Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (hasElementChildNodes(node)) {
                    MetadataElement element = new MetadataElement(node.getNodeName());
                    convertNodeToMetadataElement(node, element);
                    targetElement.addElement(element);
                } else {
                    final String nodeValue = node.getTextContent();
                    final ProductData textContent = ProductData.createInstance(nodeValue);
                    final MetadataAttribute attribute = new MetadataAttribute(node.getNodeName(), textContent, true);
                    targetElement.addAttribute(attribute);
                }
            }
        }
        return targetElement;
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
}
