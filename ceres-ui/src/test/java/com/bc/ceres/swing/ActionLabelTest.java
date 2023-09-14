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

package com.bc.ceres.swing;

import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

import static org.junit.Assert.*;

/**
 * A {@link javax.swing.JLabel} which fires action events when clicked.
 */
public class ActionLabelTest {

    public static void main(String[] args) {
        final JComponent panel = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(getBackground());
                final Rectangle bounds = getBounds();
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        };
        assertFalse(panel.isOpaque());
        panel.setBackground(new Color(0, 255, 0, 127));
        panel.add(new JButton("Button 1"));
        panel.add(new JLabel("Normal label 1"));
        panel.add(new ActionLabel("Action label 1", e -> System.out.println("e = " + e)));
        panel.add(new JLabel("Normal label 2"));
        panel.add(new ActionLabel("Action label 2", e -> System.out.println("e = " + e)));
        panel.add(new JButton("Button 2"));

        final JPanel containerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                final int w = getWidth();
                final int h = getHeight();
                for (int y = 8; y < h; y += 16) {
                    g.drawLine(0, y, w, y);
                }
                for (int x = 8; x < w; x += 16) {
                    g.drawLine(x, 0, x, h);
                }
            }
        };
        containerPanel.setBackground(new Color(255, 255, 255, 127));
        containerPanel.add(panel, BorderLayout.CENTER);

        final JFrame frame = new JFrame(ActionLabel.class.getSimpleName());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(containerPanel, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
    }

    @Test
    public void testConstructors() {
        ActionLabel label = new ActionLabel();
        assertNull(label.getText());
        assertNotNull(label.getActionListeners());
        assertEquals(0, label.getActionListeners().length);

        label = new ActionLabel("X");
        assertEquals("X", label.getText());
        assertNotNull(label.getActionListeners());
        assertEquals(0, label.getActionListeners().length);

        final ActionListener testAction = e -> {
        };
        label = new ActionLabel("Y", testAction);
        assertEquals("Y", label.getText());
        assertNotNull(label.getActionListeners());
        assertEquals(1, label.getActionListeners().length);
        assertSame(testAction, label.getActionListeners()[0]);
    }
}
