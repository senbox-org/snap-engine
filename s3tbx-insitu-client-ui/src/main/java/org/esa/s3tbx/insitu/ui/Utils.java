package org.esa.s3tbx.insitu.ui;

import org.esa.snap.rcp.util.ProgressHandleMonitor;
import org.netbeans.api.progress.ProgressUtils;

import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Marco Peters
 */
class Utils {

    private  Utils() {
    }

    static void runWithProgress(Runnable runnable, ProgressHandleMonitor handle) {
        ProgressUtils.runOffEventThreadWithProgressDialog(runnable,
                                                          "In-Situ Client",
                                                          handle.getProgressHandle(),
                                                          true,
                                                          50,
                                                          1000);
    }

    static <T> List<T> getSelectedItems(DefaultListModel<T> listModel, ListSelectionModel selectionModel) {
        int iMin = selectionModel.getMinSelectionIndex();
        int iMax = selectionModel.getMaxSelectionIndex();

        final ArrayList<T> itemList = new ArrayList<>();
        if (iMin < 0 || iMax < 0) {
            return itemList;
        }

        for (int i = iMin; i <= iMax; i++) {
            if (selectionModel.isSelectedIndex(i)) {
                itemList.add(listModel.get(i));
            }
        }

        return itemList;
    }

}
