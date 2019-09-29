package org.esa.snap.remote.execution.topology;

import org.esa.snap.remote.execution.machines.RemoteMachineProperties;
import org.esa.snap.remote.execution.utils.PasswordCryptoBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * Created by jcoravu on 8/1/2019.
 */
public class RemoteTopologyUtils {

    private RemoteTopologyUtils() {
    }

    public static RemoteTopology readTopology(Path remoteTopologyFilePath) throws IOException, ParseException, GeneralSecurityException {
        if (Files.exists(remoteTopologyFilePath)) {
            InputStream inputStream = Files.newInputStream(remoteTopologyFilePath);
            try {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                try {
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    try {
                        JSONParser jsonParser = new JSONParser();
                        JSONObject rootItem = (JSONObject)jsonParser.parse(bufferedReader);

                        String remoteSharedFolderURL = (String)rootItem.get("remote-shared-folder-url");
                        String remoteUsername = (String)rootItem.get("remote-username");
                        String encryptedRemotePassword = (String)rootItem.get("remote-password");
                        String localSharedFolderPath = (String)rootItem.get("local-shared-folder-path");
                        String localPassword = (String)rootItem.get("local-password");

                        String remotePassword = PasswordCryptoBuilder.decrypt(encryptedRemotePassword);

                        RemoteTopology remoteTopology = new RemoteTopology(remoteSharedFolderURL, remoteUsername, remotePassword);
                        remoteTopology.setLocalMachineData(localSharedFolderPath, localPassword);

                        JSONArray jsonArray = (JSONArray)rootItem.get("remote-machines");
                        for (int i=0; i<jsonArray.size(); i++) {
                            JSONObject jsonObject = (JSONObject)jsonArray.get(i);
                            String hostName = (String)jsonObject.get("host");
                            int portNumber = ((Number)jsonObject.get("port-number")).intValue();
                            String operatingSystemName = (String)jsonObject.get("operating-system");
                            String username = (String)jsonObject.get("username");
                            String encryptedPassword = (String)jsonObject.get("password");
                            String sharedFolderPath = (String)jsonObject.get("shared-folder-path");
                            String gptFilePath = (String)jsonObject.get("gpt-file-path");

                            String password = PasswordCryptoBuilder.decrypt(encryptedPassword);

                            RemoteMachineProperties server = new RemoteMachineProperties(hostName, portNumber, username, password, operatingSystemName, sharedFolderPath);
                            server.setGPTFilePath(gptFilePath);

                            remoteTopology.addRemoteMachine(server);
                        }

                        return remoteTopology;
                    } finally {
                        bufferedReader.close();
                    }
                } finally {
                    inputStreamReader.close();
                }
            } finally {
                inputStream.close();
            }
        }
        return null;
    }

    public static void writeTopology(Path remoteTopologyFilePath, RemoteTopology remoteTopology) throws IOException, GeneralSecurityException {
        String encryptedRemotePassword = PasswordCryptoBuilder.encrypt(remoteTopology.getRemotePassword());

        JSONObject rootItem = new JSONObject();
        rootItem.put("remote-shared-folder-url", remoteTopology.getRemoteSharedFolderURL());
        rootItem.put("remote-username", remoteTopology.getRemoteUsername());
        rootItem.put("remote-password", encryptedRemotePassword);
        rootItem.put("local-shared-folder-path", remoteTopology.getLocalSharedFolderPath());
        rootItem.put("local-password", remoteTopology.getLocalPassword());
        JSONArray jsonArray = new JSONArray();
        List<RemoteMachineProperties> remoteMachines = remoteTopology.getRemoteMachines();
        for  (int i=0; i<remoteMachines.size(); i++) {
            RemoteMachineProperties remoteMachineCredentials = remoteMachines.get(i);
            String encryptedPassword = PasswordCryptoBuilder.encrypt(remoteMachineCredentials.getPassword());
            JSONObject item = new JSONObject();
            item.put("host", remoteMachineCredentials.getHostName());
            item.put("port-number", remoteMachineCredentials.getPortNumber());
            item.put("operating-system", remoteMachineCredentials.getOperatingSystemName());
            item.put("username", remoteMachineCredentials.getUsername());
            item.put("password", encryptedPassword);
            item.put("shared-folder-path", remoteMachineCredentials.getSharedFolderPath());
            item.put("gpt-file-path", remoteMachineCredentials.getGPTFilePath());
            jsonArray.add(item);
        }
        rootItem.put("remote-machines", jsonArray);

        OutputStream outputStream = Files.newOutputStream(remoteTopologyFilePath);
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            try {
                BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
                try {
                    rootItem.writeJSONString(bufferedWriter);
                } finally {
                    bufferedWriter.close();
                }
            } finally {
                outputStreamWriter.close();
            }
        } finally {
            outputStream.close();
        }
    }
}
