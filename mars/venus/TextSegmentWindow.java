package mars.venus;

import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import mars.Globals;
import mars.ProgramStatement;
import mars.Settings;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryAccessNotice;
import mars.mips.hardware.RegisterFile;
import mars.simulator.Simulator;
import mars.simulator.SimulatorNotice;
import mars.util.Binary;
import mars.util.EditorFont;

public class TextSegmentWindow extends JInternalFrame implements Observer {
   private JPanel programArgumentsPanel;
   private JTextField programArgumentsTextField;
   private static final int PROGRAM_ARGUMENT_TEXTFIELD_COLUMNS = 40;
   private JTable table;
   private JScrollPane tableScroller;
   private Object[][] data;
   private int[] intAddresses;
   private Hashtable addressRows;
   private Hashtable executeMods;
   private Container contentPane;
   private TextTableModel tableModel;
   private Font tableCellFont = new Font("Lucida Sans Typewriter", 0, 12);
   private boolean codeHighlighting;
   private boolean breakpointsEnabled;
   private int highlightAddress;
   private TableModelListener tableModelListener;
   private boolean inDelaySlot;
   private static String[] columnNames = new String[]{"Bkpt", "Address", "Code", "Basic", "Source"};
   private static final int BREAK_COLUMN = 0;
   private static final int ADDRESS_COLUMN = 1;
   private static final int CODE_COLUMN = 2;
   private static final int BASIC_COLUMN = 3;
   private static final int SOURCE_COLUMN = 4;
   private static final Font monospacedPlain12Point = new Font("Lucida Sans Typewriter", 0, 14);
   private static final String modifiedCodeMarker = " ------ ";

   public TextSegmentWindow() {
      super("Text Segment", true, false, true, true);
      Simulator.getInstance().addObserver(this);
      Globals.getSettings().addObserver(this);
      this.contentPane = this.getContentPane();
      this.codeHighlighting = true;
      this.breakpointsEnabled = true;
      this.programArgumentsPanel = new JPanel(new FlowLayout(0));
      this.programArgumentsPanel.add(new JLabel("Program Arguments: "));
      this.programArgumentsTextField = new JTextField(40);
      this.programArgumentsTextField.setToolTipText("Arguments provided to program at runtime via $a0 (argc) and $a1 (argv)");
      this.programArgumentsPanel.add(this.programArgumentsTextField);
   }

