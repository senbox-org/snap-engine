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




import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.netcdf.util.DataTypeUtils;
import sun.reflect.generics.tree.ClassTypeSignature;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import ucar.nc2.*;



import java.awt.Rectangle;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

//**
// * A wrapper around the netCDF 4 {@link edu.ucar.ral.nujan.netcdf.NhVariable}.
// *
// * @author MarcoZ
// */


public class N4Variable implements NVariable {

    // MAX_ATTRIBUTE_LENGTH taken from
    // https://github.com/bcdev/nujan/blob/master/src/main/java/edu/ucar/ral/nujan/hdf/MsgAttribute.java#L185
    public static final int MAX_ATTRIBUTE_LENGTH = 65535 - 1000;


    private final Variable variable;
    private final java.awt.Dimension tileSize;
    private ChunkWriter writer;
    private final NetcdfFileWriter netcdfFileWriter;

    public N4Variable(Variable variable, java.awt.Dimension tileSize, NetcdfFileWriter netcdfFileWriter) {
        this.variable = variable;
        this.tileSize = tileSize;
        this.netcdfFileWriter = netcdfFileWriter;
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
    public void addAttribute(String name, String value) throws IOException {
        addAttributeImpl(name, cropStringToMaxAttributeLength(name, value),value.getClass().getName() ) ;
    }

    @Override
    public void addAttribute(String name, Number value) throws IOException {
        addAttribute(name, value, false);
    }

    @Override
    public void addAttribute(String name, Number value, boolean isUnsigned) throws IOException {
        addAttributeImpl(name, value, name.getClass().getName());
    }

    @Override
    public void addAttribute(String name, Array value) throws IOException {
        addAttributeImpl(name, value.getStorage(), value.getClass().getName());
    }

    private void addAttributeImpl(String name, Object value, String type ) throws IOException {
        name = name.replace('.', '_');
        try {
            if (variable.findAttribute(name) == null) {
                if (value.getClass() == Integer.class) {
                    Attribute attribute = new Attribute(name, (Integer) value);
                    variable.addAttribute(attribute);
                } else if (value.getClass() == String.class) {
                    Attribute attribute = new Attribute(name, (String) value);
                    variable.addAttribute(attribute);
                } else if (value.getClass() == Array.class) {
                    Attribute attribute = new Attribute(name, (Array) value);
                    variable.addAttribute(attribute);
                } else if (value.getClass() == Number.class) {
                    Attribute attribute = new Attribute(name, (Number) value, false);
                    variable.addAttribute(attribute);
                } else if (value.getClass() == Float.class) {
                    Attribute attribute = new Attribute(name, (Float) value);
                    variable.addAttribute(attribute);
                } else if (value.getClass() == List.class) {
                    Attribute attribute = new Attribute(name, (List) value);
                    variable.addAttribute(attribute);
                } else if (value.getClass() == DataType.class) {
                    Attribute attribute = new Attribute(name, (DataType) value, false);
                    variable.addAttribute(attribute);
                } else if (value.getClass() == Double.class) {
                    Attribute attribute = new Attribute(name, DataTypeUtils.convertTo((Double) value, DataType.DOUBLE), false);
                    variable.addAttribute(attribute);
                }
                else {
                    if (value.getClass().getName() == "[I") {
                        List<Integer> temp = Ints.asList((int[]) value);
                        Attribute attribute = new Attribute(name, temp);
                        variable.addAttribute(attribute);
                    }
                    else  if (value.getClass().getName() == "[B") {
                        List<Byte> temp = Bytes.asList((byte[]) value);
                        Attribute attribute = new Attribute(name, temp);
                        variable.addAttribute(attribute);
                    }
                }
            }
        }
        catch(Exception e){
            throw new IOException(e);
        }
    }


    @Override
    public Attribute findAttribute(String name) {
        return variable.findAttribute(name);
    }



    @Override

    public void writeFully(NetcdfFileWriter netwriter, Array values) throws IOException {
        int[] idxes = new int[values.getShape().length];
        try {
            netwriter.write(variable,idxes,values);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }



    @Override
    public void write(int x, int y, int width, int height, boolean isYFlipped, ProductData data) throws IOException {
        if (writer == null) {
            writer = createWriter(isYFlipped);
        }
        writer.write(x, y, width, height, data);
    }

    static String cropStringToMaxAttributeLength(String name,  String value) {
        if(value != null && value.length() > MAX_ATTRIBUTE_LENGTH) {
            value = value.substring(0, MAX_ATTRIBUTE_LENGTH);
            String msg = String.format("Metadata attribute '%s' has been cropped. Exceeded maximum length of %d", name, MAX_ATTRIBUTE_LENGTH);
            Logger.getLogger(N4Variable.class.getSimpleName()).log(Level.WARNING, msg);
        }
        return value;
    }

    private ChunkWriter createWriter(boolean isYFlipped) {
        List<ucar.nc2.Dimension> dimensions = variable.getDimensions();
        int sceneWidth = dimensions.get(1).getLength();
        int sceneHeight = dimensions.get(0).getLength();
        int chunkWidth = tileSize.width;
        int chunkHeight = tileSize.height;
        return new NetCDF4ChunkWriter(sceneWidth, sceneHeight, chunkWidth, chunkHeight, isYFlipped);
    }

    private class NetCDF4ChunkWriter extends ChunkWriter {
        private final Set<Rectangle> writtenChunkRects;
        public NetCDF4ChunkWriter(int sceneWidth, int sceneHeight, int chunkWidth, int chunkHeight, boolean YFlipped) {
            super(sceneWidth, sceneHeight, chunkWidth, chunkHeight, YFlipped);
            writtenChunkRects = new HashSet<Rectangle>((sceneWidth / chunkWidth) * (sceneHeight / chunkHeight));
        }
        @Override
        public void writeChunk( Rectangle rect, ProductData data) throws IOException {
            if (!writtenChunkRects.contains(rect)) {
                // netcdf4 chunks can only be written once
                final int[] origin = new int[]{rect.y, rect.x};
                final int[] shape = new int[]{rect.height, rect.width};
                DataType dataType = variable.getDataType();
                final Array values = Array.factory(dataType, shape, data.getElems());
                NetcdfFileWriter netwriter = netcdfFileWriter;
                try {
                    netwriter.write(variable,origin,values);
                } catch (Exception e) {
                    throw new IOException(e);
                }
                writtenChunkRects.add(rect);
            }
        }


    }
}
