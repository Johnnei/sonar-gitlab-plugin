package org.johnnei.sgp.it.framework.gitlab;

import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
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
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.sgp.internal.gitlab.api.JacksonConfigurator;
import org.johnnei.sgp.internal.gitlab.api.v4.AuthFilter;
import org.johnnei.sgp.internal.gitlab.api.v4.GitLabApi;
import org.johnnei.sgp.internal.gitlab.api.v4.model.CommitComment;
import org.johnnei.sgp.internal.gitlab.api.v4.model.GitLabAccessLevel;
import org.johnnei.sgp.internal.gitlab.api.v4.model.GitLabProject;
import org.johnnei.sgp.internal.gitlab.api.v4.model.GitLabSession;
import org.johnnei.sgp.internal.gitlab.api.v4.model.GitLabUser;
import org.johnnei.sgp.internal.gitlab.api.v4.model.GitlabCommitStatus;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class GitLabSupport {

	private static final Logger LOGGER = LoggerFactory.getLogger(GitLabSupport.class);

	private static final String ADMIN_PASSWORD = "Test1234@!";

	private static final String INTEGRATION_USER = "Integrator";

	private static final Pattern INPUT_FIELD = Pattern.compile("<input(.*?)>");

	private static final Pattern NAME_ATTRIBUTE = Pattern.compile("name=\"(.*?)\"");

	private static final Pattern VALUE_ATTRIBUTE = Pattern.compile("value=\"(.*?)\"");

	private final String host;

	private final String url;

	private GitLabApi rootUser;

	private GitLabApi sonarUser;

	private GitLabProject project;

	private String sonarUserToken;

	public GitLabSupport(String host, String url) {
		this.host = host;
		this.url = url;
	}

	public String getGitlabRepo() {
		return String.format(
			"http://root:%s@%s/root/%s.git",
			ADMIN_PASSWORD.replaceAll("@", "%40").replaceAll("!", "%21"),
			host,
			project.getName().toLowerCase()
		);
	}

	public void ensureAdminCreated() throws Exception {
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
			.url(url)
			.build();

		Response response = client.newCall(homeRequest).execute();

		if (isRedirectedToResetPassword(response)) {
			Request postRequest = createRegisterAdminRequest(response);
			LOGGER.info("Registering admin account: {}", postRequest.body());
			Response createResponse = client.newCall(postRequest).execute();
			if (!createResponse.isSuccessful()) {
				throw new IllegalStateException(String.format("Submit of reset password failed: %s", createResponse.message()));
			}
			LOGGER.info("GitLab Root user initialized.");
		}
	}

	private GitLabApi createApi(String username, String password) {
		GitLabApi api = createApi(null);
		GitLabSession session = api.createSession(username, password);
		return createApi(session.getPrivateToken());
	}

	private GitLabApi createApi(String token) {
		ResteasyWebTarget client = new ResteasyClientBuilder().build()
			.target(url)
			.register(JacksonConfigurator.class)
			.register((ClientRequestFilter) requestContext -> LOGGER.debug("Request: {}", requestContext.getUri()));

		if (token != null) {
			client = client.register(new AuthFilter(token));
		}

		return client.proxy(GitLabApi.class);
	}

	public void ensureItUserCreated() throws IOException {
		if (rootUser == null) {
			rootUser = createApi("root", ADMIN_PASSWORD);
			LOGGER.info("Logged in as root to create integration user.");
		}

		if (rootUser.getUsers().stream().noneMatch(user -> INTEGRATION_USER.equals(user.getUsername()))) {
			rootUser.createUser("it@localhost.nl", ADMIN_PASSWORD, INTEGRATION_USER, "SonarQube Bot", true);
			LOGGER.info("GitLab integration user created.");
		}

		if (sonarUserToken == null) {
			GitLabSession session = createApi(null).createSession(INTEGRATION_USER, ADMIN_PASSWORD);
			sonarUserToken = session.getPrivateToken();
			sonarUser = createApi(sonarUserToken);
			LOGGER.info("Logged in as {}.", INTEGRATION_USER);
		} else {
			LOGGER.info("Re-using existing GitLab session.");
		}
	}

	public void ensureProjectLimitRaised() throws IOException {
		GitLabUser user = rootUser.getUser();
		if (user.getProjectsLimit() < 1000) {
			user = rootUser.updateUser(user.getId(), user.getEmail(), 1000);
			assertThat("Project limit should be raised in order to be able to run all tests.", user.getProjectsLimit(), equalTo(1000));
		}
	}

	public void createProject(Class<?> clazz, TestName testName) throws IOException {
		LOGGER.info("Ensuring that there is no duplicate project.");

		String projectName = String.format("sgp-it-%s-%s", clazz.getSimpleName(), testName.getMethodName());

		List<String> projects = rootUser.getProjects().stream()
			.map(GitLabProject::getName)
			.collect(Collectors.toList());
		assertThat("Project with that name already exists. Duplicate test name.", projects, not(hasItem(equalTo(projectName))));

		LOGGER.info("Creating Project");
		project = rootUser.createProject(projectName);
		assertNotNull("Failed to create project in GitLab", project);

		rootUser.addProjectMember(project.getId(), sonarUser.getUser().getId(), GitLabAccessLevel.DEVELOPER.getLevel());
		LOGGER.info("Added {} as developer on project.", INTEGRATION_USER);
	}
	public List<String> getAllCommitComments(String commitHash) throws IOException {
		return fetchCommitComments(commitHash, commitComment -> true);
	}

	public List<String> getCommitSummary(String commitHash) throws IOException {
		return fetchCommitComments(commitHash, GitLabSupport::filterSummary);
	}

	public List<String> getCommitComments(String commitHash) throws IOException {
		return fetchCommitComments(commitHash, GitLabSupport::filterIssues);
	}

	private List<String> fetchCommitComments(String commitHash, Predicate<CommitComment> filter) throws IOException {
		return sonarUser.getCommitComments(project.getId(), commitHash)
			.stream()
			.filter(filter)
			.map(CommitComment::getNote)
			.collect(Collectors.toList());
	}

	public GitlabCommitStatus getCommitStatus(String commit) throws IOException {
		return sonarUser.getCommitStatuses(project.getId(), commit).stream().filter(status -> "SonarQube".equals(status.getName())).findAny().orElse(null);
	}

	private static boolean filterIssues(CommitComment comment) {
		return comment.getLine() != null;
	}

	private static boolean filterSummary(CommitComment comment) {
		return comment.getLine() == null;
	}

	private boolean isRedirectedToResetPassword(Response response) {
		if (response.priorResponse() == null) {
			return false;
		}

		if (!response.priorResponse().isRedirect()) {
			return false;
		}

		return response.priorResponse().header("location").contains("/users/password/edit");
	}

	private Request createRegisterAdminRequest(Response response) throws IOException {
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
			.url(url+ "/users/password")
			.post(formBodyBuilder.build())
			.build();
	}

	public String getSonarUserToken() {
		return sonarUserToken;
	}

	public String getProjectName() {
		return "root/" + project.getName();
	}

	public String getUrl() {
		return url;
	}
}
