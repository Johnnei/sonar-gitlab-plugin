package org.johnnei.sgp.internal.sonar;

import java.io.File;
import java.util.Collections;

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.reflect.Whitebox;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.PostJobDescriptor;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.utils.log.LogTester;

import org.johnnei.sgp.internal.gitlab.CommitCommenter;
import org.johnnei.sgp.internal.model.SonarReport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by Johnnei on 2016-12-21.
 */
public class CommitIssueJobTest {

	@Rule
	public LogTester logTester = new LogTester();

	private CommitIssueJob cut;

	private CommitCommenter commitCommenterMock;

	private GitLabPluginConfiguration configurationMock;

	@Before
	public void setUp() {
		commitCommenterMock = mock(CommitCommenter.class);
		configurationMock = mock(GitLabPluginConfiguration.class);

		when(configurationMock.createGitLabConnection()).thenReturn(mock(GitlabAPI.class));
		cut = new CommitIssueJob(configurationMock);

		Whitebox.setInternalState(cut, CommitCommenter.class, commitCommenterMock);
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
		PostJobContext postJobContextMock = mock(PostJobContext.class);

		PostJobIssue issueMock = mock(PostJobIssue.class);
		GitlabProject projectMock = mock(GitlabProject.class);

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
	}

}