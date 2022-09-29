import java.io.BufferedOutputStream;

/**
 * @author Stanislav Rakitov
 */
@FunctionalInterface
public interface Handler {
  void handle(Request request, BufferedOutputStream outputStream);
}
