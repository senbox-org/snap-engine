package eu.esa.snap.core.renderer.classfraction;

import com.bc.ceres.annotation.STTM;
import eu.esa.snap.core.renderer.classfraction.ClassBandGroupConfig.ValidityPolicy;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.awt.image.RenderedImage;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;


public class ClassFractionMultiLevelSourceTest {


    @Test
    @STTM("SNAP-4339")
    public void createsArgbImageFromMoreThanThreeInputsAndCachesLevel() {
        Product product = new Product("p", "t", 2, 1);

        Band red = band(product, "red", new float[]{0.5f, 255f});
        red.setNoDataValueUsed(true);
        red.setNoDataValue(255);
        Band green = band(product, "green", new float[]{0.25f, 0.25f});
        Band blue = band(product, "blue", new float[]{0, 0});
        Band unused = band(product, "unused", new float[]{0, 0});

        ClassFractionMultiLevelSource source = new ClassFractionMultiLevelSource(config(), Arrays.asList(red, green, blue, unused));
        RenderedImage image = source.getImage(0);

        assertEquals(0xFF804000, ((int[]) image.getData().getDataElements(0, 0, null))[0]);
        assertEquals(0, ((int[]) image.getData().getDataElements(1, 0, null))[0]);
        assertSame(image, source.getImage(0));
        source.reset();
    }


    private static Band band(Product product, String name, float[] values) {
        Band band = product.addBand(name, ProductData.TYPE_FLOAT32);
        band.setRasterData(ProductData.createInstance(values));
        return band;
    }

    private static ClassFractionRendererConfig config() {
        return new ClassFractionRendererConfig(
                "four",
                new ClassBandGroupConfig(Arrays.asList(entry("red", 0xFFFF0000),
                        entry("green", 0xFF00FF00),
                        entry("blue", 0xFF0000FF),
                        entry("unused", 0)),
                        1.0,
                        ValidityPolicy.STRICT_SOURCE_VALIDITY)
        );
    }

    private static ClassBandConfig entry(String band, int color) {
        return new ClassBandConfig("p", band, band, color, true);
    }
}
