package mars.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import mars.Globals;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.MemoryAccessNotice;
import mars.util.Binary;

public class ScavengerHunt implements Observer, MarsTool {
   private static final int GRAPHIC_WIDTH = 712;
   private static final int GRAPHIC_HEIGHT = 652;
   private static final int NUM_PLAYERS = 22;
   private static final int MAX_X_MOVEMENT = 2;
   private static final int MAX_Y_MOVEMENT = 2;
   private static final double MAX_MOVE_DISTANCE = 2.5;
   private static final int ENERGY_AWARD = 20;
   private static final int ENERGY_PER_MOVE = 1;
   private static final int SIZE_OF_TASK = 20;
   private static final int NUM_LOCATIONS = 7;
   private static final int START_AND_END_LOCATION = 255;
   private static final int ADMINISTRATOR_ID = 999;
   private static final int ADDR_AUTHENTICATION = -8192;
   private static final int ADDR_PLAYER_ID = -8188;
   private static final int ADDR_GAME_ON = -8184;
   private static final int ADDR_NUM_TURNS = -8180;
   private static final int ADDR_BASE = -32768;
   private static final int MEM_PER_PLAYER = 1024;
   private static final int OFFSET_WHERE_AM_I_X = 0;
   private static final int OFFSET_WHERE_AM_I_Y = 4;
   private static final int OFFSET_MOVE_TO_X = 8;
   private static final int OFFSET_MOVE_TO_Y = 12;
   private static final int OFFSET_MOVE_READY = 16;
   private static final int OFFSET_ENERGY = 20;
   private static final int OFFSET_NUMBER_LOCATIONS = 24;
   private static final int OFFSET_PLAYER_COLOR = 28;
   private static final int OFFSET_SIZE_OF_TASK = 32;
   private static final int OFFSET_LOC_ARRAY = 36;
   private static final int OFFSET_TASK_COMPLETE = 292;
   private static final int OFFSET_TASK_ARRAY = 296;
   private ScavengerHuntDisplay graphicArea;
   private int authenticationValue = 0;
   private boolean GameOn = false;
   private static int SetWordCounter = 0;
   private static int accessCounter = 0;
   private static int playerID = 999;
   private boolean KENVDEBUG = false;
   private static PlayerData[] pd = new PlayerData[22];
   private static Location[] loc = new Location[7];
   private Random randomStream;
   private long startTime;

   public String getName() {
      return "ScavengerHunt";
   }

   public void action() {
      ScavengerHuntRunnable shr = new ScavengerHuntRunnable();
      Thread t1 = new Thread(shr);
      t1.start();

      try {
         Globals.memory.addObserver(this, -32768, -16);
      } catch (AddressErrorException var4) {
         System.out.println("\n\nScavengerHunt.action: Globals.memory.addObserver caused AddressErrorException.\n\n");
         System.exit(0);
      }

   }

