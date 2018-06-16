package xin.beanan.blog.entity;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import xin.beanan.blog.util.DateUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author BeanNan
 */
@DataObject(generateConverter = true)
public class Article {
  
  private String article_id;
  private String article_name;
  private String article_content_pos;
  private Long create_time;
  private Long update_time;
  private Integer read_num;
  private Category category;
  private List<Tag> tags;
  private String content;
  
  public Article() {
  }
  
  public Article(String article_id, String article_name, Category category, List<Tag> tags, String content) {
    this.article_id = article_id;
    this.article_name = article_name;
    this.category = category;
    this.tags = tags;
    this.create_time = new Date().getTime();
    this.update_time = new Date().getTime();
    this.read_num = 0;
    this.content = content;
  }
  
  public Article(JsonObject jsonObject) {
    ArticleConverter.fromJson(jsonObject, this);
  }
  
  public Article(String article_id,
                 String article_name,
                 String article_content_pos,
                 Integer read_num,
                 Category category,
                 List<Tag> tags) {
    this.article_id = article_id;
    this.article_name = article_name;
    this.article_content_pos = article_content_pos;
    this.create_time = new Date().getTime();
    this.update_time = new Date().getTime();
    this.read_num = read_num;
    this.category = category;
    this.tags = tags;
  }
  
  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    ArticleConverter.toJson(this, jsonObject);
    return jsonObject;
  }
  
  public String getArticle_id() {
    return article_id;
  }
  
  public void setArticle_id(String article_id) {
    this.article_id = article_id;
  }
  
  public String getArticle_name() {
    return article_name;
  }
  
  public void setArticle_name(String article_name) {
    this.article_name = article_name;
  }
  
  public String getArticle_content_pos() {
    return article_content_pos;
  }
  
  public void setArticle_content_pos(String article_content_pos) {
    this.article_content_pos = article_content_pos;
  }
  
  public Long getCreate_time() {
    return create_time;
  }
  
  public void setCreate_time(Long create_time) {
    this.create_time = create_time;
  }
  
  public Long getUpdate_time() {
    return update_time;
  }
  
  public void setUpdate_time(Long update_time) {
    this.update_time = update_time;
  }
  
  public Integer getRead_num() {
    return read_num;
  }
  
  public void setRead_num(Integer read_num) {
    this.read_num = read_num;
  }
  
  public Category getCategory() {
    return category;
  }
  
  public void setCategory(Category category) {
    this.category = category;
  }
  
  public List<Tag> getTags() {
    return tags;
  }
  
  public void setTags(List<Tag> tags) {
    this.tags = tags;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Article)) return false;
    Article article = (Article) o;
    return Objects.equals(getArticle_id(), article.getArticle_id()) &&
        Objects.equals(getArticle_name(), article.getArticle_name()) &&
        Objects.equals(getArticle_content_pos(), article.getArticle_content_pos()) &&
        Objects.equals(getCreate_time(), article.getCreate_time()) &&
        Objects.equals(getUpdate_time(), article.getUpdate_time()) &&
        Objects.equals(getRead_num(), article.getRead_num()) &&
        Objects.equals(getCategory(), article.getCategory()) &&
        Objects.equals(getTags(), article.getTags());
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(getArticle_id(), getArticle_name(), getArticle_content_pos(), getCreate_time(), getUpdate_time(), getRead_num(), getCategory(), getTags());
  }
  
  public JsonArray toJsonArray() {
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(article_id);
    jsonArray.add(article_name);
    jsonArray.add(article_content_pos);
    jsonArray.add(DateUtils.DATE_FORMAT.format(new Date(create_time)));
    jsonArray.add(DateUtils.DATE_FORMAT.format(new Date(update_time)));
    jsonArray.add(read_num);
    jsonArray.add(category.getCategory_id());
    return jsonArray;
  }
  
  public String getContent() {
    return content;
  }
  
  public void setContent(String content) {
    this.content = content;
  }
}
  

