package mars.mips.hardware;

import java.util.Observer;
import mars.Globals;
import mars.assembler.SymbolTable;
import mars.util.Binary;

public class RegisterFile {
   public static final int GLOBAL_POINTER_REGISTER = 28;
   public static final int STACK_POINTER_REGISTER = 29;
   private static Register[] regFile;
   private static Register programCounter;
   private static Register hi;
   private static Register lo;

   public static void showRegisters() {
      for(int i = 0; i < regFile.length; ++i) {
         System.out.println("Name: " + regFile[i].getName());
         System.out.println("Number: " + regFile[i].getNumber());
         System.out.println("Value: " + regFile[i].getValue());
         System.out.println("");
      }

   }

   public static int updateRegister(int num, int val) {
      int old = 0;
      if (num != 0) {
         for(int i = 0; i < regFile.length; ++i) {
            if (regFile[i].getNumber() == num) {
               old = Globals.getSettings().getBackSteppingEnabled() ? Globals.program.getBackStepper().addRegisterFileRestore(num, regFile[i].setValue(val)) : regFile[i].setValue(val);
               break;
            }
         }
      }

      if (num == 33) {
         old = Globals.getSettings().getBackSteppingEnabled() ? Globals.program.getBackStepper().addRegisterFileRestore(num, hi.setValue(val)) : hi.setValue(val);
      } else if (num == 34) {
         old = Globals.getSettings().getBackSteppingEnabled() ? Globals.program.getBackStepper().addRegisterFileRestore(num, lo.setValue(val)) : lo.setValue(val);
      }

      return old;
   }

   public static void updateRegister(String reg, int val) {
      if (!reg.equals("zero")) {
         for(int i = 0; i < regFile.length; ++i) {
            if (regFile[i].getName().equals(reg)) {
               updateRegister(i, val);
               break;
            }
         }
      }

   }

   public static int getValue(int num) {
      if (num == 33) {
         return hi.getValue();
      } else {
         return num == 34 ? lo.getValue() : regFile[num].getValue();
      }
   }

   public static int getNumber(String n) {
      int j = -1;

      for(int i = 0; i < regFile.length; ++i) {
         if (regFile[i].getName().equals(n)) {
            j = regFile[i].getNumber();
            break;
         }
      }

      return j;
   }

   public static Register[] getRegisters() {
      return regFile;
   }

   public static Register getUserRegister(String Rname) {
      Register reg = null;
      if (Rname.charAt(0) == '$') {
         try {
            reg = regFile[Binary.stringToInt(Rname.substring(1))];
         } catch (Exception var4) {
            reg = null;

            for(int i = 0; i < regFile.length; ++i) {
               if (Rname.equals(regFile[i].getName())) {
                  reg = regFile[i];
                  break;
               }
            }
         }
      }

      return reg;
   }

   public static void initializeProgramCounter(int value) {
      programCounter.setValue(value);
   }

   public static void initializeProgramCounter(boolean startAtMain) {
      int mainAddr = Globals.symbolTable.getAddress(SymbolTable.getStartLabel());
      if (!startAtMain || mainAddr == -1 || !Memory.inTextSegment(mainAddr) && !Memory.inKernelTextSegment(mainAddr)) {
         initializeProgramCounter(programCounter.getResetValue());
      } else {
         initializeProgramCounter(mainAddr);
      }

   }

   public static int setProgramCounter(int value) {
      int old = programCounter.getValue();
      programCounter.setValue(value);
      if (Globals.getSettings().getBackSteppingEnabled()) {
         Globals.program.getBackStepper().addPCRestore(old);
      }

      return old;
   }

   public static int getProgramCounter() {
      return programCounter.getValue();
   }

   public static Register getProgramCounterRegister() {
      return programCounter;
   }

   public static int getInitialProgramCounter() {
      return programCounter.getResetValue();
   }

   public static void resetRegisters() {
      for(int i = 0; i < regFile.length; ++i) {
         regFile[i].resetValue();
      }

      initializeProgramCounter(Globals.getSettings().getStartAtMain());
      hi.resetValue();
      lo.resetValue();
   }

   public static void incrementPC() {
      programCounter.setValue(programCounter.getValue() + 4);
   }

   public static void addRegistersObserver(Observer observer) {
      for(int i = 0; i < regFile.length; ++i) {
         regFile[i].addObserver(observer);
      }

      hi.addObserver(observer);
      lo.addObserver(observer);
   }

   public static void deleteRegistersObserver(Observer observer) {
      for(int i = 0; i < regFile.length; ++i) {
         regFile[i].deleteObserver(observer);
      }

      hi.deleteObserver(observer);
      lo.deleteObserver(observer);
   }

   static {
      regFile = new Register[]{new Register("$zero", 0, 0), new Register("$at", 1, 0), new Register("$v0", 2, 0), new Register("$v1", 3, 0), new Register("$a0", 4, 0), new Register("$a1", 5, 0), new Register("$a2", 6, 0), new Register("$a3", 7, 0), new Register("$t0", 8, 0), new Register("$t1", 9, 0), new Register("$t2", 10, 0), new Register("$t3", 11, 0), new Register("$t4", 12, 0), new Register("$t5", 13, 0), new Register("$t6", 14, 0), new Register("$t7", 15, 0), new Register("$s0", 16, 0), new Register("$s1", 17, 0), new Register("$s2", 18, 0), new Register("$s3", 19, 0), new Register("$s4", 20, 0), new Register("$s5", 21, 0), new Register("$s6", 22, 0), new Register("$s7", 23, 0), new Register("$t8", 24, 0), new Register("$t9", 25, 0), new Register("$k0", 26, 0), new Register("$k1", 27, 0), new Register("$gp", 28, Memory.globalPointer), new Register("$sp", 29, Memory.stackPointer), new Register("$fp", 30, 0), new Register("$ra", 31, 0)};
      programCounter = new Register("pc", 32, Memory.textBaseAddress);
      hi = new Register("hi", 33, 0);
      lo = new Register("lo", 34, 0);
   }
}
