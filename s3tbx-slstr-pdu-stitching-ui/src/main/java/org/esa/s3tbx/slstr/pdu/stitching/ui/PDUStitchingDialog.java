package org.esa.s3tbx.slstr.pdu.stitching.ui;

import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.ui.OperatorMenu;
import org.esa.snap.core.gpf.ui.OperatorParameterSupport;
import org.esa.snap.core.util.ArrayUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModelessDialog;

import javax.swing.AbstractButton;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Tonio Fincke
 */
public class PDUStitchingDialog extends ModelessDialog {

    private final PDUStitchingModel formModel;

    public PDUStitchingDialog(final String title, AppContext appContext, final String helpID) {
        super(appContext.getApplicationWindow(), title, ID_APPLY_CLOSE, helpID);

        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi("PduStitching");
        formModel = new PDUStitchingModel();
        OperatorParameterSupport parameterSupport = new OperatorParameterSupport(operatorSpi.getOperatorDescriptor(),
                                                                                 formModel.getPropertySet(),
                                                                                 formModel.getParameterMap(),
                                                                                 null);

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
            String[] before = new String[0];
            final File targetDir = (File) formModel.getPropertyValue(PDUStitchingModel.PROPERTY_TARGET_DIR);
            if (formModel.openInApp()) {
                if (targetDir.exists()) {
                    before = targetDir.list();
                }
            }
            final ProductReaderPlugIn sen3ReaderPlugIn = getSentinel3ReaderPlugin();
            final Map<String, Object> parameterMap = formModel.getParameterMap();
            final Product[] sourceProducts = formModel.getSourceProducts();
            GPF.createProduct("PduStitching", parameterMap, sourceProducts);
            if (formModel.openInApp()) {
                final String[] after = targetDir.list();
                for (String inTargetDir : after) {
                    if (!ArrayUtils.isMemberOf(inTargetDir, before)) {
                        try {
                            final ProductReader reader = sen3ReaderPlugIn.createReaderInstance();
                            final Product product = reader.readProductNodes(new File(targetDir, inTargetDir), null);
                            SnapApp.getDefault().getProductManager().addProduct(product);
                        } catch (IOException e) {
                            Dialogs.showError("Could not open stitched product " + inTargetDir + ": " + e.getMessage());
                        }
                        break;
                    }
                }

            }
        } catch (OperatorException e) {
            Dialogs.showInformation("SLSTR L1B PDU Stitching", "Could not create stitched SLSTR L1B product: " + e.getMessage(), null);
            return;
        }
        Dialogs.showInformation("SLSTR L1B PDU Stitching",
                                "Stitched SLSTR L1B product has been successfully created in the target directory.", null);
    }

    private ProductReaderPlugIn getSentinel3ReaderPlugin() {
        final ProductIOPlugInManager ioPlugInManager = ProductIOPlugInManager.getInstance();
        final Iterator<ProductReaderPlugIn> sen3ReaderPlugins = ioPlugInManager.getReaderPlugIns("Sen3");
        if(!sen3ReaderPlugins.hasNext()) {
            throw new IllegalStateException("No appropriate reader for reading Sentinel-3 products found");
        }
        return sen3ReaderPlugins.next();
    }

}
