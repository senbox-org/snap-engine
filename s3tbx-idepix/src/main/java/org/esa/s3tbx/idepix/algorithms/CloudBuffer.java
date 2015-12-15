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

package org.esa.s3tbx.idepix.algorithms;


import org.esa.snap.core.gpf.Tile;

import java.awt.*;

/**
 * cloud buffer algorithms
 */
public class CloudBuffer {

    public static void computeSimpleCloudBuffer(int x, int y,
                                                Tile sourceFlagTile, Tile targetTile,
                                                int cloudBufferWidth,
                                                int cloudFlagBit, int cloudBufferFlagBit) {
        Rectangle rectangle = targetTile.getRectangle();
        int LEFT_BORDER = Math.max(x - cloudBufferWidth, rectangle.x);
        int RIGHT_BORDER = Math.min(x + cloudBufferWidth, rectangle.x + rectangle.width - 1);
        int TOP_BORDER = Math.max(y - cloudBufferWidth, rectangle.y);
        int BOTTOM_BORDER = Math.min(y + cloudBufferWidth, rectangle.y + rectangle.height - 1);

        for (int i = LEFT_BORDER; i <= RIGHT_BORDER; i++) {
            for (int j = TOP_BORDER; j <= BOTTOM_BORDER; j++) {
                boolean is_already_cloud = sourceFlagTile.getSampleBit(i, j, cloudFlagBit);
                if (!is_already_cloud && rectangle.contains(i, j)) {
                    targetTile.setSample(i, j, cloudBufferFlagBit, true);
                }
            }
        }
    }

    public static void computeCloudBufferLC(Tile targetTile, int cloudFlagBit, int cloudBufferFlagBit) {
        //  set alternative cloud buffer flag as used in LC-CCI project:
        // 1. use 2x2 square with reference pixel in upper left
        // 2. move this square row-by-row over the tile
        // 3. if reference pixel is not clouds, don't do anything
        // 4. if reference pixel is cloudy:
        //    - if 2x2 square only has cloud pixels, then set cloud buffer of two pixels
        //      in both x and y direction of reference pixel.
        //    - if 2x2 square also has non-cloudy pixels, do the same but with cloud buffer of only 1

        Rectangle rectangle = targetTile.getRectangle();
        for (int y = rectangle.y; y < rectangle.y + rectangle.height - 1; y++) {
            for (int x = rectangle.x; x < rectangle.x + rectangle.width - 1; x++) {
                if (targetTile.getSampleBit(x, y, cloudFlagBit)) {
                    // reference pixel is upper left (x, y)
                    // first set buffer of 1 in each direction
                    int bufferWidth = 1;
                    int LEFT_BORDER = Math.max(x - bufferWidth, rectangle.x);
                    int RIGHT_BORDER = Math.min(x + bufferWidth, rectangle.x + rectangle.width - 1);
                    int TOP_BORDER = Math.max(y - bufferWidth, rectangle.y);
                    int BOTTOM_BORDER = Math.min(y + bufferWidth, rectangle.y + rectangle.height - 1);
                    // now check if whole 2x2 square (x+1,y), (x, y+1), (x+1, y+1) is cloudy
                    if (targetTile.getSampleBit(x + 1, y, cloudFlagBit) &&
                            targetTile.getSampleBit(x, y + 1, cloudFlagBit) &&
                            targetTile.getSampleBit(x + 1, y + 1, cloudFlagBit)) {
                        // set buffer of 2 in each direction
                        bufferWidth = 2;
                        LEFT_BORDER = Math.max(x - bufferWidth, rectangle.x);
                        RIGHT_BORDER = Math.min(x + 1 + bufferWidth, rectangle.x + rectangle.width - 1);
                        TOP_BORDER = Math.max(y - bufferWidth, rectangle.y);
                        BOTTOM_BORDER = Math.min(y + 1 + bufferWidth, rectangle.y + rectangle.height - 1);
                    }
                    for (int i = LEFT_BORDER; i <= RIGHT_BORDER; i++) {
                        for (int j = TOP_BORDER; j <= BOTTOM_BORDER; j++) {
                            targetTile.setSample(i, j, cloudBufferFlagBit, true);
                        }
                    }
                }
            }
        }
        int bufferWidth = 1;

        // south tile boundary...
        final int ySouth = rectangle.y + rectangle.height - 1;
        for (int x = rectangle.x; x < rectangle.x + rectangle.width - 1; x++) {
            int LEFT_BORDER = Math.max(x - bufferWidth, rectangle.x);
            int RIGHT_BORDER = Math.min(x + bufferWidth, rectangle.x + rectangle.width - 1);
            int TOP_BORDER = Math.max(rectangle.y, ySouth - bufferWidth);
            if (targetTile.getSampleBit(x, ySouth, cloudFlagBit)) {
                for (int i = LEFT_BORDER; i <= RIGHT_BORDER; i++) {
                    for (int j = TOP_BORDER; j <= ySouth; j++) {
                        targetTile.setSample(i, j, cloudBufferFlagBit, true);
                    }
                }
            }
        }

        // east tile boundary...
        final int xEast = rectangle.x + rectangle.width - 1;
        for (int y = rectangle.y; y < rectangle.y + rectangle.height - 1; y++) {
            int LEFT_BORDER = Math.max(rectangle.x, xEast - bufferWidth);
            int TOP_BORDER = Math.max(y - bufferWidth, rectangle.y);
            int BOTTOM_BORDER = Math.min(y + bufferWidth, rectangle.y + rectangle.height - 1);
            if (targetTile.getSampleBit(xEast, y, cloudFlagBit)) {
                for (int i = LEFT_BORDER; i <= xEast; i++) {
                    for (int j = TOP_BORDER; j <= BOTTOM_BORDER; j++) {
                        targetTile.setSample(i, j, cloudBufferFlagBit, true);
                    }
                }
            }
        }
        // pixel in lower right corner...
        if (targetTile.getSampleBit(xEast, ySouth, cloudFlagBit)) {
            for (int i = Math.max(rectangle.x, xEast - 1); i <= xEast; i++) {
                for (int j = Math.max(rectangle.y, ySouth - 1); j <= ySouth; j++) {
                    targetTile.setSample(i, j, cloudBufferFlagBit, true);
                }
            }
        }
    }
}
