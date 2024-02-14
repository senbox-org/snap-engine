package org.esa.snap.dataio.gdal.reader;

import org.esa.snap.engine_utilities.commons.VirtualFile;
import org.esa.snap.core.metadata.MetadataInspector;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.dataio.gdal.drivers.Band;
import org.esa.snap.dataio.gdal.drivers.Dataset;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by jcoravu on 19/12/2019.
 */
public class GDALMetadataInspector implements MetadataInspector {

    public GDALMetadataInspector() {
    }

    @Override
    public Metadata getMetadata(Path productPath) throws IOException {
        try (VirtualFile virtualFile = new VirtualFile(productPath);
             Dataset gdalDataset = GDALProductReader.openGDALDataset(virtualFile.getLocalFile())){
            Metadata metadata = new Metadata(gdalDataset.getRasterXSize(), gdalDataset.getRasterYSize());

            GeoCoding productGeoCoding = GDALProductReader.buildGeoCoding(gdalDataset, null, null);
            metadata.setGeoCoding(productGeoCoding);

            int bandCount = gdalDataset.getRasterCount();
            for (int bandIndex = 0; bandIndex < bandCount; bandIndex++) {
                // bands are not 0-base indexed, so we must add 1
                try (Band gdalBand = gdalDataset.getRasterBand(bandIndex + 1)) {

                    String bandName = GDALProductReader.computeBandName(gdalBand, bandIndex);
                    metadata.getBandList().add(bandName);

                    String maskName = GDALProductReader.computeMaskName(gdalBand, bandName);
                    if (maskName != null) {
                        metadata.getMaskList().add(maskName);
                    }
                }
            }

            return metadata;
        } catch (FactoryException | TransformException e) {
            throw new IOException(e);
        }
    }
}