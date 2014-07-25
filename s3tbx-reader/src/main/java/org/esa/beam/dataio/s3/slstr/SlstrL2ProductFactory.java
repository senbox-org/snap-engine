package org.esa.beam.dataio.s3.slstr;

import org.esa.beam.dataio.s3.Sentinel3ProductReader;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;

/**
 * @author Tonio Fincke
 */
public abstract class SlstrL2ProductFactory extends SlstrProductFactory {

    private int nadirStartOffset;
    private int nadirTrackOffset;
    private int obliqueStartOffset;
    private int obliqueTrackOffset;

    protected SlstrL2ProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected void processProductSpecificMetadata(MetadataElement metadataElement) {
        final MetadataElement slstrInformationElement = metadataElement.getElement("slstrProductInformation");
        for (int i = 0; i < slstrInformationElement.getNumElements(); i++) {
            final MetadataElement slstrElement = slstrInformationElement.getElementAt(i);
            final String slstrElementName = slstrElement.getName();
            if (slstrElementName.endsWith("ImageSize")) {
                final int startOffset =
                        Integer.parseInt(slstrElement.getAttribute("startOffset").getData().getElemString());
                final int trackOffset =
                        Integer.parseInt(slstrElement.getAttribute("trackOffset").getData().getElemString());
                if (slstrElementName.equals("nadirImageSize")) {
                    nadirStartOffset = startOffset;
                    nadirTrackOffset = trackOffset;
                    setReferenceStartOffset(startOffset);
                    setReferenceTrackOffset(trackOffset);
                    setReferenceResolutions(getResolutions("n"));
                } else {
                    obliqueStartOffset = startOffset;
                    obliqueTrackOffset = trackOffset;
                }
            }
        }
    }

    protected Integer getStartOffset(String gridIndex) {
        if(gridIndex.endsWith("o")) {
            return obliqueStartOffset;
        }
        return nadirStartOffset;
    }

    protected Integer getTrackOffset(String gridIndex) {
        if(gridIndex.endsWith("o")) {
            return obliqueTrackOffset;
        }
        return nadirTrackOffset;
    }

}
