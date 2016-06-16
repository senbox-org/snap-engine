package org.esa.s3tbx.fu;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.pointop.WritableSample;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Marco Peters
 */
abstract class BandDefinition {

    private static final String X3_BAND_NAME = "X3";
    private static final String Y3_BAND_NAME = "Y3";
    private static final String Z3_BAND_NAME = "Z3";
    private static final String CHRX_BAND_NAME = "chrX";
    private static final String CHRY_BAND_NAME = "chrY";
    private static final String HUE_BAND_NAME = "hue_band";
    private static final String POLY_CORR_BAND_NAME = "poly_corr";
    private static final String HUE_ANGLE_BAND_NAME = "hue_angle";
    private static final String FU_VALUE_BAND_NAME = "FU";

    final String name;
    final String description;
    final int dataType;
    final double noDataValue;
    final boolean noDataValueUsed;

    private BandDefinition(String description, String name, int dataType, double noDataValue, boolean noDataValueUsed) {
        this.description = description;
        this.name = name;
        this.dataType = dataType;
        this.noDataValue = noDataValue;
        this.noDataValueUsed = noDataValueUsed;
    }

    void addToProduct(Product targetProduct) {
        Band band = targetProduct.addBand(name, dataType);
        band.setDescription(description);
        band.setNoDataValue(noDataValue);
        band.setNoDataValueUsed(noDataValueUsed);
    }

    abstract void setTargetSample(FuResult result, WritableSample targetSample);

    void setNoDataValue(WritableSample targetSample) {
        targetSample.set(noDataValue);
    }


    static BandDefinition[] create(boolean includeIntermediateResults, Instrument instrument) {
        List<BandDefinition> list = new ArrayList<>();
        if (includeIntermediateResults) {
            String bandNameSuffix = "_" + instrument.name();
            list.add(new BandDefinition("", X3_BAND_NAME + bandNameSuffix, ProductData.TYPE_FLOAT32, Float.NaN, true) {
                @Override
                public void setTargetSample(FuResult result, WritableSample targetSample) {
                    targetSample.set(result.getX3());
                }
            });
            list.add(new BandDefinition("", Y3_BAND_NAME + bandNameSuffix, ProductData.TYPE_FLOAT32, Float.NaN, true) {
                @Override
                public void setTargetSample(FuResult result, WritableSample targetSample) {
                    targetSample.set(result.getY3());
                }
            });
            list.add(new BandDefinition("", Z3_BAND_NAME + bandNameSuffix, ProductData.TYPE_FLOAT32, Float.NaN, true) {
                @Override
                public void setTargetSample(FuResult result, WritableSample targetSample) {
                    targetSample.set(result.getZ3());
                }
            });
            list.add(new BandDefinition("", CHRX_BAND_NAME + bandNameSuffix, ProductData.TYPE_FLOAT32, Float.NaN, true) {
                @Override
                public void setTargetSample(FuResult result, WritableSample targetSample) {
                    targetSample.set(result.getChrX());
                }
            });
            list.add(new BandDefinition("", CHRY_BAND_NAME + bandNameSuffix, ProductData.TYPE_FLOAT32, Float.NaN, true) {
                @Override
                public void setTargetSample(FuResult result, WritableSample targetSample) {
                    targetSample.set(result.getChrY());
                }
            });
            list.add(new BandDefinition("", POLY_CORR_BAND_NAME + bandNameSuffix, ProductData.TYPE_FLOAT32, Float.NaN, true) {
                @Override
                public void setTargetSample(FuResult result, WritableSample targetSample) {
                    targetSample.set(result.getPolyCorr());
                }
            });
            list.add(new BandDefinition("", HUE_BAND_NAME + bandNameSuffix, ProductData.TYPE_FLOAT32, Float.NaN, true) {
                @Override
                public void setTargetSample(FuResult result, WritableSample targetSample) {
                    targetSample.set(result.getHue());
                }
            });
        }
        list.add(new BandDefinition("", HUE_ANGLE_BAND_NAME, ProductData.TYPE_FLOAT32, Float.NaN, true) {
            @Override
            public void setTargetSample(FuResult result, WritableSample targetSample) {
                targetSample.set(result.getHueAngle());
            }
        });
        list.add(new BandDefinition("", FU_VALUE_BAND_NAME, ProductData.TYPE_INT8, Byte.MAX_VALUE, true) {
            @Override
            public void setTargetSample(FuResult result, WritableSample targetSample) {
                targetSample.set(result.getFuValue());
            }
        });

        return list.toArray(new BandDefinition[0]);
    }

}
