/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.dataio.netcdf.metadata.profiles.cf;

import org.esa.snap.core.dataio.geocoding.ComponentFactory;
import org.esa.snap.core.dataio.geocoding.ComponentGeoCoding;
import org.esa.snap.core.dataio.geocoding.ForwardCoding;
import org.esa.snap.core.dataio.geocoding.GeoChecks;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.dataio.geocoding.InverseCoding;
import org.esa.snap.core.dataio.geocoding.forward.PixelForward;
import org.esa.snap.core.dataio.geocoding.forward.PixelInterpolatingForward;
import org.esa.snap.core.dataio.geocoding.inverse.PixelQuadTreeInverse;
import org.esa.snap.core.dataio.geocoding.util.RasterUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MapGeoCoding;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.snap.dataio.netcdf.metadata.profiles.hdfeos.HdfEosGeocodingPart;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import org.esa.snap.dataio.netcdf.util.Constants;
import org.esa.snap.dataio.netcdf.util.DimKey;
import org.esa.snap.dataio.netcdf.util.ReaderUtils;
import org.esa.snap.runtime.Config;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.Dimension;
import java.io.IOException;
import java.util.List;

import static org.esa.snap.core.dataio.geocoding.ComponentGeoCoding.SYSPROP_SNAP_PIXEL_CODING_FRACTION_ACCURACY;

public class CfGeocodingPart extends ProfilePartIO {

    private boolean geographicCRS;
    private boolean latLonVarsAddedByGeocoding;
    private boolean xYVarsAddedByGeocoding;
    private String latVarName;
    private String lonVarName;
    private String yVarName;
    private String xVarName;

    @Override
    public void decode(ProfileReadContext ctx, Product p) throws IOException {
        GeoCoding geoCoding = readConventionBasedMapGeoCoding(ctx, p);
        if (geoCoding == null) {
            geoCoding = readPixelBasedGeoCoding(ctx, p);
        }
        // If there is still no geocoding, check special case of netcdf file which was converted
        // from hdf file and has 'StructMetadata.n' element.
        // In this case, the HDF 'elements' were put into a single Netcdf String attribute
        // todo: in fact this has been checked only for MODIS09 HDF-EOS product. Try to further generalize
        if (geoCoding == null && hasHdfMetadataOrigin(ctx.getNetcdfFile().getGlobalAttributes())) {
            hdfDecode(ctx, p);
        }
        if (geoCoding != null) {
            p.setSceneGeoCoding(geoCoding);
        }
    }

    private void hdfDecode(ProfileReadContext ctx, Product p) {
        final CfHdfEosGeoInfoExtractor cfHdfEosGeoInfoExtractor = new CfHdfEosGeoInfoExtractor(
                ctx.getNetcdfFile().getGlobalAttributes());
        cfHdfEosGeoInfoExtractor.extractInfo();

        String projection = cfHdfEosGeoInfoExtractor.getProjection();
        double upperLeftLon = cfHdfEosGeoInfoExtractor.getUlLon();
        double upperLeftLat = cfHdfEosGeoInfoExtractor.getUlLat();

        double lowerRightLon = cfHdfEosGeoInfoExtractor.getLrLon();
        double lowerRightLat = cfHdfEosGeoInfoExtractor.getLrLat();

        HdfEosGeocodingPart.attachGeoCoding(p, upperLeftLon, upperLeftLat, lowerRightLon, lowerRightLat, projection, null);
    }

