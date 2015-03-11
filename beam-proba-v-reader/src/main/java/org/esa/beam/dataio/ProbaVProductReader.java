package org.esa.beam.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.IOException;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 11.03.2015
 * Time: 16:25
 *
 * @author olafd
 */
public class ProbaVProductReader extends AbstractProductReader {

    // todo: implement

    static final String PROBAV_L1C_FILENAME_REGEXP =
            "PROBAV_L1C_[0-9]{8}_[0-9]{6}_[0-9]{1}_V003.(?i)(hdf5)";
    static final String PROBAV_S1_TOA_FILENAME_REGEXP =
            "PROBAV_S1_TOA_X[0-9]{2}Y[0-9]{2}_[0-9]{8}_333M_V[0-9]{3}.(?i)(hdf5)";
    static final String PROBAV_S1_TOC_FILENAME_REGEXP =
            "PROBAV_S1_TOC_X[0-9]{2}Y[0-9]{2}_[0-9]{8}_333M_V[0-9]{3}.(?i)(hdf5)";
    static final String PROBAV_S10_TOC_FILENAME_REGEXP =
            "PROBAV_S10_TOC_X[0-9]{2}Y[0-9]{2}_[0-9]{8}_333M_V[0-9]{3}.(?i)(hdf5)";
    static final String PROBAV_S10_TOC_NDVI_FILENAME_REGEXP =
            "PROBAV_S1_TOA_X[0-9]{2}Y[0-9]{2}_[0-9]{8}_333M_NDVI_V[0-9]{3}.(?i)(hdf5)";

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    protected ProbaVProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        return null;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {

    }
}
