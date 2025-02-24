/*
 *
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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
 *
 */

package org.esa.snap.dataio.znap;

import com.bc.ceres.multilevel.MultiLevelImage;
import com.bc.ceres.multilevel.MultiLevelModel;
import com.bc.zarr.ZarrArray;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.image.SingleBandedOpImage;
import org.esa.snap.core.util.ImageUtils;
import ucar.ma2.InvalidRangeException;

import javax.media.jai.PlanarImage;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.WritableRaster;
import java.io.IOException;

/**
 * A base class for {@code OpImage}s acting as source image for
 * a {@link RasterDataNode}.
 *
 * @author Sabine Embacher
 * @see RasterDataNode#getSourceImage()
 * @see RasterDataNode#getGeophysicalImage()
 * @see RasterDataNode#setSourceImage(MultiLevelImage)
 */
public class ZarrOpImage extends SingleBandedOpImage {

    private final RasterDataNode rasterDataNode;
    private final ZarrArray arrayDataReader;

    /**
     * Constructor.
     *
     * @param rasterDataNode The target raster data node.
     * @param level          The resolution level.
     *
     * @see ResolutionLevel#create(MultiLevelModel, int)
     */
    public ZarrOpImage(RasterDataNode rasterDataNode, int[] shape, int[] chunks, ZarrArray reader, ResolutionLevel level) {
        super(ImageManager.getDataBufferType(rasterDataNode.getDataType()),
              shape[1], shape[0],
              new Dimension(chunks[1], chunks[0]),
              null, level);
        this.rasterDataNode = rasterDataNode;
        arrayDataReader = reader;
    }

    @Override
    public String toString() {
        String className = getClass().getSimpleName();
        String productName = "";
        if (rasterDataNode.getProduct() != null) {
            productName = ":" + rasterDataNode.getProduct().getName();
        }
        String bandName = "." + rasterDataNode.getName();
        return className + productName + bandName;
    }

    @Override
    protected void computeRect(PlanarImage[] sourceImages, WritableRaster tile, Rectangle destRect) {
        ProductData productData;
        boolean directMode = tile.getDataBuffer().getSize() == destRect.width * destRect.height;
        if (directMode) {
            productData = ProductData.createInstance(rasterDataNode.getDataType(),
                                                     ImageUtils.getPrimitiveArray(tile.getDataBuffer()));
        } else {
            productData = ProductData.createInstance(rasterDataNode.getDataType(),
                                                     destRect.width * destRect.height);
        }

        try {
            computeProductData(productData, destRect);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!directMode) {
            tile.setDataElements(destRect.x, destRect.y, destRect.width, destRect.height, productData.getElems());
        }
    }

    /**
     * Computes the target pixel data for this level image.
     *
     * @param productData The target pixel buffer to write to. The number of elements in this buffer will always be
     *                    {@code region.width * region.height}.
     * @param region      The target region in pixel coordinates valid for this image level.
     *
     * @throws IOException May be thrown if an I/O error occurs during the computation.
     */
    protected void computeProductData(ProductData productData, Rectangle region) throws IOException {
        try {
            arrayDataReader.read(productData.getElems(), new int[]{region.height, region.width}, new int[]{region.y, region.x});
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        }
    }
}
