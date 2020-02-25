/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
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

package org.esa.snap.lib.openjpeg.activator;

import org.esa.snap.runtime.Activator;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.commons.lang.SystemUtils.IS_OS_UNIX;

/**
 * Activator class for deploying OpenJPEG binaries to the aux data dir
 *
 * @author Julien Malik
 */
public class OpenJPEGActivator implements Activator {

    private static final Logger log = Logger.getLogger(OpenJPEGActivator.class.getName());

    public OpenJPEGActivator() {
    }

    @Override
    public void start() {
        try {
           OpenJPEGInstaller.install();
        } catch (Exception e) {
            log.log(Level.SEVERE,"OpenJPEG configuration error", e);
        }
    }

    @Override
    public void stop() {
        // Purposely no-op
    }
}
