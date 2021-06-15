package add.main;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser;
import com.martiansoftware.jsap.stringparsers.FileStringParser;

import add.entities.FeatureList;
import add.features.FeatureAnalyzer;
import add.features.detector.repairactions.RepairActionDetector;
import add.features.detector.repairpatterns.RepairPatternDetector;
import add.features.extractor.MetricExtractor;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import gumtree.spoon.diff.Diff;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;


public class Launcher {
    private static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Launcher.class);

    private Config config;

    public Launcher(String[] args) throws JSAPException {
        JSAP jsap = this.initJSAP();
        JSAPResult arguments = this.parseArguments(args, jsap);
        if (arguments == null) {
            System.exit(-1);
        }
        this.initConfig(arguments);

        Logger logger = (Logger) LoggerFactory.getLogger("fr.inria");
        logger.setLevel(Level.DEBUG);
    }

    private void showUsage(JSAP jsap) {
        System.err.println();
        System.err.println("Usage: java -jar patchclustering.jar <arguments>");
        System.err.println();
        System.err.println("Arguments:");
        System.err.println();
        System.err.println(jsap.getHelp());
    }

    private JSAPResult parseArguments(String[] args, JSAP jsap) {
        JSAPResult config = jsap.parse(args);
        if (!config.success()) {
            System.err.println();
            for (Iterator<?> errs = config.getErrorMessageIterator(); errs.hasNext(); ) {
                System.err.println("Error: " + errs.next());
            }
            this.showUsage(jsap);
            return null;
        }
        return config;
    }

    private JSAP initJSAP() throws JSAPException {
        JSAP jsap = new JSAP();

        String launcherModeValues = "";
        for (LauncherMode mode : LauncherMode.values()) {
            launcherModeValues += mode.name() + ";";
        }
        launcherModeValues = launcherModeValues.substring(0, launcherModeValues.length() - 1);

        FlaggedOption opt = new FlaggedOption("launcherMode");
        opt.setShortFlag('m');
        opt.setLongFlag("launcherMode");
        opt.setRequired(true);
        opt.setAllowMultipleDeclarations(false);
        opt.setUsageName(launcherModeValues);
        opt.setStringParser(EnumeratedStringParser.getParser(launcherModeValues));
        opt.setHelp("Provide the launcher mode, which is the type of the features that will be extracted.");
        jsap.registerParameter(opt);

        opt = new FlaggedOption("buggySourceDirectory");
        opt.setLongFlag("buggySourceDirectory");
        opt.setRequired(true);
        opt.setAllowMultipleDeclarations(false);
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Provide the path to the buggy source code directory of the bug.");
        jsap.registerParameter(opt);

        opt = new FlaggedOption("diffPath");
        opt.setLongFlag("diff");
        opt.setRequired(true);
        opt.setAllowMultipleDeclarations(true);
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Provide the path to the diff file.");
        jsap.registerParameter(opt);

        opt = new FlaggedOption("startCommit");
        opt.setShortFlag('s');
        opt.setLongFlag("start");
        opt.setRequired(true);
        opt.setAllowMultipleDeclarations(true);
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Provide the commit to start from");
        jsap.registerParameter(opt);

        opt = new FlaggedOption("endCommit");
        opt.setShortFlag('e');
        opt.setLongFlag("end");
        opt.setRequired(true);
        opt.setAllowMultipleDeclarations(true);
        opt.setStringParser(JSAP.STRING_PARSER);
        opt.setHelp("Provide the end commit.");
        jsap.registerParameter(opt);

        opt = new FlaggedOption("outputDirectory");
        opt.setShortFlag('o');
        opt.setLongFlag("output");
        opt.setRequired(false);
        opt.setAllowMultipleDeclarations(false);
        opt.setStringParser(FileStringParser.getParser().setMustBeDirectory(true).setMustExist(true));
        opt.setHelp("Provide an existing path to output the extracted features as a JSON file (optional).");
        jsap.registerParameter(opt);

        return jsap;
    }

    private void initConfig(JSAPResult arguments) {
        this.config = new Config();
        this.config.setLauncherMode(LauncherMode.valueOf(arguments.getString("launcherMode").toUpperCase()));
        this.config.setBuggySourceDirectoryPath(arguments.getString("buggySourceDirectory"));
        this.config.setDiffPath(arguments.getString("diffPath"));
        this.config.setStartCommit(arguments.getString("startCommit"));
        this.config.setEndCommit(arguments.getString("endCommit"));
        if (arguments.getFile("outputDirectory") != null) {
            this.config.setOutputDirectoryPath(arguments.getFile("outputDirectory").getAbsolutePath());
        }
    }


    public Repository openRepository(String repositoryPath) throws Exception {
        File folder = new File(repositoryPath);
        Repository repository;
        if (folder.exists()) {
            RepositoryBuilder builder = new RepositoryBuilder();
            repository = builder
                    .setGitDir(new File(folder, ".git"))
                    .readEnvironment()
                    .findGitDir()
                    .build();
        } else {
            throw new FileNotFoundException(repositoryPath);
        }
        return repository;
    }

    public void checkout(Repository repository, String commitId) throws Exception {
        try (Git git = new Git(repository)) {
            CheckoutCommand checkout = git.checkout().setName(commitId).setForce(true);
            checkout.call();
        }
    }

    public Iterable<RevCommit> getCommits(Repository repository, String startCommitId, String endCommitId)
            throws Exception {
        ObjectId from = repository.resolve(startCommitId);
        ObjectId to = repository.resolve(endCommitId);
        try (Git git = new Git(repository)) {
            List<RevCommit> revCommits = StreamSupport.stream(git.log().addRange(from, to).call()
                    .spliterator(), false)
                    .filter(r -> r.getParentCount() == 1)
                    .collect(Collectors.toList());
            Collections.reverse(revCommits);
            return revCommits;
        }
    }

    public void diff(Repository repository, RevCommit headCommit) throws Exception {
        RevCommit diffWith = headCommit.getParent(0);
        FileOutputStream stdout = new FileOutputStream(this.config.getDiffPath());
        try (DiffFormatter diffFormatter = new DiffFormatter(stdout)) {
            diffFormatter.setRepository(repository);
            for (DiffEntry entry : diffFormatter.scan(diffWith, headCommit)) {
                diffFormatter.format(diffFormatter.toFileHeader(entry));
            }
        }
    }

    protected void execute() {
        try (Repository repo = openRepository(this.config.getBuggySourceDirectoryPath())) {
            for (RevCommit currentCommit : getCommits(repo, this.config.getStartCommit(), this.config.getEndCommit())) {
                try {
                    Process process = Runtime.getRuntime().exec("git checkout -f -- . ", null, new File(this.config.getBuggySourceDirectoryPath()));
                    process.waitFor();
//                    process = Runtime.getRuntime().exec("git diff " + currentCommit.getName() + " " +currentCommit.getParent(0).getName(), null, new File(this.config.getDiffPath()));
                    ProcessBuilder pb = new ProcessBuilder("git", "diff", currentCommit.getName(), currentCommit.getParent(0).getName());
                    pb.directory(new File(this.config.getBuggySourceDirectoryPath()));
                    pb.inheritIO();
                    pb.redirectOutput(new File(this.config.getDiffPath()));
                    Process p = pb.start();
                    p.waitFor();


                    this.config.setCurrentCommit(currentCommit.getName());
                    diff(repo, currentCommit);
                    FeatureList features = new FeatureList(this.config);
                    List<FeatureAnalyzer> featureAnalyzers = new ArrayList<>();

                    Diff editScript = null;
                    if (this.config.getLauncherMode() == LauncherMode.REPAIR_PATTERNS ||
                            this.config.getLauncherMode() == LauncherMode.ALL) {
                        RepairPatternDetector detector = new RepairPatternDetector(this.config);
                        editScript = detector.getEditScript();
                        featureAnalyzers.add(detector);
                    }
                    if (this.config.getLauncherMode() == LauncherMode.REPAIR_ACTIONS ||
                            this.config.getLauncherMode() == LauncherMode.ALL) {
                        featureAnalyzers.add(new RepairActionDetector(this.config, editScript));
                    }
//                    if (this.config.getLauncherMode() == LauncherMode.METRICS ||
//                            this.config.getLauncherMode() == LauncherMode.ALL) {
//                        featureAnalyzers.add(new MetricExtractor(this.config));
//                    }


                    for (FeatureAnalyzer featureAnalyzer : featureAnalyzers) {
                        features.add(featureAnalyzer.analyze());
                    }
                    if (this.config.getOutputDirectoryPath() != null) {
                        JSONObject json = new JSONObject(features.toString());
                        JSONOutputFileCreator.writeJSONfile(json.toString(4), this.config);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public static void main(String[] args) throws Exception {
        Launcher launcher = new Launcher(args);
        launcher.execute();
    }

}
