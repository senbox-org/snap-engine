package org.esa.s3tbx.slstr.pdu.stitching.ui;

import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.grender.Viewport;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.ui.WorldMapPane;
import org.esa.snap.ui.WorldMapPaneDataModel;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tonio Fincke
 */
class PDUWorldMapPane extends WorldMapPane {

    private final PDUBoundariesProvider provider;
    private LayerCanvas.Overlay greyOverlay;

    PDUWorldMapPane(WorldMapPaneDataModel dataModel, PDUBoundariesProvider provider, LayerCanvas.Overlay overlay) {
        super(dataModel, overlay);
        this.provider = provider;
        greyOverlay = (canvas, rendering) -> {
            final Graphics2D graphics = rendering.getGraphics();
            graphics.setPaint(new Color(200, 200, 200, 180));
            graphics.fillRect(0, 0, getWidth(), getHeight());
        };
    }

    @Override
    protected Action[] getOverlayActions() {
        final Action[] overlayActions = super.getOverlayActions();
        if (overlayActions.length != 2) {
            return overlayActions;
        }
        final Object icon = overlayActions[1].getValue(Action.LARGE_ICON_KEY);
        overlayActions[1] = new ZoomToSelectedBoundariesAction(icon);
        return overlayActions;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled == isEnabled()) {
            return;
        }
        super.setEnabled(enabled);
        if (enabled) {
            final LayerCanvas layerCanvas = getLayerCanvas();
            layerCanvas.removeOverlay(greyOverlay);
            setPanSupport(new PDUWorldMapPanePanSupport(layerCanvas));
        } else {
            zoomAll();
            getLayerCanvas().addOverlay(greyOverlay);
            setPanSupport(new NullPanSupport());

        }
    }

    private void zoomToSelectedBoundaries() {
        List<GeneralPath> generalPaths = new ArrayList<>();
        for (int i = 0; i < provider.getNumberOfElements(); i++) {
            if (provider.isSelected(i)) {
                final GeneralPath generalPath = convertToGeoPath(provider.getGeoBoundary(i));
                generalPaths.add(generalPath);
            }
        }
        Rectangle2D modelArea = new Rectangle2D.Double();
        final Viewport viewport = getLayerCanvas().getViewport();
        for (GeneralPath generalPath : generalPaths) {
            final Rectangle2D rectangle2D = generalPath.getBounds2D();
            if (modelArea.isEmpty()) {
                if (!viewport.isModelYAxisDown()) {
                    modelArea.setFrame(rectangle2D.getX(), rectangle2D.getMaxY(),
                                       rectangle2D.getWidth(), rectangle2D.getHeight());
                }
                modelArea = rectangle2D;
            } else {
                modelArea.add(rectangle2D);
            }
        }
        Rectangle2D modelBounds = modelArea.getBounds2D();
        modelBounds.setFrame(modelBounds.getX() - 2, modelBounds.getY() - 2,
                             modelBounds.getWidth() + 4, modelBounds.getHeight() + 4);

        modelBounds = cropToMaxModelBounds(modelBounds);

        viewport.zoom(modelBounds);
        fireScrolled();
    }

    private GeneralPath convertToGeoPath(final GeoPos[] geoBoundary) {
        final GeneralPath gp = new GeneralPath();
        for (int i = 0; i < geoBoundary.length; i++) {
            final GeoPos geoPos = geoBoundary[i];
            if (i == 0) {
                gp.moveTo(geoPos.getLon(), geoPos.getLat());
            } else {
                gp.lineTo(geoPos.getLon(), geoPos.getLat());
            }
        }
        gp.closePath();
        return gp;
    }

    private class ZoomToSelectedBoundariesAction extends AbstractAction {

        private ZoomToSelectedBoundariesAction(Object icon) {
            putValue(LARGE_ICON_KEY, icon);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (isEnabled()) {
                zoomToSelectedBoundaries();
            }
        }
    }

    private class NullPanSupport implements PanSupport {

        @Override
        public void panStarted(MouseEvent event) {
        }

        @Override
        public void performPan(MouseEvent event) {
        }

        @Override
        public void panStopped(MouseEvent event) {
        }
    }

    private class PDUWorldMapPanePanSupport extends WorldMapPane.DefaultPanSupport {
        protected PDUWorldMapPanePanSupport(LayerCanvas layerCanvas) {
            super(layerCanvas);
        }
    }

}
