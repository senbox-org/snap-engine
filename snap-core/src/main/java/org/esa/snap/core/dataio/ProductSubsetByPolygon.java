package org.esa.snap.core.dataio;

import com.bc.ceres.core.ProgressMonitor;
import com.google.common.io.Files;
import org.esa.snap.core.dataio.placemark.PlacemarkIO;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PinDescriptor;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.metadata.MetadataInspector;
import org.esa.snap.core.util.GeoUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geojson.geom.GeometryJSON;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.DOMBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipFile;

/**
 * This class provides the core components for the new subset feature, that is Polygon Subset.
 * It provides the functionality to build a subset polygon for a product which can be defined by the one of the following:
 * - a WKT polygon with geographic coordinates (Lat,Lon)
 * - a WKT polygon with pixel coordinates (X,Y)
 * - a vector file in supported formats (SHAPEFILE, KML, KMZ, GEOJSON, WKT, TXT, PLACEMARK, PNX)
 *
 * @author Adrian Draghici
 */
public class ProductSubsetByPolygon {

    private Polygon subsetPolygon = null;
    private GeoCoding polygonGeocoding = null;

    /**
     * Checks whether the polygon is loaded
     *
     * @return {@code true} when the polygon is loaded and ready to use
     */
    public boolean isLoaded() {
        return subsetPolygon != null;
    }

    /**
     * Clears the polygon and its corresponding geocoding
     */
    public void clear() {
        this.subsetPolygon = null;
        this.polygonGeocoding = null;
    }

    /**
     * Provides the filter used by SNAP File Chooser to show only the supported vector files from which the polygon can be loaded
     *
     * @return the filter used by SNAP File Chooser to show only the supported vector files from which the polygon can be loaded
     */
    public SnapFileFilter getVectorFileFilter() {
        return new SnapFileFilter("Vector File", new String[]{"." + SupportedVectorFilesFormat.SHAPEFILE, "." + SupportedVectorFilesFormat.KML, "." + SupportedVectorFilesFormat.KMZ, "." + SupportedVectorFilesFormat.GEOJSON, "." + SupportedVectorFilesFormat.TXT, "." + SupportedVectorFilesFormat.WKT, "." + SupportedVectorFilesFormat.PLACEMARK_FILE, "." + SupportedVectorFilesFormat.PNX}, "Vector Files");
    }

    /**
     * Loads the polygon from the provided vector file for the specified product metadata
     *
     * @param vectorFile            the vector file from which the polygon will be loaded, with the one of the following supported formats:
     *                              <ul>SHAPEFILE (*.shp)</ul>
     *                              <ul>KML (*.kml)</ul>
     *                              <ul>KMZ (*.kmz)</ul>
     *                              <ul>GEOJSON (*.json)</ul>
     *                              <ul>WKT (*.wkt)</ul>
     *                              <ul>TXT (*.txt)</ul>
     *                              <ul>PLACEMARK (*.placemark)</ul>
     *                              <ul>PNX (*.pnx)</ul>
     * @param targetProductMetadata the metadata of the product for which the polygon will be loaded
     * @param pm                    the progress monitor object to show the progress of the operation
     * @throws Exception when an error occurs while loading the polygon from the vector file
     */
    public void loadPolygonFromVectorFile(File vectorFile, MetadataInspector.Metadata targetProductMetadata, ProgressMonitor pm) throws Exception {
        this.subsetPolygon = readPolygonFromVectorFile(vectorFile, targetProductMetadata, pm);
        this.polygonGeocoding = targetProductMetadata.getGeoCoding();
    }

