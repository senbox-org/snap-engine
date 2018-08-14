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

package org.esa.snap.dataio.netcdf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductWriter;
import org.esa.snap.core.dataio.ProductIOException;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.netcdf.AbstractNetCdfWriterPlugIn;
import org.esa.snap.dataio.netcdf.NetCdfWriteProfile;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.ProfileWriteContextImpl;
import org.esa.snap.dataio.netcdf.metadata.profiles.beam.BeamIndexCodingPart;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfIndexCodingPart;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import org.esa.snap.dataio.netcdf.util.Constants;
import org.esa.snap.dataio.netcdf.util.ReaderUtils;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Fire NetCDF writer configured by an implementation of {@link AbstractNetCdfWriterPlugIn}.
 */
public class FireNetCdfWriter extends AbstractProductWriter {

    private HashMap<String, NVariable> variableMap;
    private NFileWriteable writeable;
    private boolean isYFlipped;
    private boolean convertLogScaledBands;

    public FireNetCdfWriter(AbstractNetCdfWriterPlugIn writerPlugIn) {
        super(writerPlugIn);
        variableMap = new HashMap<>();
    }

    @Override
    public AbstractNetCdfWriterPlugIn getWriterPlugIn() {
        return (AbstractNetCdfWriterPlugIn) super.getWriterPlugIn();
    }

    @Override
    protected void writeProductNodesImpl() throws IOException {
        AbstractNetCdfWriterPlugIn plugIn = getWriterPlugIn();

        writeable = plugIn.createWritable(getOutputString());

        NetCdfWriteProfile profile = new NetCdfWriteProfile();
        configureProfile(profile, plugIn);
        ProfileWriteContext context = new ProfileWriteContextImpl(writeable);
        profile.writeProduct(context, getSourceProduct());
        Object yFlippedProperty = context.getProperty(Constants.Y_FLIPPED_PROPERTY_NAME);
        if (yFlippedProperty instanceof Boolean) {
            isYFlipped = (Boolean) yFlippedProperty;
        }
        Object convertLogScaledBandsProperty = context.getProperty(Constants.CONVERT_LOGSCALED_BANDS_PROPERTY);
        if (convertLogScaledBandsProperty instanceof Boolean) {
            convertLogScaledBands = (Boolean) convertLogScaledBandsProperty;
        }

    }

    public void configureProfile(NetCdfWriteProfile profile, AbstractNetCdfWriterPlugIn plugIn) throws IOException {
        profile.setInitialisationPartWriter(plugIn.createInitialisationPartWriter());
        //profile.addProfilePartWriter(plugIn.createMetadataPartWriter());
        profile.addProfilePartWriter(plugIn.createBandPartWriter());
        profile.addProfilePartWriter(plugIn.createTiePointGridPartWriter());
        profile.addProfilePartWriter(plugIn.createFlagCodingPartWriter());
        //profile.addProfilePartWriter(plugIn.createGeoCodingPartWriter());
        profile.addProfilePartWriter(plugIn.createImageInfoPartWriter());
        profile.addProfilePartWriter(new CfIndexCodingPart());
        profile.addProfilePartWriter(plugIn.createMaskPartWriter());
        profile.addProfilePartWriter(plugIn.createStxPartWriter());
        profile.addProfilePartWriter(plugIn.createTimePartWriter());
        profile.addProfilePartWriter(plugIn.createDescriptionPartWriter());
    }

