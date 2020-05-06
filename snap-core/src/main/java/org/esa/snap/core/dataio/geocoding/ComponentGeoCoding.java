package org.esa.snap.core.dataio.geocoding;

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.dataio.geocoding.util.RasterUtils;
import org.esa.snap.core.datamodel.AbstractGeoCoding;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoCodingFactory;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.Scene;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.stream.IntStream;

public class ComponentGeoCoding extends AbstractGeoCoding {

    private static final GeoPos INVALID_GEO_POS = new GeoPos(Double.NaN, Double.NaN);
    private static final PixelPos INVALID_PIXEL_POS = new PixelPos(Double.NaN, Double.NaN);

    private final ForwardCoding forwardCoding;
    private final InverseCoding inverseCoding;
    private final GeoRaster geoRaster;
    private final GeoChecks geoChecks;

    private boolean isInitialized;
    private boolean isCrossingAntiMeridian;

    /**
     * Constructs a GeoCoding with given GeoRaster, ForwardCoding and InverseCoding. No geoChecks will be performed during initialize phase.
     * Defaults to WGS84 CRS. Forward and/or Inverse coding can be null.
     *
     * @param geoRaster     the GeoRaster
     * @param forwardCoding the ForwardCoding, can be null
     * @param inverseCoding the InverseCoding, can be null
     */
    public ComponentGeoCoding(GeoRaster geoRaster, ForwardCoding forwardCoding, InverseCoding inverseCoding) {
        this(geoRaster, forwardCoding, inverseCoding, GeoChecks.NONE, DefaultGeographicCRS.WGS84);
    }

    /**
     * Constructs a GeoCoding with given GeoRaster, ForwardCoding, InverseCoding and GeoCheck definition to be executed during initialize phase.
     * Defaults to WGS84 CRS. Forward and/or Inverse coding can be null.
     *
     * @param geoRaster     the GeoRaster
     * @param forwardCoding the ForwardCoding, can be null
     * @param inverseCoding the InverseCoding, can be null
     * @param geoChecks     definition of GeoChecks to be executed during initialization
     */
    public ComponentGeoCoding(GeoRaster geoRaster, ForwardCoding forwardCoding, InverseCoding inverseCoding, GeoChecks geoChecks) {
        this(geoRaster, forwardCoding, inverseCoding, geoChecks, DefaultGeographicCRS.WGS84);
    }

    /**
     * Constructs a GeoCoding with given GeoRaster, ForwardCoding, InverseCoding and CRS. No GeoChecks will be performed during initialize phase.
     * Forward and/or Inverse coding can be null.
     *
     * @param geoRaster     the GeoRaster
     * @param forwardCoding the ForwardCoding, can be null
     * @param inverseCoding the InverseCoding, can be null
     * @param geoCRS        the CRS
     */
    public ComponentGeoCoding(GeoRaster geoRaster, ForwardCoding forwardCoding, InverseCoding inverseCoding, CoordinateReferenceSystem geoCRS) {
        this(geoRaster, forwardCoding, inverseCoding, GeoChecks.NONE, geoCRS);
    }

    /**
     * Constructs a GeoCoding with given GeoRaster, ForwardCoding, InverseCoding, GeoChecks to be performed during initialization and CRS.
     * Forward and/or Inverse coding can be null.
     *
     * @param geoRaster     the GeoRaster
     * @param forwardCoding the ForwardCoding, can be null
     * @param inverseCoding the InverseCoding, can be null
     * @param geoChecks     definition of GeoChecks to be executed during initialization
     * @param geoCRS        the CRS
     */
    public ComponentGeoCoding(GeoRaster geoRaster, ForwardCoding forwardCoding, InverseCoding inverseCoding,
                              GeoChecks geoChecks, CoordinateReferenceSystem geoCRS) {
        super(geoCRS);
        this.forwardCoding = forwardCoding;
        this.inverseCoding = inverseCoding;
        this.geoRaster = geoRaster;
        this.geoChecks = geoChecks;

        isInitialized = false;
        isCrossingAntiMeridian = false;
    }

    @Override
    public boolean isCrossingMeridianAt180() {
        if (!isInitialized) {
            throw new IllegalStateException("Geocoding is not initialized.");
        }

        return isCrossingAntiMeridian;
    }

    @Override
    public boolean canGetPixelPos() {
        return inverseCoding != null;
    }

    @Override
    public boolean canGetGeoPos() {
        return forwardCoding != null;
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        if (inverseCoding == null) {
            return INVALID_PIXEL_POS;
        }
        return inverseCoding.getPixelPos(geoPos, pixelPos);
    }

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        if (forwardCoding == null) {
            return INVALID_GEO_POS;
        }

