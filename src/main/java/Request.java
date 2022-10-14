import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents HTTP request received from a server.
 *
 * @author Stanislav Rakitov
 * @version 1.2
 */
public class Request {

  private final String method;
  private final String path;
  private final List<String> headers;
//  private byte[] body;

  public static final String GET = "GET";
  public static final String POST = "POST";
  private List<NameValuePair> params;


  public Request(String requestMethod, String requestPath) {
    this.method = requestMethod;
    this.path = requestPath;
    headers = null;
  }

  public Request(String method, String path, List<String> headers, List<NameValuePair> params) {
    this.method = method;
    this.path = path;
    this.headers = headers;
  }

  public String getMethod() {
    return method;
  }

  public String getPath() {
    return path;
  }

  static Request createRequest(BufferedInputStream in) throws IOException, URISyntaxException {
    final List<String> allowedMethods = List.of(GET, POST);

    final var limit = 4096;
    in.mark(limit);
    final var buffer = new byte[limit];
    final var read = in.read(buffer);

    // ищем request line
    final var requestLineDelimiter = new byte[]{'\r', '\n'};
    final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
    if (requestLineEnd == -1) {
      return null;
    }

    // читаем request line
    final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
    if (requestLine.length != 3) {
      return null;
    }

    final var method = requestLine[0];
    if (!allowedMethods.contains(method)) {
      return null;
    }
    System.out.println("METHOD: " + method);

    final var path = requestLine[1];
    System.out.println("PATH: " + path);

    // ищем заголовки
    final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
    final var headersStart = requestLineEnd + requestLineDelimiter.length;
    final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
    if (headersEnd == -1) {
      return null;
    }

    // отматываем на начало буфера
    in.reset();
    // пропускаем requestLine
    in.skip(headersStart);

    final var headersBytes = in.readNBytes(headersEnd - headersStart);
    List<String> headers = Arrays.asList(new String(headersBytes).split("\r\n"));
//    System.out.println(headers);

    List<NameValuePair> params = URLEncodedUtils.parse(new URI(path), StandardCharsets.UTF_8);
    System.out.println(params);

    return new Request(method, path, headers, params);
  }

  // from Google guava with modifications
  private static int indexOf(byte[] array, byte[] target, int start, int max) {
    outer:
    for (int i = start; i < max - target.length + 1; i++) {
      for (int j = 0; j < target.length; j++) {
        if (array[i + j] != target[j]) {
          continue outer;
        }
      }
      return i;
    }
    return -1;
  }

  public List<NameValuePair> getQueryParam(String name) {
    return params.stream().filter(p -> name.equals(p.getName())).collect(Collectors.toList());
  }

  public List<NameValuePair> getQueryParams() {
    return params;
  }


}
