package org.esa.snap.jp2.reader.metadata;

import org.esa.snap.core.metadata.MetadataInspector;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.metadata.XmlMetadataParser;
import org.esa.snap.core.metadata.XmlMetadataParserFactory;
import org.esa.snap.lib.openjpeg.utils.OpenJpegExecRetriever;
import org.esa.snap.lib.openjpeg.utils.OpenJpegUtils;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.esa.snap.lib.openjpeg.utils.OpenJpegUtils.validateOpenJpegExecutables;

/**
 * Metadata inspector for JP2 files.
 *
 * @author Denisa Stefanescu
 */

public class JP2MetadataInspector implements MetadataInspector {
    protected Logger logger = Logger.getLogger(getClass().getName());

    public Metadata getMetadata(Path productPath) throws IOException {
        Metadata metadata = new Metadata();
        XmlMetadataParserFactory.registerParser(Jp2XmlMetadata.class, new XmlMetadataParser<>(Jp2XmlMetadata.class));
        try {
            OpjDumpFile opjDumpFile = new OpjDumpFile();
            if (OpenJpegUtils.canReadJP2FileHeaderWithOpenJPEG()) {
                if (!validateOpenJpegExecutables(OpenJpegExecRetriever.getOpjDump(), OpenJpegExecRetriever.getOpjDecompress())) {
                    throw new IOException("Invalid OpenJpeg executables");
                }

                opjDumpFile.readHeaderWithOpenJPEG(productPath);
            } else {
                opjDumpFile.readHeaderWithInputStream(productPath, 5 * 1024, true);
            }
            Jp2XmlMetadata metadataHeader = opjDumpFile.getMetadata();
            //image info is needed for product width and height
            ImageInfo imageInfo = opjDumpFile.getImageInfo();
            int imageWidth = imageInfo.getWidth();
            int imageHeight = imageInfo.getHeight();
            //code stream info is needed for the product band list
            CodeStreamInfo csInfo = opjDumpFile.getCodeStreamInfo();
            int numBands = csInfo.getComponentTilesInfo().size();
            for (int bandIdx = 0; bandIdx < numBands; bandIdx++) {
                metadata.getBandList().add("band_" + (bandIdx + 1));
            }

            metadata.setProductWidth(imageWidth);
            metadata.setProductHeight(imageHeight);
            if(metadata.isHasGeoCoding()) {
                metadata.setGeoCoding(addGeoCoding(metadataHeader, imageWidth, imageHeight));
            }
        } catch (IOException|InterruptedException e) {
            throw new IOException("Error while reading file '" + productPath.toString() + "'.", e);
        }
        return metadata;
    }

    private GeoCoding addGeoCoding(Jp2XmlMetadata metadata, int width, int height) {

        String crsGeoCoding = metadata.getCrsGeocoding();
        Point2D origin = metadata.getOrigin();
        GeoCoding geoCoding = null;
        if (crsGeoCoding != null && origin != null) {
            try {
                CoordinateReferenceSystem mapCRS = CRS.decode(crsGeoCoding.replace("::", ":"));
                geoCoding = new CrsGeoCoding(mapCRS, width, height, origin.getX(), origin.getY(), metadata.getStepX(), -metadata.getStepY());
            } catch (Exception gEx) {
                logger.warning(gEx.getMessage());
            }
        }
        return geoCoding;
    }
}
