/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.s3tbx.about;

import javax.swing.JLabel;
import org.esa.snap.rcp.about.AboutBox;

/**
 * @author Norman
 */
@AboutBox(displayName = "S3TBX", position = 30)
public class S3tbxAboutBox extends JLabel {
    
    public S3tbxAboutBox() {
        super("<html>This is the fantastic <b>Sentinel-3 Toolbox</b>");
    }
}
