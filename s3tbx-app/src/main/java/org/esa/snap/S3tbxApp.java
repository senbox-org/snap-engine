/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap;

import com.bc.ceres.core.ProgressMonitor;
import com.jidesoft.plaf.LookAndFeelFactory;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.application.ApplicationDescriptor;
import org.esa.beam.visat.VisatApp;

import javax.swing.UIManager;

public final class S3tbxApp extends VisatApp {

    public S3tbxApp(ApplicationDescriptor applicationDescriptor) {
        super(applicationDescriptor);
    }

    @Override
    protected void initClientUI(ProgressMonitor pm) {
        super.initClientUI(pm);
    }

    protected void loadJideExtension() {
        LookAndFeelFactory.installJideExtension(LookAndFeelFactory.EXTENSION_STYLE_ECLIPSE);
        UIManager.getDefaults().put("DockableFrameTitlePane.showIcon", Boolean.TRUE);
        UIManager.getDefaults().put("SidePane.alwaysShowTabText", Boolean.TRUE);
        UIManager.getDefaults().put("SidePane.orientation", 1);
    }

    @Override
    protected String getMainFrameTitle() {
        final String ver = System.getProperty("snap.version");
        return getAppName() + ' ' + ver;
    }

    @Override
    protected ModalDialog createAboutBox() {
        //todo
        return null;
    }

}
