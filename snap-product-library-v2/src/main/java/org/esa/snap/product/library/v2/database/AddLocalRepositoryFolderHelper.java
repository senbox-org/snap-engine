package org.esa.snap.product.library.v2.database;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.quicklooks.QuicklookGenerator;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.remote.products.repository.geometry.Polygon2D;
import org.esa.snap.remote.products.repository.ThreadStatus;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Created by jcoravu on 4/10/2019.
 */
public class AddLocalRepositoryFolderHelper {

    private static final Logger logger = Logger.getLogger(AddLocalRepositoryFolderHelper.class.getName());

    protected final AllLocalFolderProductsRepository allLocalFolderProductsRepository;

    public AddLocalRepositoryFolderHelper(AllLocalFolderProductsRepository allLocalFolderProductsRepository) {
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

    public List<SaveProductData> addValidProductsFromFolder(Path localRepositoryFolderPath, ThreadStatus threadStatus)
                                                            throws IOException, SQLException, InterruptedException {

        List<SaveProductData> savedProducts = null;
        if (Files.exists(localRepositoryFolderPath)) {
            // the local repository folder exists on the disk
            ThreadStatus.checkCancelled(threadStatus);

            List<LocalProductMetadata> existingLocalRepositoryProducts = this.allLocalFolderProductsRepository.loadRepositoryProductsMetadata(localRepositoryFolderPath);

            ThreadStatus.checkCancelled(threadStatus);

            savedProducts = saveProductsFromFolder(localRepositoryFolderPath, existingLocalRepositoryProducts, true, threadStatus);
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "The local repository folder '"+localRepositoryFolderPath.toString()+"' does not exist.");
            }
        }
        return savedProducts;
    }

    protected List<SaveProductData> saveProductsFromFolder(Path localRepositoryFolderPath, List<LocalProductMetadata> existingLocalRepositoryProducts,
                                                           boolean scanOnlyFirstLevel, ThreadStatus threadStatus)
                                                           throws IOException, InterruptedException {

        List<SaveProductData> savedProducts = new ArrayList<>();
        Stack<Path> stack = new Stack<>();
        stack.push(localRepositoryFolderPath);
        while (!stack.isEmpty()) {
            Path currentPath = stack.pop();

            ThreadStatus.checkCancelled(threadStatus);

            if (Files.isDirectory(currentPath)) {
                try (Stream<Path> stream = Files.list(currentPath)) {

                    ThreadStatus.checkCancelled(threadStatus);

                    Iterator<Path> it = stream.iterator();
                    while (it.hasNext()) {

                        ThreadStatus.checkCancelled(threadStatus);

                        Path productPath = it.next();
                        try {
                            LocalProductMetadata localProductMetadata = foundLocalProductMetadata(localRepositoryFolderPath, existingLocalRepositoryProducts, productPath);
                            SaveProductData saveProductData = null;
                            if (localProductMetadata != null) {
                                // the product already exists into the database
                                FileTime fileTime = Files.getLastModifiedTime(productPath);
                                if (fileTime.toMillis() == localProductMetadata.getLastModifiedDate().getTime()) {
                                    // unchanged product
                                    saveProductData = new SaveProductData(localProductMetadata.getId(), null, null, null);
                                    savedProducts.add(saveProductData);
                                }
                            }

                            ThreadStatus.checkCancelled(threadStatus);

                            if (saveProductData == null) {
                                // read and save the product into the database
                                saveProductData = readAndSaveProduct(localRepositoryFolderPath, productPath, threadStatus);
                                if (saveProductData == null) {
                                    // no product has been loaded from the path
                                    if (!scanOnlyFirstLevel) {
                                        if (Files.isDirectory(productPath)) {
                                            stack.push(productPath);
                                        }
                                    }
                                } else {
                                    // the product has been saved into the database
                                    savedProducts.add(saveProductData);
                                    finishSavingProduct(saveProductData);
                                }
                            } else {
                                // unchanged product
                                if (logger.isLoggable(Level.FINE)) {
                                    logger.log(Level.FINE, "The local product from the path '"+productPath.toString()+"' is unchanged.");
                                }
                            }
                        } catch (Exception exception) {
                            logger.log(Level.SEVERE, "Failed to save the local product from the path '" + productPath.toString() + "'.", exception);
                        }
                    }
                }
            } else {
                throw new IllegalStateException("The path '"+currentPath.toString()+"' is not a folder.");
            }
        }
        return savedProducts;
    }

    private SaveProductData readAndSaveProduct(Path localRepositoryFolderPath, Path productPath, ThreadStatus threadStatus)
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

    private static LocalProductMetadata foundLocalProductMetadata(Path localRepositoryFolderPath, List<LocalProductMetadata> existingLocalRepositoryProducts, Path productPathToCheck) {
        for (int i = 0; i<existingLocalRepositoryProducts.size(); i++) {
            LocalProductMetadata localProductMetadata = existingLocalRepositoryProducts.get(i);
            Path path = localRepositoryFolderPath.resolve(localProductMetadata.getRelativePath());
            if (path.compareTo(productPathToCheck) == 0) {
                return localProductMetadata; // the same product path
            }
        }
        return null;
    }
}
