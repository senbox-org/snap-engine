/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.dataio.envisat;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.Debug;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

public class HeaderParserTest {

    private boolean _oldDebugState;

    @Before
    public void setUp() {
        _oldDebugState = Debug.setEnabled(false);
    }

    @After
    public void tearDown() {
        Debug.setEnabled(_oldDebugState);
    }

    @Test
    public void testThatSingleEntriesAreParsedCorrectly() {
        try {
            String source = "TOT_SIZE=+00000000000186478394<bytes>\n";
            Header header = HeaderParser.getInstance().parseHeader("TEST", source.getBytes());
            assertNotNull(header);
            assertNotNull(header.getParam("TOT_SIZE"));
            assertNotNull(header.getParam("tot_size"));
            assertSame(header.getParam("TOT_SIZE"), header.getParam("tot_size"));
            assertEquals(1, header.getNumParams());

            Field param = header.getParam("TOT_SIZE");
            assertNotNull(param);
            assertNotNull(param.getInfo());
            assertEquals("TOT_SIZE", param.getInfo().getName());
            assertEquals(ProductData.TYPE_UINT32, param.getInfo().getDataType());
            assertEquals(1, param.getInfo().getNumDataElems());
            assertEquals("bytes", param.getInfo().getPhysicalUnit());
            assertEquals(1, param.getNumElems());
            assertEquals(186478394, param.getElemInt(0));
            Debug.trace("header: " + header);
        } catch (HeaderParseException e) {
            Debug.trace(e);
            fail("unexpected HeaderParseException: " + e.getMessage());
        }

        try {
            String source = "Z_VELOCITY=+7377.421000<m/s>\n";
            Header header = HeaderParser.getInstance().parseHeader("TEST", source.getBytes());
            assertNotNull(header);
            assertEquals(1, header.getNumParams());

            Field param = header.getParam("Z_VELOCITY");
            assertNotNull(param);
            assertNotNull(param.getInfo());
            assertEquals("Z_VELOCITY", param.getInfo().getName());
            assertEquals(ProductData.TYPE_FLOAT64, param.getInfo().getDataType());
            assertEquals(1, param.getInfo().getNumDataElems());
            assertEquals("m/s", param.getInfo().getPhysicalUnit());
            assertEquals(1, param.getNumElems());
            assertEquals(7377.421, param.getElemDouble(0), 1.e-6);
            Debug.trace("header: " + header);
        } catch (HeaderParseException e) {
            Debug.trace(e);
            fail("unexpected HeaderParseException: " + e.getMessage());
        }

        try {
            String source = "COLUMN_SPACING=+2.60000000e+02<m>\n";
            Header header = HeaderParser.getInstance().parseHeader("TEST", source.getBytes());
            assertNotNull(header);
            assertEquals(1, header.getNumParams());

            Field param = header.getParam("COLUMN_SPACING");
            assertNotNull(param);
            assertNotNull(param.getInfo());
            assertEquals("COLUMN_SPACING", param.getInfo().getName());
            assertEquals(ProductData.TYPE_FLOAT64, param.getInfo().getDataType());
            assertEquals(1, param.getInfo().getNumDataElems());
            assertEquals("m", param.getInfo().getPhysicalUnit());
            assertEquals(1, param.getNumElems());
            assertEquals(2.6e2, param.getElemDouble(0), 1.e-6);
            Debug.trace("header: " + header);
        } catch (HeaderParseException e) {
            Debug.trace(e);
            fail("unexpected HeaderParseException: " + e.getMessage());
        }

        try {
            String source = "DS_NAME=\"Cloud measurement parameters\"\n";
            Header header = HeaderParser.getInstance().parseHeader("TEST", source.getBytes());
            assertNotNull(header);
            assertEquals(1, header.getNumParams());

            Field param = header.getParam("DS_NAME");
            assertNotNull(param);
            assertNotNull(param.getInfo());
            assertEquals("DS_NAME", param.getInfo().getName());
            assertEquals(ProductData.TYPE_ASCII, param.getInfo().getDataType());
            assertEquals(28, param.getInfo().getNumDataElems());
            assertNull(param.getInfo().getPhysicalUnit());
            assertEquals(28, param.getNumElems());
            assertEquals("Cloud measurement parameters", param.getAsString());
            Debug.trace("header: " + header);
        } catch (HeaderParseException e) {
            Debug.trace(e);
            fail("unexpected HeaderParseException: " + e.getMessage());
        }

        try {
            String source = "BAND_WAVELEN=+0000412500+0000442500+0000490000+0000510000+0000560000+0000620000+0000665000+0000681250+0000705000+0000753750+0000760625+0000775000+0000865000+0000885000+0000900000<10-3nm>";
            Header header = HeaderParser.getInstance().parseHeader("TEST", source.getBytes());
            assertNotNull(header);
            assertEquals(1, header.getNumParams());

            Field param = header.getParam("BAND_WAVELEN");
            assertEquals(ProductData.TYPE_INT32, param.getInfo().getDataType());
            assertEquals(15, param.getInfo().getNumDataElems());
            assertEquals("10-3nm", param.getInfo().getPhysicalUnit());
            assertEquals(15, param.getNumElems());
            assertEquals(412500, param.getElemInt(0));
            assertEquals(442500, param.getElemInt(1));
            assertEquals(490000, param.getElemInt(2));
            assertEquals(510000, param.getElemInt(3));
            assertEquals(560000, param.getElemInt(4));
            assertEquals(620000, param.getElemInt(5));
            assertEquals(665000, param.getElemInt(6));
            assertEquals(681250, param.getElemInt(7));
            assertEquals(705000, param.getElemInt(8));
            assertEquals(753750, param.getElemInt(9));
            assertEquals(760625, param.getElemInt(10));
            assertEquals(775000, param.getElemInt(11));
            assertEquals(865000, param.getElemInt(12));
            assertEquals(885000, param.getElemInt(13));
            assertEquals(900000, param.getElemInt(14));
            Debug.trace("header: " + header);
        } catch (HeaderParseException e) {
            Debug.trace(e);
            fail("unexpected HeaderParseException: " + e.getMessage());
        }

        try {
            String source = "DS_TYPE=A\nNUM_DSR=+0000036       ";
            Header header = HeaderParser.getInstance().parseHeader("TEST", source.getBytes());
            assertNotNull(header);
            assertEquals(2, header.getNumParams());


            Field param1 = header.getParam("DS_TYPE");
            assertNotNull(param1);
            assertNotNull(param1.getInfo());
            assertEquals("DS_TYPE", param1.getInfo().getName());
            assertEquals(ProductData.TYPE_ASCII, param1.getInfo().getDataType());
            assertNull(param1.getInfo().getPhysicalUnit());
            assertEquals(1, param1.getInfo().getNumDataElems());
            assertEquals("A", param1.getAsString());

            Field param2 = header.getParam("NUM_DSR");
            assertNotNull(header.getParamAt(1));
            assertNotNull(param2.getInfo());
            assertEquals("NUM_DSR", param2.getInfo().getName());
            assertEquals(ProductData.TYPE_INT32, param2.getInfo().getDataType());
            assertEquals(1, param2.getInfo().getNumDataElems());
            assertNull(param2.getInfo().getPhysicalUnit());
            assertEquals(1, param2.getNumElems());
            assertEquals(36, param2.getElemInt(0));

            Debug.trace("header: " + header);
        } catch (HeaderParseException e) {
            Debug.trace(e);
            fail("unexpected HeaderParseException: " + e.getMessage());
        }
    }

