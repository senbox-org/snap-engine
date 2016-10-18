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

package org.esa.s3tbx.arc.ui;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.ui.DefaultIOParametersPanel;
import org.esa.snap.core.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.snap.core.gpf.ui.SourceProductSelector;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.esa.s3tbx.arc.ArcFiles;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

@ActionID(category = "Processing", id = "org.esa.snap.arc.ui.ArcAction" )
@ActionRegistration(displayName = "#CTL_ArcActionText")
@ActionReference(path = "Menu/Optical/Thematic Water Processing", position = 110 )
@NbBundle.Messages({"CTL_ArcActionText=ARC SST Processor"})
public class ArcAction extends AbstractSnapAction {

    public static final String HELP_ID = "arcScientificTool";
    private HelpCtx helpCtx;

    public ArcAction() {
        helpCtx = new HelpCtx(HELP_ID);
    }

    @Override
    public void actionPerformed(ActionEvent event) {

        final ArcSingleTargetProductDialog dialog = new ArcSingleTargetProductDialog();

        final BindingContext bindingContext = dialog.getBindingContext();
        bindingContext.bindEnabledState("asdiCoefficientsFile", true, "asdi", true);
        bindingContext.bindEnabledState("asdiMaskExpression", true, "asdi", true);
        bindingContext.bindEnabledState("dual", true, "nadir", true);
        bindingContext.bindEnabledState("dualCoefficientsFile", true, "dual", true);
        bindingContext.bindEnabledState("dualMaskExpression", true, "dual", true);
        bindingContext.bindEnabledState("nadir", true, "dual", true);
        bindingContext.bindEnabledState("nadirCoefficientsFile", true, "nadir", true);
        bindingContext.bindEnabledState("nadirMaskExpression", true, "nadir", true);

        ArrayList<SourceProductSelector> selectorList = dialog.getDefaultIOParametersPanel().getSourceProductSelectorList();
        if (!selectorList.isEmpty()) {
            final SourceProductSelector sourceProductSelector = selectorList.get(0);
            sourceProductSelector.addSelectionChangeListener(new AbstractSelectionChangeListener() {
                @Override
                public void selectionChanged(SelectionChangeEvent event) {
                    final Product selectedProduct = (Product) event.getSelection().getSelectedValue();
                    final PropertySet propertySet = bindingContext.getPropertySet();
                    if (selectedProduct != null) try {
                        final Presets preset = Presets.valueOf(selectedProduct.getProductType());
                        propertySet.setValue("asdiMaskExpression", preset.getMask());
                        propertySet.setValue("nadirMaskExpression", preset.getMask());
                        propertySet.setValue("dualMaskExpression", preset.getMask());
                        propertySet.setValue("asdi", preset.isAtsr());
                        propertySet.setValue("asdiCoefficientsFile", preset.getAsdiCoef());
                        propertySet.setValue("nadirCoefficientsFile", preset.getNadirCoef());
                        propertySet.setValue("dualCoefficientsFile", preset.getDualCoef());
                        bindingContext.setComponentsEnabled("tcwvExpression", preset.isAtsr());
                    } catch (IllegalArgumentException | NullPointerException ex) {
                        // Unrecognised product type
                        // Need to catch null pointer as enable components fails when dialog is first created
                    }
                }
            });
        }
        dialog.setTargetProductNameSuffix("_arc");
        dialog.getJDialog().pack();
        dialog.show();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return helpCtx;
    }

    private class ArcSingleTargetProductDialog extends DefaultSingleTargetProductDialog {

        public ArcSingleTargetProductDialog() {
            super("Arc.SST", ArcAction.this.getAppContext(), Bundle.CTL_ArcActionText(), ArcAction.HELP_ID);
        }

        @Override
        public DefaultIOParametersPanel getDefaultIOParametersPanel() {
            return super.getDefaultIOParametersPanel();
        }
    }

    private enum Presets {
        AT1_TOA_1P(true, "!cloud_flags_nadir.LAND", ArcFiles.ASDI_ATSR1, ArcFiles.ARC_N2_ATSR1, ArcFiles.ARC_D2_ATSR1),
        AT2_TOA_1P(true, "!cloud_flags_nadir.LAND", ArcFiles.ASDI_ATSR2, ArcFiles.ARC_N2_ATSR2, ArcFiles.ARC_D2_ATSR2),
        ATS_TOA_1P(true, "!cloud_flags_nadir.LAND", ArcFiles.ASDI_AATSR, ArcFiles.ARC_N2_AATSR, ArcFiles.ARC_D2_AATSR),
        SL_1_RBT(false, "confidence_in_ocean", ArcFiles.ASDI_AATSR, ArcFiles.ARC_N2_SLSTR, ArcFiles.ARC_D2_SLSTR);

        private final boolean atsr;
        private final String mask;
        private final ArcFiles asdiCoef;
        private final ArcFiles nadirCoef;
        private final ArcFiles dualCoef;

        private Presets(boolean atsr, String mask, ArcFiles asdiCoef, ArcFiles nadirCoef, ArcFiles dualCoef) {
            this.atsr = atsr;
            this.mask = mask;
            this.asdiCoef = asdiCoef;
            this.nadirCoef = nadirCoef;
            this.dualCoef = dualCoef;
        }

        boolean isAtsr() { return this.atsr; }
        String getMask() { return this.mask; }
        ArcFiles getAsdiCoef() { return this.asdiCoef; }
        ArcFiles getNadirCoef() { return this.nadirCoef; }
        ArcFiles getDualCoef() { return this.dualCoef; }
    }

}
