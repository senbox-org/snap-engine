package eu.esa.snap.core.dataio.cache;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class CacheContextTest {

    @Test
    @STTM("SNAP-4107")
    public void testConstruction() {
        final VariableDescriptor variableDescriptor = new VariableDescriptor();
        variableDescriptor.name = "nasenmann";
        final MockCacheDataProvider dataProvider = new MockCacheDataProvider();

        final CacheContext cacheContext = new CacheContext(variableDescriptor, dataProvider);
        assertSame(variableDescriptor, cacheContext.getVariableDescriptor());
        assertSame(dataProvider, cacheContext.getDataProvider());
        assertEquals("nasenmann", cacheContext.getVariableDescriptor().name);
    }

    private static class MockCacheDataProvider implements CacheDataProvider {
        @Override
        public VariableDescriptor getVariableDescriptor(String variableName) {
            throw new RuntimeException("not implemented");
        }

        @Override
        public ProductData readCacheBlock(String variableName, int[] offsets, int[] shapes, ProductData targetData) throws IOException {
            throw new RuntimeException("not implemented");
        }
    }
}