   public void update(Observable o, Object arg) {
      if (arg instanceof MemoryAccessNotice) {
         MemoryAccessNotice notice = (MemoryAccessNotice)arg;
         int address = notice.getAddress();
         int data = notice.getValue();
         boolean isWrite = notice.getAccessType() == 1;
         boolean isRead = !isWrite;
         if (isWrite) {
            if (isWrite && playerID == 999 && address == -8184) {
               this.GameOn = true;
               this.initializeScavengerData();
            } else if ((!isWrite || address != -8192) && (!isWrite || address != -8180)) {
               if (isWrite && address == -8188) {
                  ++this.authenticationValue;
                  if (this.toolGetWord(-8192) == this.authenticationValue) {
                     playerID = this.toolGetWord(-8188);
                  } else {
                     System.out.println("ScavengerHunt.update(): Invalid write of player ID! \nPlayer " + playerID + " tried to write.  Expected:   " + Binary.intToHexString(this.authenticationValue) + ", got:  " + Binary.intToHexString(this.toolGetWord(-8192)) + "\n");
                  }
               } else {
                  int i;
                  if (isWrite && address == Short.MIN_VALUE + playerID * 1024 + 16 && data != 0) {
                     int energyLevel = this.toolReadPlayerData(playerID, 20);
                     if (energyLevel <= 0) {
                        return;
                     }

                     if (this.toolReadPlayerData(playerID, 8) < 0 || this.toolReadPlayerData(playerID, 8) > 712 || this.toolReadPlayerData(playerID, 12) < 0 || this.toolReadPlayerData(playerID, 12) > 652) {
                        System.out.println("Player " + playerID + " can't move -- out of bounds.");
                        return;
                     }

                     if (!(Math.sqrt(Math.pow((double)(this.toolReadPlayerData(playerID, 0) - this.toolReadPlayerData(playerID, 8)), 2.0) + Math.pow((double)(this.toolReadPlayerData(playerID, 4) - this.toolReadPlayerData(playerID, 12)), 2.0)) <= 2.5)) {
                        System.out.println("Player " + playerID + " can't move -- exceeded max. movement.");
                        System.out.println("    Player is at (" + this.toolReadPlayerData(playerID, 0) + ", " + this.toolReadPlayerData(playerID, 4) + "), wants to go to (" + this.toolReadPlayerData(playerID, 8) + "," + this.toolReadPlayerData(playerID, 12) + ")");
                        return;
                     }

                     this.toolWritePlayerData(playerID, 0, this.toolReadPlayerData(playerID, 8));
                     this.toolWritePlayerData(playerID, 4, this.toolReadPlayerData(playerID, 12));
                     pd[playerID].setWhereAmI(this.toolReadPlayerData(playerID, 0), this.toolReadPlayerData(playerID, 4));
                     this.toolWritePlayerData(playerID, 20, this.toolReadPlayerData(playerID, 20) - 1);
                     pd[playerID].setEnergy(this.toolReadPlayerData(playerID, 20));

                     for(i = 0; i < 7; ++i) {
                        if (this.toolReadPlayerData(playerID, 0) == loc[i].X && this.toolReadPlayerData(playerID, 4) == loc[i].Y) {
                           pd[playerID].setVisited(i);
                        }
                     }

                     i = playerID;
                     playerID = 999;
                     this.toolWritePlayerData(i, 16, 0);
                     playerID = i;
                  } else if (isWrite && address == Short.MIN_VALUE + playerID * 1024 + 292 && data != 0) {
                     i = this.toolReadPlayerData(playerID, 296);

                     int j;
                     for(j = 1; j < 20; ++j) {
                        int currentData = this.toolReadPlayerData(playerID, 296 + j * 4);
                        if (i > currentData) {
                           System.out.println("Whoops! Player has NOT completed task correctly");
                           return;
                        }

                        i = currentData;
                     }

                     this.toolWritePlayerData(playerID, 20, 20);
                     this.toolWritePlayerData(playerID, 292, 0);

                     for(j = 0; j < 20; ++j) {
                        this.toolWritePlayerData(playerID, 296 + j * 4, (int)(this.randomStream.nextDouble() * 2.147483647E9));
                     }

                     pd[playerID].setEnergy(20);
                  } else if (isWrite && address == Short.MIN_VALUE + playerID * 1024 + 28) {
                     pd[playerID].setColor(this.toolReadPlayerData(playerID, 28));
                  } else if ((!isWrite || address < Short.MIN_VALUE + playerID * 1024 || address >= Short.MIN_VALUE + (playerID + 1) * 1024) && (!isWrite || playerID != 999)) {
                     if (isWrite) {
                        JOptionPane.showMessageDialog((Component)null, "ScavengerHunt.update(): Player " + playerID + " writing outside assigned mem. loc. at address " + Binary.intToHexString(address) + " -- not implemented!");
                     } else if (isRead) {
                     }
                  }
               }
            }

         }
      }
   }

