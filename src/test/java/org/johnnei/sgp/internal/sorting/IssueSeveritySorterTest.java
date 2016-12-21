package org.johnnei.sgp.internal.sorting;


import org.junit.Test;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by Johnnei on 2016-12-21.
 */
public class IssueSeveritySorterTest {

	private IssueSeveritySorter cut = new IssueSeveritySorter();

	@Test
	public void testCompareEqual() throws Exception {
		PostJobIssue issueOne = mock(PostJobIssue.class);
		PostJobIssue issueTwo = mock(PostJobIssue.class);

		when(issueOne.severity()).thenReturn(Severity.MAJOR);
		when(issueTwo.severity()).thenReturn(Severity.MAJOR);

		assertThat("The same severity should report that they are equal.", cut.compare(issueOne, issueTwo), equalTo(0));
	}

	@Test
	public void testCompareSmaller() throws Exception {
		PostJobIssue issueOne = mock(PostJobIssue.class);
		PostJobIssue issueTwo = mock(PostJobIssue.class);

		when(issueOne.severity()).thenReturn(Severity.INFO);
		when(issueTwo.severity()).thenReturn(Severity.MAJOR);

		assertThat("The same severity should report that they are equal.", cut.compare(issueOne, issueTwo), equalTo(-1));
	}

	@Test
	public void testCompareGreater() throws Exception {
		PostJobIssue issueOne = mock(PostJobIssue.class);
		PostJobIssue issueTwo = mock(PostJobIssue.class);

		when(issueOne.severity()).thenReturn(Severity.BLOCKER);
		when(issueTwo.severity()).thenReturn(Severity.MAJOR);

		assertThat("The same severity should report that they are equal.", cut.compare(issueOne, issueTwo), equalTo(1));
	}

}