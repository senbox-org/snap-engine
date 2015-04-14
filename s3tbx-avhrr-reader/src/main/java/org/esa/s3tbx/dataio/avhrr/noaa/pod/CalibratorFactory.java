package org.esa.s3tbx.dataio.avhrr.noaa.pod;

import org.esa.s3tbx.dataio.avhrr.calibration.Calibrator;

import java.io.IOException;

/**
* @author Ralf Quast
*/
interface CalibratorFactory {

    Calibrator createCalibrator(int i) throws IOException;

    String getBandName();

    String getBandUnit();

    String getBandDescription();
}
