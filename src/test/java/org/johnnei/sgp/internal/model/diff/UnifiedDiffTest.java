package org.johnnei.sgp.internal.model.diff;

import org.gitlab.api.models.GitlabCommitDiff;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnifiedDiffTest {

	@Test
	public void testFileAdded() {
		GitlabCommitDiff diff = mock(GitlabCommitDiff.class);
		when(diff.getAMode()).thenReturn("0");
		when(diff.getBMode()).thenReturn("100644");
		when(diff.getNewFile()).thenReturn(true);
		when(diff.getRenamedFile()).thenReturn(false);
		when(diff.getDeletedFile()).thenReturn(false);
		when(diff.getNewPath()).thenReturn("web/src/test/resources/stall-recipes/iron_monger.json");
		when(diff.getOldPath()).thenReturn("web/src/test/resources/stall-recipes/iron_monger.json");
		when(diff.getDiff()).thenReturn("--- /dev/null\n+++ b/web/src/test/resources/stall-recipes/iron_monger.json\n@@ -0,0 +1 @@\n+[]\n");

		UnifiedDiff cut = new UnifiedDiff(null, diff);

		assertThat("Path should be the new path", cut.getFilepath(), equalTo("web/src/test/resources/stall-recipes/iron_monger.json"));
		assertThat("Only 1 hunk should be found", cut.getRanges(), hasSize(1));
		assertThat("Incorrect hunks", cut.getRanges(), IsCollectionContaining.hasItem(new HunkRange(1, 1)));
	}

	@Test
	public void testFileModifiedSingleHunk() {
		GitlabCommitDiff diff = mock(GitlabCommitDiff.class);
		when(diff.getAMode()).thenReturn("100644");
		when(diff.getBMode()).thenReturn("100644");
		when(diff.getNewFile()).thenReturn(false);
		when(diff.getRenamedFile()).thenReturn(false);
		when(diff.getDeletedFile()).thenReturn(false);
		when(diff.getNewPath()).thenReturn("business/pom.xml");
		when(diff.getOldPath()).thenReturn("business/pom.xml");
		when(diff.getDiff()).thenReturn("--- a/business/pom.xml\n+++ b/business/pom.xml\n@@ -1,10 +1,9 @@\n-<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n-         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n+<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n   <modelVersion>4.0.0</modelVersion>\n   <parent>\n     <groupId>org.johnnei.ypp</groupId>\n     <artifactId>stallmanagement</artifactId>\n-    <version>1.4.0-SNAPSHOT</version>\n+    <version>1.4.0</version>\n   </parent>\n \n   <artifactId>business</artifactId>\n");

		UnifiedDiff cut = new UnifiedDiff(null, diff);

		assertThat("Path should be the new path", cut.getFilepath(), equalTo("business/pom.xml"));
		assertThat("Only 1 hunk should be found", cut.getRanges(), hasSize(1));
		assertThat("Incorrect hunks", cut.getRanges(), IsCollectionContaining.hasItem(new HunkRange(1, 9)));
	}

	@Test
	public void testMovedFiled() {
		GitlabCommitDiff diff = mock(GitlabCommitDiff.class);
		when(diff.getAMode()).thenReturn("100644");
		when(diff.getBMode()).thenReturn("100644");
		when(diff.getNewFile()).thenReturn(false);
		when(diff.getRenamedFile()).thenReturn(true);
		when(diff.getDeletedFile()).thenReturn(false);
		when(diff.getNewPath()).thenReturn("src/main/java/org/johnnei/sgp/it/NoIssue.java");
		when(diff.getOldPath()).thenReturn("src/main/java/org/johnnei/sgp/it/internal/NoIssue.java");
		when(diff.getDiff()).thenReturn("--- a/src/main/java/org/johnnei/sgp/it/internal/NoIssue.java\n+++ b/src/main/java/org/johnnei/sgp/it/NoIssue.java\n");

		UnifiedDiff cut = new UnifiedDiff(null, diff);

		assertThat("Path should be the new path", cut.getFilepath(), equalTo("src/main/java/org/johnnei/sgp/it/NoIssue.java"));
		assertThat("No hunks should be found", cut.getRanges(), empty());
	}

}
