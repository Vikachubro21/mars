package mars.venus;

import java.awt.*;
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
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Register;
import mars.mips.hardware.RegisterAccessNotice;
import mars.simulator.Simulator;
import mars.simulator.SimulatorNotice;
import mars.util.Binary;

public class Coprocessor0Window extends JPanel implements Observer {
   private static JTable table;
   private static Register[] registers;
   private Object[][] tableData;
   private boolean highlighting;
   private int highlightRow;
   private ExecutePane executePane;
   private int[] rowGivenRegNumber;
   private static final int NAME_COLUMN = 0;
   private static final int NUMBER_COLUMN = 1;
   private static final int VALUE_COLUMN = 2;
   private static Settings settings;

   private static boolean darkTheme;

   public Coprocessor0Window() {
      Simulator.getInstance().addObserver(this);
      settings = Globals.getSettings();
      darkTheme = settings.getBooleanSetting(21);
      this.highlighting = false;
      table = new MyTippedJTable(new RegTableModel(this.setupWindow()));
      table.getColumnModel().getColumn(0).setPreferredWidth(50);
      table.getColumnModel().getColumn(1).setPreferredWidth(25);
      table.getColumnModel().getColumn(2).setPreferredWidth(60);
      table.getColumnModel().getColumn(0).setCellRenderer(new RegisterCellRenderer(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT, 2));
      table.getColumnModel().getColumn(1).setCellRenderer(new RegisterCellRenderer(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT, 4));
      table.getColumnModel().getColumn(2).setCellRenderer(new RegisterCellRenderer(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT, 4));
      table.setPreferredScrollableViewportSize(new Dimension(200, 700));
      this.setLayout(new BorderLayout());
      this.add(new JScrollPane(table, 20, 30));
   }

   public Object[][] setupWindow() {
      registers = Coprocessor0.getRegisters();
      this.tableData = new Object[registers.length][3];
      this.rowGivenRegNumber = new int[32];

      for(int i = 0; i < registers.length; ++i) {
         this.rowGivenRegNumber[registers[i].getNumber()] = i;
         this.tableData[i][0] = registers[i].getName();
         this.tableData[i][1] = new Integer(registers[i].getNumber());
         this.tableData[i][2] = NumberDisplayBaseChooser.formatNumber(registers[i].getValue(), NumberDisplayBaseChooser.getBase(settings.getDisplayValuesInHex()));
      }

      return this.tableData;
   }

   public void clearWindow() {
      this.clearHighlighting();
      Coprocessor0.resetRegisters();
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
      registers = Coprocessor0.getRegisters();

      for(int i = 0; i < registers.length; ++i) {
         this.updateRegisterValue(registers[i].getNumber(), registers[i].getValue(), base);
      }

   }

   public void updateRegisterValue(int number, int val, int base) {
      ((RegTableModel)table.getModel()).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(val, base), this.rowGivenRegNumber[number], 2);
   }

   public void update(Observable observable, Object obj) {
      if (observable == Simulator.getInstance()) {
         SimulatorNotice notice = (SimulatorNotice)obj;
         if (notice.getAction() == 0) {
            if (notice.getRunSpeed() != 40.0 || notice.getMaxSteps() == 1) {
               Coprocessor0.addRegistersObserver(this);
               this.highlighting = true;
            }
         } else {
            Coprocessor0.deleteRegistersObserver(this);
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
      int registerRow = Coprocessor0.getRegisterPosition(register);
      if (registerRow >= 0) {
         this.highlightRow = registerRow;
         table.tableChanged(new TableModelEvent(table.getModel()));
      }
   }

   private class MyTippedJTable extends JTable {
      private String[] regToolTips = new String[]{"Memory address at which address exception occurred", "Interrupt mask and enable bits", "Exception type and pending interrupt bits", "Address of instruction that caused exception"};
      private String[] columnToolTips = new String[]{"Each register has a tool tip describing its usage convention", "Register number.  In your program, precede it with $", "Current 32 bit value"};

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
         return col == 2;
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
            Coprocessor0.updateRegister(Coprocessor0Window.registers[row].getNumber(), val);
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
         if (Coprocessor0Window.settings.getRegistersHighlighting() && Coprocessor0Window.this.highlighting && row == Coprocessor0Window.this.highlightRow) {
            cell.setBackground(Coprocessor0Window.settings.getColorSettingByPosition(10));
            cell.setForeground(Coprocessor0Window.settings.getColorSettingByPosition(11));
            cell.setFont(Coprocessor0Window.settings.getFontByPosition(6));
         } else if (row % 2 == 0) {
            cell.setBackground(Coprocessor0Window.settings.getColorSettingByPosition(0));
            cell.setForeground(Coprocessor0Window.settings.getColorSettingByPosition(1));
            cell.setFont(Coprocessor0Window.settings.getFontByPosition(1));
         } else {
            cell.setBackground(Coprocessor0Window.settings.getColorSettingByPosition(2));
            cell.setForeground(Coprocessor0Window.settings.getColorSettingByPosition(3));
            cell.setFont(Coprocessor0Window.settings.getFontByPosition(2));
         }

         return cell;
      }
   }
}
