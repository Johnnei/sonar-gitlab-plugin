package org.johnnei.sgp.internal.sorting;

import java.util.Comparator;

import org.sonar.api.batch.postjob.issue.PostJobIssue;

/**
 * Compares two issues based on {@link PostJobIssue#severity()}.
 */
public class IssueSeveritySorter implements Comparator<PostJobIssue> {

	@Override
	public int compare(PostJobIssue o1, PostJobIssue o2) {
		int difference = o1.severity().ordinal() - o2.severity().ordinal();
		if (difference < 0) {
			return -1;
		} else if (difference > 0) {
			return 1;
		} else {
			return 0;
		}
	}
}
