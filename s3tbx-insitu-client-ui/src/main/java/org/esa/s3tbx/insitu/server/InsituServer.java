package org.esa.s3tbx.insitu.server;

import org.netbeans.api.progress.ProgressUtils;

/**
 * @author Marco Peters
 */
public interface InsituServer {

    String getName();

    InsituResponse query(InsituQuery query) throws InsituServerException;

    static void runWithProgress(InsituServerRunnable runnable) throws InsituServerException{
        ProgressUtils.runOffEventThreadWithProgressDialog(runnable,
                                                          "In-Situ Data Access",
                                                          runnable.getHandle().getProgressHandle(),
                                                          true,
                                                          50,
                                                          1000);

        if (runnable.getException() != null) {
            Exception exception = runnable.getException();
            throw new InsituServerException("Query not successful. Exception occured:" + exception.getMessage(), exception);
        }
        InsituResponse response = runnable.getResponse();
        if (InsituResponse.STATUS_CODE.NOK.equals(response.getStatus())) {
            StringBuilder sb = new StringBuilder();
            sb.append("Query not successful. Server responded with failure(s): \n");
            response.getFailureReasons().forEach(sb::append);
            throw new InsituServerException(sb.toString());
        }
    }

}
