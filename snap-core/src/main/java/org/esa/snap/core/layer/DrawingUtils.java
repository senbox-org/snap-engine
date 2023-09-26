package org.esa.snap.core.layer;

import org.esa.snap.core.datamodel.ImageLegend;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class DrawingUtils {


    public static void drawText(Graphics2D g2d, String headerString, boolean convertCaret) {


        Font origFont = g2d.getFont();

        int openParenthesisStartedSuper = 0;
        boolean currentIdxIsSuperScript = false;  // indicates whether current idx is a superscript
        boolean currentIdxIsSubScript = false;  // indicates whether current idx is a superscript
        boolean containsSuperSubScript = false;
        boolean italicsOverride = false;
        boolean boldOverride = false;
        boolean prevIdxNormal = true; // used to determine if subscript or superscript immediately follow normal
        boolean caratAwaitingEntry = false;

        if ((headerString.contains("^") && convertCaret) ||
                headerString.contains("<sup>") ||
                headerString.contains("</sup>") ||
                headerString.contains("<SUP>") ||
                headerString.contains("</SUP>") ||
                headerString.contains("<sub>") ||
                headerString.contains("</sub>") ||
                headerString.contains("<SUB>") ||
                headerString.contains("</SUB>")) {
            containsSuperSubScript = true;
        }

        for (int idx = 0; idx < headerString.length(); idx++) {
            boolean ignoreThisIdx = false;

            String charStringCurrent = headerString.substring(idx, idx + 1);
            char charCurrent = headerString.charAt(idx);

            if (charStringCurrent.equals("^") && convertCaret) {
                currentIdxIsSuperScript = true;
                caratAwaitingEntry = true;
                ignoreThisIdx = true;
            }

            if (isStartSuperScript(headerString, idx)) {
                ignoreThisIdx = true;
                currentIdxIsSuperScript = true;
            }

            if (isEndSuperScript(headerString, idx)) {
                ignoreThisIdx = true;
                currentIdxIsSuperScript = false;
            }

            if (isStartSubScript(headerString, idx)) {
                ignoreThisIdx = true;
                currentIdxIsSubScript = true;
            }

            if (isEndSubScript(headerString, idx)) {
                ignoreThisIdx = true;
                currentIdxIsSubScript = false;
            }


            if (isStartItalics(headerString, idx)) {
                ignoreThisIdx = true;
                italicsOverride = true;
            }

            if (isEndItalics(headerString, idx)) {
                ignoreThisIdx = true;
                italicsOverride = false;
            }

            if (isStartBold(headerString, idx)) {
                ignoreThisIdx = true;
                boldOverride = true;
            }

            if (isEndBold(headerString, idx)) {
                ignoreThisIdx = true;
                boldOverride = false;
            }


            if (!ignoreThisIdx) {
                if (Character.isWhitespace(charCurrent)) {
                    if (openParenthesisStartedSuper <= 0 && !caratAwaitingEntry) {
                        currentIdxIsSuperScript = false;
                    }
                } else {
                    if (caratAwaitingEntry) {
                        caratAwaitingEntry = false;
                    }
                }

                if (charStringCurrent.equals("(")) {
                    if (currentIdxIsSuperScript) {
                        openParenthesisStartedSuper++;
                    }
                }

                if (charStringCurrent.equals(")")) {
                    if (currentIdxIsSuperScript) {
                        if (openParenthesisStartedSuper > 0) {
                            openParenthesisStartedSuper--;
                        } else {
                            currentIdxIsSuperScript = false;
                        }
                    }
                }

                if (charStringCurrent.equals("(") || charStringCurrent.equals(")")) {
                    if (!currentIdxIsSuperScript && !currentIdxIsSubScript && containsSuperSubScript) {
                        int parenthesisFontSize = (int) Math.ceil(g2d.getFont().getSize() * 1.2);
                        Font parenthesisFont = new Font(g2d.getFont().getName(), g2d.getFont().getStyle(), parenthesisFontSize);
                        g2d.setFont(parenthesisFont);
                    }
                }


                if (italicsOverride) {
                    int fontType = ColorBarLayer.getFontType(true, g2d.getFont().isBold());
                    Font italicsFont = new Font(g2d.getFont().getName(), fontType, g2d.getFont().getSize());
                    g2d.setFont(italicsFont);
                }

                if (boldOverride) {
                    int fontType = ColorBarLayer.getFontType(g2d.getFont().isItalic(), true);
                    Font boldFont = new Font(g2d.getFont().getName(), fontType, g2d.getFont().getSize());
                    g2d.setFont(boldFont);
                }

                ImageLegend.FONT_SCRIPT font_script;
                if (currentIdxIsSuperScript) {
                    font_script = ImageLegend.FONT_SCRIPT.SUPER_SCRIPT;
                } else if (currentIdxIsSubScript) {
                    font_script = ImageLegend.FONT_SCRIPT.SUBSCRIPT;
                } else {
                    font_script = ImageLegend.FONT_SCRIPT.NORMAL;
                }

                // give a little space in front of subscript or superscript
                if ((currentIdxIsSuperScript || currentIdxIsSubScript) && prevIdxNormal) {
                    Rectangle2D singleLetter = g2d.getFontMetrics().getStringBounds("A", g2d);
                    double translateUnitsX = singleLetter.getWidth() * (0.1);
                    g2d.translate(translateUnitsX, 0);
                }

                drawHeaderSingleChar(g2d, charStringCurrent, font_script, true);

                g2d.setFont(origFont);

                if ((currentIdxIsSuperScript || currentIdxIsSubScript)) {
                    prevIdxNormal = false;
                } else {
                    prevIdxNormal = true;
                }

            }
        }
    }



    private static boolean isStartSubScript(String text, int idx) {
        return isStringOnIndex(text, idx, "<sub>") || isStringOnIndex(text, idx, "<SUB>");
    }

    private static boolean isEndSubScript(String text, int idx) {
        return isStringOnIndex(text, idx, "</sub>") || isStringOnIndex(text, idx, "</SUB>");
    }

    private static boolean isStartSuperScript(String text, int idx) {
        return isStringOnIndex(text, idx, "<sup>") || isStringOnIndex(text, idx, "<SUP>");
    }

    private static boolean isEndSuperScript(String text, int idx) {
        return isStringOnIndex(text, idx, "</sup>") || isStringOnIndex(text, idx, "</SUP>");
    }

    private static boolean isStartItalics(String text, int idx) {
        return  isStringOnIndex(text, idx, "<i>") || isStringOnIndex(text, idx, "<I>");
    }

    private static boolean isEndItalics(String text, int idx) {
        return isStringOnIndex(text, idx, "</i>") || isStringOnIndex(text, idx, "</I>");
    }

    private static boolean isStartBold(String text, int idx) {
        return isStringOnIndex(text, idx, "<b>") || isStringOnIndex(text, idx, "<B>");
    }

    private static boolean isEndBold(String text, int idx) {
        return  isStringOnIndex(text, idx, "</b>") || isStringOnIndex(text, idx, "</B>");
    }


    private static boolean isStringOnIndex(String text, int idx, String subtext) {

        if (text == null || subtext == null) {
            return false;
        }

        int offset = 0;

        for (int i = subtext.length(); i > 0; i--) {
            if (text.length() >= idx + i && idx >= offset) {
                String charStringCurrent = text.substring(idx - offset, idx + i);
                if (charStringCurrent.equals(subtext)) {
                    return true;
                }
            }

            offset++;
        }

        return false;
    }


    private static void drawHeaderSingleChar(Graphics2D g2d, String text, ImageLegend.FONT_SCRIPT fontScript, boolean draw) {

        double translateX = 0;
        double translateY = 0;

        if (fontScript == ImageLegend.FONT_SCRIPT.NORMAL) {
            if (draw) {
                g2d.drawString(text, 0, 0);
            }

            Rectangle2D textRectangle = g2d.getFontMetrics().getStringBounds(text, g2d);
            translateX = textRectangle.getWidth();
            g2d.translate(translateX, 0);
            return;
        }

        Font fontOrig = g2d.getFont();

        int fontSize;
        if (fontScript == ImageLegend.FONT_SCRIPT.SUPER_SCRIPT) {
//            int superScriptHeight = (int) Math.ceil(singleLetter.getHeight() * 0.3);
            int superScriptHeight = (int) Math.ceil(g2d.getFont().getSize() * 0.3);

            translateY = -superScriptHeight;
            fontSize = (int) Math.ceil(g2d.getFont().getSize() * 0.75);
        } else { // it is subscript
//            int subScriptHeight = (int) Math.ceil(singleLetter.getHeight() * 0.1);
            int subScriptHeight = (int) Math.ceil(g2d.getFont().getSize() * 0.2);
            translateY = subScriptHeight;
            fontSize = (int) Math.ceil(g2d.getFont().getSize() * 0.75);
        }

        g2d.translate(0, translateY);

        Font superScriptFont = new Font(g2d.getFont().getName(), g2d.getFont().getStyle(), fontSize);
        g2d.setFont(superScriptFont);

        if (draw) {
            g2d.drawString(text, 0, 0);
        }

        Rectangle2D textRectangle = g2d.getFontMetrics().getStringBounds(text, g2d);
        translateX = textRectangle.getWidth();
        translateY = -translateY;

        g2d.translate(translateX, translateY);

        g2d.setFont(fontOrig);
    }


}
