package mars.mips.instructions.syscalls;

import javax.swing.JOptionPane;
import mars.Globals;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.RegisterFile;

public class SyscallInputDialogInt extends AbstractSyscall {
   public SyscallInputDialogInt() {
      super(51, "InputDialogInt");
   }

   public void simulate(ProgramStatement statement) throws ProcessingException {
      String message = new String();
      int byteAddress = RegisterFile.getValue(4);
      char[] ch = new char[]{' '};

      try {
         for(ch[0] = (char)Globals.memory.getByte(byteAddress); ch[0] != 0; ch[0] = (char)Globals.memory.getByte(byteAddress)) {
            message = message.concat(new String(ch));
            ++byteAddress;
         }
      } catch (AddressErrorException var8) {
         throw new ProcessingException(statement, var8);
      }

      String inputValue = null;
      inputValue = JOptionPane.showInputDialog(message);
      if (inputValue == null) {
         RegisterFile.updateRegister(4, 0);
         RegisterFile.updateRegister(5, -2);
      } else if (inputValue.length() == 0) {
         RegisterFile.updateRegister(4, 0);
         RegisterFile.updateRegister(5, -3);
      } else {
         try {
            int i = Integer.parseInt(inputValue);
            RegisterFile.updateRegister(4, i);
            RegisterFile.updateRegister(5, 0);
         } catch (NumberFormatException var7) {
            RegisterFile.updateRegister(4, 0);
            RegisterFile.updateRegister(5, -1);
         }
      }

   }
}
