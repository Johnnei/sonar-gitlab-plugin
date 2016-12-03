package org.johnnei.internal.gitlab;

/**
 * Created by Johnnei on 2016-12-03.
 */
public class ProcessException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ProcessException(String message) {
		super(message);
	}
}
