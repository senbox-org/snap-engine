package org.esa.beam.dataio.util;

/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

/**
 * Provides helper methods which simplify the usage of {@link javax.xml.xpath.XPath}
 *
 * @author Marco Peters
 */
public class XPathHelper {

    private XPath xPath;

    public XPathHelper(XPath xPath) {
        this.xPath = xPath;
    }

    public String getString(String pathExpr, Node node) {
        try {
            return xPath.evaluate(pathExpr, node);
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException("XPath '" + pathExpr + "' invalid.", e);
        }
    }

    public NodeList getNodeList(String pathExpr, Node node) {
        try {
            return (NodeList) xPath.evaluate(pathExpr, node, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException("XPath '" + pathExpr + "' invalid.", e);
        }
    }

    public Node getNode(String pathExpr, Node node) {
        try {
            return (Node) xPath.evaluate(pathExpr, node, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException("XPpath '" + pathExpr + "' invalid.", e);
        }

    }
}
