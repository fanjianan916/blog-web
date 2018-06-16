package xin.beanan.blog.util;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;

import java.util.HashSet;
import java.util.Set;

/**
 * @author BeanNan
 */
public class RouterUtils {
  
  private RouterUtils() {}
  
  private static Vertx vertx;
  private static Router router;
  
  public static Vertx getVertx() {
    return vertx;
  }
  
  public static void setVertx(Vertx vertx) {
    if (RouterUtils.vertx == null && vertx != null) {
      RouterUtils.vertx = vertx;
    }
  }
  
  public static Router getRouter() {
    return router;
  }
  
  public static void setRouter(Router router) {
    if (RouterUtils.router == null && router != null) {
      RouterUtils.router = router;
    }
  }
  
  public static Router initRouter(Vertx vertx) {
    setVertx(vertx);
    router = Router.router(vertx);
    Set<String> allowHeaders = new HashSet<>();
    allowHeaders.add("x-requested-with");
    allowHeaders.add("Access-Control-Allow-Origin");
    allowHeaders.add("origin");
    allowHeaders.add("Content-Type");
    allowHeaders.add("accept");
    Set<HttpMethod> allowMethods = new HashSet<>();
    allowMethods.add(HttpMethod.GET);
    allowMethods.add(HttpMethod.POST);
    allowMethods.add(HttpMethod.DELETE);
    allowMethods.add(HttpMethod.PATCH);
  
    router.route().handler(CorsHandler.create("*")
        .allowedHeaders(allowHeaders)
        .allowedMethods(allowMethods));
    router.route().handler(BodyHandler.create());
    return router;
  }
  
  
}
