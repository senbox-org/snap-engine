package org.esa.beam.binning.operator.metadata;


import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

class Utilities {

    static MetadataElement createInputMetaElement(String productName, int index) {
        final MetadataElement productElement = new MetadataElement("input." + Integer.toString(index));
        final MetadataAttribute nameAttribute = new MetadataAttribute("name", new ProductData.ASCII(productName), true);
        productElement.addAttribute(nameAttribute);
        return productElement;
    }

    static MetadataElement getProcessingGraphElement(Product product) {
        final MetadataElement metadataRoot = product.getMetadataRoot();
        return metadataRoot.getElement("Processing_Graph");
    }

    static String extractProductName(Product product) {
        final MetadataElement processingGraphElement = getProcessingGraphElement(product);

        String productName = product.getName();
        if (processingGraphElement != null) {
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
        return productName;
    }
}
