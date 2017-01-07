package org.johnnei.sgp.internal.gitlab;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.CommitComment;
import org.gitlab.api.models.GitlabProject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;

import org.johnnei.sgp.internal.model.MappedIssue;
import org.johnnei.sgp.internal.model.SonarReport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

	@Test
	public void testProcessFailOnGitLabError() throws Exception {
		thrown.expect(IllegalStateException.class);
		thrown.expectCause(isA(IOException.class));

		GitlabAPI apiMock = mock(GitlabAPI.class);
		GitlabProject projectMock = mock(GitlabProject.class);
		SonarReport reportMock = mock(SonarReport.class);

		when(projectMock.getId()).thenReturn(projectId);
		when(reportMock.getProject()).thenReturn(projectMock);
		when(reportMock.getCommitSha()).thenReturn(hash);

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

		when(reportMock.getIssues()).thenReturn(Stream.of(new MappedIssue(issueMock, path)));
		when(reportMock.getCommitSha()).thenReturn(hash);
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
	public void testProcessExcludeExistingComments() throws Exception {
		String summary = "SonarQube analysis reported 0 issues.\n\nWatch the comments in this conversation to review them.";

		GitlabAPI apiMock = mock(GitlabAPI.class);
		GitlabProject projectMock = mock(GitlabProject.class);
		SonarReport reportMock = mock(SonarReport.class);
		PostJobIssue issueMock = mock(PostJobIssue.class);
		InputComponent inputComponentMock = mock(InputComponent.class);

		CommitComment commentMock = mock(CommitComment.class);
		when(commentMock.getLine()).thenReturn(Integer.toString(line));
		when(commentMock.getPath()).thenReturn(path);
		when(commentMock.getNote()).thenReturn(message);

		CommitComment summaryMock = mock(CommitComment.class);
		when(summaryMock.getLine()).thenReturn(null);
		when(summaryMock.getNote()).thenReturn(summary);

		when(apiMock.getCommitComments(projectId, hash)).thenReturn(Arrays.asList(commentMock, summaryMock));

		when(projectMock.getId()).thenReturn(projectId);

		when(inputComponentMock.isFile()).thenReturn(true);
		when(issueMock.inputComponent()).thenReturn(inputComponentMock);
		when(issueMock.message()).thenReturn(message);
		when(issueMock.line()).thenReturn(line);

		when(reportMock.getIssues()).thenReturn(Stream.of(new MappedIssue(issueMock, path)));
		when(reportMock.getCommitSha()).thenReturn(hash);
		when(reportMock.getProject()).thenReturn(projectMock);

		CommitCommenter cut = new CommitCommenter(apiMock);

		cut.process(reportMock);

		verify(apiMock).getCommitComments(projectId, hash);
		verify(apiMock, never()).createCommitComment(
			eq(projectId),
			eq(hash),
			eq(message),
			eq(path),
			eq(Integer.toString(line)),
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
		PostJobIssue issueMock = mock(PostJobIssue.class);
		InputComponent inputComponentMock = mock(InputComponent.class);

		String hash = "a2b4";
		String path = "/my/file.java";
		int projectId = 42;
		int line = 44;

		when(projectMock.getId()).thenReturn(projectId);

		when(inputComponentMock.isFile()).thenReturn(true);
		when(issueMock.inputComponent()).thenReturn(inputComponentMock);
		when(issueMock.message()).thenReturn("Remove this violation!");
		when(issueMock.line()).thenReturn(line);

		when(reportMock.getIssues()).thenReturn(Stream.of(new MappedIssue(issueMock, path)));
		when(reportMock.getCommitSha()).thenReturn(hash);
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
		PostJobIssue issueMock = mock(PostJobIssue.class);
		InputComponent inputComponentMock = mock(InputComponent.class);

		String hash = "a2b4";
		String path = "/my/file.java";
		int projectId = 42;
		int line = 44;

		when(projectMock.getId()).thenReturn(projectId);

		when(inputComponentMock.isFile()).thenReturn(true);
		when(issueMock.inputComponent()).thenReturn(inputComponentMock);
		when(issueMock.message()).thenReturn("Remove this violation!");
		when(issueMock.line()).thenReturn(line);

		when(reportMock.getIssues()).thenReturn(Stream.of(new MappedIssue(issueMock, path)));
		when(reportMock.getCommitSha()).thenReturn(hash);
		when(reportMock.getProject()).thenReturn(projectMock);

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