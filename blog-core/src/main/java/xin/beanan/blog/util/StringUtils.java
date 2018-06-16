package xin.beanan.blog.util;

/**
 * @author BeanNan
 */
public class StringUtils {
  
  private StringUtils() {}
  
  public static boolean isEmpty(String value) {
    return value == null || "".equals(value.trim());
  }
}
