package xin.beanan.blog.service.impl;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import xin.beanan.blog.service.IncrService;

/**
 * @author BeanNan
 */
public class IncrServiceImpl implements IncrService {
  
  private Vertx vertx;
  private JsonObject sqlConfig;
  private SQLClient sqlClient;
  private static final Logger LOGGER = LoggerFactory.getLogger(IncrServiceImpl.class);
  
  private static final String SQL_INCR_ARTICLE_READ_NUM = "update article set read_num=read_num+1 where article_id=?";
  
  public IncrServiceImpl(Vertx vertx, JsonObject sqlConfig) {
    this.vertx = vertx;
    this.sqlConfig = sqlConfig;
    this.sqlClient = JDBCClient.createShared(vertx, sqlConfig);
  }
  
  @Override
  public void incrArticleReadNumToDatabase(String article_id,
                                           Handler<AsyncResult<Void>> handler) {
  
    Context context = vertx.getOrCreateContext();
    context.runOnContext(action -> {
      LOGGER.info(Thread.currentThread().getName());
      sqlClient.getConnection(connnectionResult -> {
        if (connnectionResult.succeeded()) {
          SQLConnection sqlConnection = connnectionResult.result();
          JsonArray jsonArray = new JsonArray();
          jsonArray.add(article_id);
          sqlConnection.updateWithParams(SQL_INCR_ARTICLE_READ_NUM, jsonArray, updateResult -> {
            if (updateResult.succeeded()) {
              handler.handle(Future.succeededFuture());
            } else {
              handler.handle(Future.failedFuture(updateResult.cause()));
              LOGGER.error(updateResult.cause());
            }
            sqlConnection.close();
          });
        } else {
          LOGGER.error(connnectionResult.cause());
          handler.handle(Future.failedFuture(connnectionResult.cause()));
        }
      });
    });
  
    
    
  }
}
