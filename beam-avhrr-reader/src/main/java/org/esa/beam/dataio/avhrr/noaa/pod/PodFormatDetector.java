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

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.IOHandler;
import com.bc.ceres.binio.SequenceData;

import java.io.IOException;
import java.nio.ByteOrder;

/**
 * For detecting NOAA AVHRR HRPT L1B data in POD format.
 *
 * @author Ralf Quast
 */
class PodFormatDetector {

    public boolean canDecode(IOHandler ioHandler) {
        final DataFormat dataFormat = new DataFormat(PodTypes.TBM_HEADER_RECORD_TYPE, ByteOrder.BIG_ENDIAN);
        final DataContext context = dataFormat.createContext(ioHandler);
        try {
            return isTbmHeaderRecord(context.getData());
        } catch (IOException e) {
            return false;
        } finally {
            context.dispose();
        }
    }

    // package public for testing only
    static boolean isTbmHeaderRecord(CompoundData data) throws IOException {
        final String totalOrSelectiveCopy = getString(data, 2);
        if ("T".equals(totalOrSelectiveCopy) || "S".equals(totalOrSelectiveCopy)) {
            final String appendedDataSelection = getString(data, 10);
            if ("Y".equals(appendedDataSelection) || "N".equals(appendedDataSelection)) {
                final String datasetName = getString(data, 1);
                if (datasetName.matches("[A-Z]{3}\\.HRPT\\..*")) {
                    return true;
                }
            }
        }
        return false;
    }

    // package public for testing only
    static String getString(CompoundData data, int index) throws IOException {
        return toString(data.getSequence(index));
    }

    // package public for testing only
    static String toString(SequenceData valueSequence) throws IOException {
        final byte[] data = new byte[valueSequence.getElementCount()];
        for (int i = 0; i < data.length; i++) {
            data[i] = valueSequence.getByte(i);
        }
        return new String(data);
    }
}
