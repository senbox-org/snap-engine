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

package org.esa.snap.core.dataio.persistence;

import org.esa.snap.core.datamodel.Mask;

class TestMaskPersistenceSpi implements PersistenceSpi {

    @Override
    public boolean canDecode(Item item) {
        if (TestMaskPersistenceConverter.isCurrentVersion(item)) {
            return true;
        }
        final PersistenceConverter<?> converter = createConverter();
        final HistoricalDecoder[] historicalDecoders = converter.getHistoricalDecoders();
        for (HistoricalDecoder decoder : historicalDecoders) {
            if (decoder.canDecode(item)) return true;
        }
        return false;
    }

    @Override
    public boolean canEncode(Object object) {
        if (object instanceof Mask) {
            final String currentName = ((Mask) object).getImageType().getName();
            return Mask.BandMathsType.TYPE_NAME.equals(currentName);
        }
        return false;
    }

    @Override
    public PersistenceConverter<?> createConverter() {
        return new TestMaskPersistenceConverter();
    }
}
