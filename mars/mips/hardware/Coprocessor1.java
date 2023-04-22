package mars.mips.hardware;

import java.util.Observer;
import mars.Globals;
import mars.util.Binary;

public class Coprocessor1 {
   private static Register[] registers = new Register[]{new Register("$f0", 0, 0), new Register("$f1", 1, 0), new Register("$f2", 2, 0), new Register("$f3", 3, 0), new Register("$f4", 4, 0), new Register("$f5", 5, 0), new Register("$f6", 6, 0), new Register("$f7", 7, 0), new Register("$f8", 8, 0), new Register("$f9", 9, 0), new Register("$f10", 10, 0), new Register("$f11", 11, 0), new Register("$f12", 12, 0), new Register("$f13", 13, 0), new Register("$f14", 14, 0), new Register("$f15", 15, 0), new Register("$f16", 16, 0), new Register("$f17", 17, 0), new Register("$f18", 18, 0), new Register("$f19", 19, 0), new Register("$f20", 20, 0), new Register("$f21", 21, 0), new Register("$f22", 22, 0), new Register("$f23", 23, 0), new Register("$f24", 24, 0), new Register("$f25", 25, 0), new Register("$f26", 26, 0), new Register("$f27", 27, 0), new Register("$f28", 28, 0), new Register("$f29", 29, 0), new Register("$f30", 30, 0), new Register("$f31", 31, 0)};
   private static Register condition = new Register("cf", 32, 0);
   private static int numConditionFlags = 8;

   public static void showRegisters() {
      for(int i = 0; i < registers.length; ++i) {
         System.out.println("Name: " + registers[i].getName());
         System.out.println("Number: " + registers[i].getNumber());
         System.out.println("Value: " + registers[i].getValue());
         System.out.println("");
      }

   }

   public static void setRegisterToFloat(String reg, float val) {
      setRegisterToFloat(getRegisterNumber(reg), val);
   }

   public static void setRegisterToFloat(int reg, float val) {
      if (reg >= 0 && reg < registers.length) {
         registers[reg].setValue(Float.floatToRawIntBits(val));
      }

   }

   public static void setRegisterToInt(String reg, int val) {
      setRegisterToInt(getRegisterNumber(reg), val);
   }

   public static void setRegisterToInt(int reg, int val) {
      if (reg >= 0 && reg < registers.length) {
         registers[reg].setValue(val);
      }

   }

   public static void setRegisterPairToDouble(int reg, double val) throws InvalidRegisterAccessException {
      if (reg % 2 != 0) {
         throw new InvalidRegisterAccessException();
      } else {
         long bits = Double.doubleToRawLongBits(val);
         registers[reg + 1].setValue(Binary.highOrderLongToInt(bits));
         registers[reg].setValue(Binary.lowOrderLongToInt(bits));
      }
   }

   public static void setRegisterPairToDouble(String reg, double val) throws InvalidRegisterAccessException {
      setRegisterPairToDouble(getRegisterNumber(reg), val);
   }

   public static void setRegisterPairToLong(int reg, long val) throws InvalidRegisterAccessException {
      if (reg % 2 != 0) {
         throw new InvalidRegisterAccessException();
      } else {
         registers[reg + 1].setValue(Binary.highOrderLongToInt(val));
         registers[reg].setValue(Binary.lowOrderLongToInt(val));
      }
   }

   public static void setRegisterPairToLong(String reg, long val) throws InvalidRegisterAccessException {
      setRegisterPairToLong(getRegisterNumber(reg), val);
   }

   public static float getFloatFromRegister(int reg) {
      float result = 0.0F;
      if (reg >= 0 && reg < registers.length) {
         result = Float.intBitsToFloat(registers[reg].getValue());
      }

      return result;
   }

   public static float getFloatFromRegister(String reg) {
      return getFloatFromRegister(getRegisterNumber(reg));
   }

   public static int getIntFromRegister(int reg) {
      int result = 0;
      if (reg >= 0 && reg < registers.length) {
         result = registers[reg].getValue();
      }

      return result;
   }

