/*
 * AVISA software - $Id: AsciiRecord.java,v 1.1.1.1 2007/03/22 11:12:51 ralf Exp $
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
import org.esa.s3tbx.dataio.avhrr.HeaderUtil;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The abstract base class for all header containing infomation in ASCII format
 *
 * @author marcoz
 * @version $Revision: 1.1.1.1 $ $Date: 2007/03/22 11:12:51 $
 */
abstract class AsciiRecord {

    private Map<String, String> map;
    private int fieldCount;

    public AsciiRecord(int fieldCount) {
        this.fieldCount = fieldCount;
        this.map = new HashMap<String, String>();
    }

    public void readRecord(ImageInputStream imageInputStream) throws IOException {
        for (int i = 0; i < fieldCount; i++) {
            final String fieldString = imageInputStream.readLine();
            final KeyValuePair field = new KeyValuePair(fieldString);

            map.put(field.key, field.value);
        }
    }

    public String getValue(String key) {
        return map.get(key);
    }

    public int getIntValue(String key) {
        return Integer.parseInt(getValue(key));
    }

    public long getLongValue(String key) {
        return Long.parseLong(getValue(key));
    }

    abstract public MetadataElement getMetaData();

    public void printValues() {
        List<String> keys = new ArrayList<String>(map.keySet());
        Collections.sort(keys);


        for (final String key : keys) {
            System.out.println(key + "=" + map.get(key));
        }
    }

    MetadataAttribute createStringAttribute(String key, String unit) {
        String stringValue = getValue(key);
        if (stringValue != null) {
            return HeaderUtil.createAttribute(key, stringValue, unit);
        } else {
            return null;
        }
    }

    MetadataAttribute createFloatAttribute(String key, float scalingFactor, String unit) {
        String stringValue = getValue(key);
        if (stringValue != null) {
            try {
                final long longValue = Long.parseLong(stringValue);
                return HeaderUtil.createAttribute(key, longValue * scalingFactor, unit);
            } catch (NumberFormatException e) {
                return HeaderUtil.createAttribute(key, stringValue, unit);
            }
        } else {
            return null;
        }
    }

    MetadataAttribute createIntAttribute(String key, String unit) {
        String stringValue = getValue(key);
        if (stringValue != null) {
            try {
                final int intValue = Integer.parseInt(stringValue);
                return HeaderUtil.createAttribute(key, intValue, unit);
            } catch (NumberFormatException e) {
                return HeaderUtil.createAttribute(key, stringValue, unit);
            }
        } else {
            return null;
        }
    }

    MetadataAttribute createDateAttribute(String key, DateFormat dateFormat) {
        final String dateString = getValue(key);
        MetadataAttribute attribute;
        try {
            final Date date = dateFormat.parse(dateString);
            ProductData.UTC utc = ProductData.UTC.create(date, 0);
            attribute = new MetadataAttribute(key, utc, true);
        } catch (ParseException e) {
            ProductData data = ProductData.createInstance(dateString);
            attribute = new MetadataAttribute(key, data, true);
        }
        attribute.setUnit(AvhrrConstants.UNIT_DATE);
        return attribute;
    }

    private class KeyValuePair {
        final String key;
        final String value;

        public KeyValuePair(String field) {
            key = field.substring(0, 30).trim();
            value = field.substring(32).trim();
        }
    }
}