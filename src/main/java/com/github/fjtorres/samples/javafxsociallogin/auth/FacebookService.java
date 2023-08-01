package com.github.fjtorres.samples.javafxsociallogin.auth;

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Version;
import com.restfb.scope.FacebookPermissions;
import com.restfb.scope.ScopeBuilder;

import java.io.IOException;

public class FacebookService implements AuthService {

    private static final String APP_ID = System.getenv("FACEBOOK_APP_ID");
    private static final String APP_SECRET = System.getenv("FACEBOOK_APP_SECRET");

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

            String code = receiver.waitForCode();

            FacebookClient.AccessToken token = facebookClient.obtainUserAccessToken(APP_ID, APP_SECRET, redirectUri, code);

            return token.getAccessToken();

        } finally {
            receiver.stop();
        }
    }
}
