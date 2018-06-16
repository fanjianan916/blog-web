package xin.beanan.blog.util;

/**
 * @author BeanNan
 */
public class BeanUtils {
  
  private BeanUtils() {
  }
  
  public static <T> T getOrElse(T value, T defaultValue) {
    return value == null ? defaultValue : value;
  }
}
