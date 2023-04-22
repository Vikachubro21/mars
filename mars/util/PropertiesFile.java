package mars.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesFile {
   public static Properties loadPropertiesFromFile(String file) {
      Properties properties = new Properties();

      try {
         InputStream is = PropertiesFile.class.getResourceAsStream("/" + file + ".properties");
         properties.load(is);
      } catch (IOException var3) {
      } catch (NullPointerException var4) {
      }

      return properties;
   }
}
