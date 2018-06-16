package xin.beanan.blog.service.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;
import xin.beanan.blog.entity.Tag;
import xin.beanan.blog.service.TagService;
import xin.beanan.blog.util.DateUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

/**
 * @author BeanNan
 */
public class TagServiceImpl implements TagService {
  
  private Vertx vertx;
  private JsonObject config;
  private RedisClient redisClient;
  private SQLClient sqlClient;
  
  private static final Logger LOGGER = LoggerFactory.getLogger(TagServiceImpl.class);
  private static final DateFormat DATE_FORMAT = DateUtils.DATE_FORMAT;
  private static final String SQL_GET_ALL = "select * from tag";
  private static final String SQL_GET_ALL_RANGE = "select * from tag limit ?,?";
  private static final String SQL_GET_CERTAIN = "select * from tag where tag_id = ?;";
  private static final String SQL_INSERT = "insert into tag(tag_id, tag_name,create_time, update_time) values (?,?,?,?)";
  private static final String REDIT_TAG_KEY = "blog.tags";
  private static final String SQL_UPDATE = "update tag set tag_name = ?, update_time = ? where tag_id = ?";
  private static final String SQL_DELETE_ONE = "delete from tag where tag_id = ?";
  private static final String SQL_DELETE_ALL = "delete from tag";
  
  public TagServiceImpl(Vertx vertx, JsonObject config) {
    this.vertx = vertx;
    this.config = config;
    sqlClient = JDBCClient.createShared(vertx, config);
    RedisOptions redisOptions = new RedisOptions();
    redisOptions.setHost(config.getString("redis_host"));
    redisOptions.setPort(config.getInteger("redis_port"));
    redisClient = RedisClient.create(vertx, redisOptions);
  }
  
  public Future<Boolean> initData() {
    return initData(sqlClient, redisClient, "tagService", LOGGER);
  }
  
  @Override
  public Future<Long> getTotalCount() {
    Future<Long> future = Future.future();
    redisClient.llen(REDIT_TAG_KEY, redisHandler(future, future::complete));
    return future;
  }
  
  @Override
  public Future<List<Tag>> getAll() {
    Future<List<Tag>> future = Future.future();
    
    redisClient.lrange(REDIT_TAG_KEY, 0, -1, redisHandler(future, jsonArray -> {
      if (jsonArray.isEmpty()) {
        sqlClient.getConnection(connectionHandler(future, sqlConnection -> {
          sqlConnection.query(SQL_GET_ALL, res -> {
            if (res.failed()) {
              future.fail(res.cause());
            } else {
              ResultSet result = res.result();
              List<Tag> tagList
                  = result.getRows().stream()
                  .map(jsonObject -> jsonToTag(future, jsonObject))
                  .collect(toList());
              List<String> stringTagList = tagList.stream()
                  .map(Tag::toString)
                  .collect(toList());
              redisClient.rpushMany(REDIT_TAG_KEY, stringTagList, redisHandler(future, insertNum -> {
                LOGGER.info("tags insert redis\t" + insertNum);
              }));
              future.complete(tagList);
            }
            sqlConnection.close();
          });
          
        }));
      } else {
        List<Tag> tagList = jsonArray.stream()
            .map(string -> {
              JsonObject jsonObject = new JsonObject((String) string);
              return new Tag(jsonObject);
            })
            .collect(toList());
        future.complete(tagList);
      }
    }));
    return future;
  }
  
  /**
   * 为了转换时间
   *
   * @param future     future
   * @param jsonObject jsonObject
   * @return tag
   */
  public Tag jsonToTag(Future future, JsonObject jsonObject) {
    Tag tag = new Tag();
    try {
      String create_time = jsonObject.getString("create_time");
      String update_time = jsonObject.getString("update_time");
      jsonObject.put("create_time", DATE_FORMAT.parse(create_time).getTime());
      jsonObject.put("update_time", DATE_FORMAT.parse(update_time).getTime());
      tag = new Tag(jsonObject);
    } catch (ParseException e) {
      future.fail(e);
    }
    
    return tag;
  }
  
