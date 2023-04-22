package mars.venus.editors.jeditsyntax;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

public class TextUtilities {
   public static int findMatchingBracket(Document doc, int offset) throws BadLocationException {
      if (doc.getLength() == 0) {
         return -1;
      } else {
         char c = doc.getText(offset, 1).charAt(0);
         byte cprime;
         boolean direction;
         switch (c) {
            case '(':
               cprime = 41;
               direction = false;
               break;
            case ')':
               cprime = 40;
               direction = true;
               break;
            case '[':
               cprime = 93;
               direction = false;
               break;
            case ']':
               cprime = 91;
               direction = true;
               break;
            case '{':
               cprime = 125;
               direction = false;
               break;
            case '}':
               cprime = 123;
               direction = true;
               break;
            default:
               return -1;
         }

         int count;
         int i;
         if (direction) {
            count = 1;
            String text = doc.getText(0, offset);

            for(i = offset - 1; i >= 0; --i) {
               i = text.charAt(i);
               if (i == c) {
                  ++count;
               } else if (i == cprime) {
                  --count;
                  if (count == 0) {
                     return i;
                  }
               }
            }
         } else {
            count = 1;
            ++offset;
            int len = doc.getLength() - offset;
            String text = doc.getText(offset, len);

            for(i = 0; i < len; ++i) {
               char x = text.charAt(i);
               if (x == c) {
                  ++count;
               } else if (x == cprime) {
                  --count;
                  if (count == 0) {
                     return i + offset;
                  }
               }
            }
         }

         return -1;
      }
   }

   public static int findWordStart(String line, int pos, String noWordSep) {
      char ch = line.charAt(pos - 1);
      if (noWordSep == null) {
         noWordSep = "";
      }

      boolean selectNoLetter = !Character.isLetterOrDigit(ch) && noWordSep.indexOf(ch) == -1;
      int wordStart = 0;

      for(int i = pos - 1; i >= 0; --i) {
         ch = line.charAt(i);
         if (selectNoLetter ^ (!Character.isLetterOrDigit(ch) && noWordSep.indexOf(ch) == -1)) {
            wordStart = i + 1;
            break;
         }
      }

      return wordStart;
   }

   public static int findWordEnd(String line, int pos, String noWordSep) {
      char ch = line.charAt(pos);
      if (noWordSep == null) {
         noWordSep = "";
      }

      boolean selectNoLetter = !Character.isLetterOrDigit(ch) && noWordSep.indexOf(ch) == -1;
      int wordEnd = line.length();

      for(int i = pos; i < line.length(); ++i) {
         ch = line.charAt(i);
         if (selectNoLetter ^ (!Character.isLetterOrDigit(ch) && noWordSep.indexOf(ch) == -1)) {
            wordEnd = i;
            break;
         }
      }

      return wordEnd;
   }
}
