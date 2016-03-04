package org.esa.s3tbx.insitu.server.mermaid;

import com.google.gson.annotations.SerializedName;
import org.esa.s3tbx.insitu.server.InsituResponse;

import java.util.List;

/**
 * @author Marco Peters
 */
class MermaidResponse implements InsituResponse {

    @SerializedName("response") private STATUS_CODE status;
    private List<String> reasons;
    @SerializedName("nb_obs") private long observationCount;
    @SerializedName("Parameters") private List<MermaidParameter> parameters;
    @SerializedName("Campaigns") private List<Campaign> campaigns;

    @Override
    public STATUS_CODE getStatus() {
        return status;
    }

    @Override
    public List<String> getFailureReasons() {
        return reasons;
    }

    @Override
    public long getObservationCount() {
        return observationCount;
    }

    @Override
    public List<MermaidParameter> getParameters() {
        return parameters;
    }

    @Override
    public List<Campaign> getDatasets() {
        return campaigns;
    }

}
