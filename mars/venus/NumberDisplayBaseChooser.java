package mars.venus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import mars.Globals;
import mars.util.Binary;

public class NumberDisplayBaseChooser extends JCheckBox {
   public static final int DECIMAL = 10;
   public static final int HEXADECIMAL = 16;
   public static final int ASCII = 0;
   private int base;
   private JCheckBoxMenuItem settingMenuItem;

   public NumberDisplayBaseChooser(String text, boolean displayInHex) {
      super(text, displayInHex);
      this.base = getBase(displayInHex);
      this.addItemListener(new ItemListener() {
         public void itemStateChanged(ItemEvent ie) {
            NumberDisplayBaseChooser choose = (NumberDisplayBaseChooser)ie.getItem();
            if (ie.getStateChange() == 1) {
               choose.setBase(16);
            } else {
               choose.setBase(10);
            }

            if (NumberDisplayBaseChooser.this.settingMenuItem != null) {
               NumberDisplayBaseChooser.this.settingMenuItem.setSelected(choose.isSelected());
               ActionListener[] listeners = NumberDisplayBaseChooser.this.settingMenuItem.getActionListeners();
               ActionEvent event = new ActionEvent(NumberDisplayBaseChooser.this.settingMenuItem, 0, "chooser");

               for(int i = 0; i < listeners.length; ++i) {
                  listeners[i].actionPerformed(event);
               }
            }

            Globals.getGui().getMainPane().getExecutePane().numberDisplayBaseChanged(choose);
         }
      });
   }

   public int getBase() {
      return this.base;
   }

   public void setBase(int newBase) {
      if (newBase == 10 || newBase == 16) {
         this.base = newBase;
      }

   }

   public static String formatUnsignedInteger(int value, int base) {
      return base == 16 ? Binary.intToHexString(value) : Binary.unsignedIntToIntString(value);
   }

   public static String formatNumber(int value, int base) {
      String result;
      switch (base) {
         case 0:
            result = Binary.intToAscii(value);
            break;
         case 10:
            result = Integer.toString(value);
            break;
         case 16:
            result = Binary.intToHexString(value);
            break;
         default:
            result = Integer.toString(value);
      }

      return result;
   }

   public static String formatNumber(float value, int base) {
      return base == 16 ? Binary.intToHexString(Float.floatToIntBits(value)) : Float.toString(value);
   }

   public static String formatNumber(double value, int base) {
      if (base == 16) {
         long lguy = Double.doubleToLongBits(value);
         return Binary.intToHexString(Binary.highOrderLongToInt(lguy)) + Binary.intToHexString(Binary.lowOrderLongToInt(lguy)).substring(2);
      } else {
         return Double.toString(value);
      }
   }

   public String formatNumber(int value) {
      return this.base == 16 ? Binary.intToHexString(value) : (new Integer(value)).toString();
   }

   public String formatUnsignedInteger(int value) {
      return formatUnsignedInteger(value, this.base);
   }

   public static String formatFloatNumber(int value, int base) {
      return base == 16 ? Binary.intToHexString(value) : Float.toString(Float.intBitsToFloat(value));
   }

   public static String formatDoubleNumber(long value, int base) {
      return base == 16 ? Binary.longToHexString(value) : Double.toString(Double.longBitsToDouble(value));
   }

   public void setSettingsMenuItem(JCheckBoxMenuItem setter) {
      this.settingMenuItem = setter;
   }

   public static int getBase(boolean setting) {
      return setting ? 16 : 10;
   }
}
