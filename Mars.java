import javax.swing.UIManager;
import mars.MarsLaunch;

public class Mars {
   public static void main(String[] args) {
      try {
         UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
      } catch (Exception var2) {
         System.out.println("Unable to use Windows theme");
      }

      new MarsLaunch(args);
   }
}
