package mars.mips.instructions.syscalls;

import java.awt.Component;
import javax.swing.JOptionPane;
import mars.Globals;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.RegisterFile;

public class SyscallMessageDialogFloat extends AbstractSyscall {
   public SyscallMessageDialogFloat() {
      super(57, "MessageDialogFloat");
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
      } catch (AddressErrorException var6) {
         throw new ProcessingException(statement, var6);
      }

      JOptionPane.showMessageDialog((Component)null, message + Float.toString(Coprocessor1.getFloatFromRegister("$f12")), (String)null, 1);
   }
}
