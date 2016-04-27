package org.esa.s3tbx.slstr.pdu.stitching.ui;

import org.esa.s3tbx.slstr.pdu.stitching.PDUStitchingOp;
import org.esa.s3tbx.slstr.pdu.stitching.Validator;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductFilter;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.product.SourceProductList;

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
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
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
    private final PDUStitchingModel model;
    private JLabel statusLabel;
    private final Pattern directoryNamePattern;
    private boolean isReactingToChange;
    private SourceProductList sourceProductList;

    PDUStitchingPanel(AppContext appContext, PDUStitchingModel model) {
        this.appContext = appContext;
        this.model = model;
        isReactingToChange = false;
        directoryNamePattern = Pattern.compile("S3.?_SL_1_RBT_.*(.SEN3)?");
        setLayout(new BorderLayout());
        add(createSourceProductsPanel(), BorderLayout.CENTER);
        add(createTargetDirPanel(), BorderLayout.SOUTH);
    }

    private JPanel createSourceProductsPanel() {
        ListDataListener changeListener = new ListDataListener() {

            @Override
            public void contentsChanged(ListDataEvent event) {
                final SwingWorker worker = new SwingWorker() {
                    @Override
                    protected Object doInBackground() throws Exception {
                        if (!isReactingToChange) {
                            isReactingToChange = true;
                            final Product[] sourceProducts = sourceProductList.getSourceProducts();
                            final String[] filePaths = (String[]) model.getPropertyValue(PDUStitchingModel.PROPERTY_SOURCE_PRODUCT_PATHS);
                            File[] productFiles = new File[sourceProducts.length];
                            for (int i = 0; i < sourceProducts.length; i++) {
                                productFiles[i] = sourceProducts[i].getFileLocation();
                            }
                            final Logger logger = Logger.getLogger(PDUStitchingPanel.class.getName());
                            final Set<File> dissolvedFilePaths = PDUStitchingOp.getSourceProductsPathFileSet(filePaths, logger);
                            File[] pathFiles = dissolvedFilePaths.toArray(new File[dissolvedFilePaths.size()]);
                            Product[] validatedProducts = new Product[0];
                            String[] validatedPaths = new String[0];
                            if (productFiles.length == 0 && pathFiles.length == 0) {
                                statusLabel.setForeground(Color.BLACK);
                                statusLabel.setText(NO_SOURCE_PRODUCTS_TEXT);
                            } else {
                                List<String> validatedFileNamesList = new ArrayList<>();
                                List<Product> validatedProductsList = new ArrayList<>();
                                List<String> validatedPathsList = new ArrayList<>();
                                for (int i = 0; i < productFiles.length; i++) {
                                    final String fileName = productFiles[i].getAbsolutePath();
                                    if (validatedFileNamesList.contains(fileName)) {
                                        Dialogs.showInformation("Removed duplicate occurence of " + fileName + " from selection.");
                                    } else if (!isValidSlstrL1BFile(productFiles[i])) {
                                        Dialogs.showInformation(fileName + " is not a valid SLSTR L1B product. Removed from selection.");
                                    } else {
                                        validatedFileNamesList.add(fileName);
                                        validatedProductsList.add(sourceProducts[i]);
                                    }
                                }
                                for (File file : pathFiles) {
                                    final String fileName = file.getAbsolutePath();
                                    if (validatedFileNamesList.contains(fileName)) {
                                        Dialogs.showInformation("Removed duplicate occurence of " + fileName + " from selection.");
                                    } else if (!isValidSlstrL1BFile(file)) {
                                        Dialogs.showInformation(fileName + " is not a valid SLSTR L1B product. Removed from selection.");
                                    } else {
                                        validatedFileNamesList.add(fileName);
                                        validatedPathsList.add(fileName);
                                    }
                                }
                                if (validatedProductsList.size() > 0) {
                                    validatedProducts = validatedProductsList.toArray(new Product[validatedProductsList.size()]);
                                }
                                if (validatedPathsList.size() > 0) {
                                    validatedPaths = validatedPathsList.toArray(new String[validatedPathsList.size()]);
                                }
                                try {
                                    Validator.validate(productFiles);
                                    Validator.validate(pathFiles);
                                    statusLabel.setForeground(Color.GREEN.darker());
                                    statusLabel.setText(VALID_SOURCE_PRODUCTS_TEXT);
                                } catch (IOException e) {
                                    statusLabel.setForeground(Color.RED);
                                    statusLabel.setText(INVALID_SELECTION_TEXT + e.getMessage());
                                }
                            }
                            model.setPropertyValue(PDUStitchingModel.PROPERTY_SOURCE_PRODUCTS, validatedProducts);
                            if (validatedPaths.length != filePaths.length) {
                                model.setPropertyValue(PDUStitchingModel.PROPERTY_SOURCE_PRODUCT_PATHS, validatedPaths);
                            }
                            sourceProductList.bindComponents();
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
            }

            @Override
            public void intervalAdded(ListDataEvent e) {
                contentsChanged(e);
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                contentsChanged(e);
            }
        };

        sourceProductList = new SourceProductList(appContext);
        sourceProductList.setPropertyNameLastOpenInputDir(INPUT_PRODUCT_DIR_KEY);
        sourceProductList.setPropertyNameLastOpenedFormat("Sen3");
        sourceProductList.addChangeListener(changeListener);
        sourceProductList.setXAxis(false);
        sourceProductList.setExplicitPattern("xfdumanifest.xml");
        sourceProductList.setProductFilter(new ProductFilter() {
            @Override
            public boolean accept(Product product) {
                return isValidDirectoryName(product.getName());
            }
        });

        model.getBindingContext().bind(PDUStitchingModel.PROPERTY_SOURCE_PRODUCT_PATHS, sourceProductList);
        JComponent[] panels = sourceProductList.getComponents();

        final JPanel sourceProductPanel = new JPanel(new BorderLayout());
        sourceProductPanel.add(panels[0], BorderLayout.CENTER);
        sourceProductPanel.add(panels[1], BorderLayout.EAST);

        statusLabel = new JLabel(NO_SOURCE_PRODUCTS_TEXT);
        sourceProductPanel.add(statusLabel, BorderLayout.SOUTH);

        return sourceProductPanel;
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

    private boolean isValidSlstrL1BFile(File f) {
        return (f.getName().equals("xfdumanifest.xml") && isValidDirectoryName(f.getParentFile().getName()) ||
                f.isDirectory());
    }

    private boolean isValidDirectoryName(String name) {
        return directoryNamePattern.matcher(name).matches();
    }

}