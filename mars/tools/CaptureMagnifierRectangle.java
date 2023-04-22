package mars.tools;

import java.awt.Rectangle;

class CaptureMagnifierRectangle implements CaptureRectangleStrategy {
   public Rectangle getCaptureRectangle(Rectangle magnifierRectangle) {
      return magnifierRectangle;
   }
}
