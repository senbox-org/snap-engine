/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.s3tbx.about;

import org.esa.snap.rcp.about.AboutBox;
import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;

/**
 * @author Norman
 */
@AboutBox(displayName = "S3TBX", position = 30)
public class S3tbxAboutBox extends JPanel {

    public S3tbxAboutBox() {
        super(new BorderLayout(4, 4));
        setBorder(new EmptyBorder(4, 4, 4, 4));
        ModuleInfo moduleInfo = Modules.getDefault().ownerOf(S3tbxAboutBox.class);
        ImageIcon aboutImage = new ImageIcon(S3tbxAboutBox.class.getResource("about_s3tbx.jpg"));
        JLabel iconLabel = new JLabel(aboutImage);
        add(iconLabel, BorderLayout.CENTER);
        add(new JLabel("<html><b>Sentinel-3 Toolbox (S3TBX) version " + moduleInfo.getImplementationVersion() + "</b>", SwingConstants.RIGHT), BorderLayout.SOUTH);
    }
}
