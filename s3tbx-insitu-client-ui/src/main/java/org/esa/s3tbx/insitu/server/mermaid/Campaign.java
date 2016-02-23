package org.esa.s3tbx.insitu.server.mermaid;

import com.google.gson.annotations.SerializedName;
import org.esa.s3tbx.insitu.server.InsituDataset;

import java.util.List;

/**
 * @author Marco Peters
 */
class Campaign implements InsituDataset {
    private String name;
    @SerializedName("Observations") private List<MermaidObservation> observations;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<MermaidObservation> getObservations() {
        return observations;
    }
}
