package xin.beanan.blog.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.redis.RedisClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author BeanNan
 */
public interface BaseService<T> {
  
  Future<List<T>> getAll();
  
  Future<Optional<T>> getCertain(String id);
  
  Future<Boolean> insert(T obj);
  
  Future<Boolean> update(String id, T obj);
  
  Future<Boolean> delete(String id);
  
  Future<Boolean> deleteAll();
  
  Future<List<T>> range(int page, int limit);
  
  Future<Long> getTotalCount();
  
  Future<Boolean> initData();
  
  default Future<Boolean> initData(SQLClient sqlClient, RedisClient redisClient, String flag, Logger logger) {
    Future<Boolean> future = Future.future();
    Future<Boolean> sqlFuture = Future.future();
    Future<Boolean> redisFuture = Future.future();
    
    sqlClient.getConnection(result -> {
      if (result.succeeded()) {
        SQLConnection sqlConnection = result.result();
        sqlConnection.execute("insert into blog_test values('" + UUID.randomUUID().toString() + "')", res -> {
          if (res.succeeded()) {
            sqlFuture.complete(true);
            logger.info(flag + " SQLClinet insert success");
          } else {
            logger.error(res.cause());
            sqlFuture.fail(flag + " SQLClinet insert error");
          }
          sqlConnection.close();
        });
      } else {
        logger.error(result.cause());
        sqlFuture.fail(flag + " SQLClinet connection error");
      }
    });
    
    redisClient.set(flag, "value", res -> {
      if (res.succeeded()) {
        logger.info(flag + " RedisClient init success");
        redisFuture.complete(true);
      } else {
        logger.error(flag + " RedisClient init error");
        redisFuture.fail(flag + " RedisClient init error");
      }
    });
    
    CompositeFuture.all(sqlFuture, redisFuture).setHandler(res -> {
      if (res.succeeded()) {
        future.complete(true);
      } else {
        future.fail(res.cause());
      }
    });
    
    return future;
  }
  
  default <U> Handler<AsyncResult<U>> redisHandler(Future future, Consumer<U> consumer) {
    return res -> {
      if (res.succeeded()) {
        consumer.accept(res.result());
      } else {
        future.fail(res.cause());
      }
    };
  }
  
  default void redisDeleteAll(RedisClient redisClient, String REDIT_TAG_KEY, String flag, Logger logger) {
    redisClient.del(REDIT_TAG_KEY, deleteResult -> {
      if (deleteResult.failed()) {
        logger.error("failed to delete after update/insert " + flag);
      }
    });
  }
  
  
  
  default Handler<AsyncResult<SQLConnection>> connectionHandler(Future future,
                                                                Consumer<SQLConnection> consumer) {
    return res -> {
      if (res.succeeded()) {
        SQLConnection connection = res.result();
        consumer.accept(connection);
      } else {
        future.fail(res.cause());
      }
    };
  }
  
  
  
}
