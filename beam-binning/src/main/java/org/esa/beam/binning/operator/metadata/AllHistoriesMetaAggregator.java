package org.esa.beam.binning.operator.metadata;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;

class AllHistoriesMetaAggregator extends AbstractMetadataAggregator {

    @Override
    public void aggregateMetadata(Product product) {
        final String productName = Utilities.extractProductName(product);
        final MetadataElement productElement = Utilities.createInputMetaElement(productName, aggregatedCount);

        final MetadataElement processingGraphElement = Utilities.getProcessingGraphElement(product);
        if (processingGraphElement != null) {
            productElement.addElement(processingGraphElement.createDeepClone());
        }
        inputsMetaElement.addElementAt(productElement, aggregatedCount);
        ++aggregatedCount;
    }

    @Override
    public MetadataElement getMetadata() {
        return inputsMetaElement;
    }
}
