package org.esa.snap.core.dataio;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

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
        private Set<String> bandList = new HashSet<>();
        private Set<String> maskList = new HashSet<>();

        private String productWidth;
        private String productHeight;

        private String latitudeNorth;
        private String longitudeWest;
        private String latitudeSouth;
        private String longitudeEast;

        private GeoCoding geoCoding;

        private boolean hasMasks;

        //TODO Jean for Denisa: remove 'hasGeoCoding' and check if (geoCoding != null)
        private boolean hasGeoCoding;

        public Set<String> getBandList() {
            return bandList;
        }

        public void setBandList(Set<String> bandList) {
            this.bandList = bandList;
        }

        public Set<String> getMaskList() {
            return maskList;
        }

        public void setMaskList(Set<String> maskList) {
            this.maskList = maskList;
        }

        public boolean isHasMasks() {
            return hasMasks;
        }

        public void setHasMasks(boolean hasMasks) {
            this.hasMasks = hasMasks;
        }

        public boolean isHasGeoCoding() {
            return hasGeoCoding;
        }

        public void setHasGeoCoding(boolean hasGeoCoding) {
            this.hasGeoCoding = hasGeoCoding;
        }

        public String getProductWidth() {
            return productWidth;
        }

        public void setProductWidth(String productWidth) {
            this.productWidth = productWidth;
        }

        public String getProductHeight() {
            return productHeight;
        }

        public void setProductHeight(String productHeight) {
            this.productHeight = productHeight;
        }

        public String getLatitudeNorth() {
            return latitudeNorth;
        }

        public void setLatitudeNorth(String latitudeNorth) {
            this.latitudeNorth = latitudeNorth;
        }

        public String getLongitudeWest() {
            return longitudeWest;
        }

        public void setLongitudeWest(String longitudeWest) {
            this.longitudeWest = longitudeWest;
        }

        public String getLatitudeSouth() {
            return latitudeSouth;
        }

        public void setLatitudeSouth(String latitudeSouth) {
            this.latitudeSouth = latitudeSouth;
        }

        public String getLongitudeEast() {
            return longitudeEast;
        }

        public void setLongitudeEast(String longitudeEast) {
            this.longitudeEast = longitudeEast;
        }

        public GeoCoding getGeoCoding() {
            return geoCoding;
        }

        public void setGeoCoding(GeoCoding geoCoding) {
            this.geoCoding = geoCoding;
        }
    }
}
