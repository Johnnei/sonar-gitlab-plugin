package org.johnnei.sgp.test;

import java.io.File;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockIssue {

	public static PostJobIssue mockFileIssue(File file) {
		InputFile inputComponentMock = mock(InputFile.class);
		when(inputComponentMock.isFile()).thenReturn(true);
		when(inputComponentMock.file()).thenReturn(file);
		when(inputComponentMock.absolutePath()).thenReturn(file.getAbsolutePath());

		PostJobIssue issueMock = mock(PostJobIssue.class);
		when(issueMock.inputComponent()).thenReturn(inputComponentMock);
		when(issueMock.line()).thenReturn(null);
		when(issueMock.message()).thenReturn("File level violation.");
		when(issueMock.severity()).thenReturn(Severity.MAJOR);

		return issueMock;
	}

	public static PostJobIssue mockInlineIssue(String file, int line, Severity severity, String message) {
		return mockInlineIssue(new File(file), line, severity, message);
	}

	public static PostJobIssue mockInlineIssue(File file, int line, Severity severity, String message) {
		InputFile inputComponentMock = mock(InputFile.class);
		when(inputComponentMock.isFile()).thenReturn(true);
		when(inputComponentMock.file()).thenReturn(file);
		when(inputComponentMock.absolutePath()).thenReturn(file.getAbsolutePath());

		PostJobIssue issueMock = mock(PostJobIssue.class);
		when(issueMock.inputComponent()).thenReturn(inputComponentMock);
		when(issueMock.message()).thenReturn(message);
		when(issueMock.line()).thenReturn(line);
		when(issueMock.severity()).thenReturn(severity);
		return issueMock;
	}
}
