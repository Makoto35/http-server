package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        final Server server = new Server(9999);
        server.start();
    }
}

class Server {
    private final int port;
    private final List<String> validPaths;
    private final ExecutorService threadPool;

    public Server(int port) {
        this.port = port;
        this.validPaths = List.of(
                "/index.html", "/spring.svg", "/spring.png", "/resources.html",
                "/styles.css", "/app.js", "/links.html", "/forms.html",
                "/classic.html", "/events.html", "/events.js", "/messages"
        );
        this.threadPool = Executors.newFixedThreadPool(64);
    }

    public void start() {
        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                final var socket = serverSocket.accept();
                threadPool.submit(() -> processConnection(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    private void processConnection(Socket socket) {
        try (final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final var out = new BufferedOutputStream(socket.getOutputStream())) {

            // Парсим запрос
            final var requestLine = in.readLine();
            if (requestLine == null) return;

            final Request request = new Request(requestLine);
            final String cleanPath = request.getPath();

            // Логирование для демонстрации работы
            System.out.println("Path: " + cleanPath);
            System.out.println("Params: " + request.getQueryParams());

            // Обработка валидных путей (игнорируя query-строку)
            if (!validPaths.contains(cleanPath)) {
                sendNotFound(out);
                return;
            }

            // Специальная обработка для /messages
            if ("/messages".equals(cleanPath)) {
                handleMessages(request, out);
                return;
            }

            // Стандартная обработка файлов
            final var filePath = Path.of(".", "public", cleanPath);
            final var mimeType = Files.probeContentType(filePath);

            if ("/classic.html".equals(cleanPath)) {
                handleClassicTemplate(filePath, out, mimeType);
            } else {
                sendFile(filePath, out, mimeType);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Новый обработчик для /messages
    private void handleMessages(Request request, BufferedOutputStream out) throws IOException {
        final var lastParam = request.getQueryParam("last");
        int count = 10; // значение по умолчанию

        if (!lastParam.isEmpty()) {
            try {
                count = Integer.parseInt(lastParam.get(0));
            } catch (NumberFormatException e) {
                System.err.println("Invalid 'last' parameter: " + lastParam);
            }
        }

        final String response = "Last " + count + " messages";
        sendTextResponse(out, response);
    }

    private void sendTextResponse(BufferedOutputStream out, String text) throws IOException {
        final var content = text.getBytes();
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Content-Length: " + content.length + "\r\n" +
                        "Connection: close\r\n\r\n"
        ).getBytes());
        out.write(content);
        out.flush();
    }

    private void sendNotFound(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n\r\n"
        ).getBytes());
        out.flush();
    }

    private void handleClassicTemplate(Path filePath,
                                       BufferedOutputStream out,
                                       String mimeType) throws IOException {
        final var template = Files.readString(filePath);
        final var content = template.replace(
                "{time}",
                LocalDateTime.now().toString()
        ).getBytes();

        sendResponse(out, mimeType, content);
    }

    private void sendFile(Path filePath,
                          BufferedOutputStream out,
                          String mimeType) throws IOException {
        final var length = Files.size(filePath);
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n\r\n"
        ).getBytes());
        Files.copy(filePath, out);
        out.flush();
    }

    private void sendResponse(BufferedOutputStream out,
                              String mimeType,
                              byte[] content) throws IOException {
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + content.length + "\r\n" +
                        "Connection: close\r\n\r\n"
        ).getBytes());
        out.write(content);
        out.flush();
    }
}