package org.esa.snap.speclib.api;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.speclib.model.SpectralAxis;
import org.esa.snap.speclib.model.SpectralProfile;

import java.util.List;
import java.util.Optional;


public interface SpectralProfileExtractor {


    Optional<SpectralProfile> extract(
            String name,
            SpectralAxis axis,
            List<Band> bands,
            int x,
            int y,
            int level,
            String yUnit,
            String productId
    );
}
