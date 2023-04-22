package mars.tools;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.Observable;
import java.util.Random;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultCaret;
import mars.Globals;
import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryAccessNotice;
import mars.simulator.Simulator;
import mars.util.Binary;
import mars.venus.AbstractFontSettingDialog;

public class KeyboardAndDisplaySimulator extends AbstractMarsToolAndApplication {
   private static String version = "Version 1.4";
   private static String heading = "Keyboard and Display MMIO Simulator";
   private static String displayPanelTitle;
   private static String keyboardPanelTitle;
   private static char VT_FILL = ' ';
   public static Dimension preferredTextAreaDimension = new Dimension(400, 200);
   private static Insets textAreaInsets = new Insets(4, 4, 4, 4);
   private final TransmitterDelayTechnique[] delayTechniques = new TransmitterDelayTechnique[]{new FixedLengthDelay(), new UniformlyDistributedDelay(), new NormallyDistributedDelay()};
   public static int RECEIVER_CONTROL;
   public static int RECEIVER_DATA;
   public static int TRANSMITTER_CONTROL;
   public static int TRANSMITTER_DATA;
   private boolean countingInstructions;
   private int instructionCount;
   private int transmitDelayInstructionCountLimit;
   private int currentDelayInstructionLimit;
   private int intWithCharacterToDisplay;
   private boolean displayAfterDelay = true;
   private boolean displayRandomAccessMode = false;
   private int rows;
   private int columns;
   private DisplayResizeAdapter updateDisplayBorder;
   private KeyboardAndDisplaySimulator simulator = this;
   private JPanel keyboardAndDisplay;
   private JScrollPane displayScrollPane;
   private JTextArea display;
   private JPanel displayPanel;
   private JPanel displayOptions;
   private JComboBox delayTechniqueChooser;
   private DelayLengthPanel delayLengthPanel;
   private JSlider delayLengthSlider;
   private JCheckBox displayAfterDelayCheckBox;
   private JPanel keyboardPanel;
   private JScrollPane keyAccepterScrollPane;
   private JTextArea keyEventAccepter;
   private JButton fontButton;
   private Font defaultFont = new Font("Monospaced", 0, 12);
   private static final char CLEAR_SCREEN = '\f';
   private static final char SET_CURSOR_X_Y = '\u0007';

   public KeyboardAndDisplaySimulator(String title, String heading) {
      super(title, heading);
   }

   public KeyboardAndDisplaySimulator() {
      super(heading + ", " + version, heading);
   }

   public static void main(String[] args) {
      (new KeyboardAndDisplaySimulator(heading + " stand-alone, " + version, heading)).go();
   }

   public String getName() {
      return heading;
   }

   protected void initializePreGUI() {
      RECEIVER_CONTROL = Memory.memoryMapBaseAddress;
      RECEIVER_DATA = Memory.memoryMapBaseAddress + 4;
      TRANSMITTER_CONTROL = Memory.memoryMapBaseAddress + 8;
      TRANSMITTER_DATA = Memory.memoryMapBaseAddress + 12;
      displayPanelTitle = "DISPLAY: Store to Transmitter Data " + Binary.intToHexString(TRANSMITTER_DATA);
      keyboardPanelTitle = "KEYBOARD: Characters typed here are stored to Receiver Data " + Binary.intToHexString(RECEIVER_DATA);
   }

   protected void addAsObserver() {
      this.updateMMIOControl(TRANSMITTER_CONTROL, readyBitSet(TRANSMITTER_CONTROL));
      this.addAsObserver(RECEIVER_DATA, RECEIVER_DATA);
      this.addAsObserver(TRANSMITTER_DATA, TRANSMITTER_DATA);
      this.addAsObserver(Memory.textBaseAddress, Memory.textLimitAddress);
      this.addAsObserver(Memory.kernelTextBaseAddress, Memory.kernelTextLimitAddress);
   }

