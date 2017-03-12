package org.johnnei.sgp.test;

import java.io.File;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.postjob.issue.PostJobIssue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockIssue {

	public static PostJobIssue mockFileIssue(File file) {
		InputFile inputComponentMock = mock(InputFile.class);
		when(inputComponentMock.isFile()).thenReturn(true);
		when(inputComponentMock.file()).thenReturn(file);

		PostJobIssue issueMock = mock(PostJobIssue.class);
		when(issueMock.inputComponent()).thenReturn(inputComponentMock);
		when(issueMock.line()).thenReturn(null);
		when(issueMock.message()).thenReturn("File level violation.");

		return issueMock;
	}
}
