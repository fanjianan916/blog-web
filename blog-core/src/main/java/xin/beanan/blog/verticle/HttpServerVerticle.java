package xin.beanan.blog.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import xin.beanan.blog.util.RouterUtils;

/**
 * @author BeanNan
 */
public class HttpServerVerticle extends AbstractVerticle {
  
  @Override
  public void start(Future<Void> startFuture) throws Exception {
    vertx.createHttpServer()
        .requestHandler(RouterUtils.getRouter()::accept)
        .listen(config().getInteger("http_port"), res -> {
          if (res.succeeded()) {
            startFuture.complete();
          } else {
            startFuture.fail("server startup error");
          }
        });
  }
}
