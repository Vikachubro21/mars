package mars.venus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JCheckBox;
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
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.InvalidRegisterAccessException;
import mars.mips.hardware.Register;
import mars.mips.hardware.RegisterAccessNotice;
import mars.simulator.Simulator;
import mars.simulator.SimulatorNotice;
import mars.util.Binary;

public class Coprocessor1Window extends JPanel implements ActionListener, Observer {
   private static JTable table;
   private static Register[] registers;
   private Object[][] tableData;
   private boolean highlighting;
   private int highlightRow;
   private ExecutePane executePane;
   private JCheckBox[] conditionFlagCheckBox;
   private static final int NAME_COLUMN = 0;
   private static final int FLOAT_COLUMN = 1;
   private static final int DOUBLE_COLUMN = 2;
   private static Settings settings;

   public Coprocessor1Window() {
      Simulator.getInstance().addObserver(this);
      settings = Globals.getSettings();
      this.setLayout(new BorderLayout());
      table = new MyTippedJTable(new RegTableModel(this.setupWindow()));
      table.getColumnModel().getColumn(0).setPreferredWidth(20);
      table.getColumnModel().getColumn(1).setPreferredWidth(70);
      table.getColumnModel().getColumn(2).setPreferredWidth(130);
      table.getColumnModel().getColumn(0).setCellRenderer(new RegisterCellRenderer(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT, 2));
      table.getColumnModel().getColumn(1).setCellRenderer(new RegisterCellRenderer(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT, 4));
      table.getColumnModel().getColumn(2).setCellRenderer(new RegisterCellRenderer(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT, 4));
      this.add(new JScrollPane(table, 20, 31));
      JPanel flagsPane = new JPanel(new BorderLayout());
      flagsPane.setToolTipText("flags are used by certain floating point instructions, default flag is 0");
      flagsPane.add(new JLabel("Condition Flags", 0), "North");
      int numFlags = Coprocessor1.getConditionFlagCount();
      this.conditionFlagCheckBox = new JCheckBox[numFlags];
      JPanel checksPane = new JPanel(new GridLayout(2, numFlags / 2));
      for(int i = 0; i < numFlags; ++i) {
         this.conditionFlagCheckBox[i] = new JCheckBox(Integer.toString(i));
         this.conditionFlagCheckBox[i].addActionListener(this);
         this.conditionFlagCheckBox[i].setToolTipText("checked == 1, unchecked == 0");
         checksPane.add(this.conditionFlagCheckBox[i]);
      }

      flagsPane.add(checksPane, "Center");
      this.add(flagsPane, "South");
   }

   public void actionPerformed(ActionEvent e) {
      JCheckBox checkBox = (JCheckBox)e.getSource();
      int i = Integer.parseInt(checkBox.getText());
      if (checkBox.isSelected()) {
         checkBox.setSelected(true);
         Coprocessor1.setConditionFlag(i);
      } else {
         checkBox.setSelected(false);
         Coprocessor1.clearConditionFlag(i);
      }

   }

   public Object[][] setupWindow() {
      registers = Coprocessor1.getRegisters();
      this.highlighting = false;
      this.tableData = new Object[registers.length][3];

      for(int i = 0; i < registers.length; ++i) {
         this.tableData[i][0] = registers[i].getName();
         this.tableData[i][1] = NumberDisplayBaseChooser.formatFloatNumber(registers[i].getValue(), NumberDisplayBaseChooser.getBase(settings.getDisplayValuesInHex()));
         if (i % 2 == 0) {
            long longValue = 0L;

            try {
               longValue = Coprocessor1.getLongFromRegisterPair(registers[i].getName());
            } catch (InvalidRegisterAccessException var5) {
            }

            this.tableData[i][2] = NumberDisplayBaseChooser.formatDoubleNumber(longValue, NumberDisplayBaseChooser.getBase(settings.getDisplayValuesInHex()));
         } else {
            this.tableData[i][2] = "";
         }
      }

      return this.tableData;
   }

   public void clearWindow() {
      this.clearHighlighting();
      Coprocessor1.resetRegisters();
      this.updateRegisters(Globals.getGui().getMainPane().getExecutePane().getValueDisplayBase());
      Coprocessor1.clearConditionFlags();
      this.updateConditionFlagDisplay();
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
      registers = Coprocessor1.getRegisters();

      for(int i = 0; i < registers.length; ++i) {
         this.updateFloatRegisterValue(registers[i].getNumber(), registers[i].getValue(), base);
         if (i % 2 == 0) {
            this.updateDoubleRegisterValue(i, base);
         }
      }

      this.updateConditionFlagDisplay();
   }

   private void updateConditionFlagDisplay() {
      for(int i = 0; i < this.conditionFlagCheckBox.length; ++i) {
         this.conditionFlagCheckBox[i].setSelected(Coprocessor1.getConditionFlag(i) != 0);
      }

   }

