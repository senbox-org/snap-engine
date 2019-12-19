package org.esa.snap.core.dataio;

import org.esa.snap.core.datamodel.GeoCoding;

import java.io.IOException;
import java.nio.file.Path;
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
        private TreeSet<String> bandList = new TreeSet<>();
        private TreeSet<String> maskList = new TreeSet<>();

        private int productWidth;
        private int productHeight;

        private GeoCoding geoCoding;

        private boolean hasMasks;

        public TreeSet<String> getBandList() {
            return bandList;
        }

        public void setBandList(TreeSet<String> bandList) {
            this.bandList = bandList;
        }

        public TreeSet<String> getMaskList() {
            return maskList;
        }

        public void setMaskList(TreeSet<String> maskList) {
            this.maskList = maskList;
        }

        public boolean isHasMasks() {
            return hasMasks;
        }

        public void setHasMasks(boolean hasMasks) {
            this.hasMasks = hasMasks;
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
