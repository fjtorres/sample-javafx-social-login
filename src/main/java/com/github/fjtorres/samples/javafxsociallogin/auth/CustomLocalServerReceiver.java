package com.github.fjtorres.samples.javafxsociallogin.auth;

import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.util.Throwables;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 * @see LocalServerReceiver
 */
public class CustomLocalServerReceiver implements VerificationCodeReceiver {

    private static final String LOCALHOST = "localhost";

    private static final String CALLBACK_PATH = "/Callback";

    /** Server or {@code null} before {@link #getRedirectUri()}. */
    private HttpServer server;

    /** Verification code or {@code null} for none. */
    String code;

    /** Error code or {@code null} for none. */
    String error;

    /** To block until receiving an authorization response or stop() is called. */
    final Semaphore waitUnlessSignaled = new Semaphore(0 /* initially zero permit */);

    /** Port to use or {@code -1} to select an unused port in {@link #getRedirectUri()}. */
    private int port;

    /** Host name to use. */
    private final String host;

    /** Callback path of redirect_uri. */
    private final String callbackPath;

    /**
     * URL to an HTML page to be shown (via redirect) after successful login. If null, a canned
     * default landing page will be shown (via direct response).
     */
    private String successLandingPageUrl;

    /**
     * URL to an HTML page to be shown (via redirect) after failed login. If null, a canned default
     * landing page will be shown (via direct response).
     */
    private String failureLandingPageUrl;

    /**
     * Constructor that starts the server on {@link #LOCALHOST} and an unused port.
     *
     * <p>Use {@link LocalServerReceiver.Builder} if you need to specify any of the optional parameters.
     */
    public CustomLocalServerReceiver() {
        this(LOCALHOST, -1, CALLBACK_PATH, null, null);
    }

    /**
     * Constructor.
     *
     * @param host Host name to use
     * @param port Port to use or {@code -1} to select an unused port
     */
    CustomLocalServerReceiver(
            String host, int port, String successLandingPageUrl, String failureLandingPageUrl) {
        this(host, port, CALLBACK_PATH, successLandingPageUrl, failureLandingPageUrl);
    }

    /**
     * Constructor.
     *
     * @param host Host name to use
     * @param port Port to use or {@code -1} to select an unused port
     */
    CustomLocalServerReceiver(
            String host,
            int port,
            String callbackPath,
            String successLandingPageUrl,
            String failureLandingPageUrl) {
        this.host = host;
        this.port = port;
        this.callbackPath = callbackPath;
        this.successLandingPageUrl = successLandingPageUrl;
        this.failureLandingPageUrl = failureLandingPageUrl;
    }

    @Override
    public String getRedirectUri() throws IOException {

        server = HttpServer.create(new InetSocketAddress(port != -1 ? port : findOpenPort()), 0);
        HttpContext context = server.createContext(callbackPath, new CustomLocalServerReceiver.CallbackHandler());
        server.setExecutor(null);

        try {
            server.start();
            port = server.getAddress().getPort();
        } catch (Exception e) {
            Throwables.propagateIfPossible(e);
            throw new IOException(e);
        }
        return "http://" + this.getHost() + ":" + port + callbackPath;
    }

