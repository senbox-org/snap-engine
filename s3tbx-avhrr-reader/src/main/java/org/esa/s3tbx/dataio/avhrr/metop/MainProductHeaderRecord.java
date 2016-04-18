/*
 * AVISA software - $Id: MainProductHeaderRecord.java,v 1.1.1.1 2007/03/22 11:12:51 ralf Exp $
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


import org.esa.s3tbx.dataio.avhrr.AvhrrConstants;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Reads a Main Product Header Record (MPHR)and make the contained
 * metadata available as MetadataElements
 *
 * @author marcoz
 * @version $Revision: 1.1.1.1 $ $Date: 2007/03/22 11:12:51 $
 */
class MainProductHeaderRecord extends AsciiRecord {
    private static final int NUM_FIELDS = 72;
    private static final DateFormat generalTimeFormat = new SimpleDateFormat("yyyyMMddHHmmss'Z'");
    private static final DateFormat longGeneralTimeFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS'Z'");

    public MainProductHeaderRecord() {
        super(NUM_FIELDS);
    }

    @Override
    public MetadataElement getMetaData() {
        final MetadataElement element = new MetadataElement("MPH");
        element.addElement(getProductDetails());
        element.addElement(getAscendingNodeOrbitParameters());
        element.addElement(getLocationSummary());
        element.addElement(getLeapSecondInformation());
        element.addElement(getRecordCounts());
        element.addElement(getRecordBasedGenericQualityFlags());
        element.addElement(getTimeBasedGenericQualityFlags());
        element.addElement(getRegionalProductInformation());
        return element;
    }

    private MetadataElement getProductDetails() {
        MetadataAttribute attribute;
        final MetadataElement element = new MetadataElement("PRODUCT_DETAILS");

        attribute = createStringAttribute("PRODUCT_NAME", null);
        attribute.setDescription("Complete name of the product");
        element.addAttribute(attribute);

        attribute = createStringAttribute("PARENT_PRODUCT_NAME_1", null);
        attribute.setDescription("Name of the parent product");
        element.addAttribute(attribute);

        attribute = createStringAttribute("PARENT_PRODUCT_NAME_2", null);
        attribute.setDescription("Name of the parent product");
        element.addAttribute(attribute);

        attribute = createStringAttribute("PARENT_PRODUCT_NAME_3", null);
        attribute.setDescription("Name of the parent product");
        element.addAttribute(attribute);

        attribute = createStringAttribute("PARENT_PRODUCT_NAME_4", null);
        attribute.setDescription("Name of the parent product");
        element.addAttribute(attribute);

        element.addAttribute(createStringAttribute("INSTRUMENT_ID", null));
        element.addAttribute(createStringAttribute("INSTRUMENT_MODEL", null));
        element.addAttribute(createStringAttribute("PRODUCT_TYPE", null));
        element.addAttribute(createStringAttribute("PROCESSING_LEVEL", null));
        element.addAttribute(createStringAttribute("SPACECRAFT_ID", null));
        element.addAttribute(createDateAttribute("SENSING_START", generalTimeFormat));
        element.addAttribute(createDateAttribute("SENSING_END", generalTimeFormat));
        element.addAttribute(createDateAttribute("SENSING_START_THEORETICAL", generalTimeFormat));
        element.addAttribute(createDateAttribute("SENSING_END_THEORETICAL", generalTimeFormat));
        element.addAttribute(createStringAttribute("PROCESSING_CENTRE", null));
        element.addAttribute(createIntAttribute("PROCESSOR_MAJOR_VERSION", null));
        element.addAttribute(createIntAttribute("PROCESSOR_MINOR_VERSION", null));
        element.addAttribute(createIntAttribute("FORMAT_MAJOR_VERSION", null));
        element.addAttribute(createIntAttribute("FORMAT_MINOR_VERSION", null));
        element.addAttribute(createDateAttribute("PROCESSING_TIME_START", generalTimeFormat));
        element.addAttribute(createDateAttribute("PROCESSING_TIME_END", generalTimeFormat));
        element.addAttribute(createStringAttribute("PROCESSING_MODE", null));
        element.addAttribute(createStringAttribute("DISPOSITION_MODE", null));
        element.addAttribute(createStringAttribute("RECEIVING_GROUND_STATION", null));
        element.addAttribute(createDateAttribute("RECEIVE_TIME_START", generalTimeFormat));
        element.addAttribute(createDateAttribute("RECEIVE_TIME_END", generalTimeFormat));

        attribute = createIntAttribute("ORBIT_START", null);
        attribute.setDescription("Start Orbit Number, counted incrementally since launch");
        element.addAttribute(attribute);

        attribute = createIntAttribute("ORBIT_END", null);
        attribute.setDescription("Stop Orbit Number");
        element.addAttribute(attribute);

        attribute = createIntAttribute("ACTUAL_PRODUCT_SIZE", AvhrrConstants.UNIT_BYTES);
        attribute.setDescription("Size of the complete product");
        element.addAttribute(attribute);
        return element;
    }

