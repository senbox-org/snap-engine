package org.esa.snap.core.metadata;

import org.esa.snap.core.datamodel.GeoCoding;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

/**
 * Interface to be implemented by all simple metadata inspectors.
 *
 * @author Denisa Stefanescu
 */

public interface MetadataInspector {
    /**
     * Parses the metadata for the given product path
     */
    Metadata getMetadata(Path productPath) throws IOException;

    class Metadata {

        private final Set<String> bandList;
        private final Set<String> maskList;

        private int productWidth;
        private int productHeight;
        private GeoCoding geoCoding;

        public Metadata() {
            this(0, 0);
        }

        public Metadata(int productWidth, int productHeight) {
            this.bandList = new TreeSet<>();
            this.maskList = new TreeSet<>();

            setProductWidth(productWidth);
            setProductHeight(productHeight);
        }

        public void addBandName(String bandName) {
            this.bandList.add(bandName);
        }

        public void addMaskName(String maskName) {
            this.maskList.add(maskName);
        }

        public Set<String> getBandList() {
            return bandList;
        }

        public Set<String> getMaskList() {
            return maskList;
        }

        public boolean isHasMasks() {
            return !maskList.isEmpty();
        }

        public boolean isHasGeoCoding() {
            return geoCoding != null;
        }

        public int getProductWidth() {
            return productWidth;
        }

        public void setProductWidth(int productWidth) {
            this.productWidth = productWidth;
        }

        public int getProductHeight() {
            return productHeight;
        }

        public void setProductHeight(int productHeight) {
            this.productHeight = productHeight;
        }

        public GeoCoding getGeoCoding() {
            return geoCoding;
        }

        public void setGeoCoding(GeoCoding geoCoding) {
            this.geoCoding = geoCoding;
        }
    }
}