   public void setupTable() {
      int addressBase = Globals.getGui().getMainPane().getExecutePane().getAddressDisplayBase();
      this.codeHighlighting = true;
      this.breakpointsEnabled = true;
      ArrayList sourceStatementList = Globals.program.getMachineList();
      this.data = new Object[sourceStatementList.size()][columnNames.length];
      this.intAddresses = new int[this.data.length];
      this.addressRows = new Hashtable(this.data.length);
      this.executeMods = new Hashtable(this.data.length);
      int maxSourceLineNumber = 0;

      int sourceLineDigits;
      for(sourceLineDigits = sourceStatementList.size() - 1; sourceLineDigits >= 0; --sourceLineDigits) {
         ProgramStatement statement = (ProgramStatement)sourceStatementList.get(sourceLineDigits);
         if (statement.getSourceLine() > maxSourceLineNumber) {
            maxSourceLineNumber = statement.getSourceLine();
         }
      }

      sourceLineDigits = ("" + maxSourceLineNumber).length();
      int lastLine = -1;

      for(int i = 0; i < sourceStatementList.size(); ++i) {
         ProgramStatement statement = (ProgramStatement)sourceStatementList.get(i);
         this.intAddresses[i] = statement.getAddress();
         this.addressRows.put(new Integer(this.intAddresses[i]), new Integer(i));
         this.data[i][0] = Boolean.FALSE;
         this.data[i][1] = NumberDisplayBaseChooser.formatUnsignedInteger(statement.getAddress(), addressBase);
         this.data[i][2] = NumberDisplayBaseChooser.formatNumber(statement.getBinaryStatement(), 16);
         this.data[i][3] = statement.getPrintableBasicAssemblyStatement();
         String sourceString = "";
         if (!statement.getSource().equals("")) {
            int leadingSpaces = sourceLineDigits - ("" + statement.getSourceLine()).length();
            String lineNumber = "          ".substring(0, leadingSpaces) + statement.getSourceLine() + ": ";
            if (statement.getSourceLine() == lastLine) {
               lineNumber = "          ".substring(0, sourceLineDigits) + "  ";
            }

            sourceString = lineNumber + EditorFont.substituteSpacesForTabs(statement.getSource());
         }

         this.data[i][4] = sourceString;
         lastLine = statement.getSourceLine();
      }

      this.contentPane.removeAll();
      this.tableModel = new TextTableModel(this.data);
      if (this.tableModelListener != null) {
         this.tableModel.addTableModelListener(this.tableModelListener);
         this.tableModel.fireTableDataChanged();
      }

      this.table = new MyTippedJTable(this.tableModel);
      this.table.setRowSelectionAllowed(false);
      this.table.getColumnModel().getColumn(0).setMinWidth(40);
      this.table.getColumnModel().getColumn(1).setMinWidth(80);
      this.table.getColumnModel().getColumn(2).setMinWidth(80);
      this.table.getColumnModel().getColumn(0).setMaxWidth(50);
      this.table.getColumnModel().getColumn(1).setMaxWidth(90);
      this.table.getColumnModel().getColumn(2).setMaxWidth(90);
      this.table.getColumnModel().getColumn(3).setMaxWidth(200);
      this.table.getColumnModel().getColumn(0).setPreferredWidth(40);
      this.table.getColumnModel().getColumn(1).setPreferredWidth(80);
      this.table.getColumnModel().getColumn(2).setPreferredWidth(80);
      this.table.getColumnModel().getColumn(3).setPreferredWidth(160);
      this.table.getColumnModel().getColumn(4).setPreferredWidth(280);
      CodeCellRenderer codeStepHighlighter = new CodeCellRenderer();
      this.table.getColumnModel().getColumn(3).setCellRenderer(codeStepHighlighter);
      this.table.getColumnModel().getColumn(4).setCellRenderer(codeStepHighlighter);
      this.table.getColumnModel().getColumn(1).setCellRenderer(new MonoRightCellRenderer());
      this.table.getColumnModel().getColumn(2).setCellRenderer(new MachineCodeCellRenderer());
      this.table.getColumnModel().getColumn(0).setCellRenderer(new CheckBoxTableCellRenderer());
      this.reorderColumns();
      this.table.getColumnModel().addColumnModelListener(new MyTableColumnMovingListener((MyTableColumnMovingListener)null));
      this.tableScroller = new JScrollPane(this.table, 22, 32);
      this.contentPane.add(this.tableScroller);
      if (Globals.getSettings().getProgramArguments()) {
         this.addProgramArgumentsPanel();
      }

      this.deleteAsTextSegmentObserver();
      if (Globals.getSettings().getBooleanSetting(20)) {
         this.addAsTextSegmentObserver();
      }

   }

   public String getProgramArguments() {
      return this.programArgumentsTextField.getText();
   }

   public void addProgramArgumentsPanel() {
      if (this.contentPane != null && this.contentPane.getComponentCount() > 0) {
         this.contentPane.add(this.programArgumentsPanel, "North");
         this.contentPane.validate();
      }

   }

   public void removeProgramArgumentsPanel() {
      if (this.contentPane != null) {
         this.contentPane.remove(this.programArgumentsPanel);
         this.contentPane.validate();
      }

   }

   public void clearWindow() {
      this.contentPane.removeAll();
   }

   public void registerTableModelListener(TableModelListener tml) {
      this.tableModelListener = tml;
   }

