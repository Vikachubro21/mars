package mars.tools;

import java.util.Vector;
import javax.swing.table.AbstractTableModel;

public class BHTableModel extends AbstractTableModel {
   private Vector m_entries;
   private int m_entryCnt;
   private int m_historySize;
   private String[] m_columnNames = new String[]{"Index", "History", "Prediction", "Correct", "Incorrect", "Precision"};
   private Class[] m_columnClasses = new Class[]{Integer.class, String.class, String.class, Integer.class, Integer.class, Double.class};

   public BHTableModel(int numEntries, int historySize, boolean initVal) {
      this.initBHT(numEntries, historySize, initVal);
   }

   public String getColumnName(int i) {
      if (i >= 0 && i <= this.m_columnNames.length) {
         return this.m_columnNames[i];
      } else {
         throw new IllegalArgumentException("Illegal column index " + i + " (must be in range 0.." + (this.m_columnNames.length - 1) + ")");
      }
   }

   public Class getColumnClass(int i) {
      if (i >= 0 && i <= this.m_columnClasses.length) {
         return this.m_columnClasses[i];
      } else {
         throw new IllegalArgumentException("Illegal column index " + i + " (must be in range 0.." + (this.m_columnClasses.length - 1) + ")");
      }
   }

   public int getColumnCount() {
      return 6;
   }

   public int getRowCount() {
      return this.m_entryCnt;
   }

   public Object getValueAt(int row, int col) {
      BHTEntry e = (BHTEntry)this.m_entries.elementAt(row);
      if (e == null) {
         return "";
      } else if (col == 0) {
         return new Integer(row);
      } else if (col == 1) {
         return e.getHistoryAsStr();
      } else if (col == 2) {
         return e.getPredictionAsStr();
      } else if (col == 3) {
         return new Integer(e.getStatsPredCorrect());
      } else if (col == 4) {
         return new Integer(e.getStatsPredIncorrect());
      } else {
         return col == 5 ? new Double(e.getStatsPredPrecision()) : "";
      }
   }

   public void initBHT(int numEntries, int historySize, boolean initVal) {
      if (numEntries > 0 && (numEntries & numEntries - 1) == 0) {
         if (historySize >= 1 && historySize <= 2) {
            this.m_entryCnt = numEntries;
            this.m_historySize = historySize;
            this.m_entries = new Vector();

            for(int i = 0; i < this.m_entryCnt; ++i) {
               this.m_entries.add(new BHTEntry(this.m_historySize, initVal));
            }

            this.fireTableStructureChanged();
         } else {
            throw new IllegalArgumentException("Only history sizes of 1 or 2 supported.");
         }
      } else {
         throw new IllegalArgumentException("Number of entries must be a positive power of 2.");
      }
   }

   public int getIdxForAddress(int address) {
      if (address < 0) {
         throw new IllegalArgumentException("No negative addresses supported");
      } else {
         return (address >> 2) % this.m_entryCnt;
      }
   }

   public boolean getPredictionAtIdx(int index) {
      if (index >= 0 && index <= this.m_entryCnt) {
         return ((BHTEntry)this.m_entries.elementAt(index)).getPrediction();
      } else {
         throw new IllegalArgumentException("Only indexes in the range 0 to " + (this.m_entryCnt - 1) + " allowed");
      }
   }

   public void updatePredictionAtIdx(int index, boolean branchTaken) {
      if (index >= 0 && index <= this.m_entryCnt) {
         ((BHTEntry)this.m_entries.elementAt(index)).updatePrediction(branchTaken);
         this.fireTableRowsUpdated(index, index);
      } else {
         throw new IllegalArgumentException("Only indexes in the range 0 to " + (this.m_entryCnt - 1) + " allowed");
      }
   }
}
