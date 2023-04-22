package mars.mips.instructions;

import mars.ProcessingException;
import mars.ProgramStatement;

public interface SimulationCode {
   void simulate(ProgramStatement var1) throws ProcessingException;
}
