package org.esa.snap.core.datamodel;

import java.util.Arrays;
import javax.media.jai.UnpackedImageData;
import org.esa.snap.core.util.math.DoubleList;

public class QualitativeStxOp extends StxOp {

    public final static String NO_MAJORITY_CLASS = "";

    private final IndexCoding indexCoding;
    private final int[] numClassMembers;
    private int totalNumClassMembers;
    private String majorityClass;
    private String secondMajorityClass;
    private String[] indexNames;

    public QualitativeStxOp(IndexCoding indexCoding) {
        super("Qualitative");
        this.indexCoding = indexCoding;
        indexNames = indexCoding.getIndexNames();
        numClassMembers = new int[indexCoding.getNumAttributes()];
        Arrays.fill(numClassMembers, 0);
        totalNumClassMembers = 0;
        this.majorityClass = NO_MAJORITY_CLASS;
        this.secondMajorityClass = NO_MAJORITY_CLASS;
    }

    public int getNumberOfMembers(int index) {
        return numClassMembers[index];
    }

    public String getMajorityClass() {
        return majorityClass;
    }

    public String getSecondMajorityClass() {
        return secondMajorityClass;
    }

    public int getTotalNumClassMembers() {
        return totalNumClassMembers;
    }

    public String[] getClassNames() {
        return indexNames;
    }

    @Override
    public void accumulateData(UnpackedImageData dataPixels,
                               UnpackedImageData maskPixels) {

        // Do not change this code block without doing the same changes in HistogramStxOp.java
        // {{ Block Start

        final DoubleList values = asDoubleList(dataPixels);

        final int dataPixelStride = dataPixels.pixelStride;
        final int dataLineStride = dataPixels.lineStride;
        final int dataBandOffset = dataPixels.bandOffsets[0];

        byte[] mask = null;
        int maskPixelStride = 0;
        int maskLineStride = 0;
        int maskBandOffset = 0;
        if (maskPixels != null) {
            mask = maskPixels.getByteData(0);
            maskPixelStride = maskPixels.pixelStride;
            maskLineStride = maskPixels.lineStride;
            maskBandOffset = maskPixels.bandOffsets[0];
        }

        final int width = dataPixels.rect.width;
        final int height = dataPixels.rect.height;

        int dataLineOffset = dataBandOffset;
        int maskLineOffset = maskBandOffset;

        // }} Block End

        int[] indexValues = new int[indexCoding.getNumAttributes()];
        for (int i = 0; i < indexCoding.getNumAttributes(); i++) {
            indexValues[i] = indexCoding.getIndexValue(indexNames[i]);
        }
        for (int y = 0; y < height; y++) {
            int dataPixelOffset = dataLineOffset;
            int maskPixelOffset = maskLineOffset;
            for (int x = 0; x < width; x++) {
                if (mask == null || mask[maskPixelOffset] != 0) {
                    int value = (int) values.getDouble(dataPixelOffset);
                    for (int i = 0; i < indexCoding.getNumAttributes(); i++) {
                        if (value == indexValues[i]) {
                            numClassMembers[i]++;
                            totalNumClassMembers++;
                            break;
                        }
                    }
                }
                dataPixelOffset += dataPixelStride;
                maskPixelOffset += maskPixelStride;
            }
            dataLineOffset += dataLineStride;
            maskLineOffset += maskLineStride;
        }
        int maxIndex = -1;
        int secondMaxIndex = -1;
        int maxNumClassMembers = 0;
        int secondMaxNumClassMembers = 0;
        for (int i = 0; i < numClassMembers.length; i++) {
            if (numClassMembers[i] > maxNumClassMembers) {
                secondMaxIndex = maxIndex;
                secondMaxNumClassMembers = maxNumClassMembers;
                maxIndex = i;
                maxNumClassMembers = numClassMembers[i];
            } else if (numClassMembers[i] > secondMaxNumClassMembers) {
                secondMaxIndex = i;
                secondMaxNumClassMembers = numClassMembers[i];
            }
        }
        if (maxIndex >= 0) {
            majorityClass = indexNames[maxIndex];
            if (secondMaxIndex >= 0) {
                secondMajorityClass = indexNames[secondMaxIndex];
            }
        }
    }

}
