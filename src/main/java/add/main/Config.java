package add.main;

public class Config {

    private String bugId;
    private String buggySourceDirectoryPath;
    private String diffPath;

    public String getCurrentCommit() {
        return currentCommit;
    }

    public void setCurrentCommit(String currentCommit) {
        this.currentCommit = currentCommit;
    }

    private String currentCommit;

    public String getStartCommit() {
        return StartCommit;
    }

    public void setStartCommit(String startCommit) {
        StartCommit = startCommit;
    }

    private String StartCommit;

    public String getEndCommit() {
        return EndCommit;
    }

    public void setEndCommit(String endCommit) {
        EndCommit = endCommit;
    }

    private String EndCommit;
    private String outputDirectoryPath;

    public Config() {
    }

    public LauncherMode getLauncherMode() {
        return LauncherMode.ALL;
    }

    public void setLauncherMode(LauncherMode launcherMode) {
    }

    public String getBugId() {
        return bugId;
    }

    public void setBugId(String bugId) {
        this.bugId = bugId;
    }

    public String getBuggySourceDirectoryPath() {
        return buggySourceDirectoryPath;
    }

    public void setBuggySourceDirectoryPath(String buggySourceDirectoryPath) {
        this.buggySourceDirectoryPath = buggySourceDirectoryPath;
    }

    public String getDiffPath() {
        return diffPath;
    }

    public void setDiffPath(String diffPath) {
        this.diffPath = diffPath;
    }

    public String getOutputDirectoryPath() {
        return outputDirectoryPath;
    }

    public void setOutputDirectoryPath(String outputDirectoryPath) {
        this.outputDirectoryPath = outputDirectoryPath;
    }

}
