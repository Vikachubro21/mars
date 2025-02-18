package mars.mips.instructions.syscalls;

import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.Coprocessor1;
import mars.util.SystemIO;

public class SyscallReadFloat extends AbstractSyscall {
   public SyscallReadFloat() {
      super(6, "ReadFloat");
   }

   public void simulate(ProgramStatement statement) throws ProcessingException {
      float floatValue = 0.0F;

      try {
         floatValue = SystemIO.readFloat(this.getNumber());
      } catch (NumberFormatException var4) {
         throw new ProcessingException(statement, "invalid float input (syscall " + this.getNumber() + ")", 8);
      }

      Coprocessor1.updateRegister(0, Float.floatToRawIntBits(floatValue));
   }
}
