package mars.venus;

import java.awt.event.ActionEvent;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import mars.Globals;
import mars.ProcessingException;
import mars.mips.hardware.RegisterFile;
import mars.simulator.ProgramArgumentList;
import mars.util.SystemIO;

public class RunGoAction extends GuiAction {
   public static int defaultMaxSteps = -1;
   public static int maxSteps;
   private String name;
   private ExecutePane executePane;

   static {
      maxSteps = defaultMaxSteps;
   }

   public RunGoAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
      super(name, icon, descrip, mnemonic, accel, gui);
   }

   public void actionPerformed(ActionEvent e) {
      this.name = this.getValue("Name").toString();
      this.executePane = this.mainUI.getMainPane().getExecutePane();
      if (FileStatus.isAssembled()) {
         if (!VenusUI.getStarted()) {
            this.processProgramArgumentsIfAny();
         }

         if (!VenusUI.getReset() && !VenusUI.getStarted()) {
            JOptionPane.showMessageDialog(this.mainUI, "reset " + VenusUI.getReset() + " started " + VenusUI.getStarted());
         } else {
            VenusUI.setStarted(true);
            this.mainUI.messagesPane.postMarsMessage(this.name + ": running " + FileStatus.getFile().getName() + "\n\n");
            this.mainUI.getMessagesPane().selectRunMessageTab();
            this.executePane.getTextSegmentWindow().setCodeHighlighting(false);
            this.executePane.getTextSegmentWindow().unhighlightAllSteps();
            this.mainUI.setMenuState(6);

            try {
               int[] breakPoints = this.executePane.getTextSegmentWindow().getSortedBreakPointsArray();
               Globals.program.simulateFromPC(breakPoints, maxSteps, this);
            } catch (ProcessingException var4) {
            }
         }
      } else {
         JOptionPane.showMessageDialog(this.mainUI, "The program must be assembled before it can be run.");
      }

   }

   public void paused(boolean done, int pauseReason, ProcessingException pe) {
      if (done) {
         this.stopped(pe, 4);
      } else {
         if (pauseReason == 1) {
            this.mainUI.messagesPane.postMarsMessage(this.name + ": execution paused at breakpoint: " + FileStatus.getFile().getName() + "\n\n");
         } else {
            this.mainUI.messagesPane.postMarsMessage(this.name + ": execution paused by user: " + FileStatus.getFile().getName() + "\n\n");
         }

         this.mainUI.getMessagesPane().selectMarsMessageTab();
         this.executePane.getTextSegmentWindow().setCodeHighlighting(true);
         this.executePane.getTextSegmentWindow().highlightStepAtPC();
         this.executePane.getRegistersWindow().updateRegisters();
         this.executePane.getCoprocessor1Window().updateRegisters();
         this.executePane.getCoprocessor0Window().updateRegisters();
         this.executePane.getDataSegmentWindow().updateValues();
         FileStatus.set(5);
         VenusUI.setReset(false);
      }
   }

   public void stopped(ProcessingException pe, int reason) {
      this.executePane.getRegistersWindow().updateRegisters();
      this.executePane.getCoprocessor1Window().updateRegisters();
      this.executePane.getCoprocessor0Window().updateRegisters();
      this.executePane.getDataSegmentWindow().updateValues();
      FileStatus.set(7);
      SystemIO.resetFiles();
      if (pe != null) {
         this.mainUI.getRegistersPane().setSelectedComponent(this.executePane.getCoprocessor0Window());
         this.executePane.getTextSegmentWindow().setCodeHighlighting(true);
         this.executePane.getTextSegmentWindow().unhighlightAllSteps();
         this.executePane.getTextSegmentWindow().highlightStepAtAddress(RegisterFile.getProgramCounter() - 4);
      }

      switch (reason) {
         case 1:
         default:
            break;
         case 2:
            this.mainUI.getMessagesPane().postMarsMessage(pe.errors().generateErrorReport());
            this.mainUI.getMessagesPane().postMarsMessage("\n" + this.name + ": execution terminated with errors.\n\n");
            break;
         case 3:
            this.mainUI.getMessagesPane().postMarsMessage("\n" + this.name + ": execution step limit of " + maxSteps + " exceeded.\n\n");
            this.mainUI.getMessagesPane().selectMarsMessageTab();
            break;
         case 4:
            this.mainUI.getMessagesPane().postMarsMessage("\n" + this.name + ": execution completed successfully.\n\n");
            this.mainUI.getMessagesPane().postRunMessage("\n-- program is finished running --\n\n");
            this.mainUI.getMessagesPane().selectRunMessageTab();
            break;
         case 5:
            this.mainUI.getMessagesPane().postMarsMessage("\n" + this.name + ": execution terminated by null instruction.\n\n");
            this.mainUI.getMessagesPane().postRunMessage("\n-- program is finished running (dropped off bottom) --\n\n");
            this.mainUI.getMessagesPane().selectRunMessageTab();
            break;
         case 6:
            this.mainUI.getMessagesPane().postMarsMessage("\n" + this.name + ": execution terminated by user.\n\n");
            this.mainUI.getMessagesPane().selectMarsMessageTab();
      }

      resetMaxSteps();
      VenusUI.setReset(false);
   }

   public static void resetMaxSteps() {
      maxSteps = defaultMaxSteps;
   }

   private void processProgramArgumentsIfAny() {
      String programArguments = this.executePane.getTextSegmentWindow().getProgramArguments();
      if (programArguments != null && programArguments.length() != 0 && Globals.getSettings().getProgramArguments()) {
         (new ProgramArgumentList(programArguments)).storeProgramArguments();
      }
   }
}
