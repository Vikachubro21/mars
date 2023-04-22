package mars.tools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Observable;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import mars.Globals;
import mars.ProgramStatement;
import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryAccessNotice;
import mars.mips.hardware.RegisterAccessNotice;
import mars.mips.hardware.RegisterFile;

public class VisualStack extends AbstractMarsToolAndApplication {
   private static String name = "Visual Stack";
   private static String heading = "Visual Stack";
   private static String version = "Version 1.0";
   private static final int STACK_VIEWER_WIDTH = 400;
   private static final int STACK_VIEWER_HEIGHT = 400;
   private static final int STACK_VIEWER_NUM_COLS = 10;
   private static final int STACK_VIEWER_NUM_ROWS = 10;
   private JScrollPane displayAreaScrollPane;
   private StackViewer theStackViewer;
   private JLabel statusLabel;
   private int oldStackPtrValue;
   private boolean stackOK = true;

   public VisualStack(String title, String heading) {
      super(title, heading);
   }

   public VisualStack() {
      super(heading + ", " + version, heading);
   }

   public static void main(String[] args) {
      (new VisualStack(heading + ", " + version, heading)).go();
   }

   public String getName() {
      return name;
   }

   public void initializePreGUI() {
      this.addAsObserver(Memory.stackLimitAddress, Memory.stackBaseAddress);
      this.addAsObserver(RegisterFile.getUserRegister("$sp"));
      this.oldStackPtrValue = Memory.stackPointer;
   }

   protected JComponent buildMainDisplayArea() {
      JPanel displayArea = new JPanel();
      displayArea.setLayout(new BoxLayout(displayArea, 3));
      this.theStackViewer = new StackViewer(400, 400);
      this.statusLabel = new JLabel("Stack status: OK");
      this.displayAreaScrollPane = new JScrollPane(this.theStackViewer, 22, 31);
      displayArea.add(this.displayAreaScrollPane);
      displayArea.add(this.statusLabel);
      this.statusLabel.setAlignmentX(0.5F);
      return displayArea;
   }

   public void initializePostGUI() {
      if (this.isBeingUsedAsAMarsTool) {
         ((JDialog)this.theWindow).setResizable(false);
      } else {
         this.setResizable(false);
      }

   }

