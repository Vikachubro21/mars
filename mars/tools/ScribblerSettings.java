package mars.tools;

import java.awt.Color;

class ScribblerSettings {
   private int width;
   private Color color;

   public ScribblerSettings(int width, Color color) {
      this.width = width;
      this.color = color;
   }

   public int getLineWidth() {
      return this.width;
   }

   public Color getLineColor() {
      return this.color;
   }

   public void setLineWidth(int newWidth) {
      this.width = newWidth;
   }

   public void setLineColor(Color newColor) {
      this.color = newColor;
   }
}
