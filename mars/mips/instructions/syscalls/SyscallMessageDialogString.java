package mars.mips.instructions.syscalls;

import java.awt.Component;
import javax.swing.JOptionPane;
import mars.Globals;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.RegisterFile;

public class SyscallMessageDialogString extends AbstractSyscall {
   public SyscallMessageDialogString() {
      super(59, "MessageDialogString");
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

      String message2 = new String();
      byteAddress = RegisterFile.getValue(5);

      try {
         for(ch[0] = (char)Globals.memory.getByte(byteAddress); ch[0] != 0; ch[0] = (char)Globals.memory.getByte(byteAddress)) {
            message2 = message2.concat(new String(ch));
            ++byteAddress;
         }
      } catch (AddressErrorException var7) {
         throw new ProcessingException(statement, var7);
      }

      JOptionPane.showMessageDialog((Component)null, message + message2, (String)null, 1);
   }
}
