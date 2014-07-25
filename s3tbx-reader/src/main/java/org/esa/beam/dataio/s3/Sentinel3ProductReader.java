package org.esa.beam.dataio.s3;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.s3.olci.OlciLevel1ProductFactory;
import org.esa.beam.dataio.s3.olci.OlciLevel2LProductFactory;
import org.esa.beam.dataio.s3.olci.OlciLevel2WProductFactory;
import org.esa.beam.dataio.s3.slstr.SlstrLevel1ProductFactory;
import org.esa.beam.dataio.s3.slstr.SlstrLstProductFactory;
import org.esa.beam.dataio.s3.slstr.SlstrSstProductFactory;
import org.esa.beam.dataio.s3.slstr.SlstrWstProductFactory;
import org.esa.beam.dataio.s3.synergy.SynLevel2ProductFactory;
import org.esa.beam.dataio.s3.synergy.VgtProductFactory;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.File;
import java.io.IOException;

public class Sentinel3ProductReader extends AbstractProductReader {

    private ProductFactory factory;

    public Sentinel3ProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected final Product readProductNodesImpl() throws IOException {
        final String dirName = getInputFileParentDirectory().getName();

        if (dirName.matches("S3.?_OL_1_E[RF]R_.*.SEN3")) { // OLCI L1b
            factory = new OlciLevel1ProductFactory(this);
        } else if (dirName.matches("S3.?_OL_2_(L[FR]R)_.*.SEN3")) { // OLCI L2 L -
            factory = new OlciLevel2LProductFactory(this);
        } else if (dirName.matches("S3.?_OL_2_(W[FR]R)_.*.SEN3")) { // OLCI L2 W -
            factory = new OlciLevel2WProductFactory(this);
        } else if (dirName.matches("S3.?_SL_1_RBT.*")) { // SLSTR L1b
            factory = new SlstrLevel1ProductFactory(this);
        } else if (dirName.matches("S3.?_SL_2_LST_.*.SEN3")) { // SLSTR L2 LST
            factory = new SlstrLstProductFactory(this);
        } else if (dirName.matches("S3.?_SL_2_WST_.*.SEN3")) { // SLSTR L2 WST
            factory = new SlstrWstProductFactory(this);
        } else if (dirName.matches("S3.?_SL_2_WCT_.*.SEN3")) { // SLSTR L2 WCT
                factory = new SlstrSstProductFactory(this);
        } else if (dirName.matches("S3.?_SY_2_SYN_.*.SEN3")) { // SYN L2
            factory = new SynLevel2ProductFactory(this);
        } else if (dirName.matches("S3.?_SY_(2_VGP|[23]_VG1)_.*.SEN3")) { // SYN VGT
            factory = new VgtProductFactory(this);
        }
        if (factory == null) {
            throw new IOException("Cannot read product file '" + getInputFile() + "'.");
        }

        return factory.createProduct();
    }

    @Override
    protected final void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                                int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                                int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                                ProgressMonitor pm) throws IOException {
        throw new IllegalStateException("Data are provided by images.");
    }

    @Override
    public final void close() throws IOException {
        factory.dispose();
        super.close();
    }

    public final File getInputFile() {
        return new File(getInput().toString());
    }

    public final File getInputFileParentDirectory() {
        return getInputFile().getParentFile();
    }
}
