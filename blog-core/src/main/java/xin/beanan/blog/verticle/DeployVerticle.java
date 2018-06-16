package xin.beanan.blog.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import xin.beanan.blog.util.RouterUtils;

/**
 * @author BeanNan
 */
public class DeployVerticle extends AbstractVerticle {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(DeployVerticle.class);
  
  @Override
  public void start(Future<Void> startFuture) throws Exception {
    
    RouterUtils.initRouter(vertx);
    
    TagVerticle tagVerticle = new TagVerticle();
    CategoryVerticle categoryVerticle = new CategoryVerticle();
    ArticleVerticle articleVerticle = new ArticleVerticle();
    
    DeploymentOptions serviceDeployOption = new DeploymentOptions();
    serviceDeployOption.setConfig(config());
    
    Future<Boolean> tagFuture = Future.future();
    Future<Boolean> serverFuture = Future.future();
    Future<Boolean> categoryFuture = Future.future();
    Future<Boolean> articleFuture = Future.future();
  
    vertx.deployVerticle(tagVerticle, serviceDeployOption, res -> {
      if (res.succeeded()) {
        tagFuture.complete(true);
        LOGGER.info("tagVerticle deploy success");
      } else {
        tagFuture.fail(res.cause());
        LOGGER.error("tagVerticle deploy failed");
      }
    });
    
    vertx.deployVerticle(categoryVerticle, serviceDeployOption, res -> {
      if (res.succeeded()) {
        categoryFuture.complete(true);
        LOGGER.info("categoryVerticle delpoy success");
      } else {
        categoryFuture.fail(res.cause());
        LOGGER.error("categoryVerticle deploy failed");
      }
    });
    
    vertx.deployVerticle(articleVerticle, serviceDeployOption,  res -> {
      if (res.succeeded()) {
        articleFuture.complete(true);
        LOGGER.info("articleVerticle deploy success");
      } else {
        articleFuture.fail(res.cause());
        LOGGER.error("articleArticel deploy failed");
      }
    });
    
    DeploymentOptions httpServerDeployOption = new DeploymentOptions();
    httpServerDeployOption.setConfig(config());
    httpServerDeployOption.setInstances(10);
    vertx.deployVerticle("xin.beanan.blog.verticle.HttpServerVerticle",
        httpServerDeployOption, res -> {
          if (res.succeeded()) {
            serverFuture.complete(true);
            LOGGER.info("httpserver most eventloop deploy success");
          } else {
            serverFuture.fail(res.cause());
            LOGGER.error("httpserver most eventloop deploy error");
          }
        });
    
    CompositeFuture.all(tagFuture, categoryFuture, articleFuture, serverFuture).setHandler(res -> {
      if (res.succeeded()) {
        startFuture.complete();
      } else {
        startFuture.fail(res.cause());
      }
    });
    
  }
  
  
}