   public void updateCodeAddresses() {
      if (this.contentPane.getComponentCount() != 0) {
         int addressBase = Globals.getGui().getMainPane().getExecutePane().getAddressDisplayBase();

         for(int i = 0; i < this.intAddresses.length; ++i) {
            String formattedAddress = NumberDisplayBaseChooser.formatUnsignedInteger(this.intAddresses[i], addressBase);
            this.table.getModel().setValueAt(formattedAddress, i, 1);
         }

      }
   }

   public void updateBasicStatements() {
      if (this.contentPane.getComponentCount() != 0) {
         ArrayList sourceStatementList = Globals.program.getMachineList();

         for(int i = 0; i < sourceStatementList.size(); ++i) {
            ProgramStatement statement;
            if (this.executeMods.get(i) == null) {
               statement = (ProgramStatement)sourceStatementList.get(i);
               this.table.getModel().setValueAt(statement.getPrintableBasicAssemblyStatement(), i, 3);
            } else {
               try {
                  statement = new ProgramStatement(Binary.stringToInt((String)this.table.getModel().getValueAt(i, 2)), Binary.stringToInt((String)this.table.getModel().getValueAt(i, 1)));
                  this.table.getModel().setValueAt(statement.getPrintableBasicAssemblyStatement(), i, 3);
               } catch (NumberFormatException var4) {
                  this.table.getModel().setValueAt("", i, 3);
               }
            }
         }

      }
   }

   public void update(Observable observable, Object obj) {
      if (observable == Simulator.getInstance()) {
         SimulatorNotice notice = (SimulatorNotice)obj;
         if (notice.getAction() == 0) {
            this.deleteAsTextSegmentObserver();
            if (Globals.getSettings().getBooleanSetting(20)) {
               this.addAsTextSegmentObserver();
            }
         }
      } else if (observable == Globals.getSettings()) {
         this.deleteAsTextSegmentObserver();
         if (Globals.getSettings().getBooleanSetting(20)) {
            this.addAsTextSegmentObserver();
         }
      } else if (obj instanceof MemoryAccessNotice) {
         MemoryAccessNotice access = (MemoryAccessNotice)obj;
         if (access.getAccessType() == 1) {
            int address = access.getAddress();
            int value = access.getValue();
            String strValue = Binary.intToHexString(access.getValue());
            String strBasic = " ------ ";
            String strSource = " ------ ";
            int row;
            try {
               row = this.findRowForAddress(address);
            } catch (IllegalArgumentException var13) {
               return;
            }

            ModifiedCode mc = (ModifiedCode)this.executeMods.get(row);
            if (mc == null) {
               if (this.tableModel.getValueAt(row, 2).equals(strValue)) {
                  return;
               }

               mc = new ModifiedCode(row, this.tableModel.getValueAt(row, 2), this.tableModel.getValueAt(row, 3), this.tableModel.getValueAt(row, 4), (ModifiedCode)null);
               this.executeMods.put(row, mc);
               strBasic = (new ProgramStatement(value, address)).getPrintableBasicAssemblyStatement();
            } else if (mc.getCode().equals(strValue)) {
               strBasic = (String)mc.getBasic();
               strSource = (String)mc.getSource();
               this.executeMods.remove(row);
            } else {
               strBasic = (new ProgramStatement(value, address)).getPrintableBasicAssemblyStatement();
            }

            this.data[row][2] = strValue;
            this.tableModel.fireTableCellUpdated(row, 2);
            this.tableModel.setValueAt(strBasic, row, 3);
            this.tableModel.setValueAt(strSource, row, 4);

            try {
               Globals.getGui().getMainPane().getExecutePane().getDataSegmentWindow().update(Memory.getInstance(), new MemoryAccessNotice(1, address, value));
            } catch (Exception var12) {
            }
         }
      }

   }