    /**
     * Loads the polygon from the WKT string
     *
     * @param wktString             the WKT string from which the polygon will be loaded, in which all the coordinates can be pixel coordinates (X,Y) or geo coordinates (Lat,Lon), but not mixed
     * @param pixelCoordinates      the boolean flag which will indicate how the coordinates from the WKT string will be interpreted.
     *                              <ul>{@code true} the coordinates from the WKT string will be interpreted as pixel coordinates (X,Y)</ul>
     *                              <ul>{@code false} the coordinates from the WKT string will be interpreted as geo coordinates (Lat,Lon)</ul>
     * @param targetProductMetadata the metadata of the product for which the polygon will be loaded
     * @param pm                    the progress monitor object to show the progress of the operation
     * @throws Exception when an error occurs while loading the polygon from the WKT string
     */
    public void loadPolygonFromWKTString(String wktString, boolean pixelCoordinates, MetadataInspector.Metadata targetProductMetadata, ProgressMonitor pm) throws Exception {
        this.subsetPolygon = readPolygonFromWKTString(wktString, pixelCoordinates, targetProductMetadata, pm);
        this.polygonGeocoding = targetProductMetadata.getGeoCoding();
    }

    /**
     * Provides the extent of the polygon (the rectangle which contains all the points of the polygon)
     *
     * @return the extent of the polygon (the rectangle which contains all the points of the polygon)
     */
    public Rectangle getExtentOfPolygon() {
        if (isLoaded()) {
            return GeoUtils.computePolygonExtent(subsetPolygon);
        }
        return null;
    }

    /**
     * Provides the extent of the polygon (the rectangle which contains all the points of the polygon) with the coordinates projected to the specified geocoding
     *
     * @param geoCoding the target geocoding to which the coordinates of the extent will be projected
     * @return Provides the extent of the polygon (the rectangle which contains all the points of the polygon) with the coordinates projected to the specified geocoding
     */
    public Rectangle getExtentOfPolygonProjectedToGeocoding(GeoCoding geoCoding) {
        if (isLoaded()) {
            return GeoUtils.computePolygonExtent(getSubsetPolygonProjectedToGeocoding(geoCoding));
        }
        return null;
    }

    /**
     * Provides the loaded polygon (in pixel coordinates)
     *
     * @return the loaded polygon (in pixel coordinates)
     */
    public Polygon getSubsetPolygon() {
        return subsetPolygon;
    }

    /**
     * Provides the loaded polygon (in pixel coordinates) projected to the specified geocoding
     *
     * @param geoCoding the target geocoding to which the coordinates of the polygon will be projected
     * @return the loaded polygon (in pixel coordinates) projected to the specified geocoding
     */
    public Polygon getSubsetPolygonProjectedToGeocoding(GeoCoding geoCoding) {
        if (geoCoding != null && this.polygonGeocoding != null) {
            return GeoUtils.projectPolygonToGeocoding(subsetPolygon, polygonGeocoding, geoCoding);
        }
        return subsetPolygon;
    }

    private static Polygon readPolygonFromVectorFile(File vectorFile, MetadataInspector.Metadata targetProductMetadata, ProgressMonitor pm) throws Exception {
        if (targetProductMetadata == null) {
            throw new IllegalArgumentException("missing product metadata.");
        }
        final GeoCoding geoCoding = targetProductMetadata.getGeoCoding();
        final Dimension productDimension = new Dimension(targetProductMetadata.getProductWidth(), targetProductMetadata.getProductHeight());
        switch (Files.getFileExtension(vectorFile.getName())) {
            case SupportedVectorFilesFormat.SHAPEFILE:
                return readPolygonFromShapeFile(vectorFile, geoCoding, pm);
            case SupportedVectorFilesFormat.KML:
                return readPolygonFromKMLFile(vectorFile, geoCoding, productDimension, pm);
            case SupportedVectorFilesFormat.KMZ:
                return readPolygonFromKMZFile(vectorFile, geoCoding, productDimension, pm);
            case SupportedVectorFilesFormat.GEOJSON:
                return readPolygonFromGeoJsonFile(vectorFile, geoCoding, productDimension, pm);
            case SupportedVectorFilesFormat.TXT:
            case SupportedVectorFilesFormat.WKT:
                return readPolygonFromWKTFile(vectorFile, geoCoding, productDimension, pm);
            case SupportedVectorFilesFormat.PLACEMARK_FILE:
            case SupportedVectorFilesFormat.PNX:
                return readPolygonFromPlacemarkFile(vectorFile, geoCoding, productDimension, pm);
            default:
                throw new IllegalArgumentException("Unsupported vector file.");
        }
    }

