package org.johnnei.sgp.internal.util;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * Created by Johnnei on 2016-12-20.
 */
public class Stopwatch {

	private Logger LOGGER = Loggers.get(Stopwatch.class);

	private long startTime;

	private String notice;

	public void start(String notice) {
		this.notice = notice;
		LOGGER.info(notice);
		startTime = System.nanoTime();
	}

	public void stop() {
		long stopTime = System.nanoTime();
		long duration = stopTime - startTime;
		long durationInMs = duration / 1_000_000L;
		LOGGER.info("{} (done) | time={}ms", notice, durationInMs);
	}
}
