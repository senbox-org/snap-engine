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

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.TableLayout;
import org.esa.s3tbx.insitu.server.InsituDatasetDescr;
import org.esa.s3tbx.insitu.server.InsituParameter;
import org.esa.s3tbx.insitu.server.InsituQuery;
import org.esa.s3tbx.insitu.server.InsituResponse;
import org.esa.s3tbx.insitu.server.InsituServer;
import org.esa.s3tbx.insitu.server.InsituServerSpi;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.help.HelpAction;
import org.esa.snap.rcp.util.ProgressHandleMonitor;
import org.esa.snap.tango.TangoIcons;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.Cancellable;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

import javax.swing.AbstractButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
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
        refreshButton.addActionListener(e -> {
            ProgressHandleMonitor handle = ProgressHandleMonitor.create("In-Situ Data Access");
            InsituServerRunnable runnable = new InsituServerRunnable(handle);

            Utils.runWithProgress(runnable, handle);
            InsituResponse response = runnable.getResponse();
            if(InsituResponse.STATUS_CODE.NOK.equals(response.getStatus())) {
                StringBuilder sb = new StringBuilder();
                sb.append("Query not successful. Server responded with failure(s): \n");
                response.getFailureReasons().forEach(sb::append);
                SnapApp.getDefault().handleError(sb.toString(), null);
                return;
            }
            if(runnable.getException() != null) {
                SnapApp.getDefault().handleError("Could not update number of observations", runnable.getException());
                return;
            }
            setNumObs(response.getObservationCount());
        });
        refreshButton.setName("refreshButton");
        contentPanel.add(refreshButton);
        final AbstractButton downloadButton = ToolButtonFactory.createButton(TangoIcons.actions_document_save(TangoIcons.Res.R22), false);
        downloadButton.setText("Download");
        downloadButton.setName("downloadButton");
        contentPanel.add(downloadButton);
        contentPanel.add(layout.createHorizontalSpacer());

        AbstractButton helpButton = ToolButtonFactory.createButton(new HelpAction(helpCtx), false);
        helpButton.setName("helpButton");
        contentPanel.add(helpButton);

        return contentPanel;
    }

    private void setNumObs(long observationCount) {
        if (observationCount < 0) {
            refreshButton.setText("#Obs: UNKNOWN");
        }else {
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


    private class InsituServerRunnable implements Runnable, Cancellable {

        InsituResponse response;

        private final ProgressHandleMonitor handle;
        private Exception exception;

        public InsituServerRunnable(ProgressHandleMonitor handle) {
            this.handle = handle;
        }

        public InsituResponse getResponse() {
            return response;
        }

        public Exception getException() {
            return exception;
        }

        @Override
        public final void run() {
            run(handle);
        }

        public void run(ProgressMonitor pm) {
            pm.beginTask("Contacting in-situ server", ProgressMonitor.UNKNOWN);
            try {
                InsituServerSpi serverSpi = insituModel.getSelectedServerSpi();
                InsituServer server = serverSpi.createServer();

                InsituQuery query = new InsituQuery();
                query.subject(InsituQuery.SUBJECT.OBSERVATIONS).countOnly(true);
                query.latMin(insituModel.getMinLat()).latMax(insituModel.getMaxLat());
                query.lonMin(insituModel.getMinLon()).lonMax(insituModel.getMaxLon());
                query.startDate(insituModel.getStartDate()).stopDate(insituModel.getStopDate());
                InsituDatasetDescr selectedDataset = insituModel.getSelectedDataset();
                if (selectedDataset != null) {
                    query.dataset(selectedDataset.getName());
                }
                Stream<InsituParameter> stream = insituModel.getSelectedParameters().stream();
                String[] parameterNames = stream.map(InsituParameter::getName).toArray(String[]::new);
                query.param(parameterNames);

                response = server.query(query);
            } catch (Exception e) {
                exception = e;
            }finally {
                pm.done();
            }

        }

        @Override
        public boolean cancel() {
            return true;
        }
    }
}