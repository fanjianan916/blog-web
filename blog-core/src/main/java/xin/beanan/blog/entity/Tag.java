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
public class Tag {
  
  private String tag_id;
  private String tag_name;
  private Long create_time;
  private Long update_time;
  
  public Tag() {
  }
  
  public Tag(JsonObject jsonObject) {
    TagConverter.fromJson(jsonObject, this);
  }
  
  public Tag(String json) {
    JsonObject jsonObject = new JsonObject(json);
    TagConverter.fromJson(jsonObject, this);
  }
  
  public Tag(String tag_id, String tag_name) {
    this.tag_id = tag_id;
    this.tag_name = tag_name;
    this.create_time = new Date().getTime();
    this.update_time = new Date().getTime();
  }
  
  public Tag(String tag_id, String tag_name, Long create_time) {
    this.tag_id = tag_id;
    this.tag_name = tag_name;
    this.create_time = create_time;
    this.update_time = new Date().getTime();
  }
  
  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    TagConverter.toJson(this, jsonObject);
    return jsonObject;
  }
  
  public JsonArray toJsonArray() {
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(this.tag_id);
    jsonArray.add(this.tag_name);
    jsonArray.add(DateUtils.DATE_FORMAT.format(new Date(create_time)));
    jsonArray.add(DateUtils.DATE_FORMAT.format(new Date(update_time)));
    return jsonArray;
  }
  
  public String getTag_id() {
    return tag_id;
  }
  
  public void setTag_id(String tag_id) {
    this.tag_id = tag_id;
  }
  
  public String getTag_name() {
    return tag_name;
  }
  
  public void setTag_name(String tag_name) {
    this.tag_name = tag_name;
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
    if (!(o instanceof Tag)) return false;
    Tag tag = (Tag) o;
    return Objects.equals(getTag_id(), tag.getTag_id()) &&
        Objects.equals(getTag_name(), tag.getTag_name()) &&
        Objects.equals(getCreate_time(), tag.getCreate_time()) &&
        Objects.equals(getUpdate_time(), tag.getUpdate_time());
  }
  
  @Override
  public int hashCode() {
    
    return Objects.hash(getTag_id(), getTag_name(), getCreate_time(), getUpdate_time());
  }
  
  @Override
  public String toString() {
    return toJson().toString();
  }
  
  public Tag merge(Tag tag) {
    return new Tag(tag_id
        , BeanUtils.getOrElse(tag.tag_name, tag_name)
        , BeanUtils.getOrElse(tag.create_time, create_time)
    );
  }
}