    /*
     * Copied from Jetty findFreePort() as referenced by: https://gist.github.com/vorburger/3429822
     */
    private int findOpenPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("No free TCP/IP port to start embedded HTTP Server on");
        }
    }

    /**
     * Blocks until the server receives a login result, or the server is stopped by {@link #stop()},
     * to return an authorization code.
     *
     * @return authorization code if login succeeds; may return {@code null} if the server is stopped
     *     by {@link #stop()}
     * @throws IOException if the server receives an error code (through an HTTP request parameter
     *     {@code error})
     */
    @Override
    public String waitForCode() throws IOException {
        waitUnlessSignaled.acquireUninterruptibly();
        if (error != null) {
            throw new IOException("User authorization failed (" + error + ")");
        }
        return code;
    }

    @Override
    public void stop() throws IOException {
        waitUnlessSignaled.release();
        if (server != null) {
            try {
                server.stop(0);
            } catch (Exception e) {
                Throwables.propagateIfPossible(e);
                throw new IOException(e);
            }
            server = null;
        }
    }

    /** Returns the host name to use. */
    public String getHost() {
        return host;
    }

    /**
     * Returns the port to use or {@code -1} to select an unused port in {@link #getRedirectUri()}.
     */
    public int getPort() {
        return port;
    }

    /** Returns callback path used in redirect_uri. */
    public String getCallbackPath() {
        return callbackPath;
    }

    /**
     * Builder.
     *
     * <p>Implementation is not thread-safe.
     */
    public static final class Builder {

        /** Host name to use. */
        private String host = LOCALHOST;

        /** Port to use or {@code -1} to select an unused port. */
        private int port = -1;

        private String successLandingPageUrl;
        private String failureLandingPageUrl;

        private String callbackPath = CALLBACK_PATH;

        /** Builds the {@link CustomLocalServerReceiver}. */
        public CustomLocalServerReceiver build() {
            return new CustomLocalServerReceiver(
                    host, port, callbackPath, successLandingPageUrl, failureLandingPageUrl);
        }

        /** Returns the host name to use. */
        public String getHost() {
            return host;
        }

        /** Sets the host name to use. */
        public CustomLocalServerReceiver.Builder setHost(String host) {
            this.host = host;
            return this;
        }

        /** Returns the port to use or {@code -1} to select an unused port. */
        public int getPort() {
            return port;
        }

        /** Sets the port to use or {@code -1} to select an unused port. */
        public CustomLocalServerReceiver.Builder setPort(int port) {
            this.port = port;
            return this;
        }

        /** Returns the callback path of redirect_uri. */
        public String getCallbackPath() {
            return callbackPath;
        }

        /** Set the callback path of redirect_uri. */
        public CustomLocalServerReceiver.Builder setCallbackPath(String callbackPath) {
            this.callbackPath = callbackPath;
            return this;
        }

        public CustomLocalServerReceiver.Builder setLandingPages(String successLandingPageUrl, String failureLandingPageUrl) {
            this.successLandingPageUrl = successLandingPageUrl;
            this.failureLandingPageUrl = failureLandingPageUrl;
            return this;
        }
    }

    /**
     * HttpServer handler that takes the verifier token passed over from the OAuth provider and
     * stashes it where {@link #waitForCode} will find it.
     */
    class CallbackHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {

            if (!callbackPath.equals(httpExchange.getRequestURI().getPath())) {
                return;
            }

            StringBuilder body = new StringBuilder();

            try {
                Map<String, String> parms = this.queryToMap(httpExchange.getRequestURI().getQuery());
                error = parms.get("error");
                code = parms.get("code");

                Headers respHeaders = httpExchange.getResponseHeaders();
                if (error == null && successLandingPageUrl != null) {
                    respHeaders.add("Location", successLandingPageUrl);
                    httpExchange.sendResponseHeaders(HTTP_MOVED_TEMP, -1);
                } else if (error != null && failureLandingPageUrl != null) {
                    respHeaders.add("Location", failureLandingPageUrl);
                    httpExchange.sendResponseHeaders(HTTP_MOVED_TEMP, -1);
                } else {
                    writeLandingHtml(httpExchange, respHeaders);
                }
                httpExchange.close();
            } finally {
                waitUnlessSignaled.release();
            }
        }

        private Map<String, String> queryToMap(String query) {
            Map<String, String> result = new HashMap<String, String>();
            if (query != null) {
                for (String param : query.split("&")) {
                    String pair[] = param.split("=");
                    if (pair.length > 1) {
                        result.put(pair[0], pair[1]);
                    } else {
                        result.put(pair[0], "");
                    }
                }
            }
            return result;
        }

        private void writeLandingHtml(HttpExchange exchange, Headers headers) throws IOException {
            try (OutputStream os = exchange.getResponseBody()) {
                exchange.sendResponseHeaders(HTTP_OK, 0);
                headers.add("ContentType", "text/html");

                var html = getClass().getResource("/success.html");

                String content = """
                        <!DOCTYPE html>
                        <html lang="en">
                        <head>
                            <meta charset="UTF-8">
                            <title>Zerocopy - Success authentication</title>
                        </head>
                        <body>
                        Thank you to authenticate to Zerocopy desktop application. Please, close this window.
                        </body>
                        </html>
                    """;

                if (html != null) {
                    try {
                        content = Files.readString(Paths.get(html.toURI()));
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }

                OutputStreamWriter doc = new OutputStreamWriter(os, StandardCharsets.UTF_8);
                doc.write(content);
                doc.flush();
            }
        }
    }
}
