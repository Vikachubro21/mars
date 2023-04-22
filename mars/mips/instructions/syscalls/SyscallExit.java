package mars.mips.instructions.syscalls;

import mars.ProcessingException;
import mars.ProgramStatement;

public class SyscallExit extends AbstractSyscall {
   public SyscallExit() {
      super(10, "Exit");
   }

   public void simulate(ProgramStatement statement) throws ProcessingException {
      throw new ProcessingException();
   }
}
