package org.esa.snap.dem.dataio.copernicus90m;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.*;
import org.apache.http.auth.Credentials;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.product.library.v2.preferences.RepositoriesCredentialsController;
import org.esa.snap.product.library.v2.preferences.model.RemoteRepositoryCredentials;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


import java.util.ArrayList;
import java.util.List;


public class CopernicusDownloader {


    /*
     private final String baseURL = "http://panda.copernicus.eu/Mc3OpenSearch/webapi/Services/getProducts/";
    private final String [] productIdentifiers = new String []{"COP-DEM_GLO-90-DGED/2019_1"};
    private final String bbox_key = "?bbox=";
    private final String identifier_key = "&parentIdentifier=";
     */

    private Path[] demProductFiles;

    private final File installDir;

    private String username = null;
    private String password = null;
    private List<Credentials> credentialList = null;

    private String server = "cdsdata.copernicus.eu";
    private int port = 990;


    public CopernicusDownloader(File installDir) throws Exception {
        this.installDir = installDir;
        String [] credentials = validateCredentials();
        username = credentials[0];
        password = credentials[1];
    }


    public CopernicusDownloader(File installDir, String username, String password){
        this.installDir = installDir;
        this.username = username;
        this.password = password;


    }

    // Call this method for SNAP to assess if the credentials stored in the Scientific Data Hub list of credentials are valid for
    // accessing the Copernicus DEM FTP. Will also return back a String array with the valid username as the first index,
    // and the password as the second index. If the credentials are not valid, it will throw an OperatorException.
    public static String [] validateCredentials() throws IOException {
        String server = "cdsdata.copernicus.eu";
        String [] userPass = new String[2];
        int port = 990;
        String username = null;
        String password = null;
        List<Credentials> credentialList = null;
        // Get the credentials from the remote repository credentials manager.
        RepositoriesCredentialsController controller = RepositoriesCredentialsController.getInstance();
        List<RemoteRepositoryCredentials> credentials = controller.getRepositoriesCredentials();
        if(credentials.size() == 0){
            throw new OperatorException("There are no credentials stored to authenticate the Copernicus Europe DEM. Please add your credentials to the credential manager under the Scientific Data Hub credentials list (Tools > Options > Product Library > Scientific Data Hub).");
        }
        for (RemoteRepositoryCredentials credential : credentials){
            if(credential.getRepositoryName().equals("Scientific Data Hub")){
                credentialList =  credential.getCredentialsList();
            }
        }
        if(credentialList.size() == 0){
            throw new OperatorException("There are no credentials stored to authenticate the Copernicus Europe DEM. Please add your credentials to the credential manager under the Scientific Data Hub credentials list (Tools > Options > Product Library > Scientific Data Hub).");
        }
        FTPSClient client = new FTPSClient(true);
        client.setBufferSize(1024 * 1024);
        client.connect(server, port);
        for (Credentials value : credentialList) {
            //UsernamePasswordCredentials credential = (UsernamePasswordCredentials) credentialList.get(x);
            try {
                boolean success = client.login(value.getUserPrincipal().getName(), value.getPassword());
                if (success) {
                    username = value.getUserPrincipal().getName();
                    password = value.getPassword();
                    break;
                }
                client.logout();
            } catch (Exception e) {
                client.logout();
            }
        }
        client.disconnect();
        client = null;

        if(username == null){
            throw new OperatorException("No valid credentials were found under the Scientific Data Hub list of credentials (Tools > Options > Product Library > Scientific Data Hub). Please ensure that your credentials are added correctly and try again.");
        }
        userPass[0] = username;
        userPass[1] = password;
        return userPass;
    }

    public CopernicusDownloader(File installDir, Credentials credential){
        this.installDir = installDir;
        this.username = credential.getUserPrincipal().getName();
        this.password = credential.getPassword();
    }


    public boolean downloadTiles(double lat, double lon) throws Exception{
        String installDir = this.installDir.getAbsolutePath();
        System.out.println("Searching FTP endpoint for file containing coordinates " + lat + ", " + lon);
        int buffer = 0; //FTP downloading is very slow. Increase buffer if the HTTP shibboleth endpoint authentication is figured out.
        int lat_lower = ((int) lat) - buffer;
        int lat_higher = ((int) lat) + buffer;
        int lon_lower = ((int) lon) - buffer;
        int lon_higher = ((int) lon) + buffer;


        FTPSClient client = new FTPSClient(true);
        client.setBufferSize(1024 * 1024);
        client.connect(server, port);
        System.out.print(client.getReplyString());
        // After connection attempt, you should check the reply code to verify success.
        int reply = client.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            client.disconnect();
            System.err.println("FTP server refused connection.");
            return false;
        }
        boolean success = client.login(username, password);
        System.out.println(success ? "Successfully authenticated with Copernicus endpoint" : "Unable to authenticate");
        client.enterLocalPassiveMode();


        client.changeWorkingDirectory("DEM-datasets/COP-DEM_GLO-90-DGED/2019_1/Europe/");
        String [] allFiles = CopernicusConstants.remotePaths;
        /*
        for (FTPFile country : client.listDirectories()) {
            for (FTPFile tile : client.listFiles("/DEM-datasets/COP-DEM_GLO-90-DGED/2019_1/Europe/" + country.getName())) {
                allFiles.add("/DEM-datasets/COP-DEM_GLO-90-DGED/2019_1/Europe/" + country.getName() + tile.getName());
                System.out.println("/DEM-datasets/COP-DEM_GLO-90-DGED/2019_1/Europe/" + country.getName() + tile.getName());
            }
        }
        */

