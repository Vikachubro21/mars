package mars.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Observable;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import mars.Globals;
import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.MemoryAccessNotice;
import mars.util.Binary;

public class CacheSimulator extends AbstractMarsToolAndApplication {
   private static boolean debug = false;
   private static String version = "Version 1.2";
   private static String heading = "Simulate and illustrate data cache performance";
   private JComboBox cacheBlockSizeSelector;
   private JComboBox cacheBlockCountSelector;
   private JComboBox cachePlacementSelector;
   private JComboBox cacheReplacementSelector;
   private JComboBox cacheSetSizeSelector;
   private JTextField memoryAccessCountDisplay;
   private JTextField cacheHitCountDisplay;
   private JTextField cacheMissCountDisplay;
   private JTextField replacementPolicyDisplay;
   private JTextField cachableAddressesDisplay;
   private JTextField cacheSizeDisplay;
   private JProgressBar cacheHitRateDisplay;
   private Animation animations;
   private JPanel logPanel;
   private JScrollPane logScroll;
   private JTextArea logText;
   private JCheckBox logShow;
   private EmptyBorder emptyBorder = new EmptyBorder(4, 4, 4, 4);
   private Font countFonts = new Font("Times", 1, 12);
   private Color backgroundColor;
   private int[] cacheBlockSizeChoicesInt;
   private int[] cacheBlockCountChoicesInt;
   private String[] cacheBlockSizeChoices;
   private String[] cacheBlockCountChoices;
   private String[] placementPolicyChoices;
   private final int DIRECT;
   private final int FULL;
   private final int SET;
   private String[] replacementPolicyChoices;
   private final int LRU;
   private final int RANDOM;
   private String[] cacheSetSizeChoices;
   private int defaultCacheBlockSizeIndex;
   private int defaultCacheBlockCountIndex;
   private int defaultPlacementPolicyIndex;
   private int defaultReplacementPolicyIndex;
   private int defaultCacheSetSizeIndex;
   private AbstractCache theCache;
   private int memoryAccessCount;
   private int cacheHitCount;
   private int cacheMissCount;
   private double cacheHitRate;
   private Random randu;
   private Color color = (Globals.getSettings().getBooleanSetting(21))? new Color(0x1e1f22): Color.white;

   public CacheSimulator(String title, String heading) {
      super(title, heading);
      this.cacheBlockSizeChoices = new String[]{"1", "2", "4", "8", "16", "32", "64", "128", "256", "512", "1024", "2048"};
      this.cacheBlockCountChoices = new String[]{"1", "2", "4", "8", "16", "32", "64", "128", "256", "512", "1024", "2048"};
      this.placementPolicyChoices = new String[]{"Direct Mapping", "Fully Associative", "N-way Set Associative"};
      this.DIRECT = 0;
      this.FULL = 1;
      this.SET = 2;
      this.replacementPolicyChoices = new String[]{"LRU", "Random"};
      this.LRU = 0;
      this.RANDOM = 1;
      this.defaultCacheBlockSizeIndex = 2;
      this.defaultCacheBlockCountIndex = 3;
      this.defaultPlacementPolicyIndex = 0;
      this.defaultReplacementPolicyIndex = 0;
      this.defaultCacheSetSizeIndex = 0;
      this.randu = new Random(0L);
   }

   public CacheSimulator() {
      super("Data Cache Simulation Tool, " + version, heading);
      this.cacheBlockSizeChoices = new String[]{"1", "2", "4", "8", "16", "32", "64", "128", "256", "512", "1024", "2048"};
      this.cacheBlockCountChoices = new String[]{"1", "2", "4", "8", "16", "32", "64", "128", "256", "512", "1024", "2048"};
      this.placementPolicyChoices = new String[]{"Direct Mapping", "Fully Associative", "N-way Set Associative"};
      this.DIRECT = 0;
      this.FULL = 1;
      this.SET = 2;
      this.replacementPolicyChoices = new String[]{"LRU", "Random"};
      this.LRU = 0;
      this.RANDOM = 1;
      this.defaultCacheBlockSizeIndex = 2;
      this.defaultCacheBlockCountIndex = 3;
      this.defaultPlacementPolicyIndex = 0;
      this.defaultReplacementPolicyIndex = 0;
      this.defaultCacheSetSizeIndex = 0;
      this.randu = new Random(0L);
   }

