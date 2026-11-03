package org.esa.snap.classification.gpf.minimumdistance;

import net.sf.javaml.classification.AbstractMeanClassifier;
import net.sf.javaml.core.Instance;
import net.sf.javaml.distance.EuclideanDistance;

import java.io.Serial;

/**
 * Minimum Distance
 */
public class MinDistClassifier extends AbstractMeanClassifier {
        @Serial
        private static final long serialVersionUID = 3044426429892220857L;
        private final EuclideanDistance dist;

        public MinDistClassifier(EuclideanDistance dist) {
            this.dist = dist;
        }

        public Object classify(Instance instance) {
            double min = Double.MAX_VALUE;
            Object pred = null;

            for (Object o : this.mean.keySet()) {
                double d = this.dist.calculateDistance((Instance) this.mean.get(o), instance);
                if (d < min) {
                    min = d;
                    pred = o;
                }
            }

            return pred;
        }
}
