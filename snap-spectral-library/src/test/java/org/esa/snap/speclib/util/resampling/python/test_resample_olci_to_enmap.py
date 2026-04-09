import numpy as np

import resample_algorithm
import resample_utils

Array1d = np.ndarray

if __name__ == "__main__":

    # test with one pixel taken from
    # S3A_OL_1_ERR____20160715T101217_20160715T101417_20171002T010140_0119_006_236______MR1_R_NT_002.SEN3:

    input_spectrum = np.array([105.33761, 115.1706, 120.02343, 116.7073, 114.318726, 114.59385, 133.63246, 138.82625,
                               138.58409, 138.04797, 132.03772, 131.85347, 37.904022, 65.76642, 114.50595, 122.30608,
                               100.14883, 96.83181, 64.75161, 20.552023, 81.00681])

    input_wvls = np.array([400.1732, 411.75812, 442.95776, 490.5534, 510.52353, 560.5521, 620.395, 665.3918,
                           674.1551, 681.66376, 709.24176, 754.2953, 761.8105, 764.92523, 768.0407, 779.3815,
                           865.4787, 884.3511, 899.3343, 938.9488, 1015.7598])

    input_sensor = 'OLCI'
    resampl_sensor = 'ENMAP'

    # read FWHMs from csv file
    fwhm_csv_file = 'FWHM_' + resampl_sensor + '.csv'
    fwhm_table = np.loadtxt(fwhm_csv_file, delimiter=",", dtype=str)
    fwhm_wvls = list(map(float, fwhm_table[1:, 0].tolist()))
    fwhm_values = list(map(float, fwhm_table[1:, 1].tolist()))

    # read FWHMs and retrieve spectral response functions for all target wavelengths
    srf_dict = resample_algorithm.get_fully_defined_srf(dict(zip(fwhm_wvls, fwhm_values)))

    # resample spectrum
    resampl_spectrum = resample_algorithm.resample_spectrum(input_spectrum, input_wvls, srf_dict, 0.0)

    # plot input and resampled spectrum
    resample_utils.plot_spectra(input_sensor, input_wvls, input_spectrum, resampl_sensor, fwhm_wvls, resampl_spectrum)

    print('done')
