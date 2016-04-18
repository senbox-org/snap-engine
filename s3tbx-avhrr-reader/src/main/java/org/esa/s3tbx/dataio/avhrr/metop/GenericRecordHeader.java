/*
 * AVISA software - $Id: GenericRecordHeader.java,v 1.1.1.1 2007/03/22 11:12:51 ralf Exp $
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



import org.esa.snap.core.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * Can read a Generic Record Header (GRH) and holds it's information
 * in public accessible fields.
 *
 * @author marcoz
 * @version $Revision: 1.1.1.1 $ $Date: 2007/03/22 11:12:51 $
 */
class GenericRecordHeader {

    public RecordClass recordClass;

    public InstrumentGroup instrumentGroup;

    public int recordSubclass;

    public int recordSubclassVersion;

    public long recordSize;

    public ProductData.UTC recordStartTime;

    public ProductData.UTC recordEndTime;

    public boolean readGenericRecordHeader(ImageInputStream imageInputStream) throws IOException {
        byte rc = imageInputStream.readByte();
        if (!RecordClass.isValid(rc)) {
            return false;
        }
        recordClass = RecordClass.values()[rc];
        
        byte ig = imageInputStream.readByte();
        if (!InstrumentGroup.isValid(ig)) {
            return false;
        }
        instrumentGroup = InstrumentGroup.values()[ig];
        
        recordSubclass = imageInputStream.readByte();
        recordSubclassVersion = imageInputStream.readByte();
        recordSize = imageInputStream.readUnsignedInt();
        
        int day = imageInputStream.readUnsignedShort();
        long millis = imageInputStream.readUnsignedInt();
        long seconds = millis / 1000;
        long micros = (millis - seconds * 1000) * 1000;
        recordStartTime = new ProductData.UTC(day, (int) seconds, (int) micros);
        
        day = imageInputStream.readUnsignedShort();
        millis = imageInputStream.readUnsignedInt();
        seconds = millis / 1000;
        micros = (millis - seconds * 1000) * 1000;
        recordEndTime = new ProductData.UTC(day, (int) seconds, (int) micros);
        
        return true;
    }

    public void printGRH() {
        System.out.println("GRH");
        System.out.println("recordClass: " + recordClass);
        System.out.println("instrumentGroup: " + instrumentGroup);
        System.out.println("recordSubclass: " + recordSubclass);
        System.out.println("recordSubclassVersion: " + recordSubclassVersion);
        System.out.println("recordSize: " + recordSize);
//        System.out.println("recordStartTime: " + recordStartTime);
//        System.out.println("recordEndTime: " + recordEndTime);
    }

    public enum RecordClass {
        RESERVED,
        MPHR,
        SPHR,
        IPR,
        GEADR,
        GIADR,
        VEADR,
        VIADR,
        MDR;
        
        public static boolean isValid(int index) {
            return (index >=0 && index <= 8);
        }
    }

    public enum InstrumentGroup {
        GENERIC,
        AMSU_A,
        ASCAT,
        ATOVS,
        AVHRR_3,
        GOME,
        GRAS,
        HIRS_4,
        IASI,
        MHS,
        SEM,
        ADCS,
        SBUV,
        DUMMY,
        ARCHIVE,
        IASI_L2;
        
        public static boolean isValid(int index) {
            return (index >=0 && index <= 15);
        }
    }

}