   private void toolSetWord(int address, int data) {
      if (this.KENVDEBUG) {
         System.out.println("   ScavengerHunt.toolSetWord: Setting MIPS Memory[" + Binary.intToHexString(address) + "] to " + Binary.intToHexString(data) + " = " + data);
      }

      ++SetWordCounter;

      try {
         Globals.memory.setWord(address, data);
      } catch (AddressErrorException var4) {
         System.out.println("ScavengerHunt.toolSetWord: deliberate exit on AEE exception.");
         System.out.println("     SetWordCounter = " + SetWordCounter);
         System.out.println("     address = " + Binary.intToHexString(address));
         System.out.println("     data = " + data);
         System.exit(0);
      } catch (Exception var5) {
         System.out.println("ScavengerHunt.toolSetWord: deliberate exit on " + var5.getMessage() + " exception.");
         System.out.println("     SetWordCounter = " + SetWordCounter);
         System.out.println("     address = " + Binary.intToHexString(address));
         System.out.println("     data = " + data);
         System.exit(0);
      }

      if (this.KENVDEBUG) {
         int verifyData = this.toolGetWord(address);
         if (verifyData != data) {
            System.out.println("\n\nScavengerHunt.toolSetWord: Can't verify data! Special exit.");
            System.out.println("     address = " + Binary.intToHexString(address));
            System.out.println("     data = " + data);
            System.out.println("     verifyData = " + verifyData);
            System.exit(0);
         } else {
            System.out.println("  ScavengerHunt.toolSetWord: Mem[" + Binary.intToHexString(address) + " verified as " + Binary.intToHexString(data));
         }
      }

   }

   private int toolGetWord(int address) {
      try {
         int returnValue = Globals.memory.getWord(address);
         return returnValue;
      } catch (AddressErrorException var4) {
         System.out.println("ScavengerHunt.toolGetWord: deliberate exit on AEE exception.");
         System.out.println("     SetWordCounter = " + SetWordCounter);
         System.out.println("     address = " + Binary.intToHexString(address));
         System.exit(0);
      } catch (Exception var5) {
         System.out.println("ScavengerHunt.toolGetWord: deliberate exit on " + var5.getMessage() + " exception.");
         System.out.println("     SetWordCounter = " + SetWordCounter);
         System.out.println("     address = " + Binary.intToHexString(address));
         System.exit(0);
      }

      return 0;
   }

   private int toolReadPlayerData(int p, int offset) {
      if (this.KENVDEBUG) {
         System.out.println("ScavengerHunt.toolReadPlayerData: called with player " + p + ", offset = " + Binary.intToHexString(offset) + " ---> address " + Binary.intToHexString(Short.MIN_VALUE + p * 1024 + offset));
      }

      int returnValue = this.toolGetWord(Short.MIN_VALUE + p * 1024 + offset);
      if (this.KENVDEBUG) {
         System.out.println("ScavengerHunt.toolReadPlayerData: Mem[" + Binary.intToHexString(Short.MIN_VALUE + p * 1024 + offset) + "] = " + Binary.intToHexString(returnValue) + " --- returning normally");
      }

      return returnValue;
   }

   private void toolWritePlayerData(int p, int offset, int data) {
      int address = Short.MIN_VALUE + p * 1024 + offset;
      if (this.KENVDEBUG) {
         System.out.println("ScavengerHunt.toolWritePlayerData: called with player " + p + ", offset = " + Binary.intToHexString(offset) + ", data = " + Binary.intToHexString(data));
      }

      this.toolSetWord(address, data);
      if (this.KENVDEBUG) {
         int verifyData = this.toolGetWord(address);
         if (data != verifyData) {
            System.out.println("\n\nScavengerHunt.toolWritePlayerData: MAYDAY data not verified !");
            System.out.println("      requested data to be written was " + Binary.intToHexString(data));
            System.out.println("      actual data at that loc is " + Binary.intToHexString(this.toolGetWord(address)));
            System.exit(0);
         } else {
            System.out.println("  ScavengerHunt.toolWritePlayerData: Mem[" + Binary.intToHexString(address) + " verified as " + Binary.intToHexString(data));
         }
      }

   }

