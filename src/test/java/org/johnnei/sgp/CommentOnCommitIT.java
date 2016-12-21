package org.johnnei.sgp;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.gitlab.api.models.CommitComment;
import org.hamcrest.collection.IsCollectionWithSize;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Test;

import org.johnnei.sgp.it.IntegrationTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * Created by Johnnei on 2016-12-04.
 */
public class CommentOnCommitIT extends IntegrationTest {

	@Test
	public void testCommentsAreCreated() throws IOException {
		String commitHash = gitCommitAll();
		sonarAnalysis(commitHash);

		List<CommitComment> commitComments = gitlabApi.getCommitComments(project.getId(), commitHash);
		assertThat("10 Issues have been reported and thus comments should be there.", commitComments, IsCollectionWithSize.hasSize(10));
		List<String> comments = commitComments.stream()
			.map(CommitComment::getNote)
			.collect(Collectors.toList());

		String[] messages = {
			"Remove this unused \"inputStream\" local variable.",
			"Remove this useless assignment to local variable \"inputStream\".",
			"Close this \"FileInputStream\".",
			"Either log or rethrow this exception.",
			"Do not forget to remove this deprecated code someday.",
			"Replace this \"switch\" statement by \"if\" statements to increase readability.",
			"Add a default case to this switch.",
			"Reduce this switch case number of lines from 10 to at most 5, for example by extracting code into methods.",
			"Refactor this code to not nest more than 3 if/for/while/switch/try statements.",
			"Change this condition so that it does not always evaluate to \"true\""
		};

		for (String message : messages) {
			assertThat(comments, IsCollectionContaining.hasItem(equalTo(message)));
		}
	}
}
