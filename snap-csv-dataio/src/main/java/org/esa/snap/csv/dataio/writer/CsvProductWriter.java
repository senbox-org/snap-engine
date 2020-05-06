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

package org.esa.snap.csv.dataio.writer;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductWriter;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.dataio.geocoding.ComponentGeoCoding;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.csv.dataio.Constants;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import static org.esa.snap.csv.dataio.Constants.PROPERTY_NAME_RASTER_RESOLUTION;

/**
 * Allows writing a {@link Product} in CSV format.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class CsvProductWriter extends AbstractProductWriter {

    static final int WRITE_PROPERTIES = 1;
    static final int WRITE_FEATURES = 2;

    private final int config;

    protected Writer writer;

    private String separator;
    private boolean productWritten;

    CsvProductWriter(ProductWriterPlugIn plugIn, int config, Writer writer) {
        super(plugIn);
        this.writer = writer;
        this.config = config;
        this.productWritten = false;
    }

    // package access for testing only tb 2020-02-28
    static String getJavaType(int dataType) {
        switch (dataType) {
            case DataBuffer.TYPE_FLOAT: {
                return "float";
            }
            case DataBuffer.TYPE_DOUBLE: {
                return "double";
            }
            case DataBuffer.TYPE_BYTE: {
                return "byte";
            }
            case DataBuffer.TYPE_SHORT: {
                return "short";
            }
            case DataBuffer.TYPE_USHORT: {
                return "ushort";
            }
            case DataBuffer.TYPE_INT: {
                return "int";
            }
            default: {
                throw new IllegalArgumentException("Unsupported type '" + dataType + "'.");
            }
        }
    }

    @Override
    protected void writeProductNodesImpl() throws IOException {
        ensureWriter();
        getSeparatorFromMetaData();
        writeProperties();
        writeHeader();
    }

    private void writeHeader() throws IOException {
        final Band[] bands = getSourceProduct().getBands();
        StringBuilder builder = new StringBuilder();
        builder.append(getFeatureIdColumnNameFromMetadata());
        for (final Band band : bands) {
            builder.append(separator);
            builder.append(band.getName());
            builder.append(":");
            final int dataType = band.getGeophysicalImage().getData().getDataBuffer().getDataType();
            builder.append(getJavaType(dataType));
        }
        final TiePointGrid[] tiePointGrids = getSourceProduct().getTiePointGrids();
        for (final TiePointGrid tpg : tiePointGrids) {
            builder.append(separator);
            builder.append(tpg.getName());
            builder.append(":");
            final int dataType = tpg.getGeophysicalImage().getData().getDataBuffer().getDataType();
            builder.append(getJavaType(dataType));
        }
        writeLine(builder.toString());
    }

    private void ensureWriter() throws IOException {
        if (writer == null) {
            writer = new FileWriter(new File(getOutput().toString()));
        }
    }

    private void writeLine(String line) throws IOException {
        writer.write(line);
        writer.write("\n");
    }

    private void writeProperties() throws IOException {
        if ((config & WRITE_PROPERTIES) != WRITE_PROPERTIES) {
            return;
        }

        final Product product = getSourceProduct();
        writeLine(Constants.COMMENT + Constants.PROPERTY_NAME_SCENE_RASTER_WIDTH + "=" + product.getSceneRasterWidth());

        final GeoCoding sceneGeoCoding = product.getSceneGeoCoding();
        if (sceneGeoCoding instanceof ComponentGeoCoding) {
            final ComponentGeoCoding geoCoding = (ComponentGeoCoding) sceneGeoCoding;
            final double rasterResolutionInKm = geoCoding.getGeoRaster().getRasterResolutionInKm();
            writeLine(Constants.COMMENT + PROPERTY_NAME_RASTER_RESOLUTION + "=" + rasterResolutionInKm);
        }
    }

    @Override
    public void writeBandRasterData(Band sourceBand, int sourceOffsetX, int sourceOffsetY,
                                    int sourceWidth, int sourceHeight, ProductData sourceBuffer,
                                    ProgressMonitor pm) throws IOException {
        if (productWritten) {
            return;
        }

        final RasterDataNode[] bands = getSourceProduct().getBands();
        final TiePointGrid[] tiePointGrids = getSourceProduct().getTiePointGrids();
        final RasterDataNode[] rasterDataNodes = new RasterDataNode[bands.length + tiePointGrids.length];
        System.arraycopy(bands, 0, rasterDataNodes, 0, bands.length);
        System.arraycopy(tiePointGrids, 0, rasterDataNodes, bands.length, tiePointGrids.length);

        final int[] dataTypes = new int[rasterDataNodes.length];

        final Raster[] dataRasters = new Raster[rasterDataNodes.length];
        // todo: For big products this will not work because the whole scene is allocated. But no one wants to
        //  export big scenes to ASCII. So this is minor and can be addressed when the need arises.
        Rectangle dataRect = new Rectangle(getSourceProduct().getSceneRasterWidth(), getSourceProduct().getSceneRasterHeight());
        for (int i = 0; i < rasterDataNodes.length; i++) {
            dataRasters[i] = rasterDataNodes[i].getGeophysicalImage().getData(dataRect);
            dataTypes[i] = dataRasters[i].getDataBuffer().getDataType();
        }

        for (int j = 0; j < getSourceProduct().getSceneRasterHeight(); j++) {
            for (int i = 0; i < getSourceProduct().getSceneRasterWidth(); i++) {
                StringBuilder line = new StringBuilder();
                line.append(getFeatureIdFromMetadata(j * getSourceProduct().getSceneRasterWidth() + i));
                for (int k = 0; k < dataRasters.length; k++) {
                    final Raster raster = dataRasters[k];
                    line.append(separator);
                    final Number elem;
                    final int type = dataTypes[k];
                    if (type == DataBuffer.TYPE_INT || type == DataBuffer.TYPE_SHORT ||
                            type == DataBuffer.TYPE_USHORT || type == DataBuffer.TYPE_BYTE) {
                        elem = raster.getSample(i, j, 0);
                    } else if (type == DataBuffer.TYPE_FLOAT) {
                        elem = raster.getSampleFloat(i, j, 0);
                    } else if (type == DataBuffer.TYPE_DOUBLE) {
                        elem = raster.getSampleDouble(i, j, 0);
                    } else {
                        throw new IllegalArgumentException("Undefined data type '" + type + "' for raster data node '" + rasterDataNodes[k] + "' in source product.");
                    }
                    line.append(elem);
                }

                writeLine(line.toString());
            }
        }

        productWritten = true;
    }

    @Override
    public void flush() throws IOException {
        if (writer != null) {
            writer.flush();
        }
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }

    @Override
    public void deleteOutput() {
        // @todo 2 tb/** is this intentionally not implemented?? tb/tb implement this! 2020-02-27
    }

    private void getSeparatorFromMetaData() {
        separator = Constants.DEFAULT_SEPARATOR;
    }

    private String getFeatureIdFromMetadata(int rowIndex) {
        // todo - get feature id for row from metadata, if existing
        return rowIndex + "";
    }

    private String getFeatureIdColumnNameFromMetadata() {
        // todo - implement metadata search
        return "featureId";
    }
}
