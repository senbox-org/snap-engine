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

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoCodingFactory;
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
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import java.awt.Dimension;
import java.io.IOException;
import java.util.List;

public class CfGeocodingPart extends ProfilePartIO {

    private boolean geographicCRS;
    private boolean latLonVarsAddedByGeocoding;
    private String latVarName;
    private String lonVarName;

    @Override
    public void decode(ProfileReadContext ctx, Product p) throws IOException {
        GeoCoding geoCoding = readConventionBasedMapGeoCoding(ctx, p);
        if (geoCoding == null) {
            geoCoding = readPixelGeoCoding(p);
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

    private void hdfDecode(ProfileReadContext ctx, Product p) throws IOException {
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
        ctx.setProperty(Constants.Y_FLIPPED_PROPERTY_NAME, false);
    }

    private boolean isLatLonPresent(NFileWriteable ncFile) {
        return ncFile.findVariable("lat") != null && ncFile.findVariable("lon") != null;
    }

    @Override
    public void encode(ProfileWriteContext ctx, Product product) throws IOException {
        if (!latLonVarsAddedByGeocoding) {
            return;
        }
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
            latVariable.writeFully(Array.factory(lat));
            lonVariable.writeFully(Array.factory(lon));
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
                next.getAttributes().add(new Attribute("LONGITUDE_SHIFTED_180", 1));
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

    private static GeoCoding readPixelGeoCoding(Product product) throws IOException {
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
        if (latBand != null && lonBand != null) {
            return GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, latBand.getValidMaskExpression(), 5);
        }
        return null;
    }

}
