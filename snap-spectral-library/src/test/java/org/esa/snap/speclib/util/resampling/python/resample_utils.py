import matplotlib.pyplot as plt
import numpy as np
from matplotlib import colors as mcolors

Array1d = np.ndarray

def plot_spectra(sensor_in, wl_in, spec_in, sensor_resampl, wl_resampl, spec_resampl):
    """
    Plots input and result spectra. The plot is saved in a dedicated png file.

    :param sensor_in: input sensor
    :param wl_in: input wavelengths
    :param spec_in: input spectrum
    :param sensor_resampl: target sensor
    :param wl_resampl: target wavelengths
    :param spec_resampl: resampled spectrum
    :return:
    """
    colors = dict(mcolors.BASE_COLORS, **mcolors.CSS4_COLORS)

    wl_resampl_arr = np.array(wl_resampl)
    spec_resampl_arr = np.array(spec_resampl)
    plt.plot(wl_in, spec_in, color=colors['red'], label=sensor_in,
             linestyle='-', marker="*")
    plt.plot(wl_resampl_arr, spec_resampl_arr, color=colors['limegreen'], label=sensor_resampl,
             linestyle='None', marker="*")

    plt.legend()

    xmin1 = np.nanmin(wl_in)
    xmin2 = np.nanmin(wl_resampl_arr)
    xmin = np.nanmax([xmin1, xmin2])
    xmax1 = np.nanmax(wl_in)
    xmax2 = np.nanmax(wl_resampl_arr)
    xmax = np.nanmin([xmax1, xmax2])
    plt.xlim([0.9 * xmin, 1.1 * xmax])
    ymax = np.nanmax(spec_resampl_arr)
    plt.ylim([0.0, 1.1 * ymax])

    plt.xlabel('Wavelength')
    plt.ylabel('Spectrum')
    plt.title('Spectral resampling: ' + sensor_in + ' --> ' + sensor_resampl)
    plt.grid(True)
    png_file = "plot_" + sensor_in + "_" + sensor_resampl + ".png"
    plt.savefig(png_file)
    plt.show()
