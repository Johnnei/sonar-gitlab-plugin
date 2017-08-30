package org.johnnei.sgp.internal.gitlab;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import org.johnnei.sgp.internal.gitlab.api.v4.GitLabApi;
import org.johnnei.sgp.internal.gitlab.api.v4.model.GitLabCommit;
import org.johnnei.sgp.internal.gitlab.api.v4.model.GitLabCommitComparison;
import org.johnnei.sgp.internal.model.diff.UnifiedDiff;
import org.johnnei.sgp.internal.sonar.GitLabPluginConfiguration;

import static org.sonar.api.batch.InstantiationStrategy.PER_BATCH;

@BatchSide
@InstantiationStrategy(PER_BATCH)
public class DiffFetcher {

	private static final Logger LOGGER = Loggers.get(DiffFetcher.class);

	private final GitLabPluginConfiguration configuration;

	public DiffFetcher(GitLabPluginConfiguration configuration) {
		this.configuration = configuration;
	}

	@Nonnull
	public Collection<UnifiedDiff> getDiffs() {
		GitLabApi gitlabAPI = configuration.createGitLabConnection();

		GitLabCommitComparison compare;

		try {
			compare = gitlabAPI.compareCommits(configuration.getProject().getId(), configuration.getBaseBranch(), configuration.getCommitHash());
		} catch (IOException e) {
			throw new IllegalStateException("Failed to fetch compare diff.", e);
		}

		return compare.getCommits().stream()
			.flatMap(commit -> fetchCommitDiff(gitlabAPI, commit))
			.collect(Collectors.toList());
	}

	private Stream<UnifiedDiff> fetchCommitDiff(GitLabApi gitlabAPI, GitLabCommit commit) {
		try {
			LOGGER.debug("Fetching Diff for {}", commit.getShortId());
			return gitlabAPI.getCommitDiffs(configuration.getProject().getId(), commit.getShortId()).stream()
				.filter(diff -> !diff.getDeletedFile())
				.map(diff -> new UnifiedDiff(commit.getShortId(), diff));
		} catch (IOException e) {
			throw new IllegalStateException("Failed to fetch commit diff", e);
		}
	}

}
