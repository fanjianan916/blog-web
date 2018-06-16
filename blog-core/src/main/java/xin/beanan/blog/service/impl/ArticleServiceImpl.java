package xin.beanan.blog.service.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;
import io.vertx.redis.RedisTransaction;
import io.vertx.serviceproxy.ServiceProxyBuilder;
import xin.beanan.blog.entity.Article;
import xin.beanan.blog.service.ArticleService;
import xin.beanan.blog.service.IncrService;
import xin.beanan.blog.util.DateUtils;
import xin.beanan.blog.util.SqlUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author BeanNan
 */
public class ArticleServiceImpl implements ArticleService {
  
  private Vertx vertx;
  private JsonObject config;
  private RedisClient redisClient;
  private SQLClient sqlClient;
  private IncrService incrService;
  
  private static final Logger LOGGER = LoggerFactory.getLogger(ArticleServiceImpl.class);
  private static final DateFormat DATE_FORMAT = DateUtils.DATE_FORMAT;
  
  private static final String REDIS_HASH_POXFIE = "blog.articles.";
  private static final String REDIS_LIST = "blog.articles";
  
  private static final String SQL_INSERT_ARTICLE = "insert into article (article_id, \n" +
      "                     article_name,\n" +
      "                     article_content_pos,\n" +
      "                     create_time,\n" +
      "                     update_time,\n" +
      "                     read_num,\n" +
      "                     category_id)\n" +
      "values (?, ?, ?, ?, ?, ?, ?)";
  
  private static final String SQL_GET_ALL = "select * from article";
  
  private static final String SQL_GET_ALL_WITH_CATEGORY = "select\n" +
      "  article_id,\n" +
      "  article_name,\n" +
      "  article_content_pos,\n" +
      "  a.create_time,\n" +
      "  a.update_time,\n" +
      "  read_num,\n" +
      "  a.category_id,\n" +
      "  category_name\n" +
      "from article as a left join category as c on a.category_id = c.category_id";
  
  private static final String SQL_INSERT_ARTICLE_TAG = "insert into arcitle_tag_res(article_id, tag_id) values (?,?)";
  
  private static final String SQL_DELETE_ARTICLE = "delete from article where article_id = ?";
  private static final String SQL_DELETE_ARTICLE_TAG = "delete from arcitle_tag_res where article_id = ?";
  
  
  public ArticleServiceImpl(Vertx vertx, JsonObject config) {
    this.vertx = vertx;
    this.config = config;
    sqlClient = JDBCClient.createShared(vertx, config);
    RedisOptions redisOptions = new RedisOptions();
    redisOptions.setHost(config.getString("redis_host"));
    redisOptions.setPort(config.getInteger("redis_port"));
    redisClient = RedisClient.create(vertx, redisOptions);
    ServiceProxyBuilder builder = new ServiceProxyBuilder(vertx).setAddress("incr.article.readNum");
    incrService = builder.build(IncrService.class);
  }
  
  @Override
  public Future<List<Article>> getAll() {
    Future<List<Article>> future = Future.future();
    redisClient.lrange(REDIS_LIST, 0, -1, redisHandler(future, articles -> {
      if (articles.isEmpty()) {
        readArticleFromDatabase().setHandler(readResult -> {
          if (readResult.succeeded()) {
            List<Article> result = readResult.result();
            for (int i = 0; i < result.size(); i++) {
              insertRedis(result.get(i));
            }
            future.complete(readResult.result());
          } else {
            future.fail(readResult.cause());
          }
        });
      } else {
        List<Article> articleList = new ArrayList<>();
        Future<Void> getAllFuture = Future.future();
        readRedisArtitles(future, articles, articleList, getAllFuture);
        getAllFuture.setHandler(getAllResult -> {
          if (getAllFuture.succeeded()) {
            future.complete(articleList);
          } else {
            future.fail(getAllFuture.cause());
          }
        });
      }
      
    }));
    return future;
  }
  
  private Future<List<Article>> readArticleFromDatabase() {
    Future<List<Article>> sqlQueryFuture = Future.future();
    sqlClient.getConnection(connectionHandler(sqlQueryFuture, sqlConnection -> {
      sqlConnection.query(SQL_GET_ALL_WITH_CATEGORY, getAllResult -> {
        if (getAllResult.succeeded()) {
          ResultSet resultSet = getAllResult.result();
          List<Article> articleList = resultSet.getRows().stream()
              .map(jsonObject -> jsonToArticle(sqlQueryFuture, jsonObject))
              .collect(Collectors.toList());
          if (!sqlQueryFuture.failed()) {
            sqlQueryFuture.complete(articleList);
          }
        } else {
          sqlQueryFuture.fail(getAllResult.cause());
          LOGGER.error("数据库查询文章失败");
        }
      });
    }));
    
    return sqlQueryFuture;
  }
  
