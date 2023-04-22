package mars.venus;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPopupMenu;

public class PopupListener extends MouseAdapter {
   private JPopupMenu popup;

   public PopupListener(JPopupMenu p) {
      this.popup = p;
   }

   public void mousePressed(MouseEvent e) {
      this.maybeShowPopup(e);
   }

   public void mouseReleased(MouseEvent e) {
      this.maybeShowPopup(e);
   }

   private void maybeShowPopup(MouseEvent e) {
      if (e.isPopupTrigger()) {
         this.popup.show(e.getComponent(), e.getX(), e.getY());
      }

   }
}
