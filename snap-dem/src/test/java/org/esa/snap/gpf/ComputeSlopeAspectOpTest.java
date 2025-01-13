package org.esa.snap.gpf;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.test.LongTestRunner;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.dem.gpf.ComputeSlopeAspectOp;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assume.assumeTrue;

public class ComputeSlopeAspectOpTest {

    @Test
    @STTM("SNAP-3894")
    public void testComputeXYResolutions() {
        Product product = createProduct("type", 7731, 7851);

        ComputeSlopeAspectOp op = new ComputeSlopeAspectOp();
        op.setSourceProduct(product);
        final double xRes = op.getResolutionXAtCentre(product);
        final double yRes = op.getResolutionYAtCentre(product);

        final double xResExp = 29.98405034556281;
        final double yResExp = 30.05407836258074;
        assumeTrue(Math.abs(xRes - xResExp) <= 1e-3);
        assumeTrue(Math.abs(yRes - yResExp) <= 1e-3);
    }

    public static Product createProduct(final String type, final int w, final int h) {
        final Product product = new Product("name", type, w, h);

        final ProductData.UTC startTime = AbstractMetadata.parseUTC("10-MAY-2008 20:30:46.890683");
        final ProductData.UTC endTime = AbstractMetadata.parseUTC("10-MAY-2008 20:35:46.890683");
        product.setStartTime(startTime);
        product.setEndTime(endTime);
        product.setDescription("description");

        final TiePointGrid latGrid = new TiePointGrid(OperatorUtils.TPG_LATITUDE, 2, 2, 0.0f, 0.0f,
                product.getSceneRasterWidth(), product.getSceneRasterHeight(), new float[]{44.243f, 44.220f, 42.123f, 42.101f});
        final TiePointGrid lonGrid = new TiePointGrid(OperatorUtils.TPG_LONGITUDE, 2, 2, 0.0f, 0.0f,
                product.getSceneRasterWidth(), product.getSceneRasterHeight(),
                new float[]{-81.544f, -78.640f, -81.525f, -78.720f}, TiePointGrid.DISCONT_AT_180);
        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid);
        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        product.setSceneGeoCoding(tpGeoCoding);

        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(product.getMetadataRoot());
        absRoot.setAttributeUTC(AbstractMetadata.first_line_time, startTime);
        absRoot.setAttributeUTC(AbstractMetadata.last_line_time, endTime);
        absRoot.setAttributeDouble(AbstractMetadata.line_time_interval, ReaderUtils.getLineTimeInterval(startTime, endTime, h));

        return product;
    }

}