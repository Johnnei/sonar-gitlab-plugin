package org.johnnei.sgp.internal.gitlab;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.CommitComment;
import org.gitlab.api.models.GitlabProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;

import org.johnnei.sgp.internal.model.MappedIssue;
import org.johnnei.sgp.internal.model.SonarReport;
import org.johnnei.sgp.internal.model.diff.HunkRange;
import org.johnnei.sgp.internal.model.diff.UnifiedDiff;
import org.johnnei.sgp.test.MockIssue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class CommitCommenterTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private final String hash = "a2b4";

	private final String path = "/my/file.java";

	private final int projectId = 42;

	private final int line = 44;

	private final String message = "Remove this violation!";

	private UnifiedDiff diff;

	@Before
	public void setUp() throws Exception {
		diff = mock(UnifiedDiff.class);
		when(diff.getCommitSha()).thenReturn("a2b4");

	}

	@Test
	public void testProcessFailOnGitLabError() throws Exception {
		thrown.expect(IllegalStateException.class);
		thrown.expectCause(isA(IOException.class));

		GitlabAPI apiMock = mock(GitlabAPI.class);
		GitlabProject projectMock = mock(GitlabProject.class);
		SonarReport reportMock = mock(SonarReport.class);

		when(projectMock.getId()).thenReturn(projectId);
		when(reportMock.getProject()).thenReturn(projectMock);
		when(reportMock.getBuildCommitSha()).thenReturn(hash);
		when(reportMock.getCommitShas()).thenReturn(Stream.of(hash));

		when(apiMock.getCommitComments(projectId, hash)).thenThrow(new IOException("Test exception path"));

		CommitCommenter cut = new CommitCommenter(apiMock);

		cut.process(reportMock);
	}

	@Test
	public void testProcess() throws Exception {
		GitlabAPI apiMock = mock(GitlabAPI.class);
		GitlabProject projectMock = mock(GitlabProject.class);
		SonarReport reportMock = mock(SonarReport.class);
		PostJobIssue issueMock = mock(PostJobIssue.class);
		InputComponent inputComponentMock = mock(InputComponent.class);

		when(projectMock.getId()).thenReturn(projectId);

		when(inputComponentMock.isFile()).thenReturn(true);
		when(issueMock.inputComponent()).thenReturn(inputComponentMock);
		when(issueMock.message()).thenReturn("Remove this violation!");
		when(issueMock.line()).thenReturn(line);
		when(issueMock.severity()).thenReturn(Severity.CRITICAL);

		when(reportMock.getIssues()).thenReturn(Stream.of(new MappedIssue(issueMock, diff, path)));
		when(reportMock.getBuildCommitSha()).thenReturn(hash);
		when(reportMock.getCommitShas()).thenReturn(Stream.of(hash));
		when(reportMock.getProject()).thenReturn(projectMock);

		when(reportMock.countIssuesWithSeverity(Severity.CRITICAL)).thenReturn(1L);

		CommitCommenter cut = new CommitCommenter(apiMock);

		cut.process(reportMock);

		ArgumentCaptor<String> commentCaptor = ArgumentCaptor.forClass(String.class);
		verify(apiMock).createCommitComment(
			eq(projectId),
			eq(hash),
			commentCaptor.capture(),
			eq(path),
			eq(Integer.toString(line)),
			eq("new")
		);

		ArgumentCaptor<String> summaryCaptor = ArgumentCaptor.forClass(String.class);
		verify(apiMock).createCommitComment(
			eq(projectId),
			eq(hash),
			summaryCaptor.capture(),
			isNull(String.class),
			isNull(String.class),
			isNull(String.class)
		);

		assertThat(commentCaptor.getValue(), containsString(issueMock.message()));
		assertThat(summaryCaptor.getValue(), containsString("SonarQube"));
		assertThat(summaryCaptor.getValue(), containsString("1 critical"));
	}

	@Test
	public void testProcessIssueOnFile() throws Exception {
		GitlabAPI apiMock = mock(GitlabAPI.class);
		GitlabProject projectMock = mock(GitlabProject.class);
		SonarReport reportMock = mock(SonarReport.class);
		PostJobIssue issueMock = mock(PostJobIssue.class);
		InputComponent inputComponentMock = mock(InputComponent.class);

		when(projectMock.getId()).thenReturn(projectId);

		when(inputComponentMock.isFile()).thenReturn(true);
		when(issueMock.inputComponent()).thenReturn(inputComponentMock);
		when(issueMock.message()).thenReturn("Remove this violation!");
		when(issueMock.line()).thenReturn(null);
		when(issueMock.severity()).thenReturn(Severity.CRITICAL);

		when(reportMock.getIssues()).thenReturn(Stream.of(new MappedIssue(issueMock, diff, path)));
		when(reportMock.getBuildCommitSha()).thenReturn(hash);
		when(reportMock.getCommitShas()).thenReturn(Stream.of(hash));
		when(reportMock.getProject()).thenReturn(projectMock);

		when(reportMock.countIssuesWithSeverity(Severity.CRITICAL)).thenReturn(1L);

		HunkRange hunkRange = mock(HunkRange.class);
		when(hunkRange.getStart()).thenReturn(5);

		when(diff.getRanges()).thenReturn(Collections.singletonList(hunkRange));

		CommitCommenter cut = new CommitCommenter(apiMock);

		cut.process(reportMock);

		ArgumentCaptor<String> commentCaptor = ArgumentCaptor.forClass(String.class);
		verify(apiMock).createCommitComment(
			eq(projectId),
			eq(hash),
			commentCaptor.capture(),
			eq(path),
			eq(Integer.toString(5)),
			eq("new")
		);

		ArgumentCaptor<String> summaryCaptor = ArgumentCaptor.forClass(String.class);
		verify(apiMock).createCommitComment(
			eq(projectId),
			eq(hash),
			summaryCaptor.capture(),
			isNull(String.class),
			isNull(String.class),
			isNull(String.class)
		);

		assertThat(commentCaptor.getValue(), containsString(issueMock.message()));
		assertThat(summaryCaptor.getValue(), containsString("SonarQube"));
		assertThat(summaryCaptor.getValue(), containsString("1 critical"));
	}

	@Test
	public void testProcessMissingDiffForFileLevelIssue() throws Exception {
		thrown.expect(IllegalStateException.class);
		thrown.expectMessage("diff");

		GitlabAPI apiMock = mock(GitlabAPI.class);
		GitlabProject projectMock = mock(GitlabProject.class);
		SonarReport reportMock = mock(SonarReport.class);

		when(apiMock.getCommitComments(projectId, hash)).thenReturn(Collections.emptyList());

		when(projectMock.getId()).thenReturn(projectId);
		when(diff.getRanges()).thenReturn(Collections.emptyList());

		PostJobIssue fileIssueMock = MockIssue.mockFileIssue(new File(path));

		when(reportMock.getIssues()).thenReturn(Stream.of(new MappedIssue(fileIssueMock, diff, path)));
		when(reportMock.getBuildCommitSha()).thenReturn(hash);
		when(reportMock.getCommitShas()).thenReturn(Stream.of(hash));
		when(reportMock.getProject()).thenReturn(projectMock);

		CommitCommenter cut = new CommitCommenter(apiMock);

		cut.process(reportMock);
	}

	@Test
	public void testProcessExcludeExistingWithFileComments() throws Exception {
		String summary = "SonarQube analysis reported 0 issues.\n\nWatch the comments in this conversation to review them.";

		GitlabAPI apiMock = mock(GitlabAPI.class);
		GitlabProject projectMock = mock(GitlabProject.class);
		SonarReport reportMock = mock(SonarReport.class);

		CommitComment commentMock = mock(CommitComment.class);
		when(commentMock.getLine()).thenReturn(Integer.toString(line));
		when(commentMock.getPath()).thenReturn(path);
		when(commentMock.getNote()).thenReturn(":bangbang: File level violation.");

		CommitComment summaryMock = mock(CommitComment.class);
		when(summaryMock.getPath()).thenReturn(null);
		when(summaryMock.getLine()).thenReturn(null);
		when(summaryMock.getNote()).thenReturn(summary);

		when(apiMock.getCommitComments(projectId, hash)).thenReturn(Arrays.asList(commentMock, summaryMock));

		when(projectMock.getId()).thenReturn(projectId);
		HunkRange rangeMock = mock(HunkRange.class);
		when(rangeMock.getStart()).thenReturn(line);
		when(diff.getRanges()).thenReturn(Collections.singletonList(rangeMock));

		PostJobIssue fileIssueMock = MockIssue.mockFileIssue(new File(path));

		when(reportMock.getIssues()).thenReturn(Stream.of(new MappedIssue(fileIssueMock, diff, path)));
		when(reportMock.getBuildCommitSha()).thenReturn(hash);
		when(reportMock.getCommitShas()).thenReturn(Stream.of(hash));
		when(reportMock.getProject()).thenReturn(projectMock);

		CommitCommenter cut = new CommitCommenter(apiMock);

		cut.process(reportMock);

		verify(apiMock).getCommitComments(projectId, hash);
		verifyNoMoreInteractions(apiMock);
	}

	@Test
	public void testProcessExistingFileLevelIssue() throws Exception {
		String summary = "SonarQube analysis reported 0 issues.\n\nWatch the comments in this conversation to review them.";

		GitlabAPI apiMock = mock(GitlabAPI.class);
		GitlabProject projectMock = mock(GitlabProject.class);
		SonarReport reportMock = mock(SonarReport.class);

		CommitComment commentMock = mock(CommitComment.class);
		when(commentMock.getLine()).thenReturn(Integer.toString(line));
		when(commentMock.getPath()).thenReturn(path);
		when(commentMock.getNote()).thenReturn(":bangbang: " + message);

		CommitComment summaryMock = mock(CommitComment.class);
		when(summaryMock.getPath()).thenReturn(null);
		when(summaryMock.getLine()).thenReturn(null);
		when(summaryMock.getNote()).thenReturn(summary);

		when(apiMock.getCommitComments(projectId, hash)).thenReturn(Arrays.asList(commentMock, summaryMock));

		when(projectMock.getId()).thenReturn(projectId);

		PostJobIssue issueMock = MockIssue.mockInlineIssue(path, line, Severity.CRITICAL, message);

		when(reportMock.getIssues()).thenReturn(Stream.of(new MappedIssue(issueMock, diff, path)));
		when(reportMock.getBuildCommitSha()).thenReturn(hash);
		when(reportMock.getCommitShas()).thenReturn(Stream.of(hash));
		when(reportMock.getProject()).thenReturn(projectMock);

		CommitCommenter cut = new CommitCommenter(apiMock);

		cut.process(reportMock);

		verify(apiMock).getCommitComments(projectId, hash);
		verifyNoMoreInteractions(apiMock);
	}

	@Test
	public void testProcessNewIssueOnSecondAnalysis() throws Exception {
		// On the second analysis a comparison against the Summary should not cause issues.
		String summary = "SonarQube analysis reported 0 issues.\n\nWatch the comments in this conversation to review them.";

		GitlabAPI apiMock = mock(GitlabAPI.class);
		GitlabProject projectMock = mock(GitlabProject.class);
		SonarReport reportMock = mock(SonarReport.class);

		CommitComment commentMock = mock(CommitComment.class);
		when(commentMock.getLine()).thenReturn(Integer.toString(line));
		when(commentMock.getPath()).thenReturn(path);
		when(commentMock.getNote()).thenReturn(message);

		CommitComment summaryMock = mock(CommitComment.class);
		when(summaryMock.getPath()).thenReturn(null);
		when(summaryMock.getLine()).thenReturn(null);
		when(summaryMock.getNote()).thenReturn(summary);

		when(apiMock.getCommitComments(projectId, hash)).thenReturn(Arrays.asList(commentMock, summaryMock));

		when(projectMock.getId()).thenReturn(projectId);

		PostJobIssue issueMock = MockIssue.mockInlineIssue(path, line, Severity.CRITICAL, message);
		PostJobIssue newIssueMock = MockIssue.mockInlineIssue("/not/my/file.java", 88, Severity.MAJOR, message);

		when(reportMock.getIssues()).thenReturn(Stream.of(new MappedIssue(issueMock, diff, path), new MappedIssue(newIssueMock, diff, "/not/my/file.java")));
		when(reportMock.getBuildCommitSha()).thenReturn(hash);
		when(reportMock.getCommitShas()).thenReturn(Stream.of(hash));
		when(reportMock.getProject()).thenReturn(projectMock);

		CommitCommenter cut = new CommitCommenter(apiMock);

		cut.process(reportMock);

		verify(apiMock).getCommitComments(projectId, hash);
		verify(apiMock).createCommitComment(
			eq(projectId),
			eq(hash),
			eq(":exclamation: Remove this violation!"),
			eq("/not/my/file.java"),
			eq(Integer.toString(88)),
			eq("new")
		);
		verifyNoMoreInteractions(apiMock);
	}

	@Test
	public void testProcessFailedToComment() throws Exception {
		thrown.expect(ProcessException.class);
		thrown.expectMessage("comments failed");

		GitlabAPI apiMock = mock(GitlabAPI.class);
		GitlabProject projectMock = mock(GitlabProject.class);
		SonarReport reportMock = mock(SonarReport.class);

		String hash = "a2b4";
		String path = "/my/file.java";
		int projectId = 42;
		int line = 44;

		when(projectMock.getId()).thenReturn(projectId);

		PostJobIssue issueMock = MockIssue.mockInlineIssue(path, line, Severity.CRITICAL, "Remote this violation!");

		when(reportMock.getIssues()).thenReturn(Stream.of(new MappedIssue(issueMock, diff, path)));
		when(reportMock.getBuildCommitSha()).thenReturn(hash);
		when(reportMock.getCommitShas()).thenReturn(Stream.of(hash));
		when(reportMock.getProject()).thenReturn(projectMock);

		when(apiMock.createCommitComment(
			anyInt(),
			anyString(),
			anyString(),
			anyString(),
			anyString(),
			anyString()
		)).thenThrow(new IOException("Test exception path"));

		CommitCommenter cut = new CommitCommenter(apiMock);

		cut.process(reportMock);
	}

	@Test
	public void testProcessFailedToCommentSummary() throws Exception {
		thrown.expect(ProcessException.class);
		thrown.expectMessage("summary comment");

		GitlabAPI apiMock = mock(GitlabAPI.class);
		GitlabProject projectMock = mock(GitlabProject.class);
		SonarReport reportMock = mock(SonarReport.class);

		String hash = "a2b4";
		String path = "/my/file.java";
		int projectId = 42;
		int line = 44;

		when(projectMock.getId()).thenReturn(projectId);

		PostJobIssue issueMock = MockIssue.mockInlineIssue(path, line, Severity.CRITICAL, "Remove this violation!");

		when(reportMock.getIssues()).thenReturn(Stream.of(new MappedIssue(issueMock, diff, path)));
		when(reportMock.getBuildCommitSha()).thenReturn(hash);
		when(reportMock.getProject()).thenReturn(projectMock);
		when(reportMock.getCommitShas()).thenReturn(Stream.of(hash));

		when(apiMock.createCommitComment(
			anyInt(),
			anyString(),
			anyString(),
			isNull(String.class),
			isNull(String.class),
			isNull(String.class)
		)).thenThrow(new IOException("Test exception path"));

		CommitCommenter cut = new CommitCommenter(apiMock);

		cut.process(reportMock);
	}

}