   protected JComponent buildMainDisplayArea() {
      this.keyboardAndDisplay = new JPanel(new BorderLayout());
      JSplitPane both = new JSplitPane(0, this.buildDisplay(), this.buildKeyboard());
      both.setResizeWeight(0.5);
      this.keyboardAndDisplay.add(both);
      return this.keyboardAndDisplay;
   }

   protected void processMIPSUpdate(Observable memory, AccessNotice accessNotice) {
      MemoryAccessNotice notice = (MemoryAccessNotice)accessNotice;
      if (notice.getAddress() == RECEIVER_DATA && notice.getAccessType() == 0) {
         this.updateMMIOControl(RECEIVER_CONTROL, readyBitCleared(RECEIVER_CONTROL));
      }

      if (isReadyBitSet(TRANSMITTER_CONTROL) && notice.getAddress() == TRANSMITTER_DATA && notice.getAccessType() == 1) {
         this.updateMMIOControl(TRANSMITTER_CONTROL, readyBitCleared(TRANSMITTER_CONTROL));
         this.intWithCharacterToDisplay = notice.getValue();
         if (!this.displayAfterDelay) {
            this.displayCharacter(this.intWithCharacterToDisplay);
         }

         this.countingInstructions = true;
         this.instructionCount = 0;
         this.transmitDelayInstructionCountLimit = this.generateDelay();
      }

      if (this.countingInstructions && notice.getAccessType() == 0 && (Memory.inTextSegment(notice.getAddress()) || Memory.inKernelTextSegment(notice.getAddress()))) {
         ++this.instructionCount;
         if (this.instructionCount >= this.transmitDelayInstructionCountLimit) {
            if (this.displayAfterDelay) {
               this.displayCharacter(this.intWithCharacterToDisplay);
            }

            this.countingInstructions = false;
            int updatedTransmitterControl = readyBitSet(TRANSMITTER_CONTROL);
            this.updateMMIOControl(TRANSMITTER_CONTROL, updatedTransmitterControl);
            if (updatedTransmitterControl != 1 && (Coprocessor0.getValue(12) & 2) == 0 && (Coprocessor0.getValue(12) & 1) == 1) {
               Simulator.externalInterruptingDevice = 128;
            }
         }
      }

   }

   private void displayCharacter(int intWithCharacterToDisplay) {
      char characterToDisplay = (char)(intWithCharacterToDisplay & 255);
      if (characterToDisplay == '\f') {
         this.initializeDisplay(this.displayRandomAccessMode);
      } else {
         int x;
         if (characterToDisplay == 7) {
            if (!this.displayRandomAccessMode) {
               this.displayRandomAccessMode = true;
               this.initializeDisplay(this.displayRandomAccessMode);
            }

            x = (intWithCharacterToDisplay & -1048576) >>> 20;
            int y = (intWithCharacterToDisplay & 1048320) >>> 8;
            if (x < 0) {
               x = 0;
            }

            if (x >= this.columns) {
               x = this.columns - 1;
            }

            if (y < 0) {
               y = 0;
            }

            if (y >= this.rows) {
               y = this.rows - 1;
            }

            this.display.setCaretPosition(y * (this.columns + 1) + x);
         } else if (this.displayRandomAccessMode) {
            try {
               x = this.display.getCaretPosition();
               if ((x + 1) % (this.columns + 1) == 0) {
                  ++x;
                  this.display.setCaretPosition(x);
               }

               this.display.replaceRange("" + characterToDisplay, x, x + 1);
            } catch (IllegalArgumentException var5) {
               this.display.setCaretPosition(this.display.getCaretPosition() - 1);
               this.display.replaceRange("" + characterToDisplay, this.display.getCaretPosition(), this.display.getCaretPosition() + 1);
            }
         } else {
            this.display.append("" + characterToDisplay);
         }
      }

   }

   protected void initializePostGUI() {
      this.initializeTransmitDelaySimulator();
      this.keyEventAccepter.requestFocusInWindow();
   }

