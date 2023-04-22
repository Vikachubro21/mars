package mars.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import mars.Globals;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.MemoryAccessNotice;

public class MarsBot implements Observer, MarsTool {
   private static final int GRAPHIC_WIDTH = 512;
   private static final int GRAPHIC_HEIGHT = 512;
   private static final int ADDR_HEADING = -32752;
   private static final int ADDR_LEAVETRACK = -32736;
   private static final int ADDR_WHEREAREWEX = -32720;
   private static final int ADDR_WHEREAREWEY = -32704;
   private static final int ADDR_MOVE = -32688;
   private MarsBotDisplay graphicArea;
   private int MarsBotHeading = 0;
   private boolean MarsBotLeaveTrack = false;
   private double MarsBotXPosition = 0.0;
   private double MarsBotYPosition = 0.0;
   private boolean MarsBotMoving = false;
   private final int trackPts = 256;
   private Point[] arrayOfTrack = new Point[256];
   private int trackIndex = 0;

   public String getName() {
      return "Mars Bot";
   }

   public void action() {
      BotRunnable br1 = new BotRunnable();
      Thread t1 = new Thread(br1);
      t1.start();

      try {
         Globals.memory.addObserver(this, -32768, -32672);
      } catch (AddressErrorException var4) {
         System.out.println(var4);
      }

   }

   public void update(Observable o, Object arg) {
      if (arg instanceof MemoryAccessNotice) {
         MemoryAccessNotice notice = (MemoryAccessNotice)arg;
         int address = notice.getAddress();
         if (address < 0 && notice.getAccessType() == 1) {
            String message = "";
            if (address == -32752) {
               message = "MarsBot.update: got move heading value: ";
               this.MarsBotHeading = notice.getValue();
            } else if (address == -32736) {
               message = "MarsBot.update: got leave track directive value ";
               if (!this.MarsBotLeaveTrack && notice.getValue() == 1) {
                  this.MarsBotLeaveTrack = true;
                  this.arrayOfTrack[this.trackIndex] = new Point((int)this.MarsBotXPosition, (int)this.MarsBotYPosition);
                  ++this.trackIndex;
               } else if ((this.MarsBotLeaveTrack || notice.getValue() != 0) && (!this.MarsBotLeaveTrack || notice.getValue() != 1) && this.MarsBotLeaveTrack && notice.getValue() == 0) {
                  this.MarsBotLeaveTrack = false;
                  this.arrayOfTrack[this.trackIndex] = new Point((int)this.MarsBotXPosition, (int)this.MarsBotYPosition);
                  ++this.trackIndex;
               }
            } else if (address == -32688) {
               message = "MarsBot.update: got move control value: ";
               if (notice.getValue() == 0) {
                  this.MarsBotMoving = false;
               } else {
                  this.MarsBotMoving = true;
               }
            } else if (address != -32720 && address == -32704) {
            }
         }
      }

   }

   private class MarsBotDisplay extends JPanel {
      private int width;
      private int height;
      private boolean clearTheDisplay = true;

      public MarsBotDisplay(int tw, int th) {
         this.width = tw;
         this.height = th;
      }

      public void redraw() {
         this.repaint();
      }

      public void clear() {
         this.clearTheDisplay = true;
         this.repaint();
      }

      public void paintComponent(Graphics g) {
         Graphics2D g2 = (Graphics2D)g;
         g2.setColor(Color.blue);

         for(int i = 1; i <= MarsBot.this.trackIndex; i += 2) {
            try {
               g2.drawLine((int)MarsBot.this.arrayOfTrack[i - 1].getX(), (int)MarsBot.this.arrayOfTrack[i - 1].getY(), (int)MarsBot.this.arrayOfTrack[i].getX(), (int)MarsBot.this.arrayOfTrack[i].getY());
            } catch (ArrayIndexOutOfBoundsException var7) {
            } catch (NullPointerException var8) {
            }
         }

         g2.setColor(Color.black);
         g2.fillRect((int)MarsBot.this.MarsBotXPosition, (int)MarsBot.this.MarsBotYPosition, 20, 20);
      }
   }

   private class BotRunnable implements Runnable {
      JPanel panel;

      public BotRunnable() {
         final JFrame frame = new JFrame("Bot");
         this.panel = new JPanel(new BorderLayout());
         MarsBot.this.graphicArea = MarsBot.this.new MarsBotDisplay(512, 512);
         JPanel buttonPanel = new JPanel();
         JButton clearButton = new JButton("Clear");
         clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               MarsBot.this.graphicArea.clear();
               MarsBot.this.MarsBotLeaveTrack = false;
               MarsBot.this.MarsBotXPosition = 0.0;
               MarsBot.this.MarsBotYPosition = 0.0;
               MarsBot.this.MarsBotMoving = false;
               MarsBot.this.trackIndex = 0;
            }
         });
         buttonPanel.add(clearButton);
         JButton closeButton = new JButton("Close");
         closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               frame.setVisible(false);
            }
         });
         buttonPanel.add(closeButton);
         this.panel.add(MarsBot.this.graphicArea, "Center");
         this.panel.add(buttonPanel, "South");
         frame.getContentPane().add(this.panel);
         frame.pack();
         frame.setVisible(true);
         frame.setTitle(" This is the MarsBot");
         frame.setDefaultCloseOperation(2);
         frame.setSize(712, 612);
         frame.setVisible(true);
      }

      public void run() {
         while(true) {
            if (MarsBot.this.MarsBotMoving) {
               double tempAngle = (double)((360 - MarsBot.this.MarsBotHeading + 90) % 360);
               MarsBot.this.MarsBotXPosition = Math.cos(Math.toRadians(tempAngle));
               MarsBot.this.MarsBotYPosition = -Math.sin(Math.toRadians(tempAngle));

               try {
                  Globals.memory.setWord(-32720, (int)MarsBot.this.MarsBotXPosition);
                  Globals.memory.setWord(-32704, (int)MarsBot.this.MarsBotYPosition);
               } catch (AddressErrorException var5) {
               }

               MarsBot.this.arrayOfTrack[MarsBot.this.trackIndex] = new Point((int)MarsBot.this.MarsBotXPosition, (int)MarsBot.this.MarsBotYPosition);
            }

            try {
               Thread.sleep(40L);
            } catch (InterruptedException var4) {
            }

            this.panel.repaint();
         }
      }
   }
}
