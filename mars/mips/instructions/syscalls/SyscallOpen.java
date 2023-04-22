package mars.mips.instructions.syscalls;

import mars.Globals;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.RegisterFile;
import mars.util.SystemIO;

public class SyscallOpen extends AbstractSyscall {
   public SyscallOpen() {
      super(13, "Open");
   }

   public void simulate(ProgramStatement statement) throws ProcessingException {
      String filename = new String();
      int byteAddress = RegisterFile.getValue(4);
      char[] ch = new char[]{' '};

      try {
         for(ch[0] = (char)Globals.memory.getByte(byteAddress); ch[0] != 0; ch[0] = (char)Globals.memory.getByte(byteAddress)) {
            filename = filename.concat(new String(ch));
            ++byteAddress;
         }
      } catch (AddressErrorException var6) {
         throw new ProcessingException(statement, var6);
      }

      int retValue = SystemIO.openFile(filename, RegisterFile.getValue(5));
      RegisterFile.updateRegister(2, retValue);
   }
}
