package mars.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import mars.Globals;
import mars.MIPSprogram;
import mars.ProcessingException;
import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.Memory;
import mars.mips.hardware.Register;
import mars.mips.hardware.RegisterFile;
import mars.simulator.Simulator;
import mars.util.FilenameFinder;
import mars.venus.RunSpeedPanel;

public abstract class AbstractMarsToolAndApplication extends JFrame implements MarsTool, Observer {
   protected boolean isBeingUsedAsAMarsTool = false;
   protected AbstractMarsToolAndApplication thisMarsApp;
   private JDialog dialog;
   protected Window theWindow;
   JLabel headingLabel;
   private String title;
   private String heading;
   private EmptyBorder emptyBorder = new EmptyBorder(4, 4, 4, 4);
   private Color backgroundColor;
   private int lowMemoryAddress;
   private int highMemoryAddress;
   private volatile boolean observing;
   private File mostRecentlyOpenedFile;
   private Runnable interactiveGUIUpdater;
   private MessageField operationStatusMessages;
   private JButton openFileButton;
   private JButton assembleRunButton;
   private JButton stopButton;
   private boolean multiFileAssemble;
   protected ConnectButton connectButton;

   protected AbstractMarsToolAndApplication(String title, String heading) {
      this.backgroundColor = Color.WHITE;
      this.lowMemoryAddress = Memory.dataSegmentBaseAddress;
      this.highMemoryAddress = Memory.stackBaseAddress;
      this.observing = false;
      this.mostRecentlyOpenedFile = null;
      this.interactiveGUIUpdater = new GUIUpdater();
      this.multiFileAssemble = false;
      this.thisMarsApp = this;
      this.title = title;
      this.heading = heading;
   }

   public abstract String getName();

   protected abstract JComponent buildMainDisplayArea();

