package org.esa.s3tbx.slstr.pdu.stitching.ui;

import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.grender.Rendering;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

/**
 * @author Tonio Fincke
 */
class PDUBoundaryOverlay implements LayerCanvas.Overlay {

    private final PDUBoundariesProvider provider;
    private LayerCanvas layerCanvas;

    PDUBoundaryOverlay(PDUBoundariesProvider provider) {
        this.provider = provider;
    }

    @Override
    public void paintOverlay(LayerCanvas canvas, Rendering rendering) {
        layerCanvas = canvas;
        for (int i = 0; i < provider.getNumberOfElements(); i++) {
            final String name = provider.getName(i);
            final GeoPos[] geoBoundary = provider.getGeoBoundary(i);
            final boolean selected = provider.isSelected(i);
            drawGeoBoundary(rendering.getGraphics(), geoBoundary, selected, name);
        }
    }

    private void drawGeoBoundary(final Graphics2D g2d, final GeoPos[] geoBoundary, final boolean isCurrent,
                                 final String text) {
        final GeneralPath gp = convertToPixelPath(geoBoundary);
        drawPath(isCurrent, g2d, gp, 0.0f);
        final PixelPos boundaryCenter = getBoundaryCenter(geoBoundary);
        drawText(g2d, text, boundaryCenter, 0.0f, isCurrent);
    }

    private void drawPath(final boolean isCurrent, Graphics2D g2d, final GeneralPath gp, final float offsetX) {
        g2d = prepareGraphics2D(offsetX, g2d);
        if (isCurrent) {
            g2d.setColor(new Color(255, 200, 200, 30));
        } else {
            g2d.setColor(new Color(255, 255, 255, 30));
        }
        g2d.fill(gp);
        if (isCurrent) {
            g2d.setColor(new Color(255, 0, 0));
        } else {
            g2d.setColor(Color.WHITE);
        }
        g2d.draw(gp);
    }

    private GeneralPath convertToPixelPath(final GeoPos[] geoBoundary) {
        final GeneralPath gp = new GeneralPath();
        for (int i = 0; i < geoBoundary.length; i++) {
            final GeoPos geoPos = geoBoundary[i];
            final AffineTransform m2vTransform = layerCanvas.getViewport().getModelToViewTransform();
            final Point2D viewPos = m2vTransform.transform(new PixelPos.Double(geoPos.lon, geoPos.lat), null);
            if (i == 0) {
                gp.moveTo(viewPos.getX(), viewPos.getY());
            } else {
                gp.lineTo(viewPos.getX(), viewPos.getY());
            }
        }
        gp.closePath();
        return gp;
    }

    private void drawText(Graphics2D g2d, final String text, final PixelPos textCenter, final float offsetX, boolean isCurrent) {
        if (text == null || textCenter == null) {
            return;
        }
        g2d = prepareGraphics2D(offsetX, g2d);
        final FontMetrics fontMetrics = g2d.getFontMetrics();
        final Color color = g2d.getColor();
        if (isCurrent) {
            g2d.setColor(new Color(255, 0, 0));
        } else {
            g2d.setColor(Color.WHITE);
        }

        g2d.drawString(text,
                       (float) (textCenter.x - fontMetrics.stringWidth(text) / 2.0f),
                       (float) (textCenter.y + fontMetrics.getAscent() / 2.0f));
        g2d.setColor(color);
    }

    private Graphics2D prepareGraphics2D(final float offsetX, Graphics2D g2d) {
        if (offsetX != 0.0f) {
            g2d = (Graphics2D) g2d.create();
            final AffineTransform transform = g2d.getTransform();
            final AffineTransform offsetTrans = new AffineTransform();
            offsetTrans.setToTranslation(+offsetX, 0);
            transform.concatenate(offsetTrans);
            g2d.setTransform(transform);
        }
        return g2d;
    }

    private PixelPos getBoundaryCenter(final GeoPos[] geoBoundary) {
        final AffineTransform transform = layerCanvas.getViewport().getModelToViewTransform();
        double meanX = 0;
        double meanY = 0;
        for (GeoPos geoPos : geoBoundary) {
            final Point2D point2D = transform.transform(new Point2D.Double(geoPos.getLon(), geoPos.getLat()), null);
            meanX += point2D.getX();
            meanY += point2D.getY();
        }
        meanX /= geoBoundary.length;
        meanY /= geoBoundary.length;
        return new PixelPos(meanX, meanY);
    }

}
