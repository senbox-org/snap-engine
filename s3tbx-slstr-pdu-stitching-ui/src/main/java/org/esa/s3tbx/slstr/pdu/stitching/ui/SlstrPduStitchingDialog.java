package org.esa.s3tbx.slstr.pdu.stitching.ui;

import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.ui.OperatorMenu;
import org.esa.snap.core.gpf.ui.OperatorParameterSupport;
import org.esa.snap.core.gpf.ui.SingleTargetProductDialog;
import org.esa.snap.core.gpf.ui.TargetProductSelector;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.ui.AppContext;

/**
 * @author Tonio Fincke
 */
public class SlstrPduStitchingDialog extends SingleTargetProductDialog {

    private final SlstrPduStitchingIoPanel ioPanel;

    SlstrPduStitchingDialog(final String title, final String helpID, AppContext appContext) {
        super(appContext, title, ID_APPLY_CLOSE, helpID);
        final TargetProductSelector selector = getTargetProductSelector();
        selector.getModel().setSaveToFileSelected(true);
        selector.getModel().setProductName("bdgf");
        selector.getSaveToFileCheckBox().setEnabled(false);

        final OperatorSpi operatorSpi =
                GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi("Slstr.PDU.Stitching");

        final TargetProductSelector targetProductSelector = new TargetProductSelector();
        ioPanel = new SlstrPduStitchingIoPanel(appContext, targetProductSelector);
        setContent(ioPanel);

        OperatorMenu operatorMenu = new OperatorMenu(this.getJDialog(),
                                                     operatorSpi.getOperatorDescriptor(),
                                                     new OperatorParameterSupport(operatorSpi.getOperatorDescriptor()),
                                                     appContext,
                                                     helpID);
        getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        return GPF.createProduct("Slstr.PDU.Stitching", null, ioPanel.getSourceProducts());
    }
}
