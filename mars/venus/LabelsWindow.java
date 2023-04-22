package mars.venus;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import mars.Globals;
import mars.MIPSprogram;
import mars.assembler.Symbol;
import mars.assembler.SymbolTable;
import mars.mips.hardware.Memory;
import mars.util.Binary;

public class LabelsWindow extends JInternalFrame {
   private Container contentPane;
   private JPanel labelPanel;
   private JCheckBox dataLabels;
   private JCheckBox textLabels;
   private ArrayList listOfLabelsForSymbolTable;
   private LabelsWindow labelsWindow;
   private static final int MAX_DISPLAYED_CHARS = 24;
   private static final int PREFERRED_NAME_COLUMN_WIDTH = 60;
   private static final int PREFERRED_ADDRESS_COLUMN_WIDTH = 60;
   private static final int LABEL_COLUMN = 0;
   private static final int ADDRESS_COLUMN = 1;
   private static final String[] columnToolTips = new String[]{"Programmer-defined label (identifier).", "Text or data segment address at which label is defined."};
   private static String[] columnNames;
   private Comparator tableSortComparator;
   private final Comparator[] tableSortingComparators = new Comparator[]{new LabelAddressAscendingComparator((LabelAddressAscendingComparator)null), new DescendingComparator(new LabelAddressAscendingComparator((LabelAddressAscendingComparator)null), (DescendingComparator)null), new LabelAddressAscendingComparator((LabelAddressAscendingComparator)null), new DescendingComparator(new LabelAddressAscendingComparator((LabelAddressAscendingComparator)null), (DescendingComparator)null), new LabelNameAscendingComparator((LabelNameAscendingComparator)null), new LabelNameAscendingComparator((LabelNameAscendingComparator)null), new DescendingComparator(new LabelNameAscendingComparator((LabelNameAscendingComparator)null), (DescendingComparator)null), new DescendingComparator(new LabelNameAscendingComparator((LabelNameAscendingComparator)null), (DescendingComparator)null)};
   private static final int[][] sortStateTransitions = new int[][]{{4, 1}, {5, 0}, {6, 3}, {7, 2}, {6, 0}, {7, 1}, {4, 2}, {5, 3}};
   private static final char ASCENDING_SYMBOL = '▲';
   private static final char DESCENDING_SYMBOL = '▼';
   private static final String[][] sortColumnHeadings = new String[][]{{"Label", "Address  ▲"}, {"Label", "Address  ▼"}, {"Label", "Address  ▲"}, {"Label", "Address  ▼"}, {"Label  ▲", "Address"}, {"Label  ▲", "Address"}, {"Label  ▼", "Address"}, {"Label  ▼", "Address"}};
   private int sortState = 0;

   public LabelsWindow() {
      super("Labels", true, false, true, true);

      try {
         this.sortState = Integer.parseInt(Globals.getSettings().getLabelSortState());
      } catch (NumberFormatException var2) {
         this.sortState = 0;
      }

      columnNames = sortColumnHeadings[this.sortState];
      this.tableSortComparator = this.tableSortingComparators[this.sortState];
      this.labelsWindow = this;
      this.contentPane = this.getContentPane();
      this.labelPanel = new JPanel(new GridLayout(1, 2, 10, 0));
      JPanel features = new JPanel();
      this.dataLabels = new JCheckBox("Data", true);
      this.textLabels = new JCheckBox("Text", true);
      this.dataLabels.addItemListener(new LabelItemListener((LabelItemListener)null));
      this.textLabels.addItemListener(new LabelItemListener((LabelItemListener)null));
      this.dataLabels.setToolTipText("If checked, will display labels defined in data segment");
      this.textLabels.setToolTipText("If checked, will display labels defined in text segment");
      features.add(this.dataLabels);
      features.add(this.textLabels);
      this.contentPane.add(features, "South");
      this.contentPane.add(this.labelPanel);
   }

   public void setupTable() {
      this.labelPanel.removeAll();
      this.labelPanel.add(this.generateLabelScrollPane());
   }

