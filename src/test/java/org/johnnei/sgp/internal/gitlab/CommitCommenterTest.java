package org.johnnei.sgp.internal.gitlab;

import java.io.IOException;
import java.util.stream.Stream;

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabProject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.postjob.issue.PostJobIssue;

import org.johnnei.sgp.internal.model.SonarReport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CommitCommenterTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testProcess() throws Exception {
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

		when(reportMock.getIssues()).thenReturn(Stream.of(issueMock));
		when(reportMock.getCommitSha()).thenReturn(hash);
		when(reportMock.getFilePath(inputComponentMock)).thenReturn(path);
		when(reportMock.getProject()).thenReturn(projectMock);

		CommitCommenter cut = new CommitCommenter(apiMock);

		cut.process(reportMock);

		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(apiMock).createCommitComment(
			eq(projectId),
			eq(hash),
			captor.capture(),
			eq(path),
			eq(Integer.toString(line)),
			eq("new")
		);

		assertThat(captor.getValue(), containsString(issueMock.message()));
	}

	@Test
	public void testProcessUnmappedFile() throws Exception {
		GitlabAPI apiMock = mock(GitlabAPI.class);
		SonarReport reportMock = mock(SonarReport.class);
		PostJobIssue issueMock = mock(PostJobIssue.class);
		InputComponent inputComponentMock = mock(InputComponent.class);

		when(inputComponentMock.isFile()).thenReturn(false);
		when(issueMock.inputComponent()).thenReturn(inputComponentMock);
		when(issueMock.message()).thenReturn("Remove this violation!");

		when(reportMock.getIssues()).thenReturn(Stream.of(issueMock));

		CommitCommenter cut = new CommitCommenter(apiMock);

		cut.process(reportMock);

		verify(apiMock, never()).createCommitComment(anyInt(), anyString(), anyString(), anyString(), anyString(), anyString());
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

		when(reportMock.getIssues()).thenReturn(Stream.of(issueMock));
		when(reportMock.getCommitSha()).thenReturn(hash);
		when(reportMock.getFilePath(inputComponentMock)).thenReturn(path);
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

}