  private Article jsonToArticle(Future future, JsonObject jsonObject) {
    
    Article article = new Article();
    try {
      String create_time = jsonObject.getString("create_time");
      String update_time = jsonObject.getString("update_time");
      jsonObject.put("create_time", DATE_FORMAT.parse(create_time).getTime());
      jsonObject.put("update_time", DATE_FORMAT.parse(update_time).getTime());
      JsonObject catgoryJson = new JsonObject();
      catgoryJson.put("category_id", jsonObject.getString("category_id"));
      catgoryJson.put("category_name", jsonObject.getString("category_name"));
      jsonObject.remove("category_id");
      jsonObject.remove("category_name");
      jsonObject.put("category", catgoryJson);
      Buffer buffer = vertx.fileSystem().readFileBlocking(jsonObject.getString("article_content_pos"));
      jsonObject.put("content", buffer.toString());
      article = new Article(jsonObject);
    } catch (ParseException e) {
      future.fail(e);
    }
    
    return article;
  }
  
  @Override
  public Future<Optional<Article>> getCertain(String id) {
    Future<Optional<Article>> future = Future.future();
    redisClient.hincrby(REDIS_HASH_POXFIE + id, "read_num", 1, incrResult -> {
      if (incrResult.succeeded()) {
        incrService.incrArticleReadNumToDatabase(id, serviceHandler -> {
          if (serviceHandler.succeeded()) {
            LOGGER.info("数据落地成功");
          } else {
            LOGGER.error("数据落地失败");
            LOGGER.error(serviceHandler.cause());
          }
        });
        redisClient.hgetall(REDIS_HASH_POXFIE + id, getAllResult -> {
          if (getAllResult.succeeded()) {
            JsonObject articleJson = getAllResult.result();
            String category = articleJson.getString("category");
            JsonObject jsonObject = new JsonObject(category);
            articleJson.remove("category");
            articleJson.put("category", jsonObject);
            String tags = articleJson.getString("tags");
            JsonArray jsonArray = new JsonArray(tags);
            articleJson.put("tags", jsonArray);
            String read_num = articleJson.getString("read_num");
            articleJson.put("read_num", Integer.parseInt(read_num));
            articleJson.put("update_time", Long.parseLong(articleJson.getString("update_time")));
            
            Article article = new Article(articleJson);
            Optional<Article> articleOptional = Optional.of(article);
            future.complete(articleOptional);
          } else {
            future.fail(getAllResult.cause());
          }
        });
      } else {
        future.fail(incrResult.cause());
      }
    });
    return future;
  }
  
  @Override
  public Future<Boolean> insert(Article obj) {
    Future<Boolean> future = Future.future();
    sqlClient.getConnection(connectionHandler(future, sqlConnection -> {
      sqlConnection.setAutoCommit(false, autoCommitResult -> {
        if (autoCommitResult.succeeded()) {
          sqlConnection.updateWithParams(SQL_INSERT_ARTICLE,
              obj.toJsonArray(), insertArticleResult -> {
                if (insertArticleResult.succeeded()) {
                  List<JsonArray> batchArray = new ArrayList<>();
                  obj.getTags().forEach(tag -> {
                    JsonArray articleTagArray = new JsonArray();
                    articleTagArray.add(obj.getArticle_id());
                    articleTagArray.add(tag.getTag_id());
                    batchArray.add(articleTagArray);
                  });
                  sqlConnection.batchWithParams(SQL_INSERT_ARTICLE_TAG, batchArray, batch -> {
                    if (batch.failed()) {
                      future.fail(batch.cause());
                      LOGGER.error("article tag batch failed");
                      SqlUtils.sqlRollBack(sqlConnection, LOGGER);
                    } else {
                      insertRedis(obj).setHandler(insertRedis -> {
                        if (insertRedis.succeeded()) {
                          SqlUtils.sqlCommit(sqlConnection, future, LOGGER);
                        } else {
                          future.fail(insertRedis.cause());
                          LOGGER.error("article tag batch failed");
                          SqlUtils.sqlRollBack(sqlConnection, LOGGER);
                        }
                      });
                      
                    }
                  });
                } else {
                  future.fail(insertArticleResult.cause());
                  LOGGER.error("插入文章失败" + insertArticleResult.cause());
                  SqlUtils.sqlRollBack(sqlConnection, LOGGER);
                }
              });
          
        } else {
          future.fail(autoCommitResult.cause());
          LOGGER.error("事务开启失败");
        }
      });
    }));
    
    return future;
  }
  
