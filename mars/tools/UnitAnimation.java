package mars.tools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class UnitAnimation extends JPanel implements ActionListener {
   private static final long serialVersionUID = -2681757800180958534L;
   private int PERIOD = 8;
   private static final int PWIDTH = 1000;
   private static final int PHEIGHT = 574;
   private GraphicsConfiguration gc;
   private GraphicsDevice gd;
   private int accelMemory;
   private DecimalFormat df;
   private int counter;
   private boolean justStarted;
   private int indexX;
   private int indexY;
   private boolean xIsMoving;
   private boolean yIsMoving;
   private Vector outputGraph;
   private ArrayList vertexList;
   private ArrayList vertexTraversed;
   private HashMap registerEquivalenceTable;
   private String instructionCode;
   private int countRegLabel;
   private int countALULabel;
   private int countPCLabel;
   private int register = 1;
   private int control = 2;
   private int aluControl = 3;
   private int alu = 4;
   private int datapatTypeUsed;
   private Boolean cursorInIM;
   private Boolean cursorInALU;
   private Boolean cursorInDataMem;
   private Boolean cursorInReg;
   private Graphics2D g2d;
   private BufferedImage datapath;

   public UnitAnimation(String instructionBinary, int datapathType) {
      this.datapatTypeUsed = datapathType;
      this.cursorInIM = false;
      this.cursorInALU = false;
      this.cursorInDataMem = false;
      this.df = new DecimalFormat("0.0");
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
      this.registerEquivalenceTable = new HashMap();
      this.countRegLabel = 400;
      this.countALULabel = 380;
      this.countPCLabel = 380;
      this.loadHashMapValues();
   }

   public void loadHashMapValues() {
      if (this.datapatTypeUsed == this.register) {
         this.importXmlStringData("/registerDatapath.xml", this.registerEquivalenceTable, "register_equivalence", "bits", "mnemonic");
         this.importXmlDatapathMap("/registerDatapath.xml", "datapath_map");
      } else if (this.datapatTypeUsed == this.control) {
         this.importXmlStringData("/controlDatapath.xml", this.registerEquivalenceTable, "register_equivalence", "bits", "mnemonic");
         this.importXmlDatapathMap("/controlDatapath.xml", "datapath_map");
      } else if (this.datapatTypeUsed == this.aluControl) {
         this.importXmlStringData("/ALUcontrolDatapath.xml", this.registerEquivalenceTable, "register_equivalence", "bits", "mnemonic");
         this.importXmlDatapathMapAluControl("/ALUcontrolDatapath.xml", "datapath_map");
      }

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
               Vertex vert = new Vertex(Integer.parseInt(index_vertex.item(j).getTextContent()), Integer.parseInt(init.item(j).getTextContent()), Integer.parseInt(end.item(j).getTextContent()), name.item(j).getTextContent(), Integer.parseInt(other_axis.item(j).getTextContent()), Boolean.parseBoolean(isMovingXaxis.item(j).getTextContent()), color.item(j).getTextContent(), targetVertex.item(j).getTextContent(), Boolean.parseBoolean(isText.item(j).getTextContent()));
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

   public void importXmlDatapathMapAluControl(String xmlName, String elementTree) {
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
               if (this.instructionCode.substring(28, 32).matches("0000")) {
                  color = datapath_mapItem.getElementsByTagName("ALU_out010");
                  System.out.println("ALU_out010 type " + this.instructionCode.substring(28, 32));
               } else if (this.instructionCode.substring(28, 32).matches("0010")) {
                  color = datapath_mapItem.getElementsByTagName("ALU_out110");
                  System.out.println("ALU_out110 type " + this.instructionCode.substring(28, 32));
               } else if (this.instructionCode.substring(28, 32).matches("0100")) {
                  color = datapath_mapItem.getElementsByTagName("ALU_out000");
                  System.out.println("ALU_out000 type " + this.instructionCode.substring(28, 32));
               } else if (this.instructionCode.substring(28, 32).matches("0101")) {
                  color = datapath_mapItem.getElementsByTagName("ALU_out001");
                  System.out.println("ALU_out001 type " + this.instructionCode.substring(28, 32));
               } else {
                  color = datapath_mapItem.getElementsByTagName("ALU_out111");
                  System.out.println("ALU_out111 type " + this.instructionCode.substring(28, 32));
               }
            } else if (this.instructionCode.substring(0, 6).matches("00001[0-1]")) {
               color = datapath_mapItem.getElementsByTagName("color_Jtype");
               System.out.println("jtype");
            } else if (this.instructionCode.substring(0, 6).matches("100[0-1][0-1][0-1]")) {
               color = datapath_mapItem.getElementsByTagName("color_LOADtype");
               System.out.println("load type");
            } else if (this.instructionCode.substring(0, 6).matches("101[0-1][0-1][0-1]")) {
               color = datapath_mapItem.getElementsByTagName("color_STOREtype");
               System.out.println("store type");
            } else if (this.instructionCode.substring(0, 6).matches("0001[0-1][0-1]")) {
               color = datapath_mapItem.getElementsByTagName("color_BRANCHtype");
               System.out.println("branch type");
            } else {
               color = datapath_mapItem.getElementsByTagName("color_Itype");
               System.out.println("immediate type");
            }

            NodeList other_axis = datapath_mapItem.getElementsByTagName("other_axis");
            NodeList isMovingXaxis = datapath_mapItem.getElementsByTagName("isMovingXaxis");
            NodeList targetVertex = datapath_mapItem.getElementsByTagName("target_vertex");
            NodeList isText = datapath_mapItem.getElementsByTagName("is_text");

            for(int j = 0; j < index_vertex.getLength(); ++j) {
               Vertex vert = new Vertex(Integer.parseInt(index_vertex.item(j).getTextContent()), Integer.parseInt(init.item(j).getTextContent()), Integer.parseInt(end.item(j).getTextContent()), name.item(j).getTextContent(), Integer.parseInt(other_axis.item(j).getTextContent()), Boolean.parseBoolean(isMovingXaxis.item(j).getTextContent()), color.item(j).getTextContent(), targetVertex.item(j).getTextContent(), Boolean.parseBoolean(isText.item(j).getTextContent()));
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

   public void startAnimation(String codeInstruction) {
      this.instructionCode = codeInstruction;
      (new Timer(this.PERIOD, this)).start();
      this.repaint();
   }

   private void initImages() {
      try {
         BufferedImage im;
         if (this.datapatTypeUsed == this.register) {
            im = ImageIO.read(this.getClass().getResource("/images/register.png"));
         } else if (this.datapatTypeUsed == this.control) {
            im = ImageIO.read(this.getClass().getResource("/images/control.png"));
         } else if (this.datapatTypeUsed == this.aluControl) {
            im = ImageIO.read(this.getClass().getResource("/images/ALUcontrol.png"));
         } else {
            im = ImageIO.read(this.getClass().getResource("/images/alu.png"));
         }

         int transparency = im.getColorModel().getTransparency();
         this.datapath = this.gc.createCompatibleImage(im.getWidth(), im.getHeight(), transparency);
         this.g2d = this.datapath.createGraphics();
         this.g2d.drawImage(im, 0, 0, (ImageObserver)null);
         this.g2d.dispose();
      } catch (IOException var3) {
         System.out.println("Load Image error for " + this.getClass().getResource("/images/register.png") + ":\n" + var3);
      }

   }

   public void updateDisplay() {
      this.repaint();
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
            if (!vert.isText) {
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
