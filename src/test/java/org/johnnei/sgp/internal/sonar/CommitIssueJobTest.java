package org.johnnei.sgp.internal.sonar;

import java.io.File;
import java.io.IOException;
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
import org.mockito.ArgumentCaptor;
import org.powermock.reflect.Whitebox;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.PostJobDescriptor;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.log.LogTester;

import org.johnnei.sgp.internal.gitlab.CommitCommenter;
import org.johnnei.sgp.internal.gitlab.ProcessException;
import org.johnnei.sgp.internal.model.MappedIssue;
import org.johnnei.sgp.internal.model.SonarReport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isA;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by Johnnei on 2016-12-21.
 */
public class CommitIssueJobTest {

	@Rule
	public LogTester logTester = new LogTester();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private CommitIssueJob cut;

	private CommitCommenter commitCommenterMock;

	private GitLabPluginConfiguration configurationMock;

	private PathResolver pathResolverMock;

	private GitlabAPI gitlabApiMock;

	@Before
	public void setUp() {
		commitCommenterMock = mock(CommitCommenter.class);
		configurationMock = mock(GitLabPluginConfiguration.class);
		pathResolverMock = mock(PathResolver.class);
		gitlabApiMock = mock(GitlabAPI.class);

		when(configurationMock.createGitLabConnection()).thenReturn(gitlabApiMock);
		cut = new CommitIssueJob(configurationMock);

		Whitebox.setInternalState(cut, CommitCommenter.class, commitCommenterMock);
		Whitebox.setInternalState(cut, PathResolver.class, pathResolverMock);
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
	public void testExecuteThrowProcessExceptionOnApiFailure() throws Exception {
		thrown.expect(ProcessException.class);
		thrown.expectCause(isA(IOException.class));
		thrown.expectMessage("commit diff");

		int projectId = 42;
		String hash = "a2b6";

		GitlabProject projectMock = mock(GitlabProject.class);
		when(projectMock.getId()).thenReturn(projectId);

		when(configurationMock.getProject()).thenReturn(projectMock);
		when(configurationMock.getCommitHash()).thenReturn(hash);
		when(gitlabApiMock.getCommitDiffs(projectId, hash)).thenThrow(new IOException("Test failure path"));
		cut.execute(mock(PostJobContext.class));
	}

	@Test
	public void testExecute() throws Exception {
		String hash = "a2b4";
		int projectId = 42;
		File file = new File("Main.java");
		PostJobContext postJobContextMock = mock(PostJobContext.class);

		PostJobIssue issueMock = mock(PostJobIssue.class);
		GitlabProject projectMock = mock(GitlabProject.class);
		when(projectMock.getId()).thenReturn(projectId);
		InputFile inputComponentMock = mock(InputFile.class);

		when(issueMock.inputComponent()).thenReturn(inputComponentMock);
		when(issueMock.line()).thenReturn(3);
		when(inputComponentMock.isFile()).thenReturn(true);
		when(inputComponentMock.file()).thenReturn(file);
		when(pathResolverMock.relativePath(notNull(File.class), eq(file))).thenReturn("src/Main.java");

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

		// Add a deleted file for coverage.
		GitlabCommitDiff commitDiffTwo = mock(GitlabCommitDiff.class);
		when(commitDiffTwo.getDeletedFile()).thenReturn(true);

		when(gitlabApiMock.getCommitDiffs(projectId, hash)).thenReturn(Arrays.asList(commitDiffOne, commitDiffTwo));

		when(postJobContextMock.issues()).thenReturn(Collections.singletonList(issueMock));
		when(configurationMock.getGitBaseDir()).thenReturn(new File("."));
		when(configurationMock.getCommitHash()).thenReturn(hash);
		when(configurationMock.getProject()).thenReturn(projectMock);

		cut.execute(postJobContextMock);

		ArgumentCaptor<SonarReport> reportCaptor = ArgumentCaptor.forClass(SonarReport.class);

		verify(commitCommenterMock).process(reportCaptor.capture());

		SonarReport report = reportCaptor.getValue();
		assertThat("Project must not have changed", report.getProject(), equalTo(projectMock));
		assertThat("Commit sha must not have changed", report.getCommitSha(), equalTo(hash));
		assertThat("The iterable of 1 issue should have result in a stream of 1 issue", report.getIssues().count(), equalTo(1L));
		verify(commitDiffTwo, never()).getDiff();
	}

	@Test
	public void testExecuteUnmappedFile() throws Exception {
		SonarReport reportMock = mock(SonarReport.class);
		PostJobIssue issueMock = mock(PostJobIssue.class);
		InputComponent inputComponentMock = mock(InputComponent.class);

		when(inputComponentMock.isFile()).thenReturn(false);
		when(issueMock.inputComponent()).thenReturn(inputComponentMock);
		when(issueMock.message()).thenReturn("Remove this violation!");

		when(reportMock.getIssues()).thenReturn(Stream.of(new MappedIssue(issueMock, "")));
		String hash = "a2b4";
		PostJobContext postJobContextMock = mock(PostJobContext.class);

		GitlabProject projectMock = mock(GitlabProject.class);

		when(postJobContextMock.issues()).thenReturn(Collections.singletonList(issueMock));
		when(configurationMock.getGitBaseDir()).thenReturn(new File("."));
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