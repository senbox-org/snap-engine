/*
 *
 *  Copyright (C) 2020 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 3 of the License, or (at your option)
 *  any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.snap.core.dataio.geocoding;

import org.esa.snap.core.dataio.dimap.spi.DimapPersistable;
import org.esa.snap.core.dataio.dimap.spi.DimapPersistableSpi;
import org.jdom.Element;

import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistable.TAG_COMPONENT_GEO_CODING;

/**
 * @deprecated use {@link ComponentGeoCodingPersistenceSpi} instead
 */
@Deprecated
public class ComponentGeoCodingPersistableSpi implements DimapPersistableSpi {
    @Override
    public boolean canDecode(Element element) {
        return element != null && element.getChild(TAG_COMPONENT_GEO_CODING) != null;
    }

    @Override
    public boolean canPersist(Object object) {
        return object instanceof ComponentGeoCoding;
    }

    @Override
    public DimapPersistable createPersistable() {
        return new ComponentGeoCodingPersistable();
    }
}
