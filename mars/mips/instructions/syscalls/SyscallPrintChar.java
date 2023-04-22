package mars.mips.instructions.syscalls;

import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.RegisterFile;
import mars.util.SystemIO;

public class SyscallPrintChar extends AbstractSyscall {
   public SyscallPrintChar() {
      super(11, "PrintChar");
   }

   public void simulate(ProgramStatement statement) throws ProcessingException {
      char t = (char)(RegisterFile.getValue(4) & 255);
      SystemIO.printString((new Character(t)).toString());
   }
}
