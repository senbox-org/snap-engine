package org.esa.beam.binning.operator.metadata;


import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;

class FirstHistoryMetaAggregator extends AbstractMetadataAggregator {

    @Override
    public void aggregateMetadata(Product product) {
        final MetadataElement productElement = Utilities.createInputMetaElement(product.getName(), aggregatedCount);

        final MetadataElement processingGraphElement = Utilities.getProcessingGraphElement(product);
        if (processingGraphElement != null && aggregatedCount == 0) {
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
