package org.esa.s3tbx.dataio.landsat.geotiff;

/**
 * @author Marco Peters
 */
class LandsatTypeInfo {

    private static final String COLLECTION_FILENAME_REGEX = "L[COTEM]\\d{2}_L1\\w{2}_\\d{3}\\d{3}_\\d{8}_\\d{8}_\\d{2}_(T1|T2|RT)";
    private static final String L4_FILENAME_REGEX = "LT4\\d{13}\\w{3}\\d{2}";
    private static final String L5_FILENAME_REGEX = "LT5\\d{13}\\w{3}\\d{2}";
    private static final String L7_FILENAME_REGEX = "LE7\\d{13}\\w{3}\\d{2}";
    private static final String L8_FILENAME_REGEX = "L[OTC]8\\d{13}\\w{3}\\d{2}";
    private static final String MSS_FILENAME_REGEX = "LM[1-5]\\d{13}\\w{3}\\d{2}";
    private static final String L5LEGACY_FILENAME_REGEX_1 = "LT5\\d{13}\\w{3}\\d{2}";
    private static final String L5LEGACY_FILENAME_REGEX_2 = "L5\\d{6}_\\d{11}";
    private static final String L7LEGACY_FILENAME_REGEX_1 = "LE7\\d{13}\\w{3}\\d{2}";
    private static final String L7LEGACY_FILENAME_REGEX_2 = "L7\\d{7}_\\d{11}";

    private enum LandsatType {
        LANDSAT_COLLECTION {
            @Override
            boolean matchesFileNamepattern(String filename) {
                return filename.matches(COLLECTION_FILENAME_REGEX + "_MTL" + getTxtExtension()) ||
                       filename.matches(COLLECTION_FILENAME_REGEX + getCompressionExtension());
            }
        },
        LANDSAT_MSS {
            @Override
            boolean matchesFileNamepattern(String filename) {
                return filename.matches(MSS_FILENAME_REGEX + "_MTL" + getTxtExtension());
            }
        },
        LANDSAT4 {
            @Override
            boolean matchesFileNamepattern(String filename) {
                return filename.matches(L4_FILENAME_REGEX + "_MTL" + getTxtExtension()) ||
                       filename.matches(L4_FILENAME_REGEX + getCompressionExtension());
            }
        },
        LANDSAT5 {
            @Override
            boolean matchesFileNamepattern(String filename) {
                return filename.matches(L5_FILENAME_REGEX + "_MTL" + getTxtExtension()) ||
                       filename.matches(L5_FILENAME_REGEX + getCompressionExtension());
            }
        },
        LANDSAT5_LEGACY {
            @Override
            boolean matchesFileNamepattern(String filename) {
                return filename.matches(L5LEGACY_FILENAME_REGEX_1 + "_MTL" + getTxtExtension()) ||
                       filename.matches(L5LEGACY_FILENAME_REGEX_2 + "_MTL" + getTxtExtension()) ||
                       filename.matches(L5LEGACY_FILENAME_REGEX_1 + getCompressionExtension());
            }
        },
        LANDSAT7 {
            @Override
            boolean matchesFileNamepattern(String filename) {
                return filename.matches(L7_FILENAME_REGEX + "_MTL" + getTxtExtension()) ||
                       filename.matches(L7_FILENAME_REGEX + getCompressionExtension());
            }
        },
        LANDSAT7_LEGACY {
            @Override
            boolean matchesFileNamepattern(String filename) {
                return filename.matches(L7LEGACY_FILENAME_REGEX_1 + "_MTL" + getTxtExtension()) ||
                       filename.matches(L7LEGACY_FILENAME_REGEX_2 + "_MTL" + getTxtExtension()) ||
                       filename.matches(L7LEGACY_FILENAME_REGEX_1 + getCompressionExtension());
            }
        },
        LANDSAT8 {
            @Override
            boolean matchesFileNamepattern(String filename) {
                return filename.matches(L8_FILENAME_REGEX + "_MTL" + getTxtExtension()) ||
                       filename.matches(L8_FILENAME_REGEX + getCompressionExtension());
            }
        };

        abstract boolean matchesFileNamepattern(String filename);
    }


    private LandsatTypeInfo() {
    }

    public static boolean isLandsat(String fileName) {
        for (LandsatType type : LandsatType.values()) {
            if (type.matchesFileNamepattern(fileName)) {
                return true;
            }
        }
        return false;
    }

    static boolean isLandsatCollection(String fileName) {
        return LandsatType.LANDSAT_COLLECTION.matchesFileNamepattern(fileName);
    }

    static boolean isMss(String fileName) {
        return LandsatType.LANDSAT_MSS.matchesFileNamepattern(fileName);
    }

    static boolean isLandsat4(String fileName) {
        return LandsatType.LANDSAT4.matchesFileNamepattern(fileName);
    }

    static boolean isLandsat5Legacy(String fileName) {
        return LandsatType.LANDSAT5_LEGACY.matchesFileNamepattern(fileName);
    }

    static boolean isLandsat5(String fileName) {
        return LandsatType.LANDSAT5.matchesFileNamepattern(fileName);
    }

    static boolean isLandsat7Legacy(String fileName) {
        return LandsatType.LANDSAT7_LEGACY.matchesFileNamepattern(fileName);
    }

    static boolean isLandsat7(String fileName) {
        return LandsatType.LANDSAT7.matchesFileNamepattern(fileName);
    }

    static boolean isLandsat8(String fileName) {
        return LandsatType.LANDSAT8.matchesFileNamepattern(fileName);
    }


    private static String getCompressionExtension() {
        return "\\.(tar\\.gz|tgz|tar\\.bz|tbz|tar\\.bz2|tbz2|zip|ZIP)";
    }

    private static String getTxtExtension() {
        return "\\.(txt|TXT)";
    }

}
