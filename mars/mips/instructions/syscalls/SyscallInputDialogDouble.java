package mars.mips.instructions.syscalls;

import javax.swing.JOptionPane;
import mars.Globals;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.InvalidRegisterAccessException;
import mars.mips.hardware.RegisterFile;

public class SyscallInputDialogDouble extends AbstractSyscall {
   public SyscallInputDialogDouble() {
      super(53, "InputDialogDouble");
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
      } catch (AddressErrorException var10) {
         throw new ProcessingException(statement, var10);
      }

      String inputValue = null;
      inputValue = JOptionPane.showInputDialog(message);

      try {
         Coprocessor1.setRegisterPairToDouble(0, 0.0);
         if (inputValue == null) {
            RegisterFile.updateRegister(5, -2);
         } else if (inputValue.length() == 0) {
            RegisterFile.updateRegister(5, -3);
         } else {
            double doubleValue = Double.parseDouble(inputValue);
            Coprocessor1.setRegisterPairToDouble(0, doubleValue);
            RegisterFile.updateRegister(5, 0);
         }
      } catch (InvalidRegisterAccessException var8) {
         RegisterFile.updateRegister(5, -1);
         throw new ProcessingException(statement, "invalid int reg. access during double input (syscall " + this.getNumber() + ")", 8);
      } catch (NumberFormatException var9) {
         RegisterFile.updateRegister(5, -1);
      }

   }
}
