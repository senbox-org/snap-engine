package org.esa.s3tbx.watermask.operator;

import org.esa.snap.core.util.SystemUtils;

import java.nio.file.Path;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 23.12.2015
 * Time: 15:04
 *
 * @author olafd
 */
public class WatermaskConstants {
//    public static final String AUXDATA_VERSION = "3.0.0";
//    public static final String AUXDATA_FILE_EXT = ".zip";

    // test setup, not (yet) used
//    public static final String REMOTE_FTP_HOST = "ftp.brockmann-consult.de";
//    public static final String REMOTE_FTP_PATH = "/temp/olaf/s3tbx-watermask-aux/auxdata_" + AUXDATA_VERSION + "/images/";
//    public static final String FTP_USER = "diversity";

//    public static final String FTP_PASSWORD = "diversity!";
    // preliminary test HTTP location:
//    public static final String REMOTE_HTTP_HOST = "http://gws-access.cems.rl.ac.uk";
//    public static final String REMOTE_HTTP_PATH = "/public/globalbedo/olafd/s3tbx-watermask-aux_" + AUXDATA_VERSION + "/images/";
    // the 'preliminary final' HTTP location (might get a versioning later, tbd):
    public static final String REMOTE_HTTP_HOST = "http://step.esa.int";

    public static final String REMOTE_HTTP_PATH = "/auxdata/watermask/images/";

    public static final Path LOCAL_AUXDATA_PATH = SystemUtils.getAuxDataPath().resolve("s3tbx/watermask").toAbsolutePath();

    // we have:
    // 50m.zip
    // 150m.zip
    // GC_water_mask.zip
    // MODIS_north_water_mask.zip
    // MODIS_south_water_mask.zip
    public static final String[] AUXDATA_FILENAMES = {
            "50m.zip",
            "150m.zip",
            "GC_water_mask.zip",
            "MODIS_north_water_mask.zip",
            "MODIS_south_water_mask.zip"};
}
