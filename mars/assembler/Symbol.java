package mars.assembler;

public class Symbol {
   private String name;
   private int address;
   private boolean data;
   public static final boolean TEXT_SYMBOL = false;
   public static final boolean DATA_SYMBOL = true;

   public Symbol(String name, int address, boolean data) {
      this.name = name;
      this.address = address;
      this.data = data;
   }

   public int getAddress() {
      return this.address;
   }

   public String getName() {
      return this.name;
   }

   public boolean getType() {
      return this.data;
   }

   public void setAddress(int newAddress) {
      this.address = newAddress;
   }
}
