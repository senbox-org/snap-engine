package org.esa.snap.smart.configurator;

/*
 * Copyright (C) 2015 CS SI
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


import com.bc.ceres.test.LongTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * @author Nicolas Ducoin
 */
@RunWith(LongTestRunner.class)
public class JavaSystemInfosTest {

    private static final SystemInfos systemInfos = JavaSystemInfos.getInstance(1, 2);

    @Test
    public void testGetCPUs() {
        int nbCPUs = systemInfos.getNbCPUs();
        assertTrue(nbCPUs > 0);
    }

    @Test
    public void testMemory() {
        long ram = systemInfos.getRAM();
        assertTrue(ram > 0);
    }

    @Test
    public void testDisks() {
        String[] disks = systemInfos.getDisksNames();
        assertTrue(disks != null && disks.length > 0);
    }

    /**
     * Test the disk speed for all devices
     */
    @Test
    public void testDiskSpeed() {
        String[] disksNames = systemInfos.getDisksNames();

        for (String diskName : disksNames) {
            try {
                double readSpeed = systemInfos.getDiskReadSpeed(diskName);
                double writeSpeed = systemInfos.getDiskWriteSpeed(diskName);
                assert (0 != readSpeed && 0 != writeSpeed);
            } catch (IOException ex) {
                // ioException if we can't write or read to the device: ok
                System.out.println("Can't read or write on " + diskName);
            }
        }
    }
}
