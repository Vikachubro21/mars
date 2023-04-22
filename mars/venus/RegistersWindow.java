package mars.venus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import mars.Globals;
import mars.Settings;
import mars.mips.hardware.Register;
import mars.mips.hardware.RegisterAccessNotice;
import mars.mips.hardware.RegisterFile;
import mars.simulator.Simulator;
import mars.simulator.SimulatorNotice;
import mars.util.Binary;

public class RegistersWindow extends JPanel implements Observer {
   private static JTable table;
   private static Register[] registers;
   private Object[][] tableData;
   private boolean highlighting;
   private int highlightRow;
   private ExecutePane executePane;
   private static final int NAME_COLUMN = 0;
   private static final int NUMBER_COLUMN = 1;
   private static final int VALUE_COLUMN = 2;
   private static Settings settings;

   public RegistersWindow() {
      Simulator.getInstance().addObserver(this);
      settings = Globals.getSettings();
      this.highlighting = false;
      table = new MyTippedJTable(new RegTableModel(this.setupWindow()));
      table.getColumnModel().getColumn(0).setPreferredWidth(25);
      table.getColumnModel().getColumn(1).setPreferredWidth(25);
      table.getColumnModel().getColumn(2).setPreferredWidth(60);
      table.getColumnModel().getColumn(0).setCellRenderer(new RegisterCellRenderer(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT, 2));
      table.getColumnModel().getColumn(1).setCellRenderer(new RegisterCellRenderer(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT, 4));
      table.getColumnModel().getColumn(2).setCellRenderer(new RegisterCellRenderer(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT, 4));
      table.setPreferredScrollableViewportSize(new Dimension(200, 700));
      this.setLayout(new BorderLayout());
      this.add(new JScrollPane(table, 20, 31));
   }

   public Object[][] setupWindow() {
      int valueBase = NumberDisplayBaseChooser.getBase(settings.getDisplayValuesInHex());
      this.tableData = new Object[35][3];
      registers = RegisterFile.getRegisters();

      for(int i = 0; i < registers.length; ++i) {
         this.tableData[i][0] = registers[i].getName();
         this.tableData[i][1] = new Integer(registers[i].getNumber());
         this.tableData[i][2] = NumberDisplayBaseChooser.formatNumber(registers[i].getValue(), valueBase);
      }

      this.tableData[32][0] = "pc";
      this.tableData[32][1] = "";
      this.tableData[32][2] = NumberDisplayBaseChooser.formatUnsignedInteger(RegisterFile.getProgramCounter(), valueBase);
      this.tableData[33][0] = "hi";
      this.tableData[33][1] = "";
      this.tableData[33][2] = NumberDisplayBaseChooser.formatNumber(RegisterFile.getValue(33), valueBase);
      this.tableData[34][0] = "lo";
      this.tableData[34][1] = "";
      this.tableData[34][2] = NumberDisplayBaseChooser.formatNumber(RegisterFile.getValue(34), valueBase);
      return this.tableData;
   }

   public void clearWindow() {
      this.clearHighlighting();
      RegisterFile.resetRegisters();
      this.updateRegisters(Globals.getGui().getMainPane().getExecutePane().getValueDisplayBase());
   }

   public void clearHighlighting() {
      this.highlighting = false;
      if (table != null) {
         table.tableChanged(new TableModelEvent(table.getModel()));
      }

      this.highlightRow = -1;
   }

   public void refresh() {
      if (table != null) {
         table.tableChanged(new TableModelEvent(table.getModel()));
      }

   }

   public void updateRegisters() {
      this.updateRegisters(Globals.getGui().getMainPane().getExecutePane().getValueDisplayBase());
   }

   public void updateRegisters(int base) {
      registers = RegisterFile.getRegisters();

      for(int i = 0; i < registers.length; ++i) {
         this.updateRegisterValue(registers[i].getNumber(), registers[i].getValue(), base);
      }

      this.updateRegisterUnsignedValue(32, RegisterFile.getProgramCounter(), base);
      this.updateRegisterValue(33, RegisterFile.getValue(33), base);
      this.updateRegisterValue(34, RegisterFile.getValue(34), base);
   }

