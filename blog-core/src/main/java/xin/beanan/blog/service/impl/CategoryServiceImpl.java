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
import xin.beanan.blog.entity.Category;
import xin.beanan.blog.service.CategoryService;
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
public class CategoryServiceImpl implements CategoryService {
  
  private Vertx vertx;
  private JsonObject config;
  private RedisClient redisClient;
  private SQLClient sqlClient;
  
  private static final Logger LOGGER = LoggerFactory.getLogger(CategoryServiceImpl.class);
  private static final DateFormat DATE_FORMAT = DateUtils.DATE_FORMAT;
  private static final String SQL_GET_ALL = "select * from category";
  private static final String SQL_GET_ALL_RANGE = "select * from category limit ?,?";
  private static final String SQL_GET_CERTAIN = "select * from category where category_id = ?;";
  private static final String SQL_INSERT = "insert into category(category_id, category_name,create_time, update_time) values (?,?,?,?)";
  private static final String REDIT_TAG_KEY = "blog.categorys";
  private static final String SQL_UPDATE = "update category set category_name = ?, update_time = ? where category_id = ?";
  private static final String SQL_DELETE_ONE = "delete from category where category_id = ?";
  private static final String SQL_DELETE_ALL = "delete from category";
  
  public CategoryServiceImpl(Vertx vertx, JsonObject config) {
    this.vertx = vertx;
    this.config = config;
    sqlClient = JDBCClient.createShared(vertx, config);
    RedisOptions redisOptions = new RedisOptions();
    redisOptions.setHost(config.getString("redis_host"));
    redisOptions.setPort(config.getInteger("redis_port"));
    redisClient = RedisClient.create(vertx, redisOptions);
  }
  
  private Category jsonToCategory(Future future, JsonObject jsonObject) {
    Category category = new Category();
    try {
      String create_time = jsonObject.getString("create_time");
      String update_time = jsonObject.getString("update_time");
      jsonObject.put("create_time", DATE_FORMAT.parse(create_time).getTime());
      jsonObject.put("update_time", DATE_FORMAT.parse(update_time).getTime());
      category = new Category(jsonObject);
    } catch (ParseException e) {
      future.fail(e);
    }
    
    return category;
  }
  
  private void updateAfterDelRedis(Future<Boolean> future, JsonArray jsonArray, SQLConnection sqlConnection, String sqlUpdate) {
    sqlConnection.updateWithParams(sqlUpdate, jsonArray, updateResult -> {
      if (updateResult.succeeded()) {
        redisDeleteAll();
        future.complete(true);
      } else {
        future.fail(updateResult.cause());
      }
      sqlConnection.close();
    });
  }
  
  private void redisDeleteAll() {
    redisDeleteAll(redisClient, REDIT_TAG_KEY, "category", LOGGER);
  }
  
  @Override
  public Future<List<Category>> getAll() {
    
    Future<List<Category>> future = Future.future();
    
    redisClient.lrange(REDIT_TAG_KEY, 0, -1, redisHandler(future, jsonArray -> {
      if (jsonArray.isEmpty()) {
        sqlClient.getConnection(connectionHandler(future, sqlConnection -> {
          sqlConnection.query(SQL_GET_ALL, res -> {
            if (res.failed()) {
              future.fail(res.cause());
            } else {
              ResultSet result = res.result();
              List<Category> tagList
                  = result.getRows().stream()
                  .map(jsonObject -> jsonToCategory(future, jsonObject))
                  .collect(toList());
              List<String> stringTagList = tagList.stream()
                  .map(Category::toString)
                  .collect(toList());
              redisClient.rpushMany(REDIT_TAG_KEY, stringTagList, redisHandler(future, insertNum -> {
                LOGGER.info("categorys insert redis\t" + insertNum);
              }));
              future.complete(tagList);
            }
            sqlConnection.close();
          });
          
        }));
      } else {
        List<Category> tagList = jsonArray.stream()
            .map(string -> {
              JsonObject jsonObject = new JsonObject((String) string);
              return new Category(jsonObject);
            })
            .collect(toList());
        future.complete(tagList);
      }
    }));
    return future;
  }
  
  @Override
  public Future<Optional<Category>> getCertain(String id) {
    
    Future<Optional<Category>> future = Future.future();
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
            future.complete(Optional.of(jsonToCategory(future, rows.get(0))));
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
  public Future<Boolean> insert(Category obj) {
    Future<Boolean> future = Future.future();
    sqlClient.getConnection(connectionHandler(future, sqlConnection -> {
      JsonArray jsonArray = obj.toJsonArray();
      updateAfterDelRedis(future, jsonArray, sqlConnection, SQL_INSERT);
    }));
    return future;
  }
  
  @Override
  public Future<Boolean> update(String id, Category obj) {
    Future<Boolean> future = Future.future();
    return getCertain(id).compose(oldCategoryOptional -> {
      if (!oldCategoryOptional.isPresent()) {
        future.fail("tag not exists");
      } else {
        Category oldCategory = oldCategoryOptional.get();
        Category newCategory = oldCategory.merge(obj);
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(newCategory.getCategory_name());
        jsonArray.add(DATE_FORMAT.format(new Date(newCategory.getUpdate_time())));
        jsonArray.add(newCategory.getCategory_id());
        sqlClient.getConnection(connectionHandler(Future.future(), sqlConnection -> {
          updateAfterDelRedis(future, jsonArray, sqlConnection, SQL_UPDATE);
        }));
      }
    }, future);
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
  
  @Override
  public Future<Boolean> deleteAll() {
    Future<Boolean> future = Future.future();
    sqlClient.getConnection(connectionHandler(future, sqlConnection -> {
      sqlConnection.execute(SQL_DELETE_ALL, deleteResult -> {
        if (deleteResult.succeeded()) {
          redisDeleteAll();
          future.complete(true);
        } else {
          future.fail(deleteResult.cause());
        }
        sqlConnection.close();
      });
      
    }));
    return future;
  }
  
  @Override
  public Future<List<Category>> range(int page, int limit) {
    Future<List<Category>> future = Future.future();
    int start = limit * (page - 1);
    int end = start + limit - 1;
    redisClient.lrange(REDIT_TAG_KEY, start, end, redisHandler(future, jsonArray -> {
      if (jsonArray.isEmpty()) {
        Future<List<Category>> rangeListFuture = getAll().compose(categoryList -> {
          Future<List<Category>> resultFuture = Future.future();
          try {
            List<Category> subTagList = categoryList.subList(start, end + 1);
            resultFuture.complete(subTagList);
          } catch (Exception e) {
            LOGGER.error("catgory out of range");
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
        List<Category> tagList = jsonArray.stream()
            .map(string -> {
              JsonObject jsonObject = new JsonObject((String) string);
              return new Category(jsonObject);
            })
            .collect(toList());
        future.complete(tagList);
      }
    }));
  
    return future;
  }
  
  
  public Future<Boolean> initData() {
    return initData(sqlClient, redisClient, "categoryService", LOGGER);
  }
  
  @Override
  public Future<Long> getTotalCount() {
    Future<Long> future = Future.future();
    redisClient.llen(REDIT_TAG_KEY, redisHandler(future, future::complete));
    return future;
  }
}
