package mars.venus;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import mars.Globals;
import mars.mips.dump.DumpFormat;
import mars.mips.dump.DumpFormatLoader;
import mars.mips.hardware.AddressErrorException;
import mars.util.Binary;
import mars.util.MemoryDump;

public class FileDumpMemoryAction extends GuiAction {
   private JDialog dumpDialog;
   private static final String title = "Dump Memory To File";
   private String[] segmentArray;
   private int[] baseAddressArray;
   private int[] limitAddressArray;
   private int[] highAddressArray;
   private String[] segmentListArray;
   private int[] segmentListBaseArray;
   private int[] segmentListHighArray;
   private JComboBox segmentListSelector;
   private JComboBox formatListSelector;

   public FileDumpMemoryAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
      super(name, icon, descrip, mnemonic, accel, gui);
   }

   public void actionPerformed(ActionEvent e) {
      this.dumpMemory();
   }

   private boolean dumpMemory() {
      this.dumpDialog = this.createDumpDialog();
      this.dumpDialog.pack();
      this.dumpDialog.setLocationRelativeTo(Globals.getGui());
      this.dumpDialog.setVisible(true);
      return true;
   }

   private JDialog createDumpDialog() {
      JDialog dumpDialog = new JDialog(Globals.getGui(), "Dump Memory To File", true);
      dumpDialog.setContentPane(this.buildDialogPanel());
      dumpDialog.setDefaultCloseOperation(0);
      dumpDialog.addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent we) {
            FileDumpMemoryAction.this.closeDialog();
         }
      });
      return dumpDialog;
   }

   private JPanel buildDialogPanel() {
      JPanel contents = new JPanel(new BorderLayout(20, 20));
      contents.setBorder(new EmptyBorder(10, 10, 10, 10));
      this.segmentArray = MemoryDump.getSegmentNames();
      this.baseAddressArray = MemoryDump.getBaseAddresses(this.segmentArray);
      this.limitAddressArray = MemoryDump.getLimitAddresses(this.segmentArray);
      this.highAddressArray = new int[this.segmentArray.length];
      this.segmentListArray = new String[this.segmentArray.length];
      this.segmentListBaseArray = new int[this.segmentArray.length];
      this.segmentListHighArray = new int[this.segmentArray.length];
      int segmentCount = 0;

      for(int i = 0; i < this.segmentArray.length; ++i) {
         try {
            this.highAddressArray[i] = Globals.memory.getAddressOfFirstNull(this.baseAddressArray[i], this.limitAddressArray[i]) - 4;
         } catch (AddressErrorException var9) {
            this.highAddressArray[i] = this.baseAddressArray[i] - 4;
         }

         if (this.highAddressArray[i] >= this.baseAddressArray[i]) {
            this.segmentListBaseArray[segmentCount] = this.baseAddressArray[i];
            this.segmentListHighArray[segmentCount] = this.highAddressArray[i];
            this.segmentListArray[segmentCount] = this.segmentArray[i] + " (" + Binary.intToHexString(this.baseAddressArray[i]) + " - " + Binary.intToHexString(this.highAddressArray[i]) + ")";
            ++segmentCount;
         }
      }

      if (segmentCount == 0) {
         contents.add(new Label("There is nothing to dump!"), "North");
         JButton OKButton = new JButton("OK");
         OKButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               FileDumpMemoryAction.this.closeDialog();
            }
         });
         contents.add(OKButton, "South");
         return contents;
      } else {
         if (segmentCount < this.segmentListArray.length) {
            String[] tempArray = new String[segmentCount];
            System.arraycopy(this.segmentListArray, 0, tempArray, 0, segmentCount);
            this.segmentListArray = tempArray;
         }

         this.segmentListSelector = new JComboBox(this.segmentListArray);
         this.segmentListSelector.setSelectedIndex(0);
         JPanel segmentPanel = new JPanel(new BorderLayout());
         segmentPanel.add(new Label("Memory Segment"), "North");
         segmentPanel.add(this.segmentListSelector);
         contents.add(segmentPanel, "West");
         ArrayList dumpFormats = (new DumpFormatLoader()).loadDumpFormats();
         this.formatListSelector = new JComboBox(dumpFormats.toArray());
         this.formatListSelector.setRenderer(new DumpFormatComboBoxRenderer(this.formatListSelector));
         this.formatListSelector.setSelectedIndex(0);
         JPanel formatPanel = new JPanel(new BorderLayout());
         formatPanel.add(new Label("Dump Format"), "North");
         formatPanel.add(this.formatListSelector);
         contents.add(formatPanel, "East");
         Box controlPanel = Box.createHorizontalBox();
         JButton dumpButton = new JButton("Dump To File...");
         dumpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               if (FileDumpMemoryAction.this.performDump(FileDumpMemoryAction.this.segmentListBaseArray[FileDumpMemoryAction.this.segmentListSelector.getSelectedIndex()], FileDumpMemoryAction.this.segmentListHighArray[FileDumpMemoryAction.this.segmentListSelector.getSelectedIndex()], (DumpFormat)FileDumpMemoryAction.this.formatListSelector.getSelectedItem())) {
                  FileDumpMemoryAction.this.closeDialog();
               }

            }
         });
         JButton cancelButton = new JButton("Cancel");
         cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               FileDumpMemoryAction.this.closeDialog();
            }
         });
         controlPanel.add(Box.createHorizontalGlue());
         controlPanel.add(dumpButton);
         controlPanel.add(Box.createHorizontalGlue());
         controlPanel.add(cancelButton);
         controlPanel.add(Box.createHorizontalGlue());
         contents.add(controlPanel, "South");
         return contents;
      }
   }

   private boolean performDump(int firstAddress, int lastAddress, DumpFormat format) {
      File theFile = null;
      JFileChooser saveDialog = null;
      boolean operationOK = false;
      saveDialog = new JFileChooser(this.mainUI.getEditor().getCurrentSaveDirectory());
      saveDialog.setDialogTitle("Dump Memory To File");

      while(!operationOK) {
         int decision = saveDialog.showSaveDialog(this.mainUI);
         if (decision != 0) {
            return false;
         }

         theFile = saveDialog.getSelectedFile();
         operationOK = true;
         if (theFile.exists()) {
            int overwrite = JOptionPane.showConfirmDialog(this.mainUI, "File " + theFile.getName() + " already exists.  Do you wish to overwrite it?", "Overwrite existing file?", 1, 2);
            switch (overwrite) {
               case 0:
                  operationOK = true;
                  break;
               case 1:
                  operationOK = false;
                  break;
               case 2:
                  return false;
               default:
                  return false;
            }
         }

         if (operationOK) {
            try {
               format.dumpMemoryRange(theFile, firstAddress, lastAddress);
            } catch (AddressErrorException var9) {
            } catch (IOException var10) {
            }
         }
      }

      return true;
   }

   private void closeDialog() {
      this.dumpDialog.setVisible(false);
      this.dumpDialog.dispose();
   }

   private class DumpFormatComboBoxRenderer extends BasicComboBoxRenderer {
      private JComboBox myMaster;

      public DumpFormatComboBoxRenderer(JComboBox myMaster) {
         this.myMaster = myMaster;
      }

      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
         super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
         this.setToolTipText(value.toString());
         if (index >= 0 && ((DumpFormat)this.myMaster.getItemAt(index)).getDescription() != null) {
            this.setToolTipText(((DumpFormat)this.myMaster.getItemAt(index)).getDescription());
         }

         return this;
      }
   }
}
