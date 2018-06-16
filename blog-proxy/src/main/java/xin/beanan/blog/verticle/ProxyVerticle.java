package xin.beanan.blog.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.serviceproxy.ServiceBinder;
import xin.beanan.blog.service.IncrService;
import xin.beanan.blog.service.impl.IncrServiceImpl;

/**
 * @author BeanNan
 */
public class ProxyVerticle extends AbstractVerticle {
  
  private IncrService incrService;
  
  private void initData() {
    incrService = new IncrServiceImpl(vertx, config());
  }
  
  @Override
  public void start(Future<Void> startFuture) throws Exception {
    initData();
    new ServiceBinder(vertx)
        .setAddress("incr.article.readNum")
        .register(IncrService.class, incrService);
    startFuture.complete();
  }
}
