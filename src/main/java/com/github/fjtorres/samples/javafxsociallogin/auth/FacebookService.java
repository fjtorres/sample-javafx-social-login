package com.github.fjtorres.samples.javafxsociallogin.auth;

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.restfb.DefaultFacebookClient;
import com.restfb.Version;
import com.restfb.scope.FacebookPermissions;
import com.restfb.scope.ScopeBuilder;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

public class FacebookService implements AuthService {

    private static final String APP_ID = "5111783918918508";

    private final CustomLocalServerReceiver receiver;
    private final AuthorizationCodeInstalledApp.Browser browser;

    public FacebookService(CustomLocalServerReceiver receiver, AuthorizationCodeInstalledApp.Browser browser) {
        this.receiver = receiver;
        this.browser = browser;
    }

    public String authorize() throws IOException {
        DefaultFacebookClient facebookClient = new DefaultFacebookClient(Version.LATEST);
        ScopeBuilder scopes = new ScopeBuilder().addPermission(FacebookPermissions.EMAIL);

        try {

            String redirectUri = receiver.getRedirectUri();

            String loadUrl = facebookClient.getLoginDialogUrl(APP_ID, redirectUri, scopes);
            browser.browse(loadUrl + "&display=popup&response_type=code");

            return receiver.waitForCode();

        } finally {
            receiver.stop();
        }
    }
}
