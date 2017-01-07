package org.johnnei.sgp.sonar;

import org.junit.Test;
import org.sonar.api.Plugin;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GitLabPluginTest {

	@Test
	public void testDefineRegistersPluginClasses() throws Exception {
		GitLabPlugin cut = new GitLabPlugin();

		Plugin.Context contextMock = mock(Plugin.Context.class);
		when(contextMock.addExtension(any())).thenReturn(contextMock);

		cut.define(contextMock);

		verify(contextMock, atLeastOnce()).addExtension(any());
	}

}