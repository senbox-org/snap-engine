package org.esa.snap.core.dataio;

import org.esa.snap.core.util.ArrayUtils;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tonio Fincke
 */
public class MultiSizeProductSubsetDef extends ProductSubsetDef{

    private List<Rectangle> regions;
    private List<Integer> subSamplingXs;
    private List<Integer> subSamplingYs;

    public MultiSizeProductSubsetDef(String subsetName) {
        super(subsetName);
        regions = new ArrayList<>();
        subSamplingXs = new ArrayList<>();
        subSamplingYs = new ArrayList<>();
    }

    public MultiSizeProductSubsetDef() {
        this(null);
    }

    public void addNode(String nodeName, Rectangle region, int subSamplingX, int subSamplingY) {
        if (!containsNodeName(nodeName)) {
            addNodeName(nodeName);
        }
        regions.add(region);
        subSamplingXs.add(subSamplingX);
        subSamplingYs.add(subSamplingY);
    }

    public Rectangle getRegion(String bandName) {
        if (regions.size() == 0) {
            return null;
        }
        final int index = ArrayUtils.getElementIndex(bandName, getNodeNames());
        return regions.get(index);
    }

    public int getSubSamplingX(String bandName) {
        if (regions.size() == 0) {
            return 1;
        }
        final int index = ArrayUtils.getElementIndex(bandName, getNodeNames());
        return subSamplingXs.get(index);    }

    public int getSubSamplingY(String bandName) {
        if (regions.size() == 0) {
            return 1;
        }
        final int index = ArrayUtils.getElementIndex(bandName, getNodeNames());
        return subSamplingYs.get(index);
    }

    @Override
    public boolean isEntireProductSelected() {
        return regions.size() == 1;
    }
}
