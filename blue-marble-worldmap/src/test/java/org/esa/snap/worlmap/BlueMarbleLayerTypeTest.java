package org.esa.snap.worlmap;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import org.esa.snap.worldmap.BlueMarbleLayerType;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BlueMarbleLayerTypeTest {

    private BlueMarbleLayerType layerType;

    @Before
    public void setUp() {
        layerType = new BlueMarbleLayerType();
    }

    @Test
    public void testGetLabel() {
        assertEquals("NASA Blue Marble", layerType.getLabel());
    }

    @Test
    public void testIsValidFor(){
        final DummyLayerContext ctx = new DummyLayerContext();

        ctx.setCoordinateReferenceSystem("42");
        assertFalse(layerType.isValidFor(ctx));

        ctx.setCoordinateReferenceSystem(DefaultGeographicCRS.WGS84);
        assertTrue(layerType.isValidFor(ctx));
    }

    private static class DummyLayerContext implements LayerContext {

        private Object coordinateReferenceSystem;

        @Override
        public Object getCoordinateReferenceSystem() {
            return coordinateReferenceSystem;
        }

        public void setCoordinateReferenceSystem(Object coordinateReferenceSystem) {
            this.coordinateReferenceSystem = coordinateReferenceSystem;
        }

        @Override
        public Layer getRootLayer() {
            throw new RuntimeException("not implemented");
        }


    }
}