   void resetModifiedSourceCode() {
      if (this.executeMods != null && !this.executeMods.isEmpty()) {
         Enumeration elements = this.executeMods.elements();

         while(elements.hasMoreElements()) {
            ModifiedCode mc = (ModifiedCode)elements.nextElement();
            this.tableModel.setValueAt(mc.getCode(), mc.getRow(), 2);
            this.tableModel.setValueAt(mc.getBasic(), mc.getRow(), 3);
            this.tableModel.setValueAt(mc.getSource(), mc.getRow(), 4);
         }

         this.executeMods.clear();
      }

   }

   int getIntCodeAddressAtRow(int row) {
      return this.intAddresses[row];
   }

   public int getBreakpointCount() {
      int breakpointCount = 0;

      for(int i = 0; i < this.data.length; ++i) {
         if ((Boolean)this.data[i][0]) {
            ++breakpointCount;
         }
      }

      return breakpointCount;
   }

   public int[] getSortedBreakPointsArray() {
      int breakpointCount = this.getBreakpointCount();
      if (breakpointCount != 0 && this.breakpointsEnabled) {
         int[] breakpoints = new int[breakpointCount];
         breakpointCount = 0;

         for(int i = 0; i < this.data.length; ++i) {
            if ((Boolean)this.data[i][0]) {
               breakpoints[breakpointCount++] = this.intAddresses[i];
            }
         }

         Arrays.sort(breakpoints);
         return breakpoints;
      } else {
         return null;
      }
   }

   public void clearAllBreakpoints() {
      for(int i = 0; i < this.tableModel.getRowCount(); ++i) {
         if ((Boolean)this.data[i][0]) {
            this.tableModel.setValueAt(Boolean.FALSE, i, 0);
         }
      }

      ((JCheckBox)((DefaultCellEditor)this.table.getCellEditor(0, 0)).getComponent()).setSelected(false);
   }

   public void highlightStepAtPC() {
      this.highlightStepAtAddress(RegisterFile.getProgramCounter(), false);
   }

   public void highlightStepAtPC(boolean inDelaySlot) {
      this.highlightStepAtAddress(RegisterFile.getProgramCounter(), inDelaySlot);
   }

   public void highlightStepAtAddress(int address) {
      this.highlightStepAtAddress(address, false);
   }

   public void highlightStepAtAddress(int address, boolean inDelaySlot) {
      this.highlightAddress = address;
      int row;
      try {
         row = this.findRowForAddress(address);
      } catch (IllegalArgumentException var5) {
         return;
      }

      this.table.scrollRectToVisible(this.table.getCellRect(row, 0, true));
      this.inDelaySlot = inDelaySlot;
      this.table.tableChanged(new TableModelEvent(this.tableModel));
   }

   public void setCodeHighlighting(boolean highlightSetting) {
      this.codeHighlighting = highlightSetting;
   }

   public boolean getCodeHighlighting() {
      return this.codeHighlighting;
   }

   public void unhighlightAllSteps() {
      boolean saved = this.getCodeHighlighting();
      this.setCodeHighlighting(false);
      this.table.tableChanged(new TableModelEvent(this.tableModel, 0, this.data.length - 1, 3));
      this.table.tableChanged(new TableModelEvent(this.tableModel, 0, this.data.length - 1, 4));
      this.setCodeHighlighting(saved);
   }

   void selectStepAtAddress(int address) {
      int addressRow;
      try {
         addressRow = this.findRowForAddress(address);
      } catch (IllegalArgumentException var14) {
         return;
      }

      int addressSourceColumn = this.table.convertColumnIndexToView(4);
      Rectangle sourceCell = this.table.getCellRect(addressRow, addressSourceColumn, true);
      double cellHeight = sourceCell.getHeight();
      double viewHeight = this.tableScroller.getViewport().getExtentSize().getHeight();
      int numberOfVisibleRows = (int)(viewHeight / cellHeight);
      int newViewPositionY = Math.max((int)((double)(addressRow - numberOfVisibleRows / 2) * cellHeight), 0);
      this.tableScroller.getViewport().setViewPosition(new Point(0, newViewPositionY));
      MouseEvent fakeMouseEvent = new MouseEvent(this.table, 501, (new Date()).getTime(), 16, (int)sourceCell.getX() + 1, (int)sourceCell.getY() + 1, 1, false);
      MouseListener[] mouseListeners = this.table.getMouseListeners();

      for(int i = 0; i < mouseListeners.length; ++i) {
         mouseListeners[i].mousePressed(fakeMouseEvent);
      }

   }

