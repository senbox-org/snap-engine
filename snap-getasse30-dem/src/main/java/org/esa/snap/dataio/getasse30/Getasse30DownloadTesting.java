package org.esa.snap.dataio.getasse30;

import org.esa.snap.core.dataop.dem.ElevationFile;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.util.io.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Getasse30DownloadTesting {

    public static void main(String[] args) throws Exception {

        Getasse30DownloadTesting dTester = new Getasse30DownloadTesting();

        List<Long> getasse30DownloadTimes = dTester.getDownloadTimes(getCoordinates());

        long meanGetasse30 = getMean(getasse30DownloadTimes);

        System.out.println("\n\nGETASSE30 download Times are: ");
        printTimes(getasse30DownloadTimes);
        System.out.println("With a mean of: " + meanGetasse30 + "ms");
    }

    private static void printTimes(List<Long> downloadTimes) {
        for (int ii = 0; ii < downloadTimes.size(); ii++) {
            System.out.println("Index: " + ii + "  =>  " + downloadTimes.get(ii) + "ms");
        }
    }

    private static long getMean(List<Long> downloadTimes) {
        long result = 0;
        for (Long time : downloadTimes) {
            result += time;
        }
        return result / downloadTimes.size();
    }

    public List<Long> getDownloadTimes(ArrayList<double[]> coordinates) throws Exception {
        List<Long> downloadTimes = new ArrayList<>();

        // Der Descriptor und das Resampling müssen entsprechend deiner Anforderungen angepasst werden
        GETASSE30ElevationModelDescriptor descriptor = new GETASSE30ElevationModelDescriptor();
        Resampling resampling = Resampling.BILINEAR_INTERPOLATION;  // Beispiel Resampling-Methode

        GETASSE30ElevationModel elevationModel = new GETASSE30ElevationModel(descriptor, resampling);

        for (double[] coordinate : coordinates) {
            File folder = Files.createTempDirectory(this.getClass().getSimpleName()).toFile();

            // Hier erstellst du die Kacheln für die jeweilige Koordinate
            int x = (int) Math.floor((coordinate[0] + 180.0) / descriptor.getTileWidth());
            int y = (int) Math.floor((coordinate[1] + 90.0) / descriptor.getTileHeight());

            long startTime = System.currentTimeMillis();
            System.out.println("\n\nStart: " + startTime);

            // Die Methode createElevationFile wird verwendet, um die Datei herunterzuladen und zu speichern
            ElevationFile[][] elevationFiles = new ElevationFile[descriptor.getNumXTiles()][descriptor.getNumYTiles()];
            elevationModel.createElevationFile(elevationFiles, x, y, folder);

            long endTime = System.currentTimeMillis();
            System.out.println("\n\nEnd: " + endTime);
            long duration = endTime - startTime;
            downloadTimes.add(duration);
            FileUtils.deleteTree(folder);
        }

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

