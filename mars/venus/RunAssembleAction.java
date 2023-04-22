package mars.venus;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import mars.ErrorList;
import mars.ErrorMessage;
import mars.Globals;
import mars.MIPSprogram;
import mars.ProcessingException;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.Memory;
import mars.mips.hardware.RegisterFile;
import mars.util.FilenameFinder;
import mars.util.SystemIO;

public class RunAssembleAction extends GuiAction {
   private static ArrayList MIPSprogramsToAssemble;
   private static boolean extendedAssemblerEnabled;
   private static boolean warningsAreErrors;
   private static final int LINE_LENGTH_LIMIT = 60;

   public RunAssembleAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
      super(name, icon, descrip, mnemonic, accel, gui);
   }

   static ArrayList getMIPSprogramsToAssemble() {
      return MIPSprogramsToAssemble;
   }

   static boolean getExtendedAssemblerEnabled() {
      return extendedAssemblerEnabled;
   }

   static boolean getWarningsAreErrors() {
      return warningsAreErrors;
   }

   public void actionPerformed(ActionEvent e) {
      String name = this.getValue("Name").toString();
      Component editPane = this.mainUI.getMainPane().getEditPane();
      ExecutePane executePane = this.mainUI.getMainPane().getExecutePane();
      RegistersPane registersPane = this.mainUI.getRegistersPane();
      extendedAssemblerEnabled = Globals.getSettings().getExtendedAssemblerEnabled();
      warningsAreErrors = Globals.getSettings().getWarningsAreErrors();
      if (FileStatus.getFile() != null) {
         if (FileStatus.get() == 4) {
            this.mainUI.editor.save();
         }

         String exceptionHandler;
         try {
            Globals.program = new MIPSprogram();
            ArrayList filesToAssemble;
            if (Globals.getSettings().getAssembleAllEnabled()) {
               filesToAssemble = FilenameFinder.getFilenameList((new File(FileStatus.getName())).getParent(), Globals.fileExtensions);
            } else {
               filesToAssemble = new ArrayList();
               filesToAssemble.add(FileStatus.getName());
            }

            exceptionHandler = null;
            if (Globals.getSettings().getExceptionHandlerEnabled() && Globals.getSettings().getExceptionHandler() != null && Globals.getSettings().getExceptionHandler().length() > 0) {
               exceptionHandler = Globals.getSettings().getExceptionHandler();
            }

            MIPSprogramsToAssemble = Globals.program.prepareFilesForAssembly(filesToAssemble, FileStatus.getFile().getPath(), exceptionHandler);
            this.mainUI.messagesPane.postMarsMessage(this.buildFileNameList(name + ": assembling ", MIPSprogramsToAssemble));
            ErrorList warnings = Globals.program.assemble(MIPSprogramsToAssemble, extendedAssemblerEnabled, warningsAreErrors);
            if (warnings.warningsOccurred()) {
               this.mainUI.messagesPane.postMarsMessage(warnings.generateWarningReport());
            }

            this.mainUI.messagesPane.postMarsMessage(name + ": operation completed successfully.\n\n");
            FileStatus.setAssembled(true);
            FileStatus.set(5);
            RegisterFile.resetRegisters();
            Coprocessor1.resetRegisters();
            Coprocessor0.resetRegisters();
            executePane.getTextSegmentWindow().setupTable();
            executePane.getDataSegmentWindow().setupTable();
            executePane.getDataSegmentWindow().highlightCellForAddress(Memory.dataBaseAddress);
            executePane.getDataSegmentWindow().clearHighlighting();
            executePane.getLabelsWindow().setupTable();
            executePane.getTextSegmentWindow().setCodeHighlighting(true);
            executePane.getTextSegmentWindow().highlightStepAtPC();
            registersPane.getRegistersWindow().clearWindow();
            registersPane.getCoprocessor1Window().clearWindow();
            registersPane.getCoprocessor0Window().clearWindow();
            VenusUI.setReset(true);
            VenusUI.setStarted(false);
            this.mainUI.getMainPane().setSelectedComponent(executePane);
            SystemIO.resetFiles();
         } catch (ProcessingException var11) {
            exceptionHandler = var11.errors().generateErrorAndWarningReport();
            this.mainUI.messagesPane.postMarsMessage(exceptionHandler);
            this.mainUI.messagesPane.postMarsMessage(name + ": operation completed with errors.\n\n");
            ArrayList errorMessages = var11.errors().getErrorMessages();

            for(int i = 0; i < errorMessages.size(); ++i) {
               ErrorMessage em = (ErrorMessage)errorMessages.get(i);
               if ((em.getLine() != 0 || em.getPosition() != 0) && (!em.isWarning() || warningsAreErrors)) {
                  Globals.getGui().getMessagesPane().selectErrorMessage(em.getFilename(), em.getLine(), em.getPosition());
                  if (e != null) {
                     Globals.getGui().getMessagesPane().selectEditorTextLine(em.getFilename(), em.getLine(), em.getPosition());
                  }
                  break;
               }
            }

            FileStatus.setAssembled(false);
            FileStatus.set(3);
         }
      }

   }

   private String buildFileNameList(String preamble, ArrayList programList) {
      String result = preamble;
      int lineLength = preamble.length();

      for(int i = 0; i < programList.size(); ++i) {
         String filename = ((MIPSprogram)programList.get(i)).getFilename();
         result = result + filename + (i < programList.size() - 1 ? ", " : "");
         lineLength += filename.length();
         if (lineLength > 60) {
            result = result + "\n";
            lineLength = 0;
         }
      }

      return result + (lineLength == 0 ? "" : "\n") + "\n";
   }
}
