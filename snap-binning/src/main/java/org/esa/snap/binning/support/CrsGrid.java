package org.esa.snap.binning.support;

import com.bc.ceres.core.ProgressMonitor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.ParseException;
import org.esa.snap.binning.MosaickingGrid;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.PlainFeatureFactory;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.common.reproject.ReprojectionOp;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.FeatureUtils;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.TransformException;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by marcoz on 29.02.16.
 */
public class CrsGrid implements MosaickingGrid {

    private static final int LON_DIM = 0;
    private static final int LAT_DIM = 1;

    private final CoordinateReferenceSystem crs;
    private final int numRows;
    private final int numCols;
    private final double pixelSize;
    private final double pixelSizeX;
    private final GeometryFactory geometryFactory;

    private final CrsGeoCoding crsGeoCoding;
    private final double easting;
    private final double northing;
    private Geometry targetGeometryInCrsCoordinates = null;

    public CrsGrid(int numRowsGlobal, String crsCode) {
        try {
            double pixelSizeRatio;
            if (crsCode.contains(",")) {
                String[] parts = crsCode.split(",");
                this.crs = CRS.decode(parts[0], true);
                if (parts[1].startsWith("POLYGON")) {
                    String targetWKT = crsCode.substring(crsCode.indexOf(',') + 1);
                    this.targetGeometryInCrsCoordinates = new WKTReader().read(targetWKT);
                    pixelSizeRatio = 1.0;
                } else {
                    pixelSizeRatio = Double.parseDouble(parts[1]);
                }
            } else {
                this.crs = CRS.decode(crsCode, true);
                pixelSizeRatio = 1.0;
            }
            Envelope envelopeCRS = CRS.getEnvelope(this.crs);
            System.out.println("envelopeCRS = " + envelopeCRS);
            String units = this.crs.getCoordinateSystem().getAxis(LON_DIM).getUnit().toString();
            if (!units.equalsIgnoreCase("m") && !units.equalsIgnoreCase("meter")) {
                this.pixelSize = 180.0D / numRowsGlobal;
            } else {
                Ellipsoid ellipsoid = CRS.getEllipsoid(this.crs);
                double semiMinorAxis = ellipsoid.getSemiMinorAxis();
                double meterSpanGlobal = semiMinorAxis * Math.PI;
                this.pixelSize = meterSpanGlobal / numRowsGlobal;
            }
            this.pixelSizeX = this.pixelSize / pixelSizeRatio;
            System.out.println("pixelSize = " + this.pixelSize + " [" + units + "]");
            this.numCols = (int)(envelopeCRS.getSpan(LON_DIM) / this.pixelSizeX);
            this.easting = envelopeCRS.getMinimum(LON_DIM);
            this.numRows = (int)(envelopeCRS.getSpan(LAT_DIM) / this.pixelSize);
            this.northing = envelopeCRS.getMaximum(LAT_DIM);
            this.crsGeoCoding = new CrsGeoCoding(this.crs, this.numCols, this.numRows, this.easting, this.northing, this.pixelSizeX, this.pixelSize, 0.0, 0.0);
        } catch (FactoryException | TransformException | ParseException e) {
            throw new IllegalArgumentException("Can not create crs for:" + crsCode, e);
        }

        this.geometryFactory = new GeometryFactory();
    }

