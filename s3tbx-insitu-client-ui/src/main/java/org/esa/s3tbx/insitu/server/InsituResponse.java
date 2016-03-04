package org.esa.s3tbx.insitu.server;

import java.util.Collections;
import java.util.List;

/**
 * @author Marco Peters
 */
public interface InsituResponse {

    InsituResponse EMPTY_RESPONSE = new InsituResponse() {
        @Override
        public STATUS_CODE getStatus() {
            return STATUS_CODE.OK;
        }

        @Override
        public List<String> getFailureReasons() {
            return Collections.emptyList();
        }

        @Override
        public long getObservationCount() {
            return 0;
        }

        @Override
        public List<? extends InsituParameter> getParameters() {
            return Collections.emptyList();
        }

        @Override
        public List<? extends InsituDataset> getDatasets() {
            return Collections.emptyList();
        }
    };

    STATUS_CODE getStatus();

    List<String> getFailureReasons();

    long getObservationCount();

    List<? extends InsituParameter> getParameters();

    List<? extends InsituDataset> getDatasets();

    enum STATUS_CODE {
        OK,
        NOK
    }

}
