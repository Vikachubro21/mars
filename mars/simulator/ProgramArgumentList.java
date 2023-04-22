package mars.simulator;

import java.util.ArrayList;
import java.util.StringTokenizer;
import mars.Globals;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.mips.hardware.Register;
import mars.mips.hardware.RegisterFile;

public class ProgramArgumentList {
   ArrayList programArgumentList;

   public ProgramArgumentList(String args) {
      StringTokenizer st = new StringTokenizer(args);
      this.programArgumentList = new ArrayList(st.countTokens());

      while(st.hasMoreTokens()) {
         this.programArgumentList.add(st.nextToken());
      }

   }

   public ProgramArgumentList(String[] list) {
      this((String[])list, 0);
   }

   public ProgramArgumentList(String[] list, int startPosition) {
      this.programArgumentList = new ArrayList(list.length - startPosition);

      for(int i = startPosition; i < list.length; ++i) {
         this.programArgumentList.add(list[i]);
      }

   }

   public ProgramArgumentList(ArrayList list) {
      this((ArrayList)list, 0);
   }

   public ProgramArgumentList(ArrayList list, int startPosition) {
      if (list != null && list.size() >= startPosition) {
         this.programArgumentList = new ArrayList(list.size() - startPosition);

         for(int i = startPosition; i < list.size(); ++i) {
            this.programArgumentList.add(list.get(i));
         }
      } else {
         this.programArgumentList = new ArrayList(0);
      }

   }

   public void storeProgramArguments() {
      if (this.programArgumentList != null && this.programArgumentList.size() != 0) {
         int highAddress = Memory.stackBaseAddress;
         int[] argStartAddress = new int[this.programArgumentList.size()];

         try {
            int stackAddress;
            int i;
            for(stackAddress = 0; stackAddress < this.programArgumentList.size(); ++stackAddress) {
               String programArgument = (String)this.programArgumentList.get(stackAddress);
               Globals.memory.set(highAddress, 0, 1);
               --highAddress;

               for(i = programArgument.length() - 1; i >= 0; --i) {
                  Globals.memory.set(highAddress, programArgument.charAt(i), 1);
                  --highAddress;
               }

               argStartAddress[stackAddress] = highAddress + 1;
            }

            stackAddress = Memory.stackPointer;
            if (highAddress < Memory.stackPointer) {
               stackAddress = highAddress - highAddress % 4 - 4;
            }

            Globals.memory.set(stackAddress, 0, 4);
            stackAddress -= 4;

            for(i = argStartAddress.length - 1; i >= 0; --i) {
               Globals.memory.set(stackAddress, argStartAddress[i], 4);
               stackAddress -= 4;
            }

            Globals.memory.set(stackAddress, argStartAddress.length, 4);
            stackAddress -= 4;
            Register[] registers = RegisterFile.getRegisters();
            RegisterFile.getUserRegister("$sp").setValue(stackAddress + 4);
            RegisterFile.getUserRegister("$a0").setValue(argStartAddress.length);
            RegisterFile.getUserRegister("$a1").setValue(stackAddress + 4 + 4);
         } catch (AddressErrorException var6) {
            System.out.println("Internal Error: Memory write error occurred while storing program arguments! " + var6);
            System.exit(0);
         }

      }
   }
}
