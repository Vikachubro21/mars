package mars.mips.instructions.syscalls;

import java.util.Random;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.RegisterFile;

public class SyscallRandIntRange extends AbstractSyscall {
   public SyscallRandIntRange() {
      super(42, "RandIntRange");
   }

   public void simulate(ProgramStatement statement) throws ProcessingException {
      Integer index = new Integer(RegisterFile.getValue(4));
      Random stream = (Random)RandomStreams.randomStreams.get(index);
      if (stream == null) {
         stream = new Random();
         RandomStreams.randomStreams.put(index, stream);
      }

      try {
         RegisterFile.updateRegister(4, stream.nextInt(RegisterFile.getValue(5)));
      } catch (IllegalArgumentException var5) {
         throw new ProcessingException(statement, "Upper bound of range cannot be negative (syscall " + this.getNumber() + ")", 8);
      }
   }
}
