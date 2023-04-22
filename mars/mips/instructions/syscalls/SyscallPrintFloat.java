package mars.mips.instructions.syscalls;

import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.Coprocessor1;
import mars.util.SystemIO;

public class SyscallPrintFloat extends AbstractSyscall {
   public SyscallPrintFloat() {
      super(2, "PrintFloat");
   }

   public void simulate(ProgramStatement statement) throws ProcessingException {
      SystemIO.printString((new Float(Float.intBitsToFloat(Coprocessor1.getValue(12)))).toString());
   }
}
