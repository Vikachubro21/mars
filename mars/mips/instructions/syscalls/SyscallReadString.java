package mars.mips.instructions.syscalls;

import mars.Globals;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.RegisterFile;
import mars.util.SystemIO;

public class SyscallReadString extends AbstractSyscall {
   public SyscallReadString() {
      super(8, "ReadString");
   }

   public void simulate(ProgramStatement statement) throws ProcessingException {
      String inputString = "";
      int buf = RegisterFile.getValue(4);
      int maxLength = RegisterFile.getValue(5) - 1;
      boolean addNullByte = true;
      if (maxLength < 0) {
         maxLength = 0;
         addNullByte = false;
      }

      inputString = SystemIO.readString(this.getNumber(), maxLength);
      int stringLength = Math.min(maxLength, inputString.length());

      try {
         for(int index = 0; index < stringLength; ++index) {
            Globals.memory.setByte(buf + index, inputString.charAt(index));
         }

         if (stringLength < maxLength) {
            Globals.memory.setByte(buf + stringLength, 10);
            ++stringLength;
         }

         if (addNullByte) {
            Globals.memory.setByte(buf + stringLength, 0);
         }

      } catch (AddressErrorException var8) {
         throw new ProcessingException(statement, var8);
      }
   }
}
