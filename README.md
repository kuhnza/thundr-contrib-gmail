thundr-contrib-gmail [![Build Status](https://travis-ci.org/3wks/thundr-contrib-gmail.svg)](https://travis-ci.org/3wks/thundr-contrib-gmail)
=======================

A thundr module for integrating with the Gmail API to send email.


Overview
--------

This module allows you to send emails using the official Gmail API from Google while using a standard thundr `com.threewks.thundr.mail.Mailer`.


Setup
-----

To install the module include the dependency in your ApplicationModule. For example:

    @Override
    public void requires(DependencyRegistry dependencyRegistry) {
        super.requires(dependencyRegistry);
        dependencyRegistry.addDependency(GmailModule.class);
    }

This will do the following:

- inject a `com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow` named `gmailAuthorizationCodeFlow` used by the Gmail client. 
- add the following routes
  - `/admin/gmail/setup`
  - `/admin/gmail/setup/oauth2callback`

You will also need to create a Google client id for your web application in order to access the Gmail API. This can be done from the 'API Manager' section of the [Google Developers Console](https://console.developers.google.com).

Note: be sure to configure the redirect URIs and Javascript origins and to configure the consent screen with the required information.


Configuration
-------------

The following **mandatory** configuration options must be set in your application.properties file:

- `host` - a complete URL to the host (eg: http://www.example.com). This is used to construct the OAuth callback URL.
- `gmailOAuthClientId` - the client id (from the Google Developers Console)
- `gmailOAuthClientSecret` - the client secret (from the Google Developer Console)

The following **mandatory** dependencies must be injected into the InjectionContext:

- `com.google.api.client.util.store.DataStoreFactory` - used by the Gmail API to persist the OAuth credentials (eg: for AppEngine use `com.google.api.client.extensions.appengine.datastore.AppEngineDataStoreFactory`).
- `com.google.api.client.http.HttpTransport` - used to communicate with the Gmail API (eg: for AppEngine use `com.google.api.client.extensions.appengine.http.UrlFetchTransport`).

The following **optional** configuration options can be set in your application.properties file:

- `gmailAdminRootPath` - you can optionally override the root path to the gmail admin routes. By default the root is /admin/gmail


Authorising Access
------------------

Before you can use the module you must first authorise access to the Gmail account via OAuth. To do this run your application and request the following route:

- `/admin/gmail/setup`

This will redirect you to Google where you must login and authorise access to your Gmail account. Once complete you will be directed back
to your application and shown a success message. The OAuth credentials are stored using the DataStoreFactory (eg: for AppEngine they will be stored in the 'StoredCredential' entity in the datastore).


Usage
-----

GmailMailer is a standard thundr `com.threewks.thundr.mail.Mailer`.

eg:

    mailer
        .mail()
        .from("from@email.com")
        .to("to@email.com")
        .subject("this is the subject")
        .body(new StringView("this is the message"))
        .send();
    
    
