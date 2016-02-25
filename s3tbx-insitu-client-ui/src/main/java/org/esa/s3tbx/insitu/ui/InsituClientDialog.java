package org.esa.s3tbx.insitu.ui;

import org.esa.snap.ui.ModelessDialog;
import org.openide.util.HelpCtx;

import java.awt.Window;

/**
 * @author Marco Peters
 */
public class InsituClientDialog extends ModelessDialog {

    public InsituClientDialog(Window parent, String title, String helpID) {
        super(parent, title, ID_OK | ID_CLOSE | ID_HELP, helpID);

        setContent(new InsituClientForm(new HelpCtx(helpID)));
    }

    @Override
    protected void onOK() {
    }

    @Override
    public void close() {
    }

    @Override
    public int show() {
        return super.show();
    }

}
