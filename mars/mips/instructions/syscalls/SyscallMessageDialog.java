package mars.mips.instructions.syscalls;

import java.awt.Component;
import javax.swing.JOptionPane;
import mars.Globals;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.RegisterFile;

public class SyscallMessageDialog extends AbstractSyscall {
   public SyscallMessageDialog() {
      super(55, "MessageDialog");
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

      int msgType = RegisterFile.getValue(5);
      if (msgType < 0 || msgType > 3) {
         msgType = -1;
      }

      JOptionPane.showMessageDialog((Component)null, message, (String)null, msgType);
   }
}
