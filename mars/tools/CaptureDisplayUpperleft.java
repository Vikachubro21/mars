package mars.tools;

import javax.swing.JScrollBar;

class CaptureDisplayUpperleft implements CaptureDisplayAlignmentStrategy {
   public void setScrollBarValue(JScrollBar scrollBar) {
      scrollBar.setValue(0);
   }
}
