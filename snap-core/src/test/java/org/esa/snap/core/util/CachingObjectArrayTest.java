/*
 * Copyright (C) 2026 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.snap.core.util;

import com.bc.ceres.annotation.STTM;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class CachingObjectArrayTest {

    @Test
    @STTM("SNAP-4391")
    public void getObjectCachesWithinRange() throws Exception {
        final AtomicInteger created = new AtomicInteger();
        final CachingObjectArray array = new CachingObjectArray(index -> {
            created.incrementAndGet();
            return new Object();
        });
        array.setCachedRange(0, 9);

        final Object first = array.getObject(3);
        assertSame(first, array.getObject(3));
        assertEquals(1, created.get());
    }

    @Test
    @STTM("SNAP-4391")
    public void removeObjectClearsTheSlotSoItIsRecreated() throws Exception {
        final AtomicInteger created = new AtomicInteger();
        final CachingObjectArray array = new CachingObjectArray(index -> {
            created.incrementAndGet();
            return new Object();
        });
        array.setCachedRange(0, 9);

        final Object first = array.getObject(3);
        array.removeObject(3);
        final Object second = array.getObject(3);

        assertNotSame(first, second);
        assertEquals(2, created.get());
    }

    @Test
    @STTM("SNAP-4391")
    public void setObjectStillOnlyFillsAnEmptySlot() throws Exception {
        final CachingObjectArray array = new CachingObjectArray(index -> "created");
        array.setCachedRange(0, 9);

        array.setObject(2, "a");
        array.setObject(2, null);   // the former (no-op) eviction call
        assertEquals("a", array.getObject(2));
    }

    @Test
    @STTM("SNAP-4391")
    public void removeObjectIgnoresOutOfRangeAndUninitialised() {
        final CachingObjectArray array = new CachingObjectArray(index -> "x");
        array.removeObject(5);      // no range set yet
        array.setCachedRange(0, 9);
        array.removeObject(-1);
        array.removeObject(10);
    }
}
