/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.s3tbx.aerosol;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Instrument specific constants
 *
 * @author akheckel
 *         <p/>
 *         TODO: revise validPixel Expression: properly include range of LUTs
 *         TODO: revise validPixel Expression: enable separate treatment of snow pixels
 */
class InstrumentConsts {

    private static InstrumentConsts instance;

    private final String[] supportedInstruments;

    /****************************************
     * MERIS
     ****************************************/

    private final String idepixFlagBandName = "pixel_classif_flags";

    private final Map<String, String[]> reflecNames;
    private final Map<String, String[]> geomNames;
    private final Map<String, double[]> fitWeights;
    private final Map<String, String> validRetrievalExpr;
    private final Map<String, String> validAotOutExpr;
    private final Map<String, Integer> nLutBands;
    private final Map<String, String> surfPressureName;
    private final Map<String, String> ozoneName;
    private final Map<String, String> ndviExpression;

    private final String ndviName;

    private InstrumentConsts() {

        this.supportedInstruments = new String[]{"MERIS", "VGT", "AATSR", "PROBAV"};

        this.reflecNames = new HashMap<String, String[]>(supportedInstruments.length);
        String[] merisReflectanceNames = {
                "reflectance_1",
                "reflectance_2",
                "reflectance_3",
                "reflectance_4",
                "reflectance_5",
                "reflectance_6",
                "reflectance_7",
                "reflectance_8",
                "reflectance_9",
                "reflectance_10",
                "reflectance_11",
                "reflectance_12",
                "reflectance_13",
                "reflectance_14",
                "reflectance_15"
        };
        reflecNames.put(supportedInstruments[0], merisReflectanceNames);
        /***************************************
         VGT
         */
        String[] vgtReflectanceNames = {"B0", "B2", "B3", "MIR"};
        reflecNames.put(supportedInstruments[1], vgtReflectanceNames);
        /***************************************
         AATSR
         */String[] aatsrReflectanceNames = {
                "reflec_nadir_0550",
                "reflec_nadir_0670",
                "reflec_nadir_0870",
                "reflec_nadir_1600",
                "reflec_fward_0550",
                "reflec_fward_0670",
                "reflec_fward_0870",
                "reflec_fward_1600",
        };
        reflecNames.put(supportedInstruments[2], aatsrReflectanceNames);

        String[] probavReflectanceNames = {"TOA_REFL_BLUE", "TOA_REFL_RED", "TOA_REFL_NIR", "TOA_REFL_SWIR"};
        reflecNames.put(supportedInstruments[3], probavReflectanceNames);

        this.geomNames = new HashMap<String, String[]>(supportedInstruments.length);
        String[] merisGeomNames = {
                EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME,
                EnvisatConstants.MERIS_SUN_AZIMUTH_DS_NAME,
                EnvisatConstants.MERIS_VIEW_ZENITH_DS_NAME,
                EnvisatConstants.MERIS_VIEW_AZIMUTH_DS_NAME
        };
        geomNames.put(supportedInstruments[0], merisGeomNames);
        String[] vgtGeomNames = {"SZA", "SAA", "VZA", "VAA"};
        geomNames.put(supportedInstruments[1], vgtGeomNames);
        String[] aatsrGeomNames = {
                EnvisatConstants.AATSR_SUN_ELEV_NADIR_DS_NAME,
                EnvisatConstants.AATSR_SUN_AZIMUTH_NADIR_DS_NAME,
                EnvisatConstants.AATSR_VIEW_ELEV_NADIR_DS_NAME,
                EnvisatConstants.AATSR_VIEW_AZIMUTH_NADIR_DS_NAME,
                EnvisatConstants.AATSR_SUN_ELEV_FWARD_DS_NAME,
                EnvisatConstants.AATSR_SUN_AZIMUTH_FWARD_DS_NAME,
                EnvisatConstants.AATSR_VIEW_ELEV_FWARD_DS_NAME,
                EnvisatConstants.AATSR_VIEW_AZIMUTH_FWARD_DS_NAME
        };
        geomNames.put(supportedInstruments[2], aatsrGeomNames);

        String[] probavGeomNames = {"SZA", "SAA", "VZA_SWIR", "VAA_SWIR", "VZA_VNIR", "VAA_VNIR"};
        geomNames.put(supportedInstruments[3], probavGeomNames);

        this.fitWeights = new HashMap<String, double[]>(supportedInstruments.length);
        double[] merisFitWeights = {1.0, 1.0, 1.0, 1.0, 0.2, 1.0, 1.0, 1.0,
                0.5, 0.5, 0.0, 0.5, 0.5, 0.5, 0.0};
        fitWeights.put(supportedInstruments[0], merisFitWeights);
        double[] vgtFitWeights = {1.0, 1.0, 0.5, 0.1};
        fitWeights.put(supportedInstruments[1], vgtFitWeights);
        double[] aatsrFitWeights = {1.5, 1.0, 1.0, 1.55};
        fitWeights.put(supportedInstruments[2], aatsrFitWeights);

        double[] probavFitWeights = {1.0, 1.0, 0.5, 0.1};
        fitWeights.put(supportedInstruments[3], probavFitWeights);

        this.validRetrievalExpr = new HashMap<String, String>(supportedInstruments.length);
        // todo: clarify if the valid retrieval expressions make sense !!
        String merisValidRetrievalExpr = "(!l1_flags.INVALID "
                + " &&  " + idepixFlagBandName + ".F_LAND "
                + " && !" + idepixFlagBandName + ".F_SNOW_ICE "
                + " && !" + idepixFlagBandName + ".F_CLOUD "   // ???
                + " && !" + idepixFlagBandName + ".F_CLOUD_BUFFER "   // ???
                + " && (" + EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME + "<70))";
        validRetrievalExpr.put(supportedInstruments[0], merisValidRetrievalExpr);
        String vgtValidRetrievaExpr = "(SM.B0_GOOD && SM.B2_GOOD && SM.B3_GOOD && (SM.MIR_GOOD or MIR <= 0.65) "
                + " &&  " + idepixFlagBandName + ".F_LAND "
                + " && !" + idepixFlagBandName + ".F_SNOW_ICE "
//                + " && !" + idepixFlagBandName + ".F_CLOUD_BUFFER "
                + " && (SZA<70)) ";
        validRetrievalExpr.put(supportedInstruments[1], vgtValidRetrievaExpr);
        String aatsrValidRetrievalExpr = "(" + idepixFlagBandName + ".F_LAND "
                + " && !" + idepixFlagBandName + ".F_CLOUD_BUFFER"
                + " && !" + idepixFlagBandName + ".F_SNOW_ICE"
                + " && (90-sun_elev_nadir) < 70"
                + " && (90-sun_elev_fward) < 70"
                + " && reflec_nadir_0550 >= 0"
                + " && reflec_nadir_0670 >= 0"
                + " && reflec_nadir_0870 >= 0"
                + " && reflec_nadir_1600 >= 0"
                + " && reflec_fward_0550 >= 0"
                + " && reflec_fward_0670 >= 0"
                + " && reflec_fward_0870 >= 0"
                + " && reflec_fward_1600 >= 0 )";
        validRetrievalExpr.put(supportedInstruments[2], aatsrValidRetrievalExpr);
        String probavValidRetrievaExpr = "(SM_FLAGS.GOOD_BLUE && SM_FLAGS.GOOD_RED && SM_FLAGS.GOOD_NIR && (SM_FLAGS.GOOD_SWIR or TOA_REFL_SWIR <= 0.65) "
                + " &&  " + idepixFlagBandName + ".F_LAND "
                + " && !" + idepixFlagBandName + ".F_SNOW_ICE "
                + " && !" + idepixFlagBandName + ".F_CLOUD_BUFFER "
                + " && (SZA<70)) ";
        validRetrievalExpr.put(supportedInstruments[3], probavValidRetrievaExpr);

        this.validAotOutExpr = new HashMap<String, String>(supportedInstruments.length);
        // todo: clarify if the aot output expressions make sense !!
        String merisValAotOutputExpr = "(!l1_flags.INVALID "
                + " &&  " + idepixFlagBandName + ".F_LAND "
//                + " && (!" + idepixFlagBandName + ".F_CLOUD_BUFFER || " + idepixFlagBandName + ".F_SNOW_ICE)"
                + " && (" + EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME + "<70))";
        validAotOutExpr.put(supportedInstruments[0], merisValAotOutputExpr);
        String vgtValAotOutputExpr = "(SM.B0_GOOD && SM.B2_GOOD && SM.B3_GOOD "
                + " &&  " + idepixFlagBandName + ".F_LAND "
//                + " && (!" + idepixFlagBandName + ".F_CLOUD_BUFFER || " + idepixFlagBandName + ".F_SNOW_ICE)"
                + " && (SZA<70)) ";
        validAotOutExpr.put(supportedInstruments[1], vgtValAotOutputExpr);
        String aatsrValAotOutputExpr = "(" + idepixFlagBandName + ".F_LAND "
                + " && (!" + idepixFlagBandName + ".F_CLOUD_BUFFER || " + idepixFlagBandName + ".F_SNOW_ICE)"
                + " && (90-sun_elev_nadir) < 70"
                + " && (90-sun_elev_fward) < 70"
                + " && reflec_nadir_0550 >= 0"
                + " && reflec_nadir_0670 >= 0"
                + " && reflec_nadir_0870 >= 0"
                + " && reflec_nadir_1600 >= 0"
                + " && reflec_fward_0550 >= 0"
                + " && reflec_fward_0670 >= 0"
                + " && reflec_fward_0870 >= 0"
                + " && reflec_fward_1600 >= 0 )";
        validAotOutExpr.put(supportedInstruments[2], aatsrValAotOutputExpr);
        String probavValAotOutputExpr = "(SM_FLAGS.GOOD_BLUE && SM_FLAGS.GOOD_RED && SM_FLAGS.GOOD_NIR "
                + " &&  " + idepixFlagBandName + ".F_LAND "
                + " && (!" + idepixFlagBandName + ".F_CLOUD_BUFFER || " + idepixFlagBandName + ".F_SNOW_ICE)"
                + " && (SZA<70)) ";
        validAotOutExpr.put(supportedInstruments[3], probavValAotOutputExpr);

        this.nLutBands = new HashMap<String, Integer>(supportedInstruments.length);
        int merisNLutBands = 15;
        nLutBands.put(supportedInstruments[0], merisNLutBands);
        int vgtNLutBands = 4;
        nLutBands.put(supportedInstruments[1], vgtNLutBands);
        int aatsrNLutBands = 4;
        nLutBands.put(supportedInstruments[2], aatsrNLutBands);
        int probavNLutBands = 4;
        nLutBands.put(supportedInstruments[3], probavNLutBands);

        this.surfPressureName = new HashMap<String, String>(supportedInstruments.length);
        String merisSurfPressureName = "surfPressEstimate";
        surfPressureName.put(supportedInstruments[0], merisSurfPressureName);
        String vgtSurfPressureName = "surfPressEstimate";
        surfPressureName.put(supportedInstruments[1], vgtSurfPressureName);
        String aatsrSurfPressureName = "surfPressEstimate";
        surfPressureName.put(supportedInstruments[2], aatsrSurfPressureName);
        String probavSurfPressureName = "surfPressEstimate";
        surfPressureName.put(supportedInstruments[3], probavSurfPressureName);

        this.ozoneName = new HashMap<String, String>(supportedInstruments.length);
        String merisOzoneName = "ozone";
        ozoneName.put(supportedInstruments[0], merisOzoneName);
        String vgtOzoneName = "OG";
        ozoneName.put(supportedInstruments[1], vgtOzoneName);
        String aatsrOzoneName = "ozoneConst";
        ozoneName.put(supportedInstruments[2], aatsrOzoneName);
        String probavOzoneName = "NOT AVAILABLE";
        ozoneName.put(supportedInstruments[3], probavOzoneName);

        this.ndviName = "toaNdvi";
        this.ndviExpression = new HashMap<String, String>(supportedInstruments.length);
        String merisNdviExp = "(reflectance_13 - reflectance_7) / (reflectance_13 + reflectance_7)";
        ndviExpression.put(supportedInstruments[0], merisNdviExp);
        String vgtNdviExp = "(B3-B2)/(B3+B2)";
        ndviExpression.put(supportedInstruments[1], vgtNdviExp);
        String aatsrNdviExp = "(reflec_nadir_0870 - reflec_nadir_0670) / (reflec_nadir_0870 + reflec_nadir_0670)";
        ndviExpression.put(supportedInstruments[2], aatsrNdviExp);
        String probavNdviExp = "NDVI";
        ndviExpression.put(supportedInstruments[3], probavNdviExp);
    }