    @Test
    public void testThatInvalidEntriesAreRejected() {
        try {
            String source = "=\"Holla Senor\"";
            HeaderParser.getInstance().parseHeader("TEST", source.getBytes());
            fail("expected HeaderParseException since source is invalid");
        } catch (HeaderParseException e) {
        }

        try {
            String source = "BIBO WAS HERE";
            HeaderParser.getInstance().parseHeader("TEST", source.getBytes());
            fail("expected HeaderParseException since source is invalid");
        } catch (HeaderParseException e) {
        }

        try {
            String source = new String(new byte[]{8, 27, 94, 10, 9, 4, 23, 56, 63, 3, 0, 0, 8, 7, 32, 32, 67});
            HeaderParser.getInstance().parseHeader("TEST", source.getBytes());
            fail("expected HeaderParseException since source is invalid");
        } catch (HeaderParseException expected) {
            // Ok
        }
    }

    @Test
    public void testThatInvalidNumbersAreHandledAsStringValues() throws HeaderParseException,
            HeaderEntryNotFoundException {
        Header header;
        header = HeaderParser.getInstance().parseHeader("TEST", "X=+000+".getBytes());
        assertEquals(ProductData.TYPE_ASCII, header.getParamDataType("X"));

        header = HeaderParser.getInstance().parseHeader("TEST", "X=+000+0a".getBytes());
        assertEquals(ProductData.TYPE_ASCII, header.getParamDataType("X"));

        header = HeaderParser.getInstance().parseHeader("TEST", "X=+ 001".getBytes());
    }

