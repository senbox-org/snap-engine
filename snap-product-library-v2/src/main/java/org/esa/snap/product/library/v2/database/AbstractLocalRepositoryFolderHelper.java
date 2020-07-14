package org.esa.snap.product.library.v2.database;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.quicklooks.QuicklookGenerator;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.remote.products.repository.ThreadStatus;
import org.esa.snap.remote.products.repository.geometry.Polygon2D;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractLocalRepositoryFolderHelper {

    private static final Logger logger = Logger.getLogger(AbstractLocalRepositoryFolderHelper.class.getName());

    protected final AllLocalFolderProductsRepository allLocalFolderProductsRepository;

    protected AbstractLocalRepositoryFolderHelper(AllLocalFolderProductsRepository allLocalFolderProductsRepository) {
        this.allLocalFolderProductsRepository = allLocalFolderProductsRepository;
    }

    protected void finishSavingProduct(SaveProductData saveProductData) {
    }

    protected void invalidProduct(Path path) throws IOException {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "The path '"+path.toString()+"' does not represent a valid product.");
        }
    }

    protected void missingProductGeoCoding(Path path) throws IOException {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "The local product from the path '"+path.toString()+"' does not contain the geo-coding associated with the scene raster.");
        }
    }

    protected final SaveProductData readAndSaveProduct(Path localRepositoryFolderPath, Path productPath, ThreadStatus threadStatus)
                                                       throws IOException, SQLException, InterruptedException {

        SaveProductData saveProductData = null;
        Product product = ProductIO.readProduct(productPath.toFile());
        if (product == null) {
            // the local product has not been read
            invalidProduct(productPath);
        } else if (product.getSceneGeoCoding() == null) {
            // the local product has not geo coding
            try {
                product.dispose();
            } finally {
                missingProductGeoCoding(productPath);
            }
        } else {
            // the local product has geo coding
            try {
                ThreadStatus.checkCancelled(threadStatus);

                Polygon2D polygon2D = buildProductPolygon(product);
                BufferedImage quickLookImage = null;
                try {
                    QuicklookGenerator quicklookGenerator = new QuicklookGenerator();
                    quickLookImage = quicklookGenerator.createQuickLookFromBrowseProduct(product);
                } catch (Exception exception) {
                    logger.log(Level.SEVERE, "Failed to create the quick look image for product '" + product.getName() + "'.", exception);
                }

                ThreadStatus.checkCancelled(threadStatus);

                saveProductData = this.allLocalFolderProductsRepository.saveLocalProduct(product, quickLookImage, polygon2D, productPath, localRepositoryFolderPath);
            } finally {
                product.dispose();
            }
        }
        return saveProductData;
    }

    private static Polygon2D buildProductPolygon(Product product) {
        int productWidth = product.getSceneRasterWidth();
        int productHeight = product.getSceneRasterHeight();
        PixelPos[] pixelPositions = new PixelPos[5];
        pixelPositions[0] = new PixelPos(0.0d, 0.0d);
        pixelPositions[1] = new PixelPos(productWidth, 0.0d);
        pixelPositions[2] = new PixelPos(productWidth, productHeight);
        pixelPositions[3] = new PixelPos(0.0d, productHeight);
        pixelPositions[4] = new PixelPos(0.0d, 0.0d);

        GeoPos[] geographicalPositions = new GeoPos[pixelPositions.length];
        GeoCoding sceneGeoCoding = product.getSceneGeoCoding();
        for (int i = 0; i < pixelPositions.length; i++) {
            geographicalPositions[i] = sceneGeoCoding.getGeoPos(pixelPositions[i], null);
        }
        ProductUtils.normalizeGeoPolygon(geographicalPositions);

        Polygon2D polygon2D = new Polygon2D();
        for (int i = 0; i < geographicalPositions.length; i++) {
            polygon2D.append(geographicalPositions[i].getLon(), geographicalPositions[i].getLat());
        }
        return polygon2D;
    }
}
