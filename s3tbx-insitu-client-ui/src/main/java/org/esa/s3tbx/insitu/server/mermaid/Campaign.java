package org.esa.s3tbx.insitu.server.mermaid;

import com.google.gson.annotations.SerializedName;
import org.esa.s3tbx.insitu.server.InsituDataset;

import java.util.List;

/**
 * @author Marco Peters
 */
class Campaign implements InsituDataset {
    private String name;
    private String pi;
    private String contact;
    private String website;
    private String policy;
    private String description;
    @SerializedName("Observations") private List<MermaidObservation> observations;

    public String getName() {
        return name;
    }

    @Override
    public String getPi() {
        return pi;
    }

    @Override
    public String getContact() {
        return contact;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getPolicy() {
        return policy;
    }

    @Override
    public String getWebsite() {
        return website;
    }

    @Override
    public List<MermaidObservation> getObservations() {
        return observations;
    }
}
