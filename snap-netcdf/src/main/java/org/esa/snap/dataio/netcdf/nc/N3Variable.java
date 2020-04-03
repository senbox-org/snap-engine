/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.netcdf.nc;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.netcdf.PartialDataCopier;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.*;

/**
 * A wrapper around the netCDF 3 {@link ucar.nc2.Variable}.
 *
 * @author MarcoZ
 */
public class N3Variable implements NVariable {

    private final Variable variable;
    private final NetcdfFileWriter netcdfFileWriteable;
    private final HashMap<Integer, TileRowCache> tileRowCacheMap;
    private static final int Y_INDEX = 0;
    private static final int X_INDEX = 1;

    public N3Variable(Variable variable, NetcdfFileWriter netcdfFileWriteable) {
        this.variable = variable;
        this.netcdfFileWriteable = netcdfFileWriteable;
        tileRowCacheMap = new HashMap<>();
    }

    @Override
    public String getName() {
        return variable.getFullName();
    }

    @Override
    public DataType getDataType() {
        return variable.getDataType();
    }

    @Override
    public void setDataType(DataType dataType) {
        variable.setDataType(dataType);
    }

    @Override
    public Attribute addAttribute(String name, String value) {
        return variable.addAttribute(new Attribute(name, value));
    }

    @Override
    public Attribute addAttribute(String name, Number value) {
        return addAttribute(name, value, false);
    }

    @Override
    public Attribute addAttribute(String name, Number value, boolean isUnsigned) {
        if (value instanceof Long) {
            return variable.addAttribute(new Attribute(name, value.intValue()));
        } else {
            return variable.addAttribute(new Attribute(name, value));
        }
    }

    @Override
    public Attribute addAttribute(String name, Array value) {
        if (DataType.getType(value.getElementType(), false) == DataType.LONG) {
            long[] longElems = (long[]) value.get1DJavaArray(DataType.LONG);
            int[] intElems = new int[longElems.length];
            for (int i = 0; i < longElems.length; i++) {
                intElems[i] = (int) longElems[i];
            }
            return variable.addAttribute(new Attribute(name, Array.factory(DataType.INT, new int[]{longElems.length}, intElems)));
        } else {
            return variable.addAttribute(new Attribute(name, value));
        }
    }

    @Override
    public void writeFully(Array values) throws IOException {
        try {
            netcdfFileWriteable.write(variable, values);
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Attribute findAttribute(String name) {
        return variable.findAttribute(name);
    }

    @Override
    public void write(int x, int y, int width, int height, boolean isYFlipped, ProductData data) throws IOException {
        final TileRowCache tileRowCache;
        if (!tileRowCacheMap.containsKey(y)) {
            tileRowCache = new TileRowCache(height, variable);
            tileRowCacheMap.put(y, tileRowCache);
        } else {
            tileRowCache = tileRowCacheMap.get(y);
        }
        final DataType dataType = variable.getDataType();
        final int[] sourceShape = new int[]{height, width};
        Array sourceArray = Array.factory(dataType, sourceShape, data.getElems());
        if (isYFlipped) {
            sourceArray = sourceArray.flip(Y_INDEX);
        }
        try {
            tileRowCache.addTile(x, sourceArray);
            if (tileRowCache.isFilled()) {
                Array tileRow = tileRowCache.getTileRow();
                tileRowCacheMap.remove(y);
                String variableName = variable.getFullName();
                final int[] writeOrigin = new int[2];
                final int sceneHeight = variable.getDimension(Y_INDEX).getLength();
                writeOrigin[Y_INDEX] = isYFlipped ? (sceneHeight - 1) - y : y;
                synchronized (netcdfFileWriteable) {
                    netcdfFileWriteable.write(variableName, writeOrigin, tileRow);
                }
            }
        } catch (InvalidRangeException e) {
            e.printStackTrace();
            throw new IOException("Unable to encode netCDF data.", e);
        }
    }

    private static class TileRowCache {

        private final int height;
        private final Array row;
        private final Map<Integer, Integer> fillingAreaMap;
        private final int maxX;

        public TileRowCache(int height, Variable variable) {
            this.height = height;
            fillingAreaMap = Collections.synchronizedMap(new TreeMap<>());
            maxX = variable.getShape(X_INDEX);
            row = Array.factory(variable.getDataType(), new int[]{height, maxX});
        }

        public void addTile(int x, Array sourceArray) throws InvalidRangeException {
            int[] shape = sourceArray.getShape();
            if (shape[Y_INDEX] != height) {
                throw new IllegalArgumentException("The array hight is " + shape[Y_INDEX] + " but expected " + height);
            }
            PartialDataCopier.copy(new int[]{0, x * -1}, sourceArray, row);
            fillingAreaMap.put(x, x + shape[X_INDEX]);
        }

        public boolean isFilled() {
            combineFillingAreas();
            return fillingAreaMap.size() == 1 && fillingAreaMap.containsKey(0) && fillingAreaMap.get(0) == maxX;
        }

        private synchronized void combineFillingAreas() {
            Integer[] keys = fillingAreaMap.keySet().toArray(new Integer[0]);
            int currentKey = keys[0];
            int currentEnd = fillingAreaMap.get(currentKey);
            for (int i = 1; i < keys.length; i++) {
                Integer key2 = keys[i];
                if (key2 == currentEnd) {
                    currentEnd = fillingAreaMap.remove(key2);
                    fillingAreaMap.put(currentKey, currentEnd);
                } else {
                    currentKey = key2;
                    currentEnd = fillingAreaMap.get(key2);
                }
            }
        }

        public Array getTileRow() {
            return row;
        }
    }
}
