package xin.beanan.blog.verticle;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import xin.beanan.blog.constants.TagConstants;
import xin.beanan.blog.entity.Tag;
import xin.beanan.blog.service.TagService;
import xin.beanan.blog.service.impl.TagServiceImpl;
import xin.beanan.blog.util.RouterUtils;
import xin.beanan.blog.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * @author BeanNan
 */
public class TagVerticle extends AbstractVerticle implements BaseVerticle{
  
  private TagService tagService;
  private static final Logger LOGGER = LoggerFactory.getLogger(TagVerticle.class);
  
  private Future<Boolean> initData() {
    tagService = new TagServiceImpl(vertx, config());
    return tagService.initData();
  }
  
  @Override
  public void start(Future<Void> startFuture) throws Exception {
    Router router = RouterUtils.getRouter();
    /*
    设置tag标签的路由
     */
    router.get(TagConstants.API_GET).handler(this::handleTagGet);
    router.get(TagConstants.API_LIST_ALL).handler(this::handleTagGetAll);
    router.post(TagConstants.API_CREATE).handler(this::handleTagCreate);
    router.patch(TagConstants.API_UPDATE).handler(this::handleTagUpdate);
    router.delete(TagConstants.API_DELETE).handler(this::handleTagDelete);
    router.delete(TagConstants.API_DELETE_ALL).handler(this::handleTagDeleteAll);
  
    Future<Boolean> booleanFuture = initData();
    booleanFuture.setHandler(res -> {
      if (res.succeeded()) {
        startFuture.complete();
      } else {
        startFuture.fail("tagService init failure");
      }
    });
    
    
  }
  
  private void handleTagCreate(RoutingContext routingContext) {
    try {
    /*
      1. 获取tag_name参数
      2. 插入数据库
      3. 返回结果
     */
      HttpServerRequest request = routingContext.request();
      HttpServerResponse response = routingContext.response();
      @Nullable JsonObject bodyAsJson = routingContext.getBodyAsJson();
      String tag_name = bodyAsJson.getString("tag_name");
      if (tag_name == null) {
        badRequest(routingContext);
        return;
      }
    
      Tag tag = new Tag(UUID.randomUUID().toString(), tag_name);
      tagService.insert(tag).setHandler(resultHandler(routingContext, result -> {
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
  
  private void handleTagUpdate(RoutingContext routingContext) {
    try {
      HttpServerRequest request = routingContext.request();
      HttpServerResponse response = routingContext.response();
      String tagId = request.getParam("tag_id");
      if (tagId == null || "".equals(tagId)) {
        badRequest(routingContext);
        return;
      }
      JsonObject tagJson = routingContext.getBodyAsJson();
      Tag tag = new Tag(tagJson);
      tagService.update(tagId, tag).setHandler(resultHandler(routingContext, result -> {
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
  
  private void handleTagGetAll(RoutingContext routingContext) {
    try {
      HttpServerRequest request = routingContext.request();
      HttpServerResponse response = routingContext.response();
      int page = request.getParam("page") == null ?
          0 : Integer.parseInt(request.getParam("page"));
      int limit = request.getParam("limit") == null ?
          0 : Integer.parseInt(request.getParam("limit"));
      
      if (page == 0 && limit == 0) {
        tagService.getAll().setHandler(resultHandler(routingContext, tagList -> {
          warpResult(response, tagList);
        }));
      } else if (page >= 0 && limit >= 0) {
        tagService.range(page, limit).setHandler(resultHandler(routingContext, tagList -> {
          warpResult(response, tagList);
        }));
      } else {
        badRequest(routingContext);
      }
      
    } catch (NumberFormatException e) {
      badRequest(routingContext);
    }
  }
  
  private void warpResult(HttpServerResponse response, List<Tag> tagList) {
    Future<Long> totalCount = tagService.getTotalCount();
    totalCount.setHandler(res -> {
      if (res.succeeded()) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("data", tagList);
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
  
  private void handleTagDelete(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = request.response();
    String tag_id = request.getParam("tag_id");
    if (StringUtils.isEmpty(tag_id)) {
      badRequest(routingContext);
      return;
    }
    
    tagService.delete(tag_id).setHandler(resultHandler(routingContext, result -> {
      if (result) {
        response.setStatusCode(200).end();
      } else {
        response.setStatusCode(500).end();
      }
    }));
  }
  
  private void handleTagDeleteAll(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    tagService.deleteAll().setHandler(resultHandler(routingContext, result -> {
      if (result) {
        response.setStatusCode(200).end();
      } else {
        response.setStatusCode(500).end();
      }
    }));
  }
  
  private void handleTagGet(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    @Nullable String tag_id = request.getParam("tag_id");
    if (StringUtils.isEmpty(tag_id)) {
      badRequest(routingContext);
      return;
    }
    tagService.getCertain(tag_id).setHandler(resultHandler(routingContext, result -> {
      if (result.isPresent()) {
        Tag tag = result.get();
        String data = tag.toJson().toString();
        response.putHeader("content-type", "application/json");
        response.end(data);
      } else {
        badRequest(routingContext);
      }
    }));
    
  }
}
