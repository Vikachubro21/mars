package mars.mips.instructions.syscalls;

import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.RegisterFile;

public class SyscallSleep extends AbstractSyscall {
   public SyscallSleep() {
      super(32, "Sleep");
   }

   public void simulate(ProgramStatement statement) throws ProcessingException {
      try {
         Thread.sleep((long)RegisterFile.getValue(4));
      } catch (InterruptedException var3) {
      }
   }
}
