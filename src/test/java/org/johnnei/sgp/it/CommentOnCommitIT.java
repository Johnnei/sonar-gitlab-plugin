package org.johnnei.sgp.it;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gitlab.api.models.CommitComment;
import org.hamcrest.collection.IsCollectionWithSize;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Test;

import org.johnnei.sgp.it.framework.IntegrationTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class CommentOnCommitIT extends IntegrationTest {

	@Test
	public void testCommentsAreCreated() throws IOException {
		createInitialCommit();
		String commitHash = gitCommitAll();
		sonarAnalysis(commitHash);

		List<CommitComment> commitComments = gitlabApi.getCommitComments(project.getId(), commitHash);
		List<String> comments = commitComments.stream()
			.filter(comment -> comment.getLine() != null)
			.map(CommitComment::getNote)
			.collect(Collectors.toList());

		List<String> messages = Files.readAllLines(getTestResource("sonarqube/issues.txt"));

		for (String message : messages) {
			assertThat(comments, IsCollectionContaining.hasItem(equalTo(message)));
			remoteMatchedComment(comments, message);
		}

		assertThat(
			String.format("%s Issues have been reported and thus comments should be there.", messages.size()),
			comments,
			IsEmptyCollection.empty()
		);
	}

	@Test
	public void testCommentIsCreatedForFileIssues() throws Exception {
		createInitialCommit();
		String commit = gitCommitAll();

		// Enable violations on the TAB characters which I do use.
		try (AutoCloseable ignored = enableSonarqubeRule("squid:S00105")) {
			sonarAnalysis(commit);
		}

		List<CommitComment> commitComments = gitlabApi.getCommitComments(project.getId(), commit);
		assertThat("File issues should have been reported.", commitComments.stream()
			.filter(comment -> comment.getLine() != null)
			.map(CommitComment::getNote)
			.anyMatch(comment -> comment.contains("tab")));
	}

	@Test
	public void testSummaryIsCreated() throws IOException {
		final String expectedSummary = Files
			.readAllLines(getTestResource("sonarqube/summary.txt"))
			.stream()
			.reduce((a, b) -> a + "\n" + b)
			.orElseThrow(() -> new IllegalStateException("Missing Summary information"));

		createInitialCommit();
		String commitHash = gitCommitAll();
		sonarAnalysis(commitHash);

		List<CommitComment> commitComments = gitlabApi.getCommitComments(project.getId(), commitHash);
		List<String> comments = commitComments.stream()
			.filter(comment -> comment.getLine() == null)
			.map(CommitComment::getNote)
			.collect(Collectors.toList());

		assertThat("Only 1 summary comment should be created", comments, IsCollectionWithSize.hasSize(1));
		assertThat("The summary doesn't match the expected summary.", comments.get(0), equalTo(expectedSummary));
	}

	@Test
	public void testCommentsAreCreatedWhenMultipleCommitsAreUsed() throws IOException {
		gitAdd("src/main/java/org/johnnei/sgp/it/internal/NoIssue.java pom.xml");
		gitCommit();
		gitCreateBranch("feature/my-feature");

		gitAdd("src/main/java/org/johnnei/sgp/it/api/sources/Main.java");
		String firstCommmit = gitCommit();
		String secondCommit = gitCommitAll();

		// checkout to the initial state to prevent analyse on uncommited files.
		gitCheckoutBranch("master");
		sonarAnalysis();

		gitCheckoutBranch("feature/my-feature");
		sonarAnalysis(secondCommit);

		List<CommitComment> firstCommitComments = gitlabApi.getCommitComments(project.getId(), firstCommmit);
		List<CommitComment> secondCommitComments = gitlabApi.getCommitComments(project.getId(), secondCommit);
		List<String> comments = Stream.concat(firstCommitComments.stream(), secondCommitComments.stream())
			.filter(comment -> comment.getLine() != null)
			.map(CommitComment::getNote)
			.collect(Collectors.toList());

		List<String> messages = Files.readAllLines(getTestResource("sonarqube/issues.txt"));

		for (String message : messages) {
			assertThat(comments, IsCollectionContaining.hasItem(equalTo(message)));
			remoteMatchedComment(comments, message);
		}

		assertThat(
			String.format("%s Issues have been reported and thus comments should be there.", messages.size()),
			comments,
			IsEmptyCollection.empty()
		);
	}
}