    private MetadataElement getAscendingNodeOrbitParameters() {
        final MetadataElement element = new MetadataElement("ASCENDING_NODE_ORBIT_PARAMETERS");
        element.addAttribute(createDateAttribute("STATE_VECTOR_TIME", longGeneralTimeFormat));
        element.addAttribute(createStringAttribute("SEMI_MAJOR_AXIS", AvhrrConstants.UNIT_MM));
        element.addAttribute(createFloatAttribute("ECCENTRICITY", 1E-6f, null));
        element.addAttribute(createFloatAttribute("INCLINATION", 1E-3f, AvhrrConstants.UNIT_DEG));
        element.addAttribute(createFloatAttribute("PERIGEE_ARGUMENT", 1E-3f, AvhrrConstants.UNIT_DEG));
        element.addAttribute(createFloatAttribute("RIGHT_ASCENSION", 1E-3f, AvhrrConstants.UNIT_DEG));
        element.addAttribute(createFloatAttribute("MEAN_ANOMALY", 1E-3f, AvhrrConstants.UNIT_DEG));
        element.addAttribute(createFloatAttribute("X_POSITION", 1E-3f, AvhrrConstants.UNIT_M));
        element.addAttribute(createFloatAttribute("Y_POSITION", 1E-3f, AvhrrConstants.UNIT_M));
        element.addAttribute(createFloatAttribute("Z_POSITION", 1E-3f, AvhrrConstants.UNIT_M));
        element.addAttribute(createFloatAttribute("X_VELOCITY", 1E-3f, AvhrrConstants.UNIT_M_PER_S));
        element.addAttribute(createFloatAttribute("Y_VELOCITY", 1E-3f, AvhrrConstants.UNIT_M_PER_S));
        element.addAttribute(createFloatAttribute("Z_VELOCITY", 1E-3f, AvhrrConstants.UNIT_M_PER_S));
        element.addAttribute(createIntAttribute("EARTH_SUN_DISTANCE_RATIO", null));
        element.addAttribute(createIntAttribute("LOCATION_TOLERANCE_RADIAL", AvhrrConstants.UNIT_M));
        element.addAttribute(createIntAttribute("LOCATION_TOLERANCE_CROSSTRACK", AvhrrConstants.UNIT_M));
        element.addAttribute(createIntAttribute("LOCATION_TOLERANCE_ALONGTRACK", AvhrrConstants.UNIT_M));
        element.addAttribute(createFloatAttribute("YAW_ERROR", 1E-3f, AvhrrConstants.UNIT_DEG));
        element.addAttribute(createFloatAttribute("ROLL_ERROR", 1E-3f, AvhrrConstants.UNIT_DEG));
        element.addAttribute(createFloatAttribute("PITCH_ERROR", 1E-3f, AvhrrConstants.UNIT_DEG));
        return element;
    }

    private MetadataElement getLocationSummary() {
        final MetadataElement element = new MetadataElement("LOCATION_SUMMARY");
        element.addAttribute(createFloatAttribute("SUBSAT_LATITUDE_START", 1E-3f, AvhrrConstants.UNIT_DEG));
        element.addAttribute(createFloatAttribute("SUBSAT_LONGITUDE_START", 1E-3f, AvhrrConstants.UNIT_DEG));
        element.addAttribute(createFloatAttribute("SUBSAT_LATITUDE_END", 1E-3f, AvhrrConstants.UNIT_DEG));
        element.addAttribute(createFloatAttribute("SUBSAT_LONGITUDE_END", 1E-3f, AvhrrConstants.UNIT_DEG));
        return element;
    }

    private MetadataElement getLeapSecondInformation() {
        final MetadataElement element = new MetadataElement("LEAP_SECOND_INFORMATION");
        element.addAttribute(createIntAttribute("LEAP_SECOND", null));
        element.addAttribute(createDateAttribute("LEAP_SECOND_UTC", generalTimeFormat));
        return element;
    }

    private MetadataElement getRecordCounts() {
        final MetadataElement element = new MetadataElement("RECORD_COUNTS");
        element.addAttribute(createIntAttribute("TOTAL_RECORDS", null));
        element.addAttribute(createIntAttribute("TOTAL_MPHR", null));
        element.addAttribute(createIntAttribute("TOTAL_SPHR", null));
        element.addAttribute(createIntAttribute("TOTAL_IPR", null));
        element.addAttribute(createIntAttribute("TOTAL_GEADR", null));
        element.addAttribute(createIntAttribute("TOTAL_GIADR", null));
        element.addAttribute(createIntAttribute("TOTAL_VEADR", null));
        element.addAttribute(createIntAttribute("TOTAL_VIADR", null));
        element.addAttribute(createIntAttribute("TOTAL_MDR", null));
        return element;
    }

    private MetadataElement getRecordBasedGenericQualityFlags() {
        final MetadataElement element = new MetadataElement("RECORD_BASED_GENERIC_QUALITY_FLAGS");
        element.addAttribute(createIntAttribute("COUNT_DEGRADED_INST_MDR", null));
        element.addAttribute(createIntAttribute("COUNT_DEGRADED_PROC_MDR", null));
        element.addAttribute(createIntAttribute("COUNT_DEGRADED_INST_MDR_BLOCKS", null));
        element.addAttribute(createIntAttribute("COUNT_DEGRADED_PROC_MDR_BLOCKS", null));
        return element;
    }

    private MetadataElement getTimeBasedGenericQualityFlags() {
        final MetadataElement element = new MetadataElement("TIME_BASED_GENERIC_RECORD_FLAGS");
        element.addAttribute(createIntAttribute("DURATION_OF_PRODUCT", AvhrrConstants.UNIT_MS));
        element.addAttribute(createIntAttribute("MILLISECONDS_OF_DATA_PRESENT", AvhrrConstants.UNIT_MS));
        element.addAttribute(createIntAttribute("MILLISECONDS_OF_DATA_MISSING", AvhrrConstants.UNIT_MS));
        return element;
    }

    private MetadataElement getRegionalProductInformation() {
        final MetadataElement element = new MetadataElement("REGIONAL_PRODUCT_INFORMATION");
        element.addAttribute(createStringAttribute("SUBSETTED_PRODUCT", null));
        return element;
    }
}