   public void go() {
      this.theWindow = this;
      this.isBeingUsedAsAMarsTool = false;
      this.thisMarsApp.setTitle(this.title);
      Globals.initialize(true);
      this.thisMarsApp.addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent e) {
            AbstractMarsToolAndApplication.this.performAppClosingDuties();
         }
      });
      this.initializePreGUI();
      JPanel contentPane = new JPanel(new BorderLayout(5, 5));
      contentPane.setBorder(this.emptyBorder);
      contentPane.setOpaque(true);
      contentPane.add(this.buildHeadingArea(), "North");
      contentPane.add(this.buildMainDisplayArea(), "Center");
      contentPane.add(this.buildButtonAreaStandAlone(), "South");
      this.thisMarsApp.setContentPane(contentPane);
      this.thisMarsApp.pack();
      this.thisMarsApp.setLocationRelativeTo((Component)null);
      this.thisMarsApp.setVisible(true);
      this.initializePostGUI();
   }

   public void action() {
      this.isBeingUsedAsAMarsTool = true;
      this.dialog = new JDialog(Globals.getGui(), this.title);
      this.dialog.addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent e) {
            AbstractMarsToolAndApplication.this.performToolClosingDuties();
         }
      });
      this.theWindow = this.dialog;
      this.initializePreGUI();
      JPanel contentPane = new JPanel(new BorderLayout(5, 5));
      contentPane.setBorder(this.emptyBorder);
      contentPane.setOpaque(true);
      contentPane.add(this.buildHeadingArea(), "North");
      contentPane.add(this.buildMainDisplayArea(), "Center");
      contentPane.add(this.buildButtonAreaMarsTool(), "South");
      this.initializePostGUI();
      this.dialog.setContentPane(contentPane);
      this.dialog.pack();
      this.dialog.setLocationRelativeTo(Globals.getGui());
      this.dialog.setVisible(true);
   }

   protected void initializePreGUI() {
   }

   protected void initializePostGUI() {
   }

   protected void reset() {
   }

   protected JComponent buildHeadingArea() {
      this.headingLabel = new JLabel();
      Box headingPanel = Box.createHorizontalBox();
      headingPanel.add(Box.createHorizontalGlue());
      headingPanel.add(this.headingLabel);
      headingPanel.add(Box.createHorizontalGlue());
      this.headingLabel.setText(this.heading);
      this.headingLabel.setHorizontalTextPosition(0);
      this.headingLabel.setFont(new Font(this.headingLabel.getFont().getFontName(), 0, 18));
      return headingPanel;
   }

   protected JComponent buildButtonAreaMarsTool() {
      Box buttonArea = Box.createHorizontalBox();
      TitledBorder tc = new TitledBorder("Tool Control");
      tc.setTitleJustification(2);
      buttonArea.setBorder(tc);
      this.connectButton = new ConnectButton();
      this.connectButton.setToolTipText("Control whether tool will respond to running MIPS program");
      this.connectButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            if (AbstractMarsToolAndApplication.this.connectButton.isConnected()) {
               AbstractMarsToolAndApplication.this.connectButton.disconnect();
            } else {
               AbstractMarsToolAndApplication.this.connectButton.connect();
            }

         }
      });
      this.connectButton.addKeyListener(new EnterKeyListener(this.connectButton));
      JButton resetButton = new JButton("Reset");
      resetButton.setToolTipText("Reset all counters and other structures");
      resetButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            AbstractMarsToolAndApplication.this.reset();
         }
      });
      resetButton.addKeyListener(new EnterKeyListener(resetButton));
      JButton closeButton = new JButton("Close");
      closeButton.setToolTipText("Close (exit) this tool");
      closeButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            AbstractMarsToolAndApplication.this.performToolClosingDuties();
         }
      });
      closeButton.addKeyListener(new EnterKeyListener(closeButton));
      buttonArea.add(this.connectButton);
      buttonArea.add(Box.createHorizontalGlue());
      buttonArea.add(resetButton);
      buttonArea.add(Box.createHorizontalGlue());
      JComponent helpComponent = this.getHelpComponent();
      if (helpComponent != null) {
         buttonArea.add(helpComponent);
         buttonArea.add(Box.createHorizontalGlue());
      }

      buttonArea.add(closeButton);
      return buttonArea;
   }

   protected JComponent buildButtonAreaStandAlone() {
      Box operationArea = Box.createVerticalBox();
      Box fileControlArea = Box.createHorizontalBox();
      Box buttonArea = Box.createHorizontalBox();
      operationArea.add(fileControlArea);
      operationArea.add(Box.createVerticalStrut(5));
      operationArea.add(buttonArea);
      TitledBorder ac = new TitledBorder("Application Control");
      ac.setTitleJustification(2);
      operationArea.setBorder(ac);
      this.openFileButton = new JButton("Open MIPS program...");
      this.openFileButton.setToolTipText("Select MIPS program file to assemble and run");
      this.openFileButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            JCheckBox multiFileAssembleChoose = new JCheckBox("Assemble all in selected file's directory", AbstractMarsToolAndApplication.this.multiFileAssemble);
            multiFileAssembleChoose.setToolTipText("If checked, selected file will be assembled first and all other assembly files in directory will be assembled also.");
            fileChooser.setAccessory(multiFileAssembleChoose);
            if (AbstractMarsToolAndApplication.this.mostRecentlyOpenedFile != null) {
               fileChooser.setSelectedFile(AbstractMarsToolAndApplication.this.mostRecentlyOpenedFile);
            }

            FileFilter defaultFileFilter = FilenameFinder.getFileFilter(Globals.fileExtensions, "Assembler Files", true);
            fileChooser.addChoosableFileFilter(defaultFileFilter);
            fileChooser.addChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
            fileChooser.setFileFilter(defaultFileFilter);
            if (fileChooser.showOpenDialog(AbstractMarsToolAndApplication.this.thisMarsApp) == 0) {
               AbstractMarsToolAndApplication.this.multiFileAssemble = multiFileAssembleChoose.isSelected();
               File theFile = fileChooser.getSelectedFile();

               try {
                  theFile = theFile.getCanonicalFile();
               } catch (IOException var7) {
               }

               String currentFilePath = theFile.getPath();
               AbstractMarsToolAndApplication.this.mostRecentlyOpenedFile = theFile;
               AbstractMarsToolAndApplication.this.operationStatusMessages.setText("File: " + currentFilePath);
               AbstractMarsToolAndApplication.this.operationStatusMessages.setCaretPosition(0);
               AbstractMarsToolAndApplication.this.assembleRunButton.setEnabled(true);
            }

         }
      });
      this.openFileButton.addKeyListener(new EnterKeyListener(this.openFileButton));
      this.operationStatusMessages = new MessageField("No file open.");
      this.operationStatusMessages.setColumns(40);
      this.operationStatusMessages.setMargin(new Insets(0, 3, 0, 3));
      this.operationStatusMessages.setBackground(this.backgroundColor);
      this.operationStatusMessages.setFocusable(false);
      this.operationStatusMessages.setToolTipText("Display operation status messages");
      RunSpeedPanel speed = RunSpeedPanel.getInstance();
      this.assembleRunButton = new JButton("Assemble and Run");
      this.assembleRunButton.setToolTipText("Assemble and run the currently selected MIPS program");
      this.assembleRunButton.setEnabled(false);
      this.assembleRunButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            AbstractMarsToolAndApplication.this.assembleRunButton.setEnabled(false);
            AbstractMarsToolAndApplication.this.openFileButton.setEnabled(false);
            AbstractMarsToolAndApplication.this.stopButton.setEnabled(true);
            (new Thread(AbstractMarsToolAndApplication.this.new CreateAssembleRunMIPSprogram())).start();
         }
      });
      this.assembleRunButton.addKeyListener(new EnterKeyListener(this.assembleRunButton));
      this.stopButton = new JButton("Stop");
      this.stopButton.setToolTipText("Terminate MIPS program execution");
      this.stopButton.setEnabled(false);
      this.stopButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            Simulator.getInstance().stopExecution((AbstractAction)null);
         }
      });
      this.stopButton.addKeyListener(new EnterKeyListener(this.stopButton));
      JButton resetButton = new JButton("Reset");
      resetButton.setToolTipText("Reset all counters and other structures");
      resetButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            AbstractMarsToolAndApplication.this.reset();
         }
      });
      resetButton.addKeyListener(new EnterKeyListener(resetButton));
      JButton closeButton = new JButton("Exit");
      closeButton.setToolTipText("Exit this application");
      closeButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            AbstractMarsToolAndApplication.this.performAppClosingDuties();
         }
      });
      closeButton.addKeyListener(new EnterKeyListener(closeButton));
      Box fileDisplayBox = Box.createVerticalBox();
      fileDisplayBox.add(Box.createVerticalStrut(8));
      fileDisplayBox.add(this.operationStatusMessages);
      fileDisplayBox.add(Box.createVerticalStrut(8));
      fileControlArea.add(fileDisplayBox);
      fileControlArea.add(Box.createHorizontalGlue());
      fileControlArea.add(speed);
      buttonArea.add(this.openFileButton);
      buttonArea.add(Box.createHorizontalGlue());
      buttonArea.add(this.assembleRunButton);
      buttonArea.add(Box.createHorizontalGlue());
      buttonArea.add(this.stopButton);
      buttonArea.add(Box.createHorizontalGlue());
      buttonArea.add(resetButton);
      buttonArea.add(Box.createHorizontalGlue());
      JComponent helpComponent = this.getHelpComponent();
      if (helpComponent != null) {
         buttonArea.add(helpComponent);
         buttonArea.add(Box.createHorizontalGlue());
      }

      buttonArea.add(closeButton);
      return operationArea;
   }

   public void update(Observable resource, Object accessNotice) {
      if (((AccessNotice)accessNotice).accessIsFromMIPS()) {
         this.processMIPSUpdate(resource, (AccessNotice)accessNotice);
         this.updateDisplay();
      }

   }

   protected void processMIPSUpdate(Observable resource, AccessNotice notice) {
   }

   protected void performSpecialClosingDuties() {
   }

   protected void addAsObserver() {
      this.addAsObserver(this.lowMemoryAddress, this.highMemoryAddress);
   }

   protected void addAsObserver(int lowEnd, int highEnd) {
      String errorMessage = "Error connecting to MIPS memory";

      try {
         Globals.memory.addObserver(this.thisMarsApp, lowEnd, highEnd);
      } catch (AddressErrorException var5) {
         if (this.isBeingUsedAsAMarsTool) {
            this.headingLabel.setText(errorMessage);
         } else {
            this.operationStatusMessages.displayTerminatingMessage(errorMessage);
         }
      }

   }

   protected void addAsObserver(Register reg) {
      if (reg != null) {
         reg.addObserver(this.thisMarsApp);
      }

   }

   protected void deleteAsObserver() {
      Globals.memory.deleteObserver(this.thisMarsApp);
   }

   protected void deleteAsObserver(Register reg) {
      if (reg != null) {
         reg.deleteObserver(this.thisMarsApp);
      }

   }

   protected boolean isObserving() {
      return this.observing;
   }

   protected void updateDisplay() {
   }

   protected JComponent getHelpComponent() {
      return null;
   }

   private void performToolClosingDuties() {
      this.performSpecialClosingDuties();
      if (this.connectButton.isConnected()) {
         this.connectButton.disconnect();
      }

      this.dialog.setVisible(false);
      this.dialog.dispose();
   }

   private void performAppClosingDuties() {
      this.performSpecialClosingDuties();
      this.thisMarsApp.setVisible(false);
      System.exit(0);
   }

   private class GUIUpdater implements Runnable {
      private GUIUpdater() {
      }

      public void run() {
         AbstractMarsToolAndApplication.this.updateDisplay();
      }

      // $FF: synthetic method
      GUIUpdater(Object x1) {
         this();
      }
   }

   private class MessageField extends JTextField {
      public MessageField(String text) {
         super(text);
      }

      private void displayTerminatingMessage(String text) {
         this.displayMessage(text, true);
      }

      private void displayNonTerminatingMessage(String text) {
         this.displayMessage(text, false);
      }

      private void displayMessage(String text, boolean terminating) {
         SwingUtilities.invokeLater(new MessageWriter(text, terminating));
      }

      private class MessageWriter implements Runnable {
         private String text;
         private boolean terminatingMessage;

         public MessageWriter(String text, boolean terminating) {
            this.text = text;
            this.terminatingMessage = terminating;
         }

         public void run() {
            if (this.text != null) {
               AbstractMarsToolAndApplication.this.operationStatusMessages.setText(this.text);
               AbstractMarsToolAndApplication.this.operationStatusMessages.setCaretPosition(0);
            }

            if (this.terminatingMessage) {
               AbstractMarsToolAndApplication.this.assembleRunButton.setEnabled(true);
               AbstractMarsToolAndApplication.this.openFileButton.setEnabled(true);
               AbstractMarsToolAndApplication.this.stopButton.setEnabled(false);
            }

         }
      }
   }

   private class CreateAssembleRunMIPSprogram implements Runnable {
      private CreateAssembleRunMIPSprogram() {
      }

      public void run() {
         String noSupportForExceptionHandler = null;
         String exceptionHandler = null;
         if (Globals.getSettings().getExceptionHandlerEnabled() && Globals.getSettings().getExceptionHandler() != null && Globals.getSettings().getExceptionHandler().length() > 0) {
            exceptionHandler = Globals.getSettings().getExceptionHandler();
         }

         Thread.currentThread().setPriority(4);
         Thread.yield();
         MIPSprogram program = new MIPSprogram();
         Globals.program = program;
         String fileToAssemble = AbstractMarsToolAndApplication.this.mostRecentlyOpenedFile.getPath();
         ArrayList filesToAssemble = null;
         if (AbstractMarsToolAndApplication.this.multiFileAssemble) {
            filesToAssemble = FilenameFinder.getFilenameList((new File(fileToAssemble)).getParent(), Globals.fileExtensions);
         } else {
            filesToAssemble = new ArrayList();
            filesToAssemble.add(fileToAssemble);
         }

         ArrayList programsToAssemble = null;

         try {
            AbstractMarsToolAndApplication.this.operationStatusMessages.displayNonTerminatingMessage("Assembling " + fileToAssemble);
            programsToAssemble = program.prepareFilesForAssembly(filesToAssemble, fileToAssemble, exceptionHandler);
         } catch (ProcessingException var19) {
            AbstractMarsToolAndApplication.this.operationStatusMessages.displayTerminatingMessage("Error reading file(s): " + fileToAssemble);
            return;
         }

         try {
            program.assemble(programsToAssemble, Globals.getSettings().getExtendedAssemblerEnabled(), Globals.getSettings().getWarningsAreErrors());
         } catch (ProcessingException var18) {
            AbstractMarsToolAndApplication.this.operationStatusMessages.displayTerminatingMessage("Assembly Error: " + fileToAssemble);
            return;
         }

         RegisterFile.resetRegisters();
         Coprocessor1.resetRegisters();
         Coprocessor0.resetRegisters();
         AbstractMarsToolAndApplication.this.addAsObserver();
         AbstractMarsToolAndApplication.this.observing = true;
         String terminatingMessage = "Normal termination: ";

         try {
            AbstractMarsToolAndApplication.this.operationStatusMessages.displayNonTerminatingMessage("Running " + fileToAssemble);
            program.simulate(-1);
         } catch (NullPointerException var15) {
            terminatingMessage = "User interrupt: ";
         } catch (ProcessingException var16) {
            terminatingMessage = "Runtime error: ";
         } finally {
            AbstractMarsToolAndApplication.this.deleteAsObserver();
            AbstractMarsToolAndApplication.this.observing = false;
            AbstractMarsToolAndApplication.this.operationStatusMessages.displayTerminatingMessage(terminatingMessage + fileToAssemble);
         }

      }

      // $FF: synthetic method
      CreateAssembleRunMIPSprogram(Object x1) {
         this();
      }
   }

   protected class EnterKeyListener extends KeyAdapter {
      AbstractButton myButton;

      public EnterKeyListener(AbstractButton who) {
         this.myButton = who;
      }

      public void keyPressed(KeyEvent e) {
         if (e.getKeyChar() == '\n') {
            e.consume();

            try {
               this.myButton.getActionListeners()[0].actionPerformed(new ActionEvent(this.myButton, 0, this.myButton.getText()));
            } catch (ArrayIndexOutOfBoundsException var3) {
            }
         }

      }
   }

   protected class ConnectButton extends JButton {
      private static final String connectText = "Connect to MIPS";
      private static final String disconnectText = "Disconnect from MIPS";

      public ConnectButton() {
         this.disconnect();
      }

      public void connect() {
         AbstractMarsToolAndApplication.this.observing = true;
         synchronized(Globals.memoryAndRegistersLock) {
            AbstractMarsToolAndApplication.this.addAsObserver();
         }

         this.setText("Disconnect from MIPS");
      }

      public void disconnect() {
         synchronized(Globals.memoryAndRegistersLock) {
            AbstractMarsToolAndApplication.this.deleteAsObserver();
         }

         AbstractMarsToolAndApplication.this.observing = false;
         this.setText("Connect to MIPS");
      }

      public boolean isConnected() {
         return AbstractMarsToolAndApplication.this.observing;
      }
   }
}
