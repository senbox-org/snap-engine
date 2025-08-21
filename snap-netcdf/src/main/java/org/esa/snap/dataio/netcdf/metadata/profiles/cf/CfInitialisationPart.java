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

package org.esa.snap.dataio.netcdf.metadata.profiles.cf;

import org.esa.snap.core.dataio.ProductIOException;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.ProfileInitPartIO;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.util.Constants;
import org.esa.snap.dataio.netcdf.util.DimKey;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;

import java.awt.Dimension;
import java.io.IOException;
import java.util.List;

public class CfInitialisationPart extends ProfileInitPartIO {

    @Override
    public Product readProductBody(ProfileReadContext ctx) throws ProductIOException {
        Product product = new Product(
                (String) ctx.getProperty(Constants.PRODUCT_FILENAME_PROPERTY),
                readProductType(ctx),
                ctx.getRasterDigest().getRasterDim().getDimensionX().getLength(),
                ctx.getRasterDigest().getRasterDim().getDimensionY().getLength()
        );

        initPreferredTileSize(ctx, product);

        return product;
    }

    protected void initPreferredTileSize(ProfileReadContext ctx, Product product) {
        if (!initPreferredTileSizeFromChunkSizes(ctx, product)) {
            initPreferredTileSizeFromGAttribute(ctx, product);
        }
    }

    private void initPreferredTileSizeFromGAttribute(ProfileReadContext ctx, Product product) {
        Attribute tileSize = ctx.getNetcdfFile().findGlobalAttribute("TileSize");
        if (tileSize != null) {
            String stringValue = tileSize.getStringValue();
            if (stringValue != null && stringValue.contains(":")) {
                String[] tileSizes = stringValue.split(":");
                if (tileSizes.length == 2) {
                    try {
                        int tHeight = Integer.parseInt(tileSizes[0]);
                        int tWidth = Integer.parseInt(tileSizes[1]);
                        product.setPreferredTileSize(tWidth, tHeight);
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
        }
    }

    private boolean initPreferredTileSizeFromChunkSizes(ProfileReadContext ctx, Product product) {
        List<Variable> variables = ctx.getNetcdfFile().getVariables();

        // First try to find a 'pseudo 3D' raster variable. A common case for this is a raster array
        // with a time dimension for a single time stamp, such as array(time, lat, lon) with
        // time = UNLIMITED (1 currently)
        Variable variable = getFirstPseudo3dRasterVariable(variables);
        if (variable != null) {
            final Attribute att = variable.findAttribute(CDM.CHUNK_SIZES);
            if (att != null) {
                final Number numericValue = att.getNumericValue(1);
                final Number numericValue1 = att.getNumericValue(2);
                if (numericValue != null && numericValue1 != null) {
                    product.setPreferredTileSize(numericValue.intValue(), numericValue1.intValue());
                    return true;
                }
            }
        }

        // if there is no 3D variable, search for a 2D raster variable...
        variable = getFirst2dRasterVariable(variables);
        if (variable != null) {
            final Attribute att = variable.findAttribute(CDM.CHUNK_SIZES);
            if (att != null) {
                final Number numericValue = att.getNumericValue(0);
                final Number numericValue1 = att.getNumericValue(1);
                if (numericValue != null && numericValue1 != null) {
                    product.setPreferredTileSize(numericValue.intValue(), numericValue1.intValue());
                    return true;
                }
            }
        }
        return false;
    }

    private Variable getFirst2dRasterVariable(List<Variable> variables) {
        // if present, finds the first 2D raster variable (i.e., dimensions are typical raster dimensions)
        for (Variable variable : variables) {
            final List<ucar.nc2.Dimension> dimensions = variable.getDimensions();
            if (dimensions.size() == 2) {
                // make sure that dimensions are typical raster dimensions,
                // i.e., that names of dimensions are in DimKey.TYPICAL_X_DIM_NAMES, DimKey.TYPICAL_Y_DIM_NAMES
                final DimKey dimKey = new DimKey(dimensions.get(0), dimensions.get(1));
                if (dimKey.isTypicalRasterDim()) {
                    return variable;
                }
            }
        }
        return null;
    }

    private Variable getFirstPseudo3dRasterVariable(List<Variable> variables) {
        // if present, finds the first 3D variable with first dimension value = 1,
        // and second and third dimension form typical raster dimensions
        for (Variable variable : variables) {
            final List<ucar.nc2.Dimension> dimensions = variable.getDimensions();
            if (dimensions.size() == 3) {
                final Attribute att = variable.findAttribute(CDM.CHUNK_SIZES);
                if (att != null) {
                    final Number firstDimNumericValue = att.getNumericValue(0);
                    if (firstDimNumericValue != null && firstDimNumericValue.intValue() == 1) {
                        // make sure that 2nd and 3rd dimensions are typical raster dimensions,
                        // i.e., that names of dimensions are in DimKey.TYPICAL_X_DIM_NAMES, DimKey.TYPICAL_Y_DIM_NAMES
                        final DimKey dimKey = new DimKey(dimensions.get(1), dimensions.get(2));
                        if (dimKey.isTypicalRasterDim()) {
                            return variable;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void writeProductBody(ProfileWriteContext ctx, Product product) throws IOException {
        NFileWriteable writeable = ctx.getNetcdfFileWriteable();
        writeable.addGlobalAttribute("Conventions", "CF-1.4");
        if (!isLatLonPresent(product) && CfGeocodingPart.isGeographicCRS(product.getSceneGeoCoding())) {
            writeDimensions(writeable, product, "lat", "lon");
        } else if (isLatLonPresent(product) && CfGeocodingPart.isGeographicCRS(product.getSceneGeoCoding())) {
            writeDimensions(writeable, product, "lat_intern", "lon_intern");
        } else {
            writeDimensions(writeable, product, "y", "x");
        }
        Dimension tileSize = ImageManager.getPreferredTileSize(product);
        writeable.addGlobalAttribute("TileSize", tileSize.height + ":" + tileSize.width);
    }

    private boolean isLatLonPresent(Product product) {
        return product.containsRasterDataNode("lat") && product.containsRasterDataNode("lon");
    }

    private void writeDimensions(NFileWriteable writeable, Product p, String dimY, String dimX) throws IOException {
        writeable.addDimension(dimY, p.getSceneRasterHeight());
        writeable.addDimension(dimX, p.getSceneRasterWidth());
    }

    public String readProductType(final ProfileReadContext ctx) {
        Attribute productType = ctx.getNetcdfFile().findGlobalAttribute("Conventions");
        if (productType != null && StringUtils.isNotNullAndNotEmpty(productType.getStringValue())) {
            return productType.getStringValue();
        } else {
            return Constants.FORMAT_NAME;
        }
    }
}
