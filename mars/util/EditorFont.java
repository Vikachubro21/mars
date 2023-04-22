package mars.util;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import mars.Globals;

public class EditorFont {
   private static final String[] styleStrings = new String[]{"Plain", "Bold", "Italic", "Bold + Italic"};
   private static final int[] styleInts = new int[]{0, 1, 2, 3};
   public static final String DEFAULT_STYLE_STRING;
   public static final int DEFAULT_STYLE_INT;
   public static final int MIN_SIZE = 6;
   public static final int MAX_SIZE = 72;
   public static final int DEFAULT_SIZE = 12;
   private static final String[] allCommonFamilies;
   private static final String TAB_STRING = "\t";
   private static final char TAB_CHAR = '\t';
   private static final String SPACES = "                                                  ";
   private static final String[] commonFamilies;

   static {
      DEFAULT_STYLE_STRING = styleStrings[0];
      DEFAULT_STYLE_INT = styleInts[0];
      allCommonFamilies = new String[]{"Arial", "Monospaced", "Georgia", "Lucida Sans Typewriter", "Times New Roman", "Verdana"};
      commonFamilies = actualCommonFamilies();
   }

   public static String[] getCommonFamilies() {
      return commonFamilies;
   }

   public static String[] getAllFamilies() {
      return GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
   }

   public static String[] getFontStyleStrings() {
      return styleStrings;
   }

   public static int styleStringToStyleInt(String style) {
      String styleLower = style.toLowerCase();

      for(int i = 0; i < styleStrings.length; ++i) {
         if (styleLower.equals(styleStrings[i].toLowerCase())) {
            return styleInts[i];
         }
      }

      return DEFAULT_STYLE_INT;
   }

   public static String styleIntToStyleString(int style) {
      for(int i = 0; i < styleInts.length; ++i) {
         if (style == styleInts[i]) {
            return styleStrings[i];
         }
      }

      return DEFAULT_STYLE_STRING;
   }

   public static String sizeIntToSizeString(int size) {
      int result = size < 6 ? 6 : (size > 72 ? 72 : size);
      return String.valueOf(result);
   }

   public static int sizeStringToSizeInt(String size) {
      int result = 12;

      try {
         result = Integer.parseInt(size);
      } catch (NumberFormatException var3) {
      }

      return result < 6 ? 6 : (result > 72 ? 72 : result);
   }

   public static Font createFontFromStringValues(String family, String style, String size) {
      return new Font(family, styleStringToStyleInt(style), sizeStringToSizeInt(size));
   }

   public static String substituteSpacesForTabs(String string) {
      return substituteSpacesForTabs(string, Globals.getSettings().getEditorTabSize());
   }

   public static String substituteSpacesForTabs(String string, int tabSize) {
      if (!string.contains("\t")) {
         return string;
      } else {
         StringBuffer result = new StringBuffer(string);

         for(int i = 0; i < result.length(); ++i) {
            if (result.charAt(i) == '\t') {
               result.replace(i, i + 1, "                                                  ".substring(0, tabSize - i % tabSize));
            }
         }

         return result.toString();
      }
   }

   private static String[] actualCommonFamilies() {
      String[] result = new String[allCommonFamilies.length];
      String[] availableFamilies = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
      Arrays.sort(availableFamilies);
      int k = 0;

      for(int i = 0; i < allCommonFamilies.length; ++i) {
         if (Arrays.binarySearch(availableFamilies, allCommonFamilies[i]) >= 0) {
            result[k++] = allCommonFamilies[i];
         }
      }

      if (k < allCommonFamilies.length) {
         String[] temp = new String[k];
         System.arraycopy(result, 0, temp, 0, k);
         result = temp;
      }

      return result;
   }
}
