This Python test implementation extracts parts of relevant code for Spectral Resampling provided in the 
[EnMAP-Box](https://www.enmap.org/data_tools/enmapbox/).

The implementation is stripped down to the resampling of a single spectrum (i.e. one pixel) of a given input
sensor onto the wavelength grid of a given target sensor. Spectral response functions for the target sensor
need to be applied, which are derived from FWHM values given for all target reference wavelength.
Currently, the FWHMs need to taken from csv input files.

Resampling tests for various combinations of input/target sensors were set up. Input and result spectra of these tests
are used in JUnit tests of the corresponding Spectral Resampling Java implementation in this module.

[EnMAP-Box Github source code repository](https://github.com/EnMAP-Box/enmap-box)

[EnMAP-Box documentation on Spectral Resampling](https://enmap-box.readthedocs.io/en/release_3.12/usr_section/usr_manual/processing_algorithms/spectral_resampling/index.html)
