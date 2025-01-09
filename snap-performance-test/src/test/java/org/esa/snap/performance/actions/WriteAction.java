package org.esa.snap.performance.actions;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.performance.util.Result;
import org.esa.snap.performance.util.TestUtils;
import org.esa.snap.performance.util.Threading;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class WriteAction implements Action {

    private Product product;
    private String outputDir;
    private String outputFormat;
    private String threading;
    private List<Result> allResults;

    public WriteAction(Product product, String outputDir, String outputFormat, String threading) {
        this.product = product;
        this.outputDir = outputDir;
        this.outputFormat = outputFormat;
        this.threading = threading;
    }

    @Override
    public void execute() throws Throwable {
        this.allResults = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        String fullfileName = this.outputDir + timestamp;

        if (this.threading.equals(Threading.MULTI.getName())) {
            GPF.writeProduct(this.product, new File(fullfileName), this.outputFormat, false, ProgressMonitor.NULL);
        } else {
            ProductIO.writeProduct(this.product, fullfileName, this.outputFormat);
        }

        String outputPath = TestUtils.constructOutputFilePath(fullfileName, this.outputFormat);
        Result result = new Result("ProductPath", false, outputPath, "");
        this.allResults.add(result);
    }

    @Override
    public void cleanUp() {
    }

    @Override
    public List<Result> fetchResults() {
        return this.allResults;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}
