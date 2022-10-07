import java.io.BufferedOutputStream;
import java.io.IOException;

/**
 * @author Stanislav Rakitov
 */
@FunctionalInterface
public interface Handler {
  void handle(Request request, BufferedOutputStream outputStream) throws IOException;
}
