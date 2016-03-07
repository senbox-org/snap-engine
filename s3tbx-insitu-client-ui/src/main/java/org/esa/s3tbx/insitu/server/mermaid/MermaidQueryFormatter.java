package org.esa.s3tbx.insitu.server.mermaid;

import org.esa.s3tbx.insitu.server.InsituQuery;
import org.esa.snap.core.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Marco Peters
 */
class MermaidQueryFormatter {

    private static final String PARAMETERS_SUBJECT = "parameters";
    private static final String CAMPAIGNS_SUBJECT = "campaigns";
    private static final String OBSERVATIONS_SUBJECT = "observations";

    private static final String PARAM_LON_MIN = "lon_min";
    private static final String PARAM_LAT_MIN = "lat_min";
    private static final String PARAM_LON_MAX = "lon_max=";
    private static final String PARAM_LAT_MAX = "lat_max";
    private static final String PARAM_START_DATE = "start_date";
    private static final String PARAM_STOP_DATE = "stop_date";
    private static final String PARAM_PARAM = "param";
    private static final String PARAM_CAMPAIGN = "campaign";
    private static final String PARAM_SHIFT = "shift";
    private static final String PARAM_LIMIT = "limit";
    private static final String PARAM_COUNT_ONLY = "count_only";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    public static String format(InsituQuery query) {
        List<String> queryParams= new ArrayList<>();

        if(query.lonMin() != null) {
            queryParams.add(PARAM_LON_MIN + "=" + query.lonMin());
        }
        if(query.latMin() != null) {
            queryParams.add(PARAM_LAT_MIN + "=" + query.latMin());
        }
        if(query.lonMax() != null) {
            queryParams.add(PARAM_LON_MAX + query.lonMax());
        }
        if(query.latMax() != null) {
            queryParams.add(PARAM_LAT_MAX + "=" + query.latMax());
        }
        if(query.startDate() != null) {
            try {
                queryParams.add(PARAM_START_DATE + "=" + URLEncoder.encode(DATE_FORMAT.format(query.startDate()), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException("start date could not be encoded", e);
            }
        }
        if(query.stopDate() != null) {
            try {
                queryParams.add(PARAM_STOP_DATE + "=" + URLEncoder.encode(DATE_FORMAT.format(query.stopDate()), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException("stop date could not be encoded", e);
            }
        }
        if(query.param() != null && query.param().length > 0) {
            queryParams.add(PARAM_PARAM + "=" + StringUtils.arrayToCsv(query.param()));
        }
        if(query.datasets() != null && query.datasets().length > 0) {
            queryParams.add(PARAM_CAMPAIGN + "=" + StringUtils.arrayToCsv(query.datasets()));
        }
        if(query.shift() > 0) {
            queryParams.add(PARAM_SHIFT + "=" + query.shift());
        }
        if(query.limit() > 0) {
            queryParams.add(PARAM_LIMIT + "=" + query.limit());
        }
        if(query.countOnly()) {
            queryParams.add(PARAM_COUNT_ONLY);
        }

        if(query.subject() == null) {
            throw new IllegalArgumentException("subject must be specified");
        }
        final StringBuilder sb = new StringBuilder();
        sb.append("/");
        switch (query.subject()) {
            case DATASETS:
                sb.append(CAMPAIGNS_SUBJECT);
                break;
            case PARAMETERS:
                sb.append(PARAMETERS_SUBJECT);
                break;
            case OBSERVATIONS:
                sb.append(OBSERVATIONS_SUBJECT);
                break;
        }
        sb.append("?");

        for (int i = 0; i < queryParams.size(); i++) {
            String queryParam = queryParams.get(i);
            sb.append(queryParam);
            if(i < queryParams.size()-1) {
                sb.append("&");
            }
        }

        return sb.toString();
    }
}
