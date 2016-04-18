/*
 * AVISA software - $Id: PlainBandReader.java,v 1.1.1.1 2007/03/22 11:12:51 ralf Exp $
 *
 * Copyright (C) 2005 by EUMETSAT
 *
 * The Licensee acknowledges that the AVISA software is owned by the European
 * Organisation for the Exploitation of Meteorological Satellites
 * (EUMETSAT) and the Licensee shall not transfer, assign, sub-licence,
 * reproduce or copy the AVISA software to any third party or part with
 * possession of this software or any part thereof in any way whatsoever.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.
 * The AVISA software has been developed using the ESA BEAM software which is
 * distributed under the GNU General Public License (GPL).
 *
 */
package org.esa.s3tbx.dataio.avhrr.metop;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.dataio.avhrr.AvhrrFile;
import org.esa.s3tbx.dataio.avhrr.BandReader;
import org.esa.snap.core.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * Reads cloud information directly from the METOP product.
 *
 * @author marcoz
 */
class CloudBandReader implements BandReader {

    protected MetopFile metopFile;

    protected final ImageInputStream inputStream;

    public CloudBandReader(MetopFile metopFile,
                           ImageInputStream inputStream) {
        this.metopFile = metopFile;
        this.inputStream = inputStream;
    }

    @Override
    public String getBandDescription() {
        return "CLAVR-x cloud mask";
    }

    @Override
    public String getBandName() {
        return "cloud_flags";
    }

    @Override
    public String getBandUnit() {
        return null;
    }

    @Override
    public int getDataType() {
        return ProductData.TYPE_UINT16;
    }

    @Override
    public double getScalingFactor() {
        return 1.0;
    }

    @Override
    public void readBandRasterData(int sourceOffsetX,
                                   int sourceOffsetY,
                                   int sourceWidth,
                                   int sourceHeight,
                                   int sourceStepX,
                                   int sourceStepY,
                                   final ProductData destBuffer,
                                   final ProgressMonitor pm) throws IOException {
        
        AvhrrFile.RawCoordinates rawCoord = metopFile.getRawCoordinates(
                sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight);
        final short[] targetData = (short[]) destBuffer.getElems();

        pm.beginTask(MessageFormat.format("Reading AVHRR band ''{0}''...", getBandName()),
                     rawCoord.maxY - rawCoord.minY);

        int targetIdx = rawCoord.targetStart;
        for (int sourceY = rawCoord.minY; sourceY <= rawCoord.maxY; sourceY += sourceStepY) {
            if (pm.isCanceled()) {
                break;
            }

            final int dataOffset = getDataOffset(sourceOffsetX, sourceY);
            synchronized (inputStream) {
                inputStream.seek(dataOffset);
                inputStream.readFully(targetData, targetIdx, sourceWidth);
            }
            targetIdx += sourceWidth;
            pm.worked(1);
        }
        pm.done();

    }

    protected int getDataOffset(int sourceOffsetX, int sourceY) {
        return metopFile.getScanLineOffset(sourceY) + 22472  + ((metopFile.getNumTrimX() + sourceOffsetX) * 2);
    }
}