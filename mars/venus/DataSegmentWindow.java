package mars.venus;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import mars.Globals;
import mars.Settings;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryAccessNotice;
import mars.mips.hardware.RegisterFile;
import mars.simulator.Simulator;
import mars.simulator.SimulatorNotice;
import mars.util.Binary;

public class DataSegmentWindow extends JInternalFrame implements Observer {
   private static final String[] dataSegmentNames = new String[]{"Data", "Stack", "Kernel"};
   private static Object[][] dataData;
   private static JTable dataTable;
   private JScrollPane dataTableScroller;
   private Container contentPane;
   private JPanel tablePanel;
   private JButton dataButton;
   private JButton nextButton;
   private JButton prevButton;
   private JButton stakButton;
   private JButton globButton;
   private JButton heapButton;
   private JButton kernButton;
   private JButton extnButton;
   private JButton mmioButton;
   private JButton textButton;
   private JCheckBox asciiDisplayCheckBox;
   static final int VALUES_PER_ROW = 8;
   static final int NUMBER_OF_ROWS = 16;
   static final int NUMBER_OF_COLUMNS = 9;
   static final int BYTES_PER_VALUE = 4;
   static final int BYTES_PER_ROW = 32;
   static final int MEMORY_CHUNK_SIZE = 512;
   static final int PREV_NEXT_CHUNK_SIZE = 256;
   static final int ADDRESS_COLUMN = 0;
   static final boolean USER_MODE = false;
   static final boolean KERNEL_MODE = true;
   private boolean addressHighlighting = false;
   private boolean asciiDisplay = false;
   private int addressRow;
   private int addressColumn;
   private int addressRowFirstAddress;
   private Settings settings;
   int firstAddress;
   int homeAddress;
   boolean userOrKernelMode;
   JComboBox baseAddressSelector;
   private String[] displayBaseAddressChoices;
   private int[] displayBaseAddresses;
   private int defaultBaseAddressIndex;
   JButton[] baseAddressButtons;
   private static final int EXTERN_BASE_ADDRESS_INDEX = 0;
   private static final int GLOBAL_POINTER_ADDRESS_INDEX = 3;
   private static final int TEXT_BASE_ADDRESS_INDEX = 5;
   private static final int DATA_BASE_ADDRESS_INDEX = 1;
   private static final int HEAP_BASE_ADDRESS_INDEX = 2;
   private static final int STACK_POINTER_BASE_ADDRESS_INDEX = 4;
   private static final int KERNEL_DATA_BASE_ADDRESS_INDEX = 6;
   private static final int MMIO_BASE_ADDRESS_INDEX = 7;
   private int[] displayBaseAddressArray;
   String[] descriptions;

