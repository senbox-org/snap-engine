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

import com.bc.ceres.swing.TableLayout;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.esa.s3tbx.insitu.server.InsituDataset;
import org.esa.s3tbx.insitu.server.InsituObservation;
import org.esa.s3tbx.insitu.server.InsituParameter;
import org.esa.s3tbx.insitu.server.InsituQuery;
import org.esa.s3tbx.insitu.server.InsituResponse;
import org.esa.s3tbx.insitu.server.InsituServer;
import org.esa.s3tbx.insitu.server.InsituServerException;
import org.esa.s3tbx.insitu.server.InsituServerRunnable;
import org.esa.s3tbx.insitu.server.InsituServerSpi;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.PlacemarkDescriptor;
import org.esa.snap.core.datamodel.PlacemarkDescriptorRegistry;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.help.HelpAction;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.tango.TangoIcons;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jdesktop.swingx.VerticalLayout;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

@TopComponent.Description(
        preferredID = "InsituClientTopComponent",
        iconBase = "org/esa/s3tbx/insitu/insitu.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "explorer", openAtStartup = false, position = 150)
@ActionID(category = "Window", id = "InsituClientTopComponent")
@ActionReference(path = "Menu/View/Tool Windows")
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_InsituClientTopComponent_Name",
        preferredID = "InsituClientTopComponent"
)
@NbBundle.Messages({"CTL_InsituClientTopComponent_Name=In-Situ Data Access"})
public class InsituClientTopComponent extends TopComponent implements HelpCtx.Provider {

    private static final String HELP_ID = "insituClientTool";
    private InsituClientModel insituModel;
    private AbstractButton refreshButton;

    public InsituClientTopComponent() {
        setName(Bundle.CTL_InsituClientTopComponent_Name());
    }

    private Component createStatusPanel(HelpCtx helpCtx) {
        final TableLayout layout = new TableLayout(4);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableWeightX(0.5);
        layout.setCellWeightX(0, 2, 2.0);
        layout.setCellWeightX(0, 3, 0.0);
        layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layout.setTablePadding(4, 4);

        final JPanel contentPanel = new JPanel(layout);
        refreshButton = ToolButtonFactory.createButton(TangoIcons.actions_view_refresh(TangoIcons.Res.R22), false);
        refreshButton.setName("refreshButton");
        refreshButton.addActionListener(new ServerButtonActionListener(() -> createObservationQuery().countOnly(true),
                                                                       response -> setNumObs(response.getObservationCount())));

        contentPanel.add(refreshButton);
        final AbstractButton downloadButton = ToolButtonFactory.createButton(TangoIcons.actions_document_save(TangoIcons.Res.R22), false);
        downloadButton.setText("Download");
        downloadButton.setName("downloadButton");
        downloadButton.addActionListener(new ServerButtonActionListener(this::createObservationQuery, new DownloadResponseHandler()));
        contentPanel.add(downloadButton);
        contentPanel.add(layout.createHorizontalSpacer());

        AbstractButton helpButton = ToolButtonFactory.createButton(new HelpAction(helpCtx), false);
        helpButton.setName("helpButton");
        contentPanel.add(helpButton);

        return contentPanel;
    }

    private FeatureCollection<SimpleFeatureType, SimpleFeature> getFeatureCollection(String datasetName,
                                                                                     InsituObservation observation,
                                                                                     SimpleFeatureType featureType,
                                                                                     TreeMap<String, FeatureCollection<SimpleFeatureType, SimpleFeature>> map) {
        String fcKey = datasetName + "_" + observation.getParam();
        FeatureCollection<SimpleFeatureType, SimpleFeature> fc = map.get(fcKey);
        if (fc == null) {
            fc = new ListFeatureCollection(featureType);
            map.put(fcKey, fc);
        }
        return fc;
    }

    private static SimpleFeatureType createInsituFeatureType(GeoCoding geoCoding) {
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.setName("org.esa.snap.Insitu");
        /*0*/
        ftb.add("parameter", String.class);
        /*1*/
        ftb.add("pixelPos", Point.class, geoCoding.getImageCRS());
        /*2*/
        ftb.add("geoPos", Point.class, DefaultGeographicCRS.WGS84);
        /*3*/
        ftb.add("date", Date.class);
        /*4*/
        ftb.add("value", Double.class);
        ftb.setDefaultGeometry(geoCoding instanceof CrsGeoCoding ? "geoPos" : "pixelPos");
        return ftb.buildFeatureType();
        // todo - Maybe later the user can decide if the observations shall be treated as track points
        // GeoTools Bug: this doesn't work
//        ftb.userData("trackPoints", "true");
//        ft.getUserData().put("trackPoints", "true");
//        return ft;
    }

