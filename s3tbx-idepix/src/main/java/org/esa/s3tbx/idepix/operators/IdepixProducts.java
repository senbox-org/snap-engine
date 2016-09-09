package org.esa.s3tbx.idepix.operators;

import org.esa.s3tbx.processor.rad2refl.Rad2ReflConstants;
import org.esa.s3tbx.processor.rad2refl.Rad2ReflOp;
import org.esa.s3tbx.processor.rad2refl.Sensor;
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
    public static Product computeRadiance2ReflectanceProduct(Product sourceProduct, Sensor sensor) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("sensor", sensor);
        params.put("copyNonSpectralBands", false);
        return GPF.createProduct(OperatorSpi.getOperatorAlias(Rad2ReflOp.class), params, sourceProduct);
    }

    public static Product computeCloudTopPressureProduct(Product sourceProduct) {
        return GPF.createProduct("Meris.CloudTopPressureOp", GPF.NO_PARAMS, sourceProduct);
    }

    public static void addRadianceBands(Product l1bProduct, Product targetProduct, String[] bandsToCopy) {
        for (String bandname : bandsToCopy) {
            if (!targetProduct.containsBand(bandname) && bandname.contains("radiance")) {
                System.out.println("adding band: " + bandname);
                ProductUtils.copyBand(bandname, l1bProduct, targetProduct, true);
            }
        }
    }

}
