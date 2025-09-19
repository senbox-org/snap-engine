package org.esa.snap.classification.gpf;

import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.gpf.Tile;

import java.util.Map;

public class ClassificationUtils {

    private final static int NOT_IN_POLYGON = -1;

    public static String getFirstPartOfExpression(final String polygonName, final int polygonIdx) {
        return '\'' + polygonName + "' ? " + polygonIdx + " : ";
    }

    public static String getExpression(final VectorDataNode[] polygons,
                                        final Map<VectorDataNode, Integer> indexMap) {

        if (polygons == null || indexMap == null) {
            return null;
        }

        final VectorDataNode firstNode = polygons[0];
        String expression = getFirstPartOfExpression(firstNode.getName(), indexMap.get(firstNode)) + NOT_IN_POLYGON;

        for (int i = 1; i < polygons.length; i++) {
            final VectorDataNode nextNode = polygons[i];
            expression = getFirstPartOfExpression(nextNode.getName(), indexMap.get(nextNode)) + '(' + expression + ')';
        }

        return expression;
    }

    public static double[] getFeatures(final Tile[] featureTiles, FeatureInfo[] featureInfos, final int x, final int y) {
        final double[] features = new double[featureTiles.length];
        for (int i = 0; i < featureTiles.length; ++i) {
            double val = featureTiles[i].getDataBuffer().getElemDoubleAt(featureTiles[i].getDataBufferIndex(x, y));
            if (val == featureInfos[i].featureNoDataValue) {
                return null;
            }

            // scale the value to [0, 1]
            val = (val - featureInfos[i].featureOffsetValue) * featureInfos[i].featureScaleValue;
            if (val > 1.0) {
                val = 1.0;
            } else if (val < 0.0) {
                val = 0.0;
            }
            features[i] = val;
        }
        return features;
    }

    public static boolean hasInvalidFeature(double[] features, FeatureInfo[] featureInfoList) {
        for (int i = 0; i < features.length; i++) {
            if (Double.isNaN(features[i]) || features[i] == featureInfoList[i].featureNoDataValue) return true;
        }
        return false;
    }

}
