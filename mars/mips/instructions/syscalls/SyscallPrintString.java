package mars.mips.instructions.syscalls;

import mars.Globals;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.RegisterFile;
import mars.util.SystemIO;

public class SyscallPrintString extends AbstractSyscall {
   public SyscallPrintString() {
      super(4, "PrintString");
   }

   public void simulate(ProgramStatement statement) throws ProcessingException {
      int byteAddress = RegisterFile.getValue(4);
      try {
         for(char ch = (char)Globals.memory.getByte(byteAddress); ch != 0; ch = (char)Globals.memory.getByte(byteAddress)) {
            SystemIO.printString((new Character(ch)).toString());
            ++byteAddress;
         }

      } catch (AddressErrorException var5) {
         throw new ProcessingException(statement, var5);
      }
   }
}
