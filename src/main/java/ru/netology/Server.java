package ru.netology;

import ru.netology.handler.Handler;
import ru.netology.request.Request;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final ExecutorService executorService;
    private final Map<String , Map<String, Handler>>handlers = new ConcurrentHashMap<>();
    //    private final static List<String> VALID_PATHS = List.of("/index.html",
//            "/spring.svg", "/spring.png", "/resources.html", "/styles.css",
//            "/app.js", "/links.html", "/forms.html", "/classic.html",
//            "/events.html", "/events.js");
    private final Handler notFoundHandler = ((request, out) -> {
        try {
            out.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n").getBytes());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    });

    public Server(int threadPoolSize) {
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
    }

    public void addHandler(String method, String path, Handler handler){
        if(handlers.get(method) == null){
            handlers.put(method, new ConcurrentHashMap<>());
        }
        handlers.get(method).put(path, handler);
    }

    public void launchServer(int portNumber) {
        try (final var serverSocket = new ServerSocket(portNumber)) {
            while (true) {
                final var clientSocket = serverSocket.accept();
                executorService.submit(() -> handleConnection(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleConnection(Socket socket) {
        try (
                socket;
                final var in = socket.getInputStream();
                final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {
            var request = Request.fromInputStream(in);
            final var path = request.getPath();

            if (handlers.get(request.getMethod()) == null){
                notFoundHandler.handle(request, out);
                return;
            }

            var handler = handlers.get(request.getMethod()).get(request.getPath());
            if (handler == null){
                notFoundHandler.handle(request, out);
                return;
            }
            handler.handle(request, out);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}