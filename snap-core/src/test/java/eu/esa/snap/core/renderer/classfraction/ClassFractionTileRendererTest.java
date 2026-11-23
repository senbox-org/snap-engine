package eu.esa.snap.core.renderer.classfraction;

import com.bc.ceres.annotation.STTM;
import eu.esa.snap.core.renderer.classfraction.ClassBandGroupConfig.ValidityPolicy;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;


public class ClassFractionTileRendererTest {


    @Test
    @STTM("SNAP-4339")
    public void composesVisibleClassColoursWithoutRenormalizing() {
        ClassFractionTileRenderer renderer = renderer(ValidityPolicy.STRICT_SOURCE_VALIDITY, true, true, 1.0);
        assertEquals(0xFF804000, renderer.compose(new double[]{0.5, 0.25}, new boolean[]{true, true}, true, null));
    }

    @Test
    @STTM("SNAP-4339")
    public void makesNoDataNanAndGroupMaskTransparent() {
        ClassFractionTileRenderer renderer = renderer(ValidityPolicy.STRICT_SOURCE_VALIDITY, true, true, 1.0);

        assertEquals(0, renderer.compose(new double[]{255, 0.5}, new boolean[]{true, true}, true, new double[]{255, Double.NaN}));
        assertEquals(0, renderer.compose(new double[]{Double.NaN, 0.5}, new boolean[]{true, true}, true, null));
        assertEquals(0, renderer.compose(new double[]{0.5, 0.5}, new boolean[]{true, true}, false, null));
    }

    @Test
    @STTM("SNAP-4339")
    public void acceptsFiniteOwtZeroWhenDeclaredValidityPolicyOverridesBandMask() {
        ClassFractionTileRenderer renderer = renderer(ValidityPolicy.FINITE_AND_DECLARED_NODATA, true, true, 1.0);
        assertEquals(0xFF00FF00, renderer.compose(new double[]{0.0, 1.0}, new boolean[]{false, true}, true, null));
    }

    @Test
    @STTM("SNAP-4339")
    public void strictPolicyRejectsMaskedInputAndNoVisibleClassesAreTransparent() {
        assertEquals(0, renderer(ValidityPolicy.STRICT_SOURCE_VALIDITY, true, true, 1.0)
                .compose(new double[]{0, 1}, new boolean[]{false, true}, true, null));
        assertEquals(0, renderer(ValidityPolicy.FINITE_AND_DECLARED_NODATA, false, false, 1.0)
                .compose(new double[]{0.5, 0.5}, new boolean[]{true, true}, true, null));
    }


    @Test
    @STTM("SNAP-4339")
    public void dividesTheFinalWeightedColourByTheConfiguredDisplayScale() {
        ClassFractionTileRenderer renderer = renderer(ValidityPolicy.STRICT_SOURCE_VALIDITY, true, true, 2.0);
        assertEquals(0xFF402000, renderer.compose(new double[]{0.5, 0.25}, new boolean[]{true, true}, true, null));
    }

    private static ClassFractionTileRenderer renderer(ValidityPolicy policy, boolean firstVisible, boolean secondVisible, double displayScale) {
        ClassBandGroupConfig group = new ClassBandGroupConfig(Arrays.asList(
                new ClassBandConfig("p", "a", "a", 0xFFFF0000, firstVisible),
                new ClassBandConfig("p", "b", "b", 0xFF00FF00, secondVisible)),
                displayScale, policy);
        return new ClassFractionTileRenderer(new ClassFractionRendererConfig("test", group));
    }
}
