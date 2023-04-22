package mars.mips.instructions.syscalls;

import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.RegisterFile;
import mars.util.Binary;
import mars.util.SystemIO;

public class SyscallPrintIntHex extends AbstractSyscall {
   public SyscallPrintIntHex() {
      super(34, "PrintIntHex");
   }

   public void simulate(ProgramStatement statement) throws ProcessingException {
      SystemIO.printString(Binary.intToHexString(RegisterFile.getValue(4)));
   }
}
