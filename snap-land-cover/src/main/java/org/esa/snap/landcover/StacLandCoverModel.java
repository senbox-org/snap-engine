/*
 * Copyright (C) 2024 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.snap.landcover;

import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.stac.StacClient;
import org.esa.stac.StacItem;

import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class StacLandCoverModel implements LandCoverModel {

    protected final Resampling resampling;

    private final StacClient client;
    protected final LandCoverModelDescriptor descriptor;
    protected String aoiWKT;
    protected FileLandCoverTile[] tileList = null;

    protected static final ProductReaderPlugIn productReaderPlugIn = new GeoTiffProductReaderPlugIn();

    public StacLandCoverModel(final LandCoverModelDescriptor descriptor, final Resampling resamplingMethod) {

        this.descriptor = descriptor;
        this.resampling = resamplingMethod;
        this.client = new StacClient(descriptor.getArchiveUrl().toString());
    }

    public void dispose() {
        if(tileList != null) {
            for (FileLandCoverTile tile : tileList) {
                if (tile != null) {
                    tile.dispose();
                }
            }
        }
    }

    /**
     * Inform landcover model of the target aoi
     *
     * @param geoCoding the target geocoding
     * @param rasterDim the target raster dimensions
     */
    @Override
    public void setAOIGeoCoding(final GeoCoding geoCoding, final Dimension rasterDim) {
        final List<GeoPos> geometry = new ArrayList<>();
        geometry.add(geoCoding.getGeoPos(new PixelPos(0, 0), null));
        geometry.add(geoCoding.getGeoPos(new PixelPos(rasterDim.width, 0), null));
        geometry.add(geoCoding.getGeoPos(new PixelPos(rasterDim.width, rasterDim.height), null));
        geometry.add(geoCoding.getGeoPos(new PixelPos(0, rasterDim.height), null));
        geometry.add(geoCoding.getGeoPos(new PixelPos(0, 0), null));

        aoiWKT = toWKT(geometry);
    }

    private static String toWKT(final List<GeoPos> geometry) {

        StringBuilder wkt = new StringBuilder();
        wkt.append("POLYGON").append("((");

        for (GeoPos geoPos : geometry) {
            wkt.append(geoPos.lon)
                    .append(" ")
                    .append(geoPos.lat)
                    .append(",");
        }

        wkt.setLength(wkt.length() - 1);
        wkt.append("))");
        return wkt.toString();
    }

    @Override
    public LandCoverModelDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public Resampling getResampling() {
        return resampling;
    }

    @Override
    public double getLandCover(final GeoPos geoPos) throws Exception {
        try {
            if (tileList == null) {
                search();
            }
            for (FileLandCoverTile tile : tileList) {
                if (tile.getTileGeocoding() == null)
                    continue;

                final PixelPos pix = tile.getTileGeocoding().getPixelPos(geoPos, null);
                if (!pix.isValid() || pix.x < 0 || pix.y < 0 || pix.x >= tile.getWidth() || pix.y >= tile.getHeight())
                    continue;

                final Resampling.Index resamplingIndex = resampling.createIndex();
                resampling.computeIndex(pix.x, pix.y, tile.getWidth(), tile.getHeight(), resamplingIndex);

                final double value = resampling.resample(tile, resamplingIndex);
                if (Double.isNaN(value)) {
                    return tile.getNoDataValue();
                }
                return value;
            }
            return tileList[0].getNoDataValue();
        } catch (Exception e) {
            throw new Exception("Problem reading : " + e.getMessage());
        }
    }

    private synchronized void search() throws Exception {
        StacItem[] results = client.search(
                new String[]{"io-lulc"},
                new double[]{-124.2751, 45.5469, -123.9613, 45.7458},
                null);

        downloadAssets(results);
    }

    private void downloadAssets(final StacItem[] results) throws Exception {
        final List<FileLandCoverTile> tiles = new ArrayList<>();
        for (StacItem item : results) {

            for (String assetID : item.listAssetIds()) {
                if(!assetID.equals("data"))
                    continue;
                StacItem.StacAsset asset = item.getAsset(assetID);
                String fileName = asset.getFileName();
                URL remoteURL = new URL(client.signURL(asset));

                System.out.println("Downloading " + fileName + " from " + remoteURL);
                File folder = client.downloadAsset(asset, descriptor.getInstallDir());
                File[] files = folder.listFiles();

                File localFile = new File(descriptor.getInstallDir(), fileName);
                ProductReader reader = productReaderPlugIn.createReaderInstance();
                FileLandCoverTile tile = new FileLandCoverTile(this, localFile, remoteURL, reader, ".tif");
                tiles.add(tile);
            }
        }

        tileList = tiles.toArray(new FileLandCoverTile[0]);
    }

//    private void downloadAssets(List<StacRecord> recordList) throws Exception {
//        final List<FileLandCoverTile> tiles = new ArrayList<>();
//        for(StacRecord stacRecord : recordList) {
//
//            StacItem stacItem = client.getItem(stacRecord.selfHREF);
//
//            Map<String, Assets.Asset> assets = stacItem.getImageAssets();
//
//            String href = assets.values().iterator().next().href;
//            String fileName = href.substring(href.lastIndexOf("/")+1);
//            File localFile = new File(descriptor.getInstallDir(), fileName);
//            URL remoteURL = new URL(href.replace(fileName, ""));
//            ProductReader reader = productReaderPlugIn.createReaderInstance();
//            FileLandCoverTile tile = new FileLandCoverTile(this, localFile, remoteURL, reader, ".tif");
//            tiles.add(tile);
//        }
//
//        tileList = tiles.toArray(new FileLandCoverTile[0]);
//    }

    @Override
    public PixelPos getIndex(final GeoPos geoPos) {
        return null;
    }

    @Override
    public GeoPos getGeoPos(final PixelPos pixelPos) {
        return null;
    }

    @Override
    public float getSample(double pixelX, double pixelY) {
        return 0;
    }

    @Override
    public boolean getSamples(final int[] x, final int[] y, final double[][] samples) {
        return false;
    }
}