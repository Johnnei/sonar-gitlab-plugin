package org.johnnei.sgp.it;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeThat;

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
	private static final String GITLAB_REPO = String.format(
		"http://root:%s@%s/root/sgp-it.git",
		ADMIN_PASSWORD.replaceAll("@", "%40").replaceAll("!", "%21"),
		GITLAB_HOST
	);
	private static final String SONARQUBE_HOST = getProperty("sonarqube.host", "http://localhost:9000");
	private static final String OS_SHELL = getProperty("os.shell", "/bin/bash");
	private static final String OS_COMMAND = getProperty("os.command", "-c");

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

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

	@BeforeClass
	public static void setUpClass() {
		LOGGER.info("GitLab Host: {}", GITLAB_HOST);
		LOGGER.info("GitLab URL: {}", GITLAB_URL);
		LOGGER.info("GitLab Repo: {}", GITLAB_REPO);
		LOGGER.info("SonarQube URL: {}", SONARQUBE_HOST);
	}

	@Before
	public void setUp() throws Exception {
		File repo = temporaryFolder.newFolder("repo");
		commandLine = new CommandLine(OS_SHELL, OS_COMMAND, repo);

		prepareGitRepo(repo);

		ensureAdminCreated();
		createProject();
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
		commandLine.startAndAwait("git remote add origin " + GITLAB_REPO);
		commandLine.startAndAwait("git config user.email \"example@example.com\"");
		commandLine.startAndAwait("git config user.name \"SGP Integration\"");
	}

	@After
	public void tearDown() throws Exception {
		deleteProject();
	}

	protected String gitCommitAll() throws IOException {
		commandLine.startAndAwait("git add .");
		commandLine.startAndAwait("git commit -m \"My commit message\"");
		commandLine.startAndAwait("git push -u origin master");
		return getLastCommit();
	}

	private String getLastCommit() throws IOException {
		return commandLine.startAndAwaitOutput("git log -n 1 --format=%H");
	}

	protected void sonarAnalysis(String commitHash) throws IOException {
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
			// Run analysis in issues mode in order to process the issues on the scanner side
			" -Dsonar.analysis.mode=issues" +
			// The host at which the SonarQube instance with our plugin is running.
			" -Dsonar.host.url=" + SONARQUBE_HOST +
			// The host at which our target gitlab instance is running.
			" -Dsonar.gitlab.uri=" + GITLAB_URL +
			// The authentication token to access the project within Gitlab
			" -Dsonar.gitlab.auth.token=" + gitLabAuthToken +
			// The project to comment on
			" -Dsonar.gitlab.analyse.project=root/sgp-it" +
			// The commit we're analysing.
			" -Dsonar.gitlab.analyse.commit=" + commitHash;
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
		LOGGER.info("Ensuring that there is no left over project.");

		List<GitlabProject> projects = gitlabApi.getAllProjects();
		assumeThat(projects, IsEmptyCollection.empty());

		LOGGER.info("Creating Project");
		project = gitlabApi.createProject("sgp-it");
		assertNotNull("Failed to create project in GitLab", project);
	}

	private void deleteProject() throws IOException {
		LOGGER.debug("Removing project from GitLab.");
		if (project != null) {
			gitlabApi.deleteProject(project.getId());
		}
		LOGGER.debug("Removing project from SonarQube.");
	}
}
