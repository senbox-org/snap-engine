package org.esa.beam.dataio.s3.slstr;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.beam.dataio.s3.Manifest;
import org.esa.beam.dataio.s3.Sentinel3ProductReader;
import org.esa.beam.framework.datamodel.Product;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SlstrLstProductFactory extends SlstrL2ProductFactory {

    public SlstrLstProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected List<String> getFileNames(Manifest manifest) {
        final List<String> fileNames = new ArrayList<String>();

        fileNames.addAll(manifest.getFileNames(new String[0]));
        // TODO - time data are provided in a 64-bit variable, so we currently don't use them

        return fileNames;
    }

    @Override
    protected void setGeoCoding(Product targetProduct) throws IOException {
        // TODO - delete when tie point data in LST are valid
    }

}
