package mars.mips.instructions.syscalls;

import javax.swing.JOptionPane;
import mars.Globals;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.RegisterFile;

public class SyscallInputDialogFloat extends AbstractSyscall {
   public SyscallInputDialogFloat() {
      super(52, "InputDialogFloat");
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

      try {
         Coprocessor1.setRegisterToFloat(0, 0.0F);
         if (inputValue == null) {
            RegisterFile.updateRegister(5, -2);
         } else if (inputValue.length() == 0) {
            RegisterFile.updateRegister(5, -3);
         } else {
            float floatValue = Float.parseFloat(inputValue);
            Coprocessor1.setRegisterToFloat(0, floatValue);
            RegisterFile.updateRegister(5, 0);
         }
      } catch (NumberFormatException var7) {
         RegisterFile.updateRegister(5, -1);
      }

   }
}
