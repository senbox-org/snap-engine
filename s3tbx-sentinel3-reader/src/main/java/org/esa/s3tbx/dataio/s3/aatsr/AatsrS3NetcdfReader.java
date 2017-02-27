/*
 * $Id$
 *
 * Copyright (C) 2010 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.s3tbx.dataio.s3.aatsr;

import org.esa.s3tbx.dataio.s3.util.S3NetcdfReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import ucar.ma2.DataType;
import ucar.nc2.Variable;

public class AatsrS3NetcdfReader extends S3NetcdfReader {

    @Override
    protected void addVariableAsBand(Product product, Variable variable, String variableName, boolean synthetic) {
// todo ideas+
        if (DataType.CHAR.equals(variable.getDataType())) {
            final Band band = product.addBand(variableName, ProductData.TYPE_INT8);
            band.setDescription(variable.getDescription());
            band.setUnit(variable.getUnitsString());
            band.setScalingFactor(getScalingFactor(variable));
            band.setScalingOffset(getAddOffset(variable));
            band.setSynthetic(synthetic);
            addFillValue(band, variable);
            addSampleCodings(product, band, variable, false);
        } else {
            super.addVariableAsBand(product, variable, variableName, synthetic);
        }
    }
}
