package mars.mips.instructions.syscalls;

import java.util.Random;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.InvalidRegisterAccessException;
import mars.mips.hardware.RegisterFile;

public class SyscallRandDouble extends AbstractSyscall {
   public SyscallRandDouble() {
      super(44, "RandDouble");
   }

   public void simulate(ProgramStatement statement) throws ProcessingException {
      Integer index = new Integer(RegisterFile.getValue(4));
      Random stream = (Random)RandomStreams.randomStreams.get(index);
      if (stream == null) {
         stream = new Random();
         RandomStreams.randomStreams.put(index, stream);
      }

      try {
         Coprocessor1.setRegisterPairToDouble(0, stream.nextDouble());
      } catch (InvalidRegisterAccessException var5) {
         throw new ProcessingException(statement, "Internal error storing double to register (syscall " + this.getNumber() + ")", 8);
      }
   }
}
