package com.bc.ceres.swing;

import static org.junit.Assert.*;

import org.junit.*;

/**
 * Created by Sabine on 18.11.2015.
 */
public class SwingHelperTest {

    @Test
    public void testCreateWordWrappedHtmlTextForSwingComponents() {
        String text;
        String expected;

        assertEquals("<html>Text</html>", SwingHelper.createWordWrappedHtmlTextForSwingComponents("Text", 29));

        text = "Text Text Text Text Text Text Text Text";
        expected = "<html>Text Text Text Text Text Text<br>Text Text</html>";
        assertEquals(expected, SwingHelper.createWordWrappedHtmlTextForSwingComponents(text, 29));

        text = "Text Text Text Text Text Text Text Text";
        expected = "<html>Text Text Text Text Text Text <br>Text Text</html>";
        assertEquals(expected, SwingHelper.createWordWrappedHtmlTextForSwingComponents(text, 30));
    }
}