   public void toggleBreakpoints() {
      Rectangle rect = ((MyTippedJTable)this.table).getRectForColumnIndex(0);
      MouseEvent fakeMouseEvent = new MouseEvent(this.table, 500, (new Date()).getTime(), 16, (int)rect.getX(), (int)rect.getY(), 1, false);
      MouseListener[] mouseListeners = ((MyTippedJTable)this.table).tableHeader.getMouseListeners();

      for(int i = 0; i < mouseListeners.length; ++i) {
         mouseListeners[i].mouseClicked(fakeMouseEvent);
      }

   }

   private void addAsTextSegmentObserver() {
      try {
         Memory.getInstance().addObserver(this, Memory.textBaseAddress, Memory.dataSegmentBaseAddress);
      } catch (AddressErrorException var2) {
      }

   }

   private void deleteAsTextSegmentObserver() {
      Memory.getInstance().deleteObserver(this);
   }

   private void reorderColumns() {
      TableColumnModel oldtcm = this.table.getColumnModel();
      TableColumnModel newtcm = new DefaultTableColumnModel();
      int[] savedColumnOrder = Globals.getSettings().getTextColumnOrder();
      if (savedColumnOrder.length == this.table.getColumnCount()) {
         for(int i = 0; i < savedColumnOrder.length; ++i) {
            newtcm.addColumn(oldtcm.getColumn(savedColumnOrder[i]));
         }

         this.table.setColumnModel(newtcm);
      }

   }

   private int findRowForAddress(int address) throws IllegalArgumentException {
      try {
         int addressRow = (Integer)this.addressRows.get(new Integer(address));
         return addressRow;
      } catch (NullPointerException var4) {
         throw new IllegalArgumentException();
      }
   }

   class CheckBoxTableCellRenderer extends JCheckBox implements TableCellRenderer {
      Border noFocusBorder;
      Border focusBorder;

      public CheckBoxTableCellRenderer() {
         this.setContentAreaFilled(true);
         this.setBorderPainted(true);
         this.setHorizontalAlignment(0);
         this.setVerticalAlignment(0);
      }

      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
         if (table != null) {
            if (isSelected) {
               this.setForeground(table.getSelectionForeground());
               this.setBackground(table.getSelectionBackground());
            } else {
               this.setForeground(table.getForeground());
               this.setBackground(table.getBackground());
            }

            this.setEnabled(table.isEnabled() && TextSegmentWindow.this.breakpointsEnabled);
            this.setComponentOrientation(table.getComponentOrientation());
            if (hasFocus) {
               if (this.focusBorder == null) {
                  this.focusBorder = UIManager.getBorder("Table.focusCellHighlightBorder");
               }

               this.setBorder(this.focusBorder);
            } else {
               if (this.noFocusBorder == null) {
                  if (this.focusBorder == null) {
                     this.focusBorder = UIManager.getBorder("Table.focusCellHighlightBorder");
                  }

                  if (this.focusBorder != null) {
                     Insets n = this.focusBorder.getBorderInsets(this);
                     this.noFocusBorder = new EmptyBorder(n);
                  }
               }

               this.setBorder(this.noFocusBorder);
            }

            this.setSelected(Boolean.TRUE.equals(value));
         }