    @Test
    public void testThatNumericDataTypesAreHandledCorrectly() {
        try {
            String source =
                    "AN_UINT32=+4294967295<bytes>\n" +
                            "AN_INT32=-2147483648<bytes>\n" +
                            "AN_UINT16=+65535<bytes>\n" +
                            "AN_INT16=-32768<bytes>\n" +
                            "AN_UINT8=+255<bytes>\n" +
                            "AN_INT8=-128<bytes>\n" +
                            "TRUE=1\n" +
                            "FALSE=0\n";

            Header header = HeaderParser.getInstance().parseHeader("TEST", source.getBytes());
            assertNotNull(header);
            assertEquals(8, header.getNumParams());

            Field param;

            param = header.getParam("AN_UINT32");
            assertNotNull(param);
            assertEquals(ProductData.TYPE_UINT32, param.getDataType());
            assertEquals(4294967295L, param.getElemLong(0));

            param = header.getParam("AN_INT32");
            assertNotNull(param);
            assertEquals(ProductData.TYPE_INT32, param.getDataType());
            assertEquals(-2147483648, param.getElemInt(0));
            assertEquals(-2147483648L, param.getElemLong(0));

            param = header.getParam("AN_UINT16");
            assertNotNull(param);
            assertEquals(ProductData.TYPE_INT32, param.getDataType());
            assertEquals(65535L, param.getElemLong(0));
            assertEquals(65535, param.getElemInt(0));

            param = header.getParam("AN_INT16");
            assertNotNull(param);
            assertEquals(ProductData.TYPE_INT32, param.getDataType());
            assertEquals(-32768L, param.getElemLong(0));
            assertEquals(-32768, param.getElemInt(0));

            param = header.getParam("AN_UINT8");
            assertNotNull(param);
            assertEquals(ProductData.TYPE_INT32, param.getDataType());
            assertEquals(255L, param.getElemLong(0));
            assertEquals(255, param.getElemInt(0));

            param = header.getParam("AN_INT8");
            assertNotNull(param);
            assertEquals(ProductData.TYPE_INT32, param.getDataType());
            assertEquals(-128L, param.getElemLong(0));
            assertEquals(-128, param.getElemInt(0));

            param = header.getParam("TRUE");
            assertNotNull(param);
            assertEquals(ProductData.TYPE_INT8, param.getDataType());
            assertEquals(1L, param.getElemLong(0));
            assertEquals(1, param.getElemInt(0));

            param = header.getParam("FALSE");
            assertNotNull(param);
            assertEquals(ProductData.TYPE_INT8, param.getDataType());
            assertEquals(0L, param.getElemLong(0));
            assertEquals(0, param.getElemInt(0));

        } catch (HeaderParseException e) {
            fail("unexpected HeaderParseException: " + e.getMessage());
        }
    }

    @Test
    public void testARealLifeMPHExample() throws HeaderParseException {
        String sb = "PRODUCT=\"MER_FR__2PTACR20000620_104323_00000099X000_00000_00000_0000.N1\"\n" +
                "PROC_STAGE=T\n" +
                "REF_DOC=\"PO-RS-MDA-GS-2009_3/B  \"\n" +
                "\n" +
                "ACQUISITION_STATION=\"ENVISAT SampleData#3\"\n" +
                "PROC_CENTER=\"F-ACRI\"\n" +
                "PROC_TIME=\"22-FEB-2000 19:41:46.000000\"\n" +
                "SOFTWARE_VER=\"MEGS/4.3      \"\n" +
                "\n" +
                "SENSING_START=\"20-JUN-2000 10:43:23.851360\"\n" +
                "SENSING_STOP=\"20-JUN-2000 10:45:02.411360\"\n" +
                "\n" +
                "PHASE=X\n" +
                "CYCLE=+000\n" +
                "REL_ORBIT=+00000\n" +
                "ABS_ORBIT=+00000\n" +
                "STATE_VECTOR_TIME=\"20-JUN-2000 10:06:52.269120\"\n" +
                "DELTA_UT1=+.000000<s>\n" +
                "X_POSITION=-7162215.231<m>\n" +
                "Y_POSITION=+0208912.061<m>\n" +
                "Z_POSITION=-0000004.200<m>\n" +
                "X_VELOCITY=+0056.067000<m/s>\n" +
                "Y_VELOCITY=+1629.960000<m/s>\n" +
                "Z_VELOCITY=+7377.421000<m/s>\n" +
                "VECTOR_SOURCE=\"00\"\n" +
                "\n" +
                "UTC_SBT_TIME=\"20-JUN-2000 06:29:50.343648\"\n" +
                "SAT_BINARY_TIME=+0000000000\n" +
                "CLOCK_STEP=+3906250000<ps>\n" +
                "\n" +
                "LEAP_UTC=\"                           \"\n" +
                "LEAP_SIGN=+000\n" +
                "LEAP_ERR=0\n" +
                "\n" +
                "PRODUCT_ERR=0\n" +
                "TOT_SIZE=+00000000000186478394<bytes>\n" +
                "SPH_SIZE=+0000011622<bytes>\n" +
                "NUM_DSD=+0000000036\n" +
                "DSD_SIZE=+0000000280<bytes>\n" +
                "NUM_DATA_SETS=+0000000023\n" +
                "\n";

        HeaderParser.getInstance().parseHeader("MPH", sb.getBytes());
    }