   protected JComponent getHelpComponent() {
      final String helpContent = name + ", " + version + "\n\n" + "This tool provides a graphical visualization of the MIPS stack. " + "When data is pushed to the stack, a colored rectangle representing the data appears in the appropriate position, along with information about which register the data came from. " + "Visual Stack was written to aid developers of recursive functions in debugging and to help them avoid common stack pitfalls; as such, it distinguishes between return addresses and other types of data. " + "(To do this, it uses a heuristic: if the data is the address of a jal command plus 4, it is probably a return address. This properly identifies return addresses contained in registers other than $ra.) " + "Aside from showing data on the stack, Visual Stack represents the position of the stack pointer both graphically (a green arrow points to the data at $sp) and textually (the value of $sp is displayed.) " + "\n\n" + "Visual Stack can also detect certain error conditions, such as $sp not being aligned on a word boundary. Should such a condition arise, the screen will freeze and the error will be briefly described. " + "The stack will not be updated again until the Reset button is pressed. " + "\n\n" + "This tool was written by James Hester, a student at the University of Texas at Dallas, in November 2014.";
      JButton help = new JButton("Help");
      help.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            JTextArea ja = new JTextArea(helpContent);
            ja.setRows(15);
            ja.setColumns(30);
            ja.setLineWrap(true);
            ja.setWrapStyleWord(true);
            ja.setEditable(false);
            JOptionPane.showMessageDialog(VisualStack.this.theWindow, new JScrollPane(ja), "Visual Stack", 1);
         }
      });
      return help;
   }

   protected void processMIPSUpdate(Observable resource, AccessNotice notice) {
      if (this.stackOK && this.isObserving()) {
         synchronized(Globals.memoryAndRegistersLock) {
            if (notice.getAccessType() == 1) {
               if (notice.accessIsFromGUI()) {
                  return;
               }

               if (notice instanceof MemoryAccessNotice) {
                  this.processMemoryUpdate((MemoryAccessNotice)notice);
               } else {
                  this.processStackPtrUpdate((RegisterAccessNotice)notice);
               }
            }
         }
      }

   }

   private void processMemoryUpdate(MemoryAccessNotice theNotice) {
      int address = theNotice.getAddress();
      if (address <= Memory.stackBaseAddress && address >= Memory.stackLimitAddress) {
         int valueWritten;
         int registerDataCameFrom;
         try {
            valueWritten = Memory.getInstance().getWord(address);
            ProgramStatement instr = Memory.getInstance().getStatement(RegisterFile.getProgramCounter() - 4);
            registerDataCameFrom = instr.getBinaryStatement();
            registerDataCameFrom &= 2031616;
            registerDataCameFrom >>= 16;
         } catch (AddressErrorException var8) {
            var8.printStackTrace();
            return;
         }

         int position = (Memory.stackPointer - address) / 4;
         if (position < 0) {
            this.handleError("Data was pushed to an address greater than stack base!");
         } else {
            boolean dataIsReturnAddress = this.isReturnAddress(valueWritten);
            String description;
            if (dataIsReturnAddress) {
               description = "Return address ";
            } else {
               description = "Data ";
            }

            description = description + "from register ";
            description = description + RegisterFile.getRegisters()[registerDataCameFrom].getName();
            if (position > 0) {
               this.theStackViewer.insertStackElement(position, dataIsReturnAddress, description);
            }

         }
      }
   }

   private void handleError(String errorText) {
      this.statusLabel.setForeground(Color.RED);
      this.statusLabel.setText(errorText);
      this.theStackViewer.errorOverlay();
      Toolkit.getDefaultToolkit().beep();
      this.stackOK = false;
      this.repaint();
   }

   private void processStackPtrUpdate(RegisterAccessNotice notice) {
      int newStackPtrValue = RegisterFile.getValue(29);
      int stackPtrDelta = this.oldStackPtrValue - newStackPtrValue;
      if (stackPtrDelta % 4 != 0) {
         this.handleError("$sp set to 0x" + Integer.toHexString(newStackPtrValue) + "; not word-aligned!");
      } else {
         stackPtrDelta /= 4;
         this.theStackViewer.advanceStackPointer(stackPtrDelta);
         this.oldStackPtrValue = newStackPtrValue;
      }
   }

   private synchronized boolean isReturnAddress(int theData) {
      if (Memory.inTextSegment(theData)) {
         try {
            String theStatement = Memory.getInstance().getStatement(theData - 4).getBasicAssemblyStatement();
            return theStatement.substring(0, 3).equals("jal");
         } catch (Exception var3) {
            return false;
         }
      } else {
         return false;
      }
   }

   public void reset() {
      this.theStackViewer.reset(400, 400);
      this.statusLabel.setForeground(Color.BLACK);
      this.statusLabel.setText("Stack status: OK");
      this.theStackViewer.advanceStackPointer(0);
      this.oldStackPtrValue = Memory.stackPointer;
      this.stackOK = true;
      this.repaint();
   }

   private class StackViewer extends JComponent {
      private BufferedImage screen;
      private Graphics2D imageWriter;
      private int stackPtrPosition = 0;
      private int highestOccupiedPosition;
      private Color garbageDark = new Color(8394005);
      private Color garbageLight = new Color(16755370);
      private Color dataDark = new Color(871757);
      private Color dataLight = new Color(6723993);
      private Color retDark = new Color(5666580);
      private Color retLight = new Color(13954719);
      private Color nullDark = new Color(4342338);
      private Color nullLight = new Color(13816530);

      public StackViewer(int width, int height) {
         this.reset(width, height);
      }

      public void reset(int width, int height) {
         super.setSize(width, height);
         super.setPreferredSize(new Dimension(width, height));
         this.screen = new BufferedImage(width, height, 6);
         this.imageWriter = this.screen.createGraphics();
         this.imageWriter.setFont(new Font("SansSerif", 1, 14));
         this.imageWriter.setColor(Color.BLACK);
         this.imageWriter.fillRect(0, 0, width, height);
         this.imageWriter.setColor(this.nullLight);
         this.imageWriter.drawLine(0, height / 10, width, height / 10);
         this.imageWriter.drawLine(40, height / 10, 40, height);
         this.stackPtrPosition = 0;
         this.highestOccupiedPosition = 0;
         this.advanceStackPointer(0);
      }

      public void printStackPointerAddress(int position) {
         String spAddress = "0x" + Integer.toHexString(Memory.stackPointer - position * 4);
         this.imageWriter.setColor(Color.BLACK);
         this.imageWriter.fillRect(0, 0, 400, 39);
         this.imageWriter.setColor(Color.WHITE);
         this.imageWriter.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
         this.printStringCentered("$sp: " + spAddress, 116, 0, 24);
      }

      public void advanceStackPointer(int numPositions) {
         int newPosition = this.stackPtrPosition + numPositions;
         this.printStackPointerAddress(newPosition);
         this.ensureHeight(40 * (newPosition + 1));
         this.imageWriter.setColor(Color.BLACK);
         this.imageWriter.fillRect(0, 40 * (this.stackPtrPosition > 0 ? this.stackPtrPosition : 1) + 1, 35, 39);
         this.drawArrow(newPosition, Color.GREEN);
         int i;
         if (numPositions < 0) {
            for(i = newPosition + 1; i <= this.stackPtrPosition; ++i) {
               this.removeStackElement(i);
            }
         } else if (numPositions > 0 && newPosition > this.highestOccupiedPosition) {
            for(i = this.highestOccupiedPosition + 1; i <= newPosition; ++i) {
               this.insertEmptyElement(i);
            }
         }

         this.repaint();
         this.stackPtrPosition = newPosition;
      }

      public void insertEmptyElement(int which) {
         int y1 = 2 + 40 * which;
         this.imageWriter.setColor(this.nullDark);
         this.imageWriter.fillRect(42, y1, 320, 36);
         this.imageWriter.setColor(this.nullLight);
         this.imageWriter.fillRect(46, y1 + 4, 312, 28);
         this.imageWriter.setColor(Color.BLACK);
         int strHeight = (int)this.imageWriter.getFontMetrics().getLineMetrics("Null or invalid data", this.imageWriter).getHeight();
         this.printStringCentered("Null or invalid data", 312, 42, y1 + 28 - (28 - strHeight) / 2);
      }

      public void removeStackElement(int which) {
         int y1 = 1 + 40 * which;
         this.imageWriter.setColor(Color.BLACK);
         this.imageWriter.fillRect(41, y1, 322, 42);
         this.highestOccupiedPosition = which - 1;
      }

      public void insertStackElement(int which, boolean isReturnAddress, String label) {
         if (which > this.highestOccupiedPosition) {
            this.highestOccupiedPosition = which;
         }

         this.ensureHeight(40 * (which + 1));
         int y1 = 2 + 40 * which;
         if (isReturnAddress) {
            this.imageWriter.setColor(this.retDark);
         } else {
            this.imageWriter.setColor(this.dataDark);
         }

         this.imageWriter.fillRect(42, y1, 320, 36);
         if (isReturnAddress) {
            this.imageWriter.setColor(this.retLight);
         } else {
            this.imageWriter.setColor(this.dataLight);
         }

         this.imageWriter.fillRect(46, y1 + 4, 312, 28);
         this.imageWriter.setColor(Color.BLACK);
         int strHeight = (int)this.imageWriter.getFontMetrics().getLineMetrics(label, this.imageWriter).getHeight();
         this.printStringCentered(label, 312, 42, y1 + 28 - (28 - strHeight) / 2);
         this.repaint();
      }

      public void ensureHeight(int newHeight) {
         if (newHeight > this.screen.getHeight()) {
            BufferedImage newScreen = new BufferedImage(this.screen.getWidth(), newHeight, 6);
            Graphics2D newIW = newScreen.createGraphics();
            newIW.drawImage(this.screen, 0, 0, this);
            newIW.setColor(Color.BLACK);
            newIW.fillRect(0, this.screen.getHeight(), this.screen.getWidth(), newHeight - this.screen.getHeight());
            newIW.setColor(this.nullLight);
            newIW.drawLine(40, this.screen.getHeight(), 40, newHeight);
            newIW.setColor(this.imageWriter.getColor());
            newIW.setFont(this.imageWriter.getFont());
            newIW.setRenderingHints(this.imageWriter.getRenderingHints());
            this.screen = newScreen;
            this.imageWriter = newIW;
            this.setPreferredSize(new Dimension(this.getWidth(), newHeight));
            this.revalidate();
            this.repaint();
         }

      }

      private void printStringCentered(String str, int width, int x, int y) {
         int offset = this.imageWriter.getFontMetrics().stringWidth(str);
         offset = (width - offset) / 2;
         this.imageWriter.setFont(new Font("SansSerif", 1, 14));
         this.imageWriter.drawString(str, x + offset, y);
      }

      private void drawArrow(int position, Color color) {
         int[] arrowXPoints = new int[]{12, 24, 12};
         int[] arrowYPoints = new int[]{10, 20, 30};

         for(int i = 0; i < arrowYPoints.length; ++i) {
            arrowYPoints[i] += (position > 0 ? position : 1) * 400 / 10;
         }

         this.imageWriter.setColor(color);
         this.imageWriter.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
         this.imageWriter.fillPolygon(arrowXPoints, arrowYPoints, 3);
      }

      public void errorOverlay() {
         this.imageWriter.setColor(new Color(255, 0, 0, 128));
         this.imageWriter.fillRect(0, 0, this.getSize().width, this.getSize().height);
         this.repaint();
      }

      public void paintComponent(Graphics g) {
         super.paintComponent(g);
         if (this.screen != null) {
            g.drawImage(this.screen, 0, 0, this);
         }

      }
   }
}
