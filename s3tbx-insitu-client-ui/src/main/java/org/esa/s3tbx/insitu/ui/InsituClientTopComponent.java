/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.s3tbx.insitu.ui;

import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import java.awt.BorderLayout;

//@TopComponent.Description(
//        preferredID = "InsituClientTopComponent",
//        iconBase = "org/esa/s3tbx/insitu/insitu24.png",
//        persistenceType = TopComponent.PERSISTENCE_ALWAYS
//)
//@TopComponent.Registration(mode = "rightSlidingSide", openAtStartup = false, position = 150)
//@ActionID(category = "Window", id = "InsituClientTopComponent")
//@ActionReference(path = "Menu/View/Tool Windows")
//@TopComponent.OpenActionRegistration(
//        displayName = "#CTL_InsituClientTopComponent_Name",
//        preferredID = "InsituClientTopComponent"
//)
@NbBundle.Messages({"CTL_InsituClientTopComponent_Name=In-Situ Data Access"})
public class InsituClientTopComponent extends TopComponent implements HelpCtx.Provider {

    private static final String HELP_ID = "insituClientTool";

    public InsituClientTopComponent() {
        setName(Bundle.CTL_InsituClientTopComponent_Name());
        InsituClientForm icf = new InsituClientForm(getHelpCtx());
        setLayout(new BorderLayout());
        add(icf, BorderLayout.CENTER);
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(HELP_ID);
    }


}