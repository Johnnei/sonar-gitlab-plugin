package org.johnnei.sgp.internal.sonar;

import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.LogTester;

import org.johnnei.sgp.internal.gitlab.api.v4.GitLabApi;
import org.johnnei.sgp.internal.gitlab.api.v4.model.GitLabNamespace;
import org.johnnei.sgp.internal.gitlab.api.v4.model.GitLabProject;
import org.johnnei.sgp.sonar.GitLabPlugin;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GitLabPluginConfigurationTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public LogTester logTester = new LogTester();

	private GitLabPluginConfiguration cut;

	private GitLabApi apiMock;

	private Settings settingsMock;

	@Before
	public void setUp() {
		apiMock = mock(GitLabApi.class);
		settingsMock = mock(Settings.class);
		cut = new GitLabPluginConfigurationMock(apiMock, settingsMock);
	}

	@Test
	public void testIsEnabled() {
		when(settingsMock.getString("sonar.gitlab.analyse.commit")).thenReturn("a4b8");
		assertThat("Hash has been supplied, thus the plugin should enable", cut.isEnabled(), is(true));
	}

	@Test
	public void testIsEnabledDisabled() {
		assertThat("Hash has not been supplied, thus the plugin should disable", cut.isEnabled(), is(false));
	}

	@Test
	public void testGetCommitHash() {
		String hash = "a4b8";
		when(settingsMock.getString("sonar.gitlab.analyse.commit")).thenReturn(hash);
		assertThat("Invalid hash has been returned. Value from settings should be used.", cut.getCommitHash(), equalTo(hash));
	}

	@Test
	public void testGetBaseBranch() {
		String baseBranch = "develop";
		when(settingsMock.getString("sonar.gitlab.analyse.base")).thenReturn(baseBranch);
		assertThat("Invalid branch has been returned. Value from settings should be used.", cut.getBaseBranch(), equalTo(baseBranch));
	}

	@Test
	public void testInitialiseFailOnMissingGitLabHost() throws Exception {
		when(settingsMock.getString("sonar.gitlab.analyse.project")).thenReturn("root/project");
		when(settingsMock.getString("sonar.gitlab.auth.token")).thenReturn("secure");

		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("GitLab Instance");

		cut.initialiseProject();
	}

	@Test
	public void testInitialiseFailOnMissingAuthToken() throws Exception {
		when(settingsMock.getString("sonar.gitlab.analyse.project")).thenReturn("root/project");
		when(settingsMock.getString("sonar.gitlab.uri")).thenReturn("http://localhost.localdomain/");

		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("user token");

		cut.initialiseProject();
	}

	@Test
	public void testInitialise() throws Exception {
		when(settingsMock.getString("sonar.gitlab.analyse.project")).thenReturn("root/project");
		when(settingsMock.getString("sonar.gitlab.uri")).thenReturn("http://localhost.localdomain/");
		when(settingsMock.getString("sonar.gitlab.auth.token")).thenReturn("secure");

		GitLabProject projectMock = mock(GitLabProject.class);
		GitLabNamespace namespaceMock = mock(GitLabNamespace.class);
		when(projectMock.getName()).thenReturn("project");
		when(projectMock.getNamespace()).thenReturn(namespaceMock);
		when(namespaceMock.getName()).thenReturn("root");

		when(apiMock.getProjects()).thenReturn(Collections.singletonList(projectMock));

		cut.initialiseProject();

		assertThat("Initialisation duration should be logged", logTester.logs(), hasItem(containsString("GitLab project")));
		assertThat("Project should have been initialised based on settings.", cut.getProject(), equalTo(projectMock));
	}

	@Test
	public void testInitialiseFailOnMissingProjectKey() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("sonar.gitlab.analyse.project");

		when(settingsMock.getString("sonar.gitlab.uri")).thenReturn("http://localhost.localdomain/");
		when(settingsMock.getString("sonar.gitlab.auth.token")).thenReturn("secure");

		cut.initialiseProject();
	}

	@Test
	public void testIsBreakPipelineEnabled() throws Exception {
		when(settingsMock.getBoolean(GitLabPlugin.GITLAB_BREAK_PIPELINE)).thenReturn(true);

		assertThat("Settings value should have been used", cut.isBreakPipelineEnabled(), is(true));

		verify(settingsMock).getBoolean("sonar.gitlab.pipeline.break");
	}

	private static final class GitLabPluginConfigurationMock extends GitLabPluginConfiguration {

		private GitLabApi apiMock;

		GitLabPluginConfigurationMock(GitLabApi apiMock, Settings settings) {
			super(settings);
			this.apiMock = apiMock;
		}

		@Override
		GitLabApi createConnection(String url, String token) {
			return apiMock;
		}

	}

}
