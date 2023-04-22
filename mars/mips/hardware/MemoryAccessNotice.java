package mars.mips.hardware;

public class MemoryAccessNotice extends AccessNotice {
   private int address;
   private int length;
   private int value;

   MemoryAccessNotice(int type, int address, int length, int value) {
      super(type);
      this.address = address;
      this.length = length;
      this.value = value;
   }

   public MemoryAccessNotice(int type, int address, int value) {
      super(type);
      this.address = address;
      this.length = 4;
      this.value = value;
   }

   public int getAddress() {
      return this.address;
   }

   public int getLength() {
      return this.length;
   }

   public int getValue() {
      return this.value;
   }

   public String toString() {
      return (this.getAccessType() == 0 ? "R " : "W ") + "Mem " + this.address + " " + this.length + "B = " + this.value;
   }
}
