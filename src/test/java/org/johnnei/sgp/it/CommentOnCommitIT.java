package org.johnnei.sgp.it;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.Test;

import org.johnnei.sgp.it.framework.IntegrationTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsEqual.equalTo;

public class CommentOnCommitIT extends IntegrationTest {

	@Test
	public void testCommentsAreCreated() throws IOException {
		prepareFeatureBranch();
		String commitHash = accessGit().commitAll();
		accessSonarQube().runAnalysis(commitHash);

		List<String> comments = accessGitlab().getCommitComments(commitHash);

		List<String> messages = Files.readAllLines(getTestResource("sonarqube/issues.txt"));

		for (String message : messages) {
			assertThat(comments, hasItem(equalTo(message)));
			removeMatchedComment(comments, message);
		}

		assertThat(String.format("%s Issues have been reported and thus comments should be there.", messages.size()), comments, empty());
	}

	@Test
	public void testCommentsAreCreatedWhenScanIsInvokedFromSubProject() throws IOException {
		File repo2 = new File(repoFolder.getParentFile(), "repo2");
		File subFolder = new File(repo2, "sub-folder");
		assertThat("Failed to create sub folder for test", repo2.mkdirs());
		assertThat("Failed to move source to sub directory", repoFolder.renameTo(subFolder));
		assertThat("Failed to restore git folder back to root.", new File(subFolder, ".git").renameTo(new File(repo2, ".git")));

		prepareAccessOnFolder(subFolder);

		prepareFeatureBranch();
		String commitHash = accessGit().commitAll();
		accessSonarQube().runAnalysis(commitHash);

		List<String> comments = accessGitlab().getCommitComments(commitHash);

		List<String> messages = Files.readAllLines(getTestResource("sonarqube/issues.txt"));

		for (String message : messages) {
			assertThat(comments, hasItem(equalTo(message)));
			removeMatchedComment(comments, message);
		}

		assertThat(String.format("%s Issues have been reported and thus comments should be there.", messages.size()), comments, empty());
	}

	@Test
	public void testCommentIsCreatedForFileIssues() throws Exception {
		prepareFeatureBranch();
		String commit = accessGit().commitAll();

		// Enable violations on the TAB characters which I do use.
		try (AutoCloseable ignored = accessSonarQube().enableRule("squid:S00105")) {
			accessSonarQube().runAnalysis(commit);
		}

		List<String> commitComments = accessGitlab().getCommitComments(commit);
		assertThat("File issues should have been reported.", commitComments.stream().anyMatch(comment -> comment.contains("tab")));
	}

	@Test
	public void testSummaryIsCreated() throws IOException {
		final String expectedSummary = Files
			.readAllLines(getTestResource("sonarqube/summary.txt"))
			.stream()
			.reduce((a, b) -> a + "\n" + b)
			.orElseThrow(() -> new IllegalStateException("Missing Summary information"));

		prepareFeatureBranch();
		String commitHash = accessGit().commitAll();
		accessSonarQube().runAnalysis(commitHash);

		List<String> summaries = accessGitlab().getCommitSummary(commitHash);
		assertThat("Only 1 summary comment should be created", summaries, IsCollectionWithSize.hasSize(1));
		assertThat("The summary doesn't match the expected summary.", summaries.get(0), equalTo(expectedSummary));
	}

	@Test
	public void testCommentsAreCreatedWhenMultipleCommitsAreUsed() throws IOException {
		accessGit().add("src/main/java/org/johnnei/sgp/it/internal/NoIssue.java pom.xml");
		accessGit().commit();
		accessGit().createBranch("feature/my-feature");

		accessGit().add("src/main/java/org/johnnei/sgp/it/api/sources/Main.java");
		String firstCommmit = accessGit().commit();
		String secondCommit = accessGit().commitAll();

		// checkout to the initial state to prevent analyse on uncommited files.
		accessGit().checkoutBranch("master");
		accessSonarQube().runAnalysis();

		accessGit().checkoutBranch("feature/my-feature");
		accessSonarQube().runAnalysis(secondCommit);

		List<String> comments = Stream.concat(accessGitlab().getCommitComments(firstCommmit).stream(), accessGitlab().getCommitComments(secondCommit).stream())
			.collect(Collectors.toList());

		List<String> messages = Files.readAllLines(getTestResource("sonarqube/issues.txt"));

		for (String message : messages) {
			assertThat(comments, hasItem(equalTo(message)));
			removeMatchedComment(comments, message);
		}

		assertThat(String.format("%s Issues have been reported and thus comments should be there.", messages.size()), comments, empty());
	}
}
