package org.johnnei.sgp.internal.sonar;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.InstantiationStrategy;
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
import org.johnnei.sgp.internal.gitlab.DiffFetcher;
import org.johnnei.sgp.internal.model.MappedIssue;
import org.johnnei.sgp.internal.model.SonarReport;
import org.johnnei.sgp.internal.model.diff.UnifiedDiff;
import org.johnnei.sgp.internal.util.Stopwatch;
import org.johnnei.sgp.sonar.GitLabPlugin;

import static org.sonar.api.batch.InstantiationStrategy.PER_BATCH;

/**
 * Post Job which creates comments in GitLab for the reported issues.
 */
@BatchSide
@InstantiationStrategy(PER_BATCH)
public class CommitIssueJob implements PostJob {

	private static final Logger LOGGER = Loggers.get(CommitIssueJob.class);

	private final GitLabPluginConfiguration configuration;

	private final DiffFetcher diffFetcher;

	private final PathResolver pathResolver;

	public CommitIssueJob(DiffFetcher diffFetcher, GitLabPluginConfiguration configuration) {
		this.configuration = configuration;
		this.diffFetcher = diffFetcher;
		this.pathResolver = new PathResolver();
	}

	@Override
	public void describe(@Nonnull PostJobDescriptor descriptor) {
		descriptor
			.name("GitLab Commit Issue Publisher")
			.requireProperty(GitLabPlugin.GITLAB_COMMIT_HASH);
	}

	CommitCommenter createCommenter() {
		return new CommitCommenter(configuration.createGitLabConnection());
	}

	@Override
	public void execute(@Nonnull PostJobContext context) {
		CommitCommenter commitCommenter = createCommenter();

		final Collection<UnifiedDiff> diffs = diffFetcher.getDiffs();
		Iterable<PostJobIssue> iterable = context.issues();
		Collection<MappedIssue> issues = StreamSupport.stream(iterable.spliterator(), false)
			.flatMap(issue -> mapIssueToFile(issue, diffs))
			.collect(Collectors.toList());

		Stopwatch stopwatch = new Stopwatch();
		stopwatch.start("Creating comments in GitLab.");

		SonarReport report = new SonarReport.Builder()
			.setIssues(issues)
			.setBuildCommitSha(configuration.getCommitHash())
			.setProject(configuration.getProject())
			.build();

		commitCommenter.process(report);

		stopwatch.stop();
	}

	/**
	 * Attempts to map an issue to a file in the git repository.
	 * @param issue The issue to map.
	 * @return The Stream containing the mapped issue or an empty stream on failure.
	 */
	private Stream<MappedIssue> mapIssueToFile(PostJobIssue issue, Collection<UnifiedDiff> diffs) {
		String path = getFilePath(issue.inputComponent());
		if (path == null) {
			LOGGER.warn("Failed to find file for \"{}\" in \"{}\"", issue.message(), issue.inputComponent());
			return Stream.empty();
		}

		return findDiff(path, issue, diffs).map(diff -> Stream.of(new MappedIssue(issue, diff, path))).orElseGet(() -> {
			LOGGER.warn("Failed to find diff for issue \"{}\" in \"{}\"", issue.message(), issue.inputComponent());
			return Stream.empty();
		});
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
	private static Optional<UnifiedDiff> findDiff(String path, PostJobIssue issue, Collection<UnifiedDiff> diffs) {
		Stream<UnifiedDiff> stream = diffs.stream()
			.filter(diff -> diff.getFilepath().equals(path));

		if (issue.line() != null) {
			stream = stream.filter(diff -> diff.getRanges().stream().anyMatch(range -> range.containsLine(issue.line())));
		}

		return stream.findAny();
	}

}
