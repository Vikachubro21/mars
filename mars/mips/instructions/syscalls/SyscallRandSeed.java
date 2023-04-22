package mars.mips.instructions.syscalls;

import java.util.Random;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.RegisterFile;

public class SyscallRandSeed extends AbstractSyscall {
   public SyscallRandSeed() {
      super(40, "RandSeed");
   }

   public void simulate(ProgramStatement statement) throws ProcessingException {
      Integer index = new Integer(RegisterFile.getValue(4));
      Random stream = (Random)RandomStreams.randomStreams.get(index);
      if (stream == null) {
         RandomStreams.randomStreams.put(index, new Random((long)RegisterFile.getValue(5)));
      } else {
         stream.setSeed((long)RegisterFile.getValue(5));
      }

   }
}
