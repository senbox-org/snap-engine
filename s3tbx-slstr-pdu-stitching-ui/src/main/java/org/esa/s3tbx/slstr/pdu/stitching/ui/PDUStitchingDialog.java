package org.esa.s3tbx.slstr.pdu.stitching.ui;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.ui.OperatorMenu;
import org.esa.snap.core.gpf.ui.OperatorParameterSupport;
import org.esa.snap.core.gpf.ui.ParameterUpdater;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModelessDialog;

import javax.swing.AbstractButton;
import java.util.Map;

/**
 * @author Tonio Fincke
 */
public class PDUStitchingDialog extends ModelessDialog {

    private final PDUStitchingModel formModel;

    public PDUStitchingDialog(final String title, final String helpID, AppContext appContext) {
        super(appContext.getApplicationWindow(), title, ID_APPLY_CLOSE, helpID);

        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi("PduStitching");
        formModel = new PDUStitchingModel();
        OperatorParameterSupport parameterSupport = new OperatorParameterSupport(operatorSpi.getOperatorDescriptor(),
                                                                                 formModel.getPropertySet(),
                                                                                 formModel.getParameterMap(),
                                                                                 new StitchingParametersUpdater());

        OperatorMenu operatorMenu = new OperatorMenu(this.getJDialog(),
                                                     operatorSpi.getOperatorDescriptor(),
                                                     parameterSupport,
                                                     appContext,
                                                     helpID);
        getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());
        AbstractButton button = getButton(ID_APPLY);
        button.setText("Run");
        button.setMnemonic('R');
        setContent(new PDUStitchingPanel(appContext, formModel));
    }

    @Override
    protected void onApply() {
        try {
            GPF.createProduct("PduStitching", formModel.getParameterMap());
        } catch (OperatorException e) {
            Dialogs.showInformation("SLSTR L1B PDU Stitching", "Could not create stitched SLSTR L1B product", null);
            return;
        }
        Dialogs.showInformation("SLSTR L1B PDU Stitching",
                                "Stitched SLSTR L1B product has been successfully created in the target directory.", null);
    }

    private class StitchingParametersUpdater implements ParameterUpdater {

        @Override
        public void handleParameterSaveRequest(Map<String, Object> parameterMap) throws ValidationException, ConversionException {
        }

        @Override
        public void handleParameterLoadRequest(Map<String, Object> parameterMap) throws ValidationException, ConversionException {
            if (parameterMap.containsKey(PDUStitchingModel.PROPERTY_SOURCE_PRODUCT_PATHS)) {
                formModel.setPropertyValue(PDUStitchingModel.PROPERTY_SOURCE_PRODUCT_PATHS,
                                           parameterMap.get(PDUStitchingModel.PROPERTY_SOURCE_PRODUCT_PATHS));
            }
            if (parameterMap.containsKey(PDUStitchingModel.PROPERTY_TARGET_DIR)) {
                formModel.setPropertyValue(PDUStitchingModel.PROPERTY_TARGET_DIR,
                                           parameterMap.get(PDUStitchingModel.PROPERTY_TARGET_DIR));
            }
        }
    }

}
