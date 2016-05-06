/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s3tbx.olci.radiometry.ui;

import org.esa.snap.core.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.AppContext;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;

/**
 * @author muhammad.bc.
 */

@ActionID(category = "Processing", id = "org.esa.s3tbx.olci.radiometry.ui.SmileCorrectionAction")
@ActionRegistration(displayName = "#CTL_OLCI_Smile_Correction_Text")
@ActionReference(path = "Menu/Optical/Preprocessing", position = 400)
@NbBundle.Messages({"CTL_OLCI_Smile_Correction_Text=OLCI Smile Correction..."})

public class SmileCorrectionAction extends AbstractSnapAction {

    public static final String OLCI_CORRECT_RADIOMETRY_ALIAS = "Olci.CorrectRadiometry";
    public static final String OLCI_HELP_ID = "olci_help_ID";

    @Override
    public void actionPerformed(ActionEvent e) {
        AppContext appContext = getAppContext();
        DefaultSingleTargetProductDialog dialog = new DefaultSingleTargetProductDialog(OLCI_CORRECT_RADIOMETRY_ALIAS,
                appContext,
                Bundle.CTL_OLCI_Smile_Correction_Text(),
                OLCI_HELP_ID);

        dialog.getJDialog().pack();
        dialog.show();
    }
}
