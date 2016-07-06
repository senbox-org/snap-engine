Sentinel-3 Toolbox Release Notes
================================

Changes in S3TBX 4.0
--------------------

###New Features and Important Changes
* New colour classification based on the Forel–Ule scale has been implement for OLCI, MERIS, 
  SeaWiFS and MODIS. Thanks to Hendrik Jan van der Woerd and Marcel R. Wernand from the Royal 
  Netherlands Institute for Sea Research (NOIZ) for the algorithm and the validation. 
* The Sentinel-3 Reader have been improved according errors and format changes have been adapted.
* The fractional Land/Water Mask operator has been moved into SNAP because of its general usability.          

### Solved issues
####Bugs
    [SIIITBX-096] - Read SLSTR L2 WCT oblique view bands
    [SIIITBX-097] - Subset of SeaDas L2 files not correct
    [SIIITBX-099] - Silent error on product type null or empty
    [SIIITBX-102] - Apply solar illumination correction factors to SPOT VGT P products
    [SIIITBX-108] - Reading from NetCDF file is not snychronised in all cases in OLCI reader
    [SIIITBX-112] - Reprojecting SLSTR L1B products with tie-point geo-codings creates shifts within images
    [SIIITBX-113] - S3 SLSTR WST should not use valid mask for its geo-coding

####New Feature
    [SIIITBX-114] - Integrate the colour classification based on discrete Forel–Ule scale

####Task
    [SIIITBX-107] - Move Land/water mask operator into SNAP

####Improvement
    [SIIITBX-098] - Rad2Refl operator is slow
    [SIIITBX-100] - LandsatReader should not search mtl file if it is already specified
    [SIIITBX-104] - Cloud operator should consistently use the system logger
    [SIIITBX-111] - Transfer additional file formats (.tgz) solution for LandsatGeoTiffReader from BEAM to SNAP


Changes in S3TBX 3.0
--------------------

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

