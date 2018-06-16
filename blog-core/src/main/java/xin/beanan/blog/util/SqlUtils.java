package xin.beanan.blog.util;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.ext.sql.SQLConnection;


/**
 * @author BeanNan
 */
public class SqlUtils {
  
  private SqlUtils() {}
  
  public static void sqlCommit(SQLConnection sqlConnection, Future future, Logger logger) {
    sqlConnection.commit(commitResult -> {
      if (commitResult.succeeded()) {
        future.complete(true);
        logger.info("插入文章成功");
        sqlConnection.close();
      } else {
        future.fail(commitResult.cause());
        
      }
    });
  }
  
  public static void sqlRollBack(SQLConnection sqlConnection, Logger logger) {
    sqlConnection.rollback(roll -> {
      if (roll.failed()) {
        logger.info(roll.cause());
      }
      sqlConnection.close();
    });
  }
}