   public void clearWindow() {
      this.labelPanel.removeAll();
   }

   private JScrollPane generateLabelScrollPane() {
      this.listOfLabelsForSymbolTable = new ArrayList();
      this.listOfLabelsForSymbolTable.add(new LabelsForSymbolTable((MIPSprogram)null));
      ArrayList MIPSprogramsAssembled = RunAssembleAction.getMIPSprogramsToAssemble();
      Box allSymtabTables = Box.createVerticalBox();

      for(int i = 0; i < MIPSprogramsAssembled.size(); ++i) {
         this.listOfLabelsForSymbolTable.add(new LabelsForSymbolTable((MIPSprogram)MIPSprogramsAssembled.get(i)));
      }

      ArrayList tableNames = new ArrayList();
      JTableHeader tableHeader = null;

      for(int i = 0; i < this.listOfLabelsForSymbolTable.size(); ++i) {
         LabelsForSymbolTable symtab = (LabelsForSymbolTable)this.listOfLabelsForSymbolTable.get(i);
         if (symtab.hasSymbols()) {
            String name = symtab.getSymbolTableName();
            if (name.length() > 24) {
               name = name.substring(0, 21) + "...";
            }

            JLabel nameLab = new JLabel(name, 2);
            Box nameLabel = Box.createHorizontalBox();
            nameLabel.add(nameLab);
            nameLabel.add(Box.createHorizontalGlue());
            nameLabel.add(Box.createHorizontalStrut(1));
            tableNames.add(nameLabel);
            allSymtabTables.add(nameLabel);
            JTable table = symtab.generateLabelTable();
            tableHeader = table.getTableHeader();
            tableHeader.setReorderingAllowed(false);
            table.setSelectionBackground(table.getBackground());
            table.addMouseListener(new LabelDisplayMouseListener((LabelDisplayMouseListener)null));
            allSymtabTables.add(table);
         }
      }

      JScrollPane labelScrollPane = new JScrollPane(allSymtabTables, 22, 30);

      for(int i = 0; i < tableNames.size(); ++i) {
         JComponent nameLabel = (JComponent)tableNames.get(i);
         nameLabel.setMaximumSize(new Dimension(labelScrollPane.getViewport().getViewSize().width, (int)(1.5 * (double)nameLabel.getFontMetrics(nameLabel.getFont()).getHeight())));
      }

      labelScrollPane.setColumnHeaderView(tableHeader);
      return labelScrollPane;
   }

   public void updateLabelAddresses() {
      if (this.listOfLabelsForSymbolTable != null) {
         for(int i = 0; i < this.listOfLabelsForSymbolTable.size(); ++i) {
            ((LabelsForSymbolTable)this.listOfLabelsForSymbolTable.get(i)).updateLabelAddresses();
         }
      }

   }

   private class DescendingComparator implements Comparator {
      private Comparator opposite;

      private DescendingComparator(Comparator opposite) {
         this.opposite = opposite;
      }

      public int compare(Object a, Object b) {
         return this.opposite.compare(b, a);
      }

      // $FF: synthetic method
      DescendingComparator(Comparator var2, DescendingComparator var3) {
         this(var2);
      }
   }

   private class LabelAddressAscendingComparator implements Comparator {
      private LabelAddressAscendingComparator() {
      }

      public int compare(Object a, Object b) {
         int addrA = ((Symbol)a).getAddress();
         int addrB = ((Symbol)b).getAddress();
         return (addrA < 0 || addrB < 0) && (addrA >= 0 || addrB >= 0) ? addrB : addrA - addrB;
      }

      // $FF: synthetic method
      LabelAddressAscendingComparator(LabelAddressAscendingComparator var2) {
         this();
      }
   }

   private class LabelDisplayMouseListener extends MouseAdapter {
      private LabelDisplayMouseListener() {
      }

