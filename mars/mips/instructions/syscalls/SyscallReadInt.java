package mars.mips.instructions.syscalls;

import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.RegisterFile;
import mars.util.SystemIO;

public class SyscallReadInt extends AbstractSyscall {
   public SyscallReadInt() {
      super(5, "ReadInt");
   }

   public void simulate(ProgramStatement statement) throws ProcessingException {
      int value;
      try {
         value = SystemIO.readInteger(this.getNumber());
      } catch (NumberFormatException var4) {
         throw new ProcessingException(statement, "invalid integer input (syscall " + this.getNumber() + ")", 8);
      }

      RegisterFile.updateRegister(2, value);
   }
}
