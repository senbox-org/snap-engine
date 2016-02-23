package org.esa.s3tbx.insitu.server.mermaid;

import com.google.gson.annotations.SerializedName;
import org.esa.s3tbx.insitu.server.InsituDatasetDescr;

/**
 * @author Marco Peters
 */
class CampaignDescr implements InsituDatasetDescr {
    // Example:
    // {"Identifier":"BOUSSOLE","PI":"David Antoine"}

    @SerializedName("Identifier") private String identifier;
    @SerializedName("PI") private String pi;

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String getPi() {
        return pi;
    }
}