    private boolean hasHdfMetadataOrigin(List<Attribute> netcdfAttributes) {
        for (Attribute att : netcdfAttributes) {
            if (att.getShortName().startsWith("StructMetadata")) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void preEncode(ProfileWriteContext ctx, Product product) throws IOException {
        final GeoCoding geoCoding = product.getSceneGeoCoding();
        if (geoCoding == null) {
            return;
        }
        geographicCRS = isGeographicCRS(geoCoding);
        final NFileWriteable ncFile = ctx.getNetcdfFileWriteable();
        final boolean latLonPresent = isLatLonPresent(ncFile);
        if (!latLonPresent) {
            latVarName = "lat";
            lonVarName = "lon";
            if (geographicCRS) {
                final GeoPos ul = geoCoding.getGeoPos(new PixelPos(0.5f, 0.5f), null);
                final int w = product.getSceneRasterWidth();
                final int h = product.getSceneRasterHeight();
                final GeoPos br = geoCoding.getGeoPos(new PixelPos(w - 0.5f, h - 0.5f), null);
                addGeographicCoordinateVariables(ncFile, ul, br, latVarName, lonVarName);
            } else {
                addLatLonBands(ncFile, ImageManager.getPreferredTileSize(product), latVarName, lonVarName);
            }
            latLonVarsAddedByGeocoding = true;
        } else {
            if (geographicCRS) {
                // for geographicCRS one-dimensional lat/lons are expected
                // but if lat/Lon bands already exist they are two dimensional
                // so we create new internal lat/lon variables which are one-dimensional
                final GeoPos ul = geoCoding.getGeoPos(new PixelPos(0.5f, 0.5f), null);
                final int w = product.getSceneRasterWidth();
                final int h = product.getSceneRasterHeight();
                final GeoPos br = geoCoding.getGeoPos(new PixelPos(w - 0.5f, h - 0.5f), null);
                latVarName = "lat_intern";
                lonVarName = "lon_intern";
                latLonVarsAddedByGeocoding = true;
                addGeographicCoordinateVariables(ncFile, ul, br, latVarName, lonVarName);
            }
        }
        if (geoCoding instanceof CrsGeoCoding && ! geographicCRS) {
            xVarName = "x";
            yVarName = "y";
            addMetricCoordinateVariables(ncFile,
                                         (CrsGeoCoding) geoCoding,
                                         product.getSceneRasterWidth(), product.getSceneRasterHeight(),
                                         xVarName, yVarName);
            xYVarsAddedByGeocoding = true;
        }
        ctx.setProperty(Constants.Y_FLIPPED_PROPERTY_NAME, false);
    }

    private boolean isLatLonPresent(NFileWriteable ncFile) {
        return ncFile.findVariable("lat") != null && ncFile.findVariable("lon") != null;
    }

    @Override
    public void encode(ProfileWriteContext ctx, Product product) throws IOException {
        if (xYVarsAddedByGeocoding) {
            final int h = product.getSceneRasterHeight();
            final int w = product.getSceneRasterWidth();
            NFileWriteable ncFile = ctx.getNetcdfFileWriteable();
            NVariable yVariable = ncFile.findVariable(yVarName);
            NVariable xVariable = ncFile.findVariable(xVarName);
            final double[] x = new double[w];
            final double[] y = new double[h];
            final MathTransform imageToMapTransform = product.getSceneGeoCoding().getImageToMapTransform();
            for (int j = 0; j < h; j++) {
                final DirectPosition2D p0 = new DirectPosition2D(0, j);
                final DirectPosition2D p = new DirectPosition2D();
                try {
                    imageToMapTransform.transform(p0, p);
                } catch (TransformException e) {
                    throw new IOException(e);
                }
                y[j] = p.y;
            }
            for (int i = 0; i < w; i++) {
                final DirectPosition2D p0 = new DirectPosition2D(i, 0);
                final DirectPosition2D p = new DirectPosition2D();
                try {
                    imageToMapTransform.transform(p0, p);
                } catch (TransformException e) {
                    throw new IOException(e);
                }
                x[i] = p.x;
            }
            yVariable.writeFully(Array.factory(DataType.DOUBLE, new int[]{h}, y));
            xVariable.writeFully(Array.factory(DataType.DOUBLE, new int[]{w}, x));
        }
        if (latLonVarsAddedByGeocoding) {
            final int h = product.getSceneRasterHeight();
            final int w = product.getSceneRasterWidth();

            final GeoCoding geoCoding = product.getSceneGeoCoding();
            final PixelPos pixelPos = new PixelPos();
            final GeoPos geoPos = new GeoPos();

            NFileWriteable ncFile = ctx.getNetcdfFileWriteable();
            NVariable latVariable = ncFile.findVariable(latVarName);
            NVariable lonVariable = ncFile.findVariable(lonVarName);
            if (geographicCRS) {
                final double[] lat = new double[h];
                final double[] lon = new double[w];
                pixelPos.x = 0 + 0.5f;
                for (int y = 0; y < h; y++) {
                    pixelPos.y = y + 0.5f;
                    geoCoding.getGeoPos(pixelPos, geoPos);
                    lat[y] = geoPos.getLat();
                }
                pixelPos.y = 0 + 0.5f;
                for (int x = 0; x < w; x++) {
                    pixelPos.x = x + 0.5f;
                    geoCoding.getGeoPos(pixelPos, geoPos);
                    lon[x] = geoPos.getLon();
                }
                latVariable.writeFully(Array.factory(DataType.DOUBLE, new int[]{h}, lat));
                lonVariable.writeFully(Array.factory(DataType.DOUBLE, new int[]{w}, lon));
            } else {
                final double[] lat = new double[w];
                final double[] lon = new double[w];
                final boolean isYFlipped = (Boolean) ctx.getProperty(Constants.Y_FLIPPED_PROPERTY_NAME);
                for (int y = 0; y < h; y++) {
                    pixelPos.y = y + 0.5f;
                    for (int x = 0; x < w; x++) {
                        pixelPos.x = x + 0.5f;
                        geoCoding.getGeoPos(pixelPos, geoPos);
                        lat[x] = geoPos.getLat();
                        lon[x] = geoPos.getLon();
                    }
                    latVariable.write(0, y, w, 1, isYFlipped, ProductData.createInstance(lat));
                    lonVariable.write(0, y, w, 1, isYFlipped, ProductData.createInstance(lon));
                }
            }
        }
    }

    static boolean isGeographicCRS(final GeoCoding geoCoding) {
        return (geoCoding instanceof CrsGeoCoding || geoCoding instanceof MapGeoCoding) &&
                CRS.equalsIgnoreMetadata(geoCoding.getMapCRS(), DefaultGeographicCRS.WGS84);
    }

    private void addGeographicCoordinateVariables(NFileWriteable ncFile, GeoPos ul, GeoPos br, String latVarName, String lonVarName) throws IOException {
        final NVariable lat = ncFile.addVariable(latVarName, DataType.DOUBLE, null, latVarName);
        lat.addAttribute("units", "degrees_north");
        lat.addAttribute("long_name", "latitude");
        lat.addAttribute("standard_name", "latitude");
        lat.addAttribute(Constants.VALID_MIN_ATT_NAME, br.getLat());
        lat.addAttribute(Constants.VALID_MAX_ATT_NAME, ul.getLat());

        final NVariable lon = ncFile.addVariable(lonVarName, DataType.DOUBLE, null, lonVarName);
        lon.addAttribute("units", "degrees_east");
        lon.addAttribute("long_name", "longitude");
        lon.addAttribute("standard_name", "longitude");
        lon.addAttribute(Constants.VALID_MIN_ATT_NAME, ul.getLon());
        lon.addAttribute(Constants.VALID_MAX_ATT_NAME, br.getLon());
    }

    private void addMetricCoordinateVariables(NFileWriteable ncFile,
                                              CrsGeoCoding geoCoding,
                                              int width, int height,
                                              String xVarName, String yVarName)
            throws IOException {
        final DirectPosition2D ul = new DirectPosition2D(0, 0);
        final DirectPosition2D lr = new DirectPosition2D(width, height);
        final DirectPosition2D ulm = new DirectPosition2D();
        final DirectPosition2D lrm = new DirectPosition2D();
        try {
            geoCoding.getImageToMapTransform().transform(ul, ulm);
            geoCoding.getImageToMapTransform().transform(lr, lrm);
        } catch (TransformException e) {
            throw new IOException(e);
        }

        final NVariable y = ncFile.addVariable(yVarName, DataType.DOUBLE, null, yVarName);
        y.addAttribute("units", "m");
        y.addAttribute("long_name", "y coordinate of projection");
        y.addAttribute("standard_name", "projection_y_coordinate");
        y.addAttribute(Constants.VALID_MIN_ATT_NAME, Math.min(ulm.y, lrm.y));
        y.addAttribute(Constants.VALID_MAX_ATT_NAME, Math.max(ulm.y, lrm.y));

        final NVariable x = ncFile.addVariable(xVarName, DataType.DOUBLE, null, xVarName);
        x.addAttribute("units", "m");
        x.addAttribute("long_name", "x coordinate of projection");
        x.addAttribute("standard_name", "projection_x_coordinate");
        x.addAttribute(Constants.VALID_MIN_ATT_NAME, ulm.x);
        x.addAttribute(Constants.VALID_MAX_ATT_NAME, lrm.x);
    }

    private void addLatLonBands(final NFileWriteable ncFile, Dimension tileSize,
                                String latVarName, String lonVarName) throws IOException {
        final NVariable lat = ncFile.addVariable(latVarName, DataType.DOUBLE, tileSize, "y x");
        lat.addAttribute("units", "degrees_north");
        lat.addAttribute("long_name", "latitude coordinate");
        lat.addAttribute("standard_name", "latitude");

        final NVariable lon = ncFile.addVariable(lonVarName, DataType.DOUBLE, tileSize, "y x");
        lon.addAttribute("units", "degrees_east");
        lon.addAttribute("long_name", "longitude coordinate");
        lon.addAttribute("standard_name", "longitude");
    }

    private static GeoCoding readConventionBasedMapGeoCoding(ProfileReadContext ctx, Product product) {
        final String[] cfConvention_lonLatNames = new String[]{
                Constants.LON_VAR_NAME,
                Constants.LAT_VAR_NAME
        };
        final String[] coardsConvention_lonLatNames = new String[]{
                Constants.LONGITUDE_VAR_NAME,
                Constants.LATITUDE_VAR_NAME
        };
        final String[] cfConvention_lonLatInternNames = new String[]{
                Constants.LON_INTERN_VAR_NAME,
                Constants.LAT_INTERN_VAR_NAME
        };

        Variable[] lonLat;
        List<Variable> variableList = ctx.getNetcdfFile().getVariables();
        lonLat = ReaderUtils.getVariables(variableList, cfConvention_lonLatInternNames);
        if (lonLat == null) {
            lonLat = ReaderUtils.getVariables(variableList, cfConvention_lonLatNames);
        }
        if (lonLat == null) {
            lonLat = ReaderUtils.getVariables(variableList, coardsConvention_lonLatNames);
        }

        if (lonLat != null) {
            final Variable lonVariable = lonLat[0];
            final Variable latVariable = lonLat[1];
            final DimKey rasterDim = ctx.getRasterDigest().getRasterDim();
            if (rasterDim.fitsTo(lonVariable, latVariable)) {
                try {
                    return createConventionBasedMapGeoCoding(lonVariable, latVariable,
                                                             product.getSceneRasterWidth(),
                                                             product.getSceneRasterHeight(), ctx);
                } catch (Exception e) {
                    SystemUtils.LOG.warning("Failed to create NetCDF geo-coding");
                }
            }
        }
        return null;
    }

    private static GeoCoding createConventionBasedMapGeoCoding(Variable lon,
                                                               Variable lat,
                                                               int sceneRasterWidth,
                                                               int sceneRasterHeight,
                                                               ProfileReadContext ctx) throws Exception {
        double pixelX;
        double pixelY;
        double easting;
        double northing;
        double pixelSizeX;
        double pixelSizeY;

        boolean yFlipped;
        Array lonData = lon.read();
        // SPECIAL CASE: check if we have a global geographic lat/lon with lon from 0..360 instead of -180..180
        if (isGlobalShifted180(lonData)) {
            // if this is true, subtract 180 from all longitudes and
            // add a global attribute which will be analyzed when setting up the image(s)
            final List<Variable> variables = ctx.getNetcdfFile().getVariables();
            for (Variable next : variables) {
                next.addAttribute(new Attribute("LONGITUDE_SHIFTED_180", 1));
            }
            for (int i = 0; i < lonData.getSize(); i++) {
                final Index ii = lonData.getIndex().set(i);
                final double theLon = lonData.getDouble(ii) - 180.0;
                lonData.setDouble(ii, theLon);
            }
        }

        double sum = 0;
        for (int i = 0; i < lonData.getSize() - 1; i++) {
            double delta = (lonData.getDouble(i + 1) - lonData.getDouble(i) + 360) % 360;
            sum += delta;
        }
        pixelSizeX = sum / (sceneRasterWidth - 1);

        final Index i0 = lonData.getIndex().set(0);
        easting = lonData.getDouble(i0);

        final int latSize = lat.getShape(0);
        final Array latData = lat.read();
        final Index j0 = latData.getIndex().set(0);
        final Index j1 = latData.getIndex().set(latSize - 1);
        pixelSizeY = (latData.getDouble(j1) - latData.getDouble(j0)) / (sceneRasterHeight - 1);

        pixelX = 0.5f;
        pixelY = 0.5f;

        if (pixelSizeY < 0) {
            pixelSizeY = -pixelSizeY;
            yFlipped = false;
            northing = latData.getDouble(latData.getIndex().set(0));
        } else {
            yFlipped = true;
            northing = latData.getDouble(latData.getIndex().set(latSize - 1));
        }

        if (pixelSizeX <= 0 || pixelSizeY <= 0) {
            return null;
        }
        ctx.setProperty(Constants.Y_FLIPPED_PROPERTY_NAME, yFlipped);
        return new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                                sceneRasterWidth, sceneRasterHeight,
                                easting, northing,
                                pixelSizeX, pixelSizeY,
                                pixelX, pixelY);
    }

    static boolean isGlobalShifted180(Array lonData) {
        // Idea: lonData values shall closely match [0,360] interval:
        // - first value of lonData shall be larger or equal 0.0, and less or equal 0.0 + lonDataInterval
        // - last value of lonData shall be smaller or equal 360.0, and larger or equal 360.0 - lonDataInterval
        // e.g. lonDataInterval=0.75, lonData={0.0, 0.75, 1.5,..,359.25} --> true
        // e.g. lonDataInterval=0.75, lonData={0.75, 1.5, 2.25,..,360.0} --> true
        // e.g. lonDataInterval=0.75, lonData={1.0, 1.75, 2.5,..,360.25} --> false
        final Index i0 = lonData.getIndex().set(0);
        final Index i1 = lonData.getIndex().set(1);
        final Index iN = lonData.getIndex().set((int) lonData.getSize() - 1);
        double lonDelta = (lonData.getDouble(i1) - lonData.getDouble(i0));

        final double firstValue = lonData.getDouble(0);
        final double lastValue = lonData.getDouble(iN);
        return (firstValue >= 0.0 && firstValue <= lonDelta &&
                lastValue >= 360.0 - lonDelta && lastValue <= 360.0);
    }

    private static GeoCoding readPixelBasedGeoCoding(ProfileReadContext ctx, Product product) throws IOException {
        Band lonBand = product.getBand(Constants.LON_INTERN_VAR_NAME);
        if (lonBand == null) {
            lonBand = product.getBand(Constants.LON_VAR_NAME);
        }
        if (lonBand == null) {
            lonBand = product.getBand(Constants.LONGITUDE_VAR_NAME);
        }
        Band latBand = product.getBand(Constants.LAT_INTERN_VAR_NAME);
        if (latBand == null) {
            latBand = product.getBand(Constants.LAT_VAR_NAME);
        }
        if (latBand == null) {
            latBand = product.getBand(Constants.LATITUDE_VAR_NAME);
        }
        if (latBand == null || lonBand == null) {
            return null;
        }

        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();

        final NetcdfFile netcdfFile = ctx.getNetcdfFile();
        final Variable lonVar = netcdfFile.findVariable(lonBand.getName());
        final Variable latVar = netcdfFile.findVariable(latBand.getName());
        if (lonVar == null || latVar == null)      {
            return null;
        }

        final double[] longitudes = readVarAsDoubleArray(lonVar);
        final double[] latitudes = readVarAsDoubleArray(latVar);

        final double resolutionInKm = RasterUtils.computeResolutionInKm(longitudes, latitudes, width, height);

        final GeoRaster geoRaster = new GeoRaster(longitudes, latitudes, lonBand.getName(), latBand.getName(),
                                                  width, height, resolutionInKm);

        final boolean fractionalAccuracy = Config.instance().preferences().getBoolean(SYSPROP_SNAP_PIXEL_CODING_FRACTION_ACCURACY, false);
        final ForwardCoding forward;
        if (fractionalAccuracy) {
            forward = ComponentFactory.getForward(PixelInterpolatingForward.KEY);
        } else {
            forward = ComponentFactory.getForward(PixelForward.KEY);
        }
        final InverseCoding inverse;
        if (fractionalAccuracy) {
            inverse = ComponentFactory.getInverse(PixelQuadTreeInverse.KEY_INTERPOLATING);
        } else {
            inverse = ComponentFactory.getInverse(PixelQuadTreeInverse.KEY);
        }

        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forward, inverse, GeoChecks.ANTIMERIDIAN);
        geoCoding.initialize();
        return geoCoding;
    }

    static double[] readVarAsDoubleArray(Variable lonVar) throws IOException {
        final Array lonArray = lonVar.read();
        return (double[]) lonArray.get1DJavaArray(DataType.DOUBLE);
    }
}
