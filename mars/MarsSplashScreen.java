package mars;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;

public class MarsSplashScreen extends JWindow {
   private int duration;

   public MarsSplashScreen(int d) {
      this.duration = d;
   }

   public void showSplash() {
      ImageBackgroundPanel content = new ImageBackgroundPanel();
      this.setContentPane(content);
      int width = 390;
      int height = 215;
      Toolkit tk = Toolkit.getDefaultToolkit();
      Dimension screen = tk.getScreenSize();
      int x = (screen.width - width) / 2;
      int y = (screen.height - height) / 2;
      this.setBounds(x, y, width, height);
      JLabel title = new JLabel("MARS: Mips Assembler and Runtime Simulator", 0);
      JLabel copyrt1 = new JLabel("<html><br><br>Version 4.5 Copyright (c) " + Globals.copyrightYears + "</html>", 0);
      JLabel copyrt2 = new JLabel("<html><br><br>" + Globals.copyrightHolders + "</html>", 0);
      title.setFont(new Font("Segoe UI", 1, 16));
      title.setForeground(Color.white);
      copyrt1.setFont(new Font("Segoe UI", 1, 14));
      copyrt2.setFont(new Font("Segoe UI", 1, 14));
      copyrt1.setForeground(Color.white);
      copyrt2.setForeground(Color.white);
      content.add(title, "North");
      content.add(copyrt1, "Center");
      content.add(copyrt2, "South");
      this.setVisible(true);

      try {
         Thread.sleep((long)this.duration);
      } catch (Exception var12) {
      }

      this.setVisible(false);
   }

   class ImageBackgroundPanel extends JPanel {
      Image image;

      public ImageBackgroundPanel() {
         try {
            this.image = (new ImageIcon(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/images/MarsSurfacePathfinder.jpg")))).getImage();
         } catch (Exception var3) {
            System.out.println(var3);
         }

      }

      protected void paintComponent(Graphics g) {
         super.paintComponent(g);
         if (this.image != null) {
            g.drawImage(this.image, 0, 0, this.getWidth(), this.getHeight(), this);
         }

      }
   }
}
