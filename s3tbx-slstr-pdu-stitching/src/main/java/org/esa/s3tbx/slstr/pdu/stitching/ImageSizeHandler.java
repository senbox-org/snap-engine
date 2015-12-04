package org.esa.s3tbx.slstr.pdu.stitching;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Tonio Fincke
 */
public class ImageSizeHandler {

    public static ImageSize createTargetImageSize(ImageSize[] imageSizes) {
        int startOffset = Integer.MAX_VALUE;
        int trackOffset = Integer.MAX_VALUE;
        int highestStart = Integer.MIN_VALUE;
        int highestTrack = Integer.MIN_VALUE;
        for (ImageSize imageSize : imageSizes) {
            if (imageSize.getStartOffset() < startOffset) {
                startOffset = imageSize.getStartOffset();
            }
            if (imageSize.getTrackOffset() < trackOffset) {
                trackOffset = imageSize.getTrackOffset();
            }
            if (imageSize.getStartOffset() + imageSize.getRows() > highestStart) {
                highestStart = imageSize.getStartOffset() + imageSize.getRows();
            }
            if (imageSize.getTrackOffset() + imageSize.getColumns() > highestTrack) {
                highestTrack = imageSize.getTrackOffset() + imageSize.getColumns();
            }
        }
        return new ImageSize(imageSizes[0].getIdentifier(), startOffset, trackOffset, highestStart - startOffset, highestTrack - trackOffset);
    }

    static ImageSize[] extractImageSizes(Document manifestDocument) {
        final NodeList nadirElements = manifestDocument.getElementsByTagName("slstr:nadirImageSize");
        final NodeList obliqueElements = manifestDocument.getElementsByTagName("slstr:obliqueImageSize");
        final ImageSize[] imageSizes = new ImageSize[obliqueElements.getLength() + obliqueElements.getLength()];
        for (int i = 0; i < nadirElements.getLength(); i++) {
            imageSizes[i] = extractImageSizeFromNode(nadirElements.item(i), "n");
        }
        for (int i = 0; i < obliqueElements.getLength(); i++) {
            imageSizes[nadirElements.getLength() + i] = extractImageSizeFromNode(obliqueElements.item(i), "o");
        }
        return imageSizes;
    }

    public static ImageSize extractImageSizeFromNode(Node element, String idExtension) {
        String id = getId(element.getAttributes().getNamedItem("grid").getNodeValue()) + idExtension;
        int startOffset = -1;
        int trackOffset = -1;
        int rows = -1;
        int columns = -1;
        final NodeList elementChildNodes = element.getChildNodes();
        for (int j = 0; j < elementChildNodes.getLength(); j++) {
            final Node node = elementChildNodes.item(j);
            if (node.getNodeName().equals("sentinel3:startOffset")) {
                startOffset = Integer.parseInt(node.getChildNodes().item(0).getNodeValue());
            } else if (node.getNodeName().equals("sentinel3:trackOffset")) {
                trackOffset = Integer.parseInt(node.getChildNodes().item(0).getNodeValue());
            } else if (node.getNodeName().equals("sentinel3:rows")) {
                rows = Integer.parseInt(node.getChildNodes().item(0).getNodeValue());
            } else if (node.getNodeName().equals("sentinel3:columns")) {
                columns = Integer.parseInt(node.getChildNodes().item(0).getNodeValue());
            }
        }
        return new ImageSize(id, startOffset, trackOffset, rows, columns);
    }

    private static String getId(String gridName) {
        switch (gridName) {
            case "1 km":
                return "i";
            case "0.5 km stripe A":
                return "a";
            case "0.5 km stripe B":
                return "b";
            case "0.5 km TDI":
                return "c";
            case "Tie Points":
                return "t";
            default:
                return "";
        }
    }

}
