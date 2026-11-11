package com.doors.crawler.scan;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/** Serves canned HN HTML on an ephemeral port and counts requests, so tests can assert on refetching. */
public final class StubHnServer implements AutoCloseable {

    private static final byte[] EMPTY_PAGE = "<html><body></body></html>".getBytes(StandardCharsets.UTF_8);

    private final HttpServer http;
    private final AtomicInteger hits = new AtomicInteger();
    private volatile byte[] body = EMPTY_PAGE;
    private volatile int status = 200;

    public static StubHnServer start() throws IOException {
        StubHnServer server = new StubHnServer();
        server.http.start();
        return server;
    }

    private StubHnServer() throws IOException {
        http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        http.createContext("/", exchange -> {
            hits.incrementAndGet();
            int st = status;
            if (st >= 400) {
                exchange.sendResponseHeaders(st, -1);
            } else {
                byte[] b = body;
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, b.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(b);
                }
            }
            exchange.close();
        });
    }

    public String url() {
        return "http://127.0.0.1:" + http.getAddress().getPort() + "/";
    }

    public void serve(String html) {
        this.body = html.getBytes(StandardCharsets.UTF_8);
        this.status = 200;
    }

    public void failWith(int code) {
        this.status = code;
    }

    public int hits() {
        return hits.get();
    }

    public void reset() {
        hits.set(0);
        status = 200;
        body = EMPTY_PAGE;
    }

    @Override
    public void close() {
        http.stop(0);
    }
}
