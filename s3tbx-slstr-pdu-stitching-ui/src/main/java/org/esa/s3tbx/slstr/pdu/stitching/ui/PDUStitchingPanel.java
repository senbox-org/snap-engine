package org.esa.s3tbx.slstr.pdu.stitching.ui;

import com.bc.ceres.swing.TableLayout;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.io.FileArrayEditor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.io.File;
import java.util.regex.Pattern;

/**
 * @author Tonio Fincke
 */
class PDUStitchingPanel extends JPanel {

    private static final String INPUT_PRODUCT_DIR_KEY = "gpf.slstr.pdu.stitching.input.product.dir";

    private final AppContext appContext;
    private final FileArrayEditor fileArrayEditor;
    private final PDUStitchingModel model;

    PDUStitchingPanel(AppContext appContext, PDUStitchingModel model) {
        this.appContext = appContext;
        this.model = model;
        final FileArrayEditor.EditorParent context = new FileArrayEditorContext(appContext);
        fileArrayEditor = new FileArrayEditor(context, "Source files") {
            @Override
            protected JFileChooser createFileChooserDialog() {
                final JFileChooser fileChooser = super.createFileChooserDialog();
                fileChooser.addChoosableFileFilter(new SlstrL1BFileFilter());
                fileChooser.setDialogTitle("SLSTR L1B PDU Stitching - Product Dissemination Units");
                return fileChooser;
            }
        };
        setLayout(new BorderLayout());
        add(createSourceProductsPanel(), BorderLayout.CENTER);
        add(createTargetDirPanel(), BorderLayout.SOUTH);
    }

    private JPanel createSourceProductsPanel() {
        final FileArrayEditor.FileArrayEditorListener listener = files -> {
            final SwingWorker worker = new SwingWorker() {
                @Override
                protected Object doInBackground() throws Exception {
                    String[] fileNames = new String[files.length];
                    for (int i = 0; i < files.length; i++) {
                        fileNames[i] = files[i].getAbsolutePath();
                    }
                    model.setPropertyValue(PDUStitchingModel.PROPERTY_SOURCE_PRODUCT_PATHS, fileNames);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                    } catch (Exception e) {
                        final String msg = String.format("Cannot display source product files.\n%s", e.getMessage());
                        appContext.handleError(msg, e);
                    }
                }
            };
            worker.execute();
        };
        fileArrayEditor.setListener(listener);
        JButton addFileButton = fileArrayEditor.createAddFileButton();
        JButton removeFileButton = fileArrayEditor.createRemoveFileButton();
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);

        final JPanel sourceFilesPanel = new JPanel(tableLayout);
        sourceFilesPanel.setBorder(BorderFactory.createTitledBorder("SLSTR L1B Product Dissemination Units"));
        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(addFileButton);
        buttonPanel.add(removeFileButton);
        tableLayout.setRowPadding(0, new Insets(1, 4, 1, 4));
        sourceFilesPanel.add(buttonPanel);

        final JComponent fileArrayComponent = fileArrayEditor.createFileArrayComponent();
        tableLayout.setRowWeightY(1, 1.0);
        sourceFilesPanel.add(fileArrayComponent);

        return sourceFilesPanel;
    }

    private JPanel createTargetDirPanel() {
        final JPanel targetDirPanel = new JPanel(new BorderLayout(2, 2));
        targetDirPanel.setBorder(BorderFactory.createTitledBorder("Target Directory"));
        final JTextField textField = new JTextField();
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                model.setPropertyValue(PDUStitchingModel.PROPERTY_TARGET_DIR, new File(textField.getText()));
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                model.setPropertyValue(PDUStitchingModel.PROPERTY_TARGET_DIR, new File(textField.getText()));
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                model.setPropertyValue(PDUStitchingModel.PROPERTY_TARGET_DIR, new File(textField.getText()));
            }
        });
        textField.setText(SystemUtils.getUserHomeDir().getPath());
        targetDirPanel.add(textField, BorderLayout.CENTER);
        final JButton etcButton = new JButton("...");
        final Dimension size = new Dimension(26, 16);
        etcButton.setPreferredSize(size);
        etcButton.setMinimumSize(size);
        etcButton.addActionListener(e -> {
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setAcceptAllFileFilterUsed(false);
            File currentFile = (File) model.getPropertyValue(PDUStitchingModel.PROPERTY_TARGET_DIR);
            if (currentFile != null) {
                fileChooser.setSelectedFile(currentFile);
            }
            int i = fileChooser.showDialog(targetDirPanel, "Select");
            final File selectedFile = fileChooser.getSelectedFile();
            if (i == JFileChooser.APPROVE_OPTION && selectedFile != null) {
                textField.setText(selectedFile.getAbsolutePath());
            }
        });
        targetDirPanel.add(etcButton, BorderLayout.EAST);
        return targetDirPanel;
    }

    private static class SlstrL1BFileFilter extends FileFilter {

        private final Pattern directoryNamePattern;

        SlstrL1BFileFilter() {
            directoryNamePattern = Pattern.compile("S3.?_SL_1_RBT_.*(.SEN3)?");
        }

        @Override
        public boolean accept(File f) {
            return (f.getName().equals("xdumanifest.xml") && isValidDirectoryName(f.getParentFile().getName())) ||
                    (isValidDirectoryName(f.getName()) && new File(f, "xfdumanifest.xml").exists());
        }

        @Override
        public String getDescription() {
            return "Slstr L1B";
        }

        private boolean isValidDirectoryName(String name) {
            return directoryNamePattern.matcher(name).matches();
        }
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
            applicationContext.getPreferences().setPropertyString(INPUT_PRODUCT_DIR_KEY, currentDirectory.getAbsolutePath());
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