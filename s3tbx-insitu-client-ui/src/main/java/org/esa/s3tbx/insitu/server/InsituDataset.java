package org.esa.s3tbx.insitu.server;

import java.util.List;

/**
 * @author Marco Peters
 */
public interface InsituDataset {

    String getName();

    List<? extends InsituObservation> getObservations();
}
