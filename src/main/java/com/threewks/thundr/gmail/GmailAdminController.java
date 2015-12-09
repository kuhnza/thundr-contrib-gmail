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

        return new StringView("Setup complete!");
    }

}