    private static Polygon readPolygonFromWKTString(String wktString, boolean pixelCoordinates, MetadataInspector.Metadata targetProductMetadata, ProgressMonitor pm) throws Exception {
        if (targetProductMetadata == null) {
            throw new IllegalArgumentException("missing product metadata.");
        }
        final GeoCoding geoCoding = targetProductMetadata.getGeoCoding();
        final Dimension productDimension = new Dimension(targetProductMetadata.getProductWidth(), targetProductMetadata.getProductHeight());
        return readPolygonFromWKTString(wktString, pixelCoordinates, geoCoding, productDimension, pm);
    }

    private static Polygon readPolygonFromShapeFile(File file, GeoCoding geoCoding, ProgressMonitor pm) throws Exception {
        pm.beginTask("Loading Shapefile", 100);
        try {
            final FileDataStore fileDataStore = FileDataStoreFinder.getDataStore(file);
            pm.worked(10);
            final SimpleFeatureCollection simpleFeatureCollection = fileDataStore.getFeatureSource().getFeatures();
            pm.worked(50);
            try (SimpleFeatureIterator simpleFeatureIterator = simpleFeatureCollection.features()) {
                pm.worked(75);
                final MultiPolygon vectorFileContent = (MultiPolygon) simpleFeatureIterator.next().getAttributes().stream().filter(Objects::nonNull).findFirst().orElse(null);
                pm.worked(90);
                if (vectorFileContent != null) {
                    if (vectorFileContent.getNumGeometries() > 1) {
                        throw new IllegalArgumentException("Subseting by multiple polygons is not supported.");
                    }
                    final Polygon polygon = (Polygon) vectorFileContent.getGeometryN(0);
                    return GeoUtils.projectPolygonToImage(polygon, geoCoding);
                }
            }
            return null;
        } finally {
            pm.worked(100);
            pm.done();
        }
    }

    private static Polygon readPolygonFromPlacemarkFile(File file, GeoCoding geoCoding, Dimension productDimension, ProgressMonitor pm) throws Exception {
        pm.beginTask("Loading placemark file", 100);
        try (FileReader reader = new FileReader(file)) {
            final List<Placemark> placemarks = PlacemarkIO.readPlacemarks(reader, geoCoding, PinDescriptor.getInstance());
            if (placemarks.size() < 3) {
                throw new IllegalArgumentException("Cannot create a polygon. 3 or more points are required.");
            }
            int w = 10;
            pm.worked(w);
            final List<Coordinate> placemarkCoordinates = new ArrayList<>();
            for (Placemark placemark : placemarks) {
                final PixelPos pixelPos = placemark.getPixelPos();
                placemarkCoordinates.add(new Coordinate(pixelPos.getX(), pixelPos.getY()));
                w += 40 / placemarks.size();
                pm.worked(w);
            }
            placemarkCoordinates.add(placemarkCoordinates.get(0));
            final Coordinate[] polygonCoordinates = placemarkCoordinates.toArray(new Coordinate[0]);
            pm.worked(50);
            return buildPolygonFromPixelCoordinates(polygonCoordinates, productDimension, pm);
        } finally {
            pm.worked(100);
            pm.done();
        }
    }

    private static Polygon readPolygonFromWKTFile(File file, GeoCoding geoCoding, Dimension productDimension, ProgressMonitor pm) throws Exception {
        pm.beginTask("Loading WKT file", 100);
        final StringBuilder wktInput = new StringBuilder();
        try (final BufferedReader reader = Files.newReader(file, Charset.defaultCharset())) {
            String line;
            while ((line = reader.readLine()) != null) {
                wktInput.append(line);
            }
            pm.worked(10);
            final String wktString = wktInput.toString();
            final boolean pixelCoordinates = wktString.startsWith(" ");
            return readPolygonFromWKTString(wktInput.toString(), pixelCoordinates, geoCoding, productDimension, pm);
        } finally {
            pm.worked(100);
            pm.done();
        }
    }

