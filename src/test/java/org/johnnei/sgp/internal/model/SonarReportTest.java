package org.johnnei.sgp.internal.model;

import java.util.Arrays;

import org.gitlab.api.models.GitlabProject;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;

import org.johnnei.sgp.internal.model.diff.UnifiedDiff;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SonarReportTest {

	private SonarReport cut;

	@Before
	public void setUp() {
		PostJobIssue criticalIssueMock = mock(PostJobIssue.class);
		PostJobIssue minorIssueMock = mock(PostJobIssue.class);

		when(criticalIssueMock.severity()).thenReturn(Severity.CRITICAL);
		when(minorIssueMock.severity()).thenReturn(Severity.MINOR);

		UnifiedDiff diff = mock(UnifiedDiff.class);
		when(diff.getCommitSha()).thenReturn("a2b4");
		UnifiedDiff diff2 = mock(UnifiedDiff.class);
		when(diff2.getCommitSha()).thenReturn("a3b4");

		cut = new SonarReport.Builder()
			.setBuildCommitSha("a2b4")
			.setProject(mock(GitlabProject.class))
			.setIssues(Arrays.asList(new MappedIssue(criticalIssueMock, diff, ""), new MappedIssue(minorIssueMock, diff2, "")))
			.build();
	}

	@Test
	public void testCountIssuesWithSeverity() throws Exception {
		assertThat("No blocker issues are available", cut.countIssuesWithSeverity(Severity.BLOCKER), equalTo(0L));
		assertThat("Only 1 critical issue is available", cut.countIssuesWithSeverity(Severity.CRITICAL), equalTo(1L));
		assertThat("No major issues are available", cut.countIssuesWithSeverity(Severity.MAJOR), equalTo(0L));
		assertThat("Only 1 minor issue is available", cut.countIssuesWithSeverity(Severity.MINOR), equalTo(1L));
		assertThat("No info issues are available", cut.countIssuesWithSeverity(Severity.INFO), equalTo(0L));
	}

	@Test
	public void testGetIssueCount() throws Exception {
		assertThat("2 issues were in the collection.", cut.getIssueCount(), equalTo(2));
	}

	@Test
	public void testGetCommitShas() throws Exception {
		assertThat("2 issues were in the collection.", cut.getCommitShas().count(), equalTo(2L));
	}

	@Test
	public void testGetCommitShasUnique() throws Exception {
		PostJobIssue criticalIssueMock = mock(PostJobIssue.class);
		PostJobIssue minorIssueMock = mock(PostJobIssue.class);

		when(criticalIssueMock.severity()).thenReturn(Severity.CRITICAL);
		when(minorIssueMock.severity()).thenReturn(Severity.MINOR);

		UnifiedDiff diff = mock(UnifiedDiff.class);
		when(diff.getCommitSha()).thenReturn("a2b4");
		UnifiedDiff diff2 = mock(UnifiedDiff.class);
		when(diff2.getCommitSha()).thenReturn("a3b4");

		cut = new SonarReport.Builder()
			.setBuildCommitSha("a2b4")
			.setProject(mock(GitlabProject.class))
			.setIssues(Arrays.asList(
				new MappedIssue(criticalIssueMock, diff, ""),
				new MappedIssue(minorIssueMock, diff, ""),
				new MappedIssue(minorIssueMock, diff2, ""))
			)
			.build();

		assertThat("2 issues were in the collection.", cut.getCommitShas().count(), equalTo(2L));
	}

}
