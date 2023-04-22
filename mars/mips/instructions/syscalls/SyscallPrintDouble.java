package mars.mips.instructions.syscalls;

import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.Coprocessor1;
import mars.util.Binary;
import mars.util.SystemIO;

public class SyscallPrintDouble extends AbstractSyscall {
   public SyscallPrintDouble() {
      super(3, "PrintDouble");
   }

   public void simulate(ProgramStatement statement) throws ProcessingException {
      SystemIO.printString((new Double(Double.longBitsToDouble(Binary.twoIntsToLong(Coprocessor1.getValue(13), Coprocessor1.getValue(12))))).toString());
   }
}
