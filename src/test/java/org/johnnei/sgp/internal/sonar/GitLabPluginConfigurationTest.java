package org.johnnei.sgp.internal.sonar;

import java.io.IOException;
import java.util.Collections;

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.GitlabAPIException;
import org.gitlab.api.TokenType;
import org.gitlab.api.models.GitlabNamespace;
import org.gitlab.api.models.GitlabProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.LogTester;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
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

	private GitlabAPI apiMock;

	private Settings settingsMock;

	@Before
	public void setUp() {
		apiMock = mock(GitlabAPI.class);
		settingsMock = mock(Settings.class);
		cut = new GitLabPluginConfigurationMock(apiMock, settingsMock, false);
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
	public void testInitialiseFallBackToPrivateToken() throws Exception {
		cut = new GitLabPluginConfigurationMock(apiMock, settingsMock, true);

		when(settingsMock.getString("sonar.gitlab.analyse.project")).thenReturn("root/project");
		when(settingsMock.getString("sonar.gitlab.uri")).thenReturn("http://localhost.localdomain/");
		when(settingsMock.getString("sonar.gitlab.auth.token")).thenReturn("secure");

		GitlabProject projectMock = mock(GitlabProject.class);
		GitlabNamespace namespaceMock = mock(GitlabNamespace.class);
		when(projectMock.getName()).thenReturn("project");
		when(projectMock.getNamespace()).thenReturn(namespaceMock);
		when(namespaceMock.getName()).thenReturn("root");

		when(apiMock.getUser())
			// This should cause the fallback to private token
			.thenThrow(new GitlabAPIException("test", 401, new IOException("Test cause")))
			.thenReturn(null);
		when(apiMock.getProjects()).thenReturn(Collections.singletonList(projectMock));

		cut.initialiseProject();

		assertThat("Usage of private token should be discouraged.", logTester.logs(), hasItem(containsString("less secure")));
	}

	@Test
	public void testInitialiseFailToConnect() throws Exception {
		thrown.expect(IllegalStateException.class);
		thrown.expectMessage("GitLab");

		cut = new GitLabPluginConfigurationMock(apiMock, settingsMock, true);

		when(settingsMock.getString("sonar.gitlab.analyse.project")).thenReturn("root/project");
		when(settingsMock.getString("sonar.gitlab.uri")).thenReturn("http://localhost.localdomain/");
		when(settingsMock.getString("sonar.gitlab.auth.token")).thenReturn("secure");

		GitlabProject projectMock = mock(GitlabProject.class);
		GitlabNamespace namespaceMock = mock(GitlabNamespace.class);
		when(projectMock.getName()).thenReturn("project");
		when(projectMock.getNamespace()).thenReturn(namespaceMock);
		when(namespaceMock.getName()).thenReturn("root");

		when(apiMock.getUser()).thenThrow(new IOException("test error path"));

		cut.initialiseProject();
	}

	@Test
	public void testInitialiseFailToAuthorize() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("authorize");

		cut = new GitLabPluginConfigurationMock(apiMock, settingsMock, true);

		when(settingsMock.getString("sonar.gitlab.analyse.project")).thenReturn("root/project");
		when(settingsMock.getString("sonar.gitlab.uri")).thenReturn("http://localhost.localdomain/");
		when(settingsMock.getString("sonar.gitlab.auth.token")).thenReturn("secure");

		GitlabProject projectMock = mock(GitlabProject.class);
		GitlabNamespace namespaceMock = mock(GitlabNamespace.class);
		when(projectMock.getName()).thenReturn("project");
		when(projectMock.getNamespace()).thenReturn(namespaceMock);
		when(namespaceMock.getName()).thenReturn("root");

		when(apiMock.getUser())
			// This should cause the fallback to private token
			.thenThrow(new GitlabAPIException("test", 401, new IOException("Test cause")))
			.thenThrow(new GitlabAPIException("test", 401, new IOException("Test cause")));

		cut.initialiseProject();
	}

	@Test
	public void testInitialise() throws Exception {
		when(settingsMock.getString("sonar.gitlab.analyse.project")).thenReturn("root/project");
		when(settingsMock.getString("sonar.gitlab.uri")).thenReturn("http://localhost.localdomain/");
		when(settingsMock.getString("sonar.gitlab.auth.token")).thenReturn("secure");

		GitlabProject projectMock = mock(GitlabProject.class);
		GitlabNamespace namespaceMock = mock(GitlabNamespace.class);
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

	private static final class GitLabPluginConfigurationMock extends GitLabPluginConfiguration {

		private GitlabAPI apiMock;

		private final boolean allowPrivateToken;

		GitLabPluginConfigurationMock(GitlabAPI apiMock, Settings settings, boolean allowPrivateToken) {
			super(settings);
			this.apiMock = apiMock;
			this.allowPrivateToken = allowPrivateToken;
		}

		@Override
		GitlabAPI createConnection(String url, String token, TokenType tokenType) {
			if (!allowPrivateToken) {
				assertThat("Private token should not be used by default.", tokenType, not(equalTo(TokenType.PRIVATE_TOKEN)));
			}
			return apiMock;
		}

	}

}
