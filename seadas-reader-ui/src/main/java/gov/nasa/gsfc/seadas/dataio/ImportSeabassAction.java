/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package gov.nasa.gsfc.seadas.dataio;

import org.esa.snap.framework.datamodel.GeoCoding;
import org.esa.snap.framework.datamodel.PlacemarkDescriptor;
import org.esa.snap.framework.datamodel.PlacemarkDescriptorRegistry;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductNode;
import org.esa.snap.framework.datamodel.VectorDataNode;
import org.esa.snap.framework.ui.product.ProductSceneView;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;


/**
 * Action that lets a user load a SeaBASS file.
 *
 * @author Don Shea
 * @since SeaDAS 7.0
 * @see <a href="http://seabass.gsfc.nasa.gov/wiki/article.cgi?article=Data_Submission#Data%20Format">SeaBASS File Format Description</a>
 */
@ActionID(
        category = "File",
        id = "gov.nasa.gsfc.seadas.dataio.ImportSeabassAction"
)
@ActionRegistration(
        displayName = "#CTL_ImportSeabassActionText",
        popupText = "#CTL_ImportSeabassActionText"
)
@ActionReference(
        path = "Menu/File/Import/Vector Data"
)
@NbBundle.Messages({
        "CTL_ImportSeabassActionText=SeaBASS Data",
        "CTL_ImportSeabassDialogTitle=Open SeaBASS File",
        "CTL_ImportSeabassDescription=Import SeaBASS Data."
})
public class ImportSeabassAction extends AbstractSnapAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public ImportSeabassAction() {
        this(Utilities.actionsGlobalContext());
    }

    public ImportSeabassAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        setEnableState();
        putValue(Action.NAME, Bundle.CTL_ImportSeabassActionText());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_ImportSeabassDescription());
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new ImportSeabassAction(actionContext);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        setEnableState();
    }

    @Override
    public void actionPerformed(ActionEvent event) {

        File file = SnapDialogs.requestFileForOpen(Bundle.CTL_ImportSeabassDialogTitle(), false, null, "importSeabass.lastDir");
        if (file == null) {
            return;
        }

        Product product = getAppContext().getSelectedProduct();

        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection;
        try {
            featureCollection = readTrack(file, product.getGeoCoding());
        } catch (Exception e) {
            SnapDialogs.showError(Bundle.CTL_ImportSeabassDialogTitle(), "Failed to load SeaBASS file:\n" + e.getMessage());
            return;
        }

        if (featureCollection.isEmpty()) {
            SnapDialogs.showError(Bundle.CTL_ImportSeabassDialogTitle(), "No records found.");
            return;
        }

        String name = file.getName();
        final PlacemarkDescriptor placemarkDescriptor = PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptor(featureCollection.getSchema());
        placemarkDescriptor.setUserDataOf(featureCollection.getSchema());
        VectorDataNode vectorDataNode = new VectorDataNode(name, featureCollection, placemarkDescriptor);

        product.getVectorDataGroup().add(vectorDataNode);

        final ProductSceneView view = getAppContext().getSelectedProductSceneView();
        if (view != null) {
            view.setLayersVisible(vectorDataNode);
        }
    }

    public void setEnableState() {
        boolean state = false;
        ProductNode productNode = lkp.lookup(ProductNode.class);
        if (productNode != null) {
            Product product = productNode.getProduct();
            state = product != null && product.getGeoCoding() != null;
        }
        setEnabled(state);
    }

    private static FeatureCollection<SimpleFeatureType, SimpleFeature> readTrack(File file, GeoCoding geoCoding) throws IOException {
        try (Reader reader = new FileReader(file)) {
            return readTrack(reader, geoCoding);
        }
    }

    static FeatureCollection<SimpleFeatureType, SimpleFeature> readTrack(Reader reader, GeoCoding geoCoding) throws IOException {
        SeabassReader seabassReader = new SeabassReader(reader, geoCoding);
        return seabassReader.createFeatureCollection();
    }

}
