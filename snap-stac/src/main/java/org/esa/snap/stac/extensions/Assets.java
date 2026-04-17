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
package org.esa.snap.stac.extensions;

import org.esa.snap.core.util.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class Assets implements StacExtension {

    public static final String assets = "assets";
    public final static String schema = "https://stac-extensions.github.io/assets/v1.0.0/schema.json";

    public static final String href = "href";
    public static final String title = "title";
    public static final String description = "description";
    public static final String type = "type";
    public static final String role = "role";
    public static final String roles = "roles";

    public static final String metadata = "metadata";
    public static final String thumbnail = "thumbnail";
    public static final String analytic = "analytic";
    public static final String preview = "preview";
    public static final String raster = "raster";
    public static final String vector = "vector";

    // media types
    public static final String type_image_tiff = "image/tiff";
    public static final String type_image_geotiff = "image/tiff; application=geotiff";	// GeoTIFF with standardized georeferencing metadata
    public static final String type_image_cog = "image/tiff; application=geotiff; profile=cloud-optimized";	// Cloud Optimized GeoTIFF (unofficial). Once there is an official media type it will be added and the proprietary media type here will be deprecated.
    public static final String type_image_jp2 = "image/jp2";	                    // JPEG 2000
    public static final String type_image_png = "image/png";	                    // Visual PNGs (e.g. thumbnails)
    public static final String type_image_jpg = "image/jpeg";	                    // Visual JPEGs (e.g. thumbnails, oblique)
    public static final String type_xml = "application/xml";	                    // XML metadata RFC 7303
    public static final String type_json = "application/json";	                    // JSON metadata
    public static final String type_txt = "text/plain";	                            // Plain text metadata
    public static final String type_csv = "text/csv";	                            // Comma Separated Values
    public static final String type_html = "text/html";	                            // Hypertext Markup Language
    public static final String type_geojson = "application/geo+json";	            // GeoJSON
    public static final String type_geopackage = "application/geopackage+sqlite3";	// GeoPackage
    public static final String type_hdf5 = "application/x-hdf5";	                // Hierarchical Data Format version 5
    public static final String type_hdf = "application/x-hdf";	                    // Hierarchical Data Format versions 4 and earlier.
    public static final String type_pdf = "application/pdf";	                    // Portable Document Format
    public static final String type_zip = "application/zip";	                    // Zip

    // roles
    public static final String role_thumbnail = "thumbnail";
    public static final String role_overview = "overview";
    public static final String role_visual = "visual";
    public static final String role_quicklook = "quicklook";
    public static final String role_preview = "preview";

    public static final String role_data = "data";
    public static final String role_auxdata = "auxdata";
    public static final String role_mask = "mask";
    public static final String role_cloud = "cloud";
    public static final String role_dem = "dem";
    public static final String role_landcover = "landcover";

    public static final String role_metadata = "metadata";

    private static final String[] ANALYTIC_IMAGE_TYPES = new String[] {type_image_tiff, type_image_geotiff,
            type_image_cog,type_image_jp2,type_hdf5};

    private static final String[] PREVIEWS = new String[] {role_preview, role_overview, role_thumbnail, role_visual, role_quicklook};
    private static final String[] AUX_ROLES = new String[] {role_auxdata, role_mask, role_cloud, role_dem, role_landcover};

    public static Asset addAsset(final JSONObject assetsJSON, final String name,
                                final String titleValue, final String description, final String hrefValue,
                                final String typeValue, final String[] roleValues) {

        final Asset asset = new Asset(name, titleValue, description, hrefValue, typeValue, roleValues);
        assetsJSON.put(name, asset.json);

        return asset;
    }

    public static Map<String, Asset> getImageAssets(final JSONObject assetsJSON) {
        final Map<String, Asset> assetMap = new HashMap<>();
        for(Object key : assetsJSON.keySet()) {
            final Asset asset = new Asset((String)key, (JSONObject)assetsJSON.get(key));
            if(asset.type != null && StringUtils.contains(ANALYTIC_IMAGE_TYPES, asset.type)) {
                if(isPreview(asset.name)) {
                    continue;
                }
                if(asset.roles != null && isAuxiliaryRole(asset.roles)) {
                    continue;
                }
                assetMap.put((String)key, asset);
            }
        }
        return assetMap;
    }

    private static boolean isPreview(String name) {
        String n = name.toLowerCase();
        for(String preview : PREVIEWS) {
            if(n.contains(preview)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAuxiliaryRole(JSONArray role) {
        for(Object obj : role) {
            String r = ((String) obj).toLowerCase();
            for (String auxRole : AUX_ROLES) {
                if (r.contains(auxRole)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static class Asset {
        public final JSONObject json;
        public final String name;
        public String href, title, description, type;
        public JSONArray roles;
        private String fileName;

        public Asset(final String name, final JSONObject json) {
            this.json = json;
            this.name = name;
            if(json.containsKey(Assets.href)) {
                this.href = (String)json.get(Assets.href);
                this.fileName = this.href.split("/")[this.href.split("/").length - 1];
            }
            if(json.containsKey(Assets.title)) {
                this.title = (String)json.get(Assets.title);
            }
            if(json.containsKey(Assets.description)) {
                this.description = (String)json.get(Assets.description);
            }
            if(json.containsKey(Assets.type)) {
                this.type = (String)json.get(Assets.type);
            }
            if(json.containsKey(Assets.role)) {
                this.roles = new JSONArray();
                this.roles.add(json.get(Assets.role));
            }
            if(json.containsKey(Assets.roles)) {
                this.roles = (JSONArray) json.get(Assets.roles);
            }
        }

        public Asset(final String name, final String titleValue, final String descriptionValue, final String hrefValue,
                     final String typeValue, final String[] roleValues) {
            this.json = new JSONObject();
            this.name = name;
            this.href = hrefValue;
            this.title = titleValue;
            this.description = descriptionValue;
            this.type = typeValue;
            this.roles = new JSONArray();
            this.roles.addAll(Arrays.asList(roleValues));
            this.fileName = this.href.split("/")[this.href.split("/").length - 1];

            json.put(Assets.title, title);
            json.put(Assets.href, href);
            if(description != null) {
                json.put(Assets.description, description);
            }
            if(type != null) {
                json.put(Assets.type, type);
            }
            if (roles != null) {
                json.put(Assets.roles, roles);
            }
        }

        public void addRasterBand(final String name, final String data_type, final String unit,
                                  final double nodata, final int width, final int height) {
            if(!json.containsKey(Raster.bands)) {
                json.put(Raster.bands, new JSONArray());
            }
            final JSONArray rasterBands = (JSONArray) json.get(Raster.bands);

            JSONObject raster = null;
            for(Object obj : rasterBands) {
                if(((JSONObject)obj).containsKey(Raster.name) && ((JSONObject)obj).get(Raster.name).equals(name)) {
                    raster = (JSONObject)obj;
                    break;
                }
            }
            if(raster == null) {
                raster = new JSONObject();
                rasterBands.add(raster);
            }

            raster.put(Raster.name, name);
            if (type != null) {
                raster.put(Raster.data_type, data_type);
            }
            if (unit != null) {
                raster.put(Raster.unit, unit);
            }
            raster.put(Raster.nodata, nodata);

            final JSONArray shape = new JSONArray();
            shape.add(height);
            shape.add(width);
            json.put(Proj.shape, shape);
        }

        public JSONArray getBands() {
            if(json.containsKey(Raster.bands)) {
                return (JSONArray) json.get(Raster.bands);
            } else if(json.containsKey(EO.bands)) {
                return (JSONArray) json.get(EO.bands);
            }
            return new JSONArray();
        }

        public String getURL() {
            return this.href;
        }

        public String getFileName() {
            return this.fileName;
        }

        public String getId() {
            return this.name;
        }
    }
}
