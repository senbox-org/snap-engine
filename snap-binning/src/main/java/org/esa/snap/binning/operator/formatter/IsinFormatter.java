package org.esa.snap.binning.operator.formatter;

import com.vividsolutions.jts.geom.Geometry;
import org.esa.snap.binning.PlanetaryGrid;
import org.esa.snap.binning.TemporalBinSource;
import org.esa.snap.binning.operator.formatter.Formatter;
import org.esa.snap.binning.operator.formatter.FormatterConfig;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;

public class IsinFormatter implements Formatter {

    @Override
    public void format(PlanetaryGrid planetaryGrid,
                       TemporalBinSource temporalBinSource,
                       String[] featureNames,
                       FormatterConfig formatterConfig,
                       Geometry roiGeometry,
                       ProductData.UTC startTime,
                       ProductData.UTC stopTime,
                       MetadataElement... metadataElements) throws Exception {

        throw new RuntimeException("not implemented");
    }
}
