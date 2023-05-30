package com.bc.ceres.swing;

import java.awt.image.RGBImageFilter;

public class BrightBlueImageFilter extends RGBImageFilter {

    public BrightBlueImageFilter() {
        canFilterIndexColorModel = true;
    }

    @Override
    public int filterRGB(int x, int y, int rgb) {
        int a = (rgb & 0xff000000) >> 24;
        int r = (rgb & 0x00ff0000) >> 16;
        int g = (rgb & 0x0000ff00) >> 8;
        int b = rgb & 0x000000ff;
        int i = (r + g + b) / 3;
        r = g = i;
        b = 255;
        return a << 24 | r << 16 | g << 8 | b;
    }
}