  private Future<Void> insertRedis(Article article) {
    Future<Void> future = Future.future();
    RedisTransaction transaction = redisClient.transaction();
    transaction.multi(multi -> {
      transaction.hmset(REDIS_HASH_POXFIE + article.getArticle_id(),
          article.toJson(),
          setResult -> {
            if (setResult.succeeded()) {
              transaction.lpush(REDIS_LIST, article.getArticle_id(), pushResult -> {
                if (pushResult.succeeded()) {
                  transaction.exec(execResult -> {
                    if (execResult.succeeded()) {
                      future.complete();
                    } else {
                      future.fail(execResult.cause());
                    }
                  });
                } else {
                  future.fail(pushResult.cause());
                }
              });
            } else {
              future.fail(setResult.cause());
            }
          });
    });
    return future;
  }
  
  @Override
  public Future<Boolean> update(String id, Article obj) {
    return null;
  }
  
  @Override
  public Future<Boolean> delete(String id) {
    Future<Boolean> future = Future.future();
    sqlClient.getConnection(connectionHandler(future, sqlConnection -> {
      JsonArray jsonArray = new JsonArray();
      jsonArray.add(id);
      sqlConnection.setAutoCommit(false, setAutoCommit -> {
        if (setAutoCommit.succeeded()) {
          sqlConnection.updateWithParams(SQL_DELETE_ARTICLE,
              jsonArray,
              deleteArticleResult -> {
                if (deleteArticleResult.succeeded()) {
                  sqlConnection.updateWithParams(SQL_DELETE_ARTICLE_TAG,
                      jsonArray,
                      deleteArticleTagResult -> {
                        if (deleteArticleTagResult.succeeded()) {
                          deleteRedis(id).setHandler(deleteRedis -> {
                            if (deleteRedis.succeeded()) {
                              deleteFile(id).setHandler(deleteFile -> {
                                if (deleteFile.succeeded()) {
                                  SqlUtils.sqlCommit(sqlConnection, future, LOGGER);
                                } else {
                                  SqlUtils.sqlRollBack(sqlConnection, LOGGER);
                                }
                              });
                              
                            } else {
                              SqlUtils.sqlRollBack(sqlConnection, LOGGER);
                            }
                          });
                        } else {
                          SqlUtils.sqlRollBack(sqlConnection, LOGGER);
                        }
                      });
                } else {
                  SqlUtils.sqlRollBack(sqlConnection, LOGGER);
                }
              });
        } else {
          future.fail(setAutoCommit.cause());
        }
      });
    }));
    return future;
  }
  
  private Future<Void> deleteFile(String article_id) {
    Future<Void> future = Future.future();
    vertx.fileSystem().delete("/home/beanan/blog/" + article_id + ".md",
        deleteResult -> {
          if (deleteResult.succeeded()) {
            future.complete();
          } else {
            future.fail(deleteResult.cause());
          }
        });
    
    return future;
  }
  
  private Future<Void> deleteRedis(String article_id) {
    Future<Void> future = Future.future();
    RedisTransaction transaction = redisClient.transaction();
    transaction.multi(multi -> {
      transaction.del(REDIS_HASH_POXFIE + article_id,
          setResult -> {
            if (setResult.succeeded()) {
              transaction.lrem(REDIS_LIST, 1, article_id, remResult -> {
                if (remResult.succeeded()) {
                  transaction.exec(execResult -> {
                    if (execResult.succeeded()) {
                      future.complete();
                    } else {
                      future.fail(execResult.cause());
                    }
                  });
                } else {
                  future.fail(remResult.cause());
                }
              });
            } else {
              future.fail(setResult.cause());
            }
          });
    });
    return future;
  }
  
  @Override
  public Future<Boolean> deleteAll() {
    return null;
  }
  
