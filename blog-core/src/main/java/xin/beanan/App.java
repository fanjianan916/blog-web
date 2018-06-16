package xin.beanan;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.ParseException;


/**
 * Hello world!
 */
public class App {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
  
  public static void main(String[] args) throws ParseException {
    Vertx vertx = Vertx.vertx();
    FileSystem fileSystem = vertx.fileSystem();
    fileSystem.readFile(App.class.getClassLoader().getResource("config.json").getPath(), res -> {
      Buffer result = res.result();
      JsonObject jsonObject = result.toJsonObject();
      System.out.println(jsonObject);
    });
  }
}
