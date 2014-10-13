package org.esa.beam.binning.operator.metadata;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;

class ProductNameMetaAggregator extends AbstractMetadataAggregator {

    public MetadataElement getMetadata() {
        return source_products;
    }

    public void aggregateMetadata(Product product) {
        final MetadataElement processingGraphElement = Utilities.getProcessingGraphElement(product);

        String productName = "unknown";
        if (processingGraphElement == null) {
            productName = product.getName();
        } else {
            final MetadataElement nodeElement = processingGraphElement.getElement("node.0");
            if (nodeElement != null) {
                final MetadataElement sourcesElement = nodeElement.getElement("sources");
                if (sourcesElement != null) {
                    final MetadataAttribute sourceProductAttribute = sourcesElement.getAttribute("sourceProduct");
                    if (sourceProductAttribute != null) {
                        productName = sourceProductAttribute.getData().getElemString();
                    }
                }
            }
        }

        final MetadataElement productElement = Utilities.createInputMetaElement(productName, aggregatedCount);

        source_products.addElementAt(productElement, aggregatedCount);
        ++aggregatedCount;
    }
}
