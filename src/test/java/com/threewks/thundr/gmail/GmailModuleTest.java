/*
 * This file is a component of thundr, a software library from 3wks.
 * Read more: http://3wks.github.io/thundr/
 * Copyright (C) 2015 3wks, <thundr@3wks.com.au>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.threewks.thundr.gmail;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.util.store.DataStoreFactory;
import com.threewks.thundr.configuration.ConfigurationException;
import com.threewks.thundr.injection.UpdatableInjectionContext;
import com.threewks.thundr.module.DependencyRegistry;
import com.threewks.thundr.route.Router;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class) public class GmailModuleTest {

	@Rule public ExpectedException expectedException = ExpectedException.none();

	@Mock(answer = Answers.RETURNS_DEEP_STUBS) private UpdatableInjectionContext injectionContext;
	@Mock private Router router;
	@Mock private DependencyRegistry dependencyRegistry;

	private GmailModule module;

	@Before public void before() {

		// mock behaviour
		when(injectionContext.get(String.class, "host")).thenReturn("https://gradresearchforms.apps.monash.edu");
		when(injectionContext.get(String.class, "gmailOAuthClientId")).thenReturn("oAuthClientId");
		when(injectionContext.get(String.class, "gmailOAuthClientSecret")).thenReturn("oAuthClientSecret");
		when(injectionContext.get(String.class, "gmailAdminRootPath")).thenReturn(null);
		when(injectionContext.get(DataStoreFactory.class)).thenReturn(mock(DataStoreFactory.class));
		when(injectionContext.get(HttpTransport.class)).thenReturn(mock(HttpTransport.class));
		when(injectionContext.get(Router.class)).thenReturn(router);

		module = new GmailModule();
	}

	@Test public void shouldNotAddAnyDependencies() {
		module.requires(dependencyRegistry);

		verify(dependencyRegistry, never()).addDependency(any(Class.class));
	}

	@Test public void shouldConfigureOAuthAndServices() {
		module.configure(injectionContext);

		assertThat(injectionContext.get(GoogleAuthorizationCodeFlow.class, "gmailAuthorizationCodeFlow"), notNullValue());
	}

	@Test public void shouldAddRoutesOnStart() {
		module.start(injectionContext);

		verify(router).get("/admin/gmail/setup", GmailAdminController.class, "setup", "gmail.admin.setup");
		verify(router).get("/admin/gmail/setup/oauth2callback", GmailAdminController.class, "oauthCallback", "gmail.admin.oauthCallback");
	}

	@Test public void shouldThrowExceptionIfHostPropertyNotSet() {
		expectedException.expect(ConfigurationException.class);
		expectedException.expectMessage(is("Property `host` not found. Did you forget to add it to application.properties?"));

		when(injectionContext.get(String.class, "host")).thenReturn(null);

		module.configure(injectionContext);
	}

	@Test public void shouldThrowExceptionIfOAuthClientIdPropertyNotSet() {
		expectedException.expect(ConfigurationException.class);
		expectedException.expectMessage(is("Property `gmailOAuthClientId` not found. Did you forget to add it to application.properties?"));

		when(injectionContext.get(String.class, "gmailOAuthClientId")).thenReturn(null);

		module.configure(injectionContext);
	}

	@Test public void shouldThrowExceptionIfOAuthClientSecretPropertyNotSet() {
		expectedException.expect(ConfigurationException.class);
		expectedException.expectMessage(is("Property `gmailOAuthClientSecret` not found. Did you forget to add it to application.properties?"));

		when(injectionContext.get(String.class, "gmailOAuthClientSecret")).thenReturn(null);

		module.configure(injectionContext);
	}

	@Test public void shouldThrowExceptionIfDataStoreFactoryDependencyNotSet() {
		expectedException.expect(ConfigurationException.class);
		expectedException.expectMessage(is("Required dependency of type `com.google.api.client.util.store.DataStoreFactory` not found. Did you forget to inject it?"));

		when(injectionContext.get(DataStoreFactory.class)).thenReturn(null);

		module.configure(injectionContext);
	}

	@Test public void shouldThrowExceptionIfHttpTransportDependencyNotSet() {
		expectedException.expect(ConfigurationException.class);
		expectedException.expectMessage(is("Required dependency of type `com.google.api.client.http.HttpTransport` not found. Did you forget to inject it?"));

		when(injectionContext.get(HttpTransport.class)).thenReturn(null);

		module.configure(injectionContext);
	}

	@Test public void shoudlAllowOverridingOfAdminRootPath() {
		when(injectionContext.get(String.class, "gmailAdminRootPath")).thenReturn("/custom/admin/gmail");

		module.start(injectionContext);

		verify(router).get("/custom/admin/gmail/setup", GmailAdminController.class, "setup", "gmail.admin.setup");
		verify(router).get("/custom/admin/gmail/setup/oauth2callback", GmailAdminController.class, "oauthCallback", "gmail.admin.oauthCallback");
	}

}