      public void mouseClicked(MouseEvent e) {
         JTable table = (JTable)e.getSource();
         int row = table.rowAtPoint(e.getPoint());
         int column = table.columnAtPoint(e.getPoint());
         Object data = table.getValueAt(row, column);
         if (table.getColumnName(column).equals(LabelsWindow.columnNames[0])) {
            data = table.getModel().getValueAt(row, 1);
         }

         int address = 0;

         try {
            address = Binary.stringToInt((String)data);
         } catch (NumberFormatException var8) {
         } catch (ClassCastException var9) {
         }

         if (!Memory.inTextSegment(address) && !Memory.inKernelTextSegment(address)) {
            Globals.getGui().getMainPane().getExecutePane().getDataSegmentWindow().selectCellForAddress(address);
         } else {
            Globals.getGui().getMainPane().getExecutePane().getTextSegmentWindow().selectStepAtAddress(address);
         }

      }

      // $FF: synthetic method
      LabelDisplayMouseListener(LabelDisplayMouseListener var2) {
         this();
      }
   }

   private class LabelItemListener implements ItemListener {
      private LabelItemListener() {
      }

      public void itemStateChanged(ItemEvent ie) {
         for(int i = 0; i < LabelsWindow.this.listOfLabelsForSymbolTable.size(); ++i) {
            ((LabelsForSymbolTable)LabelsWindow.this.listOfLabelsForSymbolTable.get(i)).generateLabelTable();
         }

      }

      // $FF: synthetic method
      LabelItemListener(LabelItemListener var2) {
         this();
      }
   }

   private class LabelNameAscendingComparator implements Comparator {
      private LabelNameAscendingComparator() {
      }

      public int compare(Object a, Object b) {
         return ((Symbol)a).getName().toLowerCase().compareTo(((Symbol)b).getName().toLowerCase());
      }

      // $FF: synthetic method
      LabelNameAscendingComparator(LabelNameAscendingComparator var2) {
         this();
      }
   }

   class LabelTableModel extends AbstractTableModel {
      String[] columns;
      Object[][] data;

      public LabelTableModel(Object[][] d, String[] n) {
         this.data = d;
         this.columns = n;
      }

      public int getColumnCount() {
         return this.columns.length;
      }

      public int getRowCount() {
         return this.data.length;
      }

      public String getColumnName(int col) {
         return this.columns[col];
      }

      public Object getValueAt(int row, int col) {
         return this.data[row][col];
      }

      public Class getColumnClass(int c) {
         return this.getValueAt(0, c).getClass();
      }

