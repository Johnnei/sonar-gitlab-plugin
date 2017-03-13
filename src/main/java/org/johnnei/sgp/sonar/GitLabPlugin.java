package org.johnnei.sgp.sonar;

import org.sonar.api.Plugin;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;

import org.johnnei.sgp.internal.gitlab.DiffFetcher;
import org.johnnei.sgp.internal.gitlab.PipelineBreaker;
import org.johnnei.sgp.internal.sonar.CommitAnalysisBuilder;
import org.johnnei.sgp.internal.sonar.CommitIssueJob;
import org.johnnei.sgp.internal.sonar.GitLabPluginConfiguration;

/**
 * Class which configures the GitLab plugin within the SonarQube instance.
 */
@Properties({
	@Property(
		key = GitLabPlugin.GITLAB_AUTH_TOKEN,
		name = "GitLab User Token",
		description = "The private token or access token of the SonarQube user within the GitLab instance.",
		project = true,
		type = PropertyType.PASSWORD
	),
	@Property(
		key = GitLabPlugin.GITLAB_INSTANCE_URL,
		name = "GitLab instance URL",
		description = "The URL at which the GitLab instance can be reached.",
		project = true
	),
	@Property(
		key = GitLabPlugin.GITLAB_PROJECT_NAME,
		name = "GitLab Project Name",
		description = "The namespace and name of the GitLab project that is being analysed.",
		project = true,
		global = false
	),
	@Property(
		key = GitLabPlugin.GITLAB_COMMIT_HASH,
		name = "Git commit hash to analyse",
		description = "The commit which will be considered responsible for all new issues",
		global = false
	),
	@Property(
		key = GitLabPlugin.GITLAB_BASE_BRANCH,
		name = "Git base branch",
		description = "The source branch of the analysed branch. (Ex. with GitFlow this would be develop)",
		defaultValue = "master",
		project = true
	),
	@Property(
		key = GitLabPlugin.GITLAB_BREAK_PIPELINE,
		name = "Break GitLab Pipeline",
		description = "If the Pipeline should break on when a critical or worse issue has been found in the incremental analysis.",
		defaultValue = "true",
		type = PropertyType.BOOLEAN,
		project = true
	)
})
public class GitLabPlugin implements Plugin {

	public static final String GITLAB_INSTANCE_URL = "sonar.gitlab.uri";
	public static final String GITLAB_AUTH_TOKEN = "sonar.gitlab.auth.token";
	public static final String GITLAB_PROJECT_NAME = "sonar.gitlab.analyse.project";
	public static final String GITLAB_COMMIT_HASH = "sonar.gitlab.analyse.commit";
	public static final String GITLAB_BASE_BRANCH = "sonar.gitlab.analyse.base";
	public static final String GITLAB_BREAK_PIPELINE = "sonar.gitlab.pipeline.break";

	@Override
	public void define(Context context) {
		context
			.addExtension(GitLabPluginConfiguration.class)
			.addExtension(CommitAnalysisBuilder.class)
			.addExtension(DiffFetcher.class)
			.addExtension(PipelineBreaker.class)
			.addExtension(CommitIssueJob.class);
	}
}
