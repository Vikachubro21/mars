package mars.venus;

import java.awt.Component;
import java.io.File;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import mars.FileDrop;
import mars.Globals;

public class MainPane extends JTabbedPane {
   EditPane editTab;
   ExecutePane executeTab;
   EditTabbedPane editTabbedPane;
   private VenusUI mainUI;

   public MainPane(VenusUI appFrame, Editor editor, RegistersWindow regs, Coprocessor1Window cop1Regs, Coprocessor0Window cop0Regs) {
      this.mainUI = appFrame;
      this.setTabPlacement(1);
      if (this.getUI() instanceof BasicTabbedPaneUI) {
         BasicTabbedPaneUI var6 = (BasicTabbedPaneUI)this.getUI();
      }

      this.editTabbedPane = new EditTabbedPane(appFrame, editor, this);
      this.executeTab = new ExecutePane(appFrame, regs, cop1Regs, cop0Regs);
      String editTabTitle = "Edit";
      String executeTabTitle = "Execute";
      Icon editTabIcon = null;
      Icon executeTabIcon = null;
      this.setTabLayoutPolicy(1);
      this.addTab(editTabTitle, (Icon)editTabIcon, this.editTabbedPane);
      this.addTab(executeTabTitle, (Icon)executeTabIcon, this.executeTab);
      this.setToolTipTextAt(0, "Text editor for composing MIPS programs.");
      this.setToolTipTextAt(1, "View and control assembly language program execution.  Enabled upon successful assemble.");
      this.addChangeListener(new ChangeListener() {
         public void stateChanged(ChangeEvent ce) {
            JTabbedPane tabbedPane = (JTabbedPane)ce.getSource();
            int index = tabbedPane.getSelectedIndex();
            Component c = tabbedPane.getComponentAt(index);
            ExecutePane executePane = Globals.getGui().getMainPane().getExecutePane();
            if (c == executePane) {
               executePane.setWindowBounds();
               Globals.getGui().getMainPane().removeChangeListener(this);
            }

         }
      });
      new FileDrop(this, new FileDrop.Listener() {
         public void filesDropped(File[] files) {
            EditTabbedPane.openFile(files[0]);
         }
      });
   }

   public EditPane getEditPane() {
      return this.editTabbedPane.getCurrentEditTab();
   }

   public JComponent getEditTabbedPane() {
      return this.editTabbedPane;
   }

   public ExecutePane getExecutePane() {
      return this.executeTab;
   }

   public ExecutePane getExecuteTab() {
      return this.executeTab;
   }
}
