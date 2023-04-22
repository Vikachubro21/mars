package mars.mips.instructions.syscalls;

import mars.Globals;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.RegisterFile;

public class SyscallSbrk extends AbstractSyscall {
   public SyscallSbrk() {
      super(9, "Sbrk");
   }

   public void simulate(ProgramStatement statement) throws ProcessingException {
      int address;
      try {
         address = Globals.memory.allocateBytesFromHeap(RegisterFile.getValue(4));
      } catch (IllegalArgumentException var4) {
         throw new ProcessingException(statement, var4.getMessage() + " (syscall " + this.getNumber() + ")", 8);
      }

      RegisterFile.updateRegister(2, address);
   }
}
