
What's new in 2.0
=================

New Features and Improvements
-----------------------------

* Updated Sentinel-3 OLCI and SLSTR data readers
  - to work with latest TDS; 
  - to work with SLSTR L1b multi-size/-resolutions; 
  - new OLCI RGB profiles;
  - to work with new uncertainty propagation in band-maths;
  - drag-and-drop of OLCI and SLSTR manifest files or directories into SNAP Desktop.
* Added new data reader for Envisat MERIS in SAFE format (= "Sentinels format")
* Added new preliminary data reader for Sentinel-3 L1C Synergy data products
* Added new Sentinel-3 Toolbox settings page in Tools / Options dialog
  - switch to either read all profile tie-point data or only the first grid;
  - switch to either use tie-point based or per-pixel geo-coding, and others.
* New Sentinel-3 SLSTR PDU Stitching Tool (command-line and GUI)
* New PROBA-V data product reader
* Updated Landsat-8 data reader w.r.t. multi-size bands support 

Other issues fixed
------------------

See also https://senbox.atlassian.net/issues/?filter=11103

[SIIITBX-60]	The BandMaths Operator cannot handle $sourceProducts#.bandName	(Bug)
[SIIITBX-65]	Radiometric correction operator fails if bands are not in expected order	(Bug)
[SIIITBX-67]	Allow switching between radiance and reflectance values for landsat data	(Improvement)
[SIIITBX-69]	Landsat 8 using wrong scale factors and add offsets	(Bug)
[SIIITBX-71]	Wrong MERIS L1B RGB profile	(Bug)
[SIIITBX-72]	Error displaying RGB composite for Landsat 5 data	(Bug)
[SIIITBX-73]	There shall be preference tab for the toolbox	(Improvement)
[SIIITBX-74]	MODIS AQUA SST L3 products have no time information	(Bug)
[SIIITBX-75]	S-3 OLCI and SLSTR readers to use directories as input	(Improvement)
[SIIITBX-78]	No appropriate product reader for Proba-V	(Bug)
[SIIITBX-79]	Default RGB-Profiles missing for OLCI products	(Bug)
[SIIITBX-82]	Masks for Landsat 8 do not work properly	(Bug)

