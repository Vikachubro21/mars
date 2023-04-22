package mars.venus;

import java.awt.Component;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

class MonoRightCellRenderer extends DefaultTableCellRenderer {
   public static final Font MONOSPACED_PLAIN_12POINT = new Font("Lucida Sans Typewriter", 0, 12);

   public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      JLabel cell = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      cell.setFont(MONOSPACED_PLAIN_12POINT);
      cell.setHorizontalAlignment(4);
      return cell;
   }
}
