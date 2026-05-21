package org.esa.snap.dataio.netcdf.cache;

import eu.esa.snap.core.dataio.cache.CacheDataProvider;
import eu.esa.snap.core.dataio.cache.DataBuffer;
import eu.esa.snap.core.dataio.cache.VariableDescriptor;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.netcdf.util.ArrayConverter;
import org.esa.snap.dataio.netcdf.util.DataTypeUtils;
import org.esa.snap.dataio.netcdf.util.DimKey;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Attribute;

import ucar.nc2.Dimension;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class NetcdfCacheDataProvider implements CacheDataProvider {


    private final ConcurrentHashMap<String, VariableEntry> entries = new ConcurrentHashMap<>();
    private final HashMap<String, VariableDescriptor> descriptorMap = new HashMap<>();


    public NetcdfCacheDataProvider() {}


    public void register(String bandName, Variable variable, int[] imageOrigin, boolean flipY, int dataType, ArrayConverter arrayConverter, java.awt.Dimension tileSize) {
        List<Dimension> dims = variable.getDimensions();
        DimKey rasterDim = new DimKey(dims.toArray(new Dimension[0]));
        int xIndex = rasterDim.findXDimensionIndex();
        int yIndex = rasterDim.findYDimensionIndex();
        int startIndexToCopy = DimKey.findStartIndexOfBandVariables(dims);

        int width = dims.get(xIndex).getLength();
        int height = dims.get(yIndex).getLength();
        boolean shifted = isGlobalShifted180(variable);

        final boolean is3DVariable = variable.getRank() > 2 && startIndexToCopy >= 0;
        int layerIndex = is3DVariable ? startIndexToCopy : -1;
        int baseLayer = imageOrigin.length > 0 ? imageOrigin[0] : 0;
        int layers = is3DVariable ? dims.get(layerIndex).getLength() : 1;

        VariableEntry varEntry = new VariableEntry(variable, imageOrigin.clone(), flipY,
                dataType, arrayConverter, xIndex, yIndex, startIndexToCopy,
                width, height, shifted, layerIndex, baseLayer);

        entries.put(bandName, varEntry);

        VariableDescriptor descriptor = new VariableDescriptor();
        descriptor.name = bandName;
        descriptor.dataType = dataType;
        descriptor.width = width;
        descriptor.height = height;
        descriptor.layers = layers;
        descriptor.tileWidth = tileSize.width;
        descriptor.tileHeight = tileSize.height;
        descriptor.tileLayers = is3DVariable ? 1 : -1;

        descriptorMap.put(bandName, descriptor);
    }

    @Override
    public VariableDescriptor getVariableDescriptor(String variableName) throws IOException {
        return descriptorMap.get(variableName);
    }

    @Override
    public DataBuffer readCacheBlock(String variableName, int[] offsets, int[] shapes,
                                     ProductData targetData) throws IOException {
        VariableEntry entry = getEntry(variableName);

        final boolean is3D = offsets.length == 3 && shapes.length == 3;

        int sourceLayer = is3D ? offsets[0] : entry.baseLayer;
        int sourceOffsetY = is3D ? offsets[1] : offsets[0];
        int sourceOffsetX = is3D ? offsets[2] : offsets[1];

        int sourceLayers = is3D ? shapes[0] : 1;
        if (sourceLayers != 1) {
            throw new IOException("Reading multiple 3D layers at once is not supported yet: " + sourceLayers);
        }
        int sourceHeight = is3D ? shapes[1] : shapes[0];
        int sourceWidth = is3D ? shapes[2] : shapes[1];

        int size = sourceWidth * sourceHeight * sourceLayers;

        ProductData productData = targetData != null
                ? targetData
                : ProductData.createInstance(entry.dataType, size);

        if (entry.globallyShifted180 && sourceLayers == 1) {
            int halfWidth = entry.sourceWidth / 2;
            if (sourceOffsetX < halfWidth && sourceOffsetX + sourceWidth > halfWidth) {
                fillBlock180Split(entry, sourceLayer, sourceOffsetX, sourceOffsetY,
                        sourceWidth, sourceHeight, halfWidth, productData);
                return new DataBuffer(productData, offsets, shapes);
            }
        }

        Array array = readFromVariable(entry, sourceLayer, sourceOffsetX, sourceOffsetY,
                sourceWidth, sourceHeight, sourceLayers);

        final DataType targetType = DataTypeUtils.getNetcdfDataType(productData.getType());
        productData.setElems(array.get1DJavaArray(targetType));

        return new DataBuffer(productData, offsets, shapes);
    }

    private void fillBlock180Split(VariableEntry entry, int sourceLayer,
                                   int sourceOffsetX, int sourceOffsetY,
                                   int sourceWidth, int sourceHeight, int halfWidth,
                                   ProductData productData) throws IOException {
        int rightWidth = halfWidth - sourceOffsetX;
        int leftWidth = sourceWidth - rightWidth;

        int actualY = entry.flipY
                ? entry.sourceHeight - sourceOffsetY - sourceHeight
                : sourceOffsetY;

        Array rightArr = readRawBlock(entry, sourceLayer, sourceOffsetX + halfWidth,
                actualY, rightWidth, sourceHeight, 1);
        Array leftArr = readRawBlock(entry, sourceLayer, 0,
                actualY, leftWidth, sourceHeight, 1);

        if (entry.xIndex < entry.yIndex) {
            rightArr = rightArr.transpose(entry.xIndex, entry.yIndex);
            leftArr = leftArr.transpose(entry.xIndex, entry.yIndex);
        }

        rightArr = entry.arrayConverter.convert(rightArr);
        leftArr = entry.arrayConverter.convert(leftArr);

        if (entry.flipY) {
            rightArr = rightArr.flip(0);
            leftArr = leftArr.flip(0);
        }

        final DataType targetType = DataTypeUtils.getNetcdfDataType(productData.getType());
        Object rightElems = rightArr.get1DJavaArray(targetType);
        Object leftElems = leftArr.get1DJavaArray(targetType);
        Object destElems = productData.getElems();

        for (int row = 0; row < sourceHeight; row++) {
            System.arraycopy(rightElems, row * rightWidth, destElems, row * sourceWidth, rightWidth);
            System.arraycopy(leftElems, row * leftWidth, destElems, row * sourceWidth + rightWidth, leftWidth);
        }
    }



    private Array readFromVariable(VariableEntry entry, int sourceLayer,
                                   int sourceOffsetX, int sourceOffsetY,
                                   int width, int height, int layers) throws IOException {
        int actualY = entry.flipY
                ? (entry.sourceHeight - sourceOffsetY - height)
                : sourceOffsetY;

        if (actualY < 0) {
            height += actualY;
            actualY = 0;
        }

        int fileX = resolveXOrigin(entry, sourceOffsetX);
        Array array = readRawBlock(entry, sourceLayer, fileX, actualY, width, height, layers);

        if (entry.xIndex < entry.yIndex) {
            array = array.transpose(entry.xIndex, entry.yIndex);
        }

        array = entry.arrayConverter.convert(array);

        if (entry.flipY) {
            array = array.flip(0);
        }

        return array;
    }

    private int resolveXOrigin(VariableEntry entry, int sourceOffsetX) {
        if (!entry.globallyShifted180) {
            return sourceOffsetX;
        }
        int halfWidth = entry.sourceWidth / 2;
        return sourceOffsetX < halfWidth
                ? sourceOffsetX + halfWidth
                : sourceOffsetX - halfWidth;
    }


    private Array readRawBlock(VariableEntry entry, int sourceLayer,
                               int fileOffsetX, int fileOffsetY,
                               int width, int height, int layers) throws IOException {
        Variable variable = entry.variable;
        int rank = variable.getRank();
        int[] origin = new int[rank];
        int[] shape = new int[rank];
        Arrays.fill(shape, 1);

        shape[entry.yIndex] = height;
        shape[entry.xIndex] = width;

        if (entry.layerIndex >= 0) {
            origin[entry.layerIndex] = sourceLayer;
            shape[entry.layerIndex] = layers;
        } else {
            System.arraycopy(entry.imageOrigin, 0, origin, entry.startIndexToCopy, entry.imageOrigin.length);
        }

        origin[entry.yIndex] = fileOffsetY;
        origin[entry.xIndex] = fileOffsetX;

        synchronized (this) {
            try {
                return variable.read(new Section(origin, shape));
            } catch (InvalidRangeException e) {
                throw new IOException("Invalid range reading variable: " + variable.getFullName(), e);
            }
        }
    }

    private static boolean isGlobalShifted180(Variable variable) {
        for (Attribute attribute : variable.attributes()) {
            if (attribute.getShortName().equals("LONGITUDE_SHIFTED_180")) {
                return true;
            }
        }
        return false;
    }


    private VariableEntry getEntry(String variableName) throws IOException {
        VariableEntry entry = entries.get(variableName);
        if (entry == null) {
            throw new IOException("No variable registered for band: " + variableName);
        }
        return entry;
    }


    private static class VariableEntry {

        final Variable variable;
        final int[] imageOrigin;
        final boolean flipY;
        final int dataType;
        final ArrayConverter arrayConverter;
        final int xIndex;
        final int yIndex;
        final int startIndexToCopy;
        final int sourceWidth;
        final int sourceHeight;
        final boolean globallyShifted180;
        final int layerIndex;
        final int baseLayer;

        VariableEntry(Variable variable, int[] imageOrigin, boolean flipY, int dataType, ArrayConverter arrayConverter,
                      int xIndex, int yIndex, int startIndexToCopy, int sourceWidth, int sourceHeight, boolean globallyShifted180,
                      int layerIndex, int baseLayer) {
            this.variable = variable;
            this.imageOrigin = imageOrigin;
            this.flipY = flipY;
            this.dataType = dataType;
            this.arrayConverter = arrayConverter;
            this.xIndex = xIndex;
            this.yIndex = yIndex;
            this.startIndexToCopy = startIndexToCopy;
            this.sourceWidth = sourceWidth;
            this.sourceHeight = sourceHeight;
            this.globallyShifted180 = globallyShifted180;
            this.layerIndex = layerIndex;
            this.baseLayer = baseLayer;
        }
    }
}
