package org.esa.beam.dataio.bigtiff;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import it.geosolutions.imageio.plugins.tiff.TIFFImageWriteParam;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.IIOImage;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

class NewBandWriter {

    private TIFFImageWriter imageWriter;
    private TIFFImageWriteParam writeParam;
    private final ArrayList<String> bandsWritten;

    public NewBandWriter(TIFFImageWriter imageWriter, TIFFImageWriteParam writeParam) {
        this.imageWriter = imageWriter;
        this.writeParam = writeParam;
        bandsWritten = new ArrayList<String>();
    }

    void writeBandRasterData(final Band sourceBand,
                             final int regionX,
                             final int regionY,
                             final int regionWidth,
                             final int regionHeight,
                             final ProductData regionData,
                             ProgressMonitor pm) throws IOException {

        final String name = sourceBand.getName();
        if (bandsWritten.contains(name)) {
            return;
        }

        final MultiLevelImage sourceImage = sourceBand.getSourceImage();
        final IIOImage iioImage = new IIOImage(sourceImage, null, null);
        imageWriter.write(null, iioImage, writeParam);

        bandsWritten.add(name);
    }

    void dispose() {
        imageWriter.dispose();
    }
}
