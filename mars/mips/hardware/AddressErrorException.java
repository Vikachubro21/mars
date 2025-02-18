package mars.mips.hardware;

import mars.util.Binary;

public class AddressErrorException extends Exception {
   private int address;
   private int type;

   public AddressErrorException(String message, int exceptType, int addr) {
      super(message + Binary.intToHexString(addr));
      this.address = addr;
      this.type = exceptType;
   }

   public int getAddress() {
      return this.address;
   }

   public int getType() {
      return this.type;
   }
}
