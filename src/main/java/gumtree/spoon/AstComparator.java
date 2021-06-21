package gumtree.spoon;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Map;
import java.io.*;
import java.net.*;

import add.entities.Feature;
import add.entities.FeatureList;
import add.features.detector.repairactions.RepairActionDetector;
import add.features.detector.repairpatterns.RepairPatternDetector;
import add.main.Config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.DiffImpl;
import spoon.SpoonModelBuilder;
import spoon.compiler.SpoonResource;
import spoon.compiler.SpoonResourceHelper;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.FactoryImpl;
import spoon.support.DefaultCoreFactory;
import spoon.support.StandardEnvironment;
import spoon.support.compiler.VirtualFile;
import spoon.support.compiler.jdt.JDTBasedSpoonCompiler;
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
import com.google.gson.JsonObject;


/**
 * Computes the differences between two CtElements.
 *
 * @author Matias Martinez, matias.martinez@inria.fr
 */
public class AstComparator {
	// For the moment, let's create a factory each type we get a type.
	// Sharing the factory produces a bug when asking the path of different types
	// (>1)
	// private final Factory factory;

	static {
		// default 0.3
		// it seems that default value is really bad
		// 0.1 one failing much more changes
		// 0.2 one failing much more changes
		// 0.3 one failing test_t_224542
		// 0.4 fails for issue31
		// 0.5 fails for issue31
		// 0.6 OK
		// 0.7 1 failing
		// 0.8 2 failing
		// 0.9 two failing tests with more changes
		// see GreedyBottomUpMatcher.java in Gumtree
		System.setProperty("gt.bum.smt", "0.6");

		// default 2
		// 0 is really bad for 211903 t_224542 225391 226622
		// 1 is required for t_225262 and t_213712 to pass
		System.setProperty("gt.stm.mh", "1");

		// default 1000
		// 0 fails
		// 1 fails
		// 10 fails
		// 100 OK
		// 1000 OK
		// see AbstractBottomUpMatcher#SIZE_THRESHOD in Gumtree
		// System.setProperty("gumtree.match.bu.size","10");
		// System.setProperty("gt.bum.szt", "1000");
	}
	/**
	 * By default, comments are ignored
	 */
	private boolean includeComments = false;

	public AstComparator() {
		super();
	}

	public AstComparator(boolean includeComments) {
		super();
		this.includeComments = includeComments;
	}

	public AstComparator(Map<String, String> configuration) {
		super();
		for (String k : configuration.keySet()) {
			System.setProperty(k, configuration.get(k));
		}
	}

	protected Factory createFactory() {
		Factory factory = new FactoryImpl(new DefaultCoreFactory(), new StandardEnvironment());
		factory.getEnvironment().setNoClasspath(true);
		factory.getEnvironment().setCommentEnabled(includeComments);
		return factory;
	}

	/**
	 * compares two java files
	 */
	public Diff compare(File f1, File f2) throws Exception {
		return this.compare(getCtType(f1), getCtType(f2));
	}

	/**
	 * compares two snippets
	 */
	public Diff compare(String left, String right) {
		return compare(getCtType(left), getCtType(right));
	}

	/**
	 * compares two snippets that come from the files given as argument
	 */
	public Diff compare(String left, String right, String filenameLeft, String filenameRight) {
		return compare(getCtType(left, filenameLeft), getCtType(right, filenameRight));
	}

	/**
	 * compares two AST nodes
	 */
	public Diff compare(CtElement left, CtElement right) {
		final SpoonGumTreeBuilder scanner = new SpoonGumTreeBuilder();
		return new DiffImpl(scanner.getTreeContext(), scanner.getTree(left), scanner.getTree(right));
	}

	public CtType getCtType(File file) throws Exception {

		SpoonResource resource = SpoonResourceHelper.createResource(file);
		return getCtType(resource);
	}

	public CtType getCtType(SpoonResource resource) {
		Factory factory = createFactory();
		factory.getModel().setBuildModelIsFinished(false);
		SpoonModelBuilder compiler = new JDTBasedSpoonCompiler(factory);
		compiler.getFactory().getEnvironment().setLevel("OFF");
		compiler.addInputSource(resource);
		compiler.build();

		if (factory.Type().getAll().size() == 0) {
			return null;
		}

		// let's first take the first type.
		CtType type = factory.Type().getAll().get(0);
		// Now, let's ask to the factory the type (which it will set up the
		// corresponding
		// package)
		return factory.Type().get(type.getQualifiedName());
	}

	public CtType<?> getCtType(String content) {
		return getCtType(content, "/test");
	}

	public CtType<?> getCtType(String content, String filename) {
		VirtualFile resource = new VirtualFile(content, filename);
		return getCtType(resource);
	}

//	public Iterable<RevCommit> getCommits(Repository repository, String startCommitId, String endCommitId)
//			throws Exception {
//		ObjectId from = repository.resolve(startCommitId);
//		ObjectId to = repository.resolve(endCommitId);
//		try (Git git = new Git(repository)) {
//			List<RevCommit> revCommits = StreamSupport.stream(git.log().addRange(from, to).call()
//					.spliterator(), false)
//					.filter(r -> r.getParentCount() == 1)
//					.collect(Collectors.toList());
//			Collections.reverse(revCommits);
//			return revCommits;
//		}
//	}


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


	public static void main(String[] args) throws Exception {
		ServerSocket ss = new ServerSocket(12345);
		while (true) {
		Socket s = ss.accept();
		System.out.println("Receive new connection: " + s.getInetAddress());
		BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
		PrintWriter writer = new PrintWriter(s.getOutputStream(), true);
		String str = "";
		str = reader.readLine();
		System.out.println("client says: " + str);
		if(str == null) {
			s.close();
			continue;
		}
		if(str.equals("break")) {
			s.close();
			break;
		}
		String[] arguments = str.split(";", 3);
		if (arguments.length != 3) {
			break;
		}
		saveDiffMetrics(arguments);
		writer.println("");
		writer.flush();
		s.close();
	}
	ss.close();
	}

	private static void saveDiffMetrics(String[] args) throws Exception {
		if (args.length != 3) {
			System.out.println("Usage: DiffSpoon <file_1>  <file_2> <out_file>");
			return;
		}
		final Diff result = new AstComparator().compare(new File(args[0]), new File(args[1]));
		FeatureList features = new FeatureList(new Config());
		features.add(new RepairPatternDetector(new Config(), result).analyze());
		features.add(new RepairActionDetector(new Config(), result).analyze());
		JsonObject res = new JsonObject();
		for (Feature feature : features.getFeatureList()) {
			for (String featureName : feature.getFeatureNames()) {
				res.addProperty(featureName, feature.getFeatureCounter(featureName));
			}
		}
		res.add("operations", result.toJson());
		try (Writer writer = new FileWriter(args[2])) {
			Gson gson = new GsonBuilder().create();
			gson.toJson(res, writer);
		}
	}
}
