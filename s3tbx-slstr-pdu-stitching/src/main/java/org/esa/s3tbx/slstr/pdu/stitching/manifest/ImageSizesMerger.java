package org.esa.s3tbx.slstr.pdu.stitching.manifest;

import org.esa.s3tbx.slstr.pdu.stitching.ImageSize;
import org.esa.s3tbx.slstr.pdu.stitching.ImageSizeHandler;
import org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import java.util.List;

/**
 * @author Tonio Fincke
 */
class ImageSizesMerger extends AbstractElementMerger {

    @Override
    public void mergeNodes(List<Node> fromParents, Element toParent, Document toDocument) throws PDUStitchingException {

        ImageSize[] imageSizes = new ImageSize[fromParents.size()];
        for (int i = 0; i < imageSizes.length; i++) {
            imageSizes[i] = ImageSizeHandler.extractImageSizeFromNode(fromParents.get(i), "");
        }
        final ImageSize targetImageSize = ImageSizeHandler.createTargetImageSize(imageSizes);
        final String grid = fromParents.get(0).getAttributes().item(0).getNodeValue();

        toParent.setAttribute("grid", grid);
        final Element startOffsetElement = toDocument.createElement("sentinel3:startOffset");
        final Text startOffsetTextNode = toDocument.createTextNode("" + targetImageSize.getStartOffset());
        startOffsetElement.appendChild(startOffsetTextNode);
        toParent.appendChild(startOffsetElement);
        final Element trackOffsetElement = toDocument.createElement("sentinel3:trackOffset");
        final Text trackOffsetTextNode = toDocument.createTextNode("" + targetImageSize.getTrackOffset());
        trackOffsetElement.appendChild(trackOffsetTextNode);
        toParent.appendChild(trackOffsetElement);
        final Element rowsElement = toDocument.createElement("sentinel3:rows");
        final Text rowsTextNode = toDocument.createTextNode("" + targetImageSize.getRows());
        rowsElement.appendChild(rowsTextNode);
        toParent.appendChild(rowsElement);
        final Element columnsElement = toDocument.createElement("sentinel3:columns");
        final Text columnsTextNode = toDocument.createTextNode("" + targetImageSize.getColumns());
        columnsElement.appendChild(columnsTextNode);
        toParent.appendChild(columnsElement);
    }

}
