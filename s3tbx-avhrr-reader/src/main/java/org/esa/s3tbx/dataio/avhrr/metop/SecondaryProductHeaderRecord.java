/*
 * AVISA software - $Id: SecondaryProductHeaderRecord.java,v 1.1.1.1 2007/03/22 11:12:51 ralf Exp $
 *
 * Copyright (C) 2005 by EUMETSAT
 *
 * The Licensee acknowledges that the AVISA software is owned by the European
 * Organisation for the Exploitation of Meteorological Satellites
 * (EUMETSAT) and the Licensee shall not transfer, assign, sub-licence,
 * reproduce or copy the AVISA software to any third party or part with
 * possession of this software or any part thereof in any way whatsoever.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.
 * The AVISA software has been developed using the ESA BEAM software which is
 * distributed under the GNU General Public License (GPL).
 *
 */
package org.esa.s3tbx.dataio.avhrr.metop;


import org.esa.snap.core.datamodel.MetadataElement;

/**
 * Reads a Second Product Header Record (SPHR)and make the contained
 * metadata available as MetadataElements
 *
 * @author marcoz
 * @version $Revision: 1.1.1.1 $ $Date: 2007/03/22 11:12:51 $
 */
class SecondaryProductHeaderRecord extends AsciiRecord {
    private static final int NUM_FIELDS = 3;

    public SecondaryProductHeaderRecord() {
        super(NUM_FIELDS);
    }

    @Override
    public MetadataElement getMetaData() {
        final MetadataElement element = new MetadataElement("SPH");
        element.addAttribute(createIntAttribute("SRC_DATA_QUAL", null));
        element.addAttribute(createIntAttribute("EARTH_VIEWS_PER_SCANLINE", null));
        element.addAttribute(createIntAttribute("NAV_SAMPLE_RATE", null));
        return element;
    }
}
