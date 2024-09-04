package org.esa.snap.dem.dataio.copernicus;

import org.esa.snap.core.util.io.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class CopernicusDownloadTesting {

    public static void main(String[] args) throws Exception {

        CopernicusDownloadTesting dTester = new CopernicusDownloadTesting();

        List<Long> cop30DonwloadTimes = dTester.getDownloadTimes(getCoordinates(), 30);
        List<Long> cop90DonwloadTimes = dTester.getDownloadTimes(getCoordinates(), 90);

        long meanCop30 = getMean(cop30DonwloadTimes);
        long meanCop90 = getMean(cop90DonwloadTimes);

        System.out.println("\n\nCopernicus30 download Times are: ");
        printTimes(cop30DonwloadTimes);
        System.out.println("With a mean of: " + meanCop30);

        System.out.println("\n\nCopernicus90 download Times are: ");
        printTimes(cop90DonwloadTimes);
        System.out.println("With a mean of: " + meanCop90 + "ms");
    }

    private static void printTimes(List<Long> donwloadTimes) {
        for (int ii = 0; ii < donwloadTimes.size(); ii++) {
            System.out.println("Index: " + ii + "  =>  " + donwloadTimes.get(ii) + "ms");
        }
    }

    private static long getMean(List<Long> donwloadTimes) {
        long result = 0;
        for (Long time : donwloadTimes) {
            result += time;
        }
        return result / donwloadTimes.size();
    }

    public List<Long> getDownloadTimes(ArrayList<double[]> coordinates, int resolution) throws Exception {
        List<Long> downloadTimes = new ArrayList<>();
        File folder = Files.createTempDirectory(this.getClass().getSimpleName()).toFile();
        CopernicusDownloader d = new CopernicusDownloader(folder);

        for (double[] coordinate : coordinates) {
            long startTime = System.currentTimeMillis();
            d.downloadTiles(coordinate[0], coordinate[1], resolution);
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            downloadTimes.add(duration);
        }

        FileUtils.deleteTree(folder);
        return downloadTimes;
    }

    public static ArrayList<double[]> getCoordinates() {
        ArrayList<double[]> coordinates = new ArrayList<>();

        coordinates.add(new double[]{-80.0, 43.0});
        coordinates.add(new double[]{23.0, 68.0});
        coordinates.add(new double[]{25.0, 68.0});
        coordinates.add(new double[]{3.0, 115.0});
        coordinates.add(new double[]{38.0, 43.0});

        return coordinates;
    }
}
