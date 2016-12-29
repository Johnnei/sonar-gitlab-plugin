package org.johnnei.sgp.it.framework;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabSession;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Created by Johnnei on 2016-12-04.
 */
public abstract class IntegrationTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTest.class);

	private static final String ADMIN_PASSWORD = "Test1234@!";

	private static final Pattern INPUT_FIELD = Pattern.compile("<input(.*?)>");

	private static final Pattern NAME_ATTRIBUTE = Pattern.compile("name=\"(.*?)\"");

	private static final Pattern VALUE_ATTRIBUTE = Pattern.compile("value=\"(.*?)\"");

	private static final String GITLAB_HOST = getProperty("gitlab.host", "localhost:80");
	private static final String GITLAB_URL = String.format("http://%s", GITLAB_HOST);
	private static final String SONARQUBE_HOST = getProperty("sonarqube.host", "http://localhost:9000");
	private static final String OS_SHELL = getProperty("os.shell", "/bin/bash");
	private static final String OS_COMMAND = getProperty("os.command", "-c");

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public TestName testName = new TestName();

	protected GitlabAPI gitlabApi;

	protected GitlabProject project;

	private CommandLine commandLine;

	private String gitLabAuthToken;

	private static String getProperty(String key, String defaultValue) {
		String value = System.getProperty(key);
		if (value == null || value.trim().isEmpty() || value.contains("$")) {
			LOGGER.debug("Resolve failed: \"{}\" -> \"{}\"", key, value);
			value = defaultValue;
		}

		return value;
	}

	private String getGitlabRepo() {
		return String.format(
			"http://root:%s@%s/root/%s.git",
			ADMIN_PASSWORD.replaceAll("@", "%40").replaceAll("!", "%21"),
			GITLAB_HOST,
			project.getName().toLowerCase()
		);
	}

	@BeforeClass
	public static void setUpClass() {
		LOGGER.info("GitLab Host: {}", GITLAB_HOST);
		LOGGER.info("GitLab URL: {}", GITLAB_URL);
		LOGGER.info("SonarQube URL: {}", SONARQUBE_HOST);
	}

	@Before
	public void setUp() throws Exception {
		File repo = temporaryFolder.newFolder("repo");
		commandLine = new CommandLine(OS_SHELL, OS_COMMAND, repo);

		ensureAdminCreated();
		createProject();

		prepareGitRepo(repo);
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
		commandLine.startAndAwait("git remote add origin " + getGitlabRepo());
		commandLine.startAndAwait("git config user.email \"example@example.com\"");
		commandLine.startAndAwait("git config user.name \"SGP Integration\"");
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

	protected void gitCheckout(String commitHash) throws IOException {
		commandLine.startAndAwait("git checkout " + commitHash);
	}

	protected void gitAdd(String paths) throws IOException {
		commandLine.startAndAwait("git add " + paths);
	}

	protected String gitCommit() throws IOException {
		return gitCommit("My commit message");
	}
	protected String gitCommit(String message) throws IOException {
		commandLine.startAndAwait(String.format("git commit -m \"%s\"", message));
		commandLine.startAndAwait("git push -u origin master");
		return getLastCommit();
	}

	protected String gitCommitAll() throws IOException {
		gitAdd(".");
		return gitCommit();
	}

	private String getLastCommit() throws IOException {
		return commandLine.startAndAwaitOutput("git log -n 1 --format=%H");
	}

	protected void remoteMatchedComment(List<String> comments, String message) {
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

	protected void sonarAnalysis(String commitHash) throws IOException {
		sonarAnalysis(commitHash, true);
	}

	protected void sonarAnalysis(String commitHash, boolean incremental) throws IOException {
		LOGGER.info("Starting SonarQube Analysis.");

		String argument = "mvn -B" +
			// Ensure a clean state
			" clean" +
			// Provide binaries
			" compile " +
			// Invoke sonar analysis
			" sonar:sonar" +
			// Enable Maven debug to not suppress the Sonar debug logging
			" --debug" +
			// Enable Sonar debug logging to analyse test failures.
			" -Dsonar.log.level=DEBUG" +
			// The host at which the SonarQube instance with our plugin is running.
			" -Dsonar.host.url=" + SONARQUBE_HOST;

		if (incremental) {
			// Run analysis in issues mode in order to process the issues on the scanner side
			argument += " -Dsonar.analysis.mode=issues" +
				// The host at which our target gitlab instance is running.
				" -Dsonar.gitlab.uri=" + GITLAB_URL +
				// The authentication token to access the project within Gitlab
				" -Dsonar.gitlab.auth.token=" + gitLabAuthToken +
				// The project to comment on
				" -Dsonar.gitlab.analyse.project=root/" + project.getName() +
				// The commit we're analysing.
				" -Dsonar.gitlab.analyse.commit=" + commitHash;
		}

		commandLine.startAndAwait(argument);
	}

	private void ensureAdminCreated() throws Exception {
		LOGGER.info("Verifying that GitLab Admin account exists.");

		OkHttpClient client = new OkHttpClient.Builder()
			.cookieJar(new CookieJar() {

				private List<Cookie> cookies = Collections.emptyList();

				@Override
				public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
					this.cookies = cookies;
				}

				@Override
				public List<Cookie> loadForRequest(HttpUrl url) {
					return cookies;
				}
			})
			.build();


		Request homeRequest = new Request.Builder()
			.url(GITLAB_URL)
			.build();

		Response response = client.newCall(homeRequest).execute();

		if (isRedirectedToResetPassword(response)) {
			Request postRequest = createRegisterAdminRequest(response);
			LOGGER.info("Registering admin account: {}", postRequest.body());
			Response createResponse = client.newCall(postRequest).execute();
			if (!createResponse.isSuccessful()) {
				throw new IllegalStateException(String.format("Submit of reset password failed: %s", createResponse.message()));
			}
		}

		GitlabSession session = GitlabAPI.connect(GITLAB_URL, "root", ADMIN_PASSWORD);
		gitLabAuthToken = session.getPrivateToken();
		gitlabApi = GitlabAPI.connect(GITLAB_URL, gitLabAuthToken);
		LOGGER.info("GitLab API Initialized.");
	}

	private static Request createRegisterAdminRequest(Response response) throws IOException {
		String body = response.body().string();

		FormBody.Builder formBodyBuilder = new FormBody.Builder()
			.add("user[password]", ADMIN_PASSWORD)
			.add("user[password_confirmation]", ADMIN_PASSWORD);

		Matcher inputFields = INPUT_FIELD.matcher(body);
		while (inputFields.find()) {
			String inputField = inputFields.group();

			Matcher nameMatcher = NAME_ATTRIBUTE.matcher(inputField);
			Matcher valueMatcher = VALUE_ATTRIBUTE.matcher(inputField);

			if (!nameMatcher.find()) {
				throw new IllegalStateException("Failed to extract name attribute from: " + inputField);
			}

			if (valueMatcher.find() && !"commit".equalsIgnoreCase(nameMatcher.group(1))) {
				LOGGER.debug("Adding {} = {} to form body.", nameMatcher.group(1), valueMatcher.group(1));
				formBodyBuilder.add(nameMatcher.group(1), valueMatcher.group(1));
			} else {
				LOGGER.debug("Ignoring valueless input: {}", nameMatcher.group(1));
			}
		}

		return new Request.Builder()
			.url(GITLAB_URL + "/users/password")
			.post(formBodyBuilder.build())
			.build();
	}

	private static boolean isRedirectedToResetPassword(Response response) {
		if (response.priorResponse() == null) {
			return false;
		}

		if (!response.priorResponse().isRedirect()) {
			return false;
		}

		return response.priorResponse().header("location").contains("/users/password/edit");
	}

	private void createProject() throws IOException {
		LOGGER.info("Ensuring that there is no duplicate project.");

		String projectName = String.format("sgp-it-%s-%s", this.getClass().getSimpleName(), testName.getMethodName());

		List<String> projects = gitlabApi.getAllProjects().stream()
			.map(GitlabProject::getName)
			.collect(Collectors.toList());
		assertThat("Project with that name already exists. Duplicate test name.", projects, not(hasItem(equalTo(projectName))));

		LOGGER.info("Creating Project");
		project = gitlabApi.createProject(projectName);
		assertNotNull("Failed to create project in GitLab", project);
	}
}
