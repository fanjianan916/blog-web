package xin.beanan;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import xin.beanan.blog.verticle.ProxyVerticle;

/**
 * @author BeanNan
 */
public class ProxyApplication {
  
  public static void main(String[] args) {
    Logger logger = LoggerFactory.getLogger(ProxyApplication.class);
    ClusterManager mgr = new HazelcastClusterManager();
    VertxOptions vertxOptions = new VertxOptions();
    vertxOptions.setClusterManager(mgr);
    Vertx.clusteredVertx(vertxOptions, res -> {
      if (res.succeeded()) {
        Vertx vertx = res.result();
        vertx.fileSystem().readFile(ProxyApplication.class.getClassLoader().getResource("config.json").getPath(),
            readResult -> {
              if (readResult.succeeded()) {
                Buffer buffer = readResult.result();
                JsonObject config = buffer.toJsonObject();
                ProxyVerticle proxyVerticle = new ProxyVerticle();
                DeploymentOptions deploymentOptions = new DeploymentOptions();
                deploymentOptions.setConfig(config);
                vertx.deployVerticle(proxyVerticle, deploymentOptions, result -> {
                  if (result.succeeded()) {
                    logger.error("proxy verticle deploy success");
                  } else {
                    logger.error("proxy verticle deploy failed");
                  }
                });
              } else {
                logger.error("proxy node create failed");
              }
            });
        
        
      } else {
        logger.error("proxy node create failed");
      }
    });
  }
}
