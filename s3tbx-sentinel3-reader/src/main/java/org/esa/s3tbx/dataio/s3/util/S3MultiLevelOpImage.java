package org.esa.s3tbx.dataio.s3.util;

import org.esa.snap.dataio.netcdf.util.AbstractNetcdfMultiLevelImage;
import org.esa.snap.framework.datamodel.RasterDataNode;
import org.esa.snap.jai.ImageManager;
import org.esa.snap.jai.ResolutionLevel;
import ucar.nc2.Variable;

import java.awt.Dimension;
import java.awt.image.RenderedImage;

/**
 * @author Tonio Fincke
 */
public class S3MultiLevelOpImage extends AbstractNetcdfMultiLevelImage {

    private final Variable variable;
    private final int dimensionIndex;
    private final String dimensionName;
    private Variable referencedIndexVariable;
    private String nameOfReferencingIndexDimension;
    private String nameOfDisplayedDimension;
    private boolean dimensionsTurned;
    private RasterDataNode rasterDataNode;

    public S3MultiLevelOpImage(RasterDataNode rasterDataNode, Variable variable, String dimensionName,
                               int dimensionIndex, boolean dimensionsTurned) {
        super(rasterDataNode);
        this.rasterDataNode = rasterDataNode;
        this.variable = variable;
        this.dimensionName = dimensionName;
        this.dimensionIndex = dimensionIndex;
        this.dimensionsTurned = dimensionsTurned;
    }

    public S3MultiLevelOpImage(RasterDataNode rasterDataNode, Variable variable, String dimensionName,
                               int dimensionIndex, Variable referencedIndexVariable,
                               String nameOfReferencingIndexDimension, String nameOfDisplayedDimension) {
        super(rasterDataNode);
        this.variable = variable;
        this.dimensionName = dimensionName;
        this.dimensionIndex = dimensionIndex;
        this.referencedIndexVariable = referencedIndexVariable;
        this.nameOfReferencingIndexDimension = nameOfReferencingIndexDimension;
        this.nameOfDisplayedDimension = nameOfDisplayedDimension;
    }

    @Override
    protected RenderedImage createImage(int level) {
        RasterDataNode rasterDataNode = getRasterDataNode();
        int dataBufferType = ImageManager.getDataBufferType(rasterDataNode.getDataType());
        int sceneRasterWidth = rasterDataNode.getSceneRasterWidth();
        int sceneRasterHeight = rasterDataNode.getSceneRasterHeight();
        ResolutionLevel resolutionLevel = ResolutionLevel.create(getModel(), level);
        Dimension imageTileSize = new Dimension(getTileWidth(), getTileHeight());
        if(referencedIndexVariable  != null && nameOfReferencingIndexDimension != null && nameOfDisplayedDimension != null) {
            return new S3ReferencingVariableOpImage(variable, dataBufferType, sceneRasterWidth, sceneRasterHeight,
                                                    imageTileSize, resolutionLevel, dimensionIndex,
                                                    referencedIndexVariable, nameOfReferencingIndexDimension,
                                                    nameOfDisplayedDimension);
        }
        if(rasterDataNode.getName().endsWith("_msb")) {
            return S3VariableOpImage.createS3VariableOpImage(variable, dataBufferType, sceneRasterWidth,
                                                             sceneRasterHeight, imageTileSize, resolutionLevel,
                                                             dimensionName, dimensionIndex, dimensionsTurned, true);
        } else if(rasterDataNode.getName().endsWith("_lsb")) {
            return S3VariableOpImage.createS3VariableOpImage(variable, dataBufferType, sceneRasterWidth,
                                                             sceneRasterHeight, imageTileSize, resolutionLevel,
                                                             dimensionName, dimensionIndex, dimensionsTurned, false);
        }
        return new S3VariableOpImage(variable, dataBufferType, sceneRasterWidth, sceneRasterHeight, imageTileSize,
                              resolutionLevel, dimensionName, dimensionIndex, dimensionsTurned);
    }

}
