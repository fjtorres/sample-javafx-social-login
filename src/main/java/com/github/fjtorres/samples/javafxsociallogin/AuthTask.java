package com.github.fjtorres.samples.javafxsociallogin;

import com.github.fjtorres.samples.javafxsociallogin.auth.AuthService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class AuthTask extends Service<String> {

    private final AuthService authService;

    public AuthTask(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected Task<String> createTask() {
        return new Task<>() {
            @Override
            protected String call() throws Exception {
                return authService.authorize();
            }
        };
    }
}
