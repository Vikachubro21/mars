package mars.venus.editors.jeditsyntax;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Toolkit;
import mars.util.Binary;

public class SyntaxStyle {
   private Color color;
   private boolean italic;
   private boolean bold;
   private Font lastFont;
   private Font lastStyledFont;
   private FontMetrics fontMetrics;

   public SyntaxStyle(Color color, boolean italic, boolean bold) {
      this.color = color;
      this.italic = italic;
      this.bold = bold;
   }

   public Color getColor() {
      return this.color;
   }

   public String getColorAsHexString() {
      return Binary.intToHexString(this.color.getRed() << 16 | this.color.getGreen() << 8 | this.color.getBlue());
   }

   public boolean isPlain() {
      return !this.bold && !this.italic;
   }

   public boolean isItalic() {
      return this.italic;
   }

   public boolean isBold() {
      return this.bold;
   }

   public Font getStyledFont(Font font) {
      if (font == null) {
         throw new NullPointerException("font param must not be null");
      } else if (font.equals(this.lastFont)) {
         return this.lastStyledFont;
      } else {
         this.lastFont = font;
         this.lastStyledFont = new Font(font.getFamily(), (this.bold ? 1 : 0) | (this.italic ? 2 : 0), font.getSize());
         return this.lastStyledFont;
      }
   }

   public FontMetrics getFontMetrics(Font font) {
      if (font == null) {
         throw new NullPointerException("font param must not be null");
      } else if (font.equals(this.lastFont) && this.fontMetrics != null) {
         return this.fontMetrics;
      } else {
         this.lastFont = font;
         this.lastStyledFont = new Font(font.getFamily(), (this.bold ? 1 : 0) | (this.italic ? 2 : 0), font.getSize());
         this.fontMetrics = Toolkit.getDefaultToolkit().getFontMetrics(this.lastStyledFont);
         return this.fontMetrics;
      }
   }

   public void setGraphicsFlags(Graphics gfx, Font font) {
      Font _font = this.getStyledFont(font);
      gfx.setFont(_font);
      gfx.setColor(this.color);
   }

   public String toString() {
      return this.getClass().getName() + "[color=" + this.color + (this.italic ? ",italic" : "") + (this.bold ? ",bold" : "") + "]";
   }
}
