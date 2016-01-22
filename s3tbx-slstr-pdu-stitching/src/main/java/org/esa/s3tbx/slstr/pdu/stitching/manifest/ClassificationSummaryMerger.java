package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import org.esa.s3tbx.slstr.pdu.stitching.ImageSize;
import org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Tonio Fincke
 */
class ClassificationSummaryMerger extends AbstractElementMerger {

    private final ImageSize[][] imageSizes;

    /**
     * A merger which merges classification summary elements. The merging is based on weights which are evaluated from
     * image sizes. The merger therefore requires as input an {@code ImageSize[][]} where the first dimension
     * corresponds to the number of PDUs to be stitched and the second dimension to the number of image sizes in the
     * respective PDU.
     *
     * @param imageSizes
     */
    ClassificationSummaryMerger(ImageSize[][] imageSizes) {
        this.imageSizes = imageSizes;
    }

    @Override
    public void mergeNodes(List<Node> fromParents, Element toParent, Document toDocument) throws PDUStitchingException {
        final double[] weights = getWeights(fromParents);
        final Map<String, Double>[] percentagesPerPDU = getPercentagesFromParents(fromParents);
        List<String> elementNames = new ArrayList<>();
        List<Double> percentageValues = new ArrayList<>();
        for (int i = 0; i < fromParents.size(); i++) {
            for (Map.Entry<String, Double> percentage : percentagesPerPDU[i].entrySet()) {
                final String elementName = percentage.getKey();
                final int index = elementNames.indexOf(elementName);
                if (index >= 0) {
                    percentageValues.set(index, percentageValues.get(index) + weights[i] * percentage.getValue());
                } else {
                    elementNames.add(elementName);
                    percentageValues.add(weights[i] * percentage.getValue());
                }
            }
        }
        for (int i = 0; i < elementNames.size(); i++) {
            final Element newElement = toDocument.createElement(elementNames.get(i));
            newElement.setAttribute("percentage", percentageValues.get(i).toString());
            toParent.appendChild(newElement);
        }
    }

    private Map<String, Double>[] getPercentagesFromParents(List<Node> fromParents) throws PDUStitchingException {
        Map<String, Double>[] percentages = new Map[fromParents.size()];
        for (int i = 0; i < fromParents.size(); i++) {
            percentages[i] = new LinkedHashMap<>();
            final NodeList childNodes = fromParents.get(i).getChildNodes();
            for (int j = 0; j < childNodes.getLength(); j++) {
                final Node childNode = childNodes.item(j);
                if (childNode.hasAttributes()) {
                    final Node percentageAttribute = childNode.getAttributes().getNamedItem("percentage");
                    if (percentageAttribute != null) {
                        percentages[i].put(childNode.getNodeName(), Double.parseDouble(percentageAttribute.getNodeValue()));
                    }
                }
            }
        }
        return percentages;
    }

    private double[] getWeights(List<Node> fromParents) throws PDUStitchingException {
        int[] numberOfPixels = new int[fromParents.size()];
        int totalNumberOfPixels = 0;
        for (int i = 0; i < fromParents.size(); i++) {
            final Node gridNode = fromParents.get(i).getAttributes().getNamedItem("grid");
            if (gridNode == null) {
                throw new PDUStitchingException("Grid node expected in classification summary");
            }
            final String grid = gridNode.getNodeValue();
            final String id = getId(grid);
            for (ImageSize imageSize : imageSizes[i]) {
                if (imageSize.getIdentifier().equals(id)) {
                    numberOfPixels[i] = imageSize.getColumns() * imageSize.getRows();
                    totalNumberOfPixels += numberOfPixels[i];
                    break;
                }
            }
        }
        double[] weights = new double[fromParents.size()];
        for (int i = 0; i < weights.length; i++) {
            weights[i] = (double) numberOfPixels[i] / totalNumberOfPixels;
        }
        return weights;
    }

    private static String getId(String gridName) {
        switch (gridName) {
            case "1 km":
                return "in";
            case "0.5 km stripe A":
                return "an";
            case "0.5 km stripe B":
                return "bn";
            case "0.5 km TDI":
                return "cn";
            default:
                return "";
        }

    }
}