        client.changeWorkingDirectory("/");
        InputStream mapping = client.retrieveFileStream("/DEM-datasets/COP-DEM_GLO-90-DGED/2019_1/Europe/mapping.csv");
        if (!FTPReply.isPositivePreliminary(client.getReplyCode())) {
            mapping.close();
            client.logout();
            client.disconnect();
            System.err.println("File transfer failed.");
            return false;
        }


        //String s = IOUtils.toString(mapping, StandardCharsets.UTF_8.name());
        System.out.println(Paths.get("").toAbsolutePath().toString());
        String[] lines =  IOUtils.toString(mapping, StandardCharsets.UTF_8.name()).split("\n");
        HashMap<String, String> pairing = new HashMap<>();

        List<String> matchingFileNames = new ArrayList<>();
        for (String line : lines) {
            String[] splitLine = line.split(";");
            if (splitLine[1].equals("Lat")) {
                continue;
            }
            int thisLon = Integer.parseInt(splitLine[1].replace("N", "").replace("S", ""));
            int thisLat = Integer.parseInt(splitLine[2].replace("W", "").replace("E", ""));
            if(splitLine[1].contains("S")){
                thisLon *= -1;
            }if(splitLine[2].contains("W")){
                thisLat *= -1;
            }
            if ((thisLat >= lat_lower && thisLat <= lat_higher) && (thisLon >= lon_lower && thisLon <= lon_higher)) {
                matchingFileNames.add(splitLine[0]);
                File tf = new File(installDir + "/" + CopernicusElevationModel.createTileFilename(thisLon, thisLat));
                if(!Files.exists(tf.toPath())){
                    pairing.put(thisLat + "," + thisLon, splitLine[0]);
                }

            }
        }
        //mapping.close();
        System.out.println("done mapping");
        // Must call completePendingCommand() to finish command.
        if (!client.completePendingCommand()) {
            client.logout();
            client.disconnect();
            System.err.println("File transfer failed.");
            return false;
        }
        client.logout();
        client.disconnect();
        if(pairing.size() == 0){
            File file = new File(installDir + "/" + CopernicusElevationModel.createTileFilename(lon, lat));
            file.createNewFile();
            return true;
        }

        // Have to re-login to do the actual TAR downloading due to a quirk in the FTP server configuration
        client.connect(server, port);
        System.out.print(client.getReplyString());
        // After connection attempt, you should check the reply code to verify success.
        reply = client.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            client.disconnect();
            System.err.println("FTP server refused connection.");
            return false;
        }
        success = client.login(username, password);
        client.setFileType(FTP.BINARY_FILE_TYPE);

        System.out.println(success ? "Successfully authenticated with Copernicus endpoint" : "Unable to authenticate");

        client.enterLocalPassiveMode();
        System.out.println("Downloading " + matchingFileNames.size() + " files");
        int downloaded = 0;
        // client.configure(new FTPClientConfig(FTPClientConfig.SYST_UNIX));
        for (String f : allFiles) {
            String fileName = f.split("/")[f.split("/").length - 1].replace("\r", "");
            if (pairing.values().contains(fileName)) {
                System.out.println(f);
                File origFileName = new File(installDir + "/" + fileName);
                OutputStream output = FileUtils.openOutputStream(origFileName);
                client.retrieveFile(f, output);
                if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
                    output.close();
                    client.logout();
                    client.disconnect();
                    System.err.println("File transfer failed.");
                    return false;
                }else{
                    downloaded += 1;
                }
                output.close();
                String properName = CopernicusFile.getNameFromTarFile(origFileName);
                origFileName.renameTo(new File(installDir + "/" + properName));

            }
        }
        client.logout();
        client.disconnect();

        return downloaded > 0;

    }






    public Path[] getDemProductFiles() {
        return demProductFiles;
    }

    /*


    // For opensearch implementation. Keeping for future reference if shibboleth SSO authentication within Java is solved.
    private String buildSearchURL(final double lat, final double lon){
        final int buffer = 12;
        final double lat_west = lat - buffer;
        final double lat_east = lat + buffer;
        final double lon_south = lon - buffer;
        final double lon_north = lon + buffer;

        final String bounding_area = lat_west + "," + lon_south + "," + lat_east + "," + lon_north;

        final String searchUrl = baseURL + bbox_key + bounding_area + identifier_key + productIdentifiers[0];


        return searchUrl;
    }

    // Parses the XML response from searching on the opensearch API to get a list of valid download URLs.
    private List<String> parseResponse (String xmlResponse) throws Exception{
        List<String> responseURLs = new ArrayList<String>();
        String [] xml_split = xmlResponse.split("<");
        for(String s : xml_split){
            if (s != null){
                if (s.contains("href") && s.contains("prismDownload")){
                    String url = s.split("\"")[1];
                    if (! responseURLs.contains(url)){
                        responseURLs.add(url);
                    }
                }
            }
        }



        return responseURLs;
    }

    protected String getAuthenticationToken() { //replace with credentials
        return NetUtils.getAuthToken("USERNAME", "PASSWORD");
    }
*/




}
