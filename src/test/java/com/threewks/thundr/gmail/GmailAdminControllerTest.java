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
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.services.gmail.GmailScopes;
import com.threewks.thundr.route.Router;
import com.threewks.thundr.view.redirect.RedirectView;
import com.threewks.thundr.view.string.StringView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GmailAdminControllerTest {

	@Mock private DataStoreFactory dataStoreFactory;
	@Mock private HttpTransport httpTransport;

	private Router router;

	private GmailAdminController controller;

	@Before
	public void before() throws IOException {

		router = new Router();
		router.get("/admin/gmail/setup/oauth2callback", GmailAdminController.class, "oauthCallback", "gmail.admin.oauthCallback");

		Collection<String> scopes = Collections.singleton(GmailScopes.GMAIL_COMPOSE);
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow
				.Builder(httpTransport, JacksonFactory.getDefaultInstance(), "clientId", "clientSecret", scopes)
				.setDataStoreFactory(dataStoreFactory)
				.build();

		controller = new GmailAdminController(flow, router, "https://gradresearchforms.apps.monash.edu");
	}

	@Test
	public void shouldSetupAuthorisationUrlAndRedirect() {
		RedirectView view = controller.setup("law");
		assertThat(view.getRedirect(), is("https://accounts.google.com/o/oauth2/auth?access_type=offline&approval_prompt=force&client_id=clientId&redirect_uri=https://gradresearchforms.apps.monash.edu/admin/gmail/setup/oauth2callback?credentialId%3Dlaw&response_type=code&scope=https://www.googleapis.com/auth/gmail.compose"));

		view = controller.setup(null);
		assertThat(view.getRedirect(), is("https://accounts.google.com/o/oauth2/auth?access_type=offline&approval_prompt=force&client_id=clientId&redirect_uri=https://gradresearchforms.apps.monash.edu/admin/gmail/setup/oauth2callback&response_type=code&scope=https://www.googleapis.com/auth/gmail.compose"));
	}

	@Test
	public void shouldHandleOAuthCallback() throws IOException {

		// setup some mocks so we can test without worrying about persistence
		GoogleAuthorizationCodeFlow flow = mock(GoogleAuthorizationCodeFlow.class);
		GoogleAuthorizationCodeTokenRequest tokenRequest = mock(GoogleAuthorizationCodeTokenRequest.class);
		GoogleTokenResponse tokenResponse = mock(GoogleTokenResponse.class);

		when(flow.newTokenRequest(anyString())).thenReturn(tokenRequest);
		when(tokenRequest.setRedirectUri(anyString())).thenReturn(tokenRequest);
		when(tokenRequest.execute()).thenReturn(tokenResponse);

		controller = new GmailAdminController(flow, router, "https://monash-scholarship-form-dev.appspot.com");

		StringView view = controller.oauthCallback("12345", null);
		assertThat(view.content(), is("Gmail setup complete"));

		verify(flow).newTokenRequest("12345");
		verify(tokenRequest).setRedirectUri("https://monash-scholarship-form-dev.appspot.com/admin/gmail/setup/oauth2callback");
		verify(tokenRequest).execute();
		verify(flow).createAndStoreCredential(tokenResponse, "gmail-credentials");


		view = controller.oauthCallback("12345", "monash.inbox@monash.edu.au");
		assertThat(view.content(), is("Gmail setup complete"));

		verify(flow, times(2)).newTokenRequest("12345");
		verify(tokenRequest, times(1)).setRedirectUri("https://monash-scholarship-form-dev.appspot.com/admin/gmail/setup/oauth2callback?credentialId=monash.inbox@monash.edu.au");
		verify(tokenRequest, times(2)).execute();
		verify(flow, times(1)).createAndStoreCredential(tokenResponse, "monash.inbox@monash.edu.au");


	}

}
