/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.dem.dataio.srtm1_hgt;

import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.dataop.dem.EastingNorthingParser;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

/**
 * Holds information about a HGT file.
 *
 */
public class SRTM1HgtFileInfo {

    private String fileName;
    private float easting;
    private float northing;

    private SRTM1HgtFileInfo() {
    }

    public String getFileName() {
        return fileName;
    }

    public float getEasting() {
        return easting;
    }

    public float getNorthing() {
        return northing;
    }

    public static SRTM1HgtFileInfo create(final File dataFile) throws IOException {
        final SRTM1HgtFileInfo fileInfo = new SRTM1HgtFileInfo();
        fileInfo.setFromData(dataFile.getName());
        return fileInfo;
    }

    public void setFromData(final String fileName) throws IOException {
        this.fileName = fileName;

        final int[] eastingNorthing;
        try {
            eastingNorthing = parseEastingNorthing(fileName);
        } catch (ParseException e) {
            throw new IOException("Illegal file name format: " + fileName, e);
        }
        easting = eastingNorthing[0];
        northing = eastingNorthing[1];
    }

    static int[] parseEastingNorthing(final String fileName) throws ParseException {
        Guardian.assertNotNullOrEmpty("fileName", fileName);
        final SRTMEastingNorthingParser parser = new SRTMEastingNorthingParser(fileName, "\\..+");
        return parser.parse();
    }

    private static class SRTMEastingNorthingParser extends EastingNorthingParser {

        private SRTMEastingNorthingParser(final String text, final String allowedSuffix) {
            super(text, allowedSuffix);
        }

        @Override
        protected void parseDirectionValueAndAssign(final int[] eastingNorthing) throws ParseException {
            final int direction = getDirection();
            int value = readNumber();
            value = correctValueByDirection(value, direction);
            assignValueByDirection(eastingNorthing, value, direction);
        }
    }
}
