package org.johnnei.sgp.internal.sonar;

import java.io.File;
import java.io.IOException;

import org.gitlab.api.GitlabAPI;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.utils.log.LogTester;

import org.johnnei.sgp.sonar.GitLabPlugin;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by Johnnei on 2016-12-21.
 */
public class CommitAnalysisBuilderTest {

	@Rule
	public LogTester logTester = new LogTester();

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testBuild() throws Exception {
		GitLabPluginConfiguration configurationMock = mock(GitLabPluginConfiguration.class);
		AnalysisMode analysisModeMock = mock(AnalysisMode.class);
		ProjectBuilder.Context contextMock = mock(ProjectBuilder.Context.class);

		when(configurationMock.isEnabled()).thenReturn(true);
		when(analysisModeMock.isIssues()).thenReturn(true);

		temporaryFolder.newFolder(".git");
		File baseDir = temporaryFolder.newFolder("project-a");

		ProjectReactor projectReactorMock = mock(ProjectReactor.class);
		ProjectDefinition projectDefinitionMock = mock(ProjectDefinition.class);
		when(contextMock.projectReactor()).thenReturn(projectReactorMock);
		when(projectReactorMock.getRoot()).thenReturn(projectDefinitionMock);
		when(projectDefinitionMock.getBaseDir()).thenReturn(baseDir);

		CommitAnalysisBuilder cut = new CommitAnalysisBuilder(configurationMock, analysisModeMock);
		cut.build(contextMock);

		verify(configurationMock).initialiseProject();
		verify(configurationMock).setBaseDir(temporaryFolder.getRoot());
	}

	@Test
	public void testBuildFailedToFetchProject() throws Exception {
		thrown.expect(IllegalStateException.class);
		thrown.expectCause(isA(IOException.class));
		thrown.expectMessage("GitLab project");

		GitLabPluginConfiguration configurationMock = mock(GitLabPluginConfiguration.class);
		AnalysisMode analysisModeMock = mock(AnalysisMode.class);
		ProjectBuilder.Context contextMock = mock(ProjectBuilder.Context.class);

		doThrow(new IOException("Test exception path")).when(configurationMock).initialiseProject();

		when(configurationMock.isEnabled()).thenReturn(true);
		when(analysisModeMock.isIssues()).thenReturn(true);

		CommitAnalysisBuilder cut = new CommitAnalysisBuilder(configurationMock, analysisModeMock);
		cut.build(contextMock);
	}

	@Test
	public void testBuildDisabled() throws Exception {
		GitLabPluginConfiguration configurationMock = mock(GitLabPluginConfiguration.class);
		AnalysisMode analysisModeMock = mock(AnalysisMode.class);
		ProjectBuilder.Context contextMock = mock(ProjectBuilder.Context.class);

		when(configurationMock.isEnabled()).thenReturn(false);

		CommitAnalysisBuilder cut = new CommitAnalysisBuilder(configurationMock, analysisModeMock);
		cut.build(contextMock);

		assertThat(
			"Log should indicate that the GitLab integration was disabled",
			logTester.logs(),
			hasItem(containsString("Disabling GitLab integration."))
		);
	}
	@Test
	public void testBuildWrongMode() throws Exception {
		thrown.expect(IllegalStateException.class);
		thrown.expectMessage("GitLab plugin requires");

		GitLabPluginConfiguration configurationMock = mock(GitLabPluginConfiguration.class);
		AnalysisMode analysisModeMock = mock(AnalysisMode.class);
		ProjectBuilder.Context contextMock = mock(ProjectBuilder.Context.class);

		when(configurationMock.isEnabled()).thenReturn(true);
		when(analysisModeMock.isIssues()).thenReturn(false);

		CommitAnalysisBuilder cut = new CommitAnalysisBuilder(configurationMock, analysisModeMock);
		cut.build(contextMock);
	}

}