   protected void reset() {
      this.displayRandomAccessMode = false;
      this.initializeTransmitDelaySimulator();
      this.initializeDisplay(this.displayRandomAccessMode);
      this.keyEventAccepter.setText("");
      ((TitledBorder)this.displayPanel.getBorder()).setTitle(displayPanelTitle);
      this.displayPanel.repaint();
      this.keyEventAccepter.requestFocusInWindow();
      this.updateMMIOControl(TRANSMITTER_CONTROL, readyBitSet(TRANSMITTER_CONTROL));
   }

   private void initializeDisplay(boolean randomAccess) {
      String initialText = "";
      if (randomAccess) {
         Dimension textDimensions = this.getDisplayPanelTextDimensions();
         this.columns = (int)textDimensions.getWidth();
         this.rows = (int)textDimensions.getHeight();
         this.repaintDisplayPanelBorder();
         char[] charArray = new char[this.columns];
         Arrays.fill(charArray, VT_FILL);
         String row = new String(charArray);
         StringBuffer str = new StringBuffer(row);

         for(int i = 1; i < this.rows; ++i) {
            str.append("\n" + row);
         }

         initialText = str.toString();
      }

      this.display.setText(initialText);
      this.display.setCaretPosition(0);
   }

   private void repaintDisplayPanelBorder() {
      Dimension size = this.getDisplayPanelTextDimensions();
      int cols = (int)size.getWidth();
      int rows = (int)size.getHeight();
      int caretPosition = this.display.getCaretPosition();
      String stringCaretPosition = "";
      if (this.displayRandomAccessMode) {
         if ((caretPosition + 1) % (this.columns + 1) != 0) {
            stringCaretPosition = "(" + caretPosition % (this.columns + 1) + "," + caretPosition / (this.columns + 1) + ")";
         } else if ((caretPosition + 1) % (this.columns + 1) == 0 && caretPosition / (this.columns + 1) + 1 == rows) {
            stringCaretPosition = "(" + (caretPosition % (this.columns + 1) - 1) + "," + caretPosition / (this.columns + 1) + ")";
         } else {
            stringCaretPosition = "(0," + (caretPosition / (this.columns + 1) + 1) + ")";
         }
      } else {
         stringCaretPosition = "" + caretPosition;
      }

      String title = displayPanelTitle + ", cursor " + stringCaretPosition + ", area " + cols + " x " + rows;
      ((TitledBorder)this.displayPanel.getBorder()).setTitle(title);
      this.displayPanel.repaint();
   }

   private Dimension getDisplayPanelTextDimensions() {
      Dimension areaSize = this.display.getSize();
      int widthInPixels = (int)areaSize.getWidth();
      int heightInPixels = (int)areaSize.getHeight();
      FontMetrics metrics = this.getFontMetrics(this.display.getFont());
      int rowHeight = metrics.getHeight();
      int charWidth = metrics.charWidth('m');
      return new Dimension(widthInPixels / charWidth - 1, heightInPixels / rowHeight - 1);
   }

