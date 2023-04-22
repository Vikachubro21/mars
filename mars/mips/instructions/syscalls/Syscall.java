package mars.mips.instructions.syscalls;

import mars.ProcessingException;
import mars.ProgramStatement;

public interface Syscall {
   String getName();

   void setNumber(int var1);

   int getNumber();

   void simulate(ProgramStatement var1) throws ProcessingException;
}
