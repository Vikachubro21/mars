package mars.venus;

import java.awt.event.ActionEvent;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import mars.Globals;

public class HelpAboutAction extends GuiAction {
   public HelpAboutAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
      super(name, icon, descrip, mnemonic, accel, gui);
   }

   public void actionPerformed(ActionEvent e) {
      JOptionPane.showMessageDialog(this.mainUI, "MARS 4.5    Copyright " + Globals.copyrightYears + "\n" + "MARS Plus Version: 0.2 Beta by Caleb Hoff\n" + Globals.copyrightHolders + "\n" + "MARS is the Mips Assembler and Runtime Simulator.\n\n" + "Mars image courtesy of NASA/JPL.\n" + "Print feature adapted from HardcopyWriter class in David Flanagan's\n" + "Java Examples in a Nutshell 3rd Edition, O'Reilly, ISBN 0-596-00620-9.", "About Mars", 1, new ImageIcon("images/RedMars50.gif"));
   }
}