      public void setValueAt(Object value, int row, int col) {
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

   private class LabelsForSymbolTable {
      private MIPSprogram myMIPSprogram;
      private Object[][] labelData;
      private JTable labelTable;
      private ArrayList symbols;
      private SymbolTable symbolTable;
      private String tableName;

      public LabelsForSymbolTable(MIPSprogram myMIPSprogram) {
         this.myMIPSprogram = myMIPSprogram;
         this.symbolTable = myMIPSprogram == null ? Globals.symbolTable : myMIPSprogram.getLocalSymbolTable();
         this.tableName = myMIPSprogram == null ? "(global)" : (new File(myMIPSprogram.getFilename())).getName();
      }

      public String getSymbolTableName() {
         return this.tableName;
      }

      public boolean hasSymbols() {
         return this.symbolTable.getSize() != 0;
      }

      private JTable generateLabelTable() {
         SymbolTable symbolTable = this.myMIPSprogram == null ? Globals.symbolTable : this.myMIPSprogram.getLocalSymbolTable();
         int addressBase = Globals.getGui().getMainPane().getExecutePane().getAddressDisplayBase();
         if (LabelsWindow.this.textLabels.isSelected() && LabelsWindow.this.dataLabels.isSelected()) {
            this.symbols = symbolTable.getAllSymbols();
         } else if (LabelsWindow.this.textLabels.isSelected() && !LabelsWindow.this.dataLabels.isSelected()) {
            this.symbols = symbolTable.getTextSymbols();
         } else if (!LabelsWindow.this.textLabels.isSelected() && LabelsWindow.this.dataLabels.isSelected()) {
            this.symbols = symbolTable.getDataSymbols();
         } else {
            this.symbols = new ArrayList();
         }

         Collections.sort(this.symbols, LabelsWindow.this.tableSortComparator);
         this.labelData = new Object[this.symbols.size()][2];

         for(int i = 0; i < this.symbols.size(); ++i) {
            Symbol s = (Symbol)this.symbols.get(i);
            this.labelData[i][0] = s.getName();
            this.labelData[i][1] = NumberDisplayBaseChooser.formatNumber(s.getAddress(), addressBase);
         }

         LabelTableModel m = LabelsWindow.this.new LabelTableModel(this.labelData, LabelsWindow.columnNames);
         if (this.labelTable == null) {
            this.labelTable = LabelsWindow.this.new MyTippedJTable(m);
         } else {
            this.labelTable.setModel(m);
         }

         this.labelTable.getColumnModel().getColumn(1).setCellRenderer(new MonoRightCellRenderer());
         return this.labelTable;
      }

      public void updateLabelAddresses() {
         if (LabelsWindow.this.labelPanel.getComponentCount() != 0) {
            int addressBase = Globals.getGui().getMainPane().getExecutePane().getAddressDisplayBase();
            int numSymbols = this.labelData == null ? 0 : this.labelData.length;

            for(int i = 0; i < numSymbols; ++i) {
               int address = ((Symbol)this.symbols.get(i)).getAddress();
               String formattedAddress = NumberDisplayBaseChooser.formatNumber(address, addressBase);
               this.labelTable.getModel().setValueAt(formattedAddress, i, 1);
            }

         }
      }
   }

   private class MyTippedJTable extends JTable {
      MyTippedJTable(LabelTableModel m) {
         super(m);
      }

      protected JTableHeader createDefaultTableHeader() {
         return new SymbolTableHeader(this.columnModel);
      }

      public Component prepareRenderer(TableCellRenderer renderer, int rowIndex, int vColIndex) {
         Component c = super.prepareRenderer(renderer, rowIndex, vColIndex);
         if (c instanceof JComponent) {
            JComponent jc = (JComponent)c;
            jc.setToolTipText("Click on label or address to view it in Text/Data Segment");
         }

         return c;
      }

      private class SymbolTableHeader extends JTableHeader {
         public SymbolTableHeader(TableColumnModel cm) {
            super(cm);
            this.addMouseListener(new SymbolTableHeaderMouseListener((SymbolTableHeaderMouseListener)null));
         }

         public String getToolTipText(MouseEvent e) {
            Point p = e.getPoint();
            int index = this.columnModel.getColumnIndexAtX(p.x);
            int realIndex = this.columnModel.getColumn(index).getModelIndex();
            return LabelsWindow.columnToolTips[realIndex];
         }

         private class SymbolTableHeaderMouseListener implements MouseListener {
            private SymbolTableHeaderMouseListener() {
            }

            public void mouseClicked(MouseEvent e) {
               Point p = e.getPoint();
               int index = SymbolTableHeader.this.columnModel.getColumnIndexAtX(p.x);
               int realIndex = SymbolTableHeader.this.columnModel.getColumn(index).getModelIndex();
               LabelsWindow.this.sortState = LabelsWindow.sortStateTransitions[LabelsWindow.this.sortState][realIndex];
               LabelsWindow.this.tableSortComparator = LabelsWindow.this.tableSortingComparators[LabelsWindow.this.sortState];
               LabelsWindow.columnNames = LabelsWindow.sortColumnHeadings[LabelsWindow.this.sortState];
               Globals.getSettings().setLabelSortState((new Integer(LabelsWindow.this.sortState)).toString());
               LabelsWindow.this.setupTable();
               Globals.getGui().getMainPane().getExecutePane().setLabelWindowVisibility(false);
               Globals.getGui().getMainPane().getExecutePane().setLabelWindowVisibility(true);
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
            SymbolTableHeaderMouseListener(SymbolTableHeaderMouseListener var2) {
               this();
            }
         }
      }
   }
}
