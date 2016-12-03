package org.johnnei.internal.sonar;

import javax.annotation.CheckForNull;

import java.io.File;
import java.io.IOException;

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabProject;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.config.Settings;

import org.johnnei.sonar.GitLabPlugin;

/**
 * Class to create a domain orientated facade of the SonarQube settings.
 */
@BatchSide
@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public class GitLabPluginConfiguration {

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
		return GitlabAPI.connect(getGitLabUrl(), getGitLabToken());
	}

	public void initialiseProject() throws IOException {
		String projectName = settings.getString(GitLabPlugin.GITLAB_PROJECT_NAME);
		if (isBlank(projectName)) {
			throw new IllegalArgumentException(String.format("Missing '%s' property.", GitLabPlugin.GITLAB_PROJECT_NAME));
		}

		GitlabAPI gitlabApi = createGitLabConnection();
		project = gitlabApi.getAllProjects().stream()
			.filter(p -> projectName.equals(p.getNameWithNamespace()))
			.findAny()
			.orElseThrow(() -> new IllegalArgumentException("Failed to find project '%s'. Is the user authorized to access the project?"));
	}

	public void setBaseDir(File baseDir) {
		gitBaseDir = findGitBaseDir(baseDir);
	}

	private File findGitBaseDir(@CheckForNull File baseDir) {
		if (baseDir == null) {
			return null;
		}
		if (new File(baseDir, ".git").exists()) {
			return baseDir;
		}
		return findGitBaseDir(baseDir.getParentFile());
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
