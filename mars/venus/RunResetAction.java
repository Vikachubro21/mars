package mars.venus;

import java.awt.event.ActionEvent;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import mars.Globals;
import mars.ProcessingException;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.Memory;
import mars.mips.hardware.RegisterFile;
import mars.util.SystemIO;

public class RunResetAction extends GuiAction {
   public RunResetAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
      super(name, icon, descrip, mnemonic, accel, gui);
   }

   public void actionPerformed(ActionEvent e) {
      RunGoAction.resetMaxSteps();
      String name = this.getValue("Name").toString();
      ExecutePane executePane = this.mainUI.getMainPane().getExecutePane();

      try {
         Globals.program.assemble(RunAssembleAction.getMIPSprogramsToAssemble(), RunAssembleAction.getExtendedAssemblerEnabled(), RunAssembleAction.getWarningsAreErrors());
      } catch (ProcessingException var5) {
         this.mainUI.getMessagesPane().postMarsMessage("Unable to reset.  Please close file then re-open and re-assemble.\n");
         return;
      }

      RegisterFile.resetRegisters();
      Coprocessor1.resetRegisters();
      Coprocessor0.resetRegisters();
      executePane.getRegistersWindow().clearHighlighting();
      executePane.getRegistersWindow().updateRegisters();
      executePane.getCoprocessor1Window().clearHighlighting();
      executePane.getCoprocessor1Window().updateRegisters();
      executePane.getCoprocessor0Window().clearHighlighting();
      executePane.getCoprocessor0Window().updateRegisters();
      executePane.getDataSegmentWindow().highlightCellForAddress(Memory.dataBaseAddress);
      executePane.getDataSegmentWindow().clearHighlighting();
      executePane.getTextSegmentWindow().resetModifiedSourceCode();
      executePane.getTextSegmentWindow().setCodeHighlighting(true);
      executePane.getTextSegmentWindow().highlightStepAtPC();
      this.mainUI.getRegistersPane().setSelectedComponent(executePane.getRegistersWindow());
      FileStatus.set(5);
      VenusUI.setReset(true);
      VenusUI.setStarted(false);
      SystemIO.resetFiles();
      this.mainUI.getMessagesPane().postRunMessage("\n" + name + ": reset completed.\n\n");
   }
}
