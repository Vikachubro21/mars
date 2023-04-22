package mars.tools;

public class ScreenMagnifier implements MarsTool {
   public String getName() {
      return "Screen Magnifier";
   }

   public void action() {
      new Magnifier();
   }

   public static void main(String[] args) {
      (new Thread(new Runnable() {
         public void run() {
            (new ScreenMagnifier()).action();
         }
      })).start();
   }
}
