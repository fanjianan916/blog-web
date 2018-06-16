package xin.beanan.blog.entity;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import xin.beanan.blog.util.BeanUtils;
import xin.beanan.blog.util.DateUtils;

import java.util.Date;
import java.util.Objects;

/**
 * @author BeanNan
 */
@DataObject(generateConverter = true)
public class Category {
  
  private String category_id;
  private String category_name;
  private Long create_time;
  private Long update_time;
  
  public Category() {
  }
  
  public Category(String category_id, String category_name) {
    this.category_id = category_id;
    this.category_name = category_name;
    this.create_time = new Date().getTime();
    this.update_time = new Date().getTime();
  }
  
  public Category(String category_id, String category_name, Long create_time) {
    this.category_id = category_id;
    this.category_name = category_name;
    this.create_time = create_time;
    this.update_time = new Date().getTime();
  }
  
  public Category(JsonObject jsonObject) {
    CategoryConverter.fromJson(jsonObject, this);
  }
  
  public JsonArray toJsonArray() {
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(this.category_id);
    jsonArray.add(this.category_name);
    jsonArray.add(DateUtils.DATE_FORMAT.format(new Date(create_time)));
    jsonArray.add(DateUtils.DATE_FORMAT.format(new Date(update_time)));
    return jsonArray;
  }
  
  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    CategoryConverter.toJson(this, jsonObject);
    return jsonObject;
  }
  
  public String getCategory_id() {
    return category_id;
  }
  
  public void setCategory_id(String category_id) {
    this.category_id = category_id;
  }
  
  public String getCategory_name() {
    return category_name;
  }
  
  public void setCategory_name(String category_name) {
    this.category_name = category_name;
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
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Category)) return false;
    Category category = (Category) o;
    return Objects.equals(getCategory_id(), category.getCategory_id()) &&
        Objects.equals(getCategory_name(), category.getCategory_name()) &&
        Objects.equals(getCreate_time(), category.getCreate_time()) &&
        Objects.equals(getUpdate_time(), category.getUpdate_time());
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(getCategory_id(), getCategory_name(), getCreate_time(), getUpdate_time());
  }
  
  public Category merge(Category category) {
    return new Category(category_id
        , BeanUtils.getOrElse(category.category_name, category_name)
        , BeanUtils.getOrElse(category.create_time, create_time)
    );
  }
  
  @Override
  public String toString() {
    return toJson().toString();
  }
}
