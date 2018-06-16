package xin.beanan.blog.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import xin.beanan.blog.constants.ArticleConstants;
import xin.beanan.blog.entity.Article;
import xin.beanan.blog.entity.Category;
import xin.beanan.blog.entity.Tag;
import xin.beanan.blog.service.ArticleService;
import xin.beanan.blog.service.CategoryService;
import xin.beanan.blog.service.impl.ArticleServiceImpl;
import xin.beanan.blog.service.impl.CategoryServiceImpl;
import xin.beanan.blog.util.RouterUtils;
import xin.beanan.blog.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author BeanNan
 */
public class ArticleVerticle extends AbstractVerticle
    implements BaseVerticle {
  
  private ArticleService articleService;
  private CategoryService categoryService;
  private static final Logger LOGGER = LoggerFactory.getLogger(ArticleVerticle.class);
  
  private Future<Boolean> initData() {
    categoryService = new CategoryServiceImpl(vertx, config());
    articleService = new ArticleServiceImpl(vertx, config());
    return articleService.initData();
  }
  
  @Override
  public void start(Future<Void> startFuture) throws Exception {
    Router router = RouterUtils.getRouter();
    /*
    设置category分类的路由
     */
    router.get(ArticleConstants.API_GET).handler(this::handleArticleGet);
    router.get(ArticleConstants.API_LIST_ALL).handler(this::handleArticleGetAll);
    router.post(ArticleConstants.API_CREATE).handler(this::handleArticleCreate);
    router.patch(ArticleConstants.API_UPDATE).handler(this::handleArticleUpdate);
    router.delete(ArticleConstants.API_DELETE).handler(this::handleArticleDelete);
    router.delete(ArticleConstants.API_DELETE_ALL).handler(this::handleArticleDeleteAll);
    router.get(ArticleConstants.API_GET_LAST).handler(this::handleArticleLast);
    
    Future<Boolean> booleanFuture = initData();
    booleanFuture.setHandler(res -> {
      if (res.succeeded()) {
        startFuture.complete();
      } else {
        startFuture.fail("articleService init failure");
      }
    });
  }
  
  private void handleArticleLast(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    articleService.getLast(-3, -1).setHandler(resultHandler(routingContext, articleList -> {
      warpResult(response, articleList);
    }));
  }
  
  private void handleArticleCreate(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    MultiMap params = request.params();
    List<Tag> tags = new ArrayList<>();
    params.forEach(entry -> {
      if ("tag_id".equals(entry.getKey())) {
        Tag tag = new Tag();
        tag.setTag_id(entry.getValue());
        tags.add(tag);
      }
    });
    categoryService.getCertain(request.getParam("category_id"))
        .setHandler(result -> {
          if (result.succeeded()) {
            Optional<Category> categoryOptional = result.result();
            if (categoryOptional.isPresent()) {
              Category category = categoryOptional.get();
              String content = request.getParam("test-editormd-markdown-doc");
              String article_name = request.getParam("article_name");
              Article article = new Article(UUID.randomUUID().toString(), article_name, category, tags, content);
              Buffer buffer = Buffer.buffer(content);
              vertx.fileSystem().writeFile("/home/beanan/blog/" + article.getArticle_id() + ".md", buffer, res -> {
                if (res.succeeded()) {
                  article.setArticle_content_pos("/home/beanan/blog/" + article.getArticle_id() + ".md");
                  Future<Boolean> insert = articleService.insert(article);
                  insert.setHandler(insertResult -> {
                    if (insertResult.succeeded() && insertResult.result()) {
                      response.setStatusCode(200).end();
                    } else {
                      LOGGER.error(insertResult.cause());
                      serviceUnavailable(routingContext);
                    }
                  });
                } else {
                  LOGGER.error(res.cause());
                  serviceUnavailable(routingContext);
                }
              });
            } else {
              serviceUnavailable(routingContext);
            }
          } else {
            LOGGER.error(result.cause());
            serviceUnavailable(routingContext);
          }
        });
  }
  
  private void handleArticleUpdate(RoutingContext routingContext) {
  
  }
  
  private void handleArticleGetAll(RoutingContext routingContext) {
    try {
      HttpServerRequest request = routingContext.request();
      HttpServerResponse response = routingContext.response();
      int page = request.getParam("page") == null ?
          0 : Integer.parseInt(request.getParam("page"));
      int limit = request.getParam("limit") == null ?
          0 : Integer.parseInt(request.getParam("limit"));
      
      if (page == 0 && limit == 0) {
        articleService.getAll().setHandler(resultHandler(routingContext, articleList -> {
          warpResult(response, articleList);
        }));
      } else if (page >= 0 && limit >= 0) {
        articleService.range(page, limit).setHandler(resultHandler(routingContext, articleList -> {
          warpResult(response, articleList);
        }));
      } else {
        badRequest(routingContext);
      }
      
    } catch (NumberFormatException e) {
      badRequest(routingContext);
    }
  }
  
  private void warpResult(HttpServerResponse response, List<Article> articleList) {
    Future<Long> totalCount = articleService.getTotalCount();
    totalCount.setHandler(res -> {
      if (res.succeeded()) {
        articleList.forEach(article -> article.setContent(null));
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("data", articleList);
        jsonObject.put("code", 0);
        jsonObject.put("msg", "success");
        jsonObject.put("count", res.result());
        String data = jsonObject.toString();
        response.setStatusCode(200);
        response.putHeader("content-type", "application/json");
        response.end(data);
      } else {
        response.setStatusCode(500).end();
      }
    });
    
  }
  
  private void handleArticleDelete(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = request.response();
    String article_id = request.getParam("article_id");
    if (StringUtils.isEmpty(article_id)) {
      badRequest(routingContext);
      return;
    }
  
    articleService.delete(article_id).setHandler(resultHandler(routingContext, result -> {
      if (result) {
        response.setStatusCode(200).end();
      } else {
        response.setStatusCode(500).end();
      }
    }));
  }
  
  private void handleArticleDeleteAll(RoutingContext routingContext) {
    
  }
  
  private void handleArticleGet(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String article_id = request.getParam("article_id");
    if (StringUtils.isEmpty(article_id)) {
      badRequest(routingContext);
      return;
    }
    articleService.getCertain(article_id).setHandler(resultHandler(routingContext, result -> {
      if (result.isPresent()) {
        Article category = result.get();
        String data = category.toJson().toString();
        response.putHeader("content-type", "application/json");
        response.end(data);
      } else {
        badRequest(routingContext);
      }
    }));
  }
}