    private static Polygon readPolygonFromWKTString(String wktString, boolean pixelCoordinates, GeoCoding geoCoding, Dimension productDimension, ProgressMonitor pm) throws Exception {
        final Coordinate[] wktCoordinates = extractCoordinatesFromWKT(wktString);
        pm.worked(50);
        if (pixelCoordinates) {
            return buildPolygonFromPixelCoordinates(wktCoordinates, productDimension, pm);
        } else {
            return buildPolygonFromGeoCoordinates(wktCoordinates, geoCoding, productDimension, pm);
        }
    }

    private static Coordinate[] extractCoordinatesFromWKT(String wkt) throws org.locationtech.jts.io.ParseException {
        final Geometry wktInputGeometry = new WKTReader().read(wkt);
        if (wktInputGeometry.getNumPoints() < 3) {
            throw new IllegalArgumentException("Cannot create a polygon. 3 or more points are required.");
        }
        if (wkt.startsWith("MULTIPOLYGON")) {
            final MultiPolygon mPolygon = (MultiPolygon) wktInputGeometry;
            if (mPolygon.getNumGeometries() > 1) {
                throw new IllegalArgumentException("Subseting by multiple polygons is not supported.");
            }
        }
        return wktInputGeometry.getCoordinates();
    }

    private static Polygon readPolygonFromKMLFile(File file, GeoCoding geoCoding, Dimension productDimension, ProgressMonitor pm) throws Exception {
        pm.beginTask("Loading KML file", 100);
        try (InputStream kmlInputStream = java.nio.file.Files.newInputStream(file.toPath())) {
            return readPolygonFromKMLInputStream(kmlInputStream, geoCoding, productDimension, pm);
        } finally {
            pm.worked(100);
            pm.done();
        }
    }

    private static Polygon readPolygonFromKMZFile(File file, GeoCoding geoCoding, Dimension productDimension, ProgressMonitor pm) throws Exception {
        pm.beginTask("Loading KMZ file", 100);
        try (ZipFile kmzFile = new ZipFile(file)) {
            try (InputStream kmlInputStream = kmzFile.getInputStream(kmzFile.getEntry("overlay.kml"))) {
                return readPolygonFromKMLInputStream(kmlInputStream, geoCoding, productDimension, pm);
            }
        } finally {
            pm.worked(100);
            pm.done();
        }
    }

    private static Polygon readPolygonFromGeoJsonFile(File file, GeoCoding geoCoding, Dimension productDimension, ProgressMonitor pm) throws Exception {
        pm.beginTask("Loading GeoJSON file", 100);
        final GeometryJSON gJson = new GeometryJSON();
        final Polygon vectorFileContent;
        try (InputStream geoJsonInputStream = java.nio.file.Files.newInputStream(file.toPath())) {
            final MultiPolygon multiPolygon = gJson.readMultiPolygon(geoJsonInputStream);
            pm.worked(40);
            if (multiPolygon.getNumGeometries() > 1) {
                throw new IllegalArgumentException("Subseting by multiple polygons is not supported.");
            }
            vectorFileContent = (Polygon) multiPolygon.getGeometryN(0);
            final Coordinate[] vectorFileCoordinates = vectorFileContent.getCoordinates();
            pm.worked(50);
            return buildPolygonFromGeoCoordinates(vectorFileCoordinates, geoCoding, productDimension, pm);
        } finally {
            pm.worked(100);
            pm.done();
        }
    }

    private static Polygon readPolygonFromKMLInputStream(InputStream kmlInputStream, GeoCoding geoCoding, Dimension productDimension, ProgressMonitor pm) throws Exception {
        final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document xmlDocument = new DOMBuilder().build(builder.parse(kmlInputStream));
        int w = 10;
        pm.worked(w);
        final Element xmlRoot = xmlDocument.getRootElement();
        final Namespace xmlNamespace = xmlRoot.getNamespace();
        final Element documentRoot = xmlRoot.getChild("Document", xmlNamespace);
        final List<Coordinate> placemarkCoordinates = new ArrayList<>();
        final List<Element> placemarkElements = documentRoot.getChildren("Placemark", xmlNamespace);
        for (Element placemarkElement : placemarkElements) {
            final String[] coordinates = placemarkElement.getChild("Point", xmlNamespace).getChildText("coordinates", xmlNamespace).split(",");
            placemarkCoordinates.add(new Coordinate(Double.parseDouble(coordinates[0]), Double.parseDouble(coordinates[1])));
            w += 40 / placemarkElements.size();
            pm.worked(w);
        }
        final Coordinate[] vectorFileCoordinates = placemarkCoordinates.toArray(new Coordinate[0]);
        pm.worked(50);
        return buildPolygonFromGeoCoordinates(vectorFileCoordinates, geoCoding, productDimension, pm);
    }

