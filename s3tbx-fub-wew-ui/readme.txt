File name:	readme.txt

Authors:	Thomas Schroeder and Michael Schaale

Affiliation:	Institute for Space Sciences
		Freie Universitaet Berlin
		Department of Earth Sciences
		Carl-Heinrich-Becker-Weg 6-10
		D-12165 Berlin, Germany

E-mails:	Thomas.Schroeder@wew.fu-berlin.de
		    Michael.Schaale@wew.fu-berlin.de

Document title:	Brief Documentation of the FUB/WeW WATER Processor


Introduction
------------

This file briefly describes the installation and use of the accompanying
software package. This Plugin makes use of MERIS Level-1b TOA radiances in the
bands 1-7, 9-10 and 12-14 to retrieve the following case II water properties and
atmospheric properties above case II waters :

	- chlorophyll-a concentration           (log scale, mg/m^3)
	- yellow substance absorption @ 443 nm  (log scale, 1/m)
	- total suspended matter concentration  (log scale, g/m^3)
	- aerosol optical depth @ 440 nm
	- aerosol optical depth @ 550 nm
	- aerosol optical depth @ 670 nm
	- aerosol optical depth @ 870 nm
	- water-leaving RS reflectance @ 412 nm (1/sr)
	- water-leaving RS reflectance @ 442 nm (1/sr)
	- water-leaving RS reflectance @ 490 nm (1/sr)
	- water-leaving RS reflectance @ 510 nm (1/sr)
	- water-leaving RS reflectance @ 560 nm (1/sr)
	- water-leaving RS reflectance @ 620 nm (1/sr)
	- water-leaving RS reflectance @ 665 nm (1/sr)
	- water-leaving RS reflectance @ 708 nm (1/sr)

The retrieval is based on four separate artificial neural networks which
were trained on the basis of the results of extensive radiative transfer
simulations with the MOMO code by taking varying atmospheric and oceanic
conditions into account. All networks were validated against in-situ
measurements.

During the Plugin processing the MERIS Level-1b data are masked prior to
the retrival by applying the following combination mask :

	GLINT_RISK | LAND_OCEAN | BRIGHT | COASTLINE | INVALID

The masked pixel's values are set to +5.0.

Non-masked pixels are then normalized for an atmosphere's ozone contents of
344 Dobson units by calculating transmission correction factors.

Notice that the AOT wavelengths 440, 670 and 870 nm correspond to the
AERONET data wavelengths for a convenient direct comparison with in situ data.

Each pixel is checked against the input and output values margin of the
trained networks. Additional flags are set in case of a neural network
failure for input and output separately.

The radiative transfer simulation code and the retrieval algorithm are
described in detail in the papers cited in the references section below.
We would like to stress the fact that this plugin is applicable over
case II only, thus it is likely to fail over the open ocean by producing
negative remote sensing (RS) water-leaving reflectances.


Warranties and copyright information
------------------------------------

The FUB/WeW WATER processor package described in this document is provided
'as is', with no warranty of merchantability or fitness to any particular
purpose. Although every effort has been made to ensure accuracy of computations
and conformity to the algorithms as published in the references below, the
authors assume no responsibility whatsoever for any direct, indirect or
consequential damage resulting from the use of this software. The FUB/WeW WATER
processor is distributed free of charge and cannot be sold or re-sold. It
can be copied and distributed further, provided all documentation is attached
and provided the original source of the software is explicitly and
prominently described.

Questions, concerns and problems should be referred to the authors of the
software package at the address indicated at the start of this file.

The copyright on this file and the associated software remains with the
Institute for Space Sciences (WeW), Freie Universitaet Berlin.


References
----------

Fischer J., and Grassl H., "Radiative transfer in an atmosphere-ocean system:
an azimuthally dependent matrixoperator approach",
Applied Optics, 23, 1032-1039, 1984.

Fell F., and Fischer J., "Numerical simulation of the light field in the
atmosphere-ocean system using the matrixoperator method", Journal of
Quantitative Spectroscopy & Radiative Transfer, 69, 351-388, 2001.

Schroeder Th., Schaale M., Fell F. and Fischer J., "Atmospheric correction
algorithm for MERIS data: A neural network approach", In: Proceedings of the
Ocean Optics XVI Conference, Santa Fe, New Mexico, USA, published on CD ROM, 2002.

Schroeder Th., Fischer J., Schaale M. and Fell F., "Artificial neural network
based atmospheric correction algorithm: Application to MERIS data", In: P
roceedings of the International Society for Optical Engineering (SPIE),
Vol. 4892, Hangzhou, China, 2002.

Schroeder Th., and Fischer J., "Atmospheric correction of MERIS imagery above
case-2 waters", In: Proceedings of the 2003 MERIS User Workshop, ESA ESRIN,
Frascati, Italy, 2003.

Schroeder, Th., "Fernerkundung von Wasserinhaltsstoffen in Kuestengeweassern
mit MERIS unter Anwendung expliziter und impliziter Atmosphaerenkorrektur-
verfahren", Ph.D. Dissertation, Freie Universitatet Berlin,
Berlin (Germany), 2005, http:/www.diss.fu-berlin.de/2005/78
