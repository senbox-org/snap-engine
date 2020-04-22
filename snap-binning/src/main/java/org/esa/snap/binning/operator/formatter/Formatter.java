package org.esa.snap.binning.operator.formatter;

import org.esa.snap.binning.PlanetaryGrid;
import org.esa.snap.binning.TemporalBinSource;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.locationtech.jts.geom.Geometry;

public interface Formatter {

    void format(PlanetaryGrid planetaryGrid,
           TemporalBinSource temporalBinSource,
           String[] featureNames,
           FormatterConfig formatterConfig,
           Geometry roiGeometry,
           ProductData.UTC startTime,
           ProductData.UTC stopTime,
           MetadataElement... metadataElements) throws Exception;
}
