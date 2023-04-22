package mars.mips.hardware;

import java.util.Observer;
import mars.Globals;

public class Coprocessor0 {
   public static final int VADDR = 8;
   public static final int COUNT = 9;
   public static final int COMPARE = 11;
   public static final int STATUS = 12;
   public static final int CAUSE = 13;
   public static final int EPC = 14;
   public static final int EXCEPTION_LEVEL = 1;
   public static final int DEFAULT_STATUS_VALUE = 65297;
   private static Register[] registers = new Register[]{new Register("$8 (vaddr)", 8, 0), new Register("$9 (count)", 9, 0), new Register("$11 (compare)", 11, Integer.MAX_VALUE), new Register("$12 (status)", 12, 65297), new Register("$13 (cause)", 13, 0), new Register("$14 (epc)", 14, 0)};

   public static void showRegisters() {
      for(int i = 0; i < registers.length; ++i) {
         System.out.println("Name: " + registers[i].getName());
         System.out.println("Number: " + registers[i].getNumber());
         System.out.println("Value: " + registers[i].getValue());
         System.out.println("");
      }

   }

   public static int updateRegister(String n, int val) {
      int oldValue = 0;

      for(int i = 0; i < registers.length; ++i) {
         if (("$" + registers[i].getNumber()).equals(n) || registers[i].getName().equals(n)) {
            oldValue = registers[i].getValue();
            registers[i].setValue(val);
            break;
         }
      }

      return oldValue;
   }

   public static int updateRegister(int num, int val) {
      int old = 0;

      for(int i = 0; i < registers.length; ++i) {
         if (registers[i].getNumber() == num) {
            old = Globals.getSettings().getBackSteppingEnabled() ? Globals.program.getBackStepper().addCoprocessor0Restore(num, registers[i].setValue(val)) : registers[i].setValue(val);
            break;
         }
      }

      return old;
   }

   public static int getValue(int num) {
      for(int i = 0; i < registers.length; ++i) {
         if (registers[i].getNumber() == num) {
            return registers[i].getValue();
         }
      }

      return 0;
   }

   public static int getNumber(String n) {
      for(int i = 0; i < registers.length; ++i) {
         if (("$" + registers[i].getNumber()).equals(n) || registers[i].getName().equals(n)) {
            return registers[i].getNumber();
         }
      }

      return -1;
   }

   public static Register[] getRegisters() {
      return registers;
   }

   public static int getRegisterPosition(Register r) {
      for(int i = 0; i < registers.length; ++i) {
         if (registers[i] == r) {
            return i;
         }
      }

      return -1;
   }

   public static Register getRegister(String rname) {
      for(int i = 0; i < registers.length; ++i) {
         if (("$" + registers[i].getNumber()).equals(rname) || registers[i].getName().equals(rname)) {
            return registers[i];
         }
      }

      return null;
   }

   public static void resetRegisters() {
      for(int i = 0; i < registers.length; ++i) {
         registers[i].resetValue();
      }

   }

   public static void addRegistersObserver(Observer observer) {
      for(int i = 0; i < registers.length; ++i) {
         registers[i].addObserver(observer);
      }

   }

   public static void deleteRegistersObserver(Observer observer) {
      for(int i = 0; i < registers.length; ++i) {
         registers[i].deleteObserver(observer);
      }

   }
}