    private static SimpleFeature createFeature(SimpleFeatureType type, GeoCoding geoCoding, int pointIndex, InsituObservation observation) {
        double lat = observation.getLat();
        double lon = observation.getLon();
        PixelPos pixelPos = geoCoding.getPixelPos(new GeoPos(lat, lon), null);
        if (!pixelPos.isValid()) {
            return null;
        }
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);
        GeometryFactory gf = new GeometryFactory();
        /*0*/
        fb.add(observation.getParam());
        /*1*/
        fb.add(gf.createPoint(new Coordinate(pixelPos.x, pixelPos.y)));
        /*2*/
        fb.add(gf.createPoint(new Coordinate(lon, lat)));
        /*3*/
        fb.add(observation.getDate());
        /*4*/
        fb.add(observation.getValue());
        return fb.buildFeature(String.format("ID%08d", pointIndex));
    }


    private InsituQuery createObservationQuery() {
        InsituQuery query = new InsituQuery();
        query.subject(InsituQuery.SUBJECT.OBSERVATIONS);
        query.latMin(insituModel.getMinLat()).latMax(insituModel.getMaxLat());
        query.lonMin(insituModel.getMinLon()).lonMax(insituModel.getMaxLon());
        query.startDate(insituModel.getStartDate()).stopDate(insituModel.getStopDate());
        String[] datasetNames = insituModel.getSelectedDatasetNames();
        if (datasetNames.length > 0) {
            query.datasets(datasetNames);
        }
        Stream<InsituParameter> stream = insituModel.getSelectedParameters().stream();
        String[] parameterNames = stream.map(InsituParameter::getName).toArray(String[]::new);
        query.param(parameterNames);
        return query;
    }

    private InsituServer getServer() {
        try {
            InsituServerSpi serverSpi = insituModel.getSelectedServerSpi();
            return serverSpi.createServer();
        } catch (InsituServerException ise) {
            SnapApp.getDefault().handleError("Could not contact server.", ise);
            return null;
        }
    }

    private void setNumObs(long observationCount) {
        if (observationCount < 0) {
            refreshButton.setText("#Obs: UNKNOWN");
        } else {
            refreshButton.setText("#Obs: " + observationCount);
        }
    }

    @Override
    protected void componentOpened() {
        super.componentOpened();
        insituModel = new InsituClientModel();
        setLayout(new BorderLayout());
        add(new InsituClientForm(insituModel), BorderLayout.CENTER);
        add(createStatusPanel(new HelpCtx(HELP_ID)), BorderLayout.SOUTH);
        setNumObs(-1);
    }

    @Override
    protected void componentClosed() {
        super.componentClosed();
        insituModel.dispose();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(HELP_ID);
    }


    private class ServerButtonActionListener implements ActionListener {

        private final QueryFactory factory;
        private final ResponseHandler handler;

        public ServerButtonActionListener(QueryFactory factory, ResponseHandler handler) {
            this.factory = factory;
            this.handler = handler;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            InsituServer server = getServer();
            if (server == null) {
                return;
            }
            InsituServerRunnable runnable = new InsituServerRunnable(server, factory.create());
            try {
                InsituServer.runWithProgress(runnable);
                handler.handle(runnable.getResponse());
            } catch (InsituServerException ise) {
                SnapApp.getDefault().handleError("Failed to contact server", ise);
            }
        }
    }

    @FunctionalInterface
    private interface QueryFactory {

        InsituQuery create();

    }

    @FunctionalInterface
    private interface ResponseHandler {

        void handle(InsituResponse response);

    }

    private class DownloadResponseHandler implements ResponseHandler {

        @Override
        public void handle(InsituResponse response) {
            if(!(response.getObservationCount() > 0 && response.getDatasets() != null)) {
                return;
            }
            if (InsituResponse.STATUS_CODE.OK.equals(response.getStatus())) {
                List<? extends InsituDataset> datasetList = response.getDatasets();
                JPanel content = new JPanel(new VerticalLayout(4));
                List<DataPolicyPanel> policyPanels = new ArrayList<>();
                for (InsituDataset insituDataset : datasetList) {
                    String policy = insituDataset.getPolicy();
                    if (StringUtils.isNotNullAndNotEmpty(policy)) {
                        DataPolicyPanel datasetPolicyPanel = new DataPolicyPanel(insituDataset);
                        content.add(datasetPolicyPanel);
                        policyPanels.add(datasetPolicyPanel);
                    }
                }
                if (!policyPanels.isEmpty()) {
                    ModalDialog policyDialog = new ModalDialog(null, "In-Situ Data", new JScrollPane(content), ModalDialog.ID_OK_CANCEL, null);
                    JPanel buttonPanel = policyDialog.getButtonPanel();
                    JCheckBox acceptAll = new JCheckBox("Accept All");
                    acceptAll.addActionListener(e -> {
                        for (DataPolicyPanel policyPanel : policyPanels) {
                            policyPanel.setAccepted(acceptAll.isSelected());
                        }
                    });
                    buttonPanel.add(acceptAll, 0);
                    JButton saveButton = new JButton("Save data policies");
                    saveButton.addActionListener(e -> {
                        File file = Dialogs.requestFileForSave("Save Data Policy File", false, null, ".txt", "insitu-policy", null,
                                                               "s3tbx.insitu.policyExport");
                        if (file == null) {
                            return;
                        }
                        Path path = file.toPath();
                        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(path), false)) {
                            for (InsituDataset insituDataset : datasetList) {
                                w.printf("Name: %s%n", insituDataset.getName());
                                w.printf("Pi: %s%n", insituDataset.getPi());
                                w.printf("Contact: %s%n", insituDataset.getContact());
                                w.printf("Website: %s%n", insituDataset.getWebsite());
                                w.printf("Data Policy: %s%n", insituDataset.getPolicy());
                                w.println();
                                w.flush();
                            }
                        } catch (IOException ioe) {
                            SnapApp.getDefault().handleError("Could not write data policy file.", ioe);
                        }
                    });
                    buttonPanel.add(saveButton, 0);
                    if (ModalDialog.ID_CANCEL == policyDialog.show()) {
                        return;
                    }

                    for (DataPolicyPanel policyPanel : policyPanels) {
                        if (!policyPanel.isAccepted()) {
                            Dialogs.showWarning("In-Situ Data", "Not all data policies have been accepted.", null);
                            return;
                        }
                    }
                }
            }

            List<Product> selectedProducts = insituModel.getSelectedProducts();
            List<? extends InsituDataset> datasetList = response.getDatasets();
            for (InsituDataset insituDataset : datasetList) {
                List<? extends InsituObservation> observations = insituDataset.getObservations();
                String datasetName = insituDataset.getName();
                for (Product product : selectedProducts) {
                    SimpleFeatureType featureType = createInsituFeatureType(product.getSceneGeoCoding());
                    TreeMap<String, FeatureCollection<SimpleFeatureType, SimpleFeature>> fcMap = new TreeMap<>();
                    for (int i = 0; i < observations.size(); i++) {
                        InsituObservation observation = observations.get(i);
                        SimpleFeature feature = createFeature(featureType, product.getSceneGeoCoding(), i, observation);
                        if (feature != null) {
                            FeatureCollection<SimpleFeatureType, SimpleFeature> fc = InsituClientTopComponent.this.getFeatureCollection(
                                    datasetName, observation, featureType, fcMap);
                            fc.add(feature);
                        }
                    }

                    for (Map.Entry<String, FeatureCollection<SimpleFeatureType, SimpleFeature>> entry : fcMap.entrySet()) {
                        FeatureCollection<SimpleFeatureType, SimpleFeature> fc = entry.getValue();
                        String name = entry.getKey();
                        final PlacemarkDescriptor placemarkDescriptor = PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptor(
                                fc.getSchema());
                        placemarkDescriptor.setUserDataOf(fc.getSchema());
                        ProductNodeGroup<VectorDataNode> vectorDataGroup = product.getVectorDataGroup();
                        String nodeName = ProductUtils.getAvailableNodeName(name, vectorDataGroup);
                        VectorDataNode vectorDataNode = new VectorDataNode(nodeName, fc, placemarkDescriptor);
                        vectorDataGroup.add(vectorDataNode);
                    }
                }

                // todo (mp/20160307) - Show message that download was successful.
            }
        }

    }
}