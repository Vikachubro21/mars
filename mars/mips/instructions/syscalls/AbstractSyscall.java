package mars.mips.instructions.syscalls;

import mars.ProcessingException;
import mars.ProgramStatement;

public abstract class AbstractSyscall implements Syscall {
   private int serviceNumber;
   private String serviceName;

   public AbstractSyscall(int number, String name) {
      this.serviceNumber = number;
      this.serviceName = name;
   }

   public String getName() {
      return this.serviceName;
   }

   public void setNumber(int num) {
      this.serviceNumber = num;
   }

   public int getNumber() {
      return this.serviceNumber;
   }

   public abstract void simulate(ProgramStatement var1) throws ProcessingException;
}
