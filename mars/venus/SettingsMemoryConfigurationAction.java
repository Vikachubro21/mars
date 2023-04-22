package mars.venus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import mars.Globals;
import mars.mips.hardware.MemoryConfiguration;
import mars.mips.hardware.MemoryConfigurations;
import mars.simulator.Simulator;
import mars.util.Binary;

public class SettingsMemoryConfigurationAction extends GuiAction {
   JDialog configDialog;
   JComboBox fontFamilySelector;
   JComboBox fontStyleSelector;
   JSlider fontSizeSelector;
   JTextField fontSizeDisplay;
   SettingsMemoryConfigurationAction thisAction = this;
   String initialFontFamily;
   String initialFontStyle;
   String initialFontSize;

   public SettingsMemoryConfigurationAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
      super(name, icon, descrip, mnemonic, accel, gui);
   }

   public void actionPerformed(ActionEvent e) {
      this.configDialog = new MemoryConfigurationDialog(Globals.getGui(), "MIPS Memory Configuration", true);
      this.configDialog.setVisible(true);
   }

   private class ConfigurationButton extends JRadioButton {
      private MemoryConfiguration configuration;

      public ConfigurationButton(MemoryConfiguration config) {
         super(config.getConfigurationName(), config == MemoryConfigurations.getCurrentConfiguration());
         this.configuration = config;
      }

      public MemoryConfiguration getConfiguration() {
         return this.configuration;
      }
   }

   private class MemoryConfigurationDialog extends JDialog implements ActionListener {
      JTextField[] addressDisplay;
      JLabel[] nameDisplay;
      ConfigurationButton selectedConfigurationButton;
      ConfigurationButton initialConfigurationButton;

      public MemoryConfigurationDialog(Frame owner, String title, boolean modality) {
         super(owner, title, modality);
         this.setContentPane(this.buildDialogPanel());
         this.setDefaultCloseOperation(0);
         this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
               MemoryConfigurationDialog.this.performClose();
            }
         });
         this.pack();
         this.setLocationRelativeTo(owner);
      }

      private JPanel buildDialogPanel() {
         JPanel dialogPanel = new JPanel(new BorderLayout());
         dialogPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
         JPanel configInfo = new JPanel(new FlowLayout());
         MemoryConfigurations.buildConfigurationCollection();
         configInfo.add(this.buildConfigChooser());
         configInfo.add(this.buildConfigDisplay());
         dialogPanel.add(configInfo);
         dialogPanel.add(this.buildControlPanel(), "South");
         return dialogPanel;
      }

      private Component buildConfigChooser() {
         JPanel chooserPanel = new JPanel(new GridLayout(4, 1));
         ButtonGroup choices = new ButtonGroup();
         Iterator configurationsIterator = MemoryConfigurations.getConfigurationsIterator();

         while(configurationsIterator.hasNext()) {
            MemoryConfiguration config = (MemoryConfiguration)configurationsIterator.next();
            ConfigurationButton button = SettingsMemoryConfigurationAction.this.new ConfigurationButton(config);
            button.addActionListener(this);
            if (button.isSelected()) {
               this.selectedConfigurationButton = button;
               this.initialConfigurationButton = button;
            }

            choices.add(button);
            chooserPanel.add(button);
         }

         chooserPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Configuration"));
         return chooserPanel;
      }

      private Component buildConfigDisplay() {
         JPanel displayPanel = new JPanel();
         MemoryConfiguration config = MemoryConfigurations.getCurrentConfiguration();
         String[] configurationItemNames = config.getConfigurationItemNames();
         int numItems = configurationItemNames.length;
         JPanel namesPanel = new JPanel(new GridLayout(numItems, 1));
         JPanel valuesPanel = new JPanel(new GridLayout(numItems, 1));
         Font monospaced = new Font("Lucida Sans Typewriter", 0, 12);
         this.nameDisplay = new JLabel[numItems];
         this.addressDisplay = new JTextField[numItems];

         int i;
         for(i = 0; i < numItems; ++i) {
            this.nameDisplay[i] = new JLabel();
            this.addressDisplay[i] = new JTextField();
            this.addressDisplay[i].setEditable(false);
            this.addressDisplay[i].setFont(monospaced);
         }

         for(i = this.addressDisplay.length - 1; i >= 0; --i) {
            namesPanel.add(this.nameDisplay[i]);
            valuesPanel.add(this.addressDisplay[i]);
         }

         this.setConfigDisplay(config);
         Box columns = Box.createHorizontalBox();
         columns.add(valuesPanel);
         columns.add(Box.createHorizontalStrut(6));
         columns.add(namesPanel);
         displayPanel.add(columns);
         return displayPanel;
      }

      public void actionPerformed(ActionEvent e) {
         MemoryConfiguration config = ((ConfigurationButton)e.getSource()).getConfiguration();
         this.setConfigDisplay(config);
         this.selectedConfigurationButton = (ConfigurationButton)e.getSource();
      }

      private Component buildControlPanel() {
         Box controlPanel = Box.createHorizontalBox();
         JButton okButton = new JButton("Apply and Close");
         okButton.setToolTipText("Apply current settings and close dialog");
         okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               MemoryConfigurationDialog.this.performApply();
               MemoryConfigurationDialog.this.performClose();
            }
         });
         JButton applyButton = new JButton("Apply");
         applyButton.setToolTipText("Apply current settings now and leave dialog open");
         applyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               MemoryConfigurationDialog.this.performApply();
            }
         });
         JButton cancelButton = new JButton("Cancel");
         cancelButton.setToolTipText("Close dialog without applying current settings");
         cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               MemoryConfigurationDialog.this.performClose();
            }
         });
         JButton resetButton = new JButton("Reset");
         resetButton.setToolTipText("Reset to initial settings without applying");
         resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               MemoryConfigurationDialog.this.performReset();
            }
         });
         controlPanel.add(Box.createHorizontalGlue());
         controlPanel.add(okButton);
         controlPanel.add(Box.createHorizontalGlue());
         controlPanel.add(applyButton);
         controlPanel.add(Box.createHorizontalGlue());
         controlPanel.add(cancelButton);
         controlPanel.add(Box.createHorizontalGlue());
         controlPanel.add(resetButton);
         controlPanel.add(Box.createHorizontalGlue());
         return controlPanel;
      }

      private void performApply() {
         if (MemoryConfigurations.setCurrentConfiguration(this.selectedConfigurationButton.getConfiguration())) {
            Globals.getSettings().setMemoryConfiguration(this.selectedConfigurationButton.getConfiguration().getConfigurationIdentifier());
            Globals.getGui().getRegistersPane().getRegistersWindow().clearHighlighting();
            Globals.getGui().getRegistersPane().getRegistersWindow().updateRegisters();
            Globals.getGui().getMainPane().getExecutePane().getDataSegmentWindow().updateBaseAddressComboBox();
            if (FileStatus.get() == 5 || FileStatus.get() == 6 || FileStatus.get() == 7) {
               if (FileStatus.get() == 6) {
                  Simulator.getInstance().stopExecution(SettingsMemoryConfigurationAction.this.thisAction);
               }

               Globals.getGui().getRunAssembleAction().actionPerformed((ActionEvent)null);
            }
         }

      }

      private void performClose() {
         this.setVisible(false);
         this.dispose();
      }

      private void performReset() {
         this.selectedConfigurationButton = this.initialConfigurationButton;
         this.selectedConfigurationButton.setSelected(true);
         this.setConfigDisplay(this.selectedConfigurationButton.getConfiguration());
      }

      private void setConfigDisplay(MemoryConfiguration config) {
         String[] configurationItemNames = config.getConfigurationItemNames();
         int[] configurationItemValues = config.getConfigurationItemValues();
         TreeMap treeSortedByAddress = new TreeMap();

         for(int ix = 0; ix < configurationItemValues.length; ++ix) {
            treeSortedByAddress.put(Binary.intToHexString(configurationItemValues[ix]) + configurationItemNames[ix], configurationItemNames[ix]);
         }

         Iterator setSortedByAddress = treeSortedByAddress.entrySet().iterator();
         int addressStringLength = Binary.intToHexString(configurationItemValues[0]).length();

         for(int i = 0; i < configurationItemValues.length; ++i) {
            Map.Entry pair = (Map.Entry)setSortedByAddress.next();
            this.nameDisplay[i].setText((String)pair.getValue());
            this.addressDisplay[i].setText(((String)pair.getKey()).substring(0, addressStringLength));
         }

      }
   }
}
