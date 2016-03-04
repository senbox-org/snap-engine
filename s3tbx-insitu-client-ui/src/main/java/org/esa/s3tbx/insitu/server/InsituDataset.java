package org.esa.s3tbx.insitu.server;

import java.util.List;

/**
 * @author Marco Peters
 */
public interface InsituDataset {

    String getName();

    String getPi();

    String getContact();

    String getDescription();

    String getPolicy();

    String getWebsite();

    List<? extends InsituObservation> getObservations();
}
