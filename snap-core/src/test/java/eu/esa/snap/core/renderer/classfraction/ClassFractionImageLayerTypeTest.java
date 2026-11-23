package eu.esa.snap.core.renderer.classfraction;

import com.bc.ceres.annotation.STTM;
import eu.esa.snap.core.renderer.classfraction.ClassBandGroupConfig.ValidityPolicy;

import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class ClassFractionImageLayerTypeTest {


    @Test
    @STTM("SNAP-4339")
    public void createsLayerWithOnlyProductAndScalarConfiguration() {
        Product product = new Product("p", "t", 2, 1);
        addBand(product, "a");
        addBand(product, "b");

        ClassFractionRendererConfig config = new ClassFractionRendererConfig("classes",
                new ClassBandGroupConfig(Arrays.asList(entry("a", 0xFFFF0000),
                        entry("b", 0xFF00FF00)), 1.0, ValidityPolicy.STRICT_SOURCE_VALIDITY)
        );

        ImageLayer layer = new ClassFractionImageLayerType().createLayer(product, config);
        assertEquals(product, layer.getConfiguration().getValue(ClassFractionImageLayerType.PROPERTY_NAME_PRODUCT));

        String payload = layer.getConfiguration().getValue(ClassFractionImageLayerType.PROPERTY_NAME_CONFIG_JSON);
        assertEquals("classes", ClassFractionRendererConfigJson.decode(payload).getDisplayName());
        assertNotNull(layer.getConfiguration().getValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE));
    }


    private static void addBand(Product product, String name) {
        Band band = product.addBand(name, ProductData.TYPE_FLOAT32);
        band.setRasterData(ProductData.createInstance(new float[]{0.5f, 0.5f}));
    }

    private static ClassBandConfig entry(String name, int color) {
        return new ClassBandConfig("p", name, name, color, true);
    }
}
