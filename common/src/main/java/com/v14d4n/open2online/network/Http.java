package com.v14d4n.open2online.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The one HTTP client in the mod, shared by the update check and the external address lookup.
 *
 * <p>Both used to open their own {@link java.net.HttpURLConnection}, where the only way to hear that
 * a request went wrong is an exception out of {@code getInputStream}, and where a connect and a read
 * are timed separately — so the wait a caller thought it had asked for was really twice that. This
 * client hands back a response to look at and holds a caller for one stated deadline, whatever part
 * of the exchange is being slow. It is built once, so the lookups share a thread and a pool rather
 * than standing one up per request.
 */
@Environment(EnvType.CLIENT)
public final class Http {
    private static final Logger LOGGER = LoggerFactory.getLogger("Open2Online");

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            // HttpURLConnection followed redirects on its own and this client does not. Nothing we
            // ask for moves today, but both addresses belong to somebody else to rearrange.
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private Http() {
    }

    /**
     * The body of a GET, empty if anything at all went wrong: nowhere to connect, no answer inside
     * {@code timeout}, or an answer that was not a success.
     *
     * <p>Blocking, so it belongs on a worker thread.
     */
    public static Optional<String> get(String uri, Duration timeout) {
        return getAsync(uri, timeout).join();
    }

    /**
     * The same request without the wait, for callers with more than one to make. Every way of
     * failing is already folded into an empty answer, so the future only ever completes normally.
     */
    public static CompletableFuture<Optional<String>> getAsync(String uri, Duration timeout) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                // Bounds the wait for a response to arrive, and hands the client a reason to drop
                // the exchange when none does.
                .timeout(timeout)
                .GET()
                .build();

        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                // The one above stops counting the moment the response arrives — the body that
                // follows it is not covered at all. Measured against a server that sent its headers
                // at once and then dribbled the body: thirty-two seconds against a three second
                // setting. So the deadline the caller was promised is this one.
                //
                // What it frees is the caller, not the socket: the exchange goes on draining in the
                // background until the far end is done. Asking the client to cancel it changes
                // nothing, measured the same way.
                .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .handle((response, failure) -> read(uri, response, failure));
    }

    private static Optional<String> read(String uri, HttpResponse<String> response, Throwable failure) {
        if (failure != null) {
            logFailure(uri, failure);
            return Optional.empty();
        }

        if (response.statusCode() / 100 != 2) {
            LOGGER.warn("{} answered with status {}", uri, response.statusCode());
            return Optional.empty();
        }

        return Optional.of(response.body());
    }

    /**
     * A timeout gets one line: its stack trace is the scheduler that fired it and names nothing in
     * this mod. Everything else keeps the trace, because that is where the reason is.
     */
    private static void logFailure(String uri, Throwable failure) {
        // Dependent stages see failures wrapped, so the interesting one is a layer down.
        Throwable cause = failure instanceof CompletionException && failure.getCause() != null
                ? failure.getCause()
                : failure;

        if (cause instanceof TimeoutException || cause instanceof HttpTimeoutException) {
            LOGGER.warn("{} did not answer in time", uri);
            return;
        }

        LOGGER.warn("Request to {} failed", uri, cause);
    }
}
