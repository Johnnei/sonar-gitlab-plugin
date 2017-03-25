package org.johnnei.sgp.it.framework;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.sgp.it.framework.git.GitSupport;
import org.johnnei.sgp.it.framework.gitlab.GitLabSupport;
import org.johnnei.sgp.it.framework.sonarqube.SonarQubeSupport;

public abstract class IntegrationTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTest.class);

	private static final String GITLAB_HOST = getProperty("gitlab.host", "localhost:80");
	private static final String GITLAB_URL = String.format("http://%s", GITLAB_HOST);
	private static final String SONARQUBE_HOST = getProperty("sonarqube.host", "http://localhost:9000");
	private static final String OS_SHELL = getProperty("os.shell", "/bin/bash");
	private static final String OS_COMMAND = getProperty("os.command", "-c");

	private static final GitLabSupport gitlab = new GitLabSupport(GITLAB_HOST, GITLAB_URL);

	private SonarQubeSupport sonarqube;

	private GitSupport git;

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public TestName testName = new TestName();

	private CommandLine commandLine;

	protected File repoFolder;

	private static String getProperty(String key, String defaultValue) {
		String value = System.getProperty(key);
		if (value == null || value.trim().isEmpty() || value.contains("$")) {
			LOGGER.debug("Resolve failed: \"{}\" -> \"{}\"", key, value);
			value = defaultValue;
		}

		return value;
	}

	@BeforeClass
	public static void setUpClass() {
		LOGGER.info("GitLab Host: {}", GITLAB_HOST);
		LOGGER.info("GitLab URL: {}", GITLAB_URL);
		LOGGER.info("SonarQube URL: {}", SONARQUBE_HOST);
	}

	@Before
	public void setUp() throws Exception {
		repoFolder = temporaryFolder.newFolder("repo");
		prepareAccessOnFolder(repoFolder);

		gitlab.ensureAdminCreated();
		gitlab.ensureItUserCreated();
		gitlab.ensureProjectLimitRaised();
		gitlab.createProject(getClass(), testName);

		prepareGitRepo(repoFolder);
	}

	protected void prepareAccessOnFolder(File folder) {
		LOGGER.info("Configured command line working directory to: {}", folder.getAbsolutePath());
		commandLine = new CommandLine(OS_SHELL, OS_COMMAND, folder);
		sonarqube = new SonarQubeSupport(gitlab, commandLine, SONARQUBE_HOST);
		git = new GitSupport(commandLine);
	}

	private void prepareGitRepo(File repo) throws IOException {
		LOGGER.info("Preparing GIT repository in {}", repo.toPath().toString());
		Path sourceFolder = new File("it-sources").toPath();
		Files.walk(sourceFolder)
			.forEach(file -> {
				String destination = file.toString().replace(sourceFolder.toString(), repo.toPath().toString());
				try {
					Files.copy(file, Paths.get(destination), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new IllegalStateException("Failed to prepare git repository", e);
				}
			});

		commandLine.startAndAwait("git init");
		commandLine.startAndAwait("git remote add origin " + gitlab.getGitlabRepo());
		commandLine.startAndAwait("git config user.email \"example@example.com\"");
		commandLine.startAndAwait("git config user.name \"SGP Integration\"");
	}

	/**
	 * Commits the GIT ignore file as the root commit.
	 */
	protected void prepareFeatureBranch() throws IOException {
		git.add(".gitignore");
		git.commit();
		git.createBranch("feature");
	}

	protected Path getTestResource(String pathname) {
		URL url = IntegrationTest.class.getResource("/" + pathname);
		if (url == null) {
			LOGGER.warn("Failed to find resource: {}" + pathname);
			return null;
		}

		try {
			return new File(url.toURI()).toPath();
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid resource path", e);
		}
	}

	protected void removeMatchedComment(List<String> comments, String message) {
		// Remove a single matched comment.
		Iterator<String> commentsIterator = comments.iterator();
		while (commentsIterator.hasNext()) {
			String comment = commentsIterator.next();
			if (comment.equals(message)) {
				commentsIterator.remove();
				return;
			}
		}

		throw new IllegalStateException("Matcher passed but didn't remove message.");
	}

	protected SonarQubeSupport accessSonarQube() {
		return sonarqube;
	}

	protected GitLabSupport accessGitlab() {
		return gitlab;
	}

	protected GitSupport accessGit() {
		return git;
	}
}
