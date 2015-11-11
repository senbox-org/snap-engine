package org.esa.s3tbx.dataio.s3.util;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.dataio.netcdf.util.AbstractNetcdfMultiLevelImage;
import ucar.nc2.Variable;

import java.awt.Dimension;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

/**
 * @author Tonio Fincke
 */
public class S3MultiLevelOpImage extends AbstractNetcdfMultiLevelImage {

    private final Variable variable;
    private final int[] dimensionIndexes;
    private final String[] dimensionNames;
    private Variable referencedIndexVariable;
    private String nameOfReferencingIndexDimension;
    private String nameOfDisplayedDimension;
    private int xIndex;
    private int yIndex;

    public S3MultiLevelOpImage(RasterDataNode rasterDataNode, Variable variable,
                               String[] dimensionNames, int[] dimensionIndexes,
                               int xIndex, int yIndex) {
        super(rasterDataNode);
        this.variable = variable;
        this.dimensionNames = dimensionNames;
        this.dimensionIndexes = dimensionIndexes;
        this.xIndex  = xIndex;
        this.yIndex = yIndex;
    }

    public S3MultiLevelOpImage(RasterDataNode rasterDataNode, Variable variable,
                               String[] dimensionNames, int[] dimensionIndexes,
                               Variable referencedIndexVariable,
                               String nameOfReferencingIndexDimension, String nameOfDisplayedDimension) {
        super(rasterDataNode);
        this.variable = variable;
        this.dimensionNames = dimensionNames;
        this.dimensionIndexes = dimensionIndexes;
        this.referencedIndexVariable = referencedIndexVariable;
        this.nameOfReferencingIndexDimension = nameOfReferencingIndexDimension;
        this.nameOfDisplayedDimension = nameOfDisplayedDimension;
    }

    @Override
    protected RenderedImage createImage(int level) {
        RasterDataNode rasterDataNode = getRasterDataNode();
        int dataBufferType = ImageManager.getDataBufferType(rasterDataNode.getDataType());
        int sceneRasterWidth = rasterDataNode.getRasterWidth();
        int sceneRasterHeight = rasterDataNode.getRasterHeight();
        ResolutionLevel resolutionLevel = ResolutionLevel.create(getModel(), level);
        Dimension imageTileSize = new Dimension(getTileWidth(), getTileHeight());
        if(referencedIndexVariable  != null && nameOfReferencingIndexDimension != null && nameOfDisplayedDimension != null) {
            return new S3ReferencingVariableOpImage(variable, dataBufferType, sceneRasterWidth, sceneRasterHeight,
                                                    imageTileSize, resolutionLevel, dimensionIndexes,
                                                    referencedIndexVariable, nameOfReferencingIndexDimension,
                                                    nameOfDisplayedDimension);
        }
        if(rasterDataNode.getName().endsWith("_msb")) {
            return S3VariableOpImage.createS3VariableOpImage(variable, dataBufferType, sceneRasterWidth,
                                                             sceneRasterHeight, imageTileSize, resolutionLevel,
                                                             dimensionNames, dimensionIndexes, xIndex, yIndex, true);
        } else if(rasterDataNode.getName().endsWith("_lsb")) {
            return S3VariableOpImage.createS3VariableOpImage(variable, dataBufferType, sceneRasterWidth,
                                                             sceneRasterHeight, imageTileSize, resolutionLevel,
                                                             dimensionNames, dimensionIndexes, xIndex, yIndex, false);
        }
        //todo remove references to specific band names
        if ((variable.getFullName().contains("row_corresp") || (variable.getFullName().contains("col_corresp"))) &&
                rasterDataNode.getDataType() == ProductData.TYPE_UINT32) {
            return new S3VariableOpImage(variable, DataBuffer.TYPE_FLOAT, sceneRasterWidth, sceneRasterHeight, imageTileSize,
                                         resolutionLevel, dimensionNames, dimensionIndexes, xIndex, yIndex,
                                         S3VariableOpImage.ArrayConverter.UINTCONVERTER);
        } else {
            return new S3VariableOpImage(variable, dataBufferType, sceneRasterWidth, sceneRasterHeight, imageTileSize,
                                         resolutionLevel, dimensionNames, dimensionIndexes, xIndex, yIndex);
        }
    }

}
