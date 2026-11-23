package eu.esa.snap.core.renderer.classfraction;

import com.bc.ceres.annotation.STTM;
import eu.esa.snap.core.renderer.classfraction.ClassBandGroupConfig.ValidityPolicy;

import org.junit.Test;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import java.util.Arrays;
import java.awt.geom.AffineTransform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class ClassFractionRendererConfigTest {


    @Test
    @STTM("SNAP-4339")
    public void preservesOrderedBandMetadataAndDisplayScale() {
        ClassBandConfig first = band("p", "probability_0", "Cluster 0", 0xFFFF0000);
        ClassBandConfig second = band("p", "probability_1", "Cluster 1", 0xFF00FF00);
        ClassBandGroupConfig group = new ClassBandGroupConfig(Arrays.asList(first, second), 1.0, ValidityPolicy.STRICT_SOURCE_VALIDITY);
        ClassFractionRendererConfig config = new ClassFractionRendererConfig("EM probabilities", group);

        assertEquals(1.0, config.getClassBandGroup().getDisplayScale(), 0.0);
        assertEquals("probability_0", config.getClassBandGroup().getBands().get(0).getBandName());
        assertEquals(0xFF00FF00, config.getClassBandGroup().getBands().get(1).getArgb());
    }

    @Test
    @STTM("SNAP-4339")
    public void rejectsDuplicatePersistentSources() {
        try {
            new ClassBandGroupConfig(Arrays.asList(band("p", "class_1", "one", 0),
                    band("p", "class_1", "two", 0)),
                    1.0, ValidityPolicy.FINITE_AND_DECLARED_NODATA);
            fail("Expected duplicate source rejection");
        } catch (IllegalArgumentException expected) {
            assertEquals("Duplicate class source: class_1", expected.getMessage());
        }
    }

    @Test
    @STTM("SNAP-4339")
    public void scalarJsonRoundTripKeepsConfiguration() {
        ClassBandGroupConfig group = new ClassBandGroupConfig(Arrays.asList(band("p", "class_1", "A, quoted label", 0x80FF0000),
                band("p", "class_2", "B", 0xFF0000FF)),
                2.5, ValidityPolicy.FINITE_AND_DECLARED_NODATA);
        ClassFractionRendererConfig restored = ClassFractionRendererConfigJson.decode(
                ClassFractionRendererConfigJson.encode(new ClassFractionRendererConfig("OWT classes", group))
        );

        assertEquals("OWT classes", restored.getDisplayName());
        assertEquals(ValidityPolicy.FINITE_AND_DECLARED_NODATA, restored.getClassBandGroup().getValidityPolicy());
        assertEquals(2.5, restored.getClassBandGroup().getDisplayScale(), 0.0);
        assertEquals("A, quoted label", restored.getClassBandGroup().getBands().get(0).getDisplayName());
    }

    @Test(expected = IllegalArgumentException.class)
    @STTM("SNAP-4339")
    public void rejectsAGroupWithOnlyOneBand() {
        new ClassBandGroupConfig(Arrays.asList(band("p", "class_1", "one", 0)), 1.0, ValidityPolicy.STRICT_SOURCE_VALIDITY);
    }

    @Test
    @STTM("SNAP-4339")
    public void validatesResolvedSameGridSources() {
        Product product = new Product("p", "t", 4, 3);
        Band first = product.addBand("class_1", ProductData.TYPE_FLOAT32);
        Band second = product.addBand("class_2", ProductData.TYPE_FLOAT32);

        ClassFractionRendererValidator.validate(new ClassFractionRendererConfig("classes",
                        new ClassBandGroupConfig(Arrays.asList(band("p", "class_1", "one", 0),
                                band("p", "class_2", "two", 0)),
                                1.0, ValidityPolicy.STRICT_SOURCE_VALIDITY)),
                        Arrays.asList(first, second)
        );
    }

    @Test
    @STTM("SNAP-4339")
    public void rejectsSameSizedSourcesOnDifferentRasterGrids() {
        Product product = new Product("p", "t", 4, 3);
        Band first = product.addBand("class_1", ProductData.TYPE_FLOAT32);
        Band second = product.addBand("class_2", ProductData.TYPE_FLOAT32);
        second.setImageToModelTransform(AffineTransform.getTranslateInstance(1.0, 0.0));

        try {
            ClassFractionRendererValidator.validate(new ClassFractionRendererConfig(
                    "classes", new ClassBandGroupConfig(Arrays.asList(
                    band("p", "class_1", "one", 0), band("p", "class_2", "two", 0)),
                    1.0, ValidityPolicy.STRICT_SOURCE_VALIDITY)),
                    Arrays.asList(first, second));
            fail("Expected raster-grid rejection");
        } catch (IllegalArgumentException expected) {
            assertEquals("Class sources must share the same raster grid transform", expected.getMessage());
        }
    }

    @Test
    @STTM("SNAP-4339")
    public void rejectsResolvedSourcesThatDoNotMatchTheirPersistentIdentity() {
        Product product = new Product("p", "t", 4, 3);
        Band first = product.addBand("class_1", ProductData.TYPE_FLOAT32);
        Band second = product.addBand("class_2", ProductData.TYPE_FLOAT32);
        ClassFractionRendererConfig config = new ClassFractionRendererConfig("classes",
                new ClassBandGroupConfig(Arrays.asList(
                        band("p", "class_2", "two", 0),
                        band("p", "class_1", "one", 0)),
                        1.0,
                        ValidityPolicy.STRICT_SOURCE_VALIDITY)
        );

        try {
            ClassFractionRendererValidator.validate(config, Arrays.asList(first, second));
            fail("Expected source identity rejection");
        } catch (IllegalArgumentException expected) {
            assertEquals("Configured class source does not match resolved raster: class_1", expected.getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    @STTM("SNAP-4339")
    public void rejectsNonPositiveDisplayScale() {
        new ClassBandGroupConfig(Arrays.asList(band("p", "class_1", "one", 0),
                band("p", "class_2", "two", 0)), 0.0, ValidityPolicy.STRICT_SOURCE_VALIDITY);
    }


    private static ClassBandConfig band(String product, String name, String label, int color) {
        return new ClassBandConfig(product, name, label, color, true);
    }
}
