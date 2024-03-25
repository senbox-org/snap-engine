package org.esa.snap.gpf;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.test.LongTestRunner;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.dem.gpf.AddElevationOp;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

@RunWith(LongTestRunner.class)
public class AddElevationOpTest {

    private static final String PRODUCT_NAME =
            "subset_0_of_MER_FR__1PNEPA20060730_093924_000000982049_00480_23079_0746.dim";
    private static final String DEM_NAME = "45N015E_5M.ACE2";

    @Test
    public void testAddingExternalDEM() throws IOException, URISyntaxException {
        final Map<String, Object> parametersMap = new HashMap<>();
        final File externalDEMFile = new File(getResourcePath(DEM_NAME));
        parametersMap.put("demName", "External DEM");
        parametersMap.put("externalDEMFile", externalDEMFile);
        parametersMap.put("externalDEMNoDataValue", -500.0);
        parametersMap.put("demResamplingMethod", ResamplingFactory.BILINEAR_INTERPOLATION_NAME);
        final String pathToProduct = getResourcePath(PRODUCT_NAME);
        final Product sourceProduct = ProductIO.readProduct(pathToProduct);
        final Product elevationProduct = GPF.createProduct("AddElevation", parametersMap, sourceProduct);

        final Band elevationBand = elevationProduct.getBand("elevation");
        assertNotNull(elevationBand);
        assertEquals(-500.0, elevationBand.getNoDataValue(), 1e-8);
        final float sampleFloat = elevationBand.getSampleFloat(37, 29);
        assertEquals(38.6651268, sampleFloat, 1e-8);
    }

    private static String getResourcePath(String name) throws URISyntaxException {
        URL url = AddElevationOpTest.class.getResource(name);
        URI uri = new URI(url.toString());
        return uri.getPath();
    }

    @Test
    @STTM("SNAP-3576")
    public void testDoExecute() {
        Product product = TestUtils.createProduct("type", 10, 10);

        AddElevationOp op = new AddElevationOp();
        op.setSourceProduct(product);
        op.setParameter("demName", "Copernicus 90m Global DEM");
        Product trgProduct = op.getTargetProduct();
        assertNotNull(trgProduct);

        op.doExecute(ProgressMonitor.NULL);
    }

    @Test
    @STTM("SNAP-3576")
    public void testDoExecuteError() {
        Product product = TestUtils.createProduct("type", 10, 10);

        AddElevationOp op = new AddElevationOp();
        op.setSourceProduct(product);
        op.setParameter("demName", "");
        op.setParameter("demResamplingMethod", "");
        Product trgProduct = op.getTargetProduct();
        assertNotNull(trgProduct);

        Exception exception = assertThrows(OperatorException.class, () -> op.doExecute(ProgressMonitor.NULL));
        System.out.println(exception.getMessage());
        assertEquals("DEM name is not specified.", exception.getMessage());
    }


} 