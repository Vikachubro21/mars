package mars.venus.editors.jeditsyntax;

import javax.swing.text.Segment;

public class KeywordMap {
   protected int mapLength;
   private Keyword[] map;
   private boolean ignoreCase;

   public KeywordMap(boolean ignoreCase) {
      this(ignoreCase, 52);
      this.ignoreCase = ignoreCase;
   }

   public KeywordMap(boolean ignoreCase, int mapLength) {
      this.mapLength = mapLength;
      this.ignoreCase = ignoreCase;
      this.map = new Keyword[mapLength];
   }

   public byte lookup(Segment text, int offset, int length) {
      if (length == 0) {
         return 0;
      } else if (text.array[offset] == '%') {
         return 11;
      } else {
         Keyword k = this.map[this.getSegmentMapKey(text, offset, length)];

         while(k != null) {
            if (length != k.keyword.length) {
               k = k.next;
            } else {
               if (SyntaxUtilities.regionMatches(this.ignoreCase, text, offset, k.keyword)) {
                  return k.id;
               }

               k = k.next;
            }
         }

         return 0;
      }
   }

   public void add(String keyword, byte id) {
      int key = this.getStringMapKey(keyword);
      this.map[key] = new Keyword(keyword.toCharArray(), id, this.map[key]);
   }

   public boolean getIgnoreCase() {
      return this.ignoreCase;
   }

   public void setIgnoreCase(boolean ignoreCase) {
      this.ignoreCase = ignoreCase;
   }

   protected int getStringMapKey(String s) {
      return (Character.toUpperCase(s.charAt(0)) + Character.toUpperCase(s.charAt(s.length() - 1))) % this.mapLength;
   }

   protected int getSegmentMapKey(Segment s, int off, int len) {
      return (Character.toUpperCase(s.array[off]) + Character.toUpperCase(s.array[off + len - 1])) % this.mapLength;
   }

   class Keyword {
      public char[] keyword;
      public byte id;
      public Keyword next;

      public Keyword(char[] keyword, byte id, Keyword next) {
         this.keyword = keyword;
         this.id = id;
         this.next = next;
      }
   }
}
