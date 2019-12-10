package org.esa.snap.dataio.geotiff;

import org.esa.snap.core.dataio.MetadataInspector;
import org.esa.snap.core.datamodel.Product;

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
            Product product = GeoTiffProductReader.readMetadataProduct(productPath, true);

            Metadata metadata = new Metadata();
            metadata.setProductWidth(product.getSceneRasterWidth());
            metadata.setProductHeight(product.getSceneRasterHeight());

            for (int i = 0; i < product.getNumBands(); i++) {
                metadata.getBandList().add(product.getBandAt(i).getName());
            }
            metadata.setGeoCoding(product.getSceneGeoCoding());

            return metadata;
        } catch (RuntimeException | IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException(exception);
        }
    }
}