  @Override
  public Future<Optional<Tag>> getCertain(String id) {
    Future<Optional<Tag>> future = Future.future();
    sqlClient.getConnection(connectionHandler(future, sqlConnection -> {
      JsonArray jsonArray = new JsonArray();
      jsonArray.add(id);
      sqlConnection.queryWithParams(SQL_GET_CERTAIN, jsonArray, result -> {
        if (result.succeeded()) {
          ResultSet resultSet = result.result();
          List<JsonObject> rows = resultSet.getRows();
          if (rows.isEmpty()) {
            future.complete(Optional.empty());
          } else {
            future.complete(Optional.of(jsonToTag(future, rows.get(0))));
          }
        } else {
          future.fail(result.cause());
        }
        sqlConnection.close();
      });
      
    }));
    return future;
  }
  
  @Override
  public Future<Boolean> insert(Tag obj) {
    Future<Boolean> future = Future.future();
    sqlClient.getConnection(connectionHandler(future, sqlConnection -> {
      JsonArray jsonArray = obj.toJsonArray();
      updateAfterDelRedis(future, jsonArray, sqlConnection, SQL_INSERT);
    }));
    return future;
  }
  
  @Override
  public Future<Boolean> update(String id, Tag obj) {
    Future<Boolean> future = Future.future();
    return getCertain(id).compose(oldTagOptional -> {
      if (!oldTagOptional.isPresent()) {
        future.fail("tag not exists");
      } else {
        Tag oldTag = oldTagOptional.get();
        Tag newTag = oldTag.merge(obj);
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(newTag.getTag_name());
        jsonArray.add(DATE_FORMAT.format(new Date(newTag.getUpdate_time())));
        jsonArray.add(newTag.getTag_id());
        sqlClient.getConnection(connectionHandler(Future.future(), sqlConnection -> {
          updateAfterDelRedis(future, jsonArray, sqlConnection, SQL_UPDATE);
        }));
      }
    }, future);
  }
  
  private void updateAfterDelRedis(Future<Boolean> future, JsonArray jsonArray, SQLConnection sqlConnection, String sqlUpdate) {
    sqlConnection.updateWithParams(sqlUpdate, jsonArray, updateResult -> {
      if (updateResult.succeeded()) {
        redisDeleteAll(redisClient, REDIT_TAG_KEY, "tag", LOGGER);
        future.complete(true);
      } else {
        future.fail(updateResult.cause());
      }
      sqlConnection.close();
    });
  }
  
  @Override
  public Future<Boolean> delete(String id) {
    Future<Boolean> future = Future.future();
    sqlClient.getConnection(connectionHandler(future, sqlConnection -> {
      JsonArray jsonArray = new JsonArray();
      jsonArray.add(id);
      updateAfterDelRedis(future, jsonArray, sqlConnection, SQL_DELETE_ONE);
    }));
    return future;
  }
  
  private void redisDeleteAll() {
    redisDeleteAll(redisClient, REDIT_TAG_KEY, "tag", LOGGER);
  }
  
  @Override
  public Future<Boolean> deleteAll() {
    Future<Boolean> future = Future.future();
    sqlClient.getConnection(connectionHandler(future, sqlConnection -> {
      sqlConnection.execute(SQL_DELETE_ALL, deleteResult -> {
        if (deleteResult.succeeded()) {
          future.complete(true);
          redisDeleteAll();
        } else {
          future.fail(deleteResult.cause());
        }
        sqlConnection.close();
      });
      
    }));
    return future;
  }
  
  @Override
  public Future<List<Tag>> range(int page, int limit) {
    Future<List<Tag>> future = Future.future();
    int start = limit * (page - 1);
    int end = start + limit - 1;
    redisClient.lrange(REDIT_TAG_KEY, start, end, redisHandler(future, jsonArray -> {
      if (jsonArray.isEmpty()) {
        Future<List<Tag>> rangeListFuture = getAll().compose(tagList -> {
          Future<List<Tag>> resultFuture = Future.future();
          try {
            List<Tag> subTagList = tagList.subList(start, end + 1);
            resultFuture.complete(subTagList);
          } catch (Exception e) {
            LOGGER.error("tag out of range");
            resultFuture.fail(e);
          }
          return resultFuture;
        });
        
        rangeListFuture.setHandler(res -> {
          if (res.succeeded()) {
            future.complete(rangeListFuture.result());
          } else {
            future.fail(rangeListFuture.cause());
          }
        });
        
      } else {
        List<Tag> tagList = jsonArray.stream()
            .map(string -> {
              JsonObject jsonObject = new JsonObject((String) string);
              return new Tag(jsonObject);
            })
            .collect(toList());
        future.complete(tagList);
      }
    }));
    
    return future;
  }
}
