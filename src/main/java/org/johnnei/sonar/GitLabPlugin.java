package org.johnnei.sonar;

import org.sonar.api.Plugin;
import org.sonar.api.Properties;
import org.sonar.api.Property;

import org.johnnei.internal.sonar.CommitAnalysisBuilder;
import org.johnnei.internal.sonar.CommitIssueJob;
import org.johnnei.internal.sonar.GitLabPluginConfiguration;

/**
 * Class which configures the GitLab plugin within the SonarQube instance.
 */
@Properties({
	@Property(
		key = GitLabPlugin.GITLAB_AUTH_TOKEN,
		name = "GitLab User Token",
		description = "The private token or access token of the SonarQube user within the GitLab instance."
	),
	@Property(
		key = GitLabPlugin.GITLAB_INSTANCE_URL,
		name = "GitLab instance URL",
		description = "The URL at which the GitLab instance can be reached."
	),
	@Property(
		key = GitLabPlugin.GITLAB_PROJECT_NAME,
		name = "GitLab Project Name",
		description = "The namespace and name of the GitLab project that is being analysed."
	),
	@Property(
		key = GitLabPlugin.GITLAB_COMMIT_HASH,
		name = "Git commit hash to analyse",
		description = "The commit which will be considered responsible for all new issues",
		global = false
	)
})
public class GitLabPlugin implements Plugin {

	public static final String GITLAB_INSTANCE_URL = "sonar.gitlab.uri";
	public static final String GITLAB_AUTH_TOKEN = "sonar.gitlab.auth.token";
	public static final String GITLAB_PROJECT_NAME = "sonar.gitlab.analyse.project";
	public static final String GITLAB_COMMIT_HASH = "sonar.gitlab.analyse.commit";

	@Override
	public void define(Context context) {
		context
			.addExtension(GitLabPluginConfiguration.class)
			.addExtension(CommitAnalysisBuilder.class)
			.addExtension(CommitIssueJob.class);
	}
}