   private void initializeScavengerData() {
      this.authenticationValue = 0;
      playerID = 999;
      this.startTime = System.currentTimeMillis();
      this.randomStream = new Random(42L);

      int i;
      for(i = 0; i < 6; ++i) {
         loc[i] = new Location();
         loc[i].X = (int)(this.randomStream.nextDouble() * 712.0);
         loc[i].Y = (int)(this.randomStream.nextDouble() * 602.0);
      }

      loc[6] = new Location();
      loc[6].X = 255;
      loc[6].Y = 255;

      for(i = 0; i < 22; ++i) {
         pd[i] = new PlayerData();

         int j;
         for(j = 0; j < 7; ++j) {
            this.toolWritePlayerData(i, 36 + j * 8 + 0, loc[j].X);
            this.toolWritePlayerData(i, 36 + j * 8 + 4, loc[j].Y);
         }

         for(j = 0; j < 20; ++j) {
            this.toolWritePlayerData(i, 296 + j * 4, (int)(this.randomStream.nextDouble() * 2.147483647E9));
         }
      }

   }

   private class ScavengerHuntDisplay extends JPanel {
      private int width;
      private int height;
      private boolean clearTheDisplay = true;

      public ScavengerHuntDisplay(int tw, int th) {
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
         if (!ScavengerHunt.this.GameOn) {
            g2.setColor(Color.lightGray);
            g2.fillRect(0, 0, this.width - 1, this.height - 1);
            g2.setColor(Color.black);
            g2.drawString(" ScavengerHunt not yet initialized by MIPS administrator program.", 100, 200);
         } else {
            g2.setColor(Color.lightGray);
            g2.fillRect(0, 0, this.width - 1, this.height - 1);

            int xCoord;
            int yCoord;
            int i;
            for(i = 0; i < 7; ++i) {
               xCoord = ScavengerHunt.loc[i].X;
               yCoord = ScavengerHunt.loc[i].Y;
               g2.setColor(Color.blue);
               g2.fillRect(xCoord, yCoord, 20, 20);
               g2.setColor(Color.white);
               g2.drawString(" " + i, xCoord + 4, yCoord + 15);
            }

            g2.setColor(Color.black);
            g2.drawString("Player", this.width - 160, 30);
            g2.drawString("Locations", this.width - 110, 30);
            g2.drawString("Energy", this.width - 50, 30);
            g2.drawLine(this.width - 160, 35, this.width - 10, 35);
            g2.drawLine(this.width - 120, 35, this.width - 120, 365);
            g2.drawLine(this.width - 50, 35, this.width - 50, 365);

            for(i = 0; i < 22; ++i) {
               g2.setColor(new Color(ScavengerHunt.pd[i].getColor()));
               xCoord = ScavengerHunt.pd[i].getWhereAmIX();
               yCoord = ScavengerHunt.pd[i].getWhereAmIY();
               g2.drawOval(xCoord, yCoord, 20, 20);
               g2.drawString(" " + i, xCoord + 4, yCoord + 15);
               g2.setColor(Color.black);
               g2.drawString(" " + i, this.width - 150, 50 + i * 15);
               g2.drawString(" " + ScavengerHunt.pd[i].getEnergy(), this.width - 40, 50 + i * 15);
               if (ScavengerHunt.pd[i].isFinished()) {
                  g2.drawString(ScavengerHunt.pd[i].getFinishMin() + ":" + ScavengerHunt.pd[i].getFinishSec() + ":" + ScavengerHunt.pd[i].getFinishMillisec(), this.width - 115, 50 + i * 15);
               } else {
                  int visCount = 0;

                  int j;
                  for(j = 0; j < 7; ++j) {
                     if (ScavengerHunt.pd[i].hasVisited(j)) {
                        ++visCount;
                     }
                  }

                  if (visCount == 7) {
                     ScavengerHunt.pd[i].setFinished();
                     ScavengerHunt.pd[i].setFinishTime(System.currentTimeMillis() - ScavengerHunt.this.startTime);
                  } else {
                     for(j = 0; j < 7; ++j) {
                        if (ScavengerHunt.pd[i].hasVisited(j)) {
                           g2.fillRect(this.width - 120 + j * 10, 42 + i * 15, 10, 8);
                        }
                     }
                  }
               }
            }

         }
      }
   }

