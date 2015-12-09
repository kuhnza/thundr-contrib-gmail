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
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.services.gmail.GmailScopes;
import com.threewks.thundr.configuration.ConfigurationException;
import com.threewks.thundr.injection.BaseModule;
import com.threewks.thundr.injection.InjectionContext;
import com.threewks.thundr.injection.UpdatableInjectionContext;
import com.threewks.thundr.logger.Logger;
import com.threewks.thundr.module.DependencyRegistry;
import com.threewks.thundr.route.Router;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

public class GmailModule extends BaseModule {

	@Override
	public void requires(DependencyRegistry dependencyRegistry) {
		super.requires(dependencyRegistry);
	}

	@Override
	public void configure(UpdatableInjectionContext injectionContext) {
		super.configure(injectionContext);

		// applications using this module must provide a 'host' config property
		getRequiredProperty(injectionContext, "host");

		// applications using this module must provide 'gmailOAuthClientId' and 'gmailOAuthClientSecret' config properties
		String clientId = getRequiredProperty(injectionContext, "gmailOAuthClientId");
		String clientSecret = getRequiredProperty(injectionContext, "gmailOAuthClientSecret");

		// applications using this module must inject a DataStoreFactory and HttpTransport instance
		DataStoreFactory dataStoreFactory = getRequredDependency(injectionContext, DataStoreFactory.class);
		HttpTransport httpTransport = getRequredDependency(injectionContext, HttpTransport.class);

		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
		Collection<String> scopes = Collections.singleton(GmailScopes.GMAIL_COMPOSE);

		try {
			GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory, clientId, clientSecret, scopes).setDataStoreFactory(dataStoreFactory).build();
			injectionContext.inject(flow).named("gmailAuthorizationCodeFlow").as(GoogleAuthorizationCodeFlow.class);
		} catch (IOException e) {
			throw new ConfigurationException(e, "Couldn't initialize GoogleAuthorizationCodeFlow");
		}
	}

	@Override
	public void start(UpdatableInjectionContext injectionContext) {
		super.start(injectionContext);
		addRoutes(injectionContext);
	}

	private void addRoutes(InjectionContext injectionContext) {
		Router router = injectionContext.get(Router.class);

		// optional config to change the base path of the gmail routes (to prevent clashes)
		String gmailAdminRootPath = getOptionalProperty(injectionContext, "gmailAdminRootPath", "/admin/gmail");

		router.get(String.format("%s/setup", gmailAdminRootPath), GmailAdminController.class, "setup", "gmail.admin.setup");
		router.get(String.format("%s/oauth2callback", gmailAdminRootPath), GmailAdminController.class, "oauthCallback", "gmail.admin.oauthCallback");
	}

	/**
	 * Get a required string property from the injection context.
	 *
	 * @param injectionContext the injection context to get the property from.
	 * @param propertyName the name of the property.
	 * @return
	 */
	private String getRequiredProperty(InjectionContext injectionContext, String propertyName) {
		String property = injectionContext.get(String.class, propertyName);
		if (StringUtils.isBlank(property)) {
			throw new ConfigurationException("Property `%s` not found. Did you forget to add it to application.properties?", propertyName);
		}
		return property;
	}

	/**
	 * Get an optional string property from the injection context.
	 *
	 * @param injectionContext the injection context to get the property from.
	 * @param propertyName the name of the property.
	 * @param defaultValue the default value to use if the property is not present.
	 * @return the property value, or the default value if not found.
	 */
	private String getOptionalProperty(InjectionContext injectionContext, String propertyName, String defaultValue) {
		String property = injectionContext.get(String.class, propertyName);
		if (StringUtils.isBlank(property)) {
			Logger.info("No value found for property: %s, using default value: %s", propertyName, defaultValue);
			property = defaultValue;
		}
		return property;
	}

	/**
	 * Get a required dependency from the injection context.
	 *
	 * @param injectionContext the injection context to get the depdency from.
	 * @param type the type of the dependency.
	 * @param <T> the type of the return type.
	 * @return the dependency.
	 */
	private <T> T getRequredDependency(InjectionContext injectionContext, Class<T> type) {
		T dependency = injectionContext.get(type);
		if (dependency == null) {
			throw new ConfigurationException("Required Dependency of type `%s` not found.", type.getName());
		}
		return dependency;
	}

}
