package xin.beanan.blog;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import xin.beanan.blog.verticle.DeployVerticle;

/**
 * @author BeanNan
 */
public class CoreApplication {
  
  public static void main(String[] args) {
    Logger logger = LoggerFactory.getLogger(CoreApplication.class);
  
    ClusterManager mgr = new HazelcastClusterManager();
    VertxOptions vertxOptions = new VertxOptions();
    vertxOptions.setClusterManager(mgr);
    Vertx.clusteredVertx(vertxOptions, res -> {
      if (res.succeeded()) {
        Vertx vertx = res.result();
        vertx.fileSystem().readFile(DeployVerticle.class.getClassLoader()
            .getResource("config.json")
            .getPath(), readResult  -> {
          if (readResult.succeeded()) {
            Buffer buffer = readResult.result();
            JsonObject config = buffer.toJsonObject();
            DeploymentOptions deploymentOptions = new DeploymentOptions();
            deploymentOptions.setConfig(config);
            vertx.deployVerticle(new DeployVerticle(), deploymentOptions, result -> {
              if (result.succeeded()) {
                logger.info("app startup success");
              } else {
                logger.error("app startup failed");
              }
            });
          } else {
            logger.error("app node create failed");
          }
        });
  
        
      } else {
        logger.error("app node create failed");
      }
    });
    
    
  }
}
