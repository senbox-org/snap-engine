package org.esa.s3tbx.idepix.operators;

import org.esa.s3tbx.idepix.algorithms.landsat8.Landsat8ClassificationOp;
import org.esa.s3tbx.processor.rad2refl.Rad2ReflConstants;
import org.esa.s3tbx.processor.rad2refl.Rad2ReflOp;
import org.esa.s3tbx.processor.rad2refl.Sensor;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.util.ProductUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for handling products in Idepix processing
 *
 * @author olafd
 */
public class IdepixProducts {
    public static Product computeRadiance2ReflectanceProduct(Product sourceProduct) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("sensor", Sensor.MERIS);
        params.put("copyNonSpectralBands", false);
        return GPF.createProduct(OperatorSpi.getOperatorAlias(Rad2ReflOp.class), params, sourceProduct);
    }

    public static Product computeCloudTopPressureProduct(Product sourceProduct) {
        return GPF.createProduct("Meris.CloudTopPressureOp", GPF.NO_PARAMS, sourceProduct);
    }

    public static void addRadianceBands(Product l1bProduct, Product targetProduct) {
        for (String bandname : l1bProduct.getBandNames()) {
            if (!targetProduct.containsBand(bandname) && bandname.startsWith(Rad2ReflConstants.MERIS_AUTOGROUPING_RAD_STRING)) {
                System.out.println("adding band: " + bandname);
                ProductUtils.copyBand(bandname, l1bProduct, targetProduct, true);
            }
        }
    }

    public static void addRadiance2ReflectanceBands(Product rad2reflProduct, Product targetProduct) {
        addRadiance2ReflectanceBands(rad2reflProduct, targetProduct, 1, 15);
    }

    public static void addRadiance2ReflectanceBands(Product rad2reflProduct, Product targetProduct, int minBand, int maxBand) {
        for (int i = minBand; i <= maxBand; i++) {
            for (String bandname : rad2reflProduct.getBandNames()) {
                if (!targetProduct.containsBand(bandname) &&
                        bandname.startsWith(Rad2ReflConstants.MERIS_AUTOGROUPING_REFL_STRING) &&
                        bandname.endsWith("_" + String.valueOf(i))) {
                    System.out.println("adding band: " + bandname);
                    ProductUtils.copyBand(bandname, rad2reflProduct, targetProduct, true);
                    targetProduct.getBand(bandname).setUnit("dl");
                }
            }
        }
    }

}
