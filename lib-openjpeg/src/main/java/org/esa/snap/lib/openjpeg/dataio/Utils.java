/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2013-2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.lib.openjpeg.dataio;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.win32.W32APIOptions;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

public class Utils {

    public static String getStackTrace(Throwable tr) {
        StringWriter sw = new StringWriter();
        tr.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static String GetShortPathNameW(String path) {
        if (!org.apache.commons.lang.SystemUtils.IS_OS_WINDOWS) {
            return path;
        }

        if (!(new File(path).exists())) {
            return "";
        }

        int sizeBuffer = 2048;
        char[] shortt = new char[sizeBuffer];

        //Call CKernel32 interface to execute GetShortPathNameW method
        CKernel32.INSTANCE.GetShortPathNameW(path, shortt, sizeBuffer);

        return Native.toString(shortt);
    }


    public static String GetLongPathNameW(String path) {
        if (!org.apache.commons.lang.SystemUtils.IS_OS_WINDOWS) {
            return path;
        }

        if (!(new File(path).exists())) {
            return "";
        }

        int sizeBuffer = 2048;
        char[] longg = new char[sizeBuffer];

        //Call CKernel32 interface to execute GetLongPathNameW method
        CKernel32.INSTANCE.GetLongPathNameW(path, longg, sizeBuffer);

        return Native.toString(longg);
    }


    public static String GetIterativeShortPathNameW(String path) throws IOException{
        if (!org.apache.commons.lang.SystemUtils.IS_OS_WINDOWS) {
            return path;
        }

        if (!(new File(path).exists())) {
            return "";
        }

        String firstTry = GetShortPathNameW(path);
        if (firstTry.length() != 0) {
            return firstTry;
        }

        int lenght = 0;
        String workingPath = path;
        while (lenght == 0) {
            workingPath = new File(workingPath).getParent();
            lenght = GetShortPathNameW(workingPath).length();
        }

        String[] shortenedFragments = GetShortPathNameW(workingPath).split(Pattern.quote(File.separator));
        String[] fragments = path.split(Pattern.quote(File.separator));
        // if the path did not split, we didn't have the system separator but '/'
        if (fragments.length == 1) {
            fragments = path.split(Pattern.quote("/"));
        }

        System.arraycopy(shortenedFragments, 0, fragments, 0, shortenedFragments.length);

        String complete = String.join(File.separator, (CharSequence[]) fragments);
        String shortComplete = GetShortPathNameW(complete);

        if (shortComplete.length() != 0) {
            return shortComplete;
        }

        if(complete.equals(path)) {
            if(path.length() <= 260) {
                return path;
            } else {
                throw new IOException(String.format("The path %s is too long and it is not possible to get the windows short path. Please, try to copy the product to the root folder.", path));
            }
        }
        return GetIterativeShortPathNameW(complete);
    }

    private interface CKernel32 extends Library {

        CKernel32 INSTANCE = (CKernel32) Native.loadLibrary("kernel32", CKernel32.class, W32APIOptions.UNICODE_OPTIONS);

        int GetShortPathNameW(String LongName, char[] ShortName, int BufferCount); //Unicode version of GetShortPathNameW

        int GetLongPathNameW(String ShortName, char[] LongName, int BufferCount);
    }

    /**
     * Method for computing the difference between file1 lastModified time and file2  lastModified time.
     *
     * @param file1
     * @param file2
     * @return OL if it is not possible to compute, in other case time1-time2
     */
    public static long diffLastModifiedTimes(File file1, File file2) {

        try {
            long time1 = file1.lastModified();
            long time2 = file2.lastModified();

            if ((time1 != 0L) && (time2 != 0L)) {
                return (time1 - time2);
            } else {
                return 0L;
            }
        } catch (Exception e) {
            return 0L;
        }
    }
    
    public static String getMD5sum(String input) {

        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(input.getBytes());
            byte[] messageDigestMD5 = messageDigest.digest();
            StringBuffer stringBuffer = new StringBuffer();
            for (byte bytes : messageDigestMD5) {
                stringBuffer.append(String.format("%02x", bytes & 0xff));
            }
            return stringBuffer.toString();
        } catch (NoSuchAlgorithmException exception) {
            return null;
        }
    }

}
