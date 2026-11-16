/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.watermask.util;

import com.bc.ceres.core.runtime.internal.IOHelper;
import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.watermask.operator.WatermaskUtils;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.MapContent;
import org.geotools.map.MapViewport;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.builder.GridToEnvelopeMapper;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Responsible for transferring shapefiles containing a land/water-mask into a rasterized image.
 *
 * @author Thomas Storm
 */
class ShapeFileRasterizer {

    private final Path targetDir;

    private final String tempDir;

    ShapeFileRasterizer(Path targetDir) {
        this.targetDir = targetDir;
        tempDir = IOHelper.createATempDir().toString();
    }

    /**
     * The main method of this tool.
     *
     * @param args Three arguments are needed: 1) directory containing shapefiles. 2) target directory.
     *             3) resolution in meters / pixel.
     * @throws java.io.IOException If some IO error occurs.
     */
    public static void main(String[] args) throws IOException {
        final Path resourceDir = Path.of(args[0]);
        final Path targetDir = Path.of(args[1]);
        IOHelper.createDirectories(targetDir);
        int sideLength = WatermaskUtils.computeSideLength(Integer.parseInt(args[2]));
        boolean createImage = false;
        if (args.length == 4) {
            createImage = Boolean.parseBoolean(args[3]);
        }
        final ShapeFileRasterizer rasterizer = new ShapeFileRasterizer(targetDir);
        rasterizer.rasterizeShapeFiles(resourceDir, sideLength, createImage);
    }

    void rasterizeShapeFiles(Path directory, int tileSize, boolean createImage) throws IOException {
        Collection<Path> shapeFiles = IOHelper.listFiles(directory, path -> path.getFileName().toString().endsWith(".zip"));
        if (shapeFiles != null) {
            rasterizeShapeFiles(shapeFiles, tileSize, createImage);
        }
        Collection<Path> subdirs = IOHelper.listFiles(directory, Files::isDirectory);
        if (subdirs != null) {
            for (Path subDir : subdirs) {
                rasterizeShapeFiles(subDir, tileSize, createImage);
            }
        }
    }

