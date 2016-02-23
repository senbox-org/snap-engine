package org.esa.s3tbx.insitu.server.mermaid;

import org.esa.s3tbx.insitu.server.InsituParameter;

/**
 * @author Marco Peters
 */
class MermaidParameter implements InsituParameter {

    // Example:
    // {"name":"es_443","type":"radiance","unit":"mW.m-2.nm-1","description":"Sea-level solar illumination at 443nm"}
    private String name;
    private String type;
    private String unit;
    private String description;

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getUnit() {
        return unit;
    }
}
