package eu.esa.snap.core.renderer.classfraction;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.binding.PropertySet;
import eu.esa.snap.core.renderer.classfraction.ClassBandGroupConfig.ValidityPolicy;

import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


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

    @Test
    @STTM("SNAP-4339")
    public void recognisesCurrentAndLegacyRendererTypeNames() {
        assertTrue(ClassFractionImageLayerType.isRendererTypeName(ClassFractionImageLayerType.class.getName()));
        assertTrue(ClassFractionImageLayerType.isRendererTypeName(ClassFractionImageLayerType.LEGACY_TYPE_NAME));
        assertFalse(ClassFractionImageLayerType.isRendererTypeName("RgbImageLayerType"));
    }

    @Test
    @STTM("SNAP-4339")
    public void rejectsMissingChangedOrUnresolvedLayerSources() {
        ClassFractionImageLayerType type = new ClassFractionImageLayerType();
        ClassFractionRendererConfig config = config("p", "a", "b");

        assertLayerCreationFails("Class renderer product is missing", () -> {
            PropertySet properties = type.createLayerConfig(null);
            properties.setValue(ClassFractionImageLayerType.PROPERTY_NAME_CONFIG_JSON, ClassFractionRendererConfigJson.encode(config));
            type.createLayer(null, properties);
        });

        Product product = new Product("p", "t", 2, 1);
        addBand(product, "a");
        addBand(product, "b");
        assertLayerCreationFails("Class renderer source product changed: changed", () ->
                type.createLayer(product, config("changed", "a", "b")));
        assertLayerCreationFails("Class renderer source is unresolved: missing", () ->
                type.createLayer(product, config("p", "a", "missing")));
    }


    private static void addBand(Product product, String name) {
        Band band = product.addBand(name, ProductData.TYPE_FLOAT32);
        band.setRasterData(ProductData.createInstance(new float[]{0.5f, 0.5f}));
    }

    private static ClassBandConfig entry(String name, int color) {
        return new ClassBandConfig("p", name, name, color, true);
    }

    private static ClassFractionRendererConfig config(String productName, String firstBand, String secondBand) {
        return new ClassFractionRendererConfig("classes",
                new ClassBandGroupConfig(Arrays.asList(new ClassBandConfig(productName, firstBand, firstBand, 0xFFFF0000, true),
                        new ClassBandConfig(productName, secondBand, secondBand, 0xFF00FF00, true)),
                        1.0, ValidityPolicy.STRICT_SOURCE_VALIDITY));
    }

    private static void assertLayerCreationFails(String expectedMessage, Runnable action) {
        try {
            action.run();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertEquals(expectedMessage, expected.getMessage());
        }
    }
}
