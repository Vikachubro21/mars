package mars;

import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.RegisterFile;
import mars.simulator.Exceptions;
import mars.util.Binary;

public class ProcessingException extends Exception {
   private ErrorList errs;

   public ProcessingException(ErrorList e) {
      this.errs = e;
   }

   public ProcessingException(ErrorList e, AddressErrorException aee) {
      this.errs = e;
      Exceptions.setRegisters(aee.getType(), aee.getAddress());
   }

   public ProcessingException(ProgramStatement ps, String m) {
      this.errs = new ErrorList();
      this.errs.add(new ErrorMessage(ps, "Runtime exception at " + Binary.intToHexString(RegisterFile.getProgramCounter() - 4) + ": " + m));
   }

   public ProcessingException(ProgramStatement ps, String m, int cause) {
      this(ps, m);
      Exceptions.setRegisters(cause);
   }

   public ProcessingException(ProgramStatement ps, AddressErrorException aee) {
      this(ps, aee.getMessage());
      Exceptions.setRegisters(aee.getType(), aee.getAddress());
   }

   public ProcessingException() {
      this.errs = null;
   }

   public ErrorList errors() {
      return this.errs;
   }
}
