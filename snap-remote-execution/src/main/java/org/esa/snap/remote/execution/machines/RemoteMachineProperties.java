package org.esa.snap.remote.execution.machines;

/**
 * Created by jcoravu on 28/12/2018.
 */
public class RemoteMachineProperties {

    public static final String WINDOWS_OPERATING_SYSTEM = "Windows";
    public static final String LINUX_OPERATING_SYSTEM = "Linux";

    private String hostName;
    private int portNumber;
    private String username;
    private String password;
    private String operatingSystemName;
    private String sharedFolderPath;
    private String gptFilePath;

    public RemoteMachineProperties() {
    }

    public RemoteMachineProperties(String hostName, int portNumber, String username, String password, String operatingSystemName, String sharedFolderPath) {
        this.hostName = hostName;
        this.portNumber = portNumber;
        this.username = username;
        this.password = password;
        this.operatingSystemName = operatingSystemName;
        this.sharedFolderPath = sharedFolderPath;
    }

    public void setSharedFolderPath(String sharedFolderPath) {
        this.sharedFolderPath = sharedFolderPath;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setOperatingSystemName(String operatingSystemName) {
        this.operatingSystemName = operatingSystemName;
    }

    public String getOperatingSystemName() {
        return operatingSystemName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public String getSharedFolderPath() {
        return sharedFolderPath;
    }

    public boolean isWindows() {
        return this.operatingSystemName.equalsIgnoreCase(WINDOWS_OPERATING_SYSTEM);
    }

    public boolean isLinux() {
        return this.operatingSystemName.equalsIgnoreCase(LINUX_OPERATING_SYSTEM);
    }

    public void setGPTFilePath(String gptFilePath) {
        this.gptFilePath = gptFilePath;
    }

    public String getGPTFilePath() {
        return gptFilePath;
    }
}
