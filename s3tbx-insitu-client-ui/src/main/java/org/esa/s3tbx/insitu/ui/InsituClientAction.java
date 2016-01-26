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

import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.rcp.util.Dialogs;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;

@ActionID(category = "Tools", id = "org.esa.s3tbx.insitu.ui.InsituClientAction" )
@ActionRegistration(displayName = "#CTL_InsituClientAction_Text")
@ActionReference(path = "Menu/Vector/Import", position = 0)
@NbBundle.Messages({"CTL_InsituClientAction_Text=In-Situ Client"})
public class InsituClientAction extends AbstractSnapAction {

    public InsituClientAction() {
        putValue(SHORT_DESCRIPTION, "Obtain In-Situ data from a web service");
        putValue(HELP_ID, "insituClientTool");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Dialogs.showMessage(Bundle.CTL_InsituClientAction_Text(), "Still to be implemented!", JOptionPane.INFORMATION_MESSAGE, null);
    }

}
