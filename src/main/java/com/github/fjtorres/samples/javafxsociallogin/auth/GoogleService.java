package com.github.fjtorres.samples.javafxsociallogin.auth;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;

import java.io.IOException;
import java.util.Arrays;

public class GoogleService implements AuthService {

    /** Global instance of the HTTP transport. */
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /** Global instance of the JSON factory. */
    static final JsonFactory JSON_FACTORY = new GsonFactory();

    private static final String APP_ID = System.getenv("GOOGLE_APP_ID");
    private static final String APP_SECRET = System.getenv("GOOGLE_APP_SECRET");

    private final CustomLocalServerReceiver receiver;
    private final AuthorizationCodeInstalledApp.Browser browser;

    public GoogleService(CustomLocalServerReceiver receiver, AuthorizationCodeInstalledApp.Browser browser) {
        this.receiver = receiver;
        this.browser = browser;
    }

    public String authorize() throws IOException {

        // set up authorization code flow
        AuthorizationCodeFlow flow =
                new AuthorizationCodeFlow.Builder(
                        BearerToken.authorizationHeaderAccessMethod(),
                        HTTP_TRANSPORT,
                        JSON_FACTORY,
                        new GenericUrl("https://oauth2.googleapis.com/token"),
                        new ClientParametersAuthentication(APP_ID, APP_SECRET),
                        APP_ID,
                        "https://accounts.google.com/o/oauth2/v2/auth")
                        .setScopes(Arrays.asList("https://www.googleapis.com/auth/userinfo.email"))
                        .build();
        return new AuthorizationCodeInstalledApp(flow, receiver, browser).authorize("user").getAccessToken();
    }
}
