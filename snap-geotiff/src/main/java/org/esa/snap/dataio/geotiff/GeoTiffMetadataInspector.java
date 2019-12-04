package org.esa.snap.dataio.geotiff;

import it.geosolutions.imageio.plugins.tiff.TIFFField;
import it.geosolutions.imageio.plugins.tiff.TIFFTag;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageMetadata;
import org.esa.snap.core.dataio.MetadataInspector;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;

import java.awt.Dimension;
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
        try (GeoTiffImageReader geoTiffImageReader = GeoTiffImageReader.buildGeoTiffImageReader(productPath)) {
            Dimension productSize = new Dimension(geoTiffImageReader.getImageWidth(), geoTiffImageReader.getImageHeight());

            Metadata metadata = new Metadata();
            metadata.setProductWidth(productSize.width);
            metadata.setProductHeight(productSize.height);

            TIFFImageMetadata imageMetadata = geoTiffImageReader.getImageMetadata();
            TiffFileInfo tiffInfo = new TiffFileInfo(imageMetadata.getRootIFD());
            TIFFField tagNumberField = tiffInfo.getField(Utils.PRIVATE_BEAM_TIFF_TAG_NUMBER);

            Product product = null;
            if (tagNumberField != null && tagNumberField.getType() == TIFFTag.TIFF_ASCII) {
                String tagNumberText = tagNumberField.getAsString(0).trim();
                if (tagNumberText.contains("<Dimap_Document")) { // with DIMAP header
                    product = GeoTiffProductReader.buildProductFromDimapHeader(tagNumberText, productSize);
                }
            }
            if (product == null) {            // without DIMAP header
                product = GeoTiffProductReader.buildProductWithoutDimapHeader(productPath, "GeoTIFF", tiffInfo, geoTiffImageReader.getBaseImage(), productSize);
            }
            for (int i = 0; i < product.getNumBands(); i++) {
                metadata.getBandList().add(product.getBandAt(i).getName());
            }

            if (tiffInfo.isGeotiff()) {
                GeoCoding geoCoding = GeoTiffProductReader.buildGeoCoding(imageMetadata, product.getSceneRasterSize(), null);
                metadata.setGeoCoding(geoCoding);
            }

            return metadata;
        } catch (RuntimeException | IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException(exception);
        }
    }
}
