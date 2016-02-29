package org.esa.s3tbx.insitu.server;

import java.util.List;

/**
 * @author Marco Peters
 */
public interface InsituResponse {

    STATUS_CODE getStatus();

    List<String> getFailureReasons();

    long getObservationCount();

    List<? extends InsituParameter> getParameters();

    List<? extends InsituDatasetDescr> getDatasetDescriptions();

    List<? extends InsituDataset> getDatasetList();

    enum STATUS_CODE {
        OK,
        NOK
    }
}
