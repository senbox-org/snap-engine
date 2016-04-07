package org.esa.s3tbx.slstr.pdu.stitching.ui;

import com.bc.ceres.swing.TableLayout;
import org.esa.s3tbx.slstr.pdu.stitching.Validator;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.io.FileArrayEditor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Tonio Fincke
 */
class PDUStitchingPanel extends JPanel {

    private static final String INPUT_PRODUCT_DIR_KEY = "gpf.slstr.pdu.stitching.input.product.dir";
    private static final String NO_SOURCE_PRODUCTS_TEXT = "No Product Dissemination Units selected";
    private static final String VALID_SOURCE_PRODUCTS_TEXT = "Selection of Product Dissemination Units is valid";
    private static final String INVALID_SELECTION_TEXT = "Selection of Product Dissemination Units is invalid: ";

    private final AppContext appContext;
    private final FileArrayEditor fileArrayEditor;
    private final PDUStitchingModel model;
    private JLabel statusLabel;
    private final Pattern directoryNamePattern;
    private boolean isReactingToChange;

    PDUStitchingPanel(AppContext appContext, PDUStitchingModel model) {
        this.appContext = appContext;
        this.model = model;
        isReactingToChange = false;
        directoryNamePattern = Pattern.compile("S3.?_SL_1_RBT_.*(.SEN3)?");
        final FileArrayEditor.EditorParent context = new FileArrayEditorContext(appContext);
        fileArrayEditor = new FileArrayEditor(context, "Source files") {
            @Override
            protected JFileChooser createFileChooserDialog() {
                final JFileChooser fileChooser = super.createFileChooserDialog();
                fileChooser.setAcceptAllFileFilterUsed(false);
                fileChooser.setFileFilter(new SlstrL1BFileFilter());
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
                    if (!isReactingToChange) {
                        isReactingToChange = true;
                        String[] fileNames;
                        if (files.length == 0) {
                            statusLabel.setForeground(Color.BLACK);
                            statusLabel.setText(NO_SOURCE_PRODUCTS_TEXT);
                            fileNames = new String[0];
                        } else {
                            List<File> fileList = new ArrayList<>();
                            List<String> fileNameList = new ArrayList<>();
                            for (File file : files) {
                                final String fileName = file.getAbsolutePath();
                                if (fileNameList.contains(fileName)) {
                                    Dialogs.showInformation("Removed duplicate occurence of " + fileName + " from selection.");
                                } else if (!isValidSlstrL1BFile(file)){
                                    Dialogs.showInformation(fileName + " is not a valid SLSTR L1B product. Removed from selection.");
                                } else {
                                    fileList.add(file);
                                    fileNameList.add(fileName);
                                }
                            }
                            if (files.length != fileList.size()) {
                                fileArrayEditor.setFiles(fileList);
                            }
                            fileNames = fileNameList.toArray(new String[fileNameList.size()]);
                            try {
                                Validator.validate(files);
                                statusLabel.setForeground(Color.GREEN.darker());
                                statusLabel.setText(VALID_SOURCE_PRODUCTS_TEXT);
                            } catch (IOException e) {
                                statusLabel.setForeground(Color.RED);
                                statusLabel.setText(INVALID_SELECTION_TEXT + e.getMessage());
                            }
                        }
                        model.setPropertyValue(PDUStitchingModel.PROPERTY_SOURCE_PRODUCT_PATHS, fileNames);
                        isReactingToChange = false;
                    }
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

        statusLabel = new JLabel(NO_SOURCE_PRODUCTS_TEXT);
        tableLayout.setRowWeightY(2, 0.0);
        sourceFilesPanel.add(statusLabel);

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
        final JCheckBox openInAppCheckBox = new JCheckBox("Open in " + appContext.getApplicationName());
        openInAppCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.setOpenInApp(openInAppCheckBox.isSelected());
            }
        });
        targetDirPanel.add(openInAppCheckBox, BorderLayout.SOUTH);
        return targetDirPanel;
    }

    boolean isValidSlstrL1BFile(File f) {
        return (f.getName().equals("xfdumanifest.xml") && isValidDirectoryName(f.getParentFile().getName()) ||
                f.isDirectory());
    }

    private boolean isValidDirectoryName(String name) {
        return directoryNamePattern.matcher(name).matches();
    }

    private class SlstrL1BFileFilter extends FileFilter {

        @Override
        public boolean accept(File f) {
            return isValidSlstrL1BFile(f);
        }

        @Override
        public String getDescription() {
            return "Slstr L1B manifest";
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