    /**
     * Constructor for metric grids and for geographic grids with non-rectangular pixels.
     * <p/>
     * WKT for projections usually support meters and degrees as units but not pixels.
     * And there seem to be no example how to represent different resolutions in north and east.
     * Therefore, the initial idea to use WKT to define the raster has been dropped.
     * Instead, pixelsize is the size in y dimension, and EPSG:code[,pixelsizeydivx] contains the divisor,
     * e.g. 0.7 suitable for ~45 degrees north.
     * <p/>
     * 60 and EPSG:32636 for an UTM grid in zone 36N in 60m.
     * 0.1 and EPSG:4326 for a WGS84 geographic grid with 0.1 degree pixels
     * any value and EPSG:4326,0.7 for a WGS84 geographic grid with rectangular pixels
     * 100 and EPSG:3995,POLYGON((-300 -1200,9700 ...)) for a polar stereographic projection subset of 10000 m width ...
     * @param pixelSize
     * @param crsCode
     */
    public CrsGrid(double pixelSize, String crsCode) {
        try {
            if (crsCode.contains(",")) {
                String[] parts = crsCode.split(",");
                this.crs = CRS.decode(parts[0], true);
                if (parts[1].startsWith("POLYGON")) {
                    this.pixelSizeX = pixelSize;
                    String targetWKT = crsCode.substring(crsCode.indexOf(',') + 1);
                    this.targetGeometryInCrsCoordinates = new WKTReader().read(targetWKT);
                } else {
                    this.pixelSizeX = pixelSize / Double.parseDouble(parts[1]);
                }
            } else {
                this.crs = CRS.decode(crsCode, true);
                this.pixelSizeX = pixelSize;
            }
            Envelope envelopeCRS = CRS.getEnvelope(this.crs);
            System.out.println("envelopeCRS = " + envelopeCRS);
            // The envelope may be odd regarding any assumed resolution, minX for UTM is 166021.4430960772
            // A given target geometry may expand the nominal envelope.
            final double minLon;
            final double minLat;
            final double maxLon;
            final double maxLat;
            if (this.targetGeometryInCrsCoordinates != null) {
                final org.locationtech.jts.geom.Envelope providedEnvelope =
                        this.targetGeometryInCrsCoordinates.getEnvelopeInternal();
                System.out.println("targetEnvelope = " + providedEnvelope);
                minLon = providedEnvelope.getMinX();
                minLat = providedEnvelope.getMinY();
                maxLon = providedEnvelope.getMaxX();
                maxLat = providedEnvelope.getMaxY();
            } else {
                minLon = envelopeCRS.getMinimum(LON_DIM);
                minLat = envelopeCRS.getMinimum(LAT_DIM);
                maxLon = envelopeCRS.getMaximum(LON_DIM);
                maxLat = envelopeCRS.getMaximum(LAT_DIM);
            }
            envelopeCRS = new CRSEnvelope(crsCode,
                                          Math.floor(minLon / this.pixelSizeX) * this.pixelSizeX,
                                          Math.floor(minLat / pixelSize) * pixelSize,
                                          Math.ceil(maxLon / this.pixelSizeX) * this.pixelSizeX,
                                          Math.ceil(maxLat / pixelSize) * pixelSize);
            System.out.println("gridded envelopeCRS = " + envelopeCRS);
            String units = this.crs.getCoordinateSystem().getAxis(LON_DIM).getUnit().toString();
            this.pixelSize = pixelSize;
            System.out.println("pixelSize = " + this.pixelSize + " [" + units + "]");
            System.out.println("pixelSizeX= " + this.pixelSizeX + " [" + units + "]");
            this.numCols = (int)Math.round(envelopeCRS.getSpan(LON_DIM) / this.pixelSizeX);
            this.easting = envelopeCRS.getMinimum(LON_DIM);
            this.numRows = (int)Math.round(envelopeCRS.getSpan(LAT_DIM) / this.pixelSize);
            this.northing = envelopeCRS.getMaximum(LAT_DIM);
            this.crsGeoCoding = new CrsGeoCoding(this.crs, this.numCols, this.numRows, this.easting, this.northing, this.pixelSizeX, this.pixelSize, 0.0, 0.0);
        } catch (FactoryException | TransformException | ParseException e) {
            throw new IllegalArgumentException("Can not create crs for:" + crsCode, e);
        }

        this.geometryFactory = new GeometryFactory();
    }

    public long getBinIndex(double lat, double lon) {
        PixelPos pixelPos = this.crsGeoCoding.getPixelPos(new GeoPos(lat, lon), null);
        long x = (long)pixelPos.getX();
        long y = (long)pixelPos.getY();
        return y * this.numCols + x;
    }

    public int getRowIndex(long bin) {
        long x = bin % this.numCols;
        return (int)((bin - x) / this.numCols);
    }

    public long getNumBins() {
        return this.numCols * (long)this.numRows;
    }

    public int getNumRows() {
        return this.numRows;
    }

    public int getNumCols(int row) {
        return this.numCols;
    }

    public long getFirstBinIndex(int row) {
        return (long)row * this.numCols;
    }

    public double getCenterLat(int row) {
        GeoPos geoPos = this.crsGeoCoding.getGeoPos(new PixelPos(0.5D, row + 0.5D), null);
        return geoPos.getLat();
    }

    public double[] getCenterLatLon(long bin) {
        int x = (int)(bin % this.numCols);
        int y = (int)((bin - x) / this.numCols);
        GeoPos geoPos = this.crsGeoCoding.getGeoPos(new PixelPos(x + 0.5D, y + 0.5D), null);
        return new double[]{geoPos.getLat(), geoPos.getLon()};
    }

    public GeoPos getCenterPos(long bin) {
        return crsGeoCoding.getGeoPos(new PixelPos(  bin % numCols + 0.5, bin / numCols + 0.5), null);
    }

    public Product reprojectToGrid(Product sourceProduct) {
        Product gridProduct = new Product("ColocationGrid", "ColocationGrid", this.numCols, this.numRows);
        gridProduct.setSceneGeoCoding(this.crsGeoCoding);
        ReprojectionOp repro = new ReprojectionOp();
        repro.setParameter("resampling", "Nearest");
        repro.setParameter("includeTiePointGrids", false);
        Dimension tileSize = ImageManager.getPreferredTileSize(sourceProduct);
        repro.setParameter("tileSizeX", tileSize.width);
        repro.setParameter("tileSizeY", tileSize.height);

        repro.setSourceProduct("collocateWith", gridProduct);
        repro.setSourceProduct("source", sourceProduct);
        Product targetProduct = repro.getTargetProduct();
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        return targetProduct;
    }

    public Geometry getImageGeometry(Geometry geometry) {
        Product gridProduct = new Product("ColocationGrid", "ColocationGrid", this.numCols, this.numRows);
        gridProduct.setSceneGeoCoding(this.crsGeoCoding);
        RasterDataNode rdn = gridProduct.addBand("dummy", ProductData.TYPE_UINT8);
        if (this.targetGeometryInCrsCoordinates != null) {
            try {
                AffineTransform i2mTransform = rdn.getImageToModelTransform();
                i2mTransform.invert();
                GeometryCoordinateSequenceTransformer transformer = new GeometryCoordinateSequenceTransformer();
                transformer.setMathTransform(new AffineTransform2D(i2mTransform));
                Geometry targetGeometryInPixels = transformer.transform(this.targetGeometryInCrsCoordinates);
                System.out.println("target subset of CRS grid: " + targetGeometryInPixels);
                return targetGeometryInPixels;
            } catch (TransformException | NoninvertibleTransformException e) {
                throw new IllegalArgumentException("Could not invert model-to-image transformation.", e);
            }
        }
        SimpleFeatureType wktFeatureType = PlainFeatureFactory.createDefaultFeatureType(DefaultGeographicCRS.WGS84);
        ListFeatureCollection featureCollection = new ListFeatureCollection(wktFeatureType);
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(wktFeatureType);
        SimpleFeature wktFeature = featureBuilder.buildFeature("ID1");
        wktFeature.setDefaultGeometry(geometry);
        featureCollection.add(wktFeature);
        FeatureCollection<SimpleFeatureType, SimpleFeature> productFeatures = FeatureUtils.clipFeatureCollectionToProductBounds(featureCollection, gridProduct,
                                                                                                                                null, ProgressMonitor.NULL);
        FeatureIterator<SimpleFeature> features = productFeatures.features();
        if (!features.hasNext()) {
            return null;
        } else {
            SimpleFeature simpleFeature = features.next();
            Geometry clippedGeometry = (Geometry)simpleFeature.getDefaultGeometry();

            try {
                AffineTransform i2mTransform = rdn.getImageToModelTransform();
                i2mTransform.invert();
                GeometryCoordinateSequenceTransformer transformer = new GeometryCoordinateSequenceTransformer();
                transformer.setMathTransform(new AffineTransform2D(i2mTransform));
                return transformer.transform(clippedGeometry);
            } catch (TransformException | NoninvertibleTransformException e) {
                throw new IllegalArgumentException("Could not invert model-to-image transformation.", e);
            }
        }
    }