   public void updateRegisterValue(int number, int val, int base) {
      ((RegTableModel)table.getModel()).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(val, base), number, 2);
   }

   private void updateRegisterUnsignedValue(int number, int val, int base) {
      ((RegTableModel)table.getModel()).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatUnsignedInteger(val, base), number, 2);
   }

   public void update(Observable observable, Object obj) {
      if (observable == Simulator.getInstance()) {
         SimulatorNotice notice = (SimulatorNotice)obj;
         if (notice.getAction() == 0) {
            if (notice.getRunSpeed() != 40.0 || notice.getMaxSteps() == 1) {
               RegisterFile.addRegistersObserver(this);
               this.highlighting = true;
            }
         } else {
            RegisterFile.deleteRegistersObserver(this);
         }
      } else if (obj instanceof RegisterAccessNotice) {
         RegisterAccessNotice access = (RegisterAccessNotice)obj;
         if (access.getAccessType() == 1) {
            this.highlighting = true;
            this.highlightCellForRegister((Register)observable);
            Globals.getGui().getRegistersPane().setSelectedComponent(this);
         }
      }

   }

   void highlightCellForRegister(Register register) {
      this.highlightRow = register.getNumber();
      table.tableChanged(new TableModelEvent(table.getModel()));
   }

   private class MyTippedJTable extends JTable {
      private String[] regToolTips = new String[]{"constant 0", "reserved for assembler", "expression evaluation and results of a function", "expression evaluation and results of a function", "argument 1", "argument 2", "argument 3", "argument 4", "temporary (not preserved across call)", "temporary (not preserved across call)", "temporary (not preserved across call)", "temporary (not preserved across call)", "temporary (not preserved across call)", "temporary (not preserved across call)", "temporary (not preserved across call)", "temporary (not preserved across call)", "saved temporary (preserved across call)", "saved temporary (preserved across call)", "saved temporary (preserved across call)", "saved temporary (preserved across call)", "saved temporary (preserved across call)", "saved temporary (preserved across call)", "saved temporary (preserved across call)", "saved temporary (preserved across call)", "temporary (not preserved across call)", "temporary (not preserved across call)", "reserved for OS kernel", "reserved for OS kernel", "pointer to global area", "stack pointer", "frame pointer", "return address (used by function call)", "program counter", "high-order word of multiply product, or divide remainder", "low-order word of multiply product, or divide quotient"};
      private String[] columnToolTips = new String[]{"Each register has a tool tip describing its usage convention", "Corresponding register number", "Current 32 bit value"};

      MyTippedJTable(RegTableModel m) {
         super(m);
         this.setRowSelectionAllowed(true);
         this.setSelectionBackground(Color.GREEN);
      }

      public String getToolTipText(MouseEvent e) {
         String tip = null;
         Point p = e.getPoint();
         int rowIndex = this.rowAtPoint(p);
         int colIndex = this.columnAtPoint(p);
         int realColumnIndex = this.convertColumnIndexToModel(colIndex);
         if (realColumnIndex == 0) {
            tip = this.regToolTips[rowIndex];
         } else {
            tip = super.getToolTipText(e);
         }

         return tip;
      }

      protected JTableHeader createDefaultTableHeader() {
         return new JTableHeader(this.columnModel) {
            public String getToolTipText(MouseEvent e) {
               String tip = null;
               Point p = e.getPoint();
               int index = this.columnModel.getColumnIndexAtX(p.x);
               int realIndex = this.columnModel.getColumn(index).getModelIndex();
               return MyTippedJTable.this.columnToolTips[realIndex];
            }
         };
      }
   }

   class RegTableModel extends AbstractTableModel {
      final String[] columnNames = new String[]{"Name", "Number", "Value"};
      Object[][] data;

      public RegTableModel(Object[][] d) {
         this.data = d;
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

      public Class getColumnClass(int c) {
         return this.getValueAt(0, c).getClass();
      }

      public boolean isCellEditable(int row, int col) {
         return col == 2 && row != 0 && row != 32 && row != 31;
      }

      public void setValueAt(Object value, int row, int col) {
         int val;
         try {
            val = Binary.stringToInt((String)value);
         } catch (NumberFormatException var7) {
            this.data[row][col] = "INVALID";
            this.fireTableCellUpdated(row, col);
            return;
         }

         synchronized(Globals.memoryAndRegistersLock) {
            RegisterFile.updateRegister(row, val);
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

   private class RegisterCellRenderer extends DefaultTableCellRenderer {
      private Font font;
      private int alignment;

      public RegisterCellRenderer(Font font, int alignment) {
         this.font = font;
         this.alignment = alignment;
      }

      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
         JLabel cell = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
         cell.setFont(this.font);
         cell.setHorizontalAlignment(this.alignment);
         if (RegistersWindow.settings.getRegistersHighlighting() && RegistersWindow.this.highlighting && row == RegistersWindow.this.highlightRow) {
            cell.setBackground(RegistersWindow.settings.getColorSettingByPosition(10));
            cell.setForeground(RegistersWindow.settings.getColorSettingByPosition(11));
            cell.setFont(RegistersWindow.settings.getFontByPosition(6));
         } else if (row % 2 == 0) {
            cell.setBackground(RegistersWindow.settings.getColorSettingByPosition(0));
            cell.setForeground(RegistersWindow.settings.getColorSettingByPosition(1));
            cell.setFont(RegistersWindow.settings.getFontByPosition(1));
         } else {
            cell.setBackground(RegistersWindow.settings.getColorSettingByPosition(2));
            cell.setForeground(RegistersWindow.settings.getColorSettingByPosition(3));
            cell.setFont(RegistersWindow.settings.getFontByPosition(2));
         }

         return cell;
      }
   }
}
