package mars.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Observable;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import mars.Globals;
import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.Register;
import mars.util.Binary;

public class FloatRepresentation extends AbstractMarsToolAndApplication {
   private static String version = "Version 1.1";
   private static String heading = "32-bit IEEE 754 Floating Point Representation";
   private static final String title = "Floating Point Representation, ";
   private static final String defaultHex = "00000000";
   private static final String defaultDecimal = "0.0";
   private static final String defaultBinarySign = "0";
   private static final String defaultBinaryExponent = "00000000";
   private static final String defaultBinaryFraction = "00000000000000000000000";
   private static final int maxLengthHex = 8;
   private static final int maxLengthBinarySign = 1;
   private static final int maxLengthBinaryExponent = 8;
   private static final int maxLengthBinaryFraction = 23;
   private static final int maxLengthBinaryTotal = 32;
   private static final int maxLengthDecimal = 20;
   private static final String denormalizedLabel = "                 significand (denormalized - no 'hidden bit')";
   private static final String normalizedLabel = "                 significand ('hidden bit' underlined)       ";
   private static final Font instructionsFont = new Font("Arial", 0, 14);
   private static final Font hexDisplayFont = new Font("Courier", 0, 32);
   private static final Font binaryDisplayFont = new Font("Courier", 0, 18);
   private static final Font decimalDisplayFont = new Font("Courier", 0, 18);
   private static final Color hexDisplayColor;
   private static Color binaryDisplayColor;
   private static final Color decimalDisplayColor;
   private static final String expansionFontTag = "<font size=\"+1\" face=\"Courier\" color=\"#000000\">";
   private static final String instructionFontTag = "<font size=\"+0\" face=\"Verdana, Arial, Helvetica\" color=\"#000000\">";
   private static final int exponentBias = 127;
   private Register attachedRegister;
   private Register[] fpRegisters;
   private FloatRepresentation thisFloatTool;
   private JPanel binarySignDecoratedDisplay;
   private JPanel binaryExponentDecoratedDisplay;
   private JPanel binaryFractionDecoratedDisplay;
   private JTextField hexDisplay;
   private JTextField decimalDisplay;
   private JTextField binarySignDisplay;
   private JTextField binaryExponentDisplay;
   private JTextField binaryFractionDisplay;
   private JLabel expansionDisplay;
   private JLabel significandLabel;
   private BinaryToDecimalFormulaGraphic binaryToDecimalFormulaGraphic;
   private InstructionsPane instructions;
   private String defaultInstructions;
   private static final String zeroes = "0000000000000000000000000000000000000000000000000000000000000000";
   private static final String HTMLspaces = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";

   private boolean darkTheme = Globals.getSettings().getBooleanSetting(21);

   public FloatRepresentation(String title, String heading) {
      super(title, heading);
      this.attachedRegister = null;
      this.significandLabel = new JLabel("                 significand (denormalized - no 'hidden bit')", 0);
      this.defaultInstructions = "Modify any value then press the Enter key to update all values.";
      this.thisFloatTool = this;
   }

   public FloatRepresentation() {
      this("Floating Point Representation, " + version, heading);
      if(darkTheme) {
         binaryDisplayColor = new Color(0xf0f0f0);
      }
   }

   public static void main(String[] args) {
      (new FloatRepresentation("Floating Point Representation, " + version, heading)).go();
   }

   public String getName() {
      return "Floating Point Representation";
   }

   protected void addAsObserver() {
      this.addAsObserver(this.attachedRegister);
   }

   protected void deleteAsObserver() {
      this.deleteAsObserver(this.attachedRegister);
   }

   protected JComponent buildMainDisplayArea() {
      return this.buildDisplayArea();
   }

   public void update(Observable register, Object accessNotice) {
      if (((AccessNotice)accessNotice).getAccessType() == 1) {
         this.updateDisplays((new FlavorsOfFloat()).buildOneFromInt(this.attachedRegister.getValue()));
      }

   }

   protected void reset() {
      this.instructions.setText(this.defaultInstructions);
      this.updateDisplaysAndRegister(new FlavorsOfFloat());
   }

