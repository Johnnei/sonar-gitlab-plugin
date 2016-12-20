package org.johnnei.sgp.internal.util;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.StringContains.containsString;

/**
 * Created by Johnnei on 2016-12-20.
 */
public class StopwatchTest {

	@Rule
	public LogTester logTester = new LogTester();

	@Test
	public void testStart() {
		Stopwatch stopwatch = new Stopwatch();
		stopwatch.start("Task 1");

		assertThat(logTester.logs(), hasItem(containsString("Task 1")));
	}

	@Test
	public void testStop() {
		Stopwatch stopwatch = new Stopwatch();
		stopwatch.start("Task 1");
		stopwatch.stop();

		assertThat(logTester.logs(), hasItem(containsString("Task 1 (done) | time=")));
	}

}