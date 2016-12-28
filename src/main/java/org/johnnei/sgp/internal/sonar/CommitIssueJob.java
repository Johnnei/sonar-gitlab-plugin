package org.johnnei.sgp.internal.sonar;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.gitlab.api.GitlabAPI;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.postjob.PostJob;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.PostJobDescriptor;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import org.johnnei.sgp.internal.gitlab.CommitCommenter;
import org.johnnei.sgp.internal.gitlab.ProcessException;
import org.johnnei.sgp.internal.model.MappedIssue;
import org.johnnei.sgp.internal.model.SonarReport;
import org.johnnei.sgp.internal.model.diff.UnifiedDiff;
import org.johnnei.sgp.internal.util.Stopwatch;
import org.johnnei.sgp.sonar.GitLabPlugin;

/**
 * Post Job which creates comments in GitLab for the reported issues.
 */
public class CommitIssueJob implements PostJob {

	private static final Logger LOGGER = Loggers.get(CommitIssueJob.class);

	private final GitLabPluginConfiguration configuration;

	private final CommitCommenter commitCommenter;

	private final GitlabAPI gitlabApi;

	private final PathResolver pathResolver;

	public CommitIssueJob(GitLabPluginConfiguration configuration) {
		this.configuration = configuration;
		this.pathResolver = new PathResolver();

		gitlabApi = configuration.createGitLabConnection();
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
		List<UnifiedDiff> diffs;
		try {
			diffs = gitlabApi.getCommitDiffs(
				configuration.getProject().getId(),
				configuration.getCommitHash()
			).stream()
				.flatMap(commitDiff -> {
					if (commitDiff.getDeletedFile()) {
						return Stream.empty();
					} else {
						return Stream.of(new UnifiedDiff(commitDiff));
					}
				})
				.collect(Collectors.toList());
		} catch (IOException e) {
			throw new ProcessException("Failed to get commit diff", e);
		}

		Iterable<PostJobIssue> iterable = context.issues();
		Collection<MappedIssue> issues = StreamSupport.stream(iterable.spliterator(), false)
			.flatMap(this::mapIssueToFile)
			.filter(issue -> isInDiff(issue, diffs))
			.collect(Collectors.toList());

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

	/**
	 * Attempts to map an issue to a file in the git repository.
	 * @param issue The issue to map.
	 * @return The Stream containing the mapped issue or an empty stream on failure.
	 */
	private Stream<MappedIssue> mapIssueToFile(PostJobIssue issue) {
		String path = getFilePath(issue.inputComponent());
		if (path != null) {
			return Stream.of(new MappedIssue(issue, path));
		} else {
			LOGGER.debug("Failed to find file for \"{}\" in \"{}\"", issue.message(), issue.inputComponent());
			return Stream.empty();
		}
	}

	@CheckForNull
	private String getFilePath(@CheckForNull InputComponent inputComponent) {
		if (inputComponent == null || !inputComponent.isFile()) {
			return null;
		}

		InputFile inputFile = (InputFile) inputComponent;
		return pathResolver.relativePath(configuration.getGitBaseDir(), inputFile.file());
	}

	/**
	 * Matches the issue to a diff.
	 * @param issue The issue to match.
	 * @param diffs The list of diffs in the commit.
	 * @return <code>true</code> when the issue is on a modified line. Otherwise <code>false</code>.
	 */
	private static boolean isInDiff(MappedIssue issue, List<UnifiedDiff> diffs) {
		return diffs.stream()
			.filter(diff -> diff.getFilepath().equals(issue.getPath()))
			.anyMatch(diff -> diff.getRanges().stream().anyMatch(range -> range.containsLine(issue.getIssue().line())));
	}
}
