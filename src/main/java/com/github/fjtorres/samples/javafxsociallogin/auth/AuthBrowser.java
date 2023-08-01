package com.github.fjtorres.samples.javafxsociallogin.auth;

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;

public class AuthBrowser implements AuthorizationCodeInstalledApp.Browser {

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

            final WebView browser = new WebView();
            final WebEngine webEngine = browser.getEngine();
            webEngine.load(url);

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
