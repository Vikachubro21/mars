package mars.tools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Line2D;
import java.awt.image.ImageObserver;
import javax.swing.JPanel;

class MagnifierImage extends JPanel {
   private Magnifier frame;
   private Rectangle screenRectangle;
   private static Robot robot;
   private Image image;
   private Scribbler scribbler;

   public MagnifierImage(Magnifier frame) {
      this.frame = frame;
      this.scribbler = new Scribbler(frame.scribblerSettings);
      this.addMouseListener(new MouseAdapter() {
         public void mousePressed(MouseEvent e) {
            MagnifierImage.this.scribbler.moveto(e.getX(), e.getY());
         }
      });
      this.addMouseMotionListener(new MouseMotionAdapter() {
         public void mouseDragged(MouseEvent e) {
            MagnifierImage.this.scribbler.lineto(e.getX(), e.getY(), (Graphics2D)MagnifierImage.this.getGraphics());
         }
      });
   }

   public Image getImage() {
      return this.image;
   }

   public void paintComponent(Graphics g) {
      super.paintComponent(g);
      if (this.image != null) {
         g.drawImage(this.image, 0, 0, this);
      }

   }

   public void setImage(Image image) {
      this.image = image;
      this.setPreferredSize(new Dimension(image.getWidth(this), image.getHeight(this)));
      this.revalidate();
      this.repaint();
   }

   static Image getScaledImage(Image image, double scale, int scaleAlgorithm) {
      return scale < 1.01 && scale > 0.99 ? image : image.getScaledInstance((int)((double)image.getWidth((ImageObserver)null) * scale), (int)((double)image.getHeight((ImageObserver)null) * scale), scaleAlgorithm);
   }

   static Image getScaledImage(Image image, double scale) {
      return getScaledImage(image, scale, 1);
   }

   private class Scribbler {
      private ScribblerSettings scribblerSettings;
      private BasicStroke drawingStroke;
      protected int last_x;
      protected int last_y;

      Scribbler(ScribblerSettings scribblerSettings) {
         this.scribblerSettings = scribblerSettings;
         this.drawingStroke = new BasicStroke((float)scribblerSettings.getLineWidth());
      }

      public Color getColor() {
         return this.scribblerSettings.getLineColor();
      }

      public int getLineWidth() {
         this.drawingStroke = new BasicStroke((float)this.scribblerSettings.getLineWidth());
         return this.scribblerSettings.getLineWidth();
      }

      public void setColor(Color newColor) {
         this.scribblerSettings.setLineColor(newColor);
      }

      public void setLineWidth(int newWidth) {
         this.scribblerSettings.setLineWidth(newWidth);
         this.drawingStroke = new BasicStroke((float)newWidth);
      }

      private BasicStroke getStroke() {
         return this.drawingStroke;
      }

      private void setStroke(BasicStroke newStroke) {
         this.drawingStroke = newStroke;
      }

      public void moveto(int x, int y) {
         this.last_x = x;
         this.last_y = y;
      }

      public void lineto(int x, int y, Graphics2D g2d) {
         g2d.setStroke(new BasicStroke((float)this.scribblerSettings.getLineWidth()));
         g2d.setColor(this.scribblerSettings.getLineColor());
         g2d.draw(new Line2D.Float((float)this.last_x, (float)this.last_y, (float)x, (float)y));
         this.moveto(x, y);
      }
   }
}
