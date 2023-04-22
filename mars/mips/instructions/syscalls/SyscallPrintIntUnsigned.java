package mars.mips.instructions.syscalls;

import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.RegisterFile;
import mars.util.Binary;
import mars.util.SystemIO;

public class SyscallPrintIntUnsigned extends AbstractSyscall {
   public SyscallPrintIntUnsigned() {
      super(36, "PrintIntUnsigned");
   }

   public void simulate(ProgramStatement statement) throws ProcessingException {
      SystemIO.printString(Binary.unsignedIntToIntString(RegisterFile.getValue(4)));
   }
}
