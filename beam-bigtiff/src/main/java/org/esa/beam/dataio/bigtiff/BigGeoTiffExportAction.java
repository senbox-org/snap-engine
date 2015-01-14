package org.esa.beam.dataio.bigtiff;

import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.ProductExportAction;

import javax.swing.*;
import java.io.File;

public class BigGeoTiffExportAction extends ProductExportAction {

    @Override
    protected File promptForFile(Product product) {
        final GeoCoding geoCoding = product.getGeoCoding();
        // @todo 1 tb/tb remove mapgeocoding when migrating to SNAP 2015-01-14
        if (!(geoCoding instanceof MapGeoCoding) && !(geoCoding instanceof CrsGeoCoding)) {
            final String message = String.format("The product %s is not reprojected to a map.\n" +
                            "Un-projected raster data is not well supported by other GIS software.\n" +
                            "\n" +
                            "Do you want to export the product without a reprojection?",
                    product.getName());
            final int answer = JOptionPane.showConfirmDialog(VisatApp.getApp().getMainFrame(), message, getText(),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) {
                return null;
            }
        }
        return super.promptForFile(product);
    }
}
