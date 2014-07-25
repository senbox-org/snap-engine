package org.esa.beam.dataio.s3;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.ui.ModalDialog;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;

/**
 * Only used to pop-up a reader configuration dialog mock-up.
 * @author Norman Fomferra
 */
public class MockUpSentinel3ProductReader extends AbstractProductReader {

    public MockUpSentinel3ProductReader(MockUpSentinel3ProductReaderPlugIn readerPlugInMockUp) {
        super(readerPlugInMockUp);
    }

    @Override
    public Product readProductNodes(Object input, ProductSubsetDef subsetDef) throws IOException {
        ModalDialog modalDialog = new ModalDialog(null, "Sentinel-3 SLSTR L1B Reader Options", ModalDialog.ID_OK_CANCEL_HELP, "");
        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(new EmptyBorder(6, 6, 6, 6));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets.top = 2;
        constraints.insets.bottom = 2;
        constraints.insets.left = 2;
        constraints.insets.right = 2;
        constraints.gridx = 0;
        constraints.gridy = -1;

        constraints.gridy++;
        content.add(new JLabel("This data product contains images in various spatial resolutions."), constraints);
        constraints.insets.top = 0;
        constraints.gridy++;
        content.add(new JLabel("Please specify how to treat them:"), constraints);

        constraints.insets.top = 12;
        constraints.gridy++;
        content.add(new JRadioButton("Read a single data product and make all bands have the same resolution:", true), constraints);
        constraints.insets.top = 0;
        constraints.insets.left = 24;

        constraints.gridy++;
        content.add(new JRadioButton("Scale to 500m (duplicate 1km pixels)", true), constraints);
        constraints.gridy++;
        content.add(new JRadioButton("Scale to 1km (aggregate 500m pixels))", false), constraints);

        constraints.gridy++;
        constraints.insets.top = 12;
        constraints.insets.left = 2;
        content.add(new JRadioButton("Read as groups of bands with different resolutions:", false), constraints);
        constraints.insets.top = 2;
        constraints.insets.left = 24;

        constraints.insets.top = 0;
        constraints.gridy++;
        JCheckBox checkBox10 = new JCheckBox("Read 500m bands (S1-S6)", true);
        checkBox10.setEnabled(false);
        content.add(checkBox10, constraints);
        constraints.gridy++;
        JCheckBox checkBox20 = new JCheckBox("Read 1km bands (S7-S9, F1 and F2)", true);
        checkBox20.setEnabled(false);
        content.add(checkBox20, constraints);

        constraints.gridy++;
        constraints.insets.left = 2;
        constraints.insets.top = 12;
        content.add(new JCheckBox("Always use the above settings and don't ask again."), constraints);

        constraints.gridy++;
        constraints.insets.left = 24;
        constraints.insets.top = 0;
        content.add(new JLabel("<html><small>Note that you can always change settings later in the preferences dialog.</small></html>"), constraints);

        modalDialog.setContent(content);
        final int show = modalDialog.show();

        if (show == ModalDialog.ID_OK) {
            return super.readProductNodes(input, subsetDef);
        } else {
            return null;
        }
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        return new Product("S3", "S3", 16, 16);
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
    }
}
