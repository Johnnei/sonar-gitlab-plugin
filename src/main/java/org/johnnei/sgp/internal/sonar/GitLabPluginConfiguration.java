package org.johnnei.sgp.internal.sonar;

import javax.annotation.CheckForNull;
import javax.ws.rs.ext.RuntimeDelegate;
import java.io.IOException;

import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import org.jboss.resteasy.client.jaxrs.ProxyConfig;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import org.johnnei.sgp.internal.gitlab.api.JacksonConfigurator;
import org.johnnei.sgp.internal.gitlab.api.v4.AuthFilter;
import org.johnnei.sgp.internal.gitlab.api.v4.GitLabApi;
import org.johnnei.sgp.internal.gitlab.api.v4.model.GitLabProject;
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

	private GitLabProject project;

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

	public GitLabApi createGitLabConnection() {
		String url = getGitLabUrl();
		String token = getGitLabToken();
		if (isBlank(url)) {
			throw new IllegalArgumentException("GitLab Instance URL property hasn't been set.");
		}
		if (isBlank(token)) {
			throw new IllegalArgumentException("GitLab user token hasn't been set.");
		}

		return createConnection(url, token);
	}

	GitLabApi createConnection(String url, String token) {
		RuntimeDelegate.setInstance(new ResteasyProviderFactory());

		ResteasyWebTarget target = new ResteasyClientBuilder()
			.register(JacksonConfigurator.class)
			.register(new AuthFilter(token))
			.build()
			.target(url);
		ProxyConfig config = new ProxyConfig(this.getClass().getClassLoader(), null, null);
		return ProxyBuilder.proxy(GitLabApi.class, target, config);
	}

	public void initialiseProject() throws IOException {
		String projectName = settings.getString(GitLabPlugin.GITLAB_PROJECT_NAME);
		if (isBlank(projectName)) {
			throw new IllegalArgumentException(String.format("Missing '%s' property.", GitLabPlugin.GITLAB_PROJECT_NAME));
		}

		Stopwatch stopwatch = new Stopwatch();
		stopwatch.start("Looking up GitLab project.");
		GitLabApi gitlabApi = createGitLabConnection();
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

	public GitLabProject getProject() {
		return project;
	}

	public String getCommitHash() {
		return settings.getString(GitLabPlugin.GITLAB_COMMIT_HASH);
	}

	public String getBaseBranch() {
		return settings.getString(GitLabPlugin.GITLAB_BASE_BRANCH);
	}

	public boolean isBreakPipelineEnabled() {
		return settings.getBoolean(GitLabPlugin.GITLAB_BREAK_PIPELINE);
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
