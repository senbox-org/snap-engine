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

        this.supportedInstruments = new String[]{"MERIS", "VGT"};

        this.reflecNames = new HashMap<>(supportedInstruments.length);
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

        this.geomNames = new HashMap<>(supportedInstruments.length);
        String[] merisGeomNames = {
                EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME,
                EnvisatConstants.MERIS_SUN_AZIMUTH_DS_NAME,
                EnvisatConstants.MERIS_VIEW_ZENITH_DS_NAME,
                EnvisatConstants.MERIS_VIEW_AZIMUTH_DS_NAME
        };
        geomNames.put(supportedInstruments[0], merisGeomNames);
        String[] vgtGeomNames = {"SZA", "SAA", "VZA", "VAA"};
        geomNames.put(supportedInstruments[1], vgtGeomNames);

        this.fitWeights = new HashMap<>(supportedInstruments.length);
        double[] merisFitWeights = {1.0, 1.0, 1.0, 1.0, 0.2, 1.0, 1.0, 1.0,
                0.5, 0.5, 0.0, 0.5, 0.5, 0.5, 0.0};
        fitWeights.put(supportedInstruments[0], merisFitWeights);
        double[] vgtFitWeights = {1.0, 1.0, 0.5, 0.1};
        fitWeights.put(supportedInstruments[1], vgtFitWeights);

        this.validRetrievalExpr = new HashMap<>(supportedInstruments.length);
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

        this.validAotOutExpr = new HashMap<>(supportedInstruments.length);
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

        this.nLutBands = new HashMap<>(supportedInstruments.length);
        int merisNLutBands = 15;
        nLutBands.put(supportedInstruments[0], merisNLutBands);
        int vgtNLutBands = 4;
        nLutBands.put(supportedInstruments[1], vgtNLutBands);

        this.surfPressureName = new HashMap<>(supportedInstruments.length);
        String merisSurfPressureName = "surfPressEstimate";
        surfPressureName.put(supportedInstruments[0], merisSurfPressureName);
        String vgtSurfPressureName = "surfPressEstimate";
        surfPressureName.put(supportedInstruments[1], vgtSurfPressureName);

        this.ozoneName = new HashMap<>(supportedInstruments.length);
        String merisOzoneName = "ozone";
        ozoneName.put(supportedInstruments[0], merisOzoneName);
        String vgtOzoneName = "OG";
        ozoneName.put(supportedInstruments[1], vgtOzoneName);

        this.ndviName = "toaNdvi";
        this.ndviExpression = new HashMap<>(supportedInstruments.length);
        String merisNdviExp = "(reflectance_13 - reflectance_7) / (reflectance_13 + reflectance_7)";
        ndviExpression.put(supportedInstruments[0], merisNdviExp);
        String vgtNdviExp = "(B3-B2)/(B3+B2)";
        ndviExpression.put(supportedInstruments[1], vgtNdviExp);
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

    String getNirName(String instrument) {
        if (instrument.equals("MERIS")) return "reflectance_13";
        if (instrument.equals("VGT")) return "B3";
        return "";
    }

}