    public static InstrumentConsts getInstance() {
        if (instance == null) {
            instance = new InstrumentConsts();
        }
        return instance;
    }

    public String getInstrument(Product p) {
        for (String suppInstr : supportedInstruments) {
            String[] specBands = getSpecBandNames(suppInstr);
            if (specBands.length > 0 && p.containsBand(specBands[0])) {
                return suppInstr;
            }
        }
        throw new OperatorException("Product not supported.");
    }

    public double[] getSpectralFitWeights(String instrument) {
        //return fitWeights.get(instrument);
        return normalize(fitWeights.get(instrument));
    }

    public String[] getGeomBandNames(String instrument) {
        return geomNames.get(instrument);
    }

    public String[] getSpecBandNames(String instrument) {
        return reflecNames.get(instrument);
    }

    public String getValidRetrievalExpression(String instrument) {
        return validRetrievalExpr.get(instrument);
    }

    public String getValAotOutExpression(String instrument) {
        return validAotOutExpr.get(instrument);
    }

    public int getnLutBands(String instrument) {
        return nLutBands.get(instrument);
    }

    public String getSurfPressureName(String instrument) {
        return surfPressureName.get(instrument);
    }

    public String getOzoneName(String instrument) {
        return ozoneName.get(instrument);
    }

    public String getNdviName() {
        return ndviName;
    }

