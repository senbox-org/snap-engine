/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.dataio.avhrr.noaa.pod;

import com.bc.ceres.binio.IOHandler;
import com.bc.ceres.binio.util.ImageIOHandler;
import com.bc.ceres.binio.util.RandomAccessFileIOHandler;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.avhrr.BandReader;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A reader for NOAA Polar Orbiter Data (POD) products (currently AVHRR HRPT only).
 *
 * @author Ralf Quast
 * @see <a href="http://www.ncdc.noaa.gov/oa/pod-guide/ncdc/docs/podug/index.htm">NOAA Polar Orbiter Data User's Guide</a>
 */
final class PodAvhrrReader extends AbstractProductReader {

    private PodAvhrrFile avhrrFile;
    private RandomAccessFile raf;

    PodAvhrrReader(PodAvhrrReaderPlugIn plugIn) {
        super(plugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        Object input = getInput();
        final File file = PodAvhrrReaderPlugIn.getInputFile(input);
        IOHandler ioHandler = null;
        String productName = "unkown";
        if (file != null && file.exists()) {
            productName = file.getName();
            raf = new RandomAccessFile(file, "r");
            ioHandler = new RandomAccessFileIOHandler(raf);
//        } else if (input instanceof ImageInputStream) {
//            ImageInputStream iis = (ImageInputStream) input;
//            ioHandler = new ImageIOHandler(iis);
        }
        if (ioHandler == null) {
            throw new IllegalArgumentException();
        }

        final Product product;
        try {
            avhrrFile = new PodAvhrrFile(ioHandler, productName);
            product = avhrrFile.createProduct();
            if (file != null) {
                product.setFileLocation(file);
            }
        } catch (IOException e) {
            try {
                close();
            } catch (IOException ignored) {
            }
            throw e;
        }

        return product;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX,
                                          int sourceOffsetY,
                                          int sourceWidth,
                                          int sourceHeight,
                                          int sourceStepX,
                                          int sourceStepY,
                                          Band targetBand,
                                          int targetOffsetX,
                                          int targetOffsetY,
                                          int targetWidth,
                                          int targetHeight,
                                          ProductData targetBuffer, ProgressMonitor pm) throws IOException {
        final BandReader bandReader = avhrrFile.getBandReader(targetBand);
        if (bandReader == null) {
            throw new IllegalStateException("No band reader available.");
        }

        bandReader.readBandRasterData(sourceOffsetX,
                                      sourceOffsetY,
                                      sourceWidth,
                                      sourceHeight,
                                      sourceStepX,
                                      sourceStepY,
                                      targetBuffer, pm);
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (avhrrFile != null) {
            avhrrFile.dispose();
        }
        if (raf != null) {
            raf.close();
        }
    }

}
