from typing import List, Dict, Tuple, OrderedDict, Any

import numpy as np

Array1d = np.ndarray

def resample_spectrum(
        input_spec: Array1d, input_wvls: Array1d, srf: Dict[str, List[Tuple[int, float]]], no_data_value: float
) -> list[Any]:
    """
    Resamples an input spectrum given on a source wavelength array onto a target wavelength array.
    Implementation follows EnMAP Box Python code:

    https://github.com/EnMAP-Box/enmap-box/blob/main/enmapboxprocessing/algorithm/spectralresamplingbyresponsefunctionconvolutionalgorithmbase.py

    :param input_spec: input spectrum
    :param input_wvls: input wavelengths
    :param srf: Spectral Response functions: dictionary of pairs (refWvl, dict(wvl, weight))
    :param no_data_value: no data value

    :return: resampled spectrum (list of spectral values on target wavelength grid)
    """
    input_wvl = [int(round(v)) for v in input_wvls]
    resampled_spec = list()
    for name in srf:
        weights_by_resampled_wavelength = dict(srf[name])
        indices = list()
        weights = list()
        for index, wl in enumerate(input_wvl):
            weight = weights_by_resampled_wavelength.get(wl)
            if weight is not None:
                indices.append(index)
                weights.append(weight)
        if len(indices) == 0:
            message = f'no source bands ({min(input_wvl)} to {max(input_wvl)} nanometers) ' \
                      f'are covered by target band "{name}" ' \
                      f'({min(weights_by_resampled_wavelength.keys())} to {max(weights_by_resampled_wavelength.keys())} nanometers), ' \
                      f'which will result in output band filled with no data values'
            print(message)
            resampled_spec.append(np.full_like(input_spec[0], no_data_value, dtype=input_spec[0].dtype))
        else:
            specarray = np.asarray(input_spec, np.float32)[indices]
            warray = np.array(weights) * np.ones_like(specarray)
            result = np.nansum(specarray * warray, 0) / np.nansum(warray, 0)
            resampled_spec.append(round(result, 4))

    return resampled_spec


def get_fully_defined_srf(fwhm_list):
    """
    Provides a dict of fully defined Spectral Response Functions, each of them defined as pairs of (wvl, weight)
    around a given reference wavelength. A fully defined SRF is a Gaussian function retrieved from
    an input pair (refWvl, FWHM).
    See more details at

    https://en.wikipedia.org/wiki/Full_width_at_half_maximum

    :param fwhm_list: dictionary of pairs (wvl, fwhm)
    :return: fully defined Spectral Response functions: dictionary of pairs (refWvl, dict(wvl, weight))
    """
    srf_full = OrderedDict()
    for i, (k, v) in enumerate(fwhm_list.items()):
        if isinstance(k, (int, float)):
            fwhm = float(v)
            sigma = fwhm / 2.355
            x0 = float(k)
            xs = list()
            weights = list()
            a = 2 * sigma ** 2
            b = sigma * np.sqrt(2 * np.pi)
            for x in range(int(x0 - sigma * 3), int(x0 + sigma * 3) + 2):
                c = -(x - x0) ** 2
                fx = np.exp(c / a) / b
                weights.append(fx)
                xs.append(x)
            weights = np.divide(weights, np.max(weights))  # scale to 0-1 range
            srf_full[f'band {i + 1} ({x0} Nanometers)'] = [(x, round(w, 3)) for x, w in zip(xs, weights)]
        else:
            srf_full[k] = v

    return srf_full
