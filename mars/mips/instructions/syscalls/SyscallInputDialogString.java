package mars.mips.instructions.syscalls;

import javax.swing.JOptionPane;
import mars.Globals;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.RegisterFile;

public class SyscallInputDialogString extends AbstractSyscall {
   public SyscallInputDialogString() {
      super(54, "InputDialogString");
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
      } catch (AddressErrorException var9) {
         throw new ProcessingException(statement, var9);
      }

      String inputString = null;
      inputString = JOptionPane.showInputDialog(message);
      byteAddress = RegisterFile.getValue(5);
      int maxLength = RegisterFile.getValue(6);

      try {
         if (inputString == null) {
            RegisterFile.updateRegister(5, -2);
         } else if (inputString.length() == 0) {
            RegisterFile.updateRegister(5, -3);
         } else {
            for(int index = 0; index < inputString.length() && index < maxLength - 1; ++index) {
               Globals.memory.setByte(byteAddress + index, inputString.charAt(index));
            }

            if (inputString.length() < maxLength - 1) {
               Globals.memory.setByte(byteAddress + Math.min(inputString.length(), maxLength - 2), 10);
            }

            Globals.memory.setByte(byteAddress + Math.min(inputString.length() + 1, maxLength - 1), 0);
            if (inputString.length() > maxLength - 1) {
               RegisterFile.updateRegister(5, -4);
            } else {
               RegisterFile.updateRegister(5, 0);
            }
         }

      } catch (AddressErrorException var8) {
         throw new ProcessingException(statement, var8);
      }
   }
}
