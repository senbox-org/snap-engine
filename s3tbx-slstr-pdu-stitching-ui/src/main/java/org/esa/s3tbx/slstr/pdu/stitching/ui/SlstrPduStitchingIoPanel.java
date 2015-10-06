package org.esa.s3tbx.slstr.pdu.stitching.ui;

import com.bc.ceres.swing.TableLayout;
import org.esa.snap.core.gpf.ui.TargetProductSelector;
import org.esa.snap.framework.dataio.ProductIO;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.io.FileArrayEditor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Tonio Fincke
 */
public class SlstrPduStitchingIoPanel extends JPanel {

    private static final String INPUT_PRODUCT_DIR_KEY = "gpf.mosaic.input.product.dir";
    private static final String SLSTR_L1B_NAME_PATTERN = "S3.?_SL_1_RBT_.*(.SEN3)?";

    private final TargetProductSelector targetProductSelector;
    private FileArrayEditor sourceFileEditor;

    SlstrPduStitchingIoPanel(AppContext appContext, TargetProductSelector targetProductSelector) {
        final FileArrayEditor.EditorParent context = new FileArrayEditorContext(appContext);
        sourceFileEditor = new FileArrayEditor(context, "Source products") {
            @Override
            protected JFileChooser createFileChooserDialog() {
                final JFileChooser fileChooser = super.createFileChooserDialog();
                final Pattern slstrNamePattern = Pattern.compile(SLSTR_L1B_NAME_PATTERN);
                fileChooser.setDialogTitle("Slstr L1B PDU Stitching - Add Source Product(s)");
                fileChooser.setFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        final File parentFile = f.getParentFile();
                        return f.isDirectory() || (parentFile != null && f.getName().equals("xfdumanifest.xml") &&
                                slstrNamePattern.matcher(parentFile.getName()).matches());
                    }

                    @Override
                    public String getDescription() {
                        return "Sentinel-3 SLSTR L1B";
                    }
                });
                return fileChooser;
            }
        };
        this.targetProductSelector = targetProductSelector;
        init();
    }

    Product[] getSourceProducts() {
        final List<File> fileList = sourceFileEditor.getFiles();
        final Product[] sourceProducts = new Product[fileList.size()];
        for (int i = 0; i < fileList.size(); i++) {
            final File file = fileList.get(i);
            try {
                sourceProducts[i] = ProductIO.readProduct(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sourceProducts;
    }

    private void init() {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setTablePadding(3, 3);
        setLayout(tableLayout);
        tableLayout.setRowWeightY(0, 1.0);
        add(createSourceProductsPanel());
        add(createTargetProductPanel());
    }

    private JPanel createSourceProductsPanel() {
        JButton addFileButton = sourceFileEditor.createAddFileButton();
        JButton removeFileButton = sourceFileEditor.createRemoveFileButton();
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);

        final JPanel sourceProductPanel = new JPanel(tableLayout);
        sourceProductPanel.setBorder(BorderFactory.createTitledBorder("Source Products"));
        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(addFileButton);
        buttonPanel.add(removeFileButton);
        tableLayout.setRowPadding(0, new Insets(1, 4, 1, 4));
        sourceProductPanel.add(buttonPanel);

        final JComponent fileArrayComponent = sourceFileEditor.createFileArrayComponent();
        tableLayout.setRowWeightY(1, 1.0);
        sourceProductPanel.add(fileArrayComponent);

        return sourceProductPanel;
    }

    private JPanel createTargetProductPanel() {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(1.0);
        tableLayout.setTablePadding(3, 3);

        final JPanel targetProductPanel = new JPanel(tableLayout);
        targetProductPanel.setBorder(BorderFactory.createTitledBorder("Target Product"));
        targetProductPanel.add(createTargetProductSelectorPanel(targetProductSelector));
        targetProductPanel.add(targetProductSelector.getOpenInAppCheckBox());
        return targetProductPanel;
    }

    private static JPanel createTargetProductSelectorPanel(final TargetProductSelector selector) {
        final JPanel subPanel3 = new JPanel(new BorderLayout(3, 3));
        subPanel3.add(selector.getProductDirLabel(), BorderLayout.NORTH);
        subPanel3.add(selector.getProductDirTextField(), BorderLayout.CENTER);
        subPanel3.add(selector.getProductDirChooserButton(), BorderLayout.EAST);

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableWeightX(1.0);

        tableLayout.setCellPadding(0, 0, new Insets(3, 3, 3, 3));
        tableLayout.setCellPadding(1, 0, new Insets(3, 0, 3, 3));
        tableLayout.setCellPadding(2, 0, new Insets(0, 21, 3, 3));

        final JPanel panel = new JPanel(tableLayout);
        panel.add(subPanel3);

        return panel;
    }

    private static class FileArrayEditorContext implements FileArrayEditor.EditorParent {

        private final AppContext applicationContext;

        private FileArrayEditorContext(AppContext applicationContext) {

            this.applicationContext = applicationContext;
        }

        @Override
        public File getUserInputDir() {
            return getInputProductDir();
        }

        @Override
        public void setUserInputDir(File newDir) {
            setInputProductDir(newDir);
        }

        private void setInputProductDir(final File currentDirectory) {
            applicationContext.getPreferences().setPropertyString(INPUT_PRODUCT_DIR_KEY,
                                                                  currentDirectory.getAbsolutePath());
        }

        private File getInputProductDir() {
            final String path = applicationContext.getPreferences().getPropertyString(INPUT_PRODUCT_DIR_KEY);
            final File inputProductDir;
            if (path != null) {
                inputProductDir = new File(path);
            } else {
                inputProductDir = null;
            }
            return inputProductDir;
        }

    }

}
