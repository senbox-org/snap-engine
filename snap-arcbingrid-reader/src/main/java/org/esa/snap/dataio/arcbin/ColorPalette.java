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
package org.esa.snap.dataio.arcbin;

import com.bc.ceres.core.runtime.internal.IOHelper;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.ColorPaletteDef.Point;
import org.esa.snap.core.datamodel.IndexCoding;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;


class ColorPalette {

    private ColorPalette() {
    }

    private static final class ColorPaletteFilenameFilter implements Predicate<Path> {

        @Override
        public boolean test(Path dir) {
            return dir.endsWith(".clr") || dir.endsWith(".CLR");
        }
    }

    static Path findColorPaletteFile(Path gridDir) {
        Predicate<Path> filter = new ColorPaletteFilenameFilter();
        Path result = findFileInDir(gridDir, filter);
        if (result == null) {
            result = findFileInDir(gridDir.getParent(), filter);
        }
        return result;
    }

    private static Path findFileInDir(Path dir, Predicate<Path> filter) {
        Collection<Path> fileList = IOHelper.listFiles(dir, filter);
        if (fileList != null && !fileList.isEmpty()) {
            return fileList.iterator().next();
        }
        return null;
    }

    static ColorPaletteDef createColorPalette(Path file, RasterStatistics statistics) {
        BufferedReader reader = null;
        List<ColorPaletteDef.Point> pointList = new ArrayList<ColorPaletteDef.Point>();
        try {
            reader = Files.newBufferedReader(file);
            String line = reader.readLine();
            while (line != null) {
                line = line.trim();
                if (!line.startsWith("#") && !line.isEmpty()) {
                    String[] lineElems = line.split("\\s+");
                    if (lineElems.length == 4) {
                        try {
                            int sample = Integer.valueOf(lineElems[0]);
                            if (statistics != null && sample >= statistics.min && sample <= statistics.max) {
                                int red = Integer.valueOf(lineElems[1]);
                                int green = Integer.valueOf(lineElems[2]);
                                int blue = Integer.valueOf(lineElems[3]);

                                final ColorPaletteDef.Point point = new ColorPaletteDef.Point();
                                point.setSample(sample);
                                point.setLabel("class_" + sample);
                                point.setColor(new Color(red, green, blue));
                                pointList.add(point);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                line = reader.readLine();
            }
        } catch (IOException ignored) {
            return null;
        } finally {
            try {
                reader.close();
            } catch (IOException ignore) {
            }
        }
        if (!pointList.isEmpty()) {
            int numPoints = pointList.size();
            Point[] points = pointList.toArray(new ColorPaletteDef.Point[numPoints]);
            return new ColorPaletteDef(points);
        }
        return null;
    }

    static IndexCoding createIndexCoding(ColorPaletteDef colorPaletteDef, Map<Integer, String> descriptionMap ) {
        final IndexCoding indexCoding = new IndexCoding("index_coding");
        final Point[] points = colorPaletteDef.getPoints();
        for (Point point : points) {
            final int sample = (int) point.getSample();
            final String name = "class_ " + sample;
            indexCoding.addIndex(name, sample, descriptionMap.get(sample) );
        }
        return indexCoding;
    }

}