    @Test
    public void testARealLifeSPHExample() throws HeaderParseException {
        String sb = "SPH_DESCRIPTOR=\"Level 2 Full Resolution     \"\n" +
                "STRIPLINE_CONTINUITY_INDICATOR=+000\n" +
                "SLICE_POSITION=+001\n" +
                "NUM_SLICES=+001\n" +
                "FIRST_LINE_TIME=\"20-JUN-2000 10:43:23.827346\"\n" +
                "LAST_LINE_TIME=\"20-JUN-2000 10:45:02.387346\"\n" +
                "FIRST_FIRST_LAT=+0048477538<10-6degN>\n" +
                "FIRST_FIRST_LONG=-0000848029<10-6degE>\n" +
                "FIRST_MID_LAT=+0049120852<10-6degN>\n" +
                "FIRST_MID_LONG=-0004690841<10-6degE>\n" +
                "FIRST_LAST_LAT=+0049633188<10-6degN>\n" +
                "FIRST_LAST_LONG=-0008623846<10-6degE>\n" +
                "LAST_FIRST_LAT=+0042727139<10-6degN>\n" +
                "AST_FIRST_LONG=-0003066400<10-6degE>\n" +
                "LAST_MID_LAT=+0043337479<10-6degN>\n" +
                "LAST_MID_LONG=-0006541594<10-6degE>\n" +
                "LAST_LAST_LAT=+0043840261<10-6degN>\n" +
                "LAST_LAST_LONG=-0010080759<10-6degE>\n" +
                "\n" +
                "TRANS_ERR_FLAG=0\n" +
                "FORMAT_ERR_FLAG=0\n" +
                "DATABASE_FLAG=0\n" +
                "COARSE_ERR_FLAG=0\n" +
                "ECMWF_TYPE=1\n" +
                "NUM_TRANS_ERR=+0000000000\n" +
                "NUM_FORMAT_ERR=+0000000000\n" +
                "TRANS_ERR_THRESH=+0.00000000e+00<%>\n" +
                "FORMAT_ERR_THRESH=+0.00000000e+00<%>\n" +
                "\n" +
                "NUM_BANDS=+015\n" +
                "BAND_WAVELEN=+0000412500+0000442500+0000490000+0000510000+0000560000+0000620000+0000665000+0000681250+0000705000+0000753750+0000760625+0000775000+0000865000+0000885000+0000900000<10-3nm>\n" +
                "BANDWIDTH=+10000+10000+10000+10000+10000+10000+10000+07500+10000+07500+03750+15000+20000+10000+10000<10-3nm>\n" +
                "INST_FOV=+0000019151<10-6deg>\n" +
                "PROC_MODE=0\n" +
                "OFFSET_COMP=1\n" +
                "LINE_TIME_INTERVAL=+0000044000<10-6s>\n" +
                "LINE_LENGTH=+02241<samples>\n" +
                "LINES_PER_TIE_PT=+064\n" +
                "SAMPLES_PER_TIE_PT=+064\n" +
                "COLUMN_SPACING=+2.60000000e+02<m>\n" +
                "\n";

        HeaderParser.getInstance().parseHeader("SPH", sb.getBytes());
    }

    @Test
    public void testARealLifeDSDExample() throws HeaderParseException {
        String sb = "DS_NAME=\"Quality ADS                 \"\n" +
                "DS_TYPE=A\n" +
                "FILENAME=\"                                                              \"\n" +
                "DS_OFFSET=+00000000000000012869<bytes>\n" +
                "DS_SIZE=+00000000000000000160<bytes>\n" +
                "NUM_DSR=+0000000005\n" +
                "DSR_SIZE=+0000000032<bytes>\n" +
                "\n";

        HeaderParser.getInstance().parseHeader("DSD(1)", sb.getBytes());
    }

    @Test
    public final void testGetAsDate() throws HeaderParseException,
            HeaderEntryNotFoundException {
        Header header = HeaderParser.getInstance().parseHeader("MPH", "SENSING_START=\"20-JAN-2000 10:43:23.851360\"\n".getBytes());
        final Date paramDate = header.getParamDate("SENSING_START");
        final long mjd2kOffset = 946684800000L;
        assertEquals(mjd2kOffset + ((((20 - 1) * 24 + 10) * 60 + 43) * 60 + 23) * 1000 + 851, paramDate.getTime());
    }
}