   public static void main(String[] args) {
      (new CacheSimulator("Data Cache Simulator stand-alone, " + version, heading)).go();
   }

   public String getName() {
      return "Data Cache Simulator";
   }

   protected JComponent buildMainDisplayArea() {
      Box results = Box.createVerticalBox();
      results.add(this.buildOrganizationArea());
      results.add(this.buildPerformanceArea());
      results.add(this.buildLogArea());
      return results;
   }

   private JComponent buildLogArea() {
      this.logPanel = new JPanel();
      TitledBorder ltb = new TitledBorder("Runtime Log");
      ltb.setTitleJustification(2);
      this.logPanel.setBorder(ltb);
      this.logShow = new JCheckBox("Enabled", debug);
      this.logShow.addItemListener(new ItemListener() {
         public void itemStateChanged(ItemEvent e) {
            CacheSimulator.debug = e.getStateChange() == 1;
            CacheSimulator.this.resetLogDisplay();
            CacheSimulator.this.logText.setEnabled(CacheSimulator.debug);
            CacheSimulator.this.logText.setBackground(CacheSimulator.debug ? color : CacheSimulator.this.logPanel.getBackground());
         }
      });
      this.logPanel.add(this.logShow);
      this.logText = new JTextArea(5, 70);
      this.logText.setEnabled(debug);
      this.logText.setBackground(debug ? color : this.logPanel.getBackground());
      this.logText.setFont(new Font("Monospaced", 0, 12));
      this.logText.setToolTipText("Displays cache activity log if enabled");
      this.logScroll = new JScrollPane(this.logText, 20, 30);
      this.logPanel.add(this.logScroll);
      return this.logPanel;
   }