   protected JComponent getHelpComponent() {
      final String helpContent = "Keyboard And Display MMIO Simulator\n\nUse this program to simulate Memory-Mapped I/O (MMIO) for a keyboard input device and character display output device.  It may be run either from MARS' Tools menu or as a stand-alone application. For the latter, simply write a driver to instantiate a mars.tools.KeyboardAndDisplaySimulator object and invoke its go() method.\n\nWhile the tool is connected to MIPS, each keystroke in the text area causes the corresponding ASCII code to be placed in the Receiver Data register (low-order byte of memory word " + Binary.intToHexString(RECEIVER_DATA) + "), and the " + "Ready bit to be set to 1 in the Receiver Control register (low-order bit of " + Binary.intToHexString(RECEIVER_CONTROL) + ").  The Ready " + "bit is automatically reset to 0 when the MIPS program reads the Receiver Data using an 'lw' instruction.\n" + "\n" + "A program may write to the display area by detecting the Ready bit set (1) in the Transmitter Control " + "register (low-order bit of memory word " + Binary.intToHexString(TRANSMITTER_CONTROL) + "), then storing the ASCII code of the character to be " + "displayed in the Transmitter Data register (low-order byte of " + Binary.intToHexString(TRANSMITTER_DATA) + ") using a 'sw' instruction.  This " + "triggers the simulated display to clear the Ready bit to 0, delay awhile to simulate processing the data, " + "then set the Ready bit back to 1.  The delay is based on a count of executed MIPS instructions.\n" + "\n" + "In a polled approach to I/O, a MIPS program idles in a loop, testing the device's Ready bit on each " + "iteration until it is set to 1 before proceeding.  This tool also supports an interrupt-driven approach " + "which requires the program to provide an interrupt handler but allows it to perform useful processing " + "instead of idly looping.  When the device is ready, it signals an interrupt and the MARS simuator will " + "transfer control to the interrupt handler.  Note: in MARS, the interrupt handler has to co-exist with the " + "exception handler in kernel memory, both having the same entry address.  Interrupt-driven I/O is enabled " + "when the MIPS program sets the Interrupt-Enable bit in the device's control register.  Details below.\n" + "\n" + "Upon setting the Receiver Controller's Ready bit to 1, its Interrupt-Enable bit (bit position 1) is tested. " + "If 1, then an External Interrupt will be generated.  Before executing the next MIPS instruction, the runtime " + "simulator will detect the interrupt, place the interrupt code (0) into bits 2-6 of Coprocessor 0's Cause " + "register ($13), set bit 8 to 1 to identify the source as keyboard, place the program counter value (address " + "of the NEXT instruction to be executed) into its EPC register ($14), and check to see if an interrupt/trap " + "handler is present (looks for instruction code at address 0x80000180).  If so, the program counter is set to " + "that address.  If not, program execution is terminated with a message to the Run I/O tab.  The Interrupt-Enable " + "bit is 0 by default and has to be set by the MIPS program if interrupt-driven input is desired.  Interrupt-driven " + "input permits the program to perform useful tasks instead of idling in a loop polling the Receiver Ready bit!  " + "Very event-oriented.  The Ready bit is supposed to be read-only but in MARS it is not.\n" + "\n" + "A similar test and potential response occurs when the Transmitter Controller's Ready bit is set to 1.  This " + "occurs after the simulated delay described above.  The only difference is the Cause register bit to identify " + "the (simulated) display as external interrupt source is bit position 9 rather than 8.  This permits you to " + "write programs that perform interrupt-driven output - the program can perform useful tasks while the " + "output device is processing its data.  Much better than idling in a loop polling the Transmitter Ready bit! " + "The Ready bit is supposed to be read-only but in MARS it is not.\n" + "\n" + "IMPORTANT NOTE: The Transmitter Controller Ready bit is set to its initial value of 1 only when you click the tool's " + "'Connect to MIPS' button ('Assemble and Run' in the stand-alone version) or the tool's Reset button!  If you run a " + "MIPS program and reset it in MARS, the controller's Ready bit is cleared to 0!  Configure the Data Segment Window to " + "display the MMIO address range so you can directly observe values stored in the MMIO addresses given above.\n" + "\n" + "COOL NEW FEATURE (MARS 4.5, AUGUST 2014): Clear the display window from MIPS program\n" + "\n" + "When ASCII 12 (form feed) is stored in the Transmitter Data register, the tool's Display window will be cleared " + "following the specified transmission delay.\n" + "\n" + "COOL NEW FEATURE (MARS 4.5, AUGUST 2014): Simulate a text-based virtual terminal with (x,y) positioning\n" + "\n" + "When ASCII 7 (bell) is stored in the Transmitter Data register, the cursor in the tool's Display window will " + "be positioned at the (X,Y) coordinate specified by its high-order 3 bytes, following the specfied transmission delay. " + "Place the X position (column) in bit positions 20-31 of the " + "Transmitter Data register and place the Y position (row) in bit positions 8-19.  The cursor is not displayed " + "but subsequent transmitted characters will be displayed starting at that position. Position (0,0) is at upper left. " + "Why did I select the ASCII Bell character?  Just for fun!\n" + "\n" + "The dimensions (number of columns and rows) of the virtual text-based terminal are calculated based on the display " + "window size and font specifications.  This calculation occurs during program execution upon first use of the ASCII 7 code. " + "It will not change until the Reset button is clicked, even if the window is resized.  The window dimensions are included in " + "its title, which will be updated upon window resize or font change.  No attempt is made to reposition data characters already " + "transmitted by the program.  To change the dimensions of the virtual terminal, resize the Display window as desired (note there " + "is an adjustible splitter between the Display and Keyboard windows) then click the tool's Reset button.  " + "Implementation detail: the window is implemented by a JTextArea to which text is written as a string. " + "Its caret (cursor) position is required to be a position within the string.  I simulated a text terminal with random positioning " + "by pre-allocating a string of spaces with one space per (X,Y) position and an embedded newline where each line ends. Each character " + "transmitted to the window thus replaces an existing character in the string.\n" + "\n" + "Thanks to Eric Wang at Washington State University, who requested these features to enable use of this display as the target " + "for programming MMIO text-based games.\n" + "\n" + "Contact Pete Sanderson at psanderson@otterbein.edu with questions or comments.\n";
      JButton help = new JButton("Help");
      help.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            JTextArea ja = new JTextArea(helpContent);
            ja.setRows(30);
            ja.setColumns(60);
            ja.setLineWrap(true);
            ja.setWrapStyleWord(true);
            String title = "Simulating the Keyboard and Display";
            final JDialog d = KeyboardAndDisplaySimulator.this.theWindow instanceof Dialog ? new JDialog((Dialog)KeyboardAndDisplaySimulator.this.theWindow, "Simulating the Keyboard and Display", false) : new JDialog((Frame)KeyboardAndDisplaySimulator.this.theWindow, "Simulating the Keyboard and Display", false);
            d.setSize(ja.getPreferredSize());
            d.getContentPane().setLayout(new BorderLayout());
            d.getContentPane().add(new JScrollPane(ja), "Center");
            JButton b = new JButton("Close");
            b.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent ev) {
                  d.setVisible(false);
                  d.dispose();
               }
            });
            JPanel p = new JPanel();
            p.add(b);
            d.getContentPane().add(p, "South");
            d.setLocationRelativeTo(KeyboardAndDisplaySimulator.this.theWindow);
            d.setVisible(true);
         }
      });
      return help;
   }

   private JComponent buildDisplay() {
      this.displayPanel = new JPanel(new BorderLayout());
      TitledBorder tb = new TitledBorder(displayPanelTitle);
      tb.setTitleJustification(2);
      this.displayPanel.setBorder(tb);
      this.display = new JTextArea();
      this.display.setFont(this.defaultFont);
      this.display.setEditable(false);
      this.display.setMargin(textAreaInsets);
      this.updateDisplayBorder = new DisplayResizeAdapter();
      this.display.addComponentListener(this.updateDisplayBorder);
      this.display.addCaretListener(new CaretListener() {
         public void caretUpdate(CaretEvent e) {
            KeyboardAndDisplaySimulator.this.simulator.repaintDisplayPanelBorder();
         }
      });
      DefaultCaret caret = (DefaultCaret)this.display.getCaret();
      caret.setUpdatePolicy(2);
      this.displayScrollPane = new JScrollPane(this.display);
      this.displayScrollPane.setPreferredSize(preferredTextAreaDimension);
      this.displayPanel.add(this.displayScrollPane);
      this.displayOptions = new JPanel();
      this.delayTechniqueChooser = new JComboBox(this.delayTechniques);
      this.delayTechniqueChooser.setToolTipText("Technique for determining simulated transmitter device processing delay");
      this.delayTechniqueChooser.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            KeyboardAndDisplaySimulator.this.transmitDelayInstructionCountLimit = KeyboardAndDisplaySimulator.this.generateDelay();
         }
      });
      this.delayLengthPanel = new DelayLengthPanel();
      this.displayAfterDelayCheckBox = new JCheckBox("DAD", true);
      this.displayAfterDelayCheckBox.setToolTipText("Display After Delay: if checked, transmitter data not displayed until after delay");
      this.displayAfterDelayCheckBox.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            KeyboardAndDisplaySimulator.this.displayAfterDelay = KeyboardAndDisplaySimulator.this.displayAfterDelayCheckBox.isSelected();
         }
      });
      this.fontButton = new JButton("Font");
      this.fontButton.setToolTipText("Select the font for the display panel");
      this.fontButton.addActionListener(new FontChanger());
      this.displayOptions.add(this.fontButton);
      this.displayOptions.add(this.displayAfterDelayCheckBox);
      this.displayOptions.add(this.delayTechniqueChooser);
      this.displayOptions.add(this.delayLengthPanel);
      this.displayPanel.add(this.displayOptions, "South");
      return this.displayPanel;
   }

   private JComponent buildKeyboard() {
      this.keyboardPanel = new JPanel(new BorderLayout());
      this.keyEventAccepter = new JTextArea();
      this.keyEventAccepter.setEditable(true);
      this.keyEventAccepter.setFont(this.defaultFont);
      this.keyEventAccepter.setMargin(textAreaInsets);
      this.keyAccepterScrollPane = new JScrollPane(this.keyEventAccepter);
      this.keyAccepterScrollPane.setPreferredSize(preferredTextAreaDimension);
      this.keyEventAccepter.addKeyListener(new KeyboardKeyListener());
      this.keyboardPanel.add(this.keyAccepterScrollPane);
      TitledBorder tb = new TitledBorder(keyboardPanelTitle);
      tb.setTitleJustification(2);
      this.keyboardPanel.setBorder(tb);
      return this.keyboardPanel;
   }

   private void updateMMIOControl(int addr, int intValue) {
      this.updateMMIOControlAndData(addr, intValue, 0, 0, true);
   }

   private void updateMMIOControlAndData(int controlAddr, int controlValue, int dataAddr, int dataValue) {
      this.updateMMIOControlAndData(controlAddr, controlValue, dataAddr, dataValue, false);
   }

   private synchronized void updateMMIOControlAndData(int controlAddr, int controlValue, int dataAddr, int dataValue, boolean controlOnly) {
      if (!this.isBeingUsedAsAMarsTool || this.isBeingUsedAsAMarsTool && this.connectButton.isConnected()) {
         synchronized(Globals.memoryAndRegistersLock) {
            try {
               Globals.memory.setRawWord(controlAddr, controlValue);
               if (!controlOnly) {
                  Globals.memory.setRawWord(dataAddr, dataValue);
               }
            } catch (AddressErrorException var9) {
               System.out.println("Tool author specified incorrect MMIO address!" + var9);
               System.exit(0);
            }
         }

         if (Globals.getGui() != null && Globals.getGui().getMainPane().getExecutePane().getTextSegmentWindow().getCodeHighlighting()) {
            Globals.getGui().getMainPane().getExecutePane().getDataSegmentWindow().updateValues();
         }
      }

   }

   private static boolean isReadyBitSet(int mmioControlRegister) {
      try {
         return (Globals.memory.get(mmioControlRegister, 4) & 1) == 1;
      } catch (AddressErrorException var2) {
         System.out.println("Tool author specified incorrect MMIO address!" + var2);
         System.exit(0);
         return false;
      }
   }

   private static int readyBitSet(int mmioControlRegister) {
      try {
         return Globals.memory.get(mmioControlRegister, 4) | 1;
      } catch (AddressErrorException var2) {
         System.out.println("Tool author specified incorrect MMIO address!" + var2);
         System.exit(0);
         return 1;
      }
   }

   private static int readyBitCleared(int mmioControlRegister) {
      try {
         return Globals.memory.get(mmioControlRegister, 4) & 2;
      } catch (AddressErrorException var2) {
         System.out.println("Tool author specified incorrect MMIO address!" + var2);
         System.exit(0);
         return 0;
      }
   }

   private void initializeTransmitDelaySimulator() {
      this.countingInstructions = false;
      this.instructionCount = 0;
      this.transmitDelayInstructionCountLimit = this.generateDelay();
   }

   private int generateDelay() {
      double sliderValue = this.delayLengthPanel.getDelayLength();
      TransmitterDelayTechnique technique = (TransmitterDelayTechnique)this.delayTechniqueChooser.getSelectedItem();
      return technique.generateDelay(sliderValue);
   }

   private class FontChanger implements ActionListener {
      private FontChanger() {
      }

      public void actionPerformed(ActionEvent e) {
         JButton button = (JButton)e.getSource();
         FontSettingDialog fontDialog = KeyboardAndDisplaySimulator.this.new FontSettingDialog((Frame)null, "Select Text Font", KeyboardAndDisplaySimulator.this.display.getFont());
         Font newFont = fontDialog.showDialog();
      }

      // $FF: synthetic method
      FontChanger(Object x1) {
         this();
      }
   }

   private class FontSettingDialog extends AbstractFontSettingDialog {
      private boolean resultOK;

      public FontSettingDialog(Frame owner, String title, Font currentFont) {
         super(owner, title, true, currentFont);
      }

      private Font showDialog() {
         this.resultOK = true;
         this.setVisible(true);
         return this.resultOK ? this.getFont() : null;
      }

      protected void closeDialog() {
         this.setVisible(false);
         KeyboardAndDisplaySimulator.this.updateDisplayBorder.componentResized((ComponentEvent)null);
      }

      private void performCancel() {
         this.resultOK = false;
      }

      protected Component buildControlPanel() {
         Box controlPanel = Box.createHorizontalBox();
         JButton okButton = new JButton("OK");
         okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               FontSettingDialog.this.apply(FontSettingDialog.this.getFont());
               FontSettingDialog.this.closeDialog();
            }
         });
         JButton cancelButton = new JButton("Cancel");
         cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               FontSettingDialog.this.performCancel();
               FontSettingDialog.this.closeDialog();
            }
         });
         JButton resetButton = new JButton("Reset");
         resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               FontSettingDialog.this.reset();
            }
         });
         controlPanel.add(Box.createHorizontalGlue());
         controlPanel.add(okButton);
         controlPanel.add(Box.createHorizontalGlue());
         controlPanel.add(cancelButton);
         controlPanel.add(Box.createHorizontalGlue());
         controlPanel.add(resetButton);
         controlPanel.add(Box.createHorizontalGlue());
         return controlPanel;
      }

      protected void apply(Font font) {
         KeyboardAndDisplaySimulator.this.display.setFont(font);
         KeyboardAndDisplaySimulator.this.keyEventAccepter.setFont(font);
      }
   }

   private class NormallyDistributedDelay implements TransmitterDelayTechnique {
      Random randn = new Random();

      public NormallyDistributedDelay() {
      }

      public String toString() {
         return "'Normally' distributed delay: floor(abs(N(0,1)*slider)+1)";
      }

      public int generateDelay(double mult) {
         return (int)(Math.abs(this.randn.nextGaussian() * mult) + 1.0);
      }
   }

   private class UniformlyDistributedDelay implements TransmitterDelayTechnique {
      Random randu = new Random();

      public UniformlyDistributedDelay() {
      }

      public String toString() {
         return "Uniformly distributed delay, min=1, max=slider";
      }

      public int generateDelay(double max) {
         return this.randu.nextInt((int)max) + 1;
      }
   }

   private class FixedLengthDelay implements TransmitterDelayTechnique {
      private FixedLengthDelay() {
      }

      public String toString() {
         return "Fixed transmitter delay, select using slider";
      }

      public int generateDelay(double fixedDelay) {
         return (int)fixedDelay;
      }

      // $FF: synthetic method
      FixedLengthDelay(Object x1) {
         this();
      }
   }

   private interface TransmitterDelayTechnique {
      int generateDelay(double var1);
   }

   private class DelayLengthPanel extends JPanel {
      private static final int DELAY_INDEX_MIN = 0;
      private static final int DELAY_INDEX_MAX = 40;
      private static final int DELAY_INDEX_INIT = 4;
      private double[] delayTable = new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 10.0, 20.0, 30.0, 40.0, 50.0, 100.0, 150.0, 200.0, 300.0, 400.0, 500.0, 600.0, 700.0, 800.0, 900.0, 1000.0, 1500.0, 2000.0, 3000.0, 4000.0, 5000.0, 6000.0, 7000.0, 8000.0, 9000.0, 10000.0, 20000.0, 40000.0, 60000.0, 80000.0, 100000.0, 200000.0, 400000.0, 600000.0, 800000.0, 1000000.0};
      private JLabel sliderLabel = null;
      private volatile int delayLengthIndex = 4;

      public DelayLengthPanel() {
         super(new BorderLayout());
         KeyboardAndDisplaySimulator.this.delayLengthSlider = new JSlider(0, 0, 40, 4);
         KeyboardAndDisplaySimulator.this.delayLengthSlider.setSize(new Dimension(100, (int)KeyboardAndDisplaySimulator.this.delayLengthSlider.getSize().getHeight()));
         KeyboardAndDisplaySimulator.this.delayLengthSlider.setMaximumSize(KeyboardAndDisplaySimulator.this.delayLengthSlider.getSize());
         KeyboardAndDisplaySimulator.this.delayLengthSlider.addChangeListener(new DelayLengthListener());
         this.sliderLabel = new JLabel(this.setLabel(this.delayLengthIndex));
         this.sliderLabel.setHorizontalAlignment(0);
         this.sliderLabel.setAlignmentX(0.5F);
         this.add(this.sliderLabel, "North");
         this.add(KeyboardAndDisplaySimulator.this.delayLengthSlider, "Center");
         this.setToolTipText("Parameter for simulated delay length (MIPS instruction execution count)");
      }

      public double getDelayLength() {
         return this.delayTable[this.delayLengthIndex];
      }

      private String setLabel(int index) {
         return "Delay length: " + (int)this.delayTable[index] + " instruction executions";
      }

      private class DelayLengthListener implements ChangeListener {
         private DelayLengthListener() {
         }

         public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider)e.getSource();
            if (!source.getValueIsAdjusting()) {
               DelayLengthPanel.this.delayLengthIndex = source.getValue();
               KeyboardAndDisplaySimulator.this.transmitDelayInstructionCountLimit = KeyboardAndDisplaySimulator.this.generateDelay();
            } else {
               DelayLengthPanel.this.sliderLabel.setText(DelayLengthPanel.this.setLabel(source.getValue()));
            }

         }

         // $FF: synthetic method
         DelayLengthListener(Object x1) {
            this();
         }
      }
   }

   private class KeyboardKeyListener implements KeyListener {
      private KeyboardKeyListener() {
      }

      public void keyTyped(KeyEvent e) {
         int updatedReceiverControl = KeyboardAndDisplaySimulator.readyBitSet(KeyboardAndDisplaySimulator.RECEIVER_CONTROL);
         KeyboardAndDisplaySimulator.this.updateMMIOControlAndData(KeyboardAndDisplaySimulator.RECEIVER_CONTROL, updatedReceiverControl, KeyboardAndDisplaySimulator.RECEIVER_DATA, e.getKeyChar() & 255);
         if (updatedReceiverControl != 1 && (Coprocessor0.getValue(12) & 2) == 0 && (Coprocessor0.getValue(12) & 1) == 1) {
            Simulator.externalInterruptingDevice = 64;
         }

      }

      public void keyPressed(KeyEvent e) {
      }

      public void keyReleased(KeyEvent e) {
      }

      // $FF: synthetic method
      KeyboardKeyListener(Object x1) {
         this();
      }
   }

   private class DisplayResizeAdapter extends ComponentAdapter {
      private DisplayResizeAdapter() {
      }

      public void componentResized(ComponentEvent e) {
         KeyboardAndDisplaySimulator.this.getDisplayPanelTextDimensions();
         KeyboardAndDisplaySimulator.this.repaintDisplayPanelBorder();
      }

      // $FF: synthetic method
      DisplayResizeAdapter(Object x1) {
         this();
      }
   }
}
