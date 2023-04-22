package mars.mips.hardware;

public class RegisterAccessNotice extends AccessNotice {
   private String registerName;

   RegisterAccessNotice(int type, String registerName) {
      super(type);
      this.registerName = registerName;
   }

   public String getRegisterName() {
      return this.registerName;
   }

   public String toString() {
      return (this.getAccessType() == 0 ? "R " : "W ") + "Reg " + this.registerName;
   }
}
