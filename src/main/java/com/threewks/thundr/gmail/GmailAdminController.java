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
import com.threewks.thundr.route.Router;
import com.threewks.thundr.view.redirect.RedirectView;
import com.threewks.thundr.view.string.StringView;

import java.io.IOException;

public class GmailAdminController {

    private final GoogleAuthorizationCodeFlow flow;
    private final String callbackUrl;

    public GmailAdminController(GoogleAuthorizationCodeFlow gmailAuthorizationCodeFlow, Router router, String host) {
        this.flow = gmailAuthorizationCodeFlow;
        this.callbackUrl = String.format("%s%s", host, router.getNamedRoute("gmail.admin.oauthCallback").getRoute());
    }

    public RedirectView setup() {
        String url = flow.newAuthorizationUrl()
                .setRedirectUri(callbackUrl)
                .setResponseTypes(Expressive.list("code"))
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();

        return new RedirectView(url);
    }

    public StringView oauthCallback(String code) throws IOException {
        GoogleTokenResponse tokenResponse = flow.newTokenRequest(code)
                .setRedirectUri(callbackUrl)
                .execute();

        flow.createAndStoreCredential(tokenResponse, GmailMailer.CREDENTIAL_USER_ID);

        return new StringView("Gmail setup complete");
    }

}