   protected JComponent buildDisplayArea() {
      Box mainPanel = Box.createVerticalBox();
      JPanel leftPanel = new JPanel(new GridLayout(5, 1, 0, 0));
      JPanel rightPanel = new JPanel(new GridLayout(5, 1, 0, 0));
      Box subMainPanel = Box.createHorizontalBox();
      subMainPanel.add(leftPanel);
      subMainPanel.add(rightPanel);
      mainPanel.add(subMainPanel);
      this.hexDisplay = new JTextField("00000000", 9);
      this.hexDisplay.setFont(hexDisplayFont);
      this.hexDisplay.setForeground(hexDisplayColor);
      this.hexDisplay.setHorizontalAlignment(4);
      this.hexDisplay.setToolTipText("8-digit hexadecimal (base 16) display");
      this.hexDisplay.setEditable(true);
      this.hexDisplay.revalidate();
      this.hexDisplay.addKeyListener(new HexDisplayKeystrokeListener(8));
      JPanel hexPanel = new JPanel();
      hexPanel.add(this.hexDisplay);
      leftPanel.add(hexPanel);
      HexToBinaryGraphicPanel hexToBinaryGraphic = new HexToBinaryGraphicPanel();
      leftPanel.add(hexToBinaryGraphic);
      this.binarySignDisplay = new JTextField("0", 2);
      this.binarySignDisplay.setFont(binaryDisplayFont);
      this.binarySignDisplay.setForeground(binaryDisplayColor);
      this.binarySignDisplay.setHorizontalAlignment(4);
      this.binarySignDisplay.setToolTipText("The sign bit");
      this.binarySignDisplay.setEditable(true);
      this.binarySignDisplay.revalidate();
      this.binaryExponentDisplay = new JTextField("00000000", 9);
      this.binaryExponentDisplay.setFont(binaryDisplayFont);
      this.binaryExponentDisplay.setForeground(binaryDisplayColor);
      this.binaryExponentDisplay.setHorizontalAlignment(4);
      this.binaryExponentDisplay.setToolTipText("8-bit exponent");
      this.binaryExponentDisplay.setEditable(true);
      this.binaryExponentDisplay.revalidate();
      this.binaryFractionDisplay = new BinaryFractionDisplayTextField("00000000000000000000000", 24);
      this.binaryFractionDisplay.setFont(binaryDisplayFont);
      this.binaryFractionDisplay.setForeground(binaryDisplayColor);
      this.binaryFractionDisplay.setHorizontalAlignment(4);
      this.binaryFractionDisplay.setToolTipText("23-bit fraction");
      this.binaryFractionDisplay.setEditable(true);
      this.binaryFractionDisplay.revalidate();
      this.binarySignDisplay.addKeyListener(new BinaryDisplayKeystrokeListener(1));
      this.binaryExponentDisplay.addKeyListener(new BinaryDisplayKeystrokeListener(8));
      this.binaryFractionDisplay.addKeyListener(new BinaryDisplayKeystrokeListener(23));
      JPanel binaryPanel = new JPanel();
      this.binarySignDecoratedDisplay = new JPanel(new BorderLayout());
      this.binaryExponentDecoratedDisplay = new JPanel(new BorderLayout());
      this.binaryFractionDecoratedDisplay = new JPanel(new BorderLayout());
      this.binarySignDecoratedDisplay.add(this.binarySignDisplay, "Center");
      this.binarySignDecoratedDisplay.add(new JLabel("sign", 0), "South");
      this.binaryExponentDecoratedDisplay.add(this.binaryExponentDisplay, "Center");
      this.binaryExponentDecoratedDisplay.add(new JLabel("exponent", 0), "South");
      this.binaryFractionDecoratedDisplay.add(this.binaryFractionDisplay, "Center");
      this.binaryFractionDecoratedDisplay.add(new JLabel("fraction", 0), "South");
      binaryPanel.add(this.binarySignDecoratedDisplay);
      binaryPanel.add(this.binaryExponentDecoratedDisplay);
      binaryPanel.add(this.binaryFractionDecoratedDisplay);
      leftPanel.add(binaryPanel);
      this.binaryToDecimalFormulaGraphic = new BinaryToDecimalFormulaGraphic();
      this.binaryToDecimalFormulaGraphic.setBackground(leftPanel.getBackground());
      leftPanel.add(this.binaryToDecimalFormulaGraphic);
      this.expansionDisplay = new JLabel((new FlavorsOfFloat()).expansionString);
      this.expansionDisplay.setFont(new Font("Monospaced", 0, 12));
      this.expansionDisplay.setFocusable(false);
      this.expansionDisplay.setBackground(leftPanel.getBackground());
      JPanel expansionDisplayBox = new JPanel(new GridLayout(2, 1));
      expansionDisplayBox.add(this.expansionDisplay);
      expansionDisplayBox.add(this.significandLabel);
      leftPanel.add(expansionDisplayBox);
      this.decimalDisplay = new JTextField("0.0", 21);
      this.decimalDisplay.setFont(decimalDisplayFont);
      this.decimalDisplay.setForeground(decimalDisplayColor);
      this.decimalDisplay.setHorizontalAlignment(4);
      this.decimalDisplay.setToolTipText("Decimal floating point value");
      this.decimalDisplay.setMargin(new Insets(0, 0, 0, 0));
      this.decimalDisplay.setEditable(true);
      this.decimalDisplay.revalidate();
      this.decimalDisplay.addKeyListener(new DecimalDisplayKeystokeListenter());
      Box decimalDisplayBox = Box.createVerticalBox();
      decimalDisplayBox.add(Box.createVerticalStrut(5));
      decimalDisplayBox.add(this.decimalDisplay);
      decimalDisplayBox.add(Box.createVerticalStrut(15));
      FlowLayout rightPanelLayout = new FlowLayout(0);
      JPanel place1 = new JPanel(rightPanelLayout);
      JPanel place2 = new JPanel(rightPanelLayout);
      JPanel place3 = new JPanel(rightPanelLayout);
      JPanel place4 = new JPanel(rightPanelLayout);
      String color = "#000000";
      if(darkTheme)
         color = "#f0f0f0";
      JEditorPane hexExplain = new JEditorPane("text/html", "<font size=\"+1\" face=\"Courier\" color=\"" + color + "\">&lt;&nbsp;&nbsp;Hexadecimal representation</font>");
      hexExplain.setEditable(false);
      hexExplain.setFocusable(false);
      hexExplain.setForeground(Color.black);
      hexExplain.setBackground(place1.getBackground());
      JEditorPane hexToBinExplain = new JEditorPane("text/html", "<font size=\"+1\" face=\"Courier\" color=\"" + color + "\">&lt;&nbsp;&nbsp;Each hex digit represents 4 bits</font>");
      hexToBinExplain.setEditable(false);
      hexToBinExplain.setFocusable(false);
      hexToBinExplain.setBackground(place2.getBackground());
      JEditorPane binExplain = new JEditorPane("text/html", "<font size=\"+1\" face=\"Courier\" color=\"" + color + "\">&lt;&nbsp;&nbsp;Binary representation</font>");
      binExplain.setEditable(false);
      binExplain.setFocusable(false);
      binExplain.setBackground(place3.getBackground());
      JEditorPane binToDecExplain = new JEditorPane("text/html", "<font size=\"+1\" face=\"Courier\" color=\"" + color + "\">&lt;&nbsp;&nbsp;Binary-to-decimal conversion</font>");
      binToDecExplain.setEditable(false);
      binToDecExplain.setFocusable(false);
      binToDecExplain.setBackground(place4.getBackground());
      place1.add(hexExplain);
      place2.add(hexToBinExplain);
      place3.add(binExplain);
      place4.add(binToDecExplain);
      rightPanel.add(place1);
      rightPanel.add(place2);
      rightPanel.add(place3);
      rightPanel.add(place4);
      rightPanel.add(decimalDisplayBox);
      JPanel instructionsPanel = new JPanel(new FlowLayout(0));
      this.instructions = new InstructionsPane(instructionsPanel);
      instructionsPanel.add(this.instructions);
      instructionsPanel.setBorder(new TitledBorder("Instructions"));
      mainPanel.add(instructionsPanel);
      this.fpRegisters = Coprocessor1.getRegisters();
      String[] registerList = new String[this.fpRegisters.length + 1];
      registerList[0] = "None";

      for(int i = 0; i < this.fpRegisters.length; ++i) {
         registerList[i + 1] = this.fpRegisters[i].getName();
      }

      JComboBox registerSelect = new JComboBox(registerList);
      registerSelect.setSelectedIndex(0);
      registerSelect.setToolTipText("Attach to selected FP register");
      registerSelect.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            JComboBox cb = (JComboBox)e.getSource();
            int selectedIndex = cb.getSelectedIndex();
            if (FloatRepresentation.this.isObserving()) {
               FloatRepresentation.this.deleteAsObserver();
            }

            if (selectedIndex == 0) {
               FloatRepresentation.this.attachedRegister = null;
               FloatRepresentation.this.updateDisplays(FloatRepresentation.this.new FlavorsOfFloat());
               FloatRepresentation.this.instructions.setText("The program is not attached to any MIPS floating point registers.");
            } else {
               FloatRepresentation.this.attachedRegister = FloatRepresentation.this.fpRegisters[selectedIndex - 1];
               FloatRepresentation.this.updateDisplays((FloatRepresentation.this.new FlavorsOfFloat()).buildOneFromInt(FloatRepresentation.this.attachedRegister.getValue()));
               if (FloatRepresentation.this.isObserving()) {
                  FloatRepresentation.this.addAsObserver();
               }

               FloatRepresentation.this.instructions.setText("The program and register " + FloatRepresentation.this.attachedRegister.getName() + " will respond to each other when MIPS program connected or running.");
            }

         }
      });
      JPanel registerPanel = new JPanel(new BorderLayout(5, 5));
      JPanel registerAndLabel = new JPanel();
      registerAndLabel.add(new JLabel("MIPS floating point Register of interest: "));
      registerAndLabel.add(registerSelect);
      registerPanel.add(registerAndLabel, "West");
      registerPanel.add(new JLabel(" "), "North");
      mainPanel.add(registerPanel);
      return mainPanel;
   }

   private synchronized void updateAnyAttachedRegister(int intValue) {
      if (this.attachedRegister != null) {
         synchronized(Globals.memoryAndRegistersLock) {
            this.attachedRegister.setValue(intValue);
         }

         if (Globals.getGui() != null) {
            Globals.getGui().getRegistersPane().getCoprocessor1Window().updateRegisters();
         }
      }

   }

   private void updateDisplays(FlavorsOfFloat flavors) {
      int hexIndex = flavors.hexString.charAt(0) != '0' || flavors.hexString.charAt(1) != 'x' && flavors.hexString.charAt(1) != 'X' ? 0 : 2;
      this.hexDisplay.setText(flavors.hexString.substring(hexIndex).toUpperCase());
      this.binarySignDisplay.setText(flavors.binaryString.substring(0, 1));
      this.binaryExponentDisplay.setText(flavors.binaryString.substring(1, 9));
      this.binaryFractionDisplay.setText(flavors.binaryString.substring(9, 32));
      this.decimalDisplay.setText(flavors.decimalString);
      this.binaryToDecimalFormulaGraphic.drawSubtractLabel(Binary.binaryStringToInt(flavors.binaryString.substring(1, 9)));
      this.expansionDisplay.setText(flavors.expansionString);
      this.updateSignificandLabel(flavors);
   }

   private void updateDisplaysAndRegister(FlavorsOfFloat flavors) {
      this.updateDisplays(flavors);
      if (this.isObserving()) {
         this.updateAnyAttachedRegister(flavors.intValue);
      }

   }

   private void updateSignificandLabel(FlavorsOfFloat flavors) {
      if (flavors.binaryString.substring(1, 9).equals("0000000000000000000000000000000000000000000000000000000000000000".substring(1, 9))) {
         if (this.significandLabel.getText().indexOf("deno") < 0) {
            this.significandLabel.setText("                 significand (denormalized - no 'hidden bit')");
         }
      } else if (this.significandLabel.getText().indexOf("unde") < 0) {
         this.significandLabel.setText("                 significand ('hidden bit' underlined)       ");
      }

   }

   static {
      hexDisplayColor = Color.red;
      binaryDisplayColor = Color.black;
      decimalDisplayColor = Color.blue;
   }

   class BinaryFractionDisplayTextField extends JTextField {
      public BinaryFractionDisplayTextField(String value, int columns) {
         super(value, columns);
      }

      public void paintComponent(Graphics g) {
         super.paintComponent(g);
      }
   }

   class InstructionsPane extends JLabel {
      InstructionsPane(Component parent) {
         super(FloatRepresentation.this.defaultInstructions);
         this.setFont(FloatRepresentation.instructionsFont);
         this.setBackground(parent.getBackground());
      }

      public void setText(String text) {
         super.setText(text);
      }
   }

   class BinaryToDecimalFormulaGraphic extends JPanel {
      final String subtractLabelTrailer = " - 127";
      final int arrowHeadOffset = 5;
      final int lowerY = 0;
      final int upperY = 50;
      int centerX;
      int exponentCenterX;
      int subtractLabelWidth;
      int subtractLabelHeight;
      int centerY = 25;
      int upperYArrowHead = 45;
      int currentExponent = Binary.binaryStringToInt("00000000");

      public void paintComponent(Graphics g) {
         super.paintComponent(g);
         this.centerX = FloatRepresentation.this.binarySignDecoratedDisplay.getX() + FloatRepresentation.this.binarySignDecoratedDisplay.getWidth() / 2;
         g.drawLine(this.centerX, 0, this.centerX, 50);
         g.drawLine(this.centerX - 5, this.upperYArrowHead, this.centerX, 50);
         g.drawLine(this.centerX + 5, this.upperYArrowHead, this.centerX, 50);
         this.centerX = FloatRepresentation.this.binaryExponentDecoratedDisplay.getX() + FloatRepresentation.this.binaryExponentDecoratedDisplay.getWidth() / 2;
         g.drawLine(this.centerX, 0, this.centerX, 50);
         g.drawLine(this.centerX - 5, this.upperYArrowHead, this.centerX, 50);
         g.drawLine(this.centerX + 5, this.upperYArrowHead, this.centerX, 50);
         this.exponentCenterX = this.centerX;
         this.subtractLabelHeight = g.getFontMetrics().getHeight();
         this.drawSubtractLabel(g, this.buildSubtractLabel(this.currentExponent));
         this.centerX = FloatRepresentation.this.binaryFractionDecoratedDisplay.getX() + FloatRepresentation.this.binaryFractionDecoratedDisplay.getWidth() / 2;
         g.drawLine(this.centerX, 0, this.centerX, 50);
         g.drawLine(this.centerX - 5, this.upperYArrowHead, this.centerX, 50);
         g.drawLine(this.centerX + 5, this.upperYArrowHead, this.centerX, 50);
      }

      public void drawSubtractLabel(int exponent) {
         if (exponent != this.currentExponent) {
            this.currentExponent = exponent;
            this.drawSubtractLabel(this.getGraphics(), this.buildSubtractLabel(exponent));
         }

      }

      private void drawSubtractLabel(Graphics g, String label) {
         Color saved = g.getColor();
         g.setColor(FloatRepresentation.this.binaryToDecimalFormulaGraphic.getBackground());
         g.fillRect(this.exponentCenterX - this.subtractLabelWidth / 2, this.centerY - this.subtractLabelHeight / 2, this.subtractLabelWidth + 2, this.subtractLabelHeight);
         g.setColor(saved);
         this.subtractLabelWidth = g.getFontMetrics().stringWidth(label);
         g.drawString(label, this.exponentCenterX - this.subtractLabelWidth / 2, this.centerY + this.subtractLabelHeight / 2 - 3);
      }

      private String buildSubtractLabel(int value) {
         return Integer.toString(value) + " - 127";
      }
   }

   class HexToBinaryGraphicPanel extends JPanel {
      public void paintComponent(Graphics g) {
         super.paintComponent(g);
         g.setColor(Color.red);
         int upperY = 0;
         int lowerY = 60;
         int hexColumnWidth = FloatRepresentation.this.hexDisplay.getWidth() / FloatRepresentation.this.hexDisplay.getColumns();
         int binaryColumnWidth = FloatRepresentation.this.binaryFractionDisplay.getWidth() / FloatRepresentation.this.binaryFractionDisplay.getColumns();

         Polygon p;
         for(int i = 1; i < 6; ++i) {
            p = new Polygon();
            p.addPoint(FloatRepresentation.this.hexDisplay.getX() + hexColumnWidth * (FloatRepresentation.this.hexDisplay.getColumns() - i) + hexColumnWidth / 2, upperY);
            p.addPoint(FloatRepresentation.this.binaryFractionDecoratedDisplay.getX() + binaryColumnWidth * (FloatRepresentation.this.binaryFractionDisplay.getColumns() - (i * 5 - i)), lowerY);
            p.addPoint(FloatRepresentation.this.binaryFractionDecoratedDisplay.getX() + binaryColumnWidth * (FloatRepresentation.this.binaryFractionDisplay.getColumns() - (i * 5 - i - 4)), lowerY);
            g.fillPolygon(p);
         }

         p = new Polygon();
         p.addPoint(FloatRepresentation.this.hexDisplay.getX() + hexColumnWidth * (FloatRepresentation.this.hexDisplay.getColumns() - 6) + hexColumnWidth / 2, upperY);
         p.addPoint(FloatRepresentation.this.binaryFractionDecoratedDisplay.getX() + binaryColumnWidth * (FloatRepresentation.this.binaryFractionDisplay.getColumns() - 20), lowerY);
         p.addPoint(FloatRepresentation.this.binaryExponentDecoratedDisplay.getX() + binaryColumnWidth * (FloatRepresentation.this.binaryExponentDisplay.getColumns() - 1), lowerY);
         g.fillPolygon(p);
         p = new Polygon();
         p.addPoint(FloatRepresentation.this.hexDisplay.getX() + hexColumnWidth * (FloatRepresentation.this.hexDisplay.getColumns() - 7) + hexColumnWidth / 2, upperY);
         p.addPoint(FloatRepresentation.this.binaryExponentDecoratedDisplay.getX() + binaryColumnWidth * (FloatRepresentation.this.binaryExponentDisplay.getColumns() - 1), lowerY);
         p.addPoint(FloatRepresentation.this.binaryExponentDecoratedDisplay.getX() + binaryColumnWidth * (FloatRepresentation.this.binaryExponentDisplay.getColumns() - 5), lowerY);
         g.fillPolygon(p);
         p = new Polygon();
         p.addPoint(FloatRepresentation.this.hexDisplay.getX() + hexColumnWidth * (FloatRepresentation.this.hexDisplay.getColumns() - 8) + hexColumnWidth / 2, upperY);
         p.addPoint(FloatRepresentation.this.binaryExponentDecoratedDisplay.getX() + binaryColumnWidth * (FloatRepresentation.this.binaryExponentDisplay.getColumns() - 5), lowerY);
         p.addPoint(FloatRepresentation.this.binarySignDecoratedDisplay.getX(), lowerY);
         g.fillPolygon(p);
      }
   }

   private class DecimalDisplayKeystokeListenter extends KeyAdapter {
      private DecimalDisplayKeystokeListenter() {
      }

      public void keyTyped(KeyEvent e) {
         JTextField source = (JTextField)e.getComponent();
         if (e.getKeyChar() != '\b') {
            if (!this.isDecimalFloatDigit(e.getKeyChar())) {
               if (e.getKeyChar() != '\n') {
                  FloatRepresentation.this.instructions.setText("Only digits, period, signs and E (or e) are accepted in decimal field.");
                  Toolkit.getDefaultToolkit().beep();
               }

               e.consume();
            }

         }
      }

      public void keyPressed(KeyEvent e) {
         if (e.getKeyChar() == '\n') {
            FlavorsOfFloat fof = (FloatRepresentation.this.new FlavorsOfFloat()).buildOneFromDecimalString(((JTextField)e.getSource()).getText());
            if (fof == null) {
               Toolkit.getDefaultToolkit().beep();
               FloatRepresentation.this.instructions.setText("'" + ((JTextField)e.getSource()).getText() + "' is not a valid floating point number.");
            } else {
               FloatRepresentation.this.updateDisplaysAndRegister(fof);
               FloatRepresentation.this.instructions.setText(FloatRepresentation.this.defaultInstructions);
            }

            e.consume();
         }

      }

      private boolean isDecimalFloatDigit(char digit) {
         boolean result = false;
         switch (digit) {
            case '+':
            case '-':
            case '.':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case 'E':
            case 'e':
               result = true;
            case ',':
            case '/':
            case ':':
            case ';':
            case '<':
            case '=':
            case '>':
            case '?':
            case '@':
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'G':
            case 'H':
            case 'I':
            case 'J':
            case 'K':
            case 'L':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
            case '[':
            case '\\':
            case ']':
            case '^':
            case '_':
            case '`':
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            default:
               return result;
         }
      }

      // $FF: synthetic method
      DecimalDisplayKeystokeListenter(Object x1) {
         this();
      }
   }

   private class BinaryDisplayKeystrokeListener extends KeyAdapter {
      private int bitLength;

      public BinaryDisplayKeystrokeListener(int length) {
         this.bitLength = length;
      }

      public void keyTyped(KeyEvent e) {
         JTextField source = (JTextField)e.getComponent();
         if (e.getKeyChar() != '\b') {
            if (!this.isBinaryDigit(e.getKeyChar()) || e.getKeyChar() == '\n' || source.getText().length() == this.bitLength && source.getSelectedText() == null) {
               if (e.getKeyChar() != '\n') {
                  Toolkit.getDefaultToolkit().beep();
                  if (source.getText().length() == this.bitLength && source.getSelectedText() == null) {
                     FloatRepresentation.this.instructions.setText("Maximum length of this field is " + this.bitLength + ".");
                  } else {
                     FloatRepresentation.this.instructions.setText("Only 0 and 1 are accepted in binary field.");
                  }
               }

               e.consume();
            }

         }
      }

      public void keyPressed(KeyEvent e) {
         if (e.getKeyChar() == '\n') {
            FloatRepresentation.this.updateDisplaysAndRegister((FloatRepresentation.this.new FlavorsOfFloat()).buildOneFromBinaryString());
            FloatRepresentation.this.instructions.setText(FloatRepresentation.this.defaultInstructions);
            e.consume();
         }

      }

      private boolean isBinaryDigit(char digit) {
         boolean result = false;
         switch (digit) {
            case '0':
            case '1':
               result = true;
            default:
               return result;
         }
      }
   }

   private class HexDisplayKeystrokeListener extends KeyAdapter {
      private int digitLength;

      public HexDisplayKeystrokeListener(int length) {
         this.digitLength = length;
      }

      public void keyTyped(KeyEvent e) {
         JTextField source = (JTextField)e.getComponent();
         if (e.getKeyChar() != '\b' && e.getKeyChar() != '\t') {
            if (!this.isHexDigit(e.getKeyChar()) || source.getText().length() == this.digitLength && source.getSelectedText() == null) {
               if (e.getKeyChar() != '\n' && e.getKeyChar() != '\t') {
                  Toolkit.getDefaultToolkit().beep();
                  if (source.getText().length() == this.digitLength && source.getSelectedText() == null) {
                     FloatRepresentation.this.instructions.setText("Maximum length of this field is " + this.digitLength + ".");
                  } else {
                     FloatRepresentation.this.instructions.setText("Only digits and A-F (or a-f) are accepted in hexadecimal field.");
                  }
               }

               e.consume();
            }

         }
      }

      public void keyPressed(KeyEvent e) {
         if (e.getKeyChar() == '\n' || e.getKeyChar() == '\t') {
            FloatRepresentation.this.updateDisplaysAndRegister((FloatRepresentation.this.new FlavorsOfFloat()).buildOneFromHexString(((JTextField)e.getSource()).getText()));
            FloatRepresentation.this.instructions.setText(FloatRepresentation.this.defaultInstructions);
            e.consume();
         }

      }

      private boolean isHexDigit(char digit) {
         boolean result = false;
         switch (digit) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
               result = true;
            case ':':
            case ';':
            case '<':
            case '=':
            case '>':
            case '?':
            case '@':
            case 'G':
            case 'H':
            case 'I':
            case 'J':
            case 'K':
            case 'L':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
            case '[':
            case '\\':
            case ']':
            case '^':
            case '_':
            case '`':
            default:
               return result;
         }
      }
   }

   private class FlavorsOfFloat {
      String hexString;
      String binaryString;
      String decimalString;
      String expansionString;
      int intValue;

      private FlavorsOfFloat() {
         this.hexString = "00000000";
         this.decimalString = "0.0";
         this.binaryString = "00000000000000000000000000000000";
         this.expansionString = this.buildExpansionFromBinaryString(this.binaryString);
         this.intValue = Float.floatToIntBits(Float.parseFloat(this.decimalString));
      }

      public FlavorsOfFloat buildOneFromHexString(String hexString) {
         this.hexString = "0x" + this.addLeadingZeroes(hexString.indexOf("0X") != 0 && hexString.indexOf("0x") != 0 ? hexString : hexString.substring(2), 8);
         this.binaryString = Binary.hexStringToBinaryString(this.hexString);
         this.decimalString = (new Float(Float.intBitsToFloat(Binary.binaryStringToInt(this.binaryString)))).toString();
         this.expansionString = this.buildExpansionFromBinaryString(this.binaryString);
         this.intValue = Binary.binaryStringToInt(this.binaryString);
         return this;
      }

      private FlavorsOfFloat buildOneFromBinaryString() {
         this.binaryString = this.getFullBinaryStringFromDisplays();
         this.hexString = Binary.binaryStringToHexString(this.binaryString);
         this.decimalString = (new Float(Float.intBitsToFloat(Binary.binaryStringToInt(this.binaryString)))).toString();
         this.expansionString = this.buildExpansionFromBinaryString(this.binaryString);
         this.intValue = Binary.binaryStringToInt(this.binaryString);
         return this;
      }

      private FlavorsOfFloat buildOneFromDecimalString(String decimalString) {
         float floatValue;
         try {
            floatValue = Float.parseFloat(decimalString);
         } catch (NumberFormatException var4) {
            return null;
         }

         this.decimalString = (new Float(floatValue)).toString();
         this.intValue = Float.floatToIntBits(floatValue);
         this.binaryString = Binary.intToBinaryString(this.intValue);
         this.hexString = Binary.binaryStringToHexString(this.binaryString);
         this.expansionString = this.buildExpansionFromBinaryString(this.binaryString);
         return this;
      }

      private FlavorsOfFloat buildOneFromInt(int intValue) {
         this.intValue = intValue;
         this.binaryString = Binary.intToBinaryString(intValue);
         this.hexString = Binary.binaryStringToHexString(this.binaryString);
         this.decimalString = (new Float(Float.intBitsToFloat(Binary.binaryStringToInt(this.binaryString)))).toString();
         this.expansionString = this.buildExpansionFromBinaryString(this.binaryString);
         return this;
      }

      public String buildExpansionFromBinaryString(String binaryString) {
         int biasedExponent = Binary.binaryStringToInt(binaryString.substring(1, 9));
         String stringExponent = Integer.toString(biasedExponent - 127);
         String color = "#000000";
         if(darkTheme)
            color = "#f0f0f0";
         return "<html><head></head><body><font size=\"+1\" face=\"Courier\" color=\"" + color + "\">-1<sup>" + binaryString.substring(0, 1) + "</sup> &nbsp;*&nbsp; 2<sup>" + stringExponent + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;".substring(0, (5 - stringExponent.length()) * 6) + "</sup> &nbsp;* &nbsp;" + (biasedExponent == 0 ? "&nbsp;." : "<u>1</u>.") + binaryString.substring(9, 32) + " =</font></body></html>";
      }

      private String getFullBinaryStringFromDisplays() {
         return this.addLeadingZeroes(FloatRepresentation.this.binarySignDisplay.getText(), 1) + this.addLeadingZeroes(FloatRepresentation.this.binaryExponentDisplay.getText(), 8) + this.addLeadingZeroes(FloatRepresentation.this.binaryFractionDisplay.getText(), 23);
      }

      private String addLeadingZeroes(String str, int length) {
         return str.length() < length ? "0000000000000000000000000000000000000000000000000000000000000000".substring(0, Math.min("0000000000000000000000000000000000000000000000000000000000000000".length(), length - str.length())) + str : str;
      }

      // $FF: synthetic method
      FlavorsOfFloat(Object x1) {
         this();
      }
   }
}
