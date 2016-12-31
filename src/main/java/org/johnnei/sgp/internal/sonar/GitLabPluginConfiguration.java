package org.johnnei.sgp.internal.sonar;

import javax.annotation.CheckForNull;

import java.io.File;
import java.io.IOException;

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.GitlabAPIException;
import org.gitlab.api.TokenType;
import org.gitlab.api.models.GitlabProject;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import org.johnnei.sgp.internal.util.Stopwatch;
import org.johnnei.sgp.sonar.GitLabPlugin;

/**
 * Class to create a domain orientated facade of the SonarQube settings.
 */
@BatchSide
@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public class GitLabPluginConfiguration {

	private static final Logger LOGGER = Loggers.get(GitLabPluginConfiguration.class);

	private final Settings settings;

	private GitlabProject project;

	private File gitBaseDir;

	public GitLabPluginConfiguration(Settings settings) {
		this.settings = settings;
	}

	public boolean isEnabled() {
		return isNotBlank(settings.getString(GitLabPlugin.GITLAB_COMMIT_HASH));
	}

	private String getGitLabUrl() {
		return settings.getString(GitLabPlugin.GITLAB_INSTANCE_URL);
	}

	private String getGitLabToken() {
		return settings.getString(GitLabPlugin.GITLAB_AUTH_TOKEN);
	}

	public GitlabAPI createGitLabConnection() {
		String url = getGitLabUrl();
		String token = getGitLabToken();
		if (isBlank(url)) {
			throw new IllegalArgumentException("GitLab Instance URL property hasn't been set.");
		}
		if (isBlank(token)) {
			throw new IllegalArgumentException("GitLab user token hasn't been set.");
		}

		GitlabAPI api = createConnection(url, token, TokenType.ACCESS_TOKEN);
		if (!attemptConnect(api, TokenType.ACCESS_TOKEN.name())) {
			LOGGER.warn("Failed to authorize with GitLab Access Token. Falling back to less secure usage of private token.");
			api = createConnection(url, token, TokenType.PRIVATE_TOKEN);
			if (!attemptConnect(api, TokenType.PRIVATE_TOKEN.name())) {
				throw new IllegalArgumentException("Failed to authorize user with API.");
			}
		}

		return api;
	}

	private boolean attemptConnect(GitlabAPI api, String tokenType) {
		try {
			api.getUser();
		} catch (GitlabAPIException e) {
			LOGGER.debug("Failed to authorize with {}.", tokenType, e);
			if (e.getResponseCode() == 401) {
				return false;
			}
		} catch (IOException e) {
			throw new IllegalStateException("Failed to connect with GitLab.", e);
		}

		return true;
	}

	GitlabAPI createConnection(String url, String token, TokenType tokenType) {
		return GitlabAPI.connect(url, token, tokenType);
	}

	public void initialiseProject() throws IOException {
		String projectName = settings.getString(GitLabPlugin.GITLAB_PROJECT_NAME);
		if (isBlank(projectName)) {
			throw new IllegalArgumentException(String.format("Missing '%s' property.", GitLabPlugin.GITLAB_PROJECT_NAME));
		}

		Stopwatch stopwatch = new Stopwatch();
		stopwatch.start("Looking up GitLab project.");
		GitlabAPI gitlabApi = createGitLabConnection();
		project = gitlabApi.getProjects().stream()
			.filter(p -> {
				String name = String.format("%s/%s", p.getNamespace().getName(), p.getName());
				LOGGER.debug("Filtering \"{}\" = \"{}\"", name, projectName);
				return projectName.equals(name);
			})
			.findAny()
			.orElseThrow(() -> new IllegalArgumentException(String.format(
				"Failed to find project '%s'. Is the user authorized to access the project?",
				projectName
			)));
		stopwatch.stop();
	}

	public void setBaseDir(File gitBaseDir) {
		this.gitBaseDir = gitBaseDir;
	}

	public File getGitBaseDir() {
		return gitBaseDir;
	}

	public GitlabProject getProject() {
		return project;
	}

	public String getCommitHash() {
		return settings.getString(GitLabPlugin.GITLAB_COMMIT_HASH);
	}

	private static boolean isNotBlank(@CheckForNull String string) {
		return !isBlank(string);
	}

	private static boolean isBlank(@CheckForNull String string) {
		if (string == null) {
			return true;
		}

		return string.trim().isEmpty();
	}
}
