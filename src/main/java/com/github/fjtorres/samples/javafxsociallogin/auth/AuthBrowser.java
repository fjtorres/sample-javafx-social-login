package com.github.fjtorres.samples.javafxsociallogin.auth;

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;

public class AuthBrowser implements AuthorizationCodeInstalledApp.Browser {

    private static final CookieManager COOKIE_MANAGER = new CookieManager();

    private final EventHandler<WindowEvent> onClose;
    private Stage stage;

    public AuthBrowser(EventHandler<WindowEvent> onClose) {
        this.onClose = onClose;
    }

    @Override
    public void browse(String url) throws IOException {

        Platform.runLater(() -> {
            close();

            stage = new Stage();
            stage.setOnCloseRequest(onClose);

            if (CookieHandler.getDefault() == null) {
                CookieHandler.setDefault(COOKIE_MANAGER);
            }

            COOKIE_MANAGER.getCookieStore().removeAll();

            final WebView browser = new WebView();
            browser.getEngine().load(url);

            Scene scene = new Scene(browser);
            stage.setResizable(false);
            stage.setAlwaysOnTop(true);
            stage.setScene(scene);
            stage.show();
        });

    }

    public void close() {
        if (stage != null) {
            stage.close();
        }
    }
}
