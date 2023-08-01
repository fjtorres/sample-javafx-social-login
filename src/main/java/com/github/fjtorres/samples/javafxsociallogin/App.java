package com.github.fjtorres.samples.javafxsociallogin;

import com.github.fjtorres.samples.javafxsociallogin.auth.AuthBrowser;
import com.github.fjtorres.samples.javafxsociallogin.auth.AuthService;
import com.github.fjtorres.samples.javafxsociallogin.auth.CustomLocalServerReceiver;
import com.github.fjtorres.samples.javafxsociallogin.auth.FacebookService;
import com.github.fjtorres.samples.javafxsociallogin.auth.GoogleService;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.Observable;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.Supplier;

public class App extends Application {

    /** Port in the "Callback URL". */
    public static final int PORT = 8080;

    /** Domain name in the "Callback URL". */
    public static final String DOMAIN = "localhost";
    private static final int TIMEOUT = 100;

    // UX components
    private final Button logInWithGoogle = new Button("Log in with Google");
    private final Button logInWithFacebook = new Button("Log in with Facebook");
    private final Text text = new Text();

    // GUI properties
    private final SimpleStringProperty loginType = new SimpleStringProperty();
    private final SimpleBooleanProperty loading = new SimpleBooleanProperty(false);

    private final Timeline browserListener = new Timeline(new KeyFrame(Duration.seconds(TIMEOUT), e -> {
        loading.set(false);
        text.setText("Timeout during log in with " + loginType.get());
    }), new KeyFrame(Duration.seconds(TIMEOUT + 5), e -> text.setText("")));

    // Other fields


    @Override
    public void start(Stage primaryStage) {

        // Configuration
        text.wrappingWidthProperty().bind(primaryStage.widthProperty());
        logInWithGoogle.setOnAction(this::onGoogleLogin);
        logInWithFacebook.setOnAction(this::onFacebookLogin);
        loading.addListener(this::onLoadingChangeListener);

        // Load UX
        HBox buttons = new HBox();
        buttons.getChildren().addAll(logInWithGoogle, logInWithFacebook);
        HBox.setMargin(logInWithFacebook, new Insets(0, 0, 0, 10));
        VBox root = new VBox();
        root.getChildren().addAll(buttons, text);
        VBox.setMargin(buttons, new Insets(20, 0, 20, 0));
        root.setAlignment(Pos.TOP_CENTER);

        // Create and open "window"
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void onLoadingChangeListener(Observable observable, Boolean oldValue, Boolean newValue) {
        logInWithGoogle.setDisable(newValue);
        logInWithFacebook.setDisable(newValue);

        if (Boolean.TRUE.equals(newValue)) {
            browserListener.play();
        } else {
            browserListener.stop();
        }
    }

    private void onFacebookLogin(ActionEvent event) {
        onLogin("Facebook");
    }

    private void onGoogleLogin(ActionEvent event) {
        onLogin("Google");
    }

    private void onLogin(String type) {

        final CustomLocalServerReceiver receiver = new CustomLocalServerReceiver.Builder()
                .setHost(DOMAIN)
                .setPort(PORT)
                .build();

        // FIXME Change default browser or embedded browser
        final AuthorizationCodeInstalledApp.Browser browser = new AuthBrowser(e -> loading.set(false));/*new AuthorizationCodeInstalledApp.DefaultBrowser();*/

        AuthService service = switch (type) {
            case "Google" -> new GoogleService(receiver, browser);
            case "Facebook" -> new FacebookService(receiver, browser);
            default -> throw new UnsupportedOperationException("Social type is not supported.");
        };

        System.out.println("Trying to log in with " + type);

        Supplier<Void> commonSteps = () -> {
            loading.set(false);

            if (browser instanceof AuthBrowser) {
                ((AuthBrowser) browser).close();
            }

            try {
                receiver.stop();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return null;
        };

        loginType.set(type);
        AuthTask task = new AuthTask(service);
        task.setOnFailed(e -> {
            text.setText("Token cannot be loaded with " + loginType.get());
            e.getSource().getException().printStackTrace();
            commonSteps.get();
        });
        task.setOnSucceeded(e -> {
            text.setText((String)e.getSource().getValue());
            commonSteps.get();
        });
        task.setOnRunning(e -> {
            loading.set(true);
            text.setText("Loading new token with " + loginType.get());
        });
        task.setOnCancelled(e -> {
            commonSteps.get();
        });

        task.start();
    }
}
