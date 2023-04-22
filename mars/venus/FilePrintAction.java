package mars.venus;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import javax.swing.Icon;
import javax.swing.KeyStroke;

public class FilePrintAction extends GuiAction {
   public FilePrintAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
      super(name, icon, descrip, mnemonic, accel, gui);
   }

   public void actionPerformed(ActionEvent e) {
      EditPane editPane = this.mainUI.getMainPane().getEditPane();
      if (editPane != null) {
         int fontsize = 10;
         double margins = 0.5;

         HardcopyWriter out;
         try {
            out = new HardcopyWriter(this.mainUI, editPane.getFilename(), fontsize, margins, margins, margins, margins);
         } catch (HardcopyWriter.PrintCanceledException var14) {
            return;
         }

         BufferedReader in = new BufferedReader(new StringReader(editPane.getSource()));
         int lineNumberDigits = (new Integer(editPane.getSourceLineCount())).toString().length();
         String lineNumberString = "";
         int lineNumber = 0;

         try {
            for(String line = in.readLine(); line != null; line = in.readLine()) {
               if (editPane.showingLineNumbers()) {
                  ++lineNumber;

                  for(lineNumberString = (new Integer(lineNumber)).toString() + ": "; lineNumberString.length() < lineNumberDigits; lineNumberString = lineNumberString + " ") {
                  }
               }

               line = lineNumberString + line + "\n";
               out.write(line.toCharArray(), 0, line.length());
            }

            in.close();
            out.close();
         } catch (IOException var15) {
         }

      }
   }
}