    /** returns rectangle with integer bounds, allows for 1% margin to snap to grid,
     * else snap to outside border of partially included column or row
     */
    public Rectangle getBounds(Geometry pixelGeometry) {
        org.locationtech.jts.geom.Envelope envelopeInternal = pixelGeometry.getEnvelopeInternal();
        int minX = (int)Math.floor(envelopeInternal.getMinX() + 0.01);
        int minY = (int)Math.floor(envelopeInternal.getMinY() + 0.01);
        int maxX = (int)Math.ceil(envelopeInternal.getMaxX() - 0.01);
        int maxY = (int)Math.ceil(envelopeInternal.getMaxY() - 0.01);
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    public GeoCoding getGeoCoding(Rectangle outputRegion) {
        try {
            return new CrsGeoCoding(this.crs, outputRegion.width, outputRegion.height, this.easting + this.pixelSizeX * outputRegion.x, this.northing - this.pixelSize * outputRegion.y, this.pixelSizeX, this.pixelSize, 0.0, 0.0);
        } catch (TransformException | FactoryException e) {
            throw new IllegalArgumentException("Can not create geocoding for crs.", e);
        }
    }

    public Rectangle[] getDataSliceRectangles(Geometry sourceProductGeometry, Dimension tileSize) {
        Geometry imageGeometry = getImageGeometry(sourceProductGeometry);
        if (imageGeometry == null) {
            return new Rectangle[0];
        } else {
            Rectangle productBoundingBox = getBounds(imageGeometry);
            Rectangle gridAlignedBoundingBox = MosaickingGrid.alignToTileGrid(productBoundingBox, tileSize);
            int xStart = gridAlignedBoundingBox.x / tileSize.width;
            int yStart = gridAlignedBoundingBox.y / tileSize.height;
            int width = gridAlignedBoundingBox.width / tileSize.width;
            int height = gridAlignedBoundingBox.height / tileSize.height;
            List<Rectangle> rectangles = new ArrayList<>((int)(this.numCols * (long)this.numRows / (tileSize.width * tileSize.height)));

            for(int y = yStart; y < yStart + height; ++y) {
                for(int x = xStart; x < xStart + width; ++x) {
                    Rectangle tileRect = new Rectangle(x * tileSize.width, y * tileSize.height, tileSize.width, tileSize.height);
                    Geometry tileGeometry = getTileGeometry(tileRect);
                    Geometry intersection = imageGeometry.intersection(tileGeometry);
                    if (!intersection.isEmpty() && intersection.getDimension() == 2) {
                        System.out.println("tileRect = " + tileRect);
                        rectangles.add(productBoundingBox.intersection(tileRect));
                    }
                }
            }

            System.out.println("rectangles = " + rectangles.size());
            return rectangles.toArray(new Rectangle[0]);
        }
    }


    private Geometry getTileGeometry(Rectangle rect) {
        return this.geometryFactory.toGeometry(new org.locationtech.jts.geom.Envelope(rect.x, (rect.x + rect.width), rect.y, (rect.y + rect.height)));
    }
}
