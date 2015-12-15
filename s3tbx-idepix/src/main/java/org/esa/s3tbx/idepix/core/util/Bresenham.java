package org.esa.s3tbx.idepix.core.util;

import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.util.math.MathUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Bresenham's famous line drawing algorithm. Works for 2D.
 */
public class Bresenham {

    /**
     * Used for calculation
     */
    private int dx, dy, error, x_inc, y_inc, xx, yy, length, count;

    /**
     * Construct a Bresenham algorithm.
     */
    public Bresenham() {
    }

    /**
     * Plot a line between (x1,y1) and (x2,y2). To step through the line use next().
     *
     * @return the length of the line (which will be 1 more than you are expecting).
     */
    public int plot(int x1, int y1, int x2, int y2) {
        /*
      The start and end of the line
     */

        // compute horizontal and vertical deltas
        dx = x2 - x1;
        dy = y2 - y1;

        // test which direction the line is going in i.e. slope angle
        if (dx >= 0) {
            x_inc = 1;
        } else {
            x_inc = -1;
            dx = -dx; // need absolute value
        }

        // test y component of slope

        if (dy >= 0) {
            y_inc = 1;
        } else {
            y_inc = -1;
            dy = -dy; // need absolute value
        }

        xx = x1;
        yy = y1;

        if (dx > 0)
            error = dx >> 1;
        else
            error = dy >> 1;

        count = 0;
        length = Math.max(dx, dy) + 1;
        return length;
    }

    /**
     * Get the next point in the line. You must not call next() if the
     * previous invocation of next() returned false.
     * <p/>
     * Retrieve the X and Y coordinates of the line with getX() and getY().
     *
     * @return true if there is another point to come.
     */
    public boolean next() {
        // now based on which delta is greater we can draw the line
        if (dx > dy) {
            // adjust the error term
            error += dy;

            // test if error has overflowed
            if (error >= dx) {
                error -= dx;

                // move to next line
                yy += y_inc;
            }

            // move to the next pixel
            xx += x_inc;
        } else {
            // adjust the error term
            error += dx;

            // test if error overflowed
            if (error >= dy) {
                error -= dy;

                // move to next line
                xx += x_inc;
            }

            // move to the next pixel
            yy += y_inc;
        }

        count++;
        return count < length;
    }

    /**
     * @return the current X coordinate
     */
    public int getX() {
        return xx;
    }

    /**
     * @return the current Y coordinate
     */
    public int getY() {
        return yy;
    }

    /**
     * Provides the path pixels between (x1,y1) and (x2,y2).
     *
     * @param x1 - first x coordinate
     * @param y1 - first y coordinate
     * @param x2 - second x coordinate
     * @param y2 - second y coordinate
     * @param rect - rectangle where the pixels need to be inside
     *
     * @return the list of pixel positions
     */
    public static List<PixelPos> getPathPixels(final int x1, final int y1, final int x2, final int y2, Rectangle rect) {

        List<PixelPos> path = new ArrayList<PixelPos>();
        Bresenham bresenham = new Bresenham();
        int length = bresenham.plot(x1, y1, x2, y2);
        for (int i = 0; i < length; i++) {
            bresenham.next();
            PixelPos pos = new PixelPos(bresenham.getX(), bresenham.getY());
            if (rect.contains(bresenham.getX(), bresenham.getY())) {
                path.add(pos);
            }
        }

        return path;

    }

    /**
     * finds the image border pixel if walking from given pixel (x,y) on a straight line
     * under given angle towards image boundary. The angle counts in mathematical sense,
     * i.e. 0 deg is towards east, 90 deg towards north, 180deg towards west, and 270deg
     * towards south
     *
     * @param x      - pixel x coordinate
     * @param y      - pixel y coordinate
     * @param rect  - image/tile rectangle
     * @param angle  in degrees
     * @return the pixel position of the border pixel
     */
    public static PixelPos findBorderPixel(int x, int y, Rectangle rect, double angle) {
        // remove x and y offsets
        PixelPos pos2 = findBorderPixel(x - rect.x, y - rect.y, rect.width, rect.height, angle);
        int xpos = ((int) pos2.getX()) + rect.x;
        int ypos = ((int) pos2.getY()) + rect.y;

        return new PixelPos(xpos, ypos);
    }

    private static PixelPos findBorderPixel(int x, int y, int width, int height, double angle) {
        final int w = width - 1;
        final int h = height - 1;
        int xPos = -1;
        int yPos = -1;

        // find angles to corner pixels (in degrees)
        // upper right, first quadrant
        double urAngle = MathUtils.RTOD * Math.atan(y * 1.0 / (w - x));
        if (angle <= urAngle) {
            // border pixel is in first octant
            xPos = w;
            yPos = (int) Math.round(y - (w - x) * Math.tan(MathUtils.DTOR * angle));

        } else if (angle > urAngle && angle < 90.0) {
            // border pixel is in second octant
            xPos = (int) Math.round(x + y / Math.tan(MathUtils.DTOR * angle));
            yPos = 0;
        } else {
            // check second quadrant
            double ulAngle = 90.0 + MathUtils.RTOD * Math.atan(x * 1.0 / y);
            if (angle > 90.0 && angle <= ulAngle) {
                // border pixel is in third octant
                xPos = (int) Math.round(x - y * Math.tan(MathUtils.DTOR * (angle - 90.0)));
                yPos = 0;
            } else if (angle > ulAngle && angle < 180.0) {
                // border pixel is in fourth octant
                xPos = 0;
                yPos = (int) Math.round(y - x / Math.tan(MathUtils.DTOR * (angle - 90.0)));
            } else {
                // check third quadrant
                double llAngle = 180.0 + MathUtils.RTOD * Math.atan((h - y) * 1.0 / x);
                if (angle > 180.0 && angle <= llAngle) {
                    // border pixel is in fifth octant
                    xPos = 0;
                    yPos = (int) Math.round(y + x * Math.tan(MathUtils.DTOR * (angle - 180.0)));
                } else if (angle > llAngle && angle < 270.0) {
                    // border pixel is in sixth octant
                    xPos = (int) Math.round(x + (y - h) / Math.tan(MathUtils.DTOR * (angle - 180.0)));
                    yPos = h;
                } else {
                    // check fourth quadrant
                    double lrAngle = 270.0 + MathUtils.RTOD * Math.atan((w - x) * 1.0 / (h - y));
                    if (angle > 270.0 && angle <= lrAngle) {
                        // border pixel is in seventh octant
                        xPos = (int) Math.round(x + (h - y) * Math.tan(MathUtils.DTOR * (angle - 270.0)));
                        yPos = h;
                    } else if (angle > lrAngle && angle < 360.0) {
                        // border pixel is in eighth octant
                        xPos = w;
                        yPos = (int) Math.round(y + (w - x) / Math.tan(MathUtils.DTOR * (angle - 270.0)));
                    } else {
                        // special cases (for infinite tangens arguments)
                        if (angle == 90.0) {
                            xPos = x;
                            yPos = 0;
                        } else if (angle == 180.0) {
                            xPos = 0;
                            yPos = y;
                        } else if (angle == 270.0) {
                            xPos = x;
                            yPos = h;
                        } else if (angle == 360.0 || angle == 0.0) {
                            xPos = w;
                            yPos = y;
                        }
                    }
                }
            }
        }
        return new PixelPos(xPos, yPos);
    }
}
