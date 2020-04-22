package org.esa.snap.dataio.geotiff;

import org.esa.snap.core.metadata.MetadataInspector;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.ImageRegistryUtils;

import javax.imageio.spi.ImageInputStreamSpi;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by jcoravu on 25/11/2019.
 */
public class GeoTiffMetadataInspector implements MetadataInspector {

    public GeoTiffMetadataInspector() {
    }

    @Override
    public Metadata getMetadata(Path productPath) throws IOException {
        try {
            ImageInputStreamSpi imageInputStreamSpi = ImageRegistryUtils.registerImageInputStreamSpi();
            try {
                Product product = GeoTiffProductReader.readMetadataProduct(productPath, true);

                Metadata metadata = new Metadata(product.getSceneRasterWidth(), product.getSceneRasterHeight());

                for (int i = 0; i < product.getNumBands(); i++) {
                    metadata.addBandName(product.getBandAt(i).getName());
                }
                metadata.setGeoCoding(product.getSceneGeoCoding());

                return metadata;
            } finally {
                if (imageInputStreamSpi != null) {
                    ImageRegistryUtils.deregisterImageInputStreamSpi(imageInputStreamSpi);
                }
            }
        } catch (RuntimeException | IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException(exception);
        }
    }
}