    private static Polygon buildPolygonFromGeoCoordinates(Coordinate[] geoCoordinates, GeoCoding geoCoding, Dimension productDimension, ProgressMonitor pm) {
        final List<Coordinate> pixelCoordinates = new ArrayList<>();
        for (Coordinate geoCoordinate : geoCoordinates) {
            final GeoPos geoPos = new GeoPos(geoCoordinate.getY(), geoCoordinate.getX());
            if (!geoPos.isValid()) {
                throw new IllegalArgumentException("Coordinate: " + geoPos.getLonString() + "," + geoPos.getLatString() + " is not a valid Geo coordinate (Lat,Lon).");
            }
            final PixelPos pixelPos = geoCoding.getPixelPos(geoPos, null);
            pixelCoordinates.add(new Coordinate((int) pixelPos.getX(), (int) pixelPos.getY()));
        }
        if (!geoCoordinates[0].equals(geoCoordinates[geoCoordinates.length - 1])) {
            pixelCoordinates.add(pixelCoordinates.get(0));
        }
        final Coordinate[] polygonCoordinates = pixelCoordinates.toArray(new Coordinate[0]);
        pm.worked(55);
        return buildPolygonFromPixelCoordinates(polygonCoordinates, productDimension, pm);
    }

    private static Polygon buildPolygonFromPixelCoordinates(Coordinate[] polygonCoordinates, Dimension productDimension, ProgressMonitor pm) {
        pm.setSubTaskName("building polygon");
        final GeometryFactory geometryFactory = new GeometryFactory();
        final Polygon polygon = geometryFactory.createPolygon(geometryFactory.createLinearRing(polygonCoordinates), new LinearRing[0]);
        int w = 50;
        final Polygon productPolygon = buildProductPolygonFromDimension(productDimension);
        for (Coordinate polygonCoordinate : polygonCoordinates) {
            if (polygonCoordinate.getX() < 0 || polygonCoordinate.getX() > productDimension.getHeight() - 1 || polygonCoordinate.getY() < 0 || polygonCoordinate.getY() > productDimension.getHeight() - 1) {
                final Polygon intersectionPolygon = (Polygon) productPolygon.intersection(polygon);
                if (intersectionPolygon.isEmpty()) {
                    throw new IllegalArgumentException("Intersection of the polygon with the product returns an empty polygon. Subseting by empty polygon is not supported.");
                }
                return intersectionPolygon;
            }
            w += 50 / polygonCoordinates.length;
            pm.worked(w);
        }
        return polygon;
    }

    private static Polygon buildProductPolygonFromDimension(Dimension productDimension) {
        final Coordinate[] productPolygonCoordinates = new Coordinate[]{new Coordinate(0, 0), new Coordinate(0, productDimension.getHeight() - 1), new Coordinate(productDimension.getWidth() - 1, productDimension.getHeight() - 1), new Coordinate(productDimension.getWidth() - 1, 0), new Coordinate(0, 0),};
        final GeometryFactory geometryFactory = new GeometryFactory();
        return geometryFactory.createPolygon(geometryFactory.createLinearRing(productPolygonCoordinates), new LinearRing[0]);
    }

    private static class SupportedVectorFilesFormat {
        private static final String SHAPEFILE = "shp";
        private static final String KMZ = "kmz";
        private static final String KML = "kml";
        private static final String GEOJSON = "json";
        private static final String WKT = "wkt";
        private static final String TXT = "txt";
        private static final String PLACEMARK_FILE = "placemark";
        private static final String PNX = "pnx";
    }
}
