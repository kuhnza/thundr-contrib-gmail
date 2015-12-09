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
import com.threewks.thundr.configuration.ConfigurationException;
import com.threewks.thundr.injection.InjectionContextImpl;
import com.threewks.thundr.injection.UpdatableInjectionContext;
import com.threewks.thundr.module.DependencyRegistry;
import com.threewks.thundr.route.HttpMethod;
import com.threewks.thundr.route.Route;
import com.threewks.thundr.route.Router;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class GmailModuleTest {

	@Rule public ExpectedException expectedException = ExpectedException.none();

	private UpdatableInjectionContext injectionContext;
	private DependencyRegistry dependencyRegistry;
	private Router router;

	private GmailModule module;

	@Before
	public void before() {

		router = new Router();

		dependencyRegistry = new DependencyRegistry();

		injectionContext = new InjectionContextImpl();
		injectionContext.inject(router).as(Router.class);
		injectionContext.inject("blogId").named("bloggerBlogId").as(String.class);
		injectionContext.inject("oAuthUserId").named("bloggerOAuthUserId").as(String.class);
		injectionContext.inject("oAuthClientId").named("bloggerOAuthClientId").as(String.class);
		injectionContext.inject("oAuthClientSecret").named("bloggerOAuthClientSecret").as(String.class);

		module = new GmailModule();
	}

	@Test
	public void shouldRequireDependencies() {
		module.requires(dependencyRegistry);

		assertThat(dependencyRegistry.getDependencies(), hasSize(1));
	}

	@Test
	public void shouldConfigureOAuthAndServices() {
		module.configure(injectionContext);

		assertThat(injectionContext.get(GoogleAuthorizationCodeFlow.class, "bloggerAuthorizationCodeFlow"), notNullValue());
	}

	@Test
	public void shouldAddRoutesOnStart() {
		module.start(injectionContext);

		assertRoute(router.getNamedRoute("blog.admin.setup"), HttpMethod.GET, "/admin/blog/setup");
		assertRoute(router.getNamedRoute("blog.admin.setup.oauthCallback"), HttpMethod.GET, "/admin/blog/setup/oauth2callback");
	}

	@Test
	public void shouldThrowExceptionIfOAuthClientIdPropertyNotSet() {
		expectedException.expect(ConfigurationException.class);
		expectedException.expectMessage(is("Property `bloggerOAuthClientId` not found. Did you forget to add it to application.properties?"));

		injectionContext = new InjectionContextImpl();
		module.configure(injectionContext);
	}

	@Test
	public void shouldThrowExceptionIfOAuthClientSecretPropertyNotSet() {
		expectedException.expect(ConfigurationException.class);
		expectedException.expectMessage(is("Property `bloggerOAuthClientSecret` not found. Did you forget to add it to application.properties?"));

		injectionContext = new InjectionContextImpl();
		injectionContext.inject("oAuthClientId").named("bloggerOAuthClientId").as(String.class);

		module.configure(injectionContext);
	}

	@Test
	public void shoudlAllowOverridingOfAdminBlogPath() {
		injectionContext.inject("/custom/root/blog/path").named("blogAdminRootPath").as(String.class);

		module.start(injectionContext);

		assertRoute(router.getNamedRoute("blog.admin.setup"), HttpMethod.GET, "/custom/root/blog/path/setup");
		assertRoute(router.getNamedRoute("blog.admin.setup.oauthCallback"), HttpMethod.GET, "/custom/root/blog/path/setup/oauth2callback");
	}

	private void assertRoute(Route route, HttpMethod method, String url) {
		assertThat(route.getMethod(), is(method));
		assertThat(route.getRoute(), is(url));
	}
}