   public DataSegmentWindow(NumberDisplayBaseChooser[] choosers) {
      super("Data Segment", true, false, true, true);
      this.displayBaseAddressArray = new int[]{Memory.externBaseAddress, Memory.dataBaseAddress, Memory.heapBaseAddress, -1, -1, Memory.textBaseAddress, Memory.kernelDataBaseAddress, Memory.memoryMapBaseAddress};
      this.descriptions = new String[]{" (.extern)", " (.data)", " (heap)", "current $gp", "current $sp", " (.text)", " (.kdata)", " (MMIO)"};
      Simulator.getInstance().addObserver(this);
      this.settings = Globals.getSettings();
      this.settings.addObserver(this);
      this.homeAddress = Memory.dataBaseAddress;
      this.firstAddress = this.homeAddress;
      this.userOrKernelMode = false;
      this.addressHighlighting = false;
      this.contentPane = this.getContentPane();
      this.tablePanel = new JPanel(new GridLayout(1, 2, 10, 0));
      JPanel features = new JPanel();
      Toolkit tk = Toolkit.getDefaultToolkit();
      Class cs = this.getClass();

      try {
         this.prevButton = new PrevButton(new ImageIcon(tk.getImage(cs.getResource("/images/Previous22.png"))));
         this.nextButton = new NextButton(new ImageIcon(tk.getImage(cs.getResource("/images/Next22.png"))));
         this.dataButton = new JButton();
         this.stakButton = new JButton();
         this.globButton = new JButton();
         this.heapButton = new JButton();
         this.extnButton = new JButton();
         this.mmioButton = new JButton();
         this.textButton = new JButton();
         this.kernButton = new JButton();
      } catch (NullPointerException var7) {
         System.out.println("Internal Error: images folder not found");
         System.exit(0);
      }

      this.initializeBaseAddressChoices();
      this.baseAddressSelector = new JComboBox();
      this.baseAddressSelector.setModel(new CustomComboBoxModel(this.displayBaseAddressChoices));
      this.baseAddressSelector.setEditable(false);
      this.baseAddressSelector.setSelectedIndex(this.defaultBaseAddressIndex);
      this.baseAddressSelector.setToolTipText("Base address for data segment display");
      this.baseAddressSelector.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            DataSegmentWindow.this.baseAddressButtons[DataSegmentWindow.this.baseAddressSelector.getSelectedIndex()].getActionListeners()[0].actionPerformed((ActionEvent)null);
         }
      });
      this.addButtonActionListenersAndInitialize();
      JPanel navButtons = new JPanel(new GridLayout(1, 4));
      navButtons.add(this.prevButton);
      navButtons.add(this.nextButton);
      features.add(navButtons);
      features.add(this.baseAddressSelector);

      for(int i = 0; i < choosers.length; ++i) {
         features.add(choosers[i]);
      }

      this.asciiDisplayCheckBox = new JCheckBox("ASCII", this.asciiDisplay);
      this.asciiDisplayCheckBox.setToolTipText("Display data segment values in ASCII (overrides Hexadecimal Values setting)");
      this.asciiDisplayCheckBox.addItemListener(new ItemListener() {
         public void itemStateChanged(ItemEvent e) {
            DataSegmentWindow.this.asciiDisplay = e.getStateChange() == 1;
            DataSegmentWindow.this.updateValues();
         }
      });
      features.add(this.asciiDisplayCheckBox);
      this.contentPane.add(features, "South");
   }

   public void updateBaseAddressComboBox() {
      this.displayBaseAddressArray[0] = Memory.externBaseAddress;
      this.displayBaseAddressArray[3] = -1;
      this.displayBaseAddressArray[1] = Memory.dataBaseAddress;
      this.displayBaseAddressArray[2] = Memory.heapBaseAddress;
      this.displayBaseAddressArray[4] = -1;
      this.displayBaseAddressArray[6] = Memory.kernelDataBaseAddress;
      this.displayBaseAddressArray[7] = Memory.memoryMapBaseAddress;
      this.displayBaseAddressArray[5] = Memory.textBaseAddress;
      this.displayBaseAddressChoices = this.createBaseAddressLabelsArray(this.displayBaseAddressArray, this.descriptions);
      this.baseAddressSelector.setModel(new CustomComboBoxModel(this.displayBaseAddressChoices));
      this.displayBaseAddresses = this.displayBaseAddressArray;
      this.baseAddressSelector.setSelectedIndex(this.defaultBaseAddressIndex);
   }

   void selectCellForAddress(int address) {
      Point rowColumn = this.displayCellForAddress(address);
      if (rowColumn != null) {
         Rectangle addressCell = dataTable.getCellRect(rowColumn.x, rowColumn.y, true);
         MouseEvent fakeMouseEvent = new MouseEvent(dataTable, 501, (new Date()).getTime(), 16, (int)addressCell.getX() + 1, (int)addressCell.getY() + 1, 1, false);
         MouseListener[] mouseListeners = dataTable.getMouseListeners();

         for(int i = 0; i < mouseListeners.length; ++i) {
            mouseListeners[i].mousePressed(fakeMouseEvent);
         }

      }
   }

   void highlightCellForAddress(int address) {
      Point rowColumn = this.displayCellForAddress(address);
      if (rowColumn != null && rowColumn.x >= 0 && rowColumn.y >= 0) {
         this.addressRow = rowColumn.x;
         this.addressColumn = rowColumn.y;
         this.addressRowFirstAddress = Binary.stringToInt(dataTable.getValueAt(this.addressRow, 0).toString());
         dataTable.tableChanged(new TableModelEvent(dataTable.getModel(), 0, dataData.length - 1));
      }
   }

   private Point displayCellForAddress(int address) {
      int desiredComboBoxIndex = this.getBaseAddressIndexForAddress(address);
      if (desiredComboBoxIndex < 0) {
         return null;
      } else {
         this.baseAddressSelector.setSelectedIndex(desiredComboBoxIndex);
         ((CustomComboBoxModel)this.baseAddressSelector.getModel()).forceComboBoxUpdate(desiredComboBoxIndex);
         this.baseAddressButtons[desiredComboBoxIndex].getActionListeners()[0].actionPerformed((ActionEvent)null);
         int baseAddress = this.displayBaseAddressArray[desiredComboBoxIndex];
         if (baseAddress == -1) {
            if (desiredComboBoxIndex == 3) {
               baseAddress = RegisterFile.getValue(28) - RegisterFile.getValue(28) % 32;
            } else {
               if (desiredComboBoxIndex != 4) {
                  return null;
               }

               baseAddress = RegisterFile.getValue(29) - RegisterFile.getValue(29) % 32;
            }
         }

         int byteOffset = address - baseAddress;
         int chunkOffset = byteOffset / 512;
         int byteOffsetIntoChunk = byteOffset % 512;
         this.firstAddress = this.firstAddress + chunkOffset * 512 - 256;
         this.nextButton.getActionListeners()[0].actionPerformed((ActionEvent)null);
         int addrRow = byteOffsetIntoChunk / 32;
         int addrColumn = byteOffsetIntoChunk % 32 / 4 + 1;
         addrColumn = dataTable.convertColumnIndexToView(addrColumn);
         Rectangle addressCell = dataTable.getCellRect(addrRow, addrColumn, true);
         double cellHeight = addressCell.getHeight();
         double viewHeight = this.dataTableScroller.getViewport().getExtentSize().getHeight();
         int numberOfVisibleRows = (int)(viewHeight / cellHeight);
         int newViewPositionY = Math.max((int)((double)(addrRow - numberOfVisibleRows / 2) * cellHeight), 0);
         this.dataTableScroller.getViewport().setViewPosition(new Point(0, newViewPositionY));
         return new Point(addrRow, addrColumn);
      }
   }

   private void initializeBaseAddressChoices() {
      this.baseAddressButtons = new JButton[this.descriptions.length];
      this.baseAddressButtons[0] = this.extnButton;
      this.baseAddressButtons[3] = this.globButton;
      this.baseAddressButtons[1] = this.dataButton;
      this.baseAddressButtons[2] = this.heapButton;
      this.baseAddressButtons[4] = this.stakButton;
      this.baseAddressButtons[6] = this.kernButton;
      this.baseAddressButtons[7] = this.mmioButton;
      this.baseAddressButtons[5] = this.textButton;
      this.displayBaseAddresses = this.displayBaseAddressArray;
      this.displayBaseAddressChoices = this.createBaseAddressLabelsArray(this.displayBaseAddressArray, this.descriptions);
      this.defaultBaseAddressIndex = 1;
   }

   private String[] createBaseAddressLabelsArray(int[] baseAddressArray, String[] descriptions) {
      String[] baseAddressChoices = new String[baseAddressArray.length];

      for(int i = 0; i < baseAddressChoices.length; ++i) {
         baseAddressChoices[i] = (baseAddressArray[i] != -1 ? Binary.intToHexString(baseAddressArray[i]) : "") + descriptions[i];
      }

      return baseAddressChoices;
   }

   private int getBaseAddressIndexForAddress(int address) {
      int desiredComboBoxIndex = -1;
      if (Memory.inKernelDataSegment(address)) {
         return 6;
      } else if (Memory.inMemoryMapSegment(address)) {
         return 7;
      } else if (Memory.inTextSegment(address)) {
         return 5;
      } else {
         int shortDistance = Integer.MAX_VALUE;
         int thisDistance = address - Memory.externBaseAddress;
         if (thisDistance >= 0 && thisDistance < shortDistance) {
            shortDistance = thisDistance;
            desiredComboBoxIndex = 0;
         }

         thisDistance = Math.abs(address - RegisterFile.getValue(28));
         if (thisDistance < shortDistance) {
            shortDistance = thisDistance;
            desiredComboBoxIndex = 3;
         }

         thisDistance = address - Memory.dataBaseAddress;
         if (thisDistance >= 0 && thisDistance < shortDistance) {
            shortDistance = thisDistance;
            desiredComboBoxIndex = 1;
         }

         thisDistance = address - Memory.heapBaseAddress;
         if (thisDistance >= 0 && thisDistance < shortDistance) {
            shortDistance = thisDistance;
            desiredComboBoxIndex = 2;
         }

         thisDistance = Math.abs(address - RegisterFile.getValue(29));
         if (thisDistance < shortDistance) {
            desiredComboBoxIndex = 4;
         }

         return desiredComboBoxIndex;
      }
   }

   private JScrollPane generateDataPanel() {
      dataData = new Object[16][9];
      int valueBase = Globals.getGui().getMainPane().getExecutePane().getValueDisplayBase();
      int addressBase = Globals.getGui().getMainPane().getExecutePane().getAddressDisplayBase();
      int address = this.homeAddress;

      int column;
      for(int row = 0; row < 16; ++row) {
         dataData[row][0] = NumberDisplayBaseChooser.formatUnsignedInteger(address, addressBase);

         for(column = 1; column < 9; ++column) {
            try {
               dataData[row][column] = NumberDisplayBaseChooser.formatNumber(Globals.memory.getRawWord(address), valueBase);
            } catch (AddressErrorException var8) {
               dataData[row][column] = NumberDisplayBaseChooser.formatNumber(0, valueBase);
            }

            address += 4;
         }
      }

      String[] names = new String[9];

      for(column = 0; column < 9; ++column) {
         names[column] = this.getHeaderStringForColumn(column, addressBase);
      }

      dataTable = new MyTippedJTable(new DataTableModel(dataData, names));
      dataTable.getTableHeader().setReorderingAllowed(false);
      dataTable.setRowSelectionAllowed(false);
      MonoRightCellRenderer monoRightCellRenderer = new MonoRightCellRenderer();
      dataTable.getColumnModel().getColumn(0).setPreferredWidth(60);
      dataTable.getColumnModel().getColumn(0).setCellRenderer(monoRightCellRenderer);
      AddressCellRenderer addressCellRenderer = new AddressCellRenderer();

      for(int i = 1; i < 9; ++i) {
         dataTable.getColumnModel().getColumn(i).setPreferredWidth(60);
         dataTable.getColumnModel().getColumn(i).setCellRenderer(addressCellRenderer);
      }

      this.dataTableScroller = new JScrollPane(dataTable, 22, 32);
      return this.dataTableScroller;
   }

   private String getHeaderStringForColumn(int i, int base) {
      return i == 0 ? "Address" : "Value (+" + Integer.toString((i - 1) * 4, base) + ")";
   }

   public void setupTable() {
      this.tablePanel.removeAll();
      this.tablePanel.add(this.generateDataPanel());
      this.contentPane.add(this.tablePanel);
      this.enableAllButtons();
   }

   public void clearWindow() {
      this.tablePanel.removeAll();
      this.disableAllButtons();
   }

   public void clearHighlighting() {
      this.addressHighlighting = false;
      dataTable.tableChanged(new TableModelEvent(dataTable.getModel(), 0, dataData.length - 1));
      this.addressColumn = -1;
   }

   private int getValueDisplayFormat() {
      return this.asciiDisplay ? 0 : Globals.getGui().getMainPane().getExecutePane().getValueDisplayBase();
   }

   public void updateModelForMemoryRange(int firstAddr) {
      if (this.tablePanel.getComponentCount() != 0) {
         int valueBase = this.getValueDisplayFormat();
         int addressBase = Globals.getGui().getMainPane().getExecutePane().getAddressDisplayBase();
         int address = firstAddr;
         TableModel dataModel = dataTable.getModel();

         for(int row = 0; row < 16; ++row) {
            ((DataTableModel)dataModel).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatUnsignedInteger(address, addressBase), row, 0);

            for(int column = 1; column < 9; ++column) {
               try {
                  ((DataTableModel)dataModel).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(Globals.memory.getWordNoNotify(address), valueBase), row, column);
               } catch (AddressErrorException var12) {
                  if (Memory.inTextSegment(address)) {
                     int displayValue = 0;
                     if (!Globals.getSettings().getBooleanSetting(20)) {
                        Globals.getSettings().setBooleanSettingNonPersistent(20, true);

                        try {
                           displayValue = Globals.memory.getWordNoNotify(address);
                        } catch (AddressErrorException var11) {
                        }

                        Globals.getSettings().setBooleanSettingNonPersistent(20, false);
                     }

                     ((DataTableModel)dataModel).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(displayValue, valueBase), row, column);
                  } else {
                     ((DataTableModel)dataModel).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(0, valueBase), row, column);
                  }
               }

               address += 4;
            }
         }

      }
   }

   public void updateCell(int address, int value) {
      int offset = address - this.firstAddress;
      if (offset >= 0 && offset < 512) {
         int row = offset / 32;
         int column = offset % 32 / 4 + 1;
         int valueBase = Globals.getGui().getMainPane().getExecutePane().getValueDisplayBase();
         ((DataTableModel)dataTable.getModel()).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(value, valueBase), row, column);
      }
   }

   public void updateDataAddresses() {
      if (this.tablePanel.getComponentCount() != 0) {
         int addressBase = Globals.getGui().getMainPane().getExecutePane().getAddressDisplayBase();
         int address = this.firstAddress;

         int i;
         for(i = 0; i < 16; ++i) {
            String formattedAddress = NumberDisplayBaseChooser.formatUnsignedInteger(address, addressBase);
            ((DataTableModel)dataTable.getModel()).setDisplayAndModelValueAt(formattedAddress, i, 0);
            address += 32;
         }

         for(i = 1; i < 9; ++i) {
            dataTable.getColumnModel().getColumn(i).setHeaderValue(this.getHeaderStringForColumn(i, addressBase));
         }

         dataTable.getTableHeader().repaint();
      }
   }

   public void updateValues() {
      this.updateModelForMemoryRange(this.firstAddress);
   }

   public void resetMemoryRange() {
      this.baseAddressSelector.getActionListeners()[0].actionPerformed((ActionEvent)null);
   }

   public void resetValues() {
      int valueBase = Globals.getGui().getMainPane().getExecutePane().getValueDisplayBase();
      TableModel dataModel = dataTable.getModel();

      for(int row = 0; row < 16; ++row) {
         for(int column = 1; column < 9; ++column) {
            ((DataTableModel)dataModel).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(0, valueBase), row, column);
         }
      }

      this.disableAllButtons();
   }

   private void disableAllButtons() {
      this.baseAddressSelector.setEnabled(false);
      this.globButton.setEnabled(false);
      this.stakButton.setEnabled(false);
      this.heapButton.setEnabled(false);
      this.extnButton.setEnabled(false);
      this.mmioButton.setEnabled(false);
      this.textButton.setEnabled(false);
      this.kernButton.setEnabled(false);
      this.prevButton.setEnabled(false);
      this.nextButton.setEnabled(false);
      this.dataButton.setEnabled(false);
   }

   private void enableAllButtons() {
      this.baseAddressSelector.setEnabled(true);
      this.globButton.setEnabled(true);
      this.stakButton.setEnabled(true);
      this.heapButton.setEnabled(true);
      this.extnButton.setEnabled(true);
      this.mmioButton.setEnabled(true);
      this.textButton.setEnabled(this.settings.getBooleanSetting(20));
      this.kernButton.setEnabled(true);
      this.prevButton.setEnabled(true);
      this.nextButton.setEnabled(true);
      this.dataButton.setEnabled(true);
   }

   private void addButtonActionListenersAndInitialize() {
      this.disableAllButtons();
      this.globButton.setToolTipText("View range around global pointer");
      this.stakButton.setToolTipText("View range around stack pointer");
      this.heapButton.setToolTipText("View range around heap base address " + Binary.intToHexString(Memory.heapBaseAddress));
      this.kernButton.setToolTipText("View range around kernel data base address " + Binary.intToHexString(Memory.kernelDataBaseAddress));
      this.extnButton.setToolTipText("View range around static global base address " + Binary.intToHexString(Memory.externBaseAddress));
      this.mmioButton.setToolTipText("View range around MMIO base address " + Binary.intToHexString(Memory.memoryMapBaseAddress));
      this.textButton.setToolTipText("View range around program code " + Binary.intToHexString(Memory.textBaseAddress));
      this.prevButton.setToolTipText("View next lower address range; hold down for rapid fire");
      this.nextButton.setToolTipText("View next higher address range; hold down for rapid fire");
      this.dataButton.setToolTipText("View range around static data segment base address " + Binary.intToHexString(Memory.dataBaseAddress));
      this.globButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            DataSegmentWindow.this.userOrKernelMode = false;
            DataSegmentWindow.this.firstAddress = Math.max(Memory.dataSegmentBaseAddress, RegisterFile.getValue(28));
            DataSegmentWindow var10000 = DataSegmentWindow.this;
            var10000.firstAddress -= DataSegmentWindow.this.firstAddress % 32;
            DataSegmentWindow.this.homeAddress = DataSegmentWindow.this.firstAddress;
            DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.setFirstAddressAndPrevNextButtonEnableStatus(DataSegmentWindow.this.firstAddress);
            DataSegmentWindow.this.updateModelForMemoryRange(DataSegmentWindow.this.firstAddress);
         }
      });
      this.stakButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            DataSegmentWindow.this.userOrKernelMode = false;
            DataSegmentWindow.this.firstAddress = Math.max(Memory.dataSegmentBaseAddress, RegisterFile.getValue(29));
            DataSegmentWindow var10000 = DataSegmentWindow.this;
            var10000.firstAddress -= DataSegmentWindow.this.firstAddress % 32;
            DataSegmentWindow.this.homeAddress = Memory.stackBaseAddress;
            DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.setFirstAddressAndPrevNextButtonEnableStatus(DataSegmentWindow.this.firstAddress);
            DataSegmentWindow.this.updateModelForMemoryRange(DataSegmentWindow.this.firstAddress);
         }
      });
      this.heapButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            DataSegmentWindow.this.userOrKernelMode = false;
            DataSegmentWindow.this.homeAddress = Memory.heapBaseAddress;
            DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.setFirstAddressAndPrevNextButtonEnableStatus(DataSegmentWindow.this.homeAddress);
            DataSegmentWindow.this.updateModelForMemoryRange(DataSegmentWindow.this.firstAddress);
         }
      });
      this.extnButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            DataSegmentWindow.this.userOrKernelMode = false;
            DataSegmentWindow.this.homeAddress = Memory.externBaseAddress;
            DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.setFirstAddressAndPrevNextButtonEnableStatus(DataSegmentWindow.this.homeAddress);
            DataSegmentWindow.this.updateModelForMemoryRange(DataSegmentWindow.this.firstAddress);
         }
      });
      this.kernButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            DataSegmentWindow.this.userOrKernelMode = true;
            DataSegmentWindow.this.homeAddress = Memory.kernelDataBaseAddress;
            DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.homeAddress;
            DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.setFirstAddressAndPrevNextButtonEnableStatus(DataSegmentWindow.this.firstAddress);
            DataSegmentWindow.this.updateModelForMemoryRange(DataSegmentWindow.this.firstAddress);
         }
      });
      this.mmioButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            DataSegmentWindow.this.userOrKernelMode = true;
            DataSegmentWindow.this.homeAddress = Memory.memoryMapBaseAddress;
            DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.homeAddress;
            DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.setFirstAddressAndPrevNextButtonEnableStatus(DataSegmentWindow.this.firstAddress);
            DataSegmentWindow.this.updateModelForMemoryRange(DataSegmentWindow.this.firstAddress);
         }
      });
      this.textButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            DataSegmentWindow.this.userOrKernelMode = false;
            DataSegmentWindow.this.homeAddress = Memory.textBaseAddress;
            DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.homeAddress;
            DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.setFirstAddressAndPrevNextButtonEnableStatus(DataSegmentWindow.this.firstAddress);
            DataSegmentWindow.this.updateModelForMemoryRange(DataSegmentWindow.this.firstAddress);
         }
      });
      this.dataButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            DataSegmentWindow.this.userOrKernelMode = false;
            DataSegmentWindow.this.homeAddress = Memory.dataBaseAddress;
            DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.homeAddress;
            DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.setFirstAddressAndPrevNextButtonEnableStatus(DataSegmentWindow.this.firstAddress);
            DataSegmentWindow.this.updateModelForMemoryRange(DataSegmentWindow.this.firstAddress);
         }
      });
   }

   private int setFirstAddressAndPrevNextButtonEnableStatus(int lowAddress) {
      int lowLimit = !this.userOrKernelMode ? Math.min(Math.min(Memory.textBaseAddress, Memory.dataSegmentBaseAddress), Memory.dataBaseAddress) : Memory.kernelDataBaseAddress;
      int highLimit = !this.userOrKernelMode ? Memory.userHighAddress : Memory.kernelHighAddress;
      if (lowAddress <= lowLimit) {
         lowAddress = lowLimit;
         this.prevButton.setEnabled(false);
      } else {
         this.prevButton.setEnabled(true);
      }

      if (lowAddress >= highLimit - 512) {
         lowAddress = highLimit - 512 + 1;
         this.nextButton.setEnabled(false);
      } else {
         this.nextButton.setEnabled(true);
      }

      return lowAddress;
   }

   public void update(Observable observable, Object obj) {
      if (observable == Simulator.getInstance()) {
         SimulatorNotice notice = (SimulatorNotice)obj;
         if (notice.getAction() == 0) {
            if (notice.getRunSpeed() != 40.0 || notice.getMaxSteps() == 1) {
               Memory.getInstance().addObserver(this);
               this.addressHighlighting = true;
            }
         } else {
            Memory.getInstance().deleteObserver(this);
         }
      } else if (observable != this.settings && obj instanceof MemoryAccessNotice) {
         MemoryAccessNotice access = (MemoryAccessNotice)obj;
         if (access.getAccessType() == 1) {
            int address = access.getAddress();
            this.highlightCellForAddress(address);
         }
      }

   }

   class AddressCellRenderer extends DefaultTableCellRenderer {
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
         JLabel cell = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
         cell.setHorizontalAlignment(4);
         int rowFirstAddress = Binary.stringToInt(table.getValueAt(row, 0).toString());
         if (DataSegmentWindow.this.settings.getDataSegmentHighlighting() && DataSegmentWindow.this.addressHighlighting && rowFirstAddress == DataSegmentWindow.this.addressRowFirstAddress && column == DataSegmentWindow.this.addressColumn) {
            cell.setBackground(DataSegmentWindow.this.settings.getColorSettingByPosition(8));
            cell.setForeground(DataSegmentWindow.this.settings.getColorSettingByPosition(9));
            cell.setFont(DataSegmentWindow.this.settings.getFontByPosition(5));
         } else if (row % 2 == 0) {
            cell.setBackground(DataSegmentWindow.this.settings.getColorSettingByPosition(0));
            cell.setForeground(DataSegmentWindow.this.settings.getColorSettingByPosition(1));
            cell.setFont(DataSegmentWindow.this.settings.getFontByPosition(1));
         } else {
            cell.setBackground(DataSegmentWindow.this.settings.getColorSettingByPosition(2));
            cell.setForeground(DataSegmentWindow.this.settings.getColorSettingByPosition(3));
            cell.setFont(DataSegmentWindow.this.settings.getFontByPosition(2));
         }

         return cell;
      }
   }

   private class CustomComboBoxModel extends DefaultComboBoxModel {
      public CustomComboBoxModel(Object[] list) {
         super(list);
      }

      private void forceComboBoxUpdate(int index) {
         super.fireContentsChanged(this, index, index);
      }
   }

   class DataTableModel extends AbstractTableModel {
      String[] columnNames;
      Object[][] data;

      public DataTableModel(Object[][] d, String[] n) {
         this.data = d;
         this.columnNames = n;
      }

      public int getColumnCount() {
         return this.columnNames.length;
      }

      public int getRowCount() {
         return this.data.length;
      }

      public String getColumnName(int col) {
         return this.columnNames[col];
      }

      public Object getValueAt(int row, int col) {
         return this.data[row][col];
      }

      public boolean isCellEditable(int row, int col) {
         return col != 0 && !DataSegmentWindow.this.asciiDisplay;
      }

      public Class getColumnClass(int c) {
         return this.getValueAt(0, c).getClass();
      }

      public void setValueAt(Object value, int row, int col) {
         int address = 0;

         int val;
         try {
            val = Binary.stringToInt((String)value);
         } catch (NumberFormatException var11) {
            this.data[row][col] = "INVALID";
            this.fireTableCellUpdated(row, col);
            return;
         }

         try {
            address = Binary.stringToInt((String)this.data[row][0]) + (col - 1) * 4;
         } catch (NumberFormatException var10) {
         }

         synchronized(Globals.memoryAndRegistersLock) {
            try {
               Globals.memory.setRawWord(address, val);
            } catch (AddressErrorException var8) {
               return;
            }
         }

         int valueBase = Globals.getGui().getMainPane().getExecutePane().getValueDisplayBase();
         this.data[row][col] = NumberDisplayBaseChooser.formatNumber(val, valueBase);
         this.fireTableCellUpdated(row, col);
      }

      private void setDisplayAndModelValueAt(Object value, int row, int col) {
         this.data[row][col] = value;
         this.fireTableCellUpdated(row, col);
      }

      private void printDebugData() {
         int numRows = this.getRowCount();
         int numCols = this.getColumnCount();

         for(int i = 0; i < numRows; ++i) {
            System.out.print("    row " + i + ":");

            for(int j = 0; j < numCols; ++j) {
               System.out.print("  " + this.data[i][j]);
            }

            System.out.println();
         }

         System.out.println("--------------------------");
      }
   }

   private class MyTippedJTable extends JTable {
      private String[] columnToolTips = new String[]{"Base MIPS memory address for this row of the table.", "32-bit value stored at base address for its row.", "32-bit value stored ", " bytes beyond base address for its row."};

      MyTippedJTable(DataTableModel m) {
         super(m);
      }

      protected JTableHeader createDefaultTableHeader() {
         return new JTableHeader(this.columnModel) {
            public String getToolTipText(MouseEvent e) {
               String tip = null;
               Point p = e.getPoint();
               int index = this.columnModel.getColumnIndexAtX(p.x);
               int realIndex = this.columnModel.getColumn(index).getModelIndex();
               return realIndex < 2 ? MyTippedJTable.this.columnToolTips[realIndex] : MyTippedJTable.this.columnToolTips[2] + (realIndex - 1) * 4 + MyTippedJTable.this.columnToolTips[3];
            }
         };
      }
   }

   private class NextButton extends RepeatButton {
      public NextButton(Icon ico) {
         super(ico);
         this.setInitialDelay(500);
         this.setDelay(60);
         this.addActionListener(this);
      }

      public void actionPerformed(ActionEvent ae) {
         DataSegmentWindow var10000 = DataSegmentWindow.this;
         var10000.firstAddress += 256;
         DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.setFirstAddressAndPrevNextButtonEnableStatus(DataSegmentWindow.this.firstAddress);
         DataSegmentWindow.this.updateModelForMemoryRange(DataSegmentWindow.this.firstAddress);
      }
   }

   private class PrevButton extends RepeatButton {
      public PrevButton(Icon ico) {
         super(ico);
         this.setInitialDelay(500);
         this.setDelay(60);
         this.addActionListener(this);
      }

      public void actionPerformed(ActionEvent ae) {
         DataSegmentWindow var10000 = DataSegmentWindow.this;
         var10000.firstAddress -= 256;
         DataSegmentWindow.this.firstAddress = DataSegmentWindow.this.setFirstAddressAndPrevNextButtonEnableStatus(DataSegmentWindow.this.firstAddress);
         DataSegmentWindow.this.updateModelForMemoryRange(DataSegmentWindow.this.firstAddress);
      }
   }
}
