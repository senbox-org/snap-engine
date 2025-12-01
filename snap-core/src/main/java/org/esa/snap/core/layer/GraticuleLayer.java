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
package org.esa.snap.core.layer;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import org.esa.snap.core.datamodel.Graticule;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeListenerAdapter;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.PixelPos;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;

import static org.esa.snap.core.datamodel.Graticule.containsValidGeoCorners;


/**
 * @author Marco Zuehlke
 * @author Daniel Knowles
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
//JAN2018 - Daniel Knowles - updated with SeaDAS gridline revisions

public class GraticuleLayer extends Layer {

    private static final GraticuleLayerType LAYER_TYPE = LayerTypeRegistry.getLayerType(GraticuleLayerType.class);

    private RasterDataNode raster;

    private ProductNodeHandler productNodeHandler;
    private Graticule graticule;

    private double NULL_DOUBLE = -1.0;
    private double ptsToPixelsMultiplier = NULL_DOUBLE;


    private int minorStep = 4;


    public GraticuleLayer(RasterDataNode raster) {
        this(LAYER_TYPE, raster, initConfiguration(LAYER_TYPE.createLayerConfig(null), raster));
    }

    public GraticuleLayer(GraticuleLayerType type, RasterDataNode raster, PropertySet configuration) {
        super(type, configuration);
        setName("Graticule Layer");
        this.raster = raster;

        productNodeHandler = new ProductNodeHandler();
        Product product = raster.getProduct();
        if (product != null) {
            product.addProductNodeListener(productNodeHandler);
        }

        setTransparency(0.0);
    }

    private static PropertySet initConfiguration(PropertySet configurationTemplate, RasterDataNode raster) {
        configurationTemplate.setValue(GraticuleLayerType.PROPERTY_NAME_RASTER, raster);
        return configurationTemplate;
    }

    private Product getProduct() {
        return getRaster().getProduct();
    }

    RasterDataNode getRaster() {
        return raster;
    }

    @Override
    public void renderLayer(Rendering rendering) {

        getUserValues();

        int spacer = getEdgeLabelsSpacer();


        if (graticule == null) {
            graticule = Graticule.create(raster,
                    getNumGridLines(),
                    getNumMinorSteps(),
                    getGridSpacingLat(),
                    getGridSpacingLon(),
                    isInterpolate(),
                    getTolerance(),
                    isLabelsSuffix(),
                    isLabelsDecimal(),
                    spacer);
        }
        if (graticule != null) {

            final Graphics2D g2d = rendering.getGraphics();
            // added this to improve text
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            final Viewport vp = rendering.getViewport();
            final AffineTransform transformSave = g2d.getTransform();
            try {
                final AffineTransform transform = new AffineTransform();
                transform.concatenate(transformSave);
                transform.concatenate(vp.getModelToViewTransform());
                transform.concatenate(raster.getSourceImage().getModel().getImageToModelTransform(0));
                g2d.setTransform(transform);


                final GeneralPath[] linePaths = graticule.getLinePaths();
                if (linePaths != null && isGridlinesShow()) {
                    drawLinePaths(g2d, linePaths);
                }

                if (isBorderShow()) {
                    boolean containsValidGeoCorners = containsValidGeoCorners(raster);

                    if (containsValidGeoCorners) {
                        drawBorder(g2d, raster);
                    }
                }


                if (isLabelsNorth()) {
                    final Graticule.TextGlyph[] textGlyphsNorth = graticule.getTextGlyphsNorth();
                    if (textGlyphsNorth != null) {
                        if (isTickmarksShow()) {
                            drawTickMarks(g2d, graticule.getTickPointsNorth(), Graticule.TextLocation.NORTH, false);
                        }

                        drawTextLabels(g2d, textGlyphsNorth, Graticule.TextLocation.NORTH, false, raster);
                    }
                }

                if (isLabelsSouth()) {
                    final Graticule.TextGlyph[] textGlyphsSouth = graticule.getTextGlyphsSouth();
                    if (textGlyphsSouth != null) {

                        if (isTickmarksShow()) {
                            drawTickMarks(g2d, graticule.getTickPointsSouth(), Graticule.TextLocation.SOUTH, false);
                        }
                        drawTextLabels(g2d, textGlyphsSouth, Graticule.TextLocation.SOUTH, false, raster);
                    }
                }

                if (isLabelsWest()) {
                    final Graticule.TextGlyph[] textGlyphsWest = graticule.getTextGlyphsWest();
                    if (textGlyphsWest != null) {
                        if (isTickmarksShow()) {
                            drawTickMarks(g2d, graticule.getTickPointsWest(), Graticule.TextLocation.WEST, false);
                        }
                        drawTextLabels(g2d, textGlyphsWest, Graticule.TextLocation.WEST, false, raster);
                    }
                }


                if (isLabelsEast()) {
                    final Graticule.TextGlyph[] textGlyphsEast = graticule.getTextGlyphsEast();
                    if (textGlyphsEast != null) {
                        if (isTickmarksShow()) {
                            drawTickMarks(g2d, graticule.getTickPointsEast(), Graticule.TextLocation.EAST, false);
                        }
                        drawTextLabels(g2d, textGlyphsEast, Graticule.TextLocation.EAST, false, raster);
                    }
                }


                if (isCornerLabelsWest()) {
                    if (isTickmarksShow()) {
                        drawCornerTickMarks(g2d, raster, Graticule.TextLocation.WEST);
                    }

                    if (!isLabelsInside()) {
                        drawLeftSideLatCornerLabels(g2d);
                    }
                }

                if (isCornerLabelsEast()) {
                    if (isTickmarksShow()) {
                        drawCornerTickMarks(g2d, raster, Graticule.TextLocation.EAST);
                    }

                    if (!isLabelsInside()) {
                        drawRightSideLatCornerLabels(g2d);
                    }
                }

                if (isCornerLabelsNorth()) {
                    if (isTickmarksShow()) {
                        drawCornerTickMarks(g2d, raster, Graticule.TextLocation.NORTH);
                    }

                    if (!isLabelsInside()) {
                        drawNorthSideLonCornerLabels(g2d);
                    }
                }

                if (isCornerLabelsSouth()) {
                    if (isTickmarksShow()) {
                        drawCornerTickMarks(g2d, raster, Graticule.TextLocation.SOUTH);
                    }

                    if (!isLabelsInside()) {
                        drawSouthSideLonCornerLabels(g2d);
                    }
                }


            } finally {
                g2d.setTransform(transformSave);
            }
        }
    }


    private void drawLeftSideLatCornerLabels(Graphics2D g2d) {

        final ArrayList<Graticule.TextGlyph> textGlyphArrayList = new ArrayList<>();

        Graticule.TextGlyph textGlyph = graticule.getTextGlyphsLatCorners()[Graticule.TOP_LEFT_CORNER_INDEX];
        if (textGlyph != null) {
            textGlyphArrayList.add(textGlyph);
        }

        textGlyph = graticule.getTextGlyphsLatCorners()[Graticule.BOTTOM_LEFT_CORNER_INDEX];
        if (textGlyph != null) {
            textGlyphArrayList.add(textGlyph);
        }

        final Graticule.TextGlyph[] textGlyphs = textGlyphArrayList.toArray(new Graticule.TextGlyph[textGlyphArrayList.size()]);
        if (textGlyphs != null) {
            drawTextLabels(g2d, textGlyphs, Graticule.TextLocation.WEST, true, raster);
        }
    }

    private void drawRightSideLatCornerLabels(Graphics2D g2d) {

        final ArrayList<Graticule.TextGlyph> textGlyphArrayList = new ArrayList<>();

        Graticule.TextGlyph textGlyph = graticule.getTextGlyphsLatCorners()[Graticule.TOP_RIGHT_CORNER_INDEX];
        if (textGlyph != null) {
            textGlyphArrayList.add(textGlyph);
        }

        textGlyph = graticule.getTextGlyphsLatCorners()[Graticule.BOTTOM_RIGHT_CORNER_INDEX];
        if (textGlyph != null) {
            textGlyphArrayList.add(textGlyph);
        }

        final Graticule.TextGlyph[] textGlyphs = textGlyphArrayList.toArray(new Graticule.TextGlyph[textGlyphArrayList.size()]);
        if (textGlyphs != null) {
            drawTextLabels(g2d, textGlyphs, Graticule.TextLocation.EAST, true, raster);
        }
    }

    private void drawNorthSideLonCornerLabels(Graphics2D g2d) {

        final ArrayList<Graticule.TextGlyph> textGlyphArrayList = new ArrayList<>();

        Graticule.TextGlyph textGlyph = graticule.getTextGlyphsLonCorners()[Graticule.TOP_LEFT_CORNER_INDEX];
        if (textGlyph != null) {
            textGlyphArrayList.add(textGlyph);
        }

        textGlyph = graticule.getTextGlyphsLonCorners()[Graticule.TOP_RIGHT_CORNER_INDEX];
        if (textGlyph != null) {
            textGlyphArrayList.add(textGlyph);
        }

        final Graticule.TextGlyph[] textGlyphs = textGlyphArrayList.toArray(new Graticule.TextGlyph[textGlyphArrayList.size()]);
        if (textGlyphs != null) {
            drawTextLabels(g2d, textGlyphs, Graticule.TextLocation.NORTH, true, raster);
        }
    }

    private void drawSouthSideLonCornerLabels(Graphics2D g2d) {

        final ArrayList<Graticule.TextGlyph> textGlyphArrayList = new ArrayList<>();

        Graticule.TextGlyph textGlyph = graticule.getTextGlyphsLonCorners()[Graticule.BOTTOM_LEFT_CORNER_INDEX];
        if (textGlyph != null) {
            textGlyphArrayList.add(textGlyph);
        }

        textGlyph = graticule.getTextGlyphsLonCorners()[Graticule.BOTTOM_RIGHT_CORNER_INDEX];
        if (textGlyph != null) {
            textGlyphArrayList.add(textGlyph);
        }

        final Graticule.TextGlyph[] textGlyphs = textGlyphArrayList.toArray(new Graticule.TextGlyph[textGlyphArrayList.size()]);
        if (textGlyphs != null) {
            drawTextLabels(g2d, textGlyphs, Graticule.TextLocation.SOUTH, true, raster);
        }
    }


    private void getUserValues() {


    }

    private void drawLinePaths(Graphics2D g2d, final GeneralPath[] linePaths) {

        Color origPaint = (Color) g2d.getPaint();
        Stroke origStroke = g2d.getStroke();

        Composite oldComposite = null;
        if (getGridlinesTransparency() > 0.0) {

            oldComposite = g2d.getComposite();
            g2d.setComposite(getAlphaComposite(getGridlinesTransparency()));
        }
        g2d.setPaint(getGridlinesColor());


        Stroke drawingStroke;
        //   if (isDashedLine() || getDashLengthPixels() != 0.0) {
        if (getDashLengthPixels() > 0.0) {
            drawingStroke = new BasicStroke((float) getGridlinesWidthPixels(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{(float) getDashLengthPixels()}, 0);
        } else {
            drawingStroke = new BasicStroke((float) getGridlinesWidthPixels());
        }

        g2d.setStroke(drawingStroke);
        for (GeneralPath linePath : linePaths) {
            g2d.draw(linePath);
        }

        if (oldComposite != null) {
            g2d.setComposite(oldComposite);
        }
        g2d.setStroke(origStroke);
        g2d.setPaint(origPaint);
    }

    private void drawCornerTickMarks(Graphics2D g2d, RasterDataNode raster, Graticule.TextLocation textLocation) {

        PixelPos pixelPos1 = null;
        PixelPos pixelPos2 = null;

        switch (textLocation) {
            case NORTH:
                pixelPos1 = new PixelPos(0, 0);
                pixelPos2 = new PixelPos(raster.getRasterWidth(), 0);
                break;
            case SOUTH:
                pixelPos1 = new PixelPos(0, raster.getRasterHeight());
                pixelPos2 = new PixelPos(raster.getRasterWidth(), raster.getRasterHeight());
                break;
            case WEST:
                pixelPos1 = new PixelPos(0, 0);
                pixelPos2 = new PixelPos(0, raster.getRasterHeight());
                break;
            case EAST:
                pixelPos1 = new PixelPos(raster.getRasterWidth(), 0);
                pixelPos2 = new PixelPos(raster.getRasterWidth(), raster.getRasterHeight());
                break;
        }

        if (pixelPos1 != null && pixelPos2 != null) {
            PixelPos pixelPos[];
            pixelPos = new PixelPos[2];
            pixelPos[0] = pixelPos1;
            pixelPos[1] = pixelPos2;

            boolean drawCornerTicks = false;  // not sure we need this so I'm turning it off
            if (drawCornerTicks) {
                if (!isTickmarksInside()) {
                    drawTickMarks(g2d, pixelPos, textLocation, true);
                }
            }
        }
    }


    private void drawBorder(Graphics2D g2d, RasterDataNode raster) {

        Color origColor = (Color) g2d.getPaint();
        Stroke origStroke = g2d.getStroke();

        double sidewaysShift = getBorderWidthPixels() / 2;
        double lengthWiseAddOn = getBorderWidthPixels();


        GeneralPath northBorderPath = new GeneralPath();
        GeneralPath southBorderPath = new GeneralPath();
        {
            double xStart = 0 - lengthWiseAddOn;
            double xEnd = raster.getRasterWidth() + lengthWiseAddOn;

            double y = 0 - sidewaysShift;
            northBorderPath.moveTo(xStart, y);
            northBorderPath.lineTo(xEnd, y);
            northBorderPath.closePath();

            y = raster.getRasterHeight() + sidewaysShift;
            southBorderPath.moveTo(xStart, y);
            southBorderPath.lineTo(xEnd, y);
            southBorderPath.closePath();
        }

        GeneralPath westBorderPath = new GeneralPath();
        GeneralPath eastBorderPath = new GeneralPath();
        {
            double yStart = 0 - lengthWiseAddOn;
            double yEnd = raster.getRasterHeight() + lengthWiseAddOn;

            double x = 0 - sidewaysShift;
            westBorderPath.moveTo(x, yStart);
            westBorderPath.lineTo(x, yEnd);
            westBorderPath.closePath();

            x = raster.getRasterWidth() + sidewaysShift;
            eastBorderPath.moveTo(x, yStart);
            eastBorderPath.lineTo(x, yEnd);
            eastBorderPath.closePath();
        }

        Stroke drawingStroke = new BasicStroke((float) getBorderWidthPixels());
        g2d.setStroke(drawingStroke);
        g2d.setPaint(getBorderColor());

        g2d.draw(southBorderPath);
        g2d.draw(northBorderPath);
        g2d.draw(westBorderPath);
        g2d.draw(eastBorderPath);

        g2d.setPaint(origColor);
        g2d.setStroke(origStroke);
    }



    private boolean isTextConflict(PixelPos pixelPos, Graphics2D g2d, Graticule.TextLocation textLocation,
                                   boolean isCorner,
                                   RasterDataNode raster) {
        if (1 == 1) {
            return false;
        }
        Font origFont = g2d.getFont();
        Font font = new Font(getLabelsFont(), getFontType(), getFontSizePixels());
        g2d.setFont(font);

        Rectangle2D singleLetter = g2d.getFontMetrics().getStringBounds("W", g2d);

        double WRONG_AXIS_BUFFER_PERCENT = 0.02;

        boolean conflict = false;
        double overlapMultiplierBuffer = 1.0;
        double xAllowedMin = 0 + overlapMultiplierBuffer * singleLetter.getHeight();
        double xAllowedMax = raster.getRasterWidth() - overlapMultiplierBuffer * singleLetter.getHeight();
        double yAllowedMin = 0 + overlapMultiplierBuffer * singleLetter.getHeight();
        double yAllowedMax = raster.getRasterHeight() - overlapMultiplierBuffer * singleLetter.getHeight();
        double buffer = 0;

        switch (textLocation) {
            case NORTH:
                if (isCornerLabelsNorth() && !isCorner) {
                    if (pixelPos.getX() < xAllowedMin || pixelPos.getX() > xAllowedMax) {
                        conflict = true;
                    }
                }

                buffer = WRONG_AXIS_BUFFER_PERCENT * raster.getRasterHeight();
                if (pixelPos.getY() > buffer) {
                    conflict = true;
                }
                break;
            case SOUTH:
                if (isCornerLabelsSouth() && !isCorner) {
                    if (pixelPos.getX() < xAllowedMin || pixelPos.getX() > xAllowedMax) {
                        conflict = true;
                    }
                }

                buffer = WRONG_AXIS_BUFFER_PERCENT * raster.getRasterHeight();
                if (pixelPos.getY() < (raster.getRasterHeight() - buffer)) {
                    conflict = true;
                }
                break;
            case WEST:
                if (isCornerLabelsWest() && !isCorner) {
                    if (pixelPos.getY() < yAllowedMin || pixelPos.getY() > yAllowedMax) {
                        conflict = true;
                    }
                }

                buffer = WRONG_AXIS_BUFFER_PERCENT * raster.getRasterWidth();
                if (pixelPos.getX() > buffer) {
                    conflict = true;
                }
                break;
            case EAST:
                if (isCornerLabelsEast() && !isCorner) {
                    if (pixelPos.getY() < yAllowedMin || pixelPos.getY() > yAllowedMax) {
                        conflict = true;
                    }
                }

                buffer = WRONG_AXIS_BUFFER_PERCENT * raster.getRasterWidth();
                if (pixelPos.getX() < (raster.getRasterWidth() - buffer)) {
                    conflict = true;
                }
                break;

        }

        g2d.setFont(origFont);

        return conflict;
    }


    private void drawTextLabels(Graphics2D g2d,
                                final Graticule.TextGlyph[] textGlyphs,
                                Graticule.TextLocation textLocation,
                                boolean isCorner,
                                RasterDataNode raster) {

        double halfPixelCorrection = 0.0;
        if (!isCorner) {
            halfPixelCorrection = 0.5;
        }


        Color origColor = (Color) g2d.getPaint();
        if (isCorner) {
            g2d.setPaint(getLabelsColor());
        } else {
            g2d.setPaint(getLabelsColor());
        }


        Font origFont = g2d.getFont();
        Font font = new Font(getLabelsFont(), getFontType(), getFontSizePixels());
        g2d.setFont(font);


        Rectangle2D singleLetter = g2d.getFontMetrics().getStringBounds("W", g2d);
        double letterWidth = singleLetter.getWidth();
        float spacerBetweenTextAndBorder = (float) (letterWidth / 2.0);


        for (Graticule.TextGlyph glyph : textGlyphs) {


            PixelPos pixelPos = new PixelPos(glyph.getX(), glyph.getY());


            if (!isTextConflict(pixelPos, g2d, textLocation, isCorner, raster)) {

                if (isLabelsInside() && getInsideLabelsBgTransparency() < 1.0) {
                    drawRectangle(g2d, glyph);
                }

                g2d.translate(glyph.getX(), glyph.getY());
                g2d.rotate(glyph.getAngle());

                Rectangle2D labelBounds = g2d.getFontMetrics().getStringBounds(glyph.getText(), g2d);
                float width = (float) labelBounds.getWidth();
                float height = (float) labelBounds.getHeight();


                float halfLabelWidth = width / 2;


                AffineTransform orig = g2d.getTransform();


                if (!isLabelsInside()) {
                    if (textLocation == Graticule.TextLocation.NORTH) {
                        double theta = (getLabelsRotationLon() / 180) * Math.PI;

                        float xOffset = 0;
                        float yOffset = 0;
                        double verticalShift = halfPixelCorrection + getBorderWidthPixels() + spacerBetweenTextAndBorder;

                        if (isTickmarksShow() && !isTickmarksInside() && !isCorner) {
                            verticalShift += getTickmarksLength();
                        }

                        if (getLabelsRotationLon() > 85) {
                            xOffset = -halfLabelWidth;
                        }

                        if (getLabelsRotationLon() < 5) {
                            yOffset = height / 3;
                        }

                        float xMod = (float) (verticalShift * Math.cos(theta));
                        float yMod = -1 * (float) (verticalShift * Math.sin(theta));

                        g2d.rotate(-1 * Math.PI + theta);
                        g2d.drawString(glyph.getText(), xMod + xOffset, +yMod + yOffset);
                    }

                    if (textLocation == Graticule.TextLocation.SOUTH) {
                        double theta = (getLabelsRotationLon() / 180) * Math.PI;

                        float xOffset = -width;
                        float yOffset = 2 * height / 3;
                        double verticalShift = -halfPixelCorrection - getBorderWidthPixels() - spacerBetweenTextAndBorder;

                        if (isTickmarksShow() && !isTickmarksInside() && !isCorner) {
                            verticalShift -= getTickmarksLength();
                        }

                        if (getLabelsRotationLon() > 85) {
                            xOffset = xOffset + halfLabelWidth;
                        }

                        if (getLabelsRotationLon() < 5) {
                            yOffset = yOffset - height / 3;
                        }

                        float xMod = (float) (verticalShift * Math.cos(theta));
                        float yMod = -1 * (float) (verticalShift * Math.sin(theta));

                        g2d.rotate(theta);
                        g2d.drawString(glyph.getText(), xMod + xOffset, +yMod + yOffset);
                    }

                    if (textLocation == Graticule.TextLocation.EAST) {

                        double theta = (getLabelsRotationLat() / 180) * Math.PI;

                        float xOffset = 0;
                        float yOffset = 2 * height / 3;
                        double verticalShift = halfPixelCorrection + getBorderWidthPixels() + spacerBetweenTextAndBorder;

                        if (isTickmarksShow() && !isTickmarksInside() && !isCorner) {
                            verticalShift += getTickmarksLength();
                        }

                        if (getLabelsRotationLat() > 85) {
                            xOffset = -halfLabelWidth;
                        }

                        if (getLabelsRotationLat() < 5) {
                            yOffset = height / 3;
                        }

                        float xMod = (float) (verticalShift * Math.cos(theta));
                        float yMod = (float) (verticalShift * Math.sin(theta));


                        g2d.rotate(-1 * Math.PI - theta);
                        g2d.drawString(glyph.getText(), xMod + xOffset, +yMod + yOffset);
                    }


                    if (textLocation == Graticule.TextLocation.WEST) {

                        double theta = (getLabelsRotationLat() / 180) * Math.PI;

                        float xOffset = -width;
                        float yOffset = 0;
                        double verticalShift = -halfPixelCorrection - getBorderWidthPixels() - spacerBetweenTextAndBorder;

                        if (isTickmarksShow() && !isTickmarksInside() && !isCorner) {
                            verticalShift -= getTickmarksLength();
                        }

                        if (getLabelsRotationLat() > 85) {
                            xOffset = xOffset + halfLabelWidth;
                        }

                        if (getLabelsRotationLat() < 5) {
                            yOffset = yOffset + height / 3;
                        }

                        float xMod = (float) (verticalShift * Math.cos(theta));
                        float yMod = (float) (verticalShift * Math.sin(theta));


                        g2d.rotate(-theta);
                        g2d.drawString(glyph.getText(), xMod + xOffset, +yMod + yOffset);
                    }
                } else {

                    if (textLocation == Graticule.TextLocation.WEST ||
                            textLocation == Graticule.TextLocation.SOUTH) {

                        float xOffset = spacerBetweenTextAndBorder;
                        float yOffset = height / 3;

                        if (isTickmarksShow() && isTickmarksInside()) {
                            xOffset += getTickmarksLength();
                        }

                        g2d.drawString(glyph.getText(), xOffset, yOffset);
                    } else {
                        float xOffset = -width - spacerBetweenTextAndBorder;
                        float yOffset = height / 3;

                        if (isTickmarksShow() && isTickmarksInside()) {
                            xOffset -= getTickmarksLength();
                        }

                        g2d.rotate(-Math.PI);
                        g2d.drawString(glyph.getText(), xOffset, yOffset);
                    }
                }

                g2d.setTransform(orig);

                g2d.rotate(-glyph.getAngle());
                g2d.translate(-glyph.getX(), -glyph.getY());

            }
        }


        g2d.setPaint(origColor);
        g2d.setFont(origFont);

    }


    private void drawRectangle(Graphics2D g2d, final Graticule.TextGlyph glyph) {

        Composite oldComposite = null;
        if (getInsideLabelsBgTransparency() > 0.0) {
            oldComposite = g2d.getComposite();
            g2d.setComposite(getAlphaComposite(getInsideLabelsBgTransparency()));
        }

        Color origPaint = (Color) g2d.getPaint();
        Stroke origStroke = g2d.getStroke();

        g2d.setPaint(getInsideLabelsBgColor());
        g2d.setStroke(new BasicStroke(0));


        g2d.translate(glyph.getX(), glyph.getY());
        g2d.rotate(glyph.getAngle());

        Rectangle2D singleLetter = g2d.getFontMetrics().getStringBounds("W", g2d);
        double xOffset = singleLetter.getWidth() / 2.0;
        double yOffset = singleLetter.getHeight() / 3.0;

        if (isTickmarksShow() && isTickmarksInside()) {
            xOffset += getTickmarksLength();
        }

        Rectangle2D labelBounds = g2d.getFontMetrics().getStringBounds(glyph.getText(), g2d);
        labelBounds.setRect(labelBounds.getX() + xOffset - 1,
                labelBounds.getY() + yOffset - 1,
                labelBounds.getWidth(),
                labelBounds.getHeight());

        g2d.fill(labelBounds);

        g2d.rotate(-glyph.getAngle());
        g2d.translate(-glyph.getX(), -glyph.getY());


        g2d.setPaint(origPaint);
        g2d.setStroke(origStroke);
        if (oldComposite != null) {
            g2d.setComposite(oldComposite);
        }
    }


    private void drawTickMarks(Graphics2D g2d, final PixelPos[] pixelPoses, Graticule.TextLocation textLocation, boolean isCorner) {

        Composite oldComposite = g2d.getComposite();
        Stroke origStroke = g2d.getStroke();

        Stroke drawingStroke = new BasicStroke((float) getGridlinesWidthPixels());
        g2d.setStroke(drawingStroke);

        Color origPaint = (Color) g2d.getPaint();
        g2d.setPaint(getTickmarksColor());


        double halfPixelCorrection = 0.0;
        if (!isCorner) {
            halfPixelCorrection = 0.5;
        }

        double xStart = 0, xEnd = 0, yStart = 0, yEnd = 0;

        boolean validCase = false;

        for (PixelPos pixelPos : pixelPoses) {

            if (!isTextConflict(pixelPos, g2d, textLocation, isCorner, raster)) {
                switch (textLocation) {
                    case NORTH:
                        xStart = pixelPos.getX();
                        xEnd = pixelPos.getX();

                        if (isTickmarksInside()) {
                            yStart = pixelPos.getY() - halfPixelCorrection;
                            yEnd = yStart + getTickmarksLength();
                        } else {
                            yStart = pixelPos.getY() - halfPixelCorrection - getBorderWidthPixels();
                            yEnd = yStart - getTickmarksLength();
                        }
                        validCase = true;
                        break;

                    case SOUTH:
                        xStart = pixelPos.getX();
                        xEnd = pixelPos.getX();

                        if (isTickmarksInside()) {
                            yStart = pixelPos.getY() + halfPixelCorrection;
                            yEnd = yStart - getTickmarksLength();
                        } else {
                            yStart = pixelPos.getY() + halfPixelCorrection + getBorderWidthPixels();
                            yEnd = yStart + getTickmarksLength();
                        }

                        validCase = true;
                        break;

                    case WEST:
                        yStart = pixelPos.getY();
                        yEnd = pixelPos.getY();

                        if (isTickmarksInside()) {
                            xStart = pixelPos.getX() - halfPixelCorrection;
                            xEnd = xStart + getTickmarksLength();
                        } else {
                            xStart = pixelPos.getX() - halfPixelCorrection - getBorderWidthPixels();
                            xEnd = xStart - getTickmarksLength();
                        }

                        validCase = true;
                        break;

                    case EAST:
                        yStart = pixelPos.getY();
                        yEnd = pixelPos.getY();

                        if (isTickmarksInside()) {
                            xStart = pixelPos.getX() + halfPixelCorrection;
                            xEnd = xStart - getTickmarksLength();
                        } else {
                            xStart = pixelPos.getX() + halfPixelCorrection + getBorderWidthPixels();
                            xEnd = xStart + getTickmarksLength();
                        }

                        validCase = true;
                        break;
                }

                if (validCase) {
                    GeneralPath path = new GeneralPath();
                    path.moveTo(xStart, yStart);
                    path.lineTo(xEnd, yEnd);

                    path.closePath();
                    g2d.draw(path);
                }
            }

        }

        g2d.setPaint(origPaint);
        g2d.setStroke(origStroke);
        if (oldComposite != null) {
            g2d.setComposite(oldComposite);
        }
    }


    private AlphaComposite getAlphaComposite(double itemTransparancy) {
        double combinedAlpha = (1.0 - getTransparency()) * (1.0 - itemTransparancy);
        return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) combinedAlpha);
    }

    @Override
    public void disposeLayer() {
        final Product product = getProduct();
        if (product != null) {
            product.removeProductNodeListener(productNodeHandler);
            graticule = null;
            raster = null;
        }
    }

    @Override
    protected void fireLayerPropertyChanged(PropertyChangeEvent event) {
        String propertyName = event.getPropertyName();
        if (
                propertyName.equals(GraticuleLayerType.PROPERTY_GRID_SPACING_LAT_NAME) ||
                        propertyName.equals(GraticuleLayerType.PROPERTY_GRID_SPACING_LON_NAME) ||
                        propertyName.equals(GraticuleLayerType.PROPERTY_EDGE_LABELS_SPACER_NAME) ||
                        propertyName.equals(GraticuleLayerType.PROPERTY_NUM_GRID_LINES_NAME) ||
                        propertyName.equals(GraticuleLayerType.PROPERTY_MINOR_STEPS_NAME) ||
                        propertyName.equals(GraticuleLayerType.PROPERTY_INTERPOLATE_KEY) ||
                        propertyName.equals(GraticuleLayerType.PROPERTY_TOLERANCE_KEY) ||
                        propertyName.equals(GraticuleLayerType.PROPERTY_LABELS_SUFFIX_NSWE_NAME) ||
                        propertyName.equals(GraticuleLayerType.PROPERTY_LABELS_DECIMAL_VALUE_NAME)
                ) {
            graticule = null;
        }
        if (getConfiguration().getProperty(propertyName) != null) {
            getConfiguration().setValue(propertyName, event.getNewValue());
        }
        super.fireLayerPropertyChanged(event);
    }


    private double getGridSpacingLon() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_GRID_SPACING_LON_NAME,
                GraticuleLayerType.PROPERTY_GRID_SPACING_LON_DEFAULT);
    }

    private double getGridSpacingLat() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_GRID_SPACING_LAT_NAME,
                GraticuleLayerType.PROPERTY_GRID_SPACING_LAT_DEFAULT);
    }

    private int getNumGridLines() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_NUM_GRID_LINES_NAME,
                GraticuleLayerType.PROPERTY_NUM_GRID_LINES_DEFAULT);
    }

    private int getNumMinorSteps() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_MINOR_STEPS_NAME,
                GraticuleLayerType.PROPERTY_MINOR_STEPS_DEFAULT);
    }

    private boolean isInterpolate() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_INTERPOLATE_KEY,
                GraticuleLayerType.PROPERTY_INTERPOLATE_DEFAULT);
    }

    private double getTolerance() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_TOLERANCE_KEY,
                GraticuleLayerType.PROPERTY_TOLERANCE_DEFAULT);
    }



    private Color getGridlinesColor() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_GRIDLINES_COLOR_NAME,
                GraticuleLayerType.PROPERTY_GRIDLINES_COLOR_DEFAULT);
    }

    private double getGridlinesTransparency() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_GRIDLINES_TRANSPARENCY_NAME,
                GraticuleLayerType.PROPERTY_GRIDLINES_TRANSPARENCY_DEFAULT);
    }


    private Color getLabelsColor() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_LABELS_COLOR_NAME,
                GraticuleLayerType.PROPERTY_LABELS_COLOR_DEFAULT);
    }

    private Color getTickmarksColor() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_TICKMARKS_COLOR_NAME,
                GraticuleLayerType.PROPERTY_TICKMARKS_COLOR_DEFAULT);
    }



    private Color getInsideLabelsBgColor() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_COLOR_NAME,
                GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_COLOR_DEFAULT);
    }

    private double getInsideLabelsBgTransparency() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_TRANSPARENCY_NAME,
                GraticuleLayerType.PROPERTY_INSIDE_LABELS_BG_TRANSPARENCY_DEFAULT);
    }


    private double getGridlinesWidthPixels() {
        double gridLineWidthPts = getConfigurationProperty(GraticuleLayerType.PROPERTY_GRIDLINES_WIDTH_NAME,
                GraticuleLayerType.PROPERTY_GRIDLINES_WIDTH_DEFAULT);

        return getPtsToPixelsMultiplier() * gridLineWidthPts;
    }


    private double getBorderWidthPixels() {
        double borderLineWidthPts = getConfigurationProperty(GraticuleLayerType.PROPERTY_BORDER_WIDTH_NAME,
                GraticuleLayerType.PROPERTY_BORDER_WIDTH_DEFAULT);

        return getPtsToPixelsMultiplier() * borderLineWidthPts;
    }


    private double getDashLengthPixels() {
        double dashLengthPts = getConfigurationProperty(GraticuleLayerType.PROPERTY_GRIDLINES_DASHED_PHASE_NAME,
                GraticuleLayerType.PROPERTY_GRIDLINES_DASHED_PHASE_DEFAULT);

        return getPtsToPixelsMultiplier() * dashLengthPts;
    }


    private int getFontSizePixels() {
        int fontSizePts = getConfigurationProperty(GraticuleLayerType.PROPERTY_LABELS_SIZE_NAME,
                GraticuleLayerType.PROPERTY_LABELS_SIZE_DEFAULT);

        return (int) Math.round(getPtsToPixelsMultiplier() * fontSizePts);
    }


    private int getEdgeLabelsSpacer() {
        int fontSizePts = getConfigurationProperty(GraticuleLayerType.PROPERTY_EDGE_LABELS_SPACER_NAME,
                GraticuleLayerType.PROPERTY_EDGE_LABELS_SPACER_DEFAULT);

        return (int) Math.round(getPtsToPixelsMultiplier() * fontSizePts);
    }


    private double getPtsToPixelsMultiplier() {

        if (ptsToPixelsMultiplier == NULL_DOUBLE) {
//            final double PTS_PER_INCH = 72.0;
//            final double PAPER_HEIGHT = 11.0;
//            final double PAPER_WIDTH = 8.5;
//
//            double heightToWidthRatioPaper = (PAPER_HEIGHT) / (PAPER_WIDTH);
//            double heightToWidthRatioRaster = raster.getRasterHeight() / raster.getRasterWidth();
//
//            if (heightToWidthRatioRaster > heightToWidthRatioPaper) {
//                // use height
//                ptsToPixelsMultiplier = (1 / PTS_PER_INCH) * (raster.getRasterHeight() / (PAPER_HEIGHT));
//            } else {
//                // use width
//                ptsToPixelsMultiplier = (1 / PTS_PER_INCH) * (raster.getRasterWidth() / (PAPER_WIDTH));
//            }

            double averageSideSize = (raster.getRasterHeight() + raster.getRasterWidth()) / 2;
//            double maxSideSize = Math.max(raster.getRasterHeight(), raster.getRasterWidth());

            ptsToPixelsMultiplier = averageSideSize * 0.001;
        }


        return ptsToPixelsMultiplier;
    }


    private String getLabelsFont() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_LABELS_FONT_NAME,
                GraticuleLayerType.PROPERTY_LABELS_FONT_DEFAULT);
    }

    private Boolean isLabelsItalic() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_LABELS_ITALIC_NAME,
                GraticuleLayerType.PROPERTY_LABELS_ITALIC_DEFAULT);
    }

    private Boolean isLabelsBold() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_LABELS_BOLD_NAME,
                GraticuleLayerType.PROPERTY_LABELS_BOLD_DEFAULT);
    }
    private int getFontType() {
        if (isLabelsItalic() && isLabelsBold()) {
            return Font.ITALIC | Font.BOLD;
        } else if (isLabelsItalic()) {
            return Font.ITALIC;
        } else if (isLabelsBold()) {
            return Font.BOLD;
        } else {
            return Font.PLAIN;
        }
    }

    private boolean isLabelsInside() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_LABELS_INSIDE_NAME,
                GraticuleLayerType.PROPERTY_LABELS_INSIDE_DEFAULT);
    }

    private double getLabelsRotationLon() {
        return  getConfigurationProperty(GraticuleLayerType.PROPERTY_LABELS_ROTATION_LON_NAME,
                GraticuleLayerType.PROPERTY_LABELS_ROTATION_LON_DEFAULT);
    }


    private double getLabelsRotationLat() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_LABELS_ROTATION_LAT_NAME,
                GraticuleLayerType.PROPERTY_LABELS_ROTATION_LAT_DEFAULT);
    }


    private boolean isLabelsNorth() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_LABELS_NORTH_NAME,
                GraticuleLayerType.PROPERTY_LABELS_NORTH_DEFAULT);
    }

    private boolean isLabelsSouth() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_LABELS_SOUTH_NAME,
                GraticuleLayerType.PROPERTY_LABELS_SOUTH_DEFAULT);
    }

    private boolean isLabelsWest() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_LABELS_WEST_NAME,
                GraticuleLayerType.PROPERTY_LABELS_WEST_DEFAULT);
    }

    private boolean isLabelsEast() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_LABELS_EAST_NAME,
                GraticuleLayerType.PROPERTY_LABELS_EAST_DEFAULT);
    }

    private boolean isGridlinesShow() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_NAME,
                GraticuleLayerType.PROPERTY_GRIDLINES_SHOW_DEFAULT);
    }


    private boolean isBorderShow() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_BORDER_SHOW_NAME,
                GraticuleLayerType.PROPERTY_BORDER_SHOW_DEFAULT);
    }

    private boolean isLabelsSuffix() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_LABELS_SUFFIX_NSWE_NAME,
                GraticuleLayerType.PROPERTY_LABELS_SUFFIX_NSWE_DEFAULT);
    }

    private boolean isLabelsDecimal() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_LABELS_DECIMAL_VALUE_NAME,
                GraticuleLayerType.PROPERTY_LABELS_DECIMAL_VALUE_DEFAULT);
    }

    private Color getBorderColor() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_BORDER_COLOR_NAME,
                GraticuleLayerType.PROPERTY_BORDER_COLOR_DEFAULT);
    }


    private boolean isCornerLabelsNorth() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_CORNER_LABELS_NORTH_NAME,
                GraticuleLayerType.PROPERTY_CORNER_LABELS_NORTH_DEFAULT);
    }

    private boolean isCornerLabelsWest() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_CORNER_LABELS_WEST_NAME,
                GraticuleLayerType.PROPERTY_CORNER_LABELS_WEST_DEFAULT);
    }


    private boolean isCornerLabelsEast() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_CORNER_LABELS_EAST_NAME,
                GraticuleLayerType.PROPERTY_CORNER_LABELS_EAST_DEFAULT);
    }


    private boolean isCornerLabelsSouth() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_CORNER_LABELS_SOUTH_NAME,
                GraticuleLayerType.PROPERTY_CORNER_LABELS_SOUTH_DEFAULT);
    }


    private boolean isTickmarksShow() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_TICKMARKS_SHOW_NAME,
                GraticuleLayerType.PROPERTY_TICKMARKS_SHOW_DEFAULT);
    }

    private boolean isTickmarksInside() {
        return getConfigurationProperty(GraticuleLayerType.PROPERTY_TICKMARKS_INSIDE_NAME,
                GraticuleLayerType.PROPERTY_TICKMARKS_INSIDE_DEFAULT);
    }


    private double getTickmarksLength() {
        double tickMarkLengthPts = getConfigurationProperty(GraticuleLayerType.PROPERTY_TICKMARKS_LENGTH_NAME,
                GraticuleLayerType.PROPERTY_TICKMARKS_LENGTH_DEFAULT);

        return getPtsToPixelsMultiplier() * tickMarkLengthPts;
    }


    private class ProductNodeHandler extends ProductNodeListenerAdapter {

        /**
         * Overwrite this method if you want to be notified when a node changed.
         *
         * @param event the product node which the listener to be notified
         */
        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if (event.getSourceNode() == getProduct() && Product.PROPERTY_NAME_SCENE_GEO_CODING.equals(
                    event.getPropertyName())) {
                // Force recreation
                graticule = null;
                fireLayerDataChanged(getModelBounds());
            }
        }
    }

}
