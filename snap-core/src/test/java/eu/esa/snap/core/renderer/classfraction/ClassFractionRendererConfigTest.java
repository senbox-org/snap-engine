package eu.esa.snap.core.renderer.classfraction;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.multilevel.MultiLevelModel;
import com.bc.ceres.multilevel.support.DefaultMultiLevelImage;
import com.bc.ceres.multilevel.support.DefaultMultiLevelModel;
import com.bc.ceres.multilevel.support.DefaultMultiLevelSource;
import eu.esa.snap.core.renderer.classfraction.ClassBandGroupConfig.ValidityPolicy;

import org.junit.Test;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

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

    @Test
    @STTM("SNAP-4339")
    public void rejectsBlankPersistentNamesAndNullBandEntries() {
        assertIllegalArgument("productName must not be blank", () -> new ClassBandConfig(" ", "class_1", "one", 0, true));
        assertIllegalArgument("bandName must not be blank", () -> new ClassBandConfig("p", "", "one", 0, true));
        assertIllegalArgument("displayName must not be blank", () -> new ClassBandConfig("p", "class_1", "\t", 0, true));
        assertIllegalArgument("bands must not contain null", () -> new ClassBandGroupConfig(Arrays.asList(
                band("p", "class_1", "one", 0), null), 1.0, ValidityPolicy.STRICT_SOURCE_VALIDITY));
    }

    @Test
    @STTM("SNAP-4339")
    public void rejectsBlankRendererDisplayName() {
        assertIllegalArgument("displayName must not be blank", () -> new ClassFractionRendererConfig(" ", group("p")));
    }

    @Test
    @STTM("SNAP-4339")
    public void rejectsMissingAndMalformedScalarJsonPayloads() {
        assertIllegalArgument("Class renderer configuration payload is missing", () -> ClassFractionRendererConfigJson.decode(null));
        assertIllegalArgument("Invalid class renderer configuration payload", () -> ClassFractionRendererConfigJson.decode("{}"));

        String valid = ClassFractionRendererConfigJson.encode(new ClassFractionRendererConfig("classes", group("p")));
        assertIllegalArgument("Invalid class renderer band payload", () -> ClassFractionRendererConfigJson.decode(valid.replace("},{", "}x{")));
        assertIllegalArgument("Invalid class renderer band payload", () -> ClassFractionRendererConfigJson.decode(
                "{\"displayName\":\"YQ==\",\"displayScale\":1.0,\"validity\":\"STRICT_SOURCE_VALIDITY\",\"bands\":[invalid]}"));
    }

    @Test
    @STTM("SNAP-4339")
    public void rejectsIncompleteOrIncompatibleResolvedSources() {
        Product product = productWithBands();
        Band first = product.getBand("class_1");
        Band second = product.getBand("class_2");
        ClassFractionRendererConfig config = new ClassFractionRendererConfig("classes", group(product.getName()));

        assertIllegalArgument("Configured class sources and resolved rasters must have the same size",
                () -> ClassFractionRendererValidator.validate(config, Arrays.asList(first)));
        assertIllegalArgument("A product-owned anchor raster is required",
                () -> ClassFractionRendererValidator.validate(config, Arrays.asList(null, second)));
        assertIllegalArgument("Resolved raster must not be null",
                () -> ClassFractionRendererValidator.validate(config, Arrays.asList(first, null)));
        assertIllegalArgument("Duplicate resolved class source: class_1",
                () -> ClassFractionRendererValidator.validate(config, Arrays.asList(first, first)));

        Product otherProduct = new Product("other", "t", 4, 3);
        Band foreign = otherProduct.addBand("class_2", ProductData.TYPE_FLOAT32);
        assertIllegalArgument("All class sources must belong to one product",
                () -> ClassFractionRendererValidator.validate(config, Arrays.asList(first, foreign)));
    }

    @Test
    @STTM("SNAP-4339")
    public void rejectsSourcesWithIncompatibleBoundsAndPyramidModels() {
        Product product = new Product("p", "t", 4, 3);
        Band first = product.addBand("class_1", ProductData.TYPE_FLOAT32);
        Band smaller = new Band("class_2", ProductData.TYPE_FLOAT32, 2, 3);
        product.addBand(smaller);
        ClassFractionRendererConfig config = new ClassFractionRendererConfig("classes", group(product.getName()));

        assertIllegalArgument("Class sources must have equal raster bounds",
                () -> ClassFractionRendererValidator.validate(config, Arrays.asList(first, smaller)));

        Band second = product.addBand("class_3", ProductData.TYPE_FLOAT32);
        ClassFractionRendererConfig pyramidConfig = new ClassFractionRendererConfig("classes",
                new ClassBandGroupConfig(Arrays.asList(band("p", "class_1", "one", 0),
                        band("p", "class_3", "two", 0)), 1.0, ValidityPolicy.STRICT_SOURCE_VALIDITY));
        setMultiLevelImage(first, new DefaultMultiLevelModel(1, new AffineTransform(), 4, 3));
        setMultiLevelImage(second, new DefaultMultiLevelModel(2, new AffineTransform(), 4, 3));
        assertIllegalArgument("Class sources must have compatible multi-level models",
                () -> ClassFractionRendererValidator.validate(pyramidConfig, Arrays.asList(first, second)));

        setMultiLevelImage(first, new DefaultMultiLevelModel(2, new AffineTransform(), 4, 3));
        setMultiLevelImage(second, new DifferentlyScaledMultiLevelModel(2, new AffineTransform(), 4, 3));
        assertIllegalArgument("Class sources must have compatible multi-level models",
                () -> ClassFractionRendererValidator.validate(pyramidConfig, Arrays.asList(first, second)));

        setMultiLevelImage(second, new DifferentlyTransformedMultiLevelModel(2, new AffineTransform(), 4, 3));
        assertIllegalArgument("Class sources must share the same raster grid transform",
                () -> ClassFractionRendererValidator.validate(pyramidConfig, Arrays.asList(first, second)));
    }


    private static ClassBandConfig band(String product, String name, String label, int color) {
        return new ClassBandConfig(product, name, label, color, true);
    }

    private static ClassBandGroupConfig group(String productName) {
        return new ClassBandGroupConfig(Arrays.asList(band(productName, "class_1", "one", 0),
                band(productName, "class_2", "two", 0)), 1.0, ValidityPolicy.STRICT_SOURCE_VALIDITY);
    }

    private static Product productWithBands() {
        Product product = new Product("p", "t", 4, 3);
        product.addBand("class_1", ProductData.TYPE_FLOAT32);
        product.addBand("class_2", ProductData.TYPE_FLOAT32);
        return product;
    }

    private static void setMultiLevelImage(Band band, MultiLevelModel model) {
        BufferedImage image = new BufferedImage(band.getRasterWidth(), band.getRasterHeight(), BufferedImage.TYPE_BYTE_GRAY);
        band.setSourceImage(new DefaultMultiLevelImage(new DefaultMultiLevelSource(image, model)));
    }

    private static void assertIllegalArgument(String expectedMessage, Runnable action) {
        try {
            action.run();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertEquals(expectedMessage, expected.getMessage());
        }
    }

    private static final class DifferentlyScaledMultiLevelModel extends DefaultMultiLevelModel {
        private DifferentlyScaledMultiLevelModel(int levelCount, AffineTransform transform, int width, int height) {
            super(levelCount, transform, width, height);
        }

        @Override
        public double getScale(int level) {
            return level == 0 ? super.getScale(level) : 3.0;
        }
    }

    private static final class DifferentlyTransformedMultiLevelModel implements MultiLevelModel {
        private final DefaultMultiLevelModel delegate;

        private DifferentlyTransformedMultiLevelModel(int levelCount, AffineTransform transform, int width, int height) {
            this.delegate = new DefaultMultiLevelModel(levelCount, transform, width, height);
        }

        @Override
        public int getLevelCount() {
            return delegate.getLevelCount();
        }

        @Override
        public int getLevel(double scale) {
            return delegate.getLevel(scale);
        }

        @Override
        public double getScale(int level) {
            return delegate.getScale(level);
        }

        @Override
        public AffineTransform getImageToModelTransform(int level) {
            AffineTransform transformed = delegate.getImageToModelTransform(level);
            if (level == 1) {
                transformed.translate(1.0, 0.0);
            }
            return transformed;
        }

        @Override
        public AffineTransform getModelToImageTransform(int level) {
            return delegate.getModelToImageTransform(level);
        }

        @Override
        public Rectangle2D getModelBounds() {
            return delegate.getModelBounds();
        }
    }
}