         return this;
      }
   }

   class CodeCellRenderer extends DefaultTableCellRenderer {
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
         Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
         TextSegmentWindow textSegment = Globals.getGui().getMainPane().getExecutePane().getTextSegmentWindow();
         Settings settings = Globals.getSettings();
         boolean highlighting = textSegment.getCodeHighlighting();
         if (highlighting && textSegment.getIntCodeAddressAtRow(row) == TextSegmentWindow.this.highlightAddress) {
            if (!Simulator.inDelaySlot() && !textSegment.inDelaySlot) {
               cell.setBackground(settings.getColorSettingByPosition(4));
               cell.setForeground(settings.getColorSettingByPosition(5));
               cell.setFont(settings.getFontByPosition(3));
            } else {
               cell.setBackground(settings.getColorSettingByPosition(6));
               cell.setForeground(settings.getColorSettingByPosition(7));
               cell.setFont(settings.getFontByPosition(4));
            }
         } else if (row % 2 == 0) {
            cell.setBackground(settings.getColorSettingByPosition(0));
            cell.setForeground(settings.getColorSettingByPosition(1));
            cell.setFont(settings.getFontByPosition(1));
         } else {
            cell.setBackground(settings.getColorSettingByPosition(2));
            cell.setForeground(settings.getColorSettingByPosition(3));
            cell.setFont(settings.getFontByPosition(2));
         }

         return cell;
      }
   }

   class MachineCodeCellRenderer extends DefaultTableCellRenderer {
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
         JLabel cell = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
         cell.setFont(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT);
         cell.setHorizontalAlignment(4);
         if (row % 2 == 0) {
            cell.setBackground(Globals.getSettings().getColorSettingByPosition(0));
            cell.setForeground(Globals.getSettings().getColorSettingByPosition(1));
         } else {
            cell.setBackground(Globals.getSettings().getColorSettingByPosition(2));
            cell.setForeground(Globals.getSettings().getColorSettingByPosition(3));
         }

         return cell;
      }
   }

   private class ModifiedCode {
      private Integer row;
      private Object code;
      private Object basic;
      private Object source;

      private ModifiedCode(Integer row, Object code, Object basic, Object source) {
         this.row = row;
         this.code = code;
         this.basic = basic;
         this.source = source;
      }

      private Integer getRow() {
         return this.row;
      }

      private Object getCode() {
         return this.code;
      }

      private Object getBasic() {
         return this.basic;
      }

      private Object getSource() {
         return this.source;
      }

      // $FF: synthetic method
      ModifiedCode(Integer var2, Object var3, Object var4, Object var5, ModifiedCode var6) {
         this(var2, var3, var4, var5);
      }
   }

   private class MyTableColumnMovingListener implements TableColumnModelListener {
      private MyTableColumnMovingListener() {
      }

      public void columnAdded(TableColumnModelEvent e) {
      }

      public void columnRemoved(TableColumnModelEvent e) {
      }

      public void columnMarginChanged(ChangeEvent e) {
      }

      public void columnSelectionChanged(ListSelectionEvent e) {
      }

      public void columnMoved(TableColumnModelEvent e) {
         int[] columnOrder = new int[TextSegmentWindow.this.table.getColumnCount()];

         for(int i = 0; i < columnOrder.length; ++i) {
            columnOrder[i] = TextSegmentWindow.this.table.getColumnModel().getColumn(i).getModelIndex();
         }

         int[] oldOrder = Globals.getSettings().getTextColumnOrder();

         for(int ix = 0; ix < columnOrder.length; ++ix) {
            if (oldOrder[ix] != columnOrder[ix]) {
               Globals.getSettings().setTextColumnOrder(columnOrder);
               break;
            }
         }

      }

      // $FF: synthetic method
      MyTableColumnMovingListener(MyTableColumnMovingListener var2) {
         this();
      }
   }

   private class MyTippedJTable extends JTable {
      private JTableHeader tableHeader;
      private String[] columnToolTips = new String[]{"If checked, will set an execution breakpoint. Click header to disable/enable breakpoints", "Text segment address of binary instruction code", "32-bit binary MIPS instruction", "Basic assembler instruction", "Source code line"};

      MyTippedJTable(TextTableModel m) {
         super(m);
      }

      protected JTableHeader createDefaultTableHeader() {
         this.tableHeader = new TextTableHeader(this.columnModel);
         return this.tableHeader;
      }

      public Rectangle getRectForColumnIndex(int realIndex) {
         for(int i = 0; i < this.columnModel.getColumnCount(); ++i) {
            if (this.columnModel.getColumn(i).getModelIndex() == realIndex) {
               return this.tableHeader.getHeaderRect(i);
            }
         }

         return this.tableHeader.getHeaderRect(realIndex);
      }

      private class TextTableHeader extends JTableHeader {
         public TextTableHeader(TableColumnModel cm) {
            super(cm);
            this.addMouseListener(new TextTableHeaderMouseListener((TextTableHeaderMouseListener)null));
         }

         public String getToolTipText(MouseEvent e) {
            Point p = e.getPoint();
            int index = this.columnModel.getColumnIndexAtX(p.x);
            int realIndex = this.columnModel.getColumn(index).getModelIndex();
            return MyTippedJTable.this.columnToolTips[realIndex];
         }

         private class TextTableHeaderMouseListener implements MouseListener {
            private TextTableHeaderMouseListener() {
            }

            public void mouseClicked(MouseEvent e) {
               Point p = e.getPoint();
               int index = TextTableHeader.this.columnModel.getColumnIndexAtX(p.x);
               int realIndex = TextTableHeader.this.columnModel.getColumn(index).getModelIndex();
               if (realIndex == 0) {
                  JCheckBox check = (JCheckBox)((DefaultCellEditor)TextTableHeader.this.table.getCellEditor(0, index)).getComponent();
                  TextSegmentWindow.this.breakpointsEnabled = !TextSegmentWindow.this.breakpointsEnabled;
                  check.setEnabled(TextSegmentWindow.this.breakpointsEnabled);
                  TextTableHeader.this.table.tableChanged(new TableModelEvent(TextSegmentWindow.this.tableModel, 0, TextSegmentWindow.this.data.length - 1, 0));
               }

            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }

            // $FF: synthetic method
            TextTableHeaderMouseListener(TextTableHeaderMouseListener var2) {
               this();
            }
         }
      }
   }

   class TextTableModel extends AbstractTableModel {
      Object[][] data;

      public TextTableModel(Object[][] d) {
         this.data = d;
      }

      public int getColumnCount() {
         return TextSegmentWindow.columnNames.length;
      }

      public int getRowCount() {
         return this.data.length;
      }

      public String getColumnName(int col) {
         return TextSegmentWindow.columnNames[col];
      }

      public Object getValueAt(int row, int col) {
         return this.data[row][col];
      }

      public Class getColumnClass(int c) {
         return this.getValueAt(0, c).getClass();
      }

      public boolean isCellEditable(int row, int col) {
         return col == 0 || col == 2 && Globals.getSettings().getBooleanSetting(20);
      }

      public void setValueAt(Object value, int row, int col) {
         if (col != 2) {
            this.data[row][col] = value;
            this.fireTableCellUpdated(row, col);
         } else {
            int address = 0;
            if (!value.equals(this.data[row][col])) {
               int val;
               try {
                  val = Binary.stringToInt((String)value);
               } catch (NumberFormatException var11) {
                  this.data[row][col] = "INVALID";
                  this.fireTableCellUpdated(row, col);
                  return;
               }

               try {
                  address = Binary.stringToInt((String)this.data[row][1]);
               } catch (NumberFormatException var10) {
               }

               synchronized(Globals.memoryAndRegistersLock) {
                  try {
                     Globals.memory.setRawWord(address, val);
                  } catch (AddressErrorException var8) {
                     return;
                  }

               }
            }
         }
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
}