  @Override
  public Future<List<Article>> range(int page, int limit) {
    Future<List<Article>> future = Future.future();
    int start = limit * (page - 1);
    int end = start + limit - 1;
    redisClient.lrange(REDIS_LIST, start, end, redisHandler(future, jsonArray -> {
      if (jsonArray.isEmpty()) {
        Future<List<Article>> rangeListFuture = getAll().compose(articleList -> {
          Future<List<Article>> resultFuture = Future.future();
          try {
            List<Article> subTagList = articleList.subList(start, end + 1);
            resultFuture.complete(subTagList);
          } catch (Exception e) {
            LOGGER.error("article out of range");
            resultFuture.fail(e);
          }
          return resultFuture;
        });
        
        rangeListFuture.setHandler(res -> {
          if (res.succeeded()) {
            future.complete(res.result());
          } else {
            future.fail(res.cause());
          }
        });
        
      } else {
        List<Article> articleList = new ArrayList<>();
        Future<Void> getAllFuture = Future.future();
        readRedisArtitles(future, jsonArray, articleList, getAllFuture);
        getAllFuture.setHandler(getAllResult -> {
          if (getAllFuture.succeeded()) {
            future.complete(articleList);
          } else {
            future.fail(getAllFuture.cause());
          }
        });
      }
    }));
    
    return future;
  }
  
  private void readRedisArtitles(Future<List<Article>> future,
                                 JsonArray jsonArray,
                                 List<Article> articleList,
                                 Future<Void> getAllFuture) {
    for (int i = 0; i < jsonArray.size(); i++) {
      String article_id = jsonArray.getString(i);
      final int currentSize = i + 1;
      redisClient.hgetall(REDIS_HASH_POXFIE + article_id, hgetallResult -> {
        if (hgetallResult.succeeded()) {
          JsonObject singleArticle = hgetallResult.result();
          String category = singleArticle.getString("category");
          JsonObject jsonObject = new JsonObject(category);
          singleArticle.remove("category");
          singleArticle.put("category", jsonObject);
          
          String tags = singleArticle.getString("tags");
          JsonArray tagsArray = new JsonArray(tags);
          singleArticle.put("tags", tagsArray);
          
          String read_num = singleArticle.getString("read_num");
          singleArticle.put("read_num", Integer.parseInt(read_num));
          
          String create_time = singleArticle.getString("create_time");
          String update_time = singleArticle.getString("update_time");
          singleArticle.put("create_time", Long.parseLong(create_time));
          singleArticle.put("update_time", Long.parseLong(update_time));
          
          Article article = new Article(singleArticle);
          articleList.add(article);
          
        } else {
          future.fail(hgetallResult.cause());
        }
        if (currentSize == jsonArray.size()) {
          if (articleList.size() == jsonArray.size()) {
            getAllFuture.complete();
          } else {
            getAllFuture.fail("文章转换过程中发生错误");
          }
        }
      });
    }
  }
  
  @Override
  public Future<Long> getTotalCount() {
    Future<Long> future = Future.future();
    redisClient.llen(REDIS_LIST, redisHandler(future, future::complete));
    return future;
  }
  
  @Override
  public Future<Boolean> initData() {
    return initData(sqlClient, redisClient, "article", LOGGER);
  }
  
  @Override
  public Future<List<Article>> getLast(int start, int end) {
    Future<List<Article>> future = Future.future();
    redisClient.lrange(REDIS_LIST, start, end, redisHandler(future, jsonArray -> {
      if (jsonArray.isEmpty()) {
        Future<List<Article>> rangeListFuture = getAll().compose(articleList -> {
          Future<List<Article>> resultFuture = Future.future();
          try {
            List<Article> subTagList = articleList.subList(start, end + 1);
            resultFuture.complete(subTagList);
          } catch (Exception e) {
            LOGGER.error("article out of range");
            resultFuture.fail(e);
          }
          return resultFuture;
        });
      
        rangeListFuture.setHandler(res -> {
          if (res.succeeded()) {
            future.complete(res.result());
          } else {
            future.fail(res.cause());
          }
        });
      
      } else {
        List<Article> articleList = new ArrayList<>();
        Future<Void> getAllFuture = Future.future();
        readRedisArtitles(future, jsonArray, articleList, getAllFuture);
        getAllFuture.setHandler(getAllResult -> {
          if (getAllFuture.succeeded()) {
            future.complete(articleList);
          } else {
            future.fail(getAllFuture.cause());
          }
        });
      }
    }));
    return future;
  }
}
