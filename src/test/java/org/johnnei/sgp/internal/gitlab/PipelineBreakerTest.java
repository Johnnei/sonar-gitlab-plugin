package org.johnnei.sgp.internal.gitlab;

import java.io.IOException;
import java.util.stream.Stream;

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;

import org.johnnei.sgp.internal.model.MappedIssue;
import org.johnnei.sgp.internal.model.SonarReport;
import org.johnnei.sgp.internal.sonar.GitLabPluginConfiguration;

import static org.hamcrest.CoreMatchers.isA;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PipelineBreakerTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Mock
	private GitLabPluginConfiguration configuration;

	private GitlabAPI gitlabApiMock;

	private GitlabProject projectMock;

	@InjectMocks
	private PipelineBreaker cut;

	@Before
	public void setUp() {
		gitlabApiMock = mock(GitlabAPI.class);
		projectMock = mock(GitlabProject.class);
		when(configuration.createGitLabConnection()).thenReturn(gitlabApiMock);
		when(configuration.getProject()).thenReturn(projectMock);
	}

	@Test
	public void testProcessDisabled() throws Exception {
		when(configuration.isBreakPipelineEnabled()).thenReturn(false);

		SonarReport reportMock = mock(SonarReport.class);

		cut.process(reportMock);

		verifyZeroInteractions(gitlabApiMock);
	}

	@Test
	public void testProcessException() throws Exception {
		thrown.expect(ProcessException.class);
		thrown.expectCause(isA(IOException.class));

		when(configuration.isBreakPipelineEnabled()).thenReturn(true);

		String hash = "a2b4";

		SonarReport reportMock = mock(SonarReport.class);
		when(reportMock.getIssues()).thenReturn(Stream.empty());
		when(reportMock.getBuildCommitSha()).thenReturn(hash);

		when(gitlabApiMock.createCommitStatus(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenThrow(new IOException(
			"Test exception path"));

		cut.process(reportMock);
	}

	@Test
	public void testProcessNoIssue() throws Exception {
		when(configuration.isBreakPipelineEnabled()).thenReturn(true);

		String hash = "a2b4";

		SonarReport reportMock = mock(SonarReport.class);
		MappedIssue infoIssue = mockIssue(Severity.INFO);
		MappedIssue minorIssue = mockIssue(Severity.MINOR);
		MappedIssue majorIssue = mockIssue(Severity.MAJOR);

		when(reportMock.getIssues()).thenReturn(Stream.of(infoIssue, minorIssue, majorIssue));
		when(reportMock.getBuildCommitSha()).thenReturn(hash);

		cut.process(reportMock);

		verify(gitlabApiMock).createCommitStatus(projectMock, hash, "success", null, "SonarQube", null, "No critical (or worse) issues found.");
	}

	@Test
	public void testProcessCriticalIssue() throws Exception {
		when(configuration.isBreakPipelineEnabled()).thenReturn(true);

		String hash = "a2b4";

		SonarReport reportMock = mock(SonarReport.class);
		MappedIssue infoIssue = mockIssue(Severity.INFO);
		MappedIssue minorIssue = mockIssue(Severity.MINOR);
		MappedIssue criticalIssue = mockIssue(Severity.CRITICAL);

		when(reportMock.getIssues()).thenReturn(Stream.of(infoIssue, minorIssue, criticalIssue));
		when(reportMock.getBuildCommitSha()).thenReturn(hash);

		cut.process(reportMock);

		verify(gitlabApiMock).createCommitStatus(projectMock, hash, "failed", null, "SonarQube", null, "A critical or worse issue has been found.");
	}

	@Test
	public void testProcessBlockerIssue() throws Exception {
		when(configuration.isBreakPipelineEnabled()).thenReturn(true);

		String hash = "a2b4";

		SonarReport reportMock = mock(SonarReport.class);
		MappedIssue infoIssue = mockIssue(Severity.INFO);
		MappedIssue minorIssue = mockIssue(Severity.MINOR);
		MappedIssue blockerIssue = mockIssue(Severity.BLOCKER);

		when(reportMock.getIssues()).thenReturn(Stream.of(infoIssue, minorIssue, blockerIssue));
		when(reportMock.getBuildCommitSha()).thenReturn(hash);

		cut.process(reportMock);

		verify(gitlabApiMock).createCommitStatus(projectMock, hash, "failed", null, "SonarQube", null, "A critical or worse issue has been found.");
	}

	private MappedIssue mockIssue(Severity severity) {
		PostJobIssue issueMock = mock(PostJobIssue.class);
		when(issueMock.severity()).thenReturn(severity);

		MappedIssue mappedIssue = mock(MappedIssue.class);
		when(mappedIssue.getIssue()).thenReturn(issueMock);
		return mappedIssue;
	}

}
