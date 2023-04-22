package mars.mips.instructions.syscalls;

import mars.Globals;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.RegisterFile;
import mars.util.SystemIO;

public class SyscallRead extends AbstractSyscall {
   public SyscallRead() {
      super(14, "Read");
   }

   public void simulate(ProgramStatement statement) throws ProcessingException {
      int byteAddress = RegisterFile.getValue(5);
      int index = 0;
      byte[] myBuffer = new byte[RegisterFile.getValue(6)];
      int retLength = SystemIO.readFromFile(RegisterFile.getValue(4), myBuffer, RegisterFile.getValue(6));
      RegisterFile.updateRegister(2, retLength);

      try {
         while(index < retLength) {
            Globals.memory.setByte(byteAddress++, myBuffer[index++]);
         }

      } catch (AddressErrorException var8) {
         throw new ProcessingException(statement, var8);
      }
   }
}
