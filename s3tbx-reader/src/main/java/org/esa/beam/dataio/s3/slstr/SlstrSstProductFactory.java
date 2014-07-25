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

import java.util.ArrayList;
import java.util.List;

public class SlstrSstProductFactory extends SlstrL2ProductFactory {

    public SlstrSstProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected List<String> getFileNames(Manifest manifest) {
        // TODO - not clear how to represent cartesian data
        // TODO - not clear how to represent time data
        // todo read l2p_flags as flag band (and index band)
        return manifest.getFileNames(new String[0]);
    }

}
