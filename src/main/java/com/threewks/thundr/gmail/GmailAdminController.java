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

import com.atomicleopard.expressive.Expressive;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.threewks.thundr.logger.Logger;
import com.threewks.thundr.route.Router;
import com.threewks.thundr.view.redirect.RedirectView;
import com.threewks.thundr.view.string.StringView;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class GmailAdminController {

	private final GoogleAuthorizationCodeFlow flow;
	private final String callbackUrl;

	public GmailAdminController(GoogleAuthorizationCodeFlow gmailAuthorizationCodeFlow, Router router, String host) {
		this.flow = gmailAuthorizationCodeFlow;
		this.callbackUrl = String.format("%s%s", host, router.getNamedRoute("gmail.admin.oauthCallback").getRoute());
	}

	/**
	 * Starts the OAuth process and redirects user to googles permission page with access type offline and force approval prompt
	 *
	 * @param credentialId the id that will be saved on the {@link com.google.api.client.auth.oauth2.StoredCredential} object. If not provided we
	 *                     default the id to {@link GmailMailer#CREDENTIAL_USER_ID}. To handle multiple credentials to google you should pass
	 *                     in unique credentialIds.
	 * @return the {@link RedirectView} which is page that prompts you for google account details.
	 */
	public RedirectView setup(String credentialId) {
		String callBackUrlWithParams = addQueryParamCredential(callbackUrl, credentialId);
		String url = flow.newAuthorizationUrl()
				.setRedirectUri(callBackUrlWithParams)
				.setResponseTypes(Expressive.list("code"))
				.setAccessType("offline")
				.setApprovalPrompt("force")
				.build();

		return new RedirectView(url);
	}

	private String addQueryParamCredential(String callbackUrl, String credentialId) {
		return StringUtils.isNotBlank(credentialId) ? String.format("%s?credentialId=%s", callbackUrl, credentialId) :
				callbackUrl;
	}

	/**
	 * Endpoint for google to call back to once authentication has been verified.
	 * This requires the callback to be defined in Google API Console under the Authorized redirect URIs
	 * otherwise google will not be able to redirect back this endpoint
	 * <p>
	 * If using credentialId you will need to make sure your callbackUrl and credentialId=paramValue is included.
	 * otherwise it won't match.
	 *
	 * @param code
	 * @param credentialId
	 * @return
	 * @throws IOException
	 */
	public StringView oauthCallback(String code, String credentialId) throws IOException {
		Logger.info("credentialId %s and code %s", credentialId, code);

		String callBackUrlWithCredentialId = StringUtils.isNotBlank(credentialId) ?
				addQueryParamCredential(callbackUrl, credentialId) :
				callbackUrl;

		GoogleTokenResponse tokenResponse = flow.newTokenRequest(code)
				.setRedirectUri(callBackUrlWithCredentialId)
				.execute();

		credentialId = StringUtils.lowerCase(StringUtils.isBlank(credentialId) ?
				GmailMailer.CREDENTIAL_USER_ID : credentialId);

		flow.createAndStoreCredential(tokenResponse, credentialId);

		return new StringView("Gmail setup complete");
	}

}
