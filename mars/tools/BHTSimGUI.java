package mars.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.util.Vector;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;

public class BHTSimGUI extends JPanel {
   private JTextField m_tfInstruction;
   private JTextField m_tfAddress;
   private JTextField m_tfIndex;
   private JComboBox m_cbBHTentries;
   private JComboBox m_cbBHThistory;
   private JComboBox m_cbBHTinitVal;
   private JTable m_tabBHT;
   private JTextArea m_taLog;
   public static final Color COLOR_PREPREDICTION;
   public static final Color COLOR_PREDICTION_CORRECT;
   public static final Color COLOR_PREDICTION_INCORRECT;
   public static final String BHT_TAKE_BRANCH = "TAKE";
   public static final String BHT_DO_NOT_TAKE_BRANCH = "NOT TAKE";

   public BHTSimGUI() {
      BorderLayout layout = new BorderLayout();
      layout.setVgap(10);
      layout.setHgap(10);
      this.setLayout(layout);
      this.m_tabBHT = this.createAndInitTable();
      this.add(this.buildConfigPanel(), "North");
      this.add(this.buildInfoPanel(), "West");
      this.add(new JScrollPane(this.m_tabBHT), "Center");
      this.add(this.buildLogPanel(), "South");
   }

   private JTable createAndInitTable() {
      JTable theTable = new JTable();
      DefaultTableCellRenderer doubleRenderer = new DefaultTableCellRenderer() {
         private DecimalFormat formatter = new DecimalFormat("##0.00");

         public void setValue(Object value) {
            this.setText(value == null ? "" : this.formatter.format(value));
         }
      };
      doubleRenderer.setHorizontalAlignment(0);
      DefaultTableCellRenderer defRenderer = new DefaultTableCellRenderer();
      defRenderer.setHorizontalAlignment(0);
      theTable.setDefaultRenderer(Double.class, doubleRenderer);
      theTable.setDefaultRenderer(Integer.class, defRenderer);
      theTable.setDefaultRenderer(String.class, defRenderer);
      theTable.setSelectionBackground(COLOR_PREPREDICTION);
      theTable.setSelectionMode(1);
      return theTable;
   }

   private JPanel buildInfoPanel() {
      this.m_tfInstruction = new JTextField();
      this.m_tfAddress = new JTextField();
      this.m_tfIndex = new JTextField();
      this.m_tfInstruction.setColumns(10);
      this.m_tfInstruction.setEditable(false);
      this.m_tfInstruction.setHorizontalAlignment(0);
      this.m_tfAddress.setColumns(10);
      this.m_tfAddress.setEditable(false);
      this.m_tfAddress.setHorizontalAlignment(0);
      this.m_tfIndex.setColumns(10);
      this.m_tfIndex.setEditable(false);
      this.m_tfIndex.setHorizontalAlignment(0);
      JPanel panel = new JPanel();
      JPanel outerPanel = new JPanel();
      outerPanel.setLayout(new BorderLayout());
      GridBagLayout gbl = new GridBagLayout();
      panel.setLayout(gbl);
      GridBagConstraints c = new GridBagConstraints();
      c.insets = new Insets(5, 5, 2, 5);
      c.gridx = 1;
      c.gridy = 1;
      panel.add(new JLabel("Instruction"), c);
      ++c.gridy;
      panel.add(this.m_tfInstruction, c);
      ++c.gridy;
      panel.add(new JLabel("@ Address"), c);
      ++c.gridy;
      panel.add(this.m_tfAddress, c);
      ++c.gridy;
      panel.add(new JLabel("-> Index"), c);
      ++c.gridy;
      panel.add(this.m_tfIndex, c);
      outerPanel.add(panel, "North");
      return outerPanel;
   }

   private JPanel buildConfigPanel() {
      JPanel panel = new JPanel();
      Vector sizes = new Vector();
      sizes.add(new Integer(8));
      sizes.add(new Integer(16));
      sizes.add(new Integer(32));
      Vector bits = new Vector();
      bits.add(new Integer(1));
      bits.add(new Integer(2));
      Vector initVals = new Vector();
      initVals.add("NOT TAKE");
      initVals.add("TAKE");
      this.m_cbBHTentries = new JComboBox(sizes);
      this.m_cbBHThistory = new JComboBox(bits);
      this.m_cbBHTinitVal = new JComboBox(initVals);
      panel.add(new JLabel("# of BHT entries"));
      panel.add(this.m_cbBHTentries);
      panel.add(new JLabel("BHT history size"));
      panel.add(this.m_cbBHThistory);
      panel.add(new JLabel("Initial value"));
      panel.add(this.m_cbBHTinitVal);
      return panel;
   }

   private JPanel buildLogPanel() {
      JPanel panel = new JPanel();
      panel.setLayout(new BorderLayout());
      this.m_taLog = new JTextArea();
      this.m_taLog.setRows(6);
      this.m_taLog.setEditable(false);
      panel.add(new JLabel("Log"), "North");
      panel.add(new JScrollPane(this.m_taLog), "Center");
      return panel;
   }

   public JComboBox getCbBHTentries() {
      return this.m_cbBHTentries;
   }

   public JComboBox getCbBHThistory() {
      return this.m_cbBHThistory;
   }

   public JComboBox getCbBHTinitVal() {
      return this.m_cbBHTinitVal;
   }

   public JTable getTabBHT() {
      return this.m_tabBHT;
   }

   public JTextArea getTaLog() {
      return this.m_taLog;
   }

   public JTextField getTfInstruction() {
      return this.m_tfInstruction;
   }

   public JTextField getTfAddress() {
      return this.m_tfAddress;
   }

   public JTextField getTfIndex() {
      return this.m_tfIndex;
   }

   static {
      COLOR_PREPREDICTION = Color.yellow;
      COLOR_PREDICTION_CORRECT = Color.green;
      COLOR_PREDICTION_INCORRECT = Color.red;
   }
}