   private JComponent buildOrganizationArea() {
      JPanel organization = new JPanel(new GridLayout(3, 2));
      TitledBorder otb = new TitledBorder("Cache Organization");
      otb.setTitleJustification(2);
      organization.setBorder(otb);
      this.cachePlacementSelector = new JComboBox(this.placementPolicyChoices);
      this.cachePlacementSelector.setEditable(false);
      this.cachePlacementSelector.setBackground(this.backgroundColor);
      this.cachePlacementSelector.setSelectedIndex(this.defaultPlacementPolicyIndex);
      this.cachePlacementSelector.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            CacheSimulator.this.updateCacheSetSizeSelector();
            CacheSimulator.this.reset();
         }
      });
      this.cacheReplacementSelector = new JComboBox(this.replacementPolicyChoices);
      this.cacheReplacementSelector.setEditable(false);
      this.cacheReplacementSelector.setBackground(this.backgroundColor);
      this.cacheReplacementSelector.setSelectedIndex(this.defaultReplacementPolicyIndex);
      this.cacheBlockSizeSelector = new JComboBox(this.cacheBlockSizeChoices);
      this.cacheBlockSizeSelector.setEditable(false);
      this.cacheBlockSizeSelector.setBackground(this.backgroundColor);
      this.cacheBlockSizeSelector.setSelectedIndex(this.defaultCacheBlockSizeIndex);
      this.cacheBlockSizeSelector.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            CacheSimulator.this.updateCacheSizeDisplay();
            CacheSimulator.this.reset();
         }
      });
      this.cacheBlockCountSelector = new JComboBox(this.cacheBlockCountChoices);
      this.cacheBlockCountSelector.setEditable(false);
      this.cacheBlockCountSelector.setBackground(this.backgroundColor);
      this.cacheBlockCountSelector.setSelectedIndex(this.defaultCacheBlockCountIndex);
      this.cacheBlockCountSelector.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            CacheSimulator.this.updateCacheSetSizeSelector();
            CacheSimulator.this.theCache = CacheSimulator.this.createNewCache();
            CacheSimulator.this.resetCounts();
            CacheSimulator.this.updateDisplay();
            CacheSimulator.this.updateCacheSizeDisplay();
            CacheSimulator.this.animations.fillAnimationBoxWithCacheBlocks();
         }
      });
      this.cacheSetSizeSelector = new JComboBox(this.cacheSetSizeChoices);
      this.cacheSetSizeSelector.setEditable(false);
      this.cacheSetSizeSelector.setBackground(this.backgroundColor);
      this.cacheSetSizeSelector.setSelectedIndex(this.defaultCacheSetSizeIndex);
      this.cacheSetSizeSelector.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            CacheSimulator.this.reset();
         }
      });
      JPanel placementPolicyRow = this.getPanelWithBorderLayout();
      placementPolicyRow.setBorder(this.emptyBorder);
      placementPolicyRow.add(new JLabel("Placement Policy "), "West");
      placementPolicyRow.add(this.cachePlacementSelector, "East");
      JPanel replacementPolicyRow = this.getPanelWithBorderLayout();
      replacementPolicyRow.setBorder(this.emptyBorder);
      replacementPolicyRow.add(new JLabel("Block Replacement Policy "), "West");
      replacementPolicyRow.add(this.cacheReplacementSelector, "East");
      JPanel cacheSetSizeRow = this.getPanelWithBorderLayout();
      cacheSetSizeRow.setBorder(this.emptyBorder);
      cacheSetSizeRow.add(new JLabel("Set size (blocks) "), "West");
      cacheSetSizeRow.add(this.cacheSetSizeSelector, "East");
      JPanel cacheNumberBlocksRow = this.getPanelWithBorderLayout();
      cacheNumberBlocksRow.setBorder(this.emptyBorder);
      cacheNumberBlocksRow.add(new JLabel("Number of blocks "), "West");
      cacheNumberBlocksRow.add(this.cacheBlockCountSelector, "East");
      JPanel cacheBlockSizeRow = this.getPanelWithBorderLayout();
      cacheBlockSizeRow.setBorder(this.emptyBorder);
      cacheBlockSizeRow.add(new JLabel("Cache block size (words) "), "West");
      cacheBlockSizeRow.add(this.cacheBlockSizeSelector, "East");
      JPanel cacheTotalSizeRow = this.getPanelWithBorderLayout();
      cacheTotalSizeRow.setBorder(this.emptyBorder);
      cacheTotalSizeRow.add(new JLabel("Cache size (bytes) "), "West");
      this.cacheSizeDisplay = new JTextField(8);
      this.cacheSizeDisplay.setHorizontalAlignment(4);
      this.cacheSizeDisplay.setEditable(false);
      this.cacheSizeDisplay.setBackground(this.backgroundColor);
      this.cacheSizeDisplay.setFont(this.countFonts);
      cacheTotalSizeRow.add(this.cacheSizeDisplay, "East");
      this.updateCacheSizeDisplay();
      organization.add(placementPolicyRow);
      organization.add(cacheNumberBlocksRow);
      organization.add(replacementPolicyRow);
      organization.add(cacheBlockSizeRow);
      organization.add(cacheSetSizeRow);
      organization.add(cacheTotalSizeRow);
      return organization;
   }

   private JComponent buildPerformanceArea() {
      JPanel performance = new JPanel(new GridLayout(1, 2));
      TitledBorder ptb = new TitledBorder("Cache Performance");
      ptb.setTitleJustification(2);
      performance.setBorder(ptb);
      JPanel memoryAccessCountRow = this.getPanelWithBorderLayout();
      memoryAccessCountRow.setBorder(this.emptyBorder);
      memoryAccessCountRow.add(new JLabel("Memory Access Count "), "West");
      this.memoryAccessCountDisplay = new JTextField(10);
      this.memoryAccessCountDisplay.setHorizontalAlignment(4);
      this.memoryAccessCountDisplay.setEditable(false);
      this.memoryAccessCountDisplay.setBackground(this.backgroundColor);
      this.memoryAccessCountDisplay.setFont(this.countFonts);
      memoryAccessCountRow.add(this.memoryAccessCountDisplay, "East");
      JPanel cacheHitCountRow = this.getPanelWithBorderLayout();
      cacheHitCountRow.setBorder(this.emptyBorder);
      cacheHitCountRow.add(new JLabel("Cache Hit Count "), "West");
      this.cacheHitCountDisplay = new JTextField(10);
      this.cacheHitCountDisplay.setHorizontalAlignment(4);
      this.cacheHitCountDisplay.setEditable(false);
      this.cacheHitCountDisplay.setBackground(this.backgroundColor);
      this.cacheHitCountDisplay.setFont(this.countFonts);
      cacheHitCountRow.add(this.cacheHitCountDisplay, "East");
      JPanel cacheMissCountRow = this.getPanelWithBorderLayout();
      cacheMissCountRow.setBorder(this.emptyBorder);
      cacheMissCountRow.add(new JLabel("Cache Miss Count "), "West");
      this.cacheMissCountDisplay = new JTextField(10);
      this.cacheMissCountDisplay.setHorizontalAlignment(4);
      this.cacheMissCountDisplay.setEditable(false);
      this.cacheMissCountDisplay.setBackground(this.backgroundColor);
      this.cacheMissCountDisplay.setFont(this.countFonts);
      cacheMissCountRow.add(this.cacheMissCountDisplay, "East");
      JPanel cacheHitRateRow = this.getPanelWithBorderLayout();
      cacheHitRateRow.setBorder(this.emptyBorder);
      cacheHitRateRow.add(new JLabel("Cache Hit Rate "), "West");
      this.cacheHitRateDisplay = new JProgressBar(0, 0, 100);
      this.cacheHitRateDisplay.setStringPainted(true);
      this.cacheHitRateDisplay.setForeground(Color.BLUE);
      this.cacheHitRateDisplay.setBackground(this.backgroundColor);
      this.cacheHitRateDisplay.setFont(this.countFonts);
      cacheHitRateRow.add(this.cacheHitRateDisplay, "East");
      this.resetCounts();
      this.updateDisplay();
      JPanel performanceMeasures = new JPanel(new GridLayout(4, 1));
      performanceMeasures.add(memoryAccessCountRow);
      performanceMeasures.add(cacheHitCountRow);
      performanceMeasures.add(cacheMissCountRow);
      performanceMeasures.add(cacheHitRateRow);
      performance.add(performanceMeasures);
      this.animations = new Animation();
      this.animations.fillAnimationBoxWithCacheBlocks();
      JPanel animationsPanel = new JPanel(new GridLayout(1, 2));
      Box animationsLabel = Box.createVerticalBox();
      JPanel tableTitle1 = new JPanel(new FlowLayout(0));
      JPanel tableTitle2 = new JPanel(new FlowLayout(0));
      tableTitle1.add(new JLabel("Cache Block Table"));
      tableTitle2.add(new JLabel("(block 0 at top)"));
      animationsLabel.add(tableTitle1);
      animationsLabel.add(tableTitle2);
      Dimension colorKeyBoxSize = new Dimension(8, 8);
      JPanel emptyKey = new JPanel(new FlowLayout(0));
      JPanel emptyBox = new JPanel();
      emptyBox.setSize(colorKeyBoxSize);
      emptyBox.setBackground(this.animations.defaultColor);
      emptyBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
      emptyKey.add(emptyBox);
      emptyKey.add(new JLabel(" = empty"));
      JPanel missBox = new JPanel();
      JPanel missKey = new JPanel(new FlowLayout(0));
      missBox.setSize(colorKeyBoxSize);
      missBox.setBackground(this.animations.missColor);
      missBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
      missKey.add(missBox);
      missKey.add(new JLabel(" = miss"));
      JPanel hitKey = new JPanel(new FlowLayout(0));
      JPanel hitBox = new JPanel();
      hitBox.setSize(colorKeyBoxSize);
      hitBox.setBackground(this.animations.hitColor);
      hitBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
      hitKey.add(hitBox);
      hitKey.add(new JLabel(" = hit"));
      animationsLabel.add(emptyKey);
      animationsLabel.add(hitKey);
      animationsLabel.add(missKey);
      animationsLabel.add(Box.createVerticalGlue());
      animationsPanel.add(animationsLabel);
      animationsPanel.add(this.animations.getAnimationBox());
      performance.add(animationsPanel);
      return performance;
   }

   protected void processMIPSUpdate(Observable memory, AccessNotice accessNotice) {
      MemoryAccessNotice notice = (MemoryAccessNotice)accessNotice;
      ++this.memoryAccessCount;
      CacheAccessResult cacheAccessResult = this.theCache.isItAHitThenReadOnMiss(notice.getAddress());
      if (cacheAccessResult.isHit()) {
         ++this.cacheHitCount;
         this.animations.showHit(cacheAccessResult.getBlock());
      } else {
         ++this.cacheMissCount;
         this.animations.showMiss(cacheAccessResult.getBlock());
      }

      this.cacheHitRate = (double)this.cacheHitCount / (double)this.memoryAccessCount;
   }

   protected void initializePreGUI() {
      this.cacheBlockSizeChoicesInt = new int[this.cacheBlockSizeChoices.length];

      int i;
      for(i = 0; i < this.cacheBlockSizeChoices.length; ++i) {
         try {
            this.cacheBlockSizeChoicesInt[i] = Integer.parseInt(this.cacheBlockSizeChoices[i]);
         } catch (NumberFormatException var4) {
            this.cacheBlockSizeChoicesInt[i] = 1;
         }
      }

      this.cacheBlockCountChoicesInt = new int[this.cacheBlockCountChoices.length];

      for(i = 0; i < this.cacheBlockCountChoices.length; ++i) {
         try {
            this.cacheBlockCountChoicesInt[i] = Integer.parseInt(this.cacheBlockCountChoices[i]);
         } catch (NumberFormatException var3) {
            this.cacheBlockCountChoicesInt[i] = 1;
         }
      }

      this.cacheSetSizeChoices = this.determineSetSizeChoices(this.defaultCacheBlockCountIndex, this.defaultPlacementPolicyIndex);
   }

   protected void initializePostGUI() {
      this.theCache = this.createNewCache();
   }

   protected void reset() {
      this.theCache = this.createNewCache();
      this.resetCounts();
      this.updateDisplay();
      this.animations.reset();
      this.resetLogDisplay();
   }

   protected void updateDisplay() {
      this.updateMemoryAccessCountDisplay();
      this.updateCacheHitCountDisplay();
      this.updateCacheMissCountDisplay();
      this.updateCacheHitRateDisplay();
   }

   private String[] determineSetSizeChoices(int cacheBlockCountIndex, int placementPolicyIndex) {
      int firstBlockCountIndex = 0;
      String[] choices;
      switch (placementPolicyIndex) {
         case 0:
            choices = new String[]{this.cacheBlockCountChoices[firstBlockCountIndex]};
            break;
         case 1:
         default:
            choices = new String[]{this.cacheBlockCountChoices[cacheBlockCountIndex]};
            break;
         case 2:
            choices = new String[cacheBlockCountIndex - firstBlockCountIndex + 1];

            for(int i = 0; i < choices.length; ++i) {
               choices[i] = this.cacheBlockCountChoices[firstBlockCountIndex + i];
            }
      }

      return choices;
   }

   private void updateCacheSetSizeSelector() {
      this.cacheSetSizeSelector.setModel(new DefaultComboBoxModel(this.determineSetSizeChoices(this.cacheBlockCountSelector.getSelectedIndex(), this.cachePlacementSelector.getSelectedIndex())));
   }

   private AbstractCache createNewCache() {
      AbstractCache theNewCache = null;
      int setSize = 1;

      try {
         setSize = Integer.parseInt((String)this.cacheSetSizeSelector.getSelectedItem());
      } catch (NumberFormatException var4) {
      }

      theNewCache = new AnyCache(this.cacheBlockCountChoicesInt[this.cacheBlockCountSelector.getSelectedIndex()], this.cacheBlockSizeChoicesInt[this.cacheBlockSizeSelector.getSelectedIndex()], setSize);
      return theNewCache;
   }

   private void resetCounts() {
      this.memoryAccessCount = 0;
      this.cacheHitCount = 0;
      this.cacheMissCount = 0;
      this.cacheHitRate = 0.0;
   }

   private void updateMemoryAccessCountDisplay() {
      this.memoryAccessCountDisplay.setText((new Integer(this.memoryAccessCount)).toString());
   }

   private void updateCacheHitCountDisplay() {
      this.cacheHitCountDisplay.setText((new Integer(this.cacheHitCount)).toString());
   }

   private void updateCacheMissCountDisplay() {
      this.cacheMissCountDisplay.setText((new Integer(this.cacheMissCount)).toString());
   }

   private void updateCacheHitRateDisplay() {
      this.cacheHitRateDisplay.setValue((int)Math.round(this.cacheHitRate * 100.0));
   }

   private void updateCacheSizeDisplay() {
      int cacheSize = this.cacheBlockSizeChoicesInt[this.cacheBlockSizeSelector.getSelectedIndex()] * this.cacheBlockCountChoicesInt[this.cacheBlockCountSelector.getSelectedIndex()] * 4;
      this.cacheSizeDisplay.setText(Integer.toString(cacheSize));
   }

   private JPanel getPanelWithBorderLayout() {
      return new JPanel(new BorderLayout(2, 2));
   }

   private void resetLogDisplay() {
      this.logText.setText("");
   }

   private void writeLog(String text) {
      this.logText.append(text);
      this.logText.setCaretPosition(this.logText.getDocument().getLength());
   }

   private class Animation {
      private Box animation;
      private JTextField[] blocks;
      public final Color hitColor;
      public final Color missColor;
      public final Color defaultColor;

      public Animation() {
         this.hitColor = Color.GREEN;
         this.missColor = Color.RED;
         this.defaultColor = color;
         this.animation = Box.createVerticalBox();
      }

      private Box getAnimationBox() {
         return this.animation;
      }

      public int getNumberOfBlocks() {
         return this.blocks == null ? 0 : this.blocks.length;
      }

      public void showHit(int blockNum) {
         this.blocks[blockNum].setBackground(this.hitColor);
      }

      public void showMiss(int blockNum) {
         this.blocks[blockNum].setBackground(this.missColor);
      }

      public void reset() {
         for(int i = 0; i < this.blocks.length; ++i) {
            this.blocks[i].setBackground(this.defaultColor);
         }

      }

      private void fillAnimationBoxWithCacheBlocks() {
         this.animation.setVisible(false);
         this.animation.removeAll();
         int numberOfBlocks = CacheSimulator.this.cacheBlockCountChoicesInt[CacheSimulator.this.cacheBlockCountSelector.getSelectedIndex()];
         int totalVerticalPixels = 128;
         int blockPixelHeight = numberOfBlocks > totalVerticalPixels ? 1 : totalVerticalPixels / numberOfBlocks;
         int blockPixelWidth = 40;
         Dimension blockDimension = new Dimension(blockPixelWidth, blockPixelHeight);
         this.blocks = new JTextField[numberOfBlocks];

         for(int i = 0; i < numberOfBlocks; ++i) {
            this.blocks[i] = new JTextField();
            this.blocks[i].setEditable(false);
            this.blocks[i].setBackground(this.defaultColor);
            this.blocks[i].setSize(blockDimension);
            this.blocks[i].setPreferredSize(blockDimension);
            this.animation.add(this.blocks[i]);
         }

         this.animation.repaint();
         this.animation.setVisible(true);
      }
   }

   private class AnyCache extends AbstractCache {
      private final int SET_FULL = 0;
      private final int HIT = 1;
      private final int MISS = 2;

      public AnyCache(int numberOfBlocks, int blockSizeInWords, int setSizeInBlocks) {
         super(numberOfBlocks, blockSizeInWords, setSizeInBlocks);
      }

      public CacheAccessResult isItAHitThenReadOnMiss(int address) {
         boolean result = false;
         int firstBlock = this.getFirstBlockToSearch(address);
         int lastBlock = this.getLastBlockToSearch(address);
         if (CacheSimulator.debug) {
            CacheSimulator.this.writeLog("(" + CacheSimulator.this.memoryAccessCount + ") address: " + Binary.intToHexString(address) + " (tag " + Binary.intToHexString(this.getTag(address)) + ") " + " block range: " + firstBlock + "-" + lastBlock + "\n");
         }


         CacheBlock block;
         int blockNumberx;
         for(blockNumberx = firstBlock; blockNumberx <= lastBlock; ++blockNumberx) {
            block = this.blocks[blockNumberx];
            if (CacheSimulator.debug) {
               CacheSimulator.this.writeLog("   trying block " + blockNumberx + (block.valid ? " tag " + Binary.intToHexString(block.tag) : " empty"));
            }

            if (block.valid && block.tag == this.getTag(address)) {
               if (CacheSimulator.debug) {
                  CacheSimulator.this.writeLog(" -- HIT\n");
               }

               result = true;
               block.mostRecentAccessTime = CacheSimulator.this.memoryAccessCount;
               break;
            }

            if (!block.valid) {
               if (CacheSimulator.debug) {
                  CacheSimulator.this.writeLog(" -- MISS\n");
               }

               result = true;
               block.valid = true;
               block.tag = this.getTag(address);
               block.mostRecentAccessTime = CacheSimulator.this.memoryAccessCount;
               break;
            }

            if (CacheSimulator.debug) {
               CacheSimulator.this.writeLog(" -- OCCUPIED\n");
            }
         }

         if (!result) {
            if (CacheSimulator.debug) {
               CacheSimulator.this.writeLog("   MISS due to FULL SET");
            }

            int blockToReplace = this.selectBlockToReplace(firstBlock, lastBlock);
            block = this.blocks[blockToReplace];
            block.tag = this.getTag(address);
            block.mostRecentAccessTime = CacheSimulator.this.memoryAccessCount;
            blockNumberx = blockToReplace;
         }

         return CacheSimulator.this.new CacheAccessResult(result, blockNumberx);
      }

      private int selectBlockToReplace(int first, int last) {
         int replaceBlock = first;
         if (first != last) {
            switch (CacheSimulator.this.cacheReplacementSelector.getSelectedIndex()) {
               case 0:
               default:
                  int leastRecentAccessTime = CacheSimulator.this.memoryAccessCount;

                  for(int block = first; block <= last; ++block) {
                     if (this.blocks[block].mostRecentAccessTime < leastRecentAccessTime) {
                        leastRecentAccessTime = this.blocks[block].mostRecentAccessTime;
                        replaceBlock = block;
                     }
                  }

                  if (CacheSimulator.debug) {
                     CacheSimulator.this.writeLog(" -- LRU replace block " + replaceBlock + "; unused since (" + leastRecentAccessTime + ")\n");
                  }
                  break;
               case 1:
                  replaceBlock = first + CacheSimulator.this.randu.nextInt(last - first + 1);
                  if (CacheSimulator.debug) {
                     CacheSimulator.this.writeLog(" -- Random replace block " + replaceBlock + "\n");
                  }
            }
         }

         return replaceBlock;
      }
   }

   private abstract class AbstractCache {
      private int numberOfBlocks;
      private int blockSizeInWords;
      private int setSizeInBlocks;
      private int numberOfSets;
      protected CacheBlock[] blocks;

      protected AbstractCache(int numberOfBlocks, int blockSizeInWords, int setSizeInBlocks) {
         this.numberOfBlocks = numberOfBlocks;
         this.blockSizeInWords = blockSizeInWords;
         this.setSizeInBlocks = setSizeInBlocks;
         this.numberOfSets = numberOfBlocks / setSizeInBlocks;
         this.blocks = new CacheBlock[numberOfBlocks];
         this.reset();
      }

      public int getNumberOfBlocks() {
         return this.numberOfBlocks;
      }

      public int getNumberOfSets() {
         return this.numberOfSets;
      }

      public int getSetSizeInBlocks() {
         return this.setSizeInBlocks;
      }

      public int getBlockSizeInWords() {
         return this.blockSizeInWords;
      }

      public int getCacheSizeInWords() {
         return this.numberOfBlocks * this.blockSizeInWords;
      }

      public int getCacheSizeInBytes() {
         return this.numberOfBlocks * this.blockSizeInWords * 4;
      }

      public int getSetNumber(int address) {
         return address / 4 / this.blockSizeInWords % this.numberOfSets;
      }

      public int getTag(int address) {
         return address / 4 / this.blockSizeInWords / this.numberOfSets;
      }

      public int getFirstBlockToSearch(int address) {
         return this.getSetNumber(address) * this.setSizeInBlocks;
      }

      public int getLastBlockToSearch(int address) {
         return this.getFirstBlockToSearch(address) + this.setSizeInBlocks - 1;
      }

      public void reset() {
         for(int i = 0; i < this.numberOfBlocks; ++i) {
            this.blocks[i] = CacheSimulator.this.new CacheBlock(this.blockSizeInWords);
         }

         System.gc();
      }

      public abstract CacheAccessResult isItAHitThenReadOnMiss(int var1);
   }

   private class CacheAccessResult {
      private boolean hitOrMiss;
      private int blockNumber;

      public CacheAccessResult(boolean hitOrMiss, int blockNumber) {
         this.hitOrMiss = hitOrMiss;
         this.blockNumber = blockNumber;
      }

      public boolean isHit() {
         return this.hitOrMiss;
      }

      public int getBlock() {
         return this.blockNumber;
      }
   }

   private class CacheBlock {
      private boolean valid = false;
      private int tag = 0;
      private int sizeInWords;
      private int mostRecentAccessTime;

      public CacheBlock(int sizeInWords) {
         this.sizeInWords = sizeInWords;
         this.mostRecentAccessTime = -1;
      }
   }
}
