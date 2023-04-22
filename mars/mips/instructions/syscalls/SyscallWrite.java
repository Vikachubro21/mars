package mars.mips.instructions.syscalls;

import mars.Globals;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.RegisterFile;
import mars.util.SystemIO;

public class SyscallWrite extends AbstractSyscall {
   public SyscallWrite() {
      super(15, "Write");
   }

   public void simulate(ProgramStatement statement) throws ProcessingException {
      int byteAddress = RegisterFile.getValue(5);
      int reqLength = RegisterFile.getValue(6);
      int index = 0;
      byte[] myBuffer = new byte[RegisterFile.getValue(6) + 1];

      try {
         byte b = (byte)Globals.memory.getByte(byteAddress);

         while(true) {
            if (index >= reqLength) {
               myBuffer[index] = 0;
               break;
            }

            myBuffer[index++] = b;
            ++byteAddress;
            b = (byte)Globals.memory.getByte(byteAddress);
         }
      } catch (AddressErrorException var8) {
         throw new ProcessingException(statement, var8);
      }

      int retValue = SystemIO.writeToFile(RegisterFile.getValue(4), myBuffer, RegisterFile.getValue(6));
      RegisterFile.updateRegister(2, retValue);
   }
}
