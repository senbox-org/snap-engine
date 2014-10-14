package org.esa.beam.binning.operator.metadata;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;

class ProductNameMetaAggregator extends AbstractMetadataAggregator {

    public MetadataElement getMetadata() {
        return inputsMetaElement;
    }

    public void aggregateMetadata(Product product) {
        final String productName = Utilities.extractProductName(product);

        final MetadataElement productElement = Utilities.createInputMetaElement(productName, aggregatedCount);

        inputsMetaElement.addElementAt(productElement, aggregatedCount);
        ++aggregatedCount;
    }

}
