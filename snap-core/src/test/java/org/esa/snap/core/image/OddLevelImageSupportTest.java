package org.esa.snap.core.image;

import org.junit.Test;

import java.awt.Rectangle;

import static org.junit.Assert.assertEquals;


/**
 * @author Marco Peters
 */
public class OddLevelImageSupportTest {

    @Test
    public void testgetSourceRectangle() {
        int sourceWidth = 4481;
        int sourceHeight = 3265;
        LevelImageSupport lvlSupport = new LevelImageSupport(sourceWidth, sourceHeight, new ResolutionLevel(2, 4));
        Rectangle lvl2Rect = new Rectangle(561, 409, 560, 408);
        Rectangle sourceRect = lvlSupport.getSourceRectangle(lvl2Rect);

        assertEquals(sourceWidth, sourceRect.x + sourceRect.width);
        assertEquals(sourceHeight, sourceRect.y + sourceRect.height);

    }
}