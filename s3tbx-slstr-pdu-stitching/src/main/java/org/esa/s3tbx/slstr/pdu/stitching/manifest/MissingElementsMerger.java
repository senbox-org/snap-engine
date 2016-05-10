package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.text.NumberFormatter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Tonio Fincke
 */
class MissingElementsMerger extends AbstractElementMerger {

    private final static String SEPARATOR = "_";
    private final static String GLOBAL_INFO_NAME = "slstr:globalInfo";
    private final static String ELEMENT_MISSING_NAME = "slstr:elementMissing";
    private final static String BAND_SET_ELEMENT = "slstr:bandSet";

    @Override
    public void mergeNodes(List<Node> fromParents, Element toParent, Document toDocument) throws PDUStitchingException {
        final String threshold = fromParents.get(0).getAttributes().getNamedItem("threshold").getNodeValue();
        toParent.setAttribute("threshold", threshold);
        final NumberFormatter numberFormatter = getNumberFormatter();
        final Map<String, List<Node>> globalInfoNodesListMap = collectNodes(fromParents, GLOBAL_INFO_NAME);
        Map<String, String> percentages = new HashMap<>();
        for (Map.Entry<String, List<Node>> globalInfoNodesEntry : globalInfoNodesListMap.entrySet()) {
            final String[] key = globalInfoNodesEntry.getKey().split(SEPARATOR);
            final String gridValue = key[0];
            final String viewValue = key[1];
            int value = 0;
            int over = 0;
            final List<Node> globalInfoNodes = globalInfoNodesEntry.getValue();
            for (Node globalInfoNode : globalInfoNodes) {
                final Node valueNode = globalInfoNode.getAttributes().getNamedItem("value");
                final Node overNode = globalInfoNode.getAttributes().getNamedItem("over");
                if (valueNode != null && overNode != null) {
                    value += Integer.parseInt(valueNode.getNodeValue());
                    over += Integer.parseInt(overNode.getNodeValue());
                }
            }
            String percentage;
            try {
                final double percentageAsDouble = ((double) value / over) * 100;
                percentage = numberFormatter.valueToString(percentageAsDouble);
                percentages.put(globalInfoNodesEntry.getKey(), percentage);
            } catch (ParseException e) {
                throw new PDUStitchingException("Could not format number: " + e.getMessage());
            }
            final Element globalInfoElement = toDocument.createElement(GLOBAL_INFO_NAME);
            globalInfoElement.setAttribute("grid", gridValue);
            globalInfoElement.setAttribute("view", viewValue);
            globalInfoElement.setAttribute("value", String.valueOf(value));
            globalInfoElement.setAttribute("over", String.valueOf(over));
            globalInfoElement.setAttribute("percentage", percentage);
            toParent.appendChild(globalInfoElement);
        }
        final Map<String, List<Node>> elementMissingNodesListsMap = collectNodes(fromParents, ELEMENT_MISSING_NAME);
        for (Map.Entry<String, List<Node>> elementMissingNodesEntry : elementMissingNodesListsMap.entrySet()) {
            final String[] key = elementMissingNodesEntry.getKey().split(SEPARATOR);
            final String gridValue = key[0];
            final String viewValue = key[1];
            final List<Node> elementMissingNodesList = elementMissingNodesEntry.getValue();
            Date earliestStartTime = null;
            String earliestStartTimeAsNodeValue = "";
            Date latestStopTime = null;
            String latestStopTimeAsNodeValue = "";
            String bandSet = null;
            for (Node elementMissingNode : elementMissingNodesList) {
                final Node startTimeNode = elementMissingNode.getAttributes().getNamedItem("startTime");
                if (startTimeNode != null) {
                    final String startTimeNodeValue = startTimeNode.getNodeValue();
                    final Date startTime = parseDate(startTimeNodeValue);
                    if (earliestStartTime == null || startTime.before(earliestStartTime)) {
                        earliestStartTime = startTime;
                        earliestStartTimeAsNodeValue = startTimeNodeValue;
                    }
                }
                final Node stopTimeNode = elementMissingNode.getAttributes().getNamedItem("stopTime");
                if (stopTimeNode != null) {
                    final String stopTimeNodeValue = stopTimeNode.getNodeValue();
                    final Date stopTime = parseDate(stopTimeNodeValue);
                    if (latestStopTime == null || stopTime.after(latestStopTime)) {
                        latestStopTime = stopTime;
                        latestStopTimeAsNodeValue = stopTimeNodeValue;
                    }
                }
                final NodeList elementMissingNodeChildNodes = elementMissingNode.getChildNodes();
                for (int i = 0; i < elementMissingNodeChildNodes.getLength(); i++) {
                    final Node childNode = elementMissingNodeChildNodes.item(i);
                    if (childNode.getNodeName().equals(BAND_SET_ELEMENT)) {
                        final String bandSetFromNode = childNode.getTextContent();
                        if (bandSet == null) {
                            bandSet = bandSetFromNode;
                        } else {
                            if (!bandSet.equals(bandSetFromNode)) {
                                throw new PDUStitchingException("Error when stitching missing elements entry in manifest file: Could not determine bandset");
                            }
                        }
                    }
                }
            }
            final Element elementMissingElement = toDocument.createElement(ELEMENT_MISSING_NAME);
            elementMissingElement.setAttribute("grid", gridValue);
            elementMissingElement.setAttribute("view", viewValue);
            if (earliestStartTime != null) {
                elementMissingElement.setAttribute("startTime", earliestStartTimeAsNodeValue);
            }
            if (latestStopTime != null) {
                elementMissingElement.setAttribute("stopTime", latestStopTimeAsNodeValue);
            }
            elementMissingElement.setAttribute("percentage", percentages.get(elementMissingNodesEntry.getKey()));
            final Element bandSetElement = toDocument.createElement(BAND_SET_ELEMENT);
            addTextToNode(bandSetElement, bandSet, toDocument);
            elementMissingElement.appendChild(bandSetElement);
            toParent.appendChild(elementMissingElement);
        }
    }

    private NumberFormatter getNumberFormatter() {
        final DecimalFormat format = new DecimalFormat("0.000000");
        format.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        return new NumberFormatter(format);
    }

    private Map<String, List<Node>> collectNodes(List<Node> fromParents, String wantedNodeName) {
        Map<String, List<Node>> nodesMap = new LinkedHashMap<>();
        for (final Node parent : fromParents) {
            final NodeList childNodes = parent.getChildNodes();
            for (int j = 0; j < childNodes.getLength(); j++) {
                final Node globalInfoNode = childNodes.item(j);
                final String nodeName = globalInfoNode.getNodeName();
                if (nodeName.equals(wantedNodeName)) {
                    final Node gridAttribute = globalInfoNode.getAttributes().getNamedItem("grid");
                    final Node viewAttribute = globalInfoNode.getAttributes().getNamedItem("view");
                    if (gridAttribute != null && viewAttribute != null) {
                        String identifier = gridAttribute.getNodeValue() + SEPARATOR + viewAttribute.getNodeValue();
                        if (nodesMap.containsKey(identifier)) {
                            nodesMap.get(identifier).add(globalInfoNode);
                        } else {
                            final ArrayList<Node> nodeList = new ArrayList<>();
                            nodeList.add(globalInfoNode);
                            nodesMap.put(identifier, nodeList);
                        }
                    }
                }
            }
        }
        return nodesMap;
    }

}