    void rasterizeShapeFiles(Collection<Path> zippedShapeFiles, int tileSize, boolean createImage) {
        final ExecutorService executorService = Executors.newFixedThreadPool(12);
        int shapeFileIndex = 0;
        for (Path shapeFile: zippedShapeFiles) {
            shapeFileIndex++;
            ShapeFileRunnable runnable = new ShapeFileRunnable(shapeFile, tileSize, shapeFileIndex,
                    zippedShapeFiles.size(), createImage);
            executorService.submit(runnable);
        }
        executorService.shutdown();
        while (!executorService.isTerminated()) {
            try {
                executorService.awaitTermination(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    BufferedImage createImage(Path shapeFile, int tileSize) throws Exception {
        CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        final String shapeFileName = shapeFile.getFileName().toString();
        final ReferencedEnvelope referencedEnvelope = parseEnvelopeFromShapeFileName(shapeFileName, crs);

        final MapViewport viewport = new MapViewport();
        viewport.setBounds(referencedEnvelope);
        viewport.setCoordinateReferenceSystem(crs);

        //MapContext context = new DefaultMapContext(crs);
        MapContent mapContent = new MapContent();
        mapContent.setViewport(viewport);

        final URL shapeFileUrl = shapeFile.toUri().toURL();

        final FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = getFeatureSource(shapeFileUrl);
        org.geotools.map.FeatureLayer featureLayer = new org.geotools.map.FeatureLayer(featureSource, createPolygonStyle());
        mapContent.addLayer(featureLayer);

        BufferedImage landMaskImage = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D graphics = landMaskImage.createGraphics();

        StreamingRenderer renderer = new StreamingRenderer();
        renderer.setMapContent(mapContent);

        Rectangle paintArea = new Rectangle(0, 0, tileSize, tileSize);
        // the transform is computed here, because it ensures that the pixel anchor is in the pixel center and
        // not at the corner as the StreamingRenderer does by default
        AffineTransform transform = createWorldToScreenTransform(referencedEnvelope, paintArea);

        renderer.paint(graphics, paintArea, referencedEnvelope, transform);

        return landMaskImage;
    }

    private AffineTransform createWorldToScreenTransform(ReferencedEnvelope referencedEnvelope, Rectangle paintArea) throws Exception {
        GridEnvelope2D gridRange = new GridEnvelope2D(paintArea);
        final GridToEnvelopeMapper mapper = new GridToEnvelopeMapper(gridRange, referencedEnvelope);
        mapper.setPixelAnchor(PixelInCell.CELL_CENTER);
        return mapper.createAffineTransform().createInverse();
    }

    private ReferencedEnvelope parseEnvelopeFromShapeFileName(String shapeFileName, CoordinateReferenceSystem crs) {
        int lonMin = Integer.parseInt(shapeFileName.substring(1, 4));
        int lonMax;
        if (shapeFileName.startsWith("e")) {
            lonMax = lonMin + 1;
        } else if (shapeFileName.startsWith("w")) {
            lonMin--;
            lonMin = lonMin * -1;
            lonMax = lonMin--;
        } else {
            throw new IllegalStateException("Wrong shapefile-name: '" + shapeFileName + "'.");
        }

        int latMin = Integer.parseInt(shapeFileName.substring(5, 7));
        int latMax;
        if (shapeFileName.charAt(4) == 'n') {
            latMax = latMin + 1;
        } else if (shapeFileName.charAt(4) == 's') {
            latMin--;
            latMin = latMin * -1;
            latMax = latMin--;
        } else {
            throw new IllegalStateException("Wrong shapefile-name: '" + shapeFileName + "'.");
        }
        return new ReferencedEnvelope(lonMin, lonMax, latMin, latMax, crs);
    }

    private void writeToFile(BufferedImage image, String name, boolean createImage) throws IOException {
        String fileName = getFilenameWithoutExtension(name);
        fileName = fileName.substring(0, fileName.length() - 1);
        String imgFileName = fileName + ".img";
        Path outputFile = targetDir.toAbsolutePath().resolve(imgFileName);

        try (OutputStream fileOutputStream = Files.newOutputStream(outputFile)) {
            try {
                byte[] data = ((DataBufferByte) image.getData().getDataBuffer()).getData();
                fileOutputStream.write(data);
                if (createImage) {
                    final Path targetFile = targetDir.toAbsolutePath().resolve(fileName + ".png");
                    ImageUtils.writeImage(image, "png", targetFile.toFile());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    List<Path> createTempFiles(ZipFile zipFile) {
        final Enumeration<? extends ZipEntry> entries = zipFile.entries();
        List<Path> tempShapeFiles;
        try {
            tempShapeFiles = unzipTempFiles(zipFile, entries);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Error generating temp files from shapefile '" + zipFile.getName() + "'.", e);
        }
        return tempShapeFiles;
    }

    private List<Path> unzipTempFiles(ZipFile zipFile, Enumeration<? extends ZipEntry> entries) throws
            IOException {
        List<Path> files = new ArrayList<>();
        while (entries.hasMoreElements()) {
            final ZipEntry entry = entries.nextElement();
            Path file = readIntoTempFile(zipFile, entry);
            files.add(file);
        }
        return files;
    }

    private Path readIntoTempFile(ZipFile zipFile, ZipEntry entry) throws IOException {
        Path file = Path.of(tempDir).resolve(entry.getName());
        final InputStream reader = zipFile.getInputStream(entry);
        final OutputStream writer = Files.newOutputStream(file);
        try {
            byte[] buffer = new byte[1024 * 1024];
            int bytesRead = reader.read(buffer);
            while (bytesRead != -1) {
                writer.write(buffer, 0, bytesRead);
                bytesRead = reader.read(buffer);
            }
        } finally {
            reader.close();
            writer.close();
        }
        return file;
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    private void deleteTempFiles(List<Path> tempFiles) {
        for (Path tempFile : tempFiles) {
            IOHelper.delete(tempFile);
        }
        tempFiles.clear();
    }

    private static String getFilenameWithoutExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i > 0 && i < fileName.length() - 1) {
            return fileName.substring(0, i);
        }
        return fileName;
    }

    private FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource(URL url) throws IOException {
        Map<String, Object> parameterMap = new HashMap<String, Object>();
        parameterMap.put(ShapefileDataStoreFactory.URLP.key, url);
        parameterMap.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, Boolean.TRUE);
        DataStore shapefileStore = DataStoreFinder.getDataStore(parameterMap);
        String typeName = shapefileStore.getTypeNames()[0]; // Shape files do only have one type name
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
        featureSource = shapefileStore.getFeatureSource(typeName);
        return featureSource;
    }

    private Style createPolygonStyle() {
        StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
        FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);

        PolygonSymbolizer symbolizer = styleFactory.createPolygonSymbolizer();
        org.geotools.styling.Stroke stroke = styleFactory.createStroke(
                filterFactory.literal("#FFFFFF"),
                filterFactory.literal(0.0)
        );
        symbolizer.setStroke(stroke);
        Fill fill = styleFactory.createFill(
                filterFactory.literal("#FFFFFF"),
                filterFactory.literal(1.0)
        );
        symbolizer.setFill(fill);

        Rule rule = styleFactory.createRule();
        rule.symbolizers().add(symbolizer);

        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle();
        fts.rules().add(rule);

        Style style = styleFactory.createStyle();
        style.featureTypeStyles().add(fts);

        return style;
    }

    private class ShapeFileRunnable implements Runnable {

        private final Path shapeFile;
        private int tileSize;
        private int index;
        private int shapeFileCount;
        private boolean createImage;

        ShapeFileRunnable(Path shapeFile, int tileSize, int shapeFileIndex, int shapeFileCount, boolean createImage) {
            this.shapeFile = shapeFile;
            this.tileSize = tileSize;
            this.index = shapeFileIndex;
            this.shapeFileCount = shapeFileCount;
            this.createImage = createImage;
        }

        @Override
        public void run() {
            try {
                List<Path> tempShapeFiles;
                try (ZipFile zipFile = new ZipFile(shapeFile.toFile())) {
                    tempShapeFiles = createTempFiles(zipFile);
                }
                for (Path file : tempShapeFiles) {
                    if (file.getFileName().toString().endsWith("shp")) {
                        final BufferedImage image = createImage(file, tileSize);
                        writeToFile(image, shapeFile.getFileName().toString(), createImage);
                    }
                }
                deleteTempFiles(tempShapeFiles);
                System.out.printf("Path %d of %d%n", index, shapeFileCount);
            } catch (Throwable e) {
                e.printStackTrace();
            }

        }

    }
}