package mars.mips.instructions.syscalls;

import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.RegisterFile;
import mars.util.Binary;
import mars.util.SystemIO;

public class SyscallPrintIntBinary extends AbstractSyscall {
   public SyscallPrintIntBinary() {
      super(35, "PrintIntBinary");
   }

   public void simulate(ProgramStatement statement) throws ProcessingException {
      SystemIO.printString(Binary.intToBinaryString(RegisterFile.getValue(4)));
   }
}
