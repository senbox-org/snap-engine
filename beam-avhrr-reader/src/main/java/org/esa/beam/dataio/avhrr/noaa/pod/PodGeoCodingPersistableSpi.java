/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.dataio.avhrr.noaa.pod;

import org.esa.beam.dataio.dimap.spi.DimapPersistable;
import org.esa.beam.dataio.dimap.spi.DimapPersistableSpi;
import org.jdom.Element;

public class PodGeoCodingPersistableSpi implements DimapPersistableSpi {

    @Override
    public boolean canDecode(Element element) {
        return element.getChild(PodGeoCodingPersistable.POD_GEO_CODING_TAG) != null;
    }

    @Override
    public boolean canPersist(Object object) {
        return object instanceof PodGeoCoding;
    }

    @Override
    public DimapPersistable createPersistable() {
        return new PodGeoCodingPersistable();
    }
}
