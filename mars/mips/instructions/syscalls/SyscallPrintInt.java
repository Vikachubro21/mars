package mars.mips.instructions.syscalls;

import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.RegisterFile;
import mars.util.SystemIO;

public class SyscallPrintInt extends AbstractSyscall {
   public SyscallPrintInt() {
      super(1, "PrintInt");
   }

   public void simulate(ProgramStatement statement) throws ProcessingException {
      SystemIO.printString((new Integer(RegisterFile.getValue(4))).toString());
   }
}
