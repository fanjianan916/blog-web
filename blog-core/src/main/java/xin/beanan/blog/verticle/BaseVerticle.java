package xin.beanan.blog.verticle;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import java.util.function.Consumer;

/**
 * @author BeanNan
 */
public interface BaseVerticle {
  
  default <T> Handler<AsyncResult<T>> resultHandler(RoutingContext routingContext, Consumer<T> consumer) {
    return res -> {
      if (res.succeeded()) {
        consumer.accept(res.result());
      } else {
        serviceUnavailable(routingContext);
      }
    };
  }
  
  default void serviceUnavailable(RoutingContext routingContext) {
    routingContext.response().setStatusCode(503).end();
  }
  
  default void badRequest(RoutingContext routingContext) {
    routingContext.response().setStatusCode(404).end();
  }
}
