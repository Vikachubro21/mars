package mars.tools;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.PointerInfo;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import mars.Globals;
import mars.ProgramStatement;
import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryAccessNotice;
import mars.mips.instructions.BasicInstruction;
import mars.mips.instructions.BasicInstructionFormat;
import mars.venus.RunAssembleAction;
import mars.venus.RunBackstepAction;
import mars.venus.RunStepAction;
import mars.venus.VenusUI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MipsXray extends AbstractMarsToolAndApplication {
   private static final long serialVersionUID = -1L;
   private static String heading = "MIPS X-Ray - Animation of MIPS Datapath";
   private static String version = " Version 2.0";
   protected Graphics g;
   protected int lastAddress = -1;
   protected JLabel label;
   private Container painel = this.getContentPane();
   private DatapathAnimation datapathAnimation;
   private GraphicsConfiguration gc;
   private BufferedImage datapath;
   private String instructionBinary;
   private JButton Assemble;
   private JButton Step;
   private JButton runBackStep;
   private Action runAssembleAction;
   private Action runStepAction;
   private Action runBackstepAction;
   private VenusUI mainUI;
   private JToolBar toolbar;
   private Timer time;

   public MipsXray(String title, String heading) {
      super(title, heading);
   }

   public MipsXray() {
      super(heading + ", " + version, heading);
   }

   public String getName() {
      return "MIPS X-Ray";
   }

   protected JComponent getHelpComponent() {
      String helpContent = "This plugin is used to visualizate the behavior of mips processor using the default datapath. \nIt reads the source code instruction and generates an animation representing the inputs and \noutputs of functional blocks and the interconnection between them.  The basic signals \nrepresented are, control signals, opcode bits and data of functional blocks.\n\nBesides the datapath representation, information for each instruction is displayed below\nthe datapath. That display includes opcode value, with the correspondent colors used to\nrepresent the signals in datapath, mnemonic of the instruction processed at the moment, registers\nused in the instruction and a label that indicates the color code used to represent control signals\n\nTo see the datapath of register bank and control units click inside the functional unit.\n\nVersion 2.0\nDeveloped by Márcio Roberto, Guilherme Sales, Fabrício Vivas, Flávio Cardeal and Fábio Lúcio\nContact Marcio Roberto at marcio.rdaraujo@gmail.com with questions or comments.\n";
      JButton help = new JButton("Help");
      help.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(MipsXray.this.theWindow, "This plugin is used to visualizate the behavior of mips processor using the default datapath. \nIt reads the source code instruction and generates an animation representing the inputs and \noutputs of functional blocks and the interconnection between them.  The basic signals \nrepresented are, control signals, opcode bits and data of functional blocks.\n\nBesides the datapath representation, information for each instruction is displayed below\nthe datapath. That display includes opcode value, with the correspondent colors used to\nrepresent the signals in datapath, mnemonic of the instruction processed at the moment, registers\nused in the instruction and a label that indicates the color code used to represent control signals\n\nTo see the datapath of register bank and control units click inside the functional unit.\n\nVersion 2.0\nDeveloped by Márcio Roberto, Guilherme Sales, Fabrício Vivas, Flávio Cardeal and Fábio Lúcio\nContact Marcio Roberto at marcio.rdaraujo@gmail.com with questions or comments.\n");
         }
      });
      return help;
   }

   protected JComponent buildAnimationSequence() {
      JPanel image = new JPanel(new GridBagLayout());
      return image;
   }

   protected JComponent buildMainDisplayArea() {
      this.mainUI = Globals.getGui();
      this.createActionObjects();
      this.toolbar = this.setUpToolBar();
      GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      this.gc = ge.getDefaultScreenDevice().getDefaultConfiguration();

      try {
         BufferedImage im = ImageIO.read(this.getClass().getResource("/images/datapath.png"));
         int transparency = im.getColorModel().getTransparency();
         this.datapath = this.gc.createCompatibleImage(im.getWidth(), im.getHeight(), transparency);
         Graphics2D g2d = this.datapath.createGraphics();
         g2d.drawImage(im, 0, 0, (ImageObserver)null);
         g2d.dispose();
      } catch (IOException var5) {
         System.out.println("Load Image error for " + this.getClass().getResource("/images/datapath.png") + ":\n" + var5);
         var5.printStackTrace();
      }

      System.setProperty("sun.java2d.translaccel", "true");
      ImageIcon icon = new ImageIcon(this.getClass().getResource("/images/datapath.png"));
      Image im = icon.getImage();
      icon = new ImageIcon(im);
      JLabel label = new JLabel(icon);
      this.painel.add(label, "West");
      this.painel.add(this.toolbar, "North");
      this.setResizable(false);
      return (JComponent)this.painel;
   }

   protected JComponent buildMainDisplayArea(String figure) {
      this.mainUI = Globals.getGui();
      this.createActionObjects();
      this.toolbar = this.setUpToolBar();
      GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      this.gc = ge.getDefaultScreenDevice().getDefaultConfiguration();

      try {
         BufferedImage im = ImageIO.read(this.getClass().getResource("/images/" + figure));
         int transparency = im.getColorModel().getTransparency();
         this.datapath = this.gc.createCompatibleImage(im.getWidth(), im.getHeight(), transparency);
         Graphics2D g2d = this.datapath.createGraphics();
         g2d.drawImage(im, 0, 0, (ImageObserver)null);
         g2d.dispose();
      } catch (IOException var6) {
         System.out.println("Load Image error for " + this.getClass().getResource("/images/" + figure) + ":\n" + var6);
         var6.printStackTrace();
      }

      System.setProperty("sun.java2d.translaccel", "true");
      ImageIcon icon = new ImageIcon(this.getClass().getResource("/images/" + figure));
      Image im = icon.getImage();
      icon = new ImageIcon(im);
      JLabel label = new JLabel(icon);
      this.painel.add(label, "West");
      this.painel.add(this.toolbar, "North");
      this.setResizable(false);
      return (JComponent)this.painel;
   }

   protected void addAsObserver() {
      this.addAsObserver(Memory.textBaseAddress, Memory.textLimitAddress);
   }

   protected void processMIPSUpdate(Observable resource, AccessNotice notice) {
      if (notice.accessIsFromMIPS()) {
         if (notice.getAccessType() == 0) {
            MemoryAccessNotice man = (MemoryAccessNotice)notice;
            int currentAdress = man.getAddress();
            if (currentAdress != this.lastAddress) {
               this.lastAddress = currentAdress;

               try {
                  BasicInstruction instr = null;
                  ProgramStatement stmt = Memory.getInstance().getStatement(currentAdress);
                  if (stmt == null) {
                     return;
                  }

                  instr = (BasicInstruction)stmt.getInstruction();
                  this.instructionBinary = stmt.getMachineStatement();
                  BasicInstructionFormat format = instr.getInstructionFormat();
                  this.painel.removeAll();
                  this.datapathAnimation = new DatapathAnimation(this.instructionBinary);
                  this.createActionObjects();
                  this.toolbar = this.setUpToolBar();
                  this.painel.add(this.toolbar, "North");
                  this.painel.add(this.datapathAnimation, "West");
                  this.datapathAnimation.startAnimation(this.instructionBinary);
               } catch (AddressErrorException var8) {
                  var8.printStackTrace();
               }

            }
         }
      }
   }

   public void updateDisplay() {
      this.repaint();
   }

   private JToolBar setUpToolBar() {
      JToolBar toolBar = new JToolBar();
      this.Assemble = new JButton(this.runAssembleAction);
      this.Assemble.setText("");
      this.runBackStep = new JButton(this.runBackstepAction);
      this.runBackStep.setText("");
      this.Step = new JButton(this.runStepAction);
      this.Step.setText("");
      toolBar.add(this.Assemble);
      toolBar.add(this.Step);
      return toolBar;
   }

   private void createActionObjects() {
      Toolkit tk = Toolkit.getDefaultToolkit();
      Class cs = this.getClass();

      try {
         this.runAssembleAction = new RunAssembleAction("Assemble", new ImageIcon(tk.getImage(cs.getResource("/images/Assemble22.png"))), "Assemble the current file and clear breakpoints", new Integer(65), KeyStroke.getKeyStroke(114, 0), this.mainUI);
         this.runStepAction = new RunStepAction("Step", new ImageIcon(tk.getImage(cs.getResource("/images/StepForward22.png"))), "Run one step at a time", new Integer(84), KeyStroke.getKeyStroke(118, 0), this.mainUI);
         this.runBackstepAction = new RunBackstepAction("Backstep", new ImageIcon(tk.getImage(cs.getResource("/images/StepBack22.png"))), "Undo the last step", new Integer(66), KeyStroke.getKeyStroke(119, 0), this.mainUI);
      } catch (Exception var4) {
         System.out.println("Internal Error: images folder not found, or other null pointer exception while creating Action objects");
         var4.printStackTrace();
         System.exit(0);
      }

   }

   class DatapathAnimation extends JPanel implements ActionListener, MouseListener {
      private static final long serialVersionUID = -2681757800180958534L;
      private int PERIOD = 5;
      private static final int PWIDTH = 1000;
      private static final int PHEIGHT = 574;
      private GraphicsConfiguration gc;
      private GraphicsDevice gd;
      private int accelMemory;
      private DecimalFormat df = new DecimalFormat("0.0");
      private int counter;
      private boolean justStarted;
      private int indexX;
      private int indexY;
      private boolean xIsMoving;
      private boolean yIsMoving;
      private Vector outputGraph;
      private ArrayList vertexList;
      private ArrayList vertexTraversed;
      private HashMap opcodeEquivalenceTable;
      private HashMap functionEquivalenceTable;
      private HashMap registerEquivalenceTable;
      private String instructionCode;
      private int countRegLabel;
      private int countALULabel;
      private int countPCLabel;
      private Color green1 = new Color(0, 153, 0);
      private Color green2 = new Color(0, 77, 0);
      private Color yellow2 = new Color(185, 182, 42);
      private Color orange1 = new Color(255, 102, 0);
      private Color orange = new Color(119, 34, 34);
      private Color blue2 = new Color(0, 153, 255);
      private int register = 1;
      private int control = 2;
      private int aluControl = 3;
      private int alu = 4;
      private int currentUnit;
      private Graphics2D g2d;
      private BufferedImage datapath;

      public void mousePressed(MouseEvent e) {
         PointerInfo a = MouseInfo.getPointerInfo();
      }

      public DatapathAnimation(String instructionBinary) {
         GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
         this.gd = ge.getDefaultScreenDevice();
         this.gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
         this.accelMemory = this.gd.getAvailableAcceleratedMemory();
         this.setBackground(Color.white);
         this.setPreferredSize(new Dimension(1000, 574));
         this.initImages();
         this.vertexList = new ArrayList();
         this.counter = 0;
         this.justStarted = true;
         this.instructionCode = instructionBinary;
         this.opcodeEquivalenceTable = new HashMap();
         this.functionEquivalenceTable = new HashMap();
         this.registerEquivalenceTable = new HashMap();
         this.countRegLabel = 400;
         this.countALULabel = 380;
         this.countPCLabel = 380;
         this.loadHashMapValues();
         this.addMouseListener(this);
      }

      public void loadHashMapValues() {
         this.importXmlStringData("/MipsXRayOpcode.xml", this.opcodeEquivalenceTable, "equivalence", "bits", "mnemonic");
         this.importXmlStringData("/MipsXRayOpcode.xml", this.functionEquivalenceTable, "function_equivalence", "bits", "mnemonic");
         this.importXmlStringData("/MipsXRayOpcode.xml", this.registerEquivalenceTable, "register_equivalence", "bits", "mnemonic");
         this.importXmlDatapathMap("/MipsXRayOpcode.xml", "datapath_map");
      }

      public void importXmlStringData(String xmlName, HashMap table, String elementTree, String tagId, String tagData) {
         DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
         dbf.setNamespaceAware(false);

         try {
            DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            Document doc = docBuilder.parse(this.getClass().getResource(xmlName).toString());
            Element root = doc.getDocumentElement();
            NodeList equivalenceList = root.getElementsByTagName(elementTree);

            for(int i = 0; i < equivalenceList.getLength(); ++i) {
               Element equivalenceItem = (Element)equivalenceList.item(i);
               NodeList bitsList = equivalenceItem.getElementsByTagName(tagId);
               NodeList mnemonic = equivalenceItem.getElementsByTagName(tagData);

               for(int j = 0; j < bitsList.getLength(); ++j) {
                  table.put(bitsList.item(j).getTextContent(), mnemonic.item(j).getTextContent());
               }
            }
         } catch (Exception var16) {
            var16.printStackTrace();
         }

      }

      public void importXmlDatapathMap(String xmlName, String elementTree) {
         DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
         dbf.setNamespaceAware(false);

         try {
            DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            Document doc = docBuilder.parse(this.getClass().getResource(xmlName).toString());
            Element root = doc.getDocumentElement();
            NodeList datapath_mapList = root.getElementsByTagName(elementTree);

            int size;
            for(size = 0; size < datapath_mapList.getLength(); ++size) {
               Element datapath_mapItem = (Element)datapath_mapList.item(size);
               NodeList index_vertex = datapath_mapItem.getElementsByTagName("num_vertex");
               NodeList name = datapath_mapItem.getElementsByTagName("name");
               NodeList init = datapath_mapItem.getElementsByTagName("init");
               NodeList end = datapath_mapItem.getElementsByTagName("end");
               NodeList color;
               if (this.instructionCode.substring(0, 6).equals("000000")) {
                  color = datapath_mapItem.getElementsByTagName("color_Rtype");
               } else if (this.instructionCode.substring(0, 6).matches("00001[0-1]")) {
                  color = datapath_mapItem.getElementsByTagName("color_Jtype");
               } else if (this.instructionCode.substring(0, 6).matches("100[0-1][0-1][0-1]")) {
                  color = datapath_mapItem.getElementsByTagName("color_LOADtype");
               } else if (this.instructionCode.substring(0, 6).matches("101[0-1][0-1][0-1]")) {
                  color = datapath_mapItem.getElementsByTagName("color_STOREtype");
               } else if (this.instructionCode.substring(0, 6).matches("0001[0-1][0-1]")) {
                  color = datapath_mapItem.getElementsByTagName("color_BRANCHtype");
               } else {
                  color = datapath_mapItem.getElementsByTagName("color_Itype");
               }

               NodeList other_axis = datapath_mapItem.getElementsByTagName("other_axis");
               NodeList isMovingXaxis = datapath_mapItem.getElementsByTagName("isMovingXaxis");
               NodeList targetVertex = datapath_mapItem.getElementsByTagName("target_vertex");
               NodeList isText = datapath_mapItem.getElementsByTagName("is_text");

               for(int j = 0; j < index_vertex.getLength(); ++j) {
                  Vertex vert = MipsXray.this.new Vertex(Integer.parseInt(index_vertex.item(j).getTextContent()), Integer.parseInt(init.item(j).getTextContent()), Integer.parseInt(end.item(j).getTextContent()), name.item(j).getTextContent(), Integer.parseInt(other_axis.item(j).getTextContent()), Boolean.parseBoolean(isMovingXaxis.item(j).getTextContent()), color.item(j).getTextContent(), targetVertex.item(j).getTextContent(), Boolean.parseBoolean(isText.item(j).getTextContent()));
                  this.vertexList.add(vert);
               }
            }

            this.outputGraph = new Vector();
            this.vertexTraversed = new ArrayList();
            size = this.vertexList.size();

            int i;
            Vector vertexOfTargets;
            for(i = 0; i < this.vertexList.size(); ++i) {
               Vertex vertex = (Vertex)this.vertexList.get(i);
               ArrayList targetList = vertex.getTargetVertex();
               vertexOfTargets = new Vector();

               for(int k = 0; k < targetList.size(); ++k) {
                  vertexOfTargets.add(this.vertexList.get((Integer)targetList.get(k)));
               }

               this.outputGraph.add(vertexOfTargets);
            }

            for(i = 0; i < this.outputGraph.size(); ++i) {
               vertexOfTargets = (Vector)this.outputGraph.get(i);
            }

            ((Vertex)this.vertexList.get(0)).setActive(true);
            this.vertexTraversed.add(this.vertexList.get(0));
         } catch (Exception var25) {
            var25.printStackTrace();
         }

      }

      public void setUpInstructionInfo(Graphics2D g2d) {
         FontRenderContext frc = g2d.getFontRenderContext();
         Font font = new Font("Digital-7", 0, 15);
         Font fontTitle = new Font("Verdana", 0, 10);
         TextLayout textVariable;
         if (this.instructionCode.substring(0, 6).equals("000000")) {
            textVariable = new TextLayout("REGISTER TYPE INSTRUCTION", new Font("Arial", 1, 25), frc);
            g2d.setColor(Color.black);
            textVariable.draw(g2d, 280.0F, 30.0F);
            textVariable = new TextLayout("opcode", fontTitle, frc);
            g2d.setColor(Color.red);
            textVariable.draw(g2d, 25.0F, 530.0F);
            textVariable = new TextLayout(this.instructionCode.substring(0, 6), font, frc);
            g2d.setColor(Color.magenta);
            textVariable.draw(g2d, 25.0F, 550.0F);
            textVariable = new TextLayout("rs", fontTitle, frc);
            g2d.setColor(Color.red);
            textVariable.draw(g2d, 90.0F, 530.0F);
            textVariable = new TextLayout(this.instructionCode.substring(6, 11), font, frc);
            g2d.setColor(Color.green);
            textVariable.draw(g2d, 90.0F, 550.0F);
            textVariable = new TextLayout("rt", fontTitle, frc);
            g2d.setColor(Color.red);
            textVariable.draw(g2d, 150.0F, 530.0F);
            textVariable = new TextLayout(this.instructionCode.substring(11, 16), font, frc);
            g2d.setColor(Color.blue);
            textVariable.draw(g2d, 150.0F, 550.0F);
            textVariable = new TextLayout("rd", fontTitle, frc);
            g2d.setColor(Color.red);
            textVariable.draw(g2d, 210.0F, 530.0F);
            textVariable = new TextLayout(this.instructionCode.substring(16, 21), font, frc);
            g2d.setColor(Color.cyan);
            textVariable.draw(g2d, 210.0F, 550.0F);
            textVariable = new TextLayout("shamt", fontTitle, frc);
            g2d.setColor(Color.red);
            textVariable.draw(g2d, 270.0F, 530.0F);
            textVariable = new TextLayout(this.instructionCode.substring(21, 26), font, frc);
            g2d.setColor(Color.black);
            textVariable.draw(g2d, 270.0F, 550.0F);
            textVariable = new TextLayout("function", fontTitle, frc);
            g2d.setColor(Color.red);
            textVariable.draw(g2d, 330.0F, 530.0F);
            textVariable = new TextLayout(this.instructionCode.substring(26, 32), font, frc);
            g2d.setColor(this.orange1);
            textVariable.draw(g2d, 330.0F, 550.0F);
            textVariable = new TextLayout("Instruction", fontTitle, frc);
            g2d.setColor(Color.red);
            textVariable.draw(g2d, 25.0F, 480.0F);
            textVariable = new TextLayout((String)this.functionEquivalenceTable.get(this.instructionCode.substring(26, 32)), font, frc);
            g2d.setColor(Color.BLACK);
            textVariable.draw(g2d, 25.0F, 500.0F);
            textVariable = new TextLayout((String)this.registerEquivalenceTable.get(this.instructionCode.substring(6, 11)), font, frc);
            g2d.setColor(Color.BLACK);
            textVariable.draw(g2d, 65.0F, 500.0F);
            textVariable = new TextLayout((String)this.registerEquivalenceTable.get(this.instructionCode.substring(16, 21)), font, frc);
            g2d.setColor(Color.BLACK);
            textVariable.draw(g2d, 105.0F, 500.0F);
            textVariable = new TextLayout((String)this.registerEquivalenceTable.get(this.instructionCode.substring(11, 16)), font, frc);
            g2d.setColor(Color.BLACK);
            textVariable.draw(g2d, 145.0F, 500.0F);
         } else if (this.instructionCode.substring(0, 6).matches("00001[0-1]")) {
            textVariable = new TextLayout("JUMP TYPE INSTRUCTION", new Font("Verdana", 1, 25), frc);
            g2d.setColor(Color.black);
            textVariable.draw(g2d, 280.0F, 30.0F);
            textVariable = new TextLayout("opcode", fontTitle, frc);
            g2d.setColor(Color.red);
            textVariable.draw(g2d, 25.0F, 530.0F);
            textVariable = new TextLayout(this.instructionCode.substring(0, 6), font, frc);
            g2d.setColor(Color.magenta);
            textVariable.draw(g2d, 25.0F, 550.0F);
            textVariable = new TextLayout("address", fontTitle, frc);
            g2d.setColor(Color.red);
            textVariable.draw(g2d, 95.0F, 530.0F);
            textVariable = new TextLayout("Instruction", fontTitle, frc);
            g2d.setColor(Color.red);
            textVariable.draw(g2d, 25.0F, 480.0F);
            textVariable = new TextLayout(this.instructionCode.substring(6, 32), font, frc);
            g2d.setColor(Color.orange);
            textVariable.draw(g2d, 95.0F, 550.0F);
            textVariable = new TextLayout((String)this.opcodeEquivalenceTable.get(this.instructionCode.substring(0, 6)), font, frc);
            g2d.setColor(Color.cyan);
            textVariable.draw(g2d, 65.0F, 500.0F);
            textVariable = new TextLayout("LABEL", font, frc);
            g2d.setColor(Color.cyan);
            textVariable.draw(g2d, 105.0F, 500.0F);
         } else if (this.instructionCode.substring(0, 6).matches("100[0-1][0-1][0-1]")) {
            textVariable = new TextLayout("LOAD TYPE INSTRUCTION", new Font("Verdana", 1, 25), frc);
            g2d.setColor(Color.black);
            textVariable.draw(g2d, 280.0F, 30.0F);
            textVariable = new TextLayout("opcode", fontTitle, frc);
            g2d.setColor(Color.red);
            textVariable.draw(g2d, 25.0F, 530.0F);
            textVariable = new TextLayout(this.instructionCode.substring(0, 6), font, frc);
            g2d.setColor(Color.magenta);
            textVariable.draw(g2d, 25.0F, 550.0F);
            textVariable = new TextLayout("rs", fontTitle, frc);
            g2d.setColor(Color.red);
            textVariable.draw(g2d, 90.0F, 530.0F);
            textVariable = new TextLayout(this.instructionCode.substring(6, 11), font, frc);
            g2d.setColor(Color.green);
            textVariable.draw(g2d, 90.0F, 550.0F);
            textVariable = new TextLayout("rt", fontTitle, frc);
            g2d.setColor(Color.red);
            textVariable.draw(g2d, 145.0F, 530.0F);
            textVariable = new TextLayout(this.instructionCode.substring(11, 16), font, frc);
            g2d.setColor(Color.blue);
            textVariable.draw(g2d, 145.0F, 550.0F);
            textVariable = new TextLayout("Immediate", fontTitle, frc);
            g2d.setColor(Color.red);
            textVariable.draw(g2d, 200.0F, 530.0F);
            textVariable = new TextLayout(this.instructionCode.substring(16, 32), font, frc);
            g2d.setColor(this.orange1);
            textVariable.draw(g2d, 200.0F, 550.0F);
            textVariable = new TextLayout("Instruction", fontTitle, frc);
            g2d.setColor(Color.red);
            textVariable.draw(g2d, 25.0F, 480.0F);
            textVariable = new TextLayout((String)this.opcodeEquivalenceTable.get(this.instructionCode.substring(0, 6)), font, frc);
            g2d.setColor(Color.BLACK);
            textVariable.draw(g2d, 25.0F, 500.0F);
            textVariable = new TextLayout((String)this.registerEquivalenceTable.get(this.instructionCode.substring(6, 11)), font, frc);
            g2d.setColor(Color.BLACK);
            textVariable.draw(g2d, 65.0F, 500.0F);
            textVariable = new TextLayout("M[ " + (String)this.registerEquivalenceTable.get(this.instructionCode.substring(16, 21)) + " + " + this.parseBinToInt(this.instructionCode.substring(6, 32)) + " ]", font, frc);
            g2d.setColor(Color.BLACK);
            textVariable.draw(g2d, 105.0F, 500.0F);
         } else if (this.instructionCode.substring(0, 6).matches("101[0-1][0-1][0-1]")) {
            textVariable = new TextLayout("STORE TYPE INSTRUCTION", new Font("Verdana", 1, 25), frc);
            g2d.setColor(Color.black);
            textVariable.draw(g2d, 280.0F, 30.0F);
            textVariable = new TextLayout("opcode", fontTitle, frc);
            g2d.setColor(Color.red);
            textVariable.draw(g2d, 25.0F, 530.0F);
            textVariable = new TextLayout(this.instructionCode.substring(0, 6), font, frc);
            g2d.setColor(Color.magenta);
            textVariable.draw(g2d, 25.0F, 550.0F);
            textVariable = new TextLayout("rs", fontTitle, frc);
            g2d.setColor(Color.red);
            textVariable.draw(g2d, 90.0F, 530.0F);
            textVariable = new TextLayout(this.instructionCode.substring(6, 11), font, frc);
            g2d.setColor(Color.green);
            textVariable.draw(g2d, 90.0F, 550.0F);
            textVariable = new TextLayout("rt", fontTitle, frc);
            g2d.setColor(Color.red);
            textVariable.draw(g2d, 145.0F, 530.0F);
            textVariable = new TextLayout(this.instructionCode.substring(11, 16), font, frc);
            g2d.setColor(Color.blue);
            textVariable.draw(g2d, 145.0F, 550.0F);
            textVariable = new TextLayout("Immediate", fontTitle, frc);
            g2d.setColor(Color.red);
            textVariable.draw(g2d, 200.0F, 530.0F);
            textVariable = new TextLayout(this.instructionCode.substring(16, 32), font, frc);
            g2d.setColor(this.orange1);
            textVariable.draw(g2d, 200.0F, 550.0F);
            textVariable = new TextLayout("Instruction", fontTitle, frc);
            g2d.setColor(Color.red);
            textVariable.draw(g2d, 25.0F, 480.0F);
            textVariable = new TextLayout((String)this.opcodeEquivalenceTable.get(this.instructionCode.substring(0, 6)), font, frc);
            g2d.setColor(Color.BLACK);
            textVariable.draw(g2d, 25.0F, 500.0F);
            textVariable = new TextLayout((String)this.registerEquivalenceTable.get(this.instructionCode.substring(6, 11)), font, frc);
            g2d.setColor(Color.BLACK);
            textVariable.draw(g2d, 65.0F, 500.0F);
            textVariable = new TextLayout("M[ " + (String)this.registerEquivalenceTable.get(this.instructionCode.substring(16, 21)) + " + " + this.parseBinToInt(this.instructionCode.substring(6, 32)) + " ]", font, frc);
            g2d.setColor(Color.BLACK);
            textVariable.draw(g2d, 105.0F, 500.0F);
         } else if (!this.instructionCode.substring(0, 6).matches("0100[0-1][0-1]")) {
            if (this.instructionCode.substring(0, 6).matches("0001[0-1][0-1]")) {
               textVariable = new TextLayout("BRANCH TYPE INSTRUCTION", new Font("Verdana", 1, 25), frc);
               g2d.setColor(Color.black);
               textVariable.draw(g2d, 250.0F, 30.0F);
               textVariable = new TextLayout("opcode", fontTitle, frc);
               g2d.setColor(Color.red);
               textVariable.draw(g2d, 25.0F, 440.0F);
               textVariable = new TextLayout("opcode", fontTitle, frc);
               g2d.setColor(Color.red);
               textVariable.draw(g2d, 25.0F, 530.0F);
               textVariable = new TextLayout(this.instructionCode.substring(0, 6), font, frc);
               g2d.setColor(Color.magenta);
               textVariable.draw(g2d, 25.0F, 550.0F);
               textVariable = new TextLayout("rs", fontTitle, frc);
               g2d.setColor(Color.red);
               textVariable.draw(g2d, 90.0F, 530.0F);
               textVariable = new TextLayout(this.instructionCode.substring(6, 11), font, frc);
               g2d.setColor(Color.green);
               textVariable.draw(g2d, 90.0F, 550.0F);
               textVariable = new TextLayout("rt", fontTitle, frc);
               g2d.setColor(Color.red);
               textVariable.draw(g2d, 145.0F, 530.0F);
               textVariable = new TextLayout(this.instructionCode.substring(11, 16), font, frc);
               g2d.setColor(Color.blue);
               textVariable.draw(g2d, 145.0F, 550.0F);
               textVariable = new TextLayout("Immediate", fontTitle, frc);
               g2d.setColor(Color.red);
               textVariable.draw(g2d, 200.0F, 530.0F);
               textVariable = new TextLayout(this.instructionCode.substring(16, 32), font, frc);
               g2d.setColor(Color.cyan);
               textVariable.draw(g2d, 200.0F, 550.0F);
               textVariable = new TextLayout("Instruction", fontTitle, frc);
               g2d.setColor(Color.red);
               textVariable.draw(g2d, 25.0F, 480.0F);
               textVariable = new TextLayout((String)this.opcodeEquivalenceTable.get(this.instructionCode.substring(0, 6)), font, frc);
               g2d.setColor(Color.black);
               textVariable.draw(g2d, 25.0F, 500.0F);
               textVariable = new TextLayout((String)this.registerEquivalenceTable.get(this.instructionCode.substring(6, 11)), font, frc);
               g2d.setColor(Color.black);
               textVariable.draw(g2d, 105.0F, 500.0F);
               textVariable = new TextLayout((String)this.registerEquivalenceTable.get(this.instructionCode.substring(11, 16)), font, frc);
               g2d.setColor(Color.black);
               textVariable.draw(g2d, 65.0F, 500.0F);
               textVariable = new TextLayout(this.parseBinToInt(this.instructionCode.substring(16, 32)), font, frc);
               g2d.setColor(Color.black);
               textVariable.draw(g2d, 155.0F, 500.0F);
            } else {
               textVariable = new TextLayout("IMMEDIATE TYPE INSTRUCTION", new Font("Verdana", 1, 25), frc);
               g2d.setColor(Color.black);
               textVariable.draw(g2d, 250.0F, 30.0F);
               textVariable = new TextLayout("opcode", fontTitle, frc);
               g2d.setColor(Color.red);
               textVariable.draw(g2d, 25.0F, 530.0F);
               textVariable = new TextLayout(this.instructionCode.substring(0, 6), font, frc);
               g2d.setColor(Color.magenta);
               textVariable.draw(g2d, 25.0F, 550.0F);
               textVariable = new TextLayout("rs", fontTitle, frc);
               g2d.setColor(Color.red);
               textVariable.draw(g2d, 90.0F, 530.0F);
               textVariable = new TextLayout(this.instructionCode.substring(6, 11), font, frc);
               g2d.setColor(Color.green);
               textVariable.draw(g2d, 90.0F, 550.0F);
               textVariable = new TextLayout("rt", fontTitle, frc);
               g2d.setColor(Color.red);
               textVariable.draw(g2d, 145.0F, 530.0F);
               textVariable = new TextLayout(this.instructionCode.substring(11, 16), font, frc);
               g2d.setColor(Color.blue);
               textVariable.draw(g2d, 145.0F, 550.0F);
               textVariable = new TextLayout("Immediate", fontTitle, frc);
               g2d.setColor(Color.red);
               textVariable.draw(g2d, 200.0F, 530.0F);
               textVariable = new TextLayout(this.instructionCode.substring(16, 32), font, frc);
               g2d.setColor(Color.cyan);
               textVariable.draw(g2d, 200.0F, 550.0F);
               textVariable = new TextLayout("Instruction", fontTitle, frc);
               g2d.setColor(Color.red);
               textVariable.draw(g2d, 25.0F, 480.0F);
               textVariable = new TextLayout((String)this.opcodeEquivalenceTable.get(this.instructionCode.substring(0, 6)), font, frc);
               g2d.setColor(Color.black);
               textVariable.draw(g2d, 25.0F, 500.0F);
               textVariable = new TextLayout((String)this.registerEquivalenceTable.get(this.instructionCode.substring(6, 11)), font, frc);
               g2d.setColor(Color.black);
               textVariable.draw(g2d, 105.0F, 500.0F);
               textVariable = new TextLayout((String)this.registerEquivalenceTable.get(this.instructionCode.substring(11, 16)), font, frc);
               g2d.setColor(Color.black);
               textVariable.draw(g2d, 65.0F, 500.0F);
               textVariable = new TextLayout(this.parseBinToInt(this.instructionCode.substring(16, 32)), font, frc);
               g2d.setColor(Color.black);
               textVariable.draw(g2d, 155.0F, 500.0F);
            }
         }

         textVariable = new TextLayout("Control Signals", fontTitle, frc);
         g2d.setColor(Color.red);
         textVariable.draw(g2d, 25.0F, 440.0F);
         textVariable = new TextLayout("Active", font, frc);
         g2d.setColor(Color.red);
         textVariable.draw(g2d, 25.0F, 455.0F);
         textVariable = new TextLayout("Inactive", font, frc);
         g2d.setColor(Color.gray);
         textVariable.draw(g2d, 75.0F, 455.0F);
         textVariable = new TextLayout("To see details of control units and register bank click inside the functional block", font, frc);
         g2d.setColor(Color.black);
         textVariable.draw(g2d, 400.0F, 550.0F);
      }

      public void startAnimation(String codeInstruction) {
         this.instructionCode = codeInstruction;
         MipsXray.this.time = new Timer(this.PERIOD, this);
         MipsXray.this.time.start();
      }

      private void initImages() {
         try {
            BufferedImage im = ImageIO.read(this.getClass().getResource("/images/datapath.png"));
            int transparency = im.getColorModel().getTransparency();
            this.datapath = this.gc.createCompatibleImage(im.getWidth(), im.getHeight(), transparency);
            this.g2d = this.datapath.createGraphics();
            this.g2d.drawImage(im, 0, 0, (ImageObserver)null);
            this.g2d.dispose();
         } catch (IOException var3) {
            System.out.println("Load Image error for " + this.getClass().getResource("/images/datapath.png") + ":\n" + var3);
         }

      }

      public void actionPerformed(ActionEvent e) {
         if (this.justStarted) {
            this.justStarted = false;
         }

         if (this.xIsMoving) {
            ++this.indexX;
         }

         if (this.yIsMoving) {
            --this.indexY;
         }

         this.repaint();
      }

      public void paintComponent(Graphics g) {
         super.paintComponent(g);
         this.g2d = (Graphics2D)g;
         this.g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         this.g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
         this.g2d = (Graphics2D)g;
         this.drawImage(this.g2d, this.datapath, 0, 0, (Color)null);
         this.executeAnimation(g);
         this.counter = (this.counter + 1) % 100;
         this.g2d.dispose();
      }

      private void drawImage(Graphics2D g2d, BufferedImage im, int x, int y, Color c) {
         if (im == null) {
            g2d.setColor(c);
            g2d.fillOval(x, y, 20, 20);
            g2d.setColor(Color.black);
            g2d.drawString("   ", x, y);
         } else {
            g2d.drawImage(im, x, y, this);
         }

      }

      public void printTrackLtoR(Vertex v) {
         int size = v.getEnd() - v.getInit();
         int[] track = new int[size];

         int i;
         for(i = 0; i < size; ++i) {
            track[i] = v.getInit() + i;
         }

         if (v.isActive()) {
            v.setFirst_interaction(false);

            for(i = 0; i < size; ++i) {
               if (track[i] <= v.getCurrent()) {
                  this.g2d.setColor(v.getColor());
                  this.g2d.fillRect(track[i], v.getOppositeAxis(), 3, 3);
               }
            }

            if (v.getCurrent() == track[size - 1]) {
               v.setActive(false);
            }

            v.setCurrent(v.getCurrent() + 1);
         } else if (!v.isFirst_interaction()) {
            for(i = 0; i < size; ++i) {
               this.g2d.setColor(v.getColor());
               this.g2d.fillRect(track[i], v.getOppositeAxis(), 3, 3);
            }
         }

      }

      public void printTrackRtoL(Vertex v) {
         int size = v.getInit() - v.getEnd();
         int[] track = new int[size];

         int i;
         for(i = 0; i < size; ++i) {
            track[i] = v.getInit() - i;
         }

         if (v.isActive()) {
            v.setFirst_interaction(false);

            for(i = 0; i < size; ++i) {
               if (track[i] >= v.getCurrent()) {
                  this.g2d.setColor(v.getColor());
                  this.g2d.fillRect(track[i], v.getOppositeAxis(), 3, 3);
               }
            }

            if (v.getCurrent() == track[size - 1]) {
               v.setActive(false);
            }

            v.setCurrent(v.getCurrent() - 1);
         } else if (!v.isFirst_interaction()) {
            for(i = 0; i < size; ++i) {
               this.g2d.setColor(v.getColor());
               this.g2d.fillRect(track[i], v.getOppositeAxis(), 3, 3);
            }
         }

      }

      public void printTrackDtoU(Vertex v) {
         int size;
         int[] track;
         int i;
         if (v.getInit() > v.getEnd()) {
            size = v.getInit() - v.getEnd();
            track = new int[size];

            for(i = 0; i < size; ++i) {
               track[i] = v.getInit() - i;
            }
         } else {
            size = v.getEnd() - v.getInit();
            track = new int[size];

            for(i = 0; i < size; ++i) {
               track[i] = v.getInit() + i;
            }
         }

         if (v.isActive()) {
            v.setFirst_interaction(false);

            for(i = 0; i < size; ++i) {
               if (track[i] >= v.getCurrent()) {
                  this.g2d.setColor(v.getColor());
                  this.g2d.fillRect(v.getOppositeAxis(), track[i], 3, 3);
               }
            }

            if (v.getCurrent() == track[size - 1]) {
               v.setActive(false);
            }

            v.setCurrent(v.getCurrent() - 1);
         } else if (!v.isFirst_interaction()) {
            for(i = 0; i < size; ++i) {
               this.g2d.setColor(v.getColor());
               this.g2d.fillRect(v.getOppositeAxis(), track[i], 3, 3);
            }
         }

      }

      public void printTrackUtoD(Vertex v) {
         int size = v.getEnd() - v.getInit();
         int[] track = new int[size];

         int i;
         for(i = 0; i < size; ++i) {
            track[i] = v.getInit() + i;
         }

         if (v.isActive()) {
            v.setFirst_interaction(false);

            for(i = 0; i < size; ++i) {
               if (track[i] <= v.getCurrent()) {
                  this.g2d.setColor(v.getColor());
                  this.g2d.fillRect(v.getOppositeAxis(), track[i], 3, 3);
               }
            }

            if (v.getCurrent() == track[size - 1]) {
               v.setActive(false);
            }

            v.setCurrent(v.getCurrent() + 1);
         } else if (!v.isFirst_interaction()) {
            for(i = 0; i < size; ++i) {
               this.g2d.setColor(v.getColor());
               this.g2d.fillRect(v.getOppositeAxis(), track[i], 3, 3);
            }
         }

      }

      public void printTextDtoU(Vertex v) {
         FontRenderContext frc = this.g2d.getFontRenderContext();
         TextLayout actionInFunctionalBlock = new TextLayout(v.getName(), new Font("Verdana", 1, 13), frc);
         this.g2d.setColor(Color.RED);
         if (this.instructionCode.substring(0, 6).matches("101[0-1][0-1][0-1]") && !this.instructionCode.substring(0, 6).matches("0001[0-1][0-1]") && !this.instructionCode.substring(0, 6).matches("00001[0-1]")) {
            actionInFunctionalBlock = new TextLayout(" ", new Font("Verdana", 1, 13), frc);
         }

         if (v.getName().equals("ALUVALUE")) {
            if (this.instructionCode.substring(0, 6).equals("000000")) {
               actionInFunctionalBlock = new TextLayout((String)this.functionEquivalenceTable.get(this.instructionCode.substring(26, 32)), new Font("Verdana", 1, 13), frc);
            } else {
               actionInFunctionalBlock = new TextLayout((String)this.opcodeEquivalenceTable.get(this.instructionCode.substring(0, 6)), new Font("Verdana", 1, 13), frc);
            }
         }

         if (this.instructionCode.substring(0, 6).matches("0001[0-1][0-1]") && v.getName().equals("CP+4")) {
            actionInFunctionalBlock = new TextLayout("PC+OFFSET", new Font("Verdana", 1, 13), frc);
         }

         if (v.getName().equals("WRITING") && !this.instructionCode.substring(0, 6).matches("100[0-1][0-1][0-1]")) {
            actionInFunctionalBlock = new TextLayout(" ", new Font("Verdana", 1, 13), frc);
         }

         if (v.isActive()) {
            v.setFirst_interaction(false);
            actionInFunctionalBlock.draw(this.g2d, (float)v.getOppositeAxis(), (float)v.getCurrent());
            if (v.getCurrent() == v.getEnd()) {
               v.setActive(false);
            }

            v.setCurrent(v.getCurrent() - 1);
         }

      }

      public String parseBinToInt(String code) {
         int value = 0;

         for(int i = code.length() - 1; i >= 0; --i) {
            if ("1".equals(code.substring(i, i + 1))) {
               value += (int)Math.pow(2.0, (double)(code.length() - i - 1));
            }
         }

         return Integer.toString(value);
      }

      private void executeAnimation(Graphics g) {
         this.g2d = (Graphics2D)g;
         this.setUpInstructionInfo(this.g2d);

         for(int i = 0; i < this.vertexTraversed.size(); ++i) {
            Vertex vert = (Vertex)this.vertexTraversed.get(i);
            int j;
            Vertex tempVertex;
            int k;
            Boolean hasThisVertex;
            int m;
            if (vert.isMovingXaxis) {
               if (vert.getDirection() == 3) {
                  this.printTrackLtoR(vert);
                  if (!vert.isActive()) {
                     j = vert.getTargetVertex().size();

                     for(k = 0; k < j; ++k) {
                        tempVertex = (Vertex)((Vector)this.outputGraph.get(vert.getNumIndex())).get(k);
                        hasThisVertex = false;

                        for(m = 0; m < this.vertexTraversed.size(); ++m) {
                           if (tempVertex.getNumIndex() == ((Vertex)this.vertexTraversed.get(m)).getNumIndex()) {
                              hasThisVertex = true;
                           }
                        }

                        if (!hasThisVertex) {
                           ((Vertex)((Vector)this.outputGraph.get(vert.getNumIndex())).get(k)).setActive(true);
                           this.vertexTraversed.add(((Vector)this.outputGraph.get(vert.getNumIndex())).get(k));
                        }
                     }
                  }
               } else {
                  this.printTrackRtoL(vert);
                  if (!vert.isActive()) {
                     j = vert.getTargetVertex().size();

                     for(k = 0; k < j; ++k) {
                        tempVertex = (Vertex)((Vector)this.outputGraph.get(vert.getNumIndex())).get(k);
                        hasThisVertex = false;

                        for(m = 0; m < this.vertexTraversed.size(); ++m) {
                           if (tempVertex.getNumIndex() == ((Vertex)this.vertexTraversed.get(m)).getNumIndex()) {
                              hasThisVertex = true;
                           }
                        }

                        if (!hasThisVertex) {
                           ((Vertex)((Vector)this.outputGraph.get(vert.getNumIndex())).get(k)).setActive(true);
                           this.vertexTraversed.add(((Vector)this.outputGraph.get(vert.getNumIndex())).get(k));
                        }
                     }
                  }
               }
            } else if (vert.getDirection() == 2) {
               if (vert.isText) {
                  this.printTextDtoU(vert);
               } else {
                  this.printTrackDtoU(vert);
               }

               if (!vert.isActive()) {
                  j = vert.getTargetVertex().size();

                  for(k = 0; k < j; ++k) {
                     tempVertex = (Vertex)((Vector)this.outputGraph.get(vert.getNumIndex())).get(k);
                     hasThisVertex = false;

                     for(m = 0; m < this.vertexTraversed.size(); ++m) {
                        if (tempVertex.getNumIndex() == ((Vertex)this.vertexTraversed.get(m)).getNumIndex()) {
                           hasThisVertex = true;
                        }
                     }

                     if (!hasThisVertex) {
                        ((Vertex)((Vector)this.outputGraph.get(vert.getNumIndex())).get(k)).setActive(true);
                        this.vertexTraversed.add(((Vector)this.outputGraph.get(vert.getNumIndex())).get(k));
                     }
                  }
               }
            } else {
               this.printTrackUtoD(vert);
               if (!vert.isActive()) {
                  j = vert.getTargetVertex().size();

                  for(k = 0; k < j; ++k) {
                     tempVertex = (Vertex)((Vector)this.outputGraph.get(vert.getNumIndex())).get(k);
                     hasThisVertex = false;

                     for(m = 0; m < this.vertexTraversed.size(); ++m) {
                        if (tempVertex.getNumIndex() == ((Vertex)this.vertexTraversed.get(m)).getNumIndex()) {
                           hasThisVertex = true;
                        }
                     }

                     if (!hasThisVertex) {
                        ((Vertex)((Vector)this.outputGraph.get(vert.getNumIndex())).get(k)).setActive(true);
                        this.vertexTraversed.add(((Vector)this.outputGraph.get(vert.getNumIndex())).get(k));
                     }
                  }
               }
            }
         }

      }

      public void mouseClicked(MouseEvent e) {
         PointerInfo a = MouseInfo.getPointerInfo();
         FunctionUnitVisualization fu;
         if (e.getPoint().getX() > 425.0 && e.getPoint().getX() < 520.0 && e.getPoint().getY() > 300.0 && e.getPoint().getY() < 425.0) {
            MipsXray.this.buildMainDisplayArea("register.png");
            fu = new FunctionUnitVisualization(MipsXray.this.instructionBinary, this.register);
            fu.run();
         }

         if (e.getPoint().getX() > 355.0 && e.getPoint().getX() < 415.0 && e.getPoint().getY() > 180.0 && e.getPoint().getY() < 280.0) {
            MipsXray.this.buildMainDisplayArea("control.png");
            fu = new FunctionUnitVisualization(MipsXray.this.instructionBinary, this.control);
            fu.run();
         }

         if (e.getPoint().getX() > 560.0 && e.getPoint().getX() < 620.0 && e.getPoint().getY() > 450.0 && e.getPoint().getY() < 520.0) {
            MipsXray.this.buildMainDisplayArea("ALUcontrol.png");
            fu = new FunctionUnitVisualization(MipsXray.this.instructionBinary, this.aluControl);
            fu.run();
         }

      }

      public void mouseEntered(MouseEvent e) {
      }

      public void mouseExited(MouseEvent e) {
      }

      public void mouseReleased(MouseEvent e) {
      }
   }

   class Vertex {
      private int numIndex;
      private int init;
      private int end;
      private int current;
      private String name;
      public static final int movingUpside = 1;
      public static final int movingDownside = 2;
      public static final int movingLeft = 3;
      public static final int movingRight = 4;
      public int direction;
      public int oppositeAxis;
      private boolean isMovingXaxis;
      private Color color;
      private boolean first_interaction;
      private boolean active;
      private boolean isText;
      private ArrayList targetVertex;

      public Vertex(int index, int init, int end, String name, int oppositeAxis, boolean isMovingXaxis, String listOfColors, String listTargetVertex, boolean isText) {
         this.numIndex = index;
         this.init = init;
         this.current = this.init;
         this.end = end;
         this.name = name;
         this.oppositeAxis = oppositeAxis;
         this.isMovingXaxis = isMovingXaxis;
         this.first_interaction = true;
         this.active = false;
         this.isText = isText;
         this.color = new Color(0, 153, 0);
         if (isMovingXaxis) {
            if (init < end) {
               this.direction = 3;
            } else {
               this.direction = 4;
            }
         } else if (init < end) {
            this.direction = 1;
         } else {
            this.direction = 2;
         }

         String[] list = listTargetVertex.split("#");
         this.targetVertex = new ArrayList();

         for(int i = 0; i < list.length; ++i) {
            this.targetVertex.add(Integer.parseInt(list[i]));
         }

         String[] listColor = listOfColors.split("#");
         this.color = new Color(Integer.parseInt(listColor[0]), Integer.parseInt(listColor[1]), Integer.parseInt(listColor[2]));
      }

      public int getDirection() {
         return this.direction;
      }

      public boolean isText() {
         return this.isText;
      }

      public ArrayList getTargetVertex() {
         return this.targetVertex;
      }

      public int getNumIndex() {
         return this.numIndex;
      }

      public void setNumIndex(int numIndex) {
         this.numIndex = numIndex;
      }

      public int getInit() {
         return this.init;
      }

      public void setInit(int init) {
         this.init = init;
      }

      public int getEnd() {
         return this.end;
      }

      public void setEnd(int end) {
         this.end = end;
      }

      public int getCurrent() {
         return this.current;
      }

      public void setCurrent(int current) {
         this.current = current;
      }

      public String getName() {
         return this.name;
      }

      public void setName(String name) {
         this.name = name;
      }

      public int getOppositeAxis() {
         return this.oppositeAxis;
      }

      public void setOppositeAxis(int oppositeAxis) {
         this.oppositeAxis = oppositeAxis;
      }

      public boolean isMovingXaxis() {
         return this.isMovingXaxis;
      }

      public void setMovingXaxis(boolean isMovingXaxis) {
         this.isMovingXaxis = isMovingXaxis;
      }

      public Color getColor() {
         return this.color;
      }

      public void setColor(Color color) {
         this.color = color;
      }

      public boolean isFirst_interaction() {
         return this.first_interaction;
      }

      public void setFirst_interaction(boolean first_interaction) {
         this.first_interaction = first_interaction;
      }

      public boolean isActive() {
         return this.active;
      }

      public void setActive(boolean active) {
         this.active = active;
      }
   }
}
