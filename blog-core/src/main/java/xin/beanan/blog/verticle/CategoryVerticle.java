package xin.beanan.blog.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import xin.beanan.blog.constants.CategoryConstatns;
import xin.beanan.blog.entity.Category;
import xin.beanan.blog.service.CategoryService;
import xin.beanan.blog.service.impl.CategoryServiceImpl;
import xin.beanan.blog.util.RouterUtils;
import xin.beanan.blog.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * @author BeanNan
 */
public class CategoryVerticle extends AbstractVerticle implements BaseVerticle {
  
  private CategoryService categoryService;
  private static final Logger LOGGER = LoggerFactory.getLogger(CategoryVerticle.class);
  
  private Future<Boolean> initData() {
    categoryService = new CategoryServiceImpl(vertx, config());
    return categoryService.initData();
  }
  
  @Override
  public void start(Future<Void> startFuture) throws Exception {
    Router router = RouterUtils.getRouter();
    /*
    设置category分类的路由
     */
    router.get(CategoryConstatns.API_GET).handler(this::handleCategoryGet);
    router.get(CategoryConstatns.API_LIST_ALL).handler(this::handleCategoryGetAll);
    router.post(CategoryConstatns.API_CREATE).handler(this::handleCategoryCreate);
    router.patch(CategoryConstatns.API_UPDATE).handler(this::handleCategoryUpdate);
    router.delete(CategoryConstatns.API_DELETE).handler(this::handleCategoryDelete);
    router.delete(CategoryConstatns.API_DELETE_ALL).handler(this::handleCategoryDeleteAll);
    
    Future<Boolean> booleanFuture = initData();
    booleanFuture.setHandler(res -> {
      if (res.succeeded()) {
        startFuture.complete();
      } else {
        startFuture.fail("categoryService init failure");
      }
    });
  }
  
  private void handleCategoryCreate(RoutingContext routingContext) {
    try {
    /*
      1. 获取category_name参数
      2. 插入数据库
      3. 返回结果
     */
      HttpServerRequest request = routingContext.request();
      HttpServerResponse response = routingContext.response();
      JsonObject bodyAsJson = routingContext.getBodyAsJson();
      String category_name = bodyAsJson.getString("category_name");
      if (category_name == null) {
        badRequest(routingContext);
        return;
      }
      
      Category category = new Category(UUID.randomUUID().toString(), category_name);
      categoryService.insert(category).setHandler(resultHandler(routingContext, result -> {
        if (result) {
          response.setStatusCode(200).end();
        } else {
          serviceUnavailable(routingContext);
        }
      }));
    } catch (Exception e) {
      badRequest(routingContext);
    }
    
  }
  
  private void handleCategoryUpdate(RoutingContext routingContext) {
    try {
      HttpServerRequest request = routingContext.request();
      HttpServerResponse response = routingContext.response();
      String categoryId = request.getParam("category_id");
      if (categoryId == null || "".equals(categoryId)) {
        badRequest(routingContext);
        return;
      }
      JsonObject categoryJson = routingContext.getBodyAsJson();
      Category category = new Category(categoryJson);
      categoryService.update(categoryId, category).setHandler(resultHandler(routingContext, result -> {
        if (result) {
          response.setStatusCode(200).end();
        } else {
          serviceUnavailable(routingContext);
        }
      }));
    } catch (Exception e) {
      badRequest(routingContext);
    }
  }
  
  private void handleCategoryGetAll(RoutingContext routingContext) {
    try {
      HttpServerRequest request = routingContext.request();
      HttpServerResponse response = routingContext.response();
      int page = request.getParam("page") == null ?
          0 : Integer.parseInt(request.getParam("page"));
      int limit = request.getParam("limit") == null ?
          0 : Integer.parseInt(request.getParam("limit"));
      
      if (page == 0 && limit == 0) {
        categoryService.getAll().setHandler(resultHandler(routingContext, categoryList -> {
          warpResult(response, categoryList);
        }));
      } else if (page >= 0 && limit >= 0) {
        categoryService.range(page, limit).setHandler(resultHandler(routingContext, categoryList -> {
          warpResult(response, categoryList);
        }));
      } else {
        badRequest(routingContext);
      }
      
    } catch (NumberFormatException e) {
      badRequest(routingContext);
    }
  }
  
  private void warpResult(HttpServerResponse response, List<Category> categoryList) {
    Future<Long> totalCount = categoryService.getTotalCount();
    totalCount.setHandler(res -> {
      if (res.succeeded()) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("data", categoryList);
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
  
  private void handleCategoryDelete(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = request.response();
    String category_id = request.getParam("category_id");
    if (StringUtils.isEmpty(category_id)) {
      badRequest(routingContext);
      return;
    }
    
    categoryService.delete(category_id).setHandler(resultHandler(routingContext, result -> {
      if (result) {
        response.setStatusCode(200).end();
      } else {
        response.setStatusCode(500).end();
      }
    }));
  }
  
  private void handleCategoryDeleteAll(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    categoryService.deleteAll().setHandler(resultHandler(routingContext, result -> {
      if (result) {
        response.setStatusCode(200).end();
      } else {
        response.setStatusCode(500).end();
      }
    }));
  }
  
  private void handleCategoryGet(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String category_id = request.getParam("category_id");
    if (StringUtils.isEmpty(category_id)) {
      badRequest(routingContext);
      return;
    }
    categoryService.getCertain(category_id).setHandler(resultHandler(routingContext, result -> {
      if (result.isPresent()) {
        Category category = result.get();
        String data = category.toJson().toString();
        response.putHeader("content-type", "application/json");
        response.end(data);
      } else {
        badRequest(routingContext);
      }
    }));
    
  }
}