   public static int getIntFromRegister(String reg) {
      return getIntFromRegister(getRegisterNumber(reg));
   }

   public static double getDoubleFromRegisterPair(int reg) throws InvalidRegisterAccessException {
      double result = 0.0;
      if (reg % 2 != 0) {
         throw new InvalidRegisterAccessException();
      } else {
         long bits = Binary.twoIntsToLong(registers[reg + 1].getValue(), registers[reg].getValue());
         return Double.longBitsToDouble(bits);
      }
   }

   public static double getDoubleFromRegisterPair(String reg) throws InvalidRegisterAccessException {
      return getDoubleFromRegisterPair(getRegisterNumber(reg));
   }

   public static long getLongFromRegisterPair(int reg) throws InvalidRegisterAccessException {
      double result = 0.0;
      if (reg % 2 != 0) {
         throw new InvalidRegisterAccessException();
      } else {
         return Binary.twoIntsToLong(registers[reg + 1].getValue(), registers[reg].getValue());
      }
   }

   public static long getLongFromRegisterPair(String reg) throws InvalidRegisterAccessException {
      return getLongFromRegisterPair(getRegisterNumber(reg));
   }

   public static int updateRegister(int num, int val) {
      int old = 0;

      for(int i = 0; i < registers.length; ++i) {
         if (registers[i].getNumber() == num) {
            old = Globals.getSettings().getBackSteppingEnabled() ? Globals.program.getBackStepper().addCoprocessor1Restore(num, registers[i].setValue(val)) : registers[i].setValue(val);
            break;
         }
      }

      return old;
   }

   public static int getValue(int num) {
      return registers[num].getValue();
   }

   public static int getRegisterNumber(String n) {
      int j = -1;

      for(int i = 0; i < registers.length; ++i) {
         if (registers[i].getName().equals(n)) {
            j = registers[i].getNumber();
            break;
         }
      }

      return j;
   }

   public static Register[] getRegisters() {
      return registers;
   }

   public static Register getRegister(String rName) {
      Register reg = null;
      if (rName.charAt(0) == '$' && rName.length() > 1 && rName.charAt(1) == 'f') {
         try {
            reg = registers[Binary.stringToInt(rName.substring(2))];
         } catch (Exception var3) {
            reg = null;
         }
      }

      return reg;
   }

   public static void resetRegisters() {
      for(int i = 0; i < registers.length; ++i) {
         registers[i].resetValue();
      }

      clearConditionFlags();
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

   public static int setConditionFlag(int flag) {
      int old = 0;
      if (flag >= 0 && flag < numConditionFlags) {
         old = getConditionFlag(flag);
         condition.setValue(Binary.setBit(condition.getValue(), flag));
         if (Globals.getSettings().getBackSteppingEnabled()) {
            if (old == 0) {
               Globals.program.getBackStepper().addConditionFlagClear(flag);
            } else {
               Globals.program.getBackStepper().addConditionFlagSet(flag);
            }
         }
      }

      return old;
   }

   public static int clearConditionFlag(int flag) {
      int old = 0;
      if (flag >= 0 && flag < numConditionFlags) {
         old = getConditionFlag(flag);
         condition.setValue(Binary.clearBit(condition.getValue(), flag));
         if (Globals.getSettings().getBackSteppingEnabled()) {
            if (old == 0) {
               Globals.program.getBackStepper().addConditionFlagClear(flag);
            } else {
               Globals.program.getBackStepper().addConditionFlagSet(flag);
            }
         }
      }

      return old;
   }

   public static int getConditionFlag(int flag) {
      if (flag < 0 || flag >= numConditionFlags) {
         flag = 0;
      }

      return Binary.bitValue(condition.getValue(), flag);
   }

   public static int getConditionFlags() {
      return condition.getValue();
   }

   public static void clearConditionFlags() {
      condition.setValue(0);
   }

   public static void setConditionFlags() {
      condition.setValue(-1);
   }

   public static int getConditionFlagCount() {
      return numConditionFlags;
   }
}
