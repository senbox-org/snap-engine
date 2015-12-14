/*
 * $Id: TemporalFileArray.java,v 1.1 2007/03/27 12:52:21 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.aerosol;

import java.io.File;
import java.util.*;

public class TemporalFileArray {

    private final TemporalFile[] _temporalFiles;

    private TemporalFileArray(TemporalFile[] temporalFiles) {
        _temporalFiles = temporalFiles;
    }

    public static TemporalFileArray scan(File dir, boolean recursive, TemporalFileFactory fileFactory) {
        final List temporalFileList = new ArrayList(16);
        scanImpl(dir, recursive, fileFactory, temporalFileList);
        return new TemporalFileArray((TemporalFile[]) temporalFileList.toArray(new TemporalFile[temporalFileList.size()]));
    }

    public static TemporalFileArray create(String[] paths, TemporalFileFactory fileFactory) {
        final ArrayList temporalFileList = new ArrayList(paths.length);
        for (int i = 0; i < paths.length; i++) {
            addImpl(new File(paths[i]), fileFactory, temporalFileList);
        }
        return new TemporalFileArray((TemporalFile[]) temporalFileList.toArray(new TemporalFile[temporalFileList.size()]));
    }

    public TemporalFile[] getTemporalFiles() {
        return _temporalFiles;
    }

    public TemporalFile[] getTemporalFilesSorted(final Date date, int sizeMax) {
        final TemporalFile[] sortedFiles = _temporalFiles.clone();
        Arrays.sort(sortedFiles, new Comparator() {
            public int compare(Object o1, Object o2) {
                return TemporalFile.compare(date, (TemporalFile) o1, (TemporalFile) o2);
            }
        });
        if (sizeMax <= 0 || sizeMax == sortedFiles.length) {
            return sortedFiles;
        }
        TemporalFile[] croppedFiles = new TemporalFile[Math.min(sizeMax, sortedFiles.length)];
        System.arraycopy(sortedFiles, 0, croppedFiles, 0, croppedFiles.length);
        return croppedFiles;
    }

    private static void scanImpl(File dir, boolean recursive, TemporalFileFactory fileFactory, final List temporalFileList) {
        final File[] files = dir.listFiles();
        for (final File file : files) {
            if (file.isFile()) {
                addImpl(file, fileFactory, temporalFileList);
            }
        }
        for (final File file : files) {
            if (file.isDirectory()) {
                if (recursive && !file.getName().equals("..")) {
                    scanImpl(file, true, fileFactory, temporalFileList);
                }
            }
        }
    }

    private static void addImpl(File file, TemporalFileFactory fileFactory, final List temporalFileList) {
        final TemporalFile temporalFile = fileFactory.createTemporalFile(file);
        if (temporalFile != null) {
            temporalFileList.add(temporalFile);
        }
    }
}
