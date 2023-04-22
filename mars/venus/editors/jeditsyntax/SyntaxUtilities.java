package mars.venus.editors.jeditsyntax;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import javax.swing.Popup;
import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import javax.swing.text.Utilities;
import mars.Globals;
import mars.venus.editors.jeditsyntax.tokenmarker.Token;

public class SyntaxUtilities {
   public static boolean popupShowing = false;
   public static Popup popup;

   public static boolean regionMatches(boolean ignoreCase, Segment text, int offset, String match) {
      int length = offset + match.length();
      char[] textArray = text.array;
      if (length > text.offset + text.count) {
         return false;
      } else {
         int i = offset;

         for(int j = 0; i < length; ++j) {
            char c1 = textArray[i];
            char c2 = match.charAt(j);
            if (ignoreCase) {
               c1 = Character.toUpperCase(c1);
               c2 = Character.toUpperCase(c2);
            }

            if (c1 != c2) {
               return false;
            }

            ++i;
         }

         return true;
      }
   }

   public static boolean regionMatches(boolean ignoreCase, Segment text, int offset, char[] match) {
      int length = offset + match.length;
      char[] textArray = text.array;
      if (length > text.offset + text.count) {
         return false;
      } else {
         int i = offset;

         for(int j = 0; i < length; ++j) {
            char c1 = textArray[i];
            char c2 = match[j];
            if (ignoreCase) {
               c1 = Character.toUpperCase(c1);
               c2 = Character.toUpperCase(c2);
            }

            if (c1 != c2) {
               return false;
            }

            ++i;
         }

         return true;
      }
   }

   public static SyntaxStyle[] getDefaultSyntaxStyles(boolean darkTheme) {
      SyntaxStyle[] styles;
      if(darkTheme)
         styles = new SyntaxStyle[]{new SyntaxStyle(Color.white, false, false), new SyntaxStyle(new Color(52275), true, false), new SyntaxStyle(new Color(10027059), true, false), new SyntaxStyle(new Color(52275), false, false), new SyntaxStyle(new Color(52275), false, false), new SyntaxStyle(Color.white, true, false), new SyntaxStyle(Color.blue, false, false), new SyntaxStyle(Color.magenta, false, false), new SyntaxStyle(Color.red, false, false), new SyntaxStyle(Color.white, false, true), new SyntaxStyle(Color.red, false, false), new SyntaxStyle(new Color(150, 150, 0), false, false)};
      else
         styles = new SyntaxStyle[]{new SyntaxStyle(Color.black, false, false), new SyntaxStyle(new Color(52275), true, false), new SyntaxStyle(new Color(10027059), true, false), new SyntaxStyle(new Color(52275), false, false), new SyntaxStyle(new Color(52275), false, false), new SyntaxStyle(Color.black, true, false), new SyntaxStyle(Color.blue, false, false), new SyntaxStyle(Color.magenta, false, false), new SyntaxStyle(Color.red, false, false), new SyntaxStyle(Color.black, false, true), new SyntaxStyle(Color.red, false, false), new SyntaxStyle(new Color(150, 150, 0), false, false)};
      return styles;
   }

   public static SyntaxStyle[] getCurrentSyntaxStyles() {
      SyntaxStyle[] styles = new SyntaxStyle[]{Globals.getSettings().getEditorSyntaxStyleByPosition(0), Globals.getSettings().getEditorSyntaxStyleByPosition(1), Globals.getSettings().getEditorSyntaxStyleByPosition(2), Globals.getSettings().getEditorSyntaxStyleByPosition(3), Globals.getSettings().getEditorSyntaxStyleByPosition(4), Globals.getSettings().getEditorSyntaxStyleByPosition(5), Globals.getSettings().getEditorSyntaxStyleByPosition(6), Globals.getSettings().getEditorSyntaxStyleByPosition(7), Globals.getSettings().getEditorSyntaxStyleByPosition(8), Globals.getSettings().getEditorSyntaxStyleByPosition(9), Globals.getSettings().getEditorSyntaxStyleByPosition(10), Globals.getSettings().getEditorSyntaxStyleByPosition(11)};
      return styles;
   }

   public static int paintSyntaxLine(Segment line, Token tokens, SyntaxStyle[] styles, TabExpander expander, Graphics gfx, int x, int y) {
      Font defaultFont = gfx.getFont();
      Color defaultColor = gfx.getColor();
      int offset = 0;

      while(true) {
         byte id = tokens.id;
         if (id == 127) {
            return x;
         }

         int length = tokens.length;
         if (id == 0) {
            if (!defaultColor.equals(gfx.getColor())) {
               gfx.setColor(defaultColor);
            }

            if (!defaultFont.equals(gfx.getFont())) {
               gfx.setFont(defaultFont);
            }
         } else {
            styles[id].setGraphicsFlags(gfx, defaultFont);
         }

         line.count = length;
         if (id == 6) {
         }

         x = Utilities.drawTabbedText(line, x, y, gfx, expander, 0);
         line.offset += length;
         offset += length;
         tokens = tokens.next;
      }
   }

   private SyntaxUtilities() {
   }
}
