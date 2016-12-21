package org.johnnei.sgp.internal.sonar;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.gitlab.api.GitlabAPI;
import org.sonar.api.batch.postjob.PostJob;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.PostJobDescriptor;
import org.sonar.api.batch.postjob.issue.PostJobIssue;

import org.johnnei.sgp.internal.gitlab.CommitCommenter;
import org.johnnei.sgp.internal.model.SonarReport;
import org.johnnei.sgp.internal.util.Stopwatch;
import org.johnnei.sgp.sonar.GitLabPlugin;

/**
 * Created by Johnnei on 2016-11-12.
 */
public class CommitIssueJob implements PostJob {

	private GitLabPluginConfiguration configuration;

	private CommitCommenter commitCommenter;

	public CommitIssueJob(GitLabPluginConfiguration configuration) {
		this.configuration = configuration;

		GitlabAPI gitlabApi = configuration.createGitLabConnection();
		commitCommenter = new CommitCommenter(gitlabApi);
	}

	@Override
	public void describe(@Nonnull PostJobDescriptor descriptor) {
		descriptor
			.name("GitLab Commit Issue Publisher")
			.requireProperty(GitLabPlugin.GITLAB_AUTH_TOKEN)
			.requireProperty(GitLabPlugin.GITLAB_COMMIT_HASH)
			.requireProperty(GitLabPlugin.GITLAB_INSTANCE_URL);
	}

	@Override
	public void execute(@Nonnull PostJobContext context) {
		Iterable<PostJobIssue> iterable = () -> context.issues().iterator();
		Collection<PostJobIssue> issues = StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());

		Stopwatch stopwatch = new Stopwatch();
		stopwatch.start("Creating comments in GitLab.");

		SonarReport report = new SonarReport.Builder()
			.setIssues(issues)
			.setCommitSha(configuration.getCommitHash())
			.setProject(configuration.getProject())
			.setProjectBaseDir(configuration.getGitBaseDir())
			.build();

		commitCommenter.process(report);

		stopwatch.stop();
	}
}
