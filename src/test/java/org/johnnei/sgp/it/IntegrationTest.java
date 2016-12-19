package org.johnnei.sgp.it;

import java.io.File;
import java.io.IOException;
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

	private static final String GITLAB_HOST = getProperty("gitlab.host", "http://localhost:80");
	private static final String SONARQUBE_HOST = getProperty("sonarqube.host", "http://localhost:9000");
	private static final String OS_SHELL = getProperty("os.shell", "/bin/bash");
	private static final String OS_COMMAND = getProperty("os.command", "-c");

	protected GitlabAPI gitlabApi;

	protected GitlabProject project;

	private String gitLabAuthToken;

	private static String getProperty(String key, String defaultValue) {
		String value = System.getProperty(key);
		if (value == null || value.trim().isEmpty() || value.contains("$")) {
			value = defaultValue;
		}

		return value;
	}

	@Before
	public void setUp() throws Exception {
		LOGGER.debug("GitLab Host: {}", GITLAB_HOST);
		LOGGER.debug("SonarQube Host: {}", SONARQUBE_HOST);
		ensureAdminCreated();
		createProject();
	}

	@After
	public void tearDown() throws Exception {
		deleteProject();
	}

	protected void checkout(String ref) throws IOException {
		LOGGER.info("Checking out {}");
		Process process = new ProcessBuilder()
			.directory(new File("it-sources"))
			.command("git", "checkout", ref)
			.start();
		try {
			process.waitFor();
		} catch (InterruptedException e) {
			process.destroy();
		}
	}

	protected void sonarAnalysis(String commitHash) throws IOException {
		LOGGER.info("Starting SonarQube Analysis.");

		String argument = OS_COMMAND + " \"" +
				"mvn" +
				" clean" +
				" compile " +
				" sonar:sonar" +
				" -Dsonar.analysis.mode=issues" +
				" -Dsonar.host.url=" + SONARQUBE_HOST +
				" -Dsonar.gitlab.uri=" + GITLAB_HOST +
				" -Dsonar.gitlab.auth.token=" + gitLabAuthToken +
				" -Dsonar.gitlab.analyse.project=root/sgp-it" +
				" -Dsonar.gitlab.analyse.commit=" + commitHash +
				"\"";

		LOGGER.debug("Running: " + OS_SHELL + " " + argument);

		Process process = new ProcessBuilder()
			.directory(new File("it-sources"))
			.command(
				OS_SHELL,
				argument
			)
			.inheritIO()
			.start();
		try {
			int returnCode = process.waitFor();
			if (returnCode != 0) {
				throw new RuntimeException("Process failed: " + returnCode);
			}
		} catch (InterruptedException e) {
			process.destroy();
		}
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
			.url(GITLAB_HOST)
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

		GitlabSession session = GitlabAPI.connect(GITLAB_HOST, "root", ADMIN_PASSWORD);
		gitLabAuthToken = session.getPrivateToken();
		gitlabApi = GitlabAPI.connect(GITLAB_HOST, gitLabAuthToken);
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
			.url(GITLAB_HOST + "/users/password")
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