   public void updateFloatRegisterValue(int number, int val, int base) {
      ((RegTableModel)table.getModel()).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatFloatNumber(val, base), number, 1);
   }

   public void updateDoubleRegisterValue(int number, int base) {
      long val = 0L;

      try {
         val = Coprocessor1.getLongFromRegisterPair(registers[number].getName());
      } catch (InvalidRegisterAccessException var6) {
      }

      ((RegTableModel)table.getModel()).setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatDoubleNumber(val, base), number, 2);
   }

   public void update(Observable observable, Object obj) {
      if (observable == Simulator.getInstance()) {
         SimulatorNotice notice = (SimulatorNotice)obj;
         if (notice.getAction() == 0) {
            if (notice.getRunSpeed() != 40.0 || notice.getMaxSteps() == 1) {
               Coprocessor1.addRegistersObserver(this);
               this.highlighting = true;
            }
         } else {
            Coprocessor1.deleteRegistersObserver(this);
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
      private String[] regToolTips = new String[]{"floating point subprogram return value", "should not be referenced explicitly in your program", "floating point subprogram return value", "should not be referenced explicitly in your program", "temporary (not preserved across call)", "should not be referenced explicitly in your program", "temporary (not preserved across call)", "should not be referenced explicitly in your program", "temporary (not preserved across call)", "should not be referenced explicitly in your program", "temporary (not preserved across call)", "should not be referenced explicitly in your program", "floating point subprogram argument 1", "should not be referenced explicitly in your program", "floating point subprogram argument 2", "should not be referenced explicitly in your program", "temporary (not preserved across call)", "should not be referenced explicitly in your program", "temporary (not preserved across call)", "should not be referenced explicitly in your program", "saved temporary (preserved across call)", "should not be referenced explicitly in your program", "saved temporary (preserved across call)", "should not be referenced explicitly in your program", "saved temporary (preserved across call)", "should not be referenced explicitly in your program", "saved temporary (preserved across call)", "should not be referenced explicitly in your program", "saved temporary (preserved across call)", "should not be referenced explicitly in your program", "saved temporary (preserved across call)", "should not be referenced explicitly in your program"};
      private String[] columnToolTips = new String[]{"Each register has a tool tip describing its usage convention", "32-bit single precision IEEE 754 floating point register", "64-bit double precision IEEE 754 floating point register (uses a pair of 32-bit registers)"};

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
      final String[] columnNames = new String[]{"Name", "Float", "Double"};
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
         return col == 1 || col == 2 && row % 2 == 0;
      }

      public void setValueAt(Object value, int row, int col) {
         int valueBase = Globals.getGui().getMainPane().getExecutePane().getValueDisplayBase();
         String sVal = (String)value;

         try {
            if (col == 1) {
               int dReg;
               if (Binary.isHex(sVal)) {
                  dReg = Binary.stringToInt(sVal);
                  synchronized(Globals.memoryAndRegistersLock) {
                     Coprocessor1.updateRegister(row, dReg);
                  }

                  this.data[row][col] = NumberDisplayBaseChooser.formatFloatNumber(dReg, valueBase);
               } else {
                  float fVal = Float.parseFloat(sVal);
                  synchronized(Globals.memoryAndRegistersLock) {
                     Coprocessor1.setRegisterToFloat(row, fVal);
                  }

                  this.data[row][col] = NumberDisplayBaseChooser.formatNumber(fVal, valueBase);
               }

               dReg = row - row % 2;
               this.setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatDoubleNumber(Coprocessor1.getLongFromRegisterPair(dReg), valueBase), dReg, 2);
            } else if (col == 2) {
               if (Binary.isHex(sVal)) {
                  long lVal = Binary.stringToLong(sVal);
                  synchronized(Globals.memoryAndRegistersLock) {
                     Coprocessor1.setRegisterPairToLong(row, lVal);
                  }

                  this.setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatDoubleNumber(lVal, valueBase), row, col);
               } else {
                  double dVal = Double.parseDouble(sVal);
                  synchronized(Globals.memoryAndRegistersLock) {
                     Coprocessor1.setRegisterPairToDouble(row, dVal);
                  }

                  this.setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(dVal, valueBase), row, col);
               }

               this.setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(Coprocessor1.getValue(row), valueBase), row, 1);
               this.setDisplayAndModelValueAt(NumberDisplayBaseChooser.formatNumber(Coprocessor1.getValue(row + 1), valueBase), row + 1, 1);
            }
         } catch (NumberFormatException var16) {
            this.data[row][col] = "INVALID";
            this.fireTableCellUpdated(row, col);
         } catch (InvalidRegisterAccessException var17) {
            this.fireTableCellUpdated(row, col);
         }

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
         if (Coprocessor1Window.settings.getRegistersHighlighting() && Coprocessor1Window.this.highlighting && row == Coprocessor1Window.this.highlightRow) {
            cell.setBackground(Coprocessor1Window.settings.getColorSettingByPosition(10));
            cell.setForeground(Coprocessor1Window.settings.getColorSettingByPosition(11));
            cell.setFont(Coprocessor1Window.settings.getFontByPosition(6));
         } else if (row % 2 == 0) {
            cell.setBackground(Coprocessor1Window.settings.getColorSettingByPosition(0));
            cell.setForeground(Coprocessor1Window.settings.getColorSettingByPosition(1));
            cell.setFont(Coprocessor1Window.settings.getFontByPosition(1));
         } else {
            cell.setBackground(Coprocessor1Window.settings.getColorSettingByPosition(2));
            cell.setForeground(Coprocessor1Window.settings.getColorSettingByPosition(3));
            cell.setFont(Coprocessor1Window.settings.getFontByPosition(2));
         }

         return cell;
      }
   }
}
