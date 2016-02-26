package org.esa.s3tbx.insitu.server.mermaid;

import org.esa.s3tbx.insitu.server.InsituDatasetDescr;

/**
 * @author Marco Peters
 */
class CampaignDescr implements InsituDatasetDescr {
    private String name;
    private String pi;
    private String contact;
    private String website;
    private String policy;
    private String description;

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
}
