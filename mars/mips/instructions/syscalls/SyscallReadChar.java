package mars.mips.instructions.syscalls;

import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.RegisterFile;
import mars.util.SystemIO;

public class SyscallReadChar extends AbstractSyscall {
   public SyscallReadChar() {
      super(12, "ReadChar");
   }

   public void simulate(ProgramStatement statement) throws ProcessingException {
      int value;
      try {
         value = SystemIO.readChar(this.getNumber());
      } catch (IndexOutOfBoundsException var4) {
         throw new ProcessingException(statement, "invalid char input (syscall " + this.getNumber() + ")", 8);
      }

      RegisterFile.updateRegister(2, value);
   }
}
