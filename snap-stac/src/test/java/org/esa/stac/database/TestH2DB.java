/*
 * Copyright (C) 2021 SkyWatch Space Applications Inc. https://www.skywatch.com
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
package org.esa.stac.database;

import org.esa.stac.StacItem;
import org.json.simple.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TestH2DB {

    private static String itemJSON = "{\n" +
            "  \"stac_version\" : \"1.0.0\",\n" +
            "  \"assets\" : {\n" +
            "    \"image\" : {\n" +
            "      \"storage:region\" : \"us-east-1\",\n" +
            "      \"storage:platform\" : \"AWS\",\n" +
            "      \"storage:tier\" : \"Standard\",\n" +
            "      \"href\" : \"https://ai4edataeuwest.blob.core.windows.net/io-lulc/io-lulc-model-001-v01-composite-v03-supercell-v02-clip-v01/06W_20200101-20210101.tif\",\n" +
            "      \"type\" : \"image/tiff; application=geotiff; profile=cloud-optimized\",\n" +
            "      \"storage:request_pays\" : false\n" +
            "    }\n" +
            "  },\n" +
            "  \"bbox\" : [ -153.2387314633243, 62.98596758280037, -141.14931376486413, 71.12312587440528 ],\n" +
            "  \"geometry\" : {\n" +
            "    \"coordinates\" : [ [ [ -143.99999999899995, 63.99999999000005 ], [ -150.00000012099997, 63.99999999000005 ], [ -150.00000012099997, 72.00000000000006 ], [ -143.99999999899995, 72.00000000000006 ], [ -143.99999999899995, 63.99999999000005 ] ] ],\n" +
            "    \"type\" : \"Polygon\"\n" +
            "  },\n" +
            "  \"links\" : [ {\n" +
            "    \"rel\" : \"root\",\n" +
            "    \"href\" : \"http://skywatch-auxdata.s3.us-west-2.amazonaws.com/landcover/EsriLandCover/catalog.json\",\n" +
            "    \"type\" : \"application/json\"\n" +
            "  }, {\n" +
            "    \"rel\" : \"self\",\n" +
            "    \"href\" : \"http://skywatch-auxdata.s3.us-west-2.amazonaws.com/landcover/EsriLandCover/esri-lulc-2020/32606/06W_20200101-20210101/06W_20200101-20210101.json\",\n" +
            "    \"type\" : \"application/json\"\n" +
            "  }, {\n" +
            "    \"rel\" : \"parent\",\n" +
            "    \"href\" : \"http://skywatch-auxdata.s3.us-west-2.amazonaws.com/landcover/EsriLandCover/esri-lulc-2020/32606/catalog.json\",\n" +
            "    \"type\" : \"application/json\"\n" +
            "  } ],\n" +
            "  \"id\" : \"06W_20200101-20210101\",\n" +
            "  \"type\" : \"Feature\",\n" +
            "  \"stac_extensions\" : [ \"https://stac-extensions.github.io/projection/v1.0.0/schema.json\", \"https://stac-extensions.github.io/raster/v1.0.0/schema.json\", \"https://stac-extensions.github.io/storage/v1.0.0/schema.json\" ],\n" +
            "  \"properties\" : {\n" +
            "    \"proj:epsg\" : 32606,\n" +
            "    \"datetime\" : null,\n" +
            "    \"proj:shape\" : [ 89926, 43809 ],\n" +
            "    \"proj:transform\" : [ 10.0, 0.0, 273874.48550524155, 0.0, -10.0, 7891140.523386055, 0.0, 0.0, 1.0 ],\n" +
            "    \"start_datetime\" : \"2020-01-01T00:00:00.00Z\",\n" +
            "    \"end_datetime\" : \"2021-01-01T00:00:00.00Z\",\n" +
            "    \"proj:geometry\" : {\n" +
            "      \"coordinates\" : [ [ [ 273874.48550524155, 7891140.523386055 ], [ 273874.48550524155, 6991880.523386055 ], [ 711964.4855052416, 6991880.523386055 ], [ 711964.4855052416, 7891140.523386055 ], [ 273874.48550524155, 7891140.523386055 ] ] ],\n" +
            "      \"type\" : \"Polygon\"\n" +
            "    },\n" +
            "    \"proj:bbox\" : [ 273874.48550524155, 6991880.523386055, 711964.4855052416, 7891140.523386055 ],\n" +
            "    \"raster:bands\" : [ {\n" +
            "      \"histogram\" : {\n" +
            "        \"min\" : 1.0,\n" +
            "        \"max\" : 10.0,\n" +
            "        \"buckets\" : [ 16047, 122521, 26399, 14467, 175, 181125, 290, 22042, 16573, 8 ],\n" +
            "        \"count\" : 11\n" +
            "      },\n" +
            "      \"offset\" : 0.0,\n" +
            "      \"sampling\" : \"area\",\n" +
            "      \"data_type\" : \"uint8\",\n" +
            "      \"scale\" : 1.0,\n" +
            "      \"nodata\" : 0.0,\n" +
            "      \"statistics\" : {\n" +
            "        \"valid_percent\" : 78.21247964679358,\n" +
            "        \"mean\" : 4.5374593078391685,\n" +
            "        \"maximum\" : 10,\n" +
            "        \"minimum\" : 1,\n" +
            "        \"stdev\" : 2.259333355614968\n" +
            "      }\n" +
            "    } ]\n" +
            "  }\n" +
            "}";

    @Test
    public void testDB_Insert() throws Exception {
        final StacDatabase stacDatabase = new StacDatabase();
        stacDatabase.initialize();

        StacItem item = new StacItem(itemJSON);
        stacDatabase.saveItem(item);

        final Map<String, Object> parameters = new HashMap<>();
        parameters.put(StacDatabase.STAC_ID, item.getId());
        List<StacRecord> recordList = stacDatabase.search(parameters);

        assertEquals(1, recordList.size());
    }
}