        return forwardCoding.getGeoPos(pixelPos, geoPos);
    }

    /**
     * Transfers the geo-coding of the {@link Scene srcScene} to the {@link Scene destScene} with respect to the given
     * {@link ProductSubsetDef subsetDef}.
     *
     * @param srcScene  the source scene
     * @param destScene the destination scene
     * @param subsetDef the definition of the subset, may be <code>null</code>
     * @return true, if the geo-coding could be transferred.
     */
    public boolean transferGeoCoding(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef) {
        transferRequiredRasters(srcScene, destScene, subsetDef);

        if (subsetDef == null || subsetDef.isEntireProductSelected()) {
            destScene.setGeoCoding(clone());
            return true;
        }

        final String lonVariableName = this.geoRaster.getLonVariableName();
        final String latVariableName = this.geoRaster.getLatVariableName();

        final GeoRaster geoRaster = calculateGeoRaster(destScene, subsetDef, lonVariableName, latVariableName);
        ForwardCoding forwardCoding = null;
        if (this.forwardCoding != null) {
            forwardCoding = ComponentFactory.getForward(this.forwardCoding.getKey());
        }
        InverseCoding inverseCoding = null;
        if (this.inverseCoding != null) {
            inverseCoding = ComponentFactory.getInverse(this.inverseCoding.getKey());
        }

        final CoordinateReferenceSystem geoCRS = getGeoCRS();
        final ComponentGeoCoding destGeoCoding = new ComponentGeoCoding(geoRaster, forwardCoding, inverseCoding, geoChecks, geoCRS);
        destGeoCoding.initialize();
        destScene.setGeoCoding(destGeoCoding);

        return true;
    }

    // package access for testing only tb 2020-02-20
    GeoRaster cloneGeoRaster(String lonVariableName, String latVariableName) {
        return new GeoRaster(this.geoRaster.getLongitudes(), this.geoRaster.getLatitudes(),
                             lonVariableName, latVariableName,
                             this.geoRaster.getRasterWidth(), this.geoRaster.getRasterHeight(),
                             this.geoRaster.getSceneWidth(), this.geoRaster.getSceneHeight(),
                             this.geoRaster.getRasterResolutionInKm(),
                             this.geoRaster.getOffsetX(), this.geoRaster.getOffsetY(),
                             this.geoRaster.getSubsamplingX(), this.geoRaster.getSubsamplingY());
    }

    // package access for testing only tb 2020-02-20
    void transferRequiredRasters(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef) {
        final String lonVarName = geoRaster.getLonVariableName();
        final String latVarName = geoRaster.getLatVariableName();

        final Product srcProduct = srcScene.getProduct();
        final Product destProduct = destScene.getProduct();

        final RasterDataNode srcLonRaster = srcProduct.getRasterDataNode(lonVarName);
        if (srcLonRaster instanceof TiePointGrid) {
            TiePointGrid destTpgLon = destProduct.getTiePointGrid(lonVarName);
            if (destTpgLon == null) {
                final TiePointGrid srcTpgLon = srcProduct.getTiePointGrid(lonVarName);
                destTpgLon = TiePointGrid.createSubset(srcTpgLon, subsetDef);
                destProduct.addTiePointGrid(destTpgLon);
            }
            TiePointGrid destTpgLat = destProduct.getTiePointGrid(latVarName);
            if (destTpgLat == null) {
                final TiePointGrid srcTpgLat = srcProduct.getTiePointGrid(latVarName);
                destTpgLat = TiePointGrid.createSubset(srcTpgLat, subsetDef);
                destProduct.addTiePointGrid(destTpgLat);
            }
        } else {
            Band destLonBand = destProduct.getBand(lonVarName);
            if (destLonBand == null) {
                final Band srcLonBand = srcProduct.getBand(lonVarName);
                destLonBand = GeoCodingFactory.createSubset(srcLonBand, destScene, subsetDef);
                destProduct.addBand(destLonBand);
            }
            Band destLatBand = destProduct.getBand(latVarName);
            if (destLatBand == null) {
                final Band srcLatBand = srcProduct.getBand(latVarName);
                destLatBand = GeoCodingFactory.createSubset(srcLatBand, destScene, subsetDef);
                destProduct.addBand(destLatBand);
            }
        }
    }

    @Override
    public void dispose() {
        isInitialized = false;
        if (forwardCoding != null) {
            forwardCoding.dispose();
        }
        if (inverseCoding != null) {
            inverseCoding.dispose();
        }
        if (geoRaster != null) {
            geoRaster.dispose();
        }
    }

    /**
     * Gets the datum, the reference point or surface against which {@link GeoPos} measurements are made.
     *
     * @return the datum
     * @deprecated use the datum of the associated {@link #getMapCRS() map CRS}.
     */
    @Override
    @Deprecated
    public Datum getDatum() {
        throw new NotImplementedException();
    }

    @Override
    public GeoCoding clone() {
        final GeoRaster geoRaster = cloneGeoRaster(this.geoRaster.getLonVariableName(), this.geoRaster.getLatVariableName());

        ForwardCoding cloneForward = null;
        if (forwardCoding != null) {
            cloneForward = forwardCoding.clone();
        }

        InverseCoding cloneInverse = null;
        if (inverseCoding != null) {
            cloneInverse = inverseCoding.clone();
        }
        final ComponentGeoCoding clone = new ComponentGeoCoding(geoRaster, cloneForward, cloneInverse, geoChecks);

        clone.isInitialized = this.isInitialized;

        return clone;
    }

    @Override
    public boolean canClone() {
        return true;
    }

    public void initialize() {
        PixelPos[] poleLocations = new PixelPos[0];

        if (geoChecks != GeoChecks.NONE) {
            isCrossingAntiMeridian = RasterUtils.containsAntiMeridian(geoRaster.getLongitudes(), geoRaster.getRasterWidth());
            if (isCrossingAntiMeridian && geoChecks == GeoChecks.POLES) {
                poleLocations = RasterUtils.getPoleLocations(geoRaster);
            }
        }

        if (forwardCoding != null) {
            forwardCoding.initialize(geoRaster, isCrossingAntiMeridian, poleLocations);
        }
        if (inverseCoding != null) {
            inverseCoding.initialize(geoRaster, isCrossingAntiMeridian, poleLocations);
        }

        isInitialized = true;
    }

    public GeoChecks getGeoChecks() {
        return geoChecks;
    }

    public ForwardCoding getForwardCoding() {
        return forwardCoding;
    }

    public InverseCoding getInverseCoding() {
        return inverseCoding;
    }

    public GeoRaster getGeoRaster() {
        return geoRaster;
    }

    private GeoRaster calculateGeoRaster(Scene destScene, ProductSubsetDef subsetDef, String lonVariableName, String latVariableName) {
        GeoRaster geoRaster;
        final Product destProduct = destScene.getProduct();
        final RasterDataNode lonRaster = destProduct.getRasterDataNode(lonVariableName);
        final RasterDataNode latRaster = destProduct.getRasterDataNode(latVariableName);
        final double[] longitudes;
        final double[] latitudes;
        final int gridWidth;
        final int gridHeight;
        final double offsetX;
        final double offsetY;
        final double subsamplingX;
        final double subsamplingY;
        if (lonRaster instanceof TiePointGrid) {
            final TiePointGrid lonTPG = (TiePointGrid) lonRaster;
            final TiePointGrid latTPG = (TiePointGrid) latRaster;

            gridWidth = lonTPG.getGridWidth();
            gridHeight = lonTPG.getGridHeight();

            final float[] lons = (float[]) lonTPG.getGridData().getElems();
            longitudes = IntStream.range(0, lons.length).mapToDouble(i -> lons[i]).toArray();

            final float[] lats = (float[]) latTPG.getGridData().getElems();
            latitudes = IntStream.range(0, lats.length).mapToDouble(i -> lats[i]).toArray();

            offsetX = lonTPG.getOffsetX();
            offsetY = lonTPG.getOffsetY();
            subsamplingX = lonTPG.getSubSamplingX();
            subsamplingY = lonTPG.getSubSamplingY();
        } else {
            gridWidth = lonRaster.getRasterWidth();
            gridHeight = lonRaster.getRasterHeight();

            longitudes = lonRaster.getSourceImage().getImage(0).getData().getPixels(0, 0, gridWidth, gridHeight, new double[gridWidth * gridHeight]);
            latitudes = latRaster.getSourceImage().getImage(0).getData().getPixels(0, 0, gridWidth, gridHeight, new double[gridWidth * gridHeight]);

            offsetX = 0.5;
            offsetY = 0.5;
            subsamplingX = 1.0;
            subsamplingY = 1.0;
        }

        geoRaster = new GeoRaster(longitudes, latitudes, lonVariableName, latVariableName,
                                  gridWidth, gridHeight, destScene.getRasterWidth(), destScene.getRasterHeight(),
                                  this.geoRaster.getRasterResolutionInKm() * subsetDef.getSubSamplingX(),
                                  offsetX, offsetY, subsamplingX, subsamplingY);
        return geoRaster;
    }
}
