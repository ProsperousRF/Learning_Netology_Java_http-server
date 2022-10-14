import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * simple http Server
 *
 * @author Stanislav Rakitov
 * @version 1.2
 */
public class Server {

  private final int SERVER_SOCKET;
  private final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png",
                                                  "/resources.html",
                                                  "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
  private final ExecutorService executorService;
  private final ConcurrentHashMap<String, Map<String, Handler>> handlers;


  public Server(int serverSocket, int poolSize) {
    SERVER_SOCKET = serverSocket;
    executorService = Executors.newFixedThreadPool(poolSize);
    handlers = new ConcurrentHashMap<>();
  }

  void start() {
    try (final var serverSocket = new ServerSocket(SERVER_SOCKET)) {
      while (!serverSocket.isClosed()) {
        Socket socket = serverSocket.accept();
        executorService.execute(() -> proceedConnection(socket));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      executorService.shutdown();
    }
  }

  private void proceedConnection(Socket socket) {
    try (final var in = new BufferedInputStream(socket.getInputStream());
         final var out = new BufferedOutputStream(socket.getOutputStream())
    ) {

      Request request = Request.createRequest(in);

      // Check for bad requests and drop connection
      if (request == null || !handlers.containsKey(request.getMethod())) {
        responseWithoutContent(out, "400", "Bad Request");
        return;
      }

      // Get PATH, HANDLER Map
      Map<String, Handler> handlerMap = handlers.get(request.getMethod());
      String requestPath = request.getPath().split("\\?")[0];
      if (handlerMap.containsKey(requestPath)) {
        Handler handler = handlerMap.get(requestPath);
        handler.handle(request, out);
      } else {  // Defaults
        // Resource not found
        if (!validPaths.contains(requestPath)) {
          responseWithoutContent(out, "404", "Not Found");
        } else {
          defaultHandler(out, requestPath);
        }
      }

    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  void defaultHandler(BufferedOutputStream out, String path) throws IOException {
    final var filePath = Path.of(".", "public", path);
    final var mimeType = Files.probeContentType(filePath);

    // special case for classic
    if (path.startsWith("/classic.html")) {
      final var template = Files.readString(filePath);
      final var content = template.replace(
              "{time}",
              LocalDateTime.now().toString()
      ).getBytes();
      out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
      out.write(content);
      out.flush();
      return;
    }

    final var length = Files.size(filePath);
    out.write((
                      "HTTP/1.1 200 OK\r\n" +
                              "Content-Type: " + mimeType + "\r\n" +
                              "Content-Length: " + length + "\r\n" +
                              "Connection: close\r\n" +
                              "\r\n"
              ).getBytes());
    Files.copy(filePath, out);
    out.flush();
  }

  void addHandler(String method, String path, Handler handler) {
    if (!handlers.containsKey(method)) {
      handlers.put(method, new HashMap<>());
    }
    handlers.get(method).put(path, handler);
  }

  void responseWithoutContent(BufferedOutputStream out, String responseCode, String responseStatus) throws IOException {
    out.write((
                      "HTTP/1.1 " + responseCode + " " + responseStatus + "\r\n" +
                              "Content-Length: 0\r\n" +
                              "Connection: close\r\n" +
                              "\r\n"
              ).getBytes());
    out.flush();
  }

}


