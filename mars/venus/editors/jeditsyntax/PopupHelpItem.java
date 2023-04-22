package mars.venus.editors.jeditsyntax;

import java.util.ArrayList;

public class PopupHelpItem {
   private String tokenText;
   private String example;
   private String description;
   private boolean exact;
   private int exampleLength;
   private static final String spaces = "                                        ";

   public PopupHelpItem(String tokenText, String example, String description, boolean exact) {
      this.tokenText = tokenText;
      this.example = example;
      if (exact) {
         this.description = description;
      } else {
         int detailPosition = description.indexOf(":");
         this.description = detailPosition == -1 ? description : description.substring(0, detailPosition);
      }

      this.exampleLength = this.example.length();
      this.exact = exact;
   }

   public PopupHelpItem(String tokenText, String example, String description) {
      this(tokenText, example, description, true);
   }

   public String getTokenText() {
      return this.tokenText;
   }

   public String getExample() {
      return this.example;
   }

   public String getDescription() {
      return this.description;
   }

   public boolean getExact() {
      return this.exact;
   }

   public int getExampleLength() {
      return this.exampleLength;
   }

   public String getExamplePaddedToLength(int length) {
      String result = null;
      if (length > this.exampleLength) {
         int numSpaces = length - this.exampleLength;
         if (numSpaces > "                                        ".length()) {
            numSpaces = "                                        ".length();
         }

         result = this.example + "                                        ".substring(0, numSpaces);
      } else if (length == this.exampleLength) {
         result = this.example;
      } else {
         result = this.example.substring(0, length);
      }

      return result;
   }

   public void setExample(String example) {
      this.example = example;
      this.exampleLength = example.length();
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public static int maxExampleLength(ArrayList matches) {
      int length = 0;
      if (matches != null) {
         for(int i = 0; i < matches.size(); ++i) {
            Object match = matches.get(i);
            if (match instanceof PopupHelpItem) {
               length = Math.max(length, ((PopupHelpItem)match).getExampleLength());
            }
         }
      }

      return length;
   }
}
