package mars.mips.instructions.syscalls;

import java.awt.Component;
import javax.swing.JOptionPane;
import mars.Globals;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.InvalidRegisterAccessException;
import mars.mips.hardware.RegisterFile;

public class SyscallMessageDialogDouble extends AbstractSyscall {
   public SyscallMessageDialogDouble() {
      super(58, "MessageDialogDouble");
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
      } catch (AddressErrorException var7) {
         throw new ProcessingException(statement, var7);
      }

      try {
         JOptionPane.showMessageDialog((Component)null, message + Double.toString(Coprocessor1.getDoubleFromRegisterPair("$f12")), (String)null, 1);
      } catch (InvalidRegisterAccessException var6) {
         RegisterFile.updateRegister(5, -1);
         throw new ProcessingException(statement, "invalid int reg. access during double input (syscall " + this.getNumber() + ")", 8);
      }
   }
}