    public String getNdviExpression(String instrument) {
        return this.ndviExpression.get(instrument);
    }

    public String getIdepixFlagBandName() {
        return idepixFlagBandName;
    }

    public String getElevationBandName() {
        return "elevation";
    }

    private double[] normalize(double[] fa) {
        double sum = 0;
        for (double f : fa) {
            sum += f;
        }
        for (int i = 0; i < fa.length; i++) {
            fa[i] /= sum;
        }
        return fa;
    }

    public boolean isVgtAuxBand(Band b) {
        String bname = b.getName();
        for (String geomName : getGeomBandNames("VGT")) {
            if (bname.equals(geomName)) {
                return true;
            }
        }
        return bname.equals(getOzoneName("VGT")) || bname.equals("WVG");
    }

    public boolean isProbavAuxBand(Band b) {
        String bname = b.getName();
        for (String geomName : getGeomBandNames("PROBAV")) {
            if (bname.equals(geomName)) {
                return true;
            }
        }
        return false;
    }

    String getNirName(String instrument) {
        if (instrument.equals("MERIS")) return "reflectance_13";
        if (instrument.equals("VGT")) return "B3";
        if (instrument.equals("AATSR")) return "reflec_nadir_0870";
        if (instrument.equals("PROBAV")) return "TOA_REFL_NIR";
        return "";
    }

}
