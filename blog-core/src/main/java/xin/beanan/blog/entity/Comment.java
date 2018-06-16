package xin.beanan.blog.entity;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.util.Date;
import java.util.Objects;

/**
 * @author BeanNan
 */
@DataObject(generateConverter = true)
public class Comment {
  
  private String comment_id;
  private String user_name;
  private String user_mail;
  private String comment_content;
  private String article_id;
  private Long create_time;
  
  public Comment() {
  }
  
  public Comment(JsonObject jsonObject) {
    CommentConverter.fromJson(jsonObject, this);
  }
  
  public Comment(String comment_id, String user_name, String user_mail, String comment_content, String article_id) {
    this.comment_id = comment_id;
    this.user_name = user_name;
    this.user_mail = user_mail;
    this.comment_content = comment_content;
    this.article_id = article_id;
    this.create_time = new Date().getTime();
  }
  
  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    CommentConverter.toJson(this, jsonObject);
    return jsonObject;
  }
  
  public String getComment_id() {
    return comment_id;
  }
  
  public void setComment_id(String comment_id) {
    this.comment_id = comment_id;
  }
  
  public String getUser_name() {
    return user_name;
  }
  
  public void setUser_name(String user_name) {
    this.user_name = user_name;
  }
  
  public String getUser_mail() {
    return user_mail;
  }
  
  public void setUser_mail(String user_mail) {
    this.user_mail = user_mail;
  }
  
  public String getComment_content() {
    return comment_content;
  }
  
  public void setComment_content(String comment_content) {
    this.comment_content = comment_content;
  }
  
  public String getArticle_id() {
    return article_id;
  }
  
  public void setArticle_id(String article_id) {
    this.article_id = article_id;
  }
  
  public Long getCreate_time() {
    return create_time;
  }
  
  public void setCreate_time(Long create_time) {
    this.create_time = create_time;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Comment)) return false;
    Comment comment = (Comment) o;
    return Objects.equals(getComment_id(), comment.getComment_id()) &&
        Objects.equals(getUser_name(), comment.getUser_name()) &&
        Objects.equals(getUser_mail(), comment.getUser_mail()) &&
        Objects.equals(getComment_content(), comment.getComment_content()) &&
        Objects.equals(getArticle_id(), comment.getArticle_id()) &&
        Objects.equals(getCreate_time(), comment.getCreate_time());
  }
  
  @Override
  public int hashCode() {
    
    return Objects.hash(getComment_id(), getUser_name(), getUser_mail(), getComment_content(), getArticle_id(), getCreate_time());
  }
}
