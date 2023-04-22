package mars.venus;

import java.awt.event.ActionEvent;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import mars.Globals;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.Memory;
import mars.mips.hardware.RegisterFile;

public class RunBackstepAction extends GuiAction {
   String name;
   ExecutePane executePane;

   public RunBackstepAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
      super(name, icon, descrip, mnemonic, accel, gui);
   }

   public void actionPerformed(ActionEvent e) {
      this.name = this.getValue("Name").toString();
      this.executePane = this.mainUI.getMainPane().getExecutePane();
      boolean done = false;
      if (!FileStatus.isAssembled()) {
         JOptionPane.showMessageDialog(this.mainUI, "The program must be assembled before it can be run.");
      } else {
         VenusUI.setStarted(true);
         this.mainUI.messagesPane.setSelectedComponent(this.mainUI.messagesPane.runTab);
         this.executePane.getTextSegmentWindow().setCodeHighlighting(true);
         if (Globals.getSettings().getBackSteppingEnabled()) {
            boolean inDelaySlot = Globals.program.getBackStepper().inDelaySlot();
            Memory.getInstance().addObserver(this.executePane.getDataSegmentWindow());
            RegisterFile.addRegistersObserver(this.executePane.getRegistersWindow());
            Coprocessor0.addRegistersObserver(this.executePane.getCoprocessor0Window());
            Coprocessor1.addRegistersObserver(this.executePane.getCoprocessor1Window());
            Globals.program.getBackStepper().backStep();
            Memory.getInstance().deleteObserver(this.executePane.getDataSegmentWindow());
            RegisterFile.deleteRegistersObserver(this.executePane.getRegistersWindow());
            this.executePane.getRegistersWindow().updateRegisters();
            this.executePane.getCoprocessor1Window().updateRegisters();
            this.executePane.getCoprocessor0Window().updateRegisters();
            this.executePane.getDataSegmentWindow().updateValues();
            this.executePane.getTextSegmentWindow().highlightStepAtPC(inDelaySlot);
            FileStatus.set(5);
            VenusUI.setReset(false);
         }
      }
   }
}
