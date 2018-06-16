package xin.beanan.blog.service;

import io.vertx.core.Future;
import xin.beanan.blog.entity.Article;

import java.util.List;


/**
 * @author BeanNan
 */
public interface ArticleService extends BaseService<Article> {

  Future<List<Article>> getLast(int start, int end);
}
