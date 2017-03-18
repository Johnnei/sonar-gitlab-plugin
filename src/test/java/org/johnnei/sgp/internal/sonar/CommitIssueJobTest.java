package org.johnnei.sgp.internal.sonar;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabCommitDiff;
import org.gitlab.api.models.GitlabProject;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.PostJobDescriptor;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.utils.log.LogTester;

import org.johnnei.sgp.internal.gitlab.CommitCommenter;
import org.johnnei.sgp.internal.gitlab.DiffFetcher;
import org.johnnei.sgp.internal.gitlab.PipelineBreaker;
import org.johnnei.sgp.internal.model.MappedIssue;
import org.johnnei.sgp.internal.model.SonarReport;
import org.johnnei.sgp.internal.model.diff.HunkRange;
import org.johnnei.sgp.internal.model.diff.UnifiedDiff;
import org.johnnei.sgp.test.MockIssue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CommitIssueJobTest {

	@Rule
	public LogTester logTester = new LogTester();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private CommitIssueJob cut;

	@Mock
	private CommitCommenter commitCommenterMock;

	@Mock
	private GitLabPluginConfiguration configurationMock;

	@Mock
	private GitlabAPI gitlabApiMock;

	@Mock
	private DiffFetcher diffFetcherMock;

	@Mock
	private PipelineBreaker pipelineBreaker;

	private UnifiedDiff diff;

	@Before
	public void setUp() {
		when(configurationMock.createGitLabConnection()).thenReturn(gitlabApiMock);
		cut = new CommitIssueJob(diffFetcherMock, configurationMock, pipelineBreaker) {
			@Override
			CommitCommenter createCommenter() {
				// Initialize API and create commenter
				super.createCommenter();
				// But return a mock
				return commitCommenterMock;
			}
		};

		diff = mock(UnifiedDiff.class);
	}

	@Test
	public void testDescribe() throws Exception {
		PostJobDescriptor postJobDescriptorMock = mock(PostJobDescriptor.class);

		when(postJobDescriptorMock.name(anyString())).thenReturn(postJobDescriptorMock);
		when(postJobDescriptorMock.requireProperty(anyString())).thenReturn(postJobDescriptorMock);

		cut.describe(postJobDescriptorMock);

		verify(postJobDescriptorMock).name("GitLab Commit Issue Publisher");
		verify(postJobDescriptorMock, atLeastOnce()).requireProperty(anyString());
	}

	@Test
	public void testExecute() throws Exception {
		String hash = "a2b4";
		int projectId = 42;
		File file = new File(new File("src"), "Main.java");
		PostJobContext postJobContextMock = mock(PostJobContext.class);

		PostJobIssue issueMock = MockIssue.mockInlineIssue(file, 3, Severity.CRITICAL, "Steeeevvveee!");
		GitlabProject projectMock = mock(GitlabProject.class);
		when(projectMock.getId()).thenReturn(projectId);

		String diff = "--- a/src/Main.java\n" +
			"+++ b/src/Main.java\n" +
			"@@ -1,5 +1,5 @@\n" +
			"-package org.johnnei.sgp;\n" +
			"+package org.johnnei.sgp.it;\n" +
			" \n" +
			" import java.io.IOException;\n" +
			" import java.nio.file.Files;";
		GitlabCommitDiff commitDiffOne = mock(GitlabCommitDiff.class);
		when(commitDiffOne.getDiff()).thenReturn(diff);
		when(commitDiffOne.getNewPath()).thenReturn("src/Main.java");

		when(diffFetcherMock.getDiffs()).thenAnswer(invocation -> Collections.singletonList(new UnifiedDiff(hash, commitDiffOne)));

		when(postJobContextMock.issues()).thenReturn(Collections.singletonList(issueMock));
		when(configurationMock.getCommitHash()).thenReturn(hash);
		when(configurationMock.getProject()).thenReturn(projectMock);

		cut.execute(postJobContextMock);

		ArgumentCaptor<SonarReport> reportCaptor = ArgumentCaptor.forClass(SonarReport.class);

		verify(commitCommenterMock).process(reportCaptor.capture());
		verify(pipelineBreaker).process(reportCaptor.getValue());

		SonarReport report = reportCaptor.getValue();
		assertThat("Project must not have changed", report.getProject(), equalTo(projectMock));
		assertThat("Commit sha must not have changed", report.getBuildCommitSha(), equalTo(hash));
		assertThat("The iterable of 1 issue should have result in a stream of 1 issue", report.getIssues().count(), equalTo(1L));
	}

	@Test
	public void testExecuteDuplicatePaths() throws Exception {
		thrown.expect(IllegalStateException.class);
		thrown.expectMessage("matched");

		String hash = "a2b4";
		int projectId = 42;
		File file = new File(new File("src"), "Main.java");
		PostJobContext postJobContextMock = mock(PostJobContext.class);

		PostJobIssue issueMock = MockIssue.mockInlineIssue(file, 3, Severity.CRITICAL, "Steeeevvveee!");
		GitlabProject projectMock = mock(GitlabProject.class);
		when(projectMock.getId()).thenReturn(projectId);

		UnifiedDiff unifiedDiffMock = mock(UnifiedDiff.class);
		when(unifiedDiffMock.getFilepath()).thenReturn("src/Main.java");

		HunkRange range = mock(HunkRange.class);
		when(unifiedDiffMock.getRanges()).thenReturn(Collections.singletonList(range));
		when(range.containsLine(eq(3))).thenReturn(true);

		// This is not a valid state, but I don't know a case in which 2 paths collide as the diff paths contain the full path of the repository.
		when(diffFetcherMock.getDiffs()).thenAnswer(invocation -> Arrays.asList(unifiedDiffMock, unifiedDiffMock));

		when(postJobContextMock.issues()).thenReturn(Collections.singletonList(issueMock));
		when(configurationMock.getCommitHash()).thenReturn(hash);
		when(configurationMock.getProject()).thenReturn(projectMock);

		cut.execute(postJobContextMock);

		ArgumentCaptor<SonarReport> reportCaptor = ArgumentCaptor.forClass(SonarReport.class);

		verify(commitCommenterMock).process(reportCaptor.capture());
		verify(pipelineBreaker).process(reportCaptor.getValue());

		SonarReport report = reportCaptor.getValue();
		assertThat("Project must not have changed", report.getProject(), equalTo(projectMock));
		assertThat("Commit sha must not have changed", report.getBuildCommitSha(), equalTo(hash));
		assertThat("The iterable of 1 issue should have result in a stream of 1 issue", report.getIssues().count(), equalTo(1L));
	}

	@Test
	public void testExecuteWithWindowsPaths() throws Exception {
		String hash = "a2b4";
		int projectId = 42;
		PostJobContext postJobContextMock = mock(PostJobContext.class);

		GitlabProject projectMock = mock(GitlabProject.class);
		when(projectMock.getId()).thenReturn(projectId);

		// Don't use utility method as that one will use platform dependant file separators.
		InputFile inputComponentMock = mock(InputFile.class);
		when(inputComponentMock.isFile()).thenReturn(true);
		when(inputComponentMock.absolutePath()).thenReturn("D:\\project\\src\\Main.java");

		PostJobIssue issueMock = mock(PostJobIssue.class);
		when(issueMock.inputComponent()).thenReturn(inputComponentMock);
		when(issueMock.message()).thenReturn("Dammit Windows!");
		when(issueMock.line()).thenReturn(3);
		when(issueMock.severity()).thenReturn(Severity.CRITICAL);

		UnifiedDiff unifiedDiffMock = mock(UnifiedDiff.class);
		when(unifiedDiffMock.getFilepath()).thenReturn("src/Main.java");

		HunkRange range = mock(HunkRange.class);
		when(unifiedDiffMock.getRanges()).thenReturn(Collections.singletonList(range));
		when(range.containsLine(eq(3))).thenReturn(true);

		when(diffFetcherMock.getDiffs()).thenAnswer(invocation -> Collections.singletonList(unifiedDiffMock));

		when(postJobContextMock.issues()).thenReturn(Collections.singletonList(issueMock));
		when(configurationMock.getCommitHash()).thenReturn(hash);
		when(configurationMock.getProject()).thenReturn(projectMock);

		cut.execute(postJobContextMock);

		ArgumentCaptor<SonarReport> reportCaptor = ArgumentCaptor.forClass(SonarReport.class);

		verify(commitCommenterMock).process(reportCaptor.capture());

		SonarReport report = reportCaptor.getValue();
		assertThat("The iterable of 1 issue should have result in a stream of 1 issue", report.getIssues().count(), equalTo(1L));
	}

	@Test
	public void testExecuteFileIssueOnMovedFile() throws Exception {
		String hash = "a2b4";
		int projectId = 42;
		File file = new File("Main.java");
		PostJobContext postJobContextMock = mock(PostJobContext.class);

		PostJobIssue issueMock = MockIssue.mockFileIssue(file);
		GitlabProject projectMock = mock(GitlabProject.class);
		when(projectMock.getId()).thenReturn(projectId);

		String diff = "--- a/src/main/java/org/johnnei/sgp/it/internal/NoIssue.java\n+++ b/src/main/java/org/johnnei/sgp/it/NoIssue.java\n";
		GitlabCommitDiff commitDiffOne = mock(GitlabCommitDiff.class);
		when(commitDiffOne.getDiff()).thenReturn(diff);
		when(commitDiffOne.getOldPath()).thenReturn("src/main/java/org/johnnei/sgp/it/internal/NoIssue.java");
		when(commitDiffOne.getNewPath()).thenReturn("src/main/java/org/johnnei/sgp/it/NoIssue.java");
		when(commitDiffOne.getRenamedFile()).thenReturn(true);

		when(diffFetcherMock.getDiffs()).thenAnswer(invocation -> Collections.singletonList(new UnifiedDiff(hash, commitDiffOne)));

		when(postJobContextMock.issues()).thenReturn(Collections.singletonList(issueMock));
		when(configurationMock.getCommitHash()).thenReturn(hash);
		when(configurationMock.getProject()).thenReturn(projectMock);

		cut.execute(postJobContextMock);

		ArgumentCaptor<SonarReport> reportCaptor = ArgumentCaptor.forClass(SonarReport.class);

		verify(commitCommenterMock).process(reportCaptor.capture());

		SonarReport report = reportCaptor.getValue();
		assertThat("Project must not have changed", report.getProject(), equalTo(projectMock));
		assertThat("Commit sha must not have changed", report.getBuildCommitSha(), equalTo(hash));
		assertThat("The file level issue on a file that is only moved can't be inline commented.", report.getIssues().count(), equalTo(0L));
	}

	@Test
	public void testExecuteUnmappedFile() throws Exception {
		SonarReport reportMock = mock(SonarReport.class);
		PostJobIssue issueMock = mock(PostJobIssue.class);
		InputComponent inputComponentMock = mock(InputComponent.class);

		when(inputComponentMock.isFile()).thenReturn(false);
		when(issueMock.inputComponent()).thenReturn(inputComponentMock);
		when(issueMock.message()).thenReturn("Remove this violation!");

		when(reportMock.getIssues()).thenReturn(Stream.of(new MappedIssue(issueMock, diff, "")));
		String hash = "a2b4";
		PostJobContext postJobContextMock = mock(PostJobContext.class);

		GitlabProject projectMock = mock(GitlabProject.class);

		when(postJobContextMock.issues()).thenReturn(Collections.singletonList(issueMock));
		when(configurationMock.getCommitHash()).thenReturn(hash);
		when(configurationMock.getProject()).thenReturn(projectMock);

		cut.execute(postJobContextMock);

		ArgumentCaptor<SonarReport> reportCaptor = ArgumentCaptor.forClass(SonarReport.class);

		verify(commitCommenterMock).process(reportCaptor.capture());

		SonarReport report = reportCaptor.getValue();
		assertThat(
			"Issue was not mapped to a file. Should not be included.",
			report.getIssues().collect(Collectors.toList()),
			IsEmptyCollection.empty()
		);
	}

}