   private class ScavengerHuntRunnable implements Runnable {
      JPanel panel;

      public ScavengerHuntRunnable() {
         final JDialog frame = new JDialog(Globals.getGui(), "ScavengerHunt");
         this.panel = new JPanel(new BorderLayout());
         ScavengerHunt.this.graphicArea = ScavengerHunt.this.new ScavengerHuntDisplay(712, 652);
         JPanel buttonPanel = new JPanel();
         JButton resetButton = new JButton("Reset");
         resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               ScavengerHunt.this.graphicArea.clear();
               ScavengerHunt.this.initializeScavengerData();
            }
         });
         buttonPanel.add(resetButton);
         this.panel.add(ScavengerHunt.this.graphicArea, "Center");
         this.panel.add(buttonPanel, "South");
         frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
               frame.setVisible(false);
               frame.dispose();
            }
         });
         frame.getContentPane().add(this.panel);
         frame.setLocationRelativeTo((Component)null);
         frame.pack();
         frame.setVisible(true);
         frame.setTitle(" This is the ScavengerHunt");
         frame.setDefaultCloseOperation(3);
         frame.setPreferredSize(new Dimension(712, 652));
         frame.setVisible(true);
      }

      public void run() {
         while(true) {
            try {
               Thread.sleep(100L);
            } catch (InterruptedException var4) {
            }

            this.panel.repaint();
         }
      }
   }

   private class PlayerData {
      int whereAmIX;
      int whereAmIY;
      int energy;
      int color;
      long finishTime;
      boolean[] hasVisitedLoc;
      boolean finis;

      private PlayerData() {
         this.whereAmIX = 255;
         this.whereAmIY = 255;
         this.energy = 20;
         this.color = 0;
         this.hasVisitedLoc = new boolean[7];
         this.finis = false;
      }

      public void setWhereAmI(int gX, int gY) {
         this.whereAmIX = gX;
         this.whereAmIY = gY;
      }

      public void setEnergy(int e) {
         this.energy = e;
      }

      public void setColor(int c) {
         this.color = c;
      }

      public int getWhereAmIX() {
         return this.whereAmIX;
      }

      public int getWhereAmIY() {
         return this.whereAmIY;
      }

      public int getColor() {
         return this.color;
      }

      public boolean hasVisited(int i) {
         return this.hasVisitedLoc[i];
      }

      public void setVisited(int i) {
         this.hasVisitedLoc[i] = true;
      }

      public void setFinished() {
         this.finis = true;
      }

      public boolean isFinished() {
         return this.finis;
      }

      public long getFinishTime() {
         return this.finishTime;
      }

      public long getFinishMin() {
         return this.finishTime / 60000L;
      }

      public long getFinishSec() {
         return this.finishTime % 60000L / 1000L;
      }

      public long getFinishMillisec() {
         return this.finishTime % 1000L;
      }

      public void setFinishTime(long t) {
         this.finishTime = t;
      }

      public int getEnergy() {
         return this.energy;
      }

      // $FF: synthetic method
      PlayerData(Object x1) {
         this();
      }
   }

   private class Location {
      public int X;
      public int Y;

      private Location() {
      }

      // $FF: synthetic method
      Location(Object x1) {
         this();
      }
   }
}
