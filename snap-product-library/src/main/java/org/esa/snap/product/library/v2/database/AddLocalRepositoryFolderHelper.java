package org.esa.snap.product.library.v2.database;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.remote.products.repository.Polygon2D;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Created by jcoravu on 4/10/2019.
 */
public class AddLocalRepositoryFolderHelper {

    private static final Logger logger = Logger.getLogger(AddLocalRepositoryFolderHelper.class.getName());

    public AddLocalRepositoryFolderHelper() {
    }

    protected void finishSavingProduct(SaveProductData saveProductData) {
    }

    protected void invalidProduct(Path path) throws IOException {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "The path '"+path.toString()+"' does not represent a valid product.");
        }
    }

    protected void missingProductGeoCoding(Path path, Product product) throws IOException {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "The local product from the path '"+path.toString()+"' does not contain the geo-coding associated with the scene raster.");
        }
    }

    public List<SaveProductData> addValidProductsFromFolder(Path localRepositoryFolderPath) throws IOException, SQLException {
        List<SaveProductData> savedProducts = null;
        if (Files.exists(localRepositoryFolderPath)) {
            // the local repository folder exists on the disk
            List<LocalProductMetadata> existingLocalRepositoryProducts;
            try (Connection connection = H2DatabaseAccessor.getConnection()) {
                try (Statement statement = connection.createStatement()) {
                    Short localRepositoryId = ProductLibraryDAL.loadLocalRepositoryId(localRepositoryFolderPath, statement);
                    if (localRepositoryId == null) {
                        existingLocalRepositoryProducts = Collections.emptyList();
                    } else {
                        existingLocalRepositoryProducts = ProductLibraryDAL.loadProductRelativePaths(localRepositoryId.shortValue(), statement);
                    }
                }
            }
            savedProducts = saveProductsFromFolder(localRepositoryFolderPath, existingLocalRepositoryProducts);
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "The local repository folder '"+localRepositoryFolderPath.toString()+"' does not exist.");
            }
        }
        return savedProducts;
    }

    protected List<SaveProductData> saveProductsFromFolder(Path localRepositoryFolderPath, List<LocalProductMetadata> existingLocalRepositoryProducts) throws IOException {
        List<SaveProductData> savedProducts = null;
        try (Stream<Path> stream = Files.list(localRepositoryFolderPath)) {
            savedProducts = new ArrayList<>();
            Iterator<Path> it = stream.iterator();
            while (it.hasNext()) {
                Path productPath = it.next();
                try {
                    LocalProductMetadata foundLocalProductMetadata = null;
                    for (int i = 0; i<existingLocalRepositoryProducts.size() && foundLocalProductMetadata == null; i++) {
                        LocalProductMetadata localProductMetadata = existingLocalRepositoryProducts.get(i);
                        Path path = localRepositoryFolderPath.resolve(localProductMetadata.getRelativePath());
                        if (path.equals(productPath)) {
                            // the same product path
                            foundLocalProductMetadata = localProductMetadata;
                        }
                    }
                    SaveProductData saveProductData = null;
                    if (foundLocalProductMetadata != null) {
                        // the product already exists into the database
                        FileTime fileTime = Files.getLastModifiedTime(productPath);
                        if (fileTime.toMillis() == foundLocalProductMetadata.getLastModifiedDate().getTime()) {
                            // unchanged product
                            saveProductData = new SaveProductData(foundLocalProductMetadata.getId(), null, null);
                            savedProducts.add(saveProductData);
                        }
                    }

                    if (saveProductData == null) {
                        // read and save the product into the database
                        saveProductData = readAndSaveProduct(localRepositoryFolderPath, productPath);
                        if (saveProductData != null) {
                            // the product has been saved into the database
                            savedProducts.add(saveProductData);
                            finishSavingProduct(saveProductData);
                        }
                    }
                } catch (Exception exception) {
                    logger.log(Level.SEVERE, "Failed to save the local product from the path '" + productPath.toString() + "'.", exception);
                }
            }
        }
        return savedProducts;
    }

    private SaveProductData readAndSaveProduct(Path localRepositoryFolderPath, Path productPath) throws IOException, SQLException {
        SaveProductData saveProductData = null;
        Product product = ProductIO.readProduct(productPath.toFile());
        if (product == null) {
            invalidProduct(productPath);
        } else if (product.getSceneGeoCoding() == null) {
            try {
                product.dispose();
            } finally {
                missingProductGeoCoding(productPath, product);
            }
        } else {
            try {
                Polygon2D polygon2D = buildProductPolygon(product);
                BufferedImage quickLookImage = null;
//                        try {
//                            QuicklookGenerator quicklookGenerator = new QuicklookGenerator();
//                            quickLookImage = quicklookGenerator.createQuickLookFromBrowseProduct(product);
//                        } catch (Exception exception) {
//                            logger.log(Level.SEVERE, "Failed to create the quick look image for product '" + product.getName() + "'.", exception);
//                        }

                saveProductData = ProductLibraryDAL.saveProduct(product, quickLookImage, polygon2D, productPath, localRepositoryFolderPath);
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