    @Override
    public void writeBandRasterData(Band sourceBand, int sourceOffsetX, int sourceOffsetY, int sourceWidth,
                                    int sourceHeight, ProductData sourceBuffer, ProgressMonitor pm) throws IOException {
        final String variableName = ReaderUtils.getVariableName(sourceBand);
        //System.out.println(String.format("writing band... %s %d %d %d %d", variableName,sourceOffsetX,sourceOffsetY,sourceWidth,sourceHeight));
        if (shallWriteVariable(variableName)) {
            ProductData scaledBuffer;
            if (convertLogScaledBands && sourceBand.isLog10Scaled()) {
                scaledBuffer = ProductData.createInstance(ProductData.TYPE_FLOAT32, sourceBuffer.getNumElems());
                for (int i = 0; i < sourceBuffer.getNumElems(); i++) {
                    double elemDoubleAt = sourceBuffer.getElemDoubleAt(i);
                    scaledBuffer.setElemDoubleAt(i, sourceBand.scale(elemDoubleAt));
                }
            } else {
                scaledBuffer = sourceBuffer;
            }
            synchronized (writeable) {
                Object elems = scaledBuffer.getElems();
                Variable variable = writeable.getWriter().findVariable(variableName);

                /// case of shifting the whole raster by half
                //if (! sourceBand.getProduct().getFileLocation().getName().contains("LCCS"))
                //{
                    int totalWidthHalf =  sourceBand.getRasterWidth()/2;
                    int totalWidth = sourceBand.getRasterWidth();

                    if (sourceOffsetX>totalWidthHalf)
                    { final int[] shape =  new int[]{1,sourceHeight ,sourceWidth};
                        final int[] origin =  new int[]{0,sourceOffsetY ,sourceOffsetX-totalWidthHalf};
                        Array array = Array.factory(variable.getDataType(),shape, elems);
                        try { writeable.getWriter().write(variable,origin,array) ;
                        }
                        catch (InvalidRangeException e) {}
                    }
                    else if (sourceOffsetX<totalWidthHalf && (sourceOffsetX+sourceWidth+totalWidthHalf)<(totalWidth)) {
                        sourceOffsetX=sourceOffsetX+totalWidthHalf;
                        final int[] shape =  new int[]{1,sourceHeight ,sourceWidth};
                        final int[] origin =  new int[]{0,sourceOffsetY ,sourceOffsetX};
                        Array array = Array.factory(variable.getDataType(),shape, elems);
                        try { writeable.getWriter().write(variable,origin,array) ;
                        }
                        catch (InvalidRangeException e) {}

                    }
                    else {
                        final int[] shape = new int[]{1,sourceHeight, sourceWidth};
                        int[] originX1 = new int[]{0,sourceOffsetY, sourceOffsetX + totalWidthHalf};
                        int[] originX2 = new int[]{0,sourceOffsetY, 0};
                        int[] shape1 = new int[]{1,sourceHeight, totalWidthHalf - sourceOffsetX};
                        int[] shape2 = new int[]{1,sourceHeight, sourceWidth - totalWidthHalf + sourceOffsetX};
                        Array array = Array.factory(variable.getDataType(), shape, elems);
                        int[] originCut1 = new int[]{0, 0, 0};
                        int[] originCut2 = new int[]{0,0, totalWidth - sourceOffsetX - totalWidthHalf};
                        try {
                            Array piece1 = array.sectionNoReduce(originCut1, shape1, new int[]{1, 1, 1});
                            Array piece2 = array.sectionNoReduce(originCut2, shape2, new int[]{1, 1, 1});
                            writeable.getWriter().write(variable, originX1, piece1);
                            writeable.getWriter().write(variable, originX2, piece2);
                        } catch (InvalidRangeException e) {
                        }
                    }
                //}
                ///
                //else {
                //    final int[] shape =  new int[]{1,sourceHeight ,sourceWidth};  //changed
                //    final int[] origin =  new int[]{0,sourceOffsetY ,sourceOffsetX}; //changed
                //    Array array = Array.factory(variable.getDataType(),shape, elems);
                //    try { writeable.getWriter().write(variable,origin,array) ;
                //    }
                //    catch (InvalidRangeException e) {}
                    ///
                //}
            }
        }
        else if(variableName.contains("burned_area_in_vegetation_class")) {
            int vegetationClass = Integer.parseInt(variableName.replace("burned_area_in_vegetation_class_vegetation_class",""));
            ProductData scaledBuffer;
            if (convertLogScaledBands && sourceBand.isLog10Scaled()) {
                scaledBuffer = ProductData.createInstance(ProductData.TYPE_FLOAT32, sourceBuffer.getNumElems());
                for (int i = 0; i < sourceBuffer.getNumElems(); i++) {
                    double elemDoubleAt = sourceBuffer.getElemDoubleAt(i);
                    scaledBuffer.setElemDoubleAt(i, sourceBand.scale(elemDoubleAt));
                }
            } else {
                scaledBuffer = sourceBuffer;
            }

            synchronized (writeable) {
                Object elems = scaledBuffer.getElems();
                Variable variable = writeable.getWriter().findVariable("burned_area_in_vegetation_class");

                    int totalWidthHalf = sourceBand.getRasterWidth() / 2;
                    int totalWidth = sourceBand.getRasterWidth();

                    if (sourceOffsetX > totalWidthHalf) {
                        final int[] shape = new int[]{1,1,sourceHeight, sourceWidth};
                        final int[] origin = new int[]{0, vegetationClass - 1, sourceOffsetY, sourceOffsetX - totalWidthHalf  };
                        Array array = Array.factory(variable.getDataType(), shape, elems);
                        try {
                            writeable.getWriter().write(variable, origin, array);
                        } catch (InvalidRangeException e) {
                        }
                    } else if (sourceOffsetX < totalWidthHalf && (sourceOffsetX + sourceWidth + totalWidthHalf) < (totalWidth)) {
                        sourceOffsetX = sourceOffsetX + totalWidthHalf;
                        final int[] shape = new int[]{1,1,sourceHeight, sourceWidth};
                        final int[] origin = new int[]{0, vegetationClass - 1,sourceOffsetY, sourceOffsetX };
                        Array array = Array.factory(variable.getDataType(), shape, elems);
                        try {
                            writeable.getWriter().write(variable, origin, array);
                        } catch (InvalidRangeException e) {
                        }

                    } else {
                        final int[] shape = new int[]{1,1,sourceHeight, sourceWidth};
                        int[] originX1 = new int[]{0,  vegetationClass - 1,sourceOffsetY, sourceOffsetX + totalWidthHalf};
                        int[] originX2 = new int[]{0,  vegetationClass - 1,sourceOffsetY, 0};
                        int[] shape1 = new int[]{1,1,sourceHeight, totalWidthHalf - sourceOffsetX};
                        int[] shape2 = new int[]{1,1,sourceHeight, sourceWidth - totalWidthHalf + sourceOffsetX};
                        Array array = Array.factory(variable.getDataType(), shape, elems);
                        int[] originCut1 = new int[]{0, 0, 0, 0};
                        int[] originCut2 = new int[]{0,0,0, totalWidth - sourceOffsetX - totalWidthHalf};
                        try {
                            Array piece1 = array.sectionNoReduce(originCut1, shape1, new int[]{1, 1, 1, 1});
                            Array piece2 = array.sectionNoReduce(originCut2, shape2, new int[]{1, 1, 1, 1});
                            writeable.getWriter().write(variable, originX1, piece1);
                            writeable.getWriter().write(variable, originX2, piece2);
                        } catch (InvalidRangeException e) {
                        }
                    }
                //}
                /*else {
                    ///
                    final int[] shape = new int[]{sourceHeight, sourceWidth, 1, 1};
                    final int[] origin = new int[]{sourceOffsetY, sourceOffsetX, 0, vegetationClass - 1};
                    Array array = Array.factory(variable.getDataType(), shape, elems);
                    try {
                        writeable.getWriter().write(variable, origin, array);
                    } catch (InvalidRangeException e) {
                    }
                    ///
                }*/
            }
        }
    }

    private boolean shallWriteVariable(String variableName) {
        if (writeable == null) {
            throw new IllegalStateException("NetCdf writer not properly initialised. Consider calling writeProductNodes() before writing data.");
        }
        return writeable.getWriter().findVariable(variableName)!=null;

    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
        if (writeable != null) {
            writeable.close();
            writeable = null;
        }
        if (variableMap != null) {
            variableMap.clear();
            variableMap = null;
        }
    }

    @Override
    public void deleteOutput() throws IOException {
        close();
        //noinspection ResultOfMethodCallIgnored
        getOutputFile().delete();
    }

    private synchronized NVariable getVariable(String variableName) throws ProductIOException {
        NVariable variable = variableMap.get(variableName);
        if (variable == null) {
            variable = writeable.findVariable(variableName);
            if (variable == null) {
                throw new ProductIOException("Nc raster data variable '" + variableName + "' not found");
            }
            variableMap.put(variableName, variable);
        }
        return variable;
    }

    private File getOutputFile() {
        return new File(getOutputString());
    }

    private String getOutputString() {
        return String.valueOf(getOutput());
    }

}
