package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.text.NumberFormatter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Tonio Fincke
 */
public class PixelQualitySummaryMerger extends AbstractElementMerger {

    @Override
    public void mergeNodes(List<Node> fromParents, Element toParent, Document toDocument) throws PDUStitchingException {
        final String grid = fromParents.get(0).getAttributes().getNamedItem("grid").getNodeValue();
        toParent.setAttribute("grid", grid);
        final Collection<List<Node>> summaryNodesLists = collectSummaryNodes(fromParents);
        final NumberFormatter numberFormatter = getNumberFormatter();
        for (List<Node> summaryNodesList : summaryNodesLists) {
            if (summaryNodesList.size() != fromParents.size()) {
                //todo throw exception? - tf 20160216
                continue;
            }
            int totalClassifiedPixels = 0;
            int totalPixels = 0;
            for (Node summaryNode : summaryNodesList) {
                final NamedNodeMap summaryNodeAttributes = summaryNode.getAttributes();
                int classifiedPixels = Integer.parseInt(summaryNodeAttributes.getNamedItem("value").getNodeValue());
                final double percentage = Double.parseDouble(summaryNodeAttributes.getNamedItem("percentage").getNodeValue());
                totalClassifiedPixels += classifiedPixels;
                totalPixels += classifiedPixels / percentage * 100;
            }
            final Element summaryElement = toDocument.createElement(summaryNodesList.get(0).getNodeName());
            summaryElement.setAttribute("value", "" + totalClassifiedPixels);
            if (totalPixels > 0) {
                Double percentage = (totalClassifiedPixels / (double) totalPixels) * 100;
                try {
                    summaryElement.setAttribute("percentage", numberFormatter.valueToString(percentage));
                } catch (ParseException e) {
                    throw new PDUStitchingException("Could not format number: " + e.getMessage());
                }
            } else {
                summaryElement.setAttribute("percentage", "0.000000");
            }
            toParent.appendChild(summaryElement);
        }
    }

    private NumberFormatter getNumberFormatter() {
        final DecimalFormat format = new DecimalFormat("0.000000");
        format.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        return new NumberFormatter(format);
    }

    private Collection<List<Node>> collectSummaryNodes(List<Node> fromParents) {
        Map<String, List<Node>> summaryNodesMap = new LinkedHashMap<>();
        for (final Node parent : fromParents) {
            final NodeList childNodes = parent.getChildNodes();
            for (int j = 0; j < childNodes.getLength(); j++) {
                final Node summaryNode = childNodes.item(j);
                final String nodeName = summaryNode.getNodeName();
                if (nodeName.startsWith("slstr")) {
                    if (summaryNodesMap.containsKey(nodeName)) {
                        summaryNodesMap.get(nodeName).add(summaryNode);
                    } else {
                        final ArrayList<Node> nodeList = new ArrayList<>();
                        nodeList.add(summaryNode);
                        summaryNodesMap.put(nodeName, nodeList);
                    }
                }
            }
        }
        return summaryNodesMap.values();
    }

}
