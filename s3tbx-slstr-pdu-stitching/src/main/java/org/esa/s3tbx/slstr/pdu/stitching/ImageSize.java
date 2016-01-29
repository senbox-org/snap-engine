package org.esa.s3tbx.slstr.pdu.stitching;

/**
 * @author Tonio Fincke
 */
public class ImageSize {

    private final String identifier;
    private final int startOffset;
    private final int trackOffset;
    private final int rows;
    private final int columns;

    public ImageSize(String identifier, int startOffset, int trackOffset, int rows, int columns) {
        this.identifier = identifier;
        this.startOffset = startOffset;
        this.trackOffset = trackOffset;
        this.rows = rows;
        this.columns = columns;
    }

    public String getIdentifier() {
        return identifier;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getTrackOffset() {
        return trackOffset;
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ImageSize)) {
            return false;
        }
        ImageSize imageSize = (ImageSize) object;
        return identifier.equals(imageSize.getIdentifier()) &&
                    startOffset == imageSize.startOffset &&
                    trackOffset == imageSize.trackOffset &&
                    rows == imageSize.rows &&
                    columns == imageSize.columns;
    }
}
