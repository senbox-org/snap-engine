#New in Sentinel-3 Toolbox v3.0

###New Features and Important Changes
* The Idepix Processor provides a pixel classification into properties such as clear/cloudy, land/water, snow, ice etc. The processing 
options/parameters as well as the underlying classification algorithms are instrument-dependent. The Idepix Processor provided with the current 
SNAP version supports MODIS and Landsat-8.
* The ARC Processor is aimed to enable the user to calculate the sea-surface temperature and Saharan Dust Index from (A)ATSR brightness temperatures.
* The new SLSTR L1B PDU Stitching Tool stitches multiple SLSTR L1B product dissemination units (PDUs) of the same orbit to a single product.
* A new client tool has been developed for accessing online in-situ databases. In the current version this tool has the purpose of a demonstrator 
and is limited in functionality. Currently the In-Situ Client gives limited access to the [MERMAID In-Situ Database](http://mermaid.acri.fr/home/home.php)
hosted by ACRI-ST. Two datasets are available, namely BOUSSOLE and AERONET-OC.
* The Fractional Land/Water Mask Processor creates a new product based on the source product and computes a land/water mask. For each pixel, 
it contains the fraction of water; a value of 0.0 indicates land, a value of 100.0 indicates water, and every value in between indicates 
a mixed pixel.

A comprehensive list of all issues resolved in this version of the Sentinel-3 Toolbox can be found in our 
[issue tracking system](https://senbox.atlassian.net/issues/?filter=11509)

#Release notes of former versions

* [Resolved issues in version 2.x](https://senbox.atlassian.net/issues/?filter=11508)
* [Resolved issues in version 2.0](https://senbox.atlassian.net/issues/?filter=11507)
* [Resolved issues in version 2.0 beta](https://senbox.atlassian.net/issues/?filter=11506)
* [Resolved issues in version 1.0.1](https://senbox.atlassian.net/issues/?filter=11505)

