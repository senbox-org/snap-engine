package org.esa.s3tbx;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;

/**
 * @author Norman Fomferra
 */
public class ContextSearchAction extends ExecCommand {

    public static final String ID = ContextSearchAction.class.getName();

    private ContextSearch contextSearch;

    public void setContextSearch(ContextSearch contextSearch) {
        this.contextSearch = contextSearch;
    }

    @Override
    public void actionPerformed(final CommandEvent event) {
        if (contextSearch != null) {
            contextSearch.searchForNode(VisatApp.getApp().getSelectedProductNode());
        }
    }

    @Override
    public void updateState(final CommandEvent event) {
        setEnabled(contextSearch != null && VisatApp.getApp().getSelectedProductNode() != null);
    }
}
