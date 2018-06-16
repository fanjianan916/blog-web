package xin.beanan.blog.service;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * @author BeanNan
 */
@ProxyGen
public interface IncrService {
  
  void incrArticleReadNumToDatabase(String article_id, Handler<AsyncResult<Void>> result);
}
