package mars.mips.instructions;

import java.util.ArrayList;
import java.util.HashMap;
import mars.Globals;
import mars.mips.instructions.syscalls.Syscall;
import mars.mips.instructions.syscalls.SyscallNumberOverride;
import mars.util.FilenameFinder;

class SyscallLoader {
   private static final String CLASS_PREFIX = "mars.mips.instructions.syscalls.";
   private static final String SYSCALLS_DIRECTORY_PATH = "mars/mips/instructions/syscalls";
   private static final String SYSCALL_INTERFACE = "Syscall.class";
   private static final String SYSCALL_ABSTRACT = "AbstractSyscall.class";
   private static final String CLASS_EXTENSION = "class";
   private ArrayList syscallList;

   void loadSyscalls() {
      this.syscallList = new ArrayList();
      ArrayList candidates = FilenameFinder.getFilenameList(this.getClass().getClassLoader(), "mars/mips/instructions/syscalls", "class");
      HashMap syscalls = new HashMap();

      for(int i = 0; i < candidates.size(); ++i) {
         String file = (String)candidates.get(i);
         if (!syscalls.containsKey(file)) {
            syscalls.put(file, file);
            if (!file.equals("Syscall.class") && !file.equals("AbstractSyscall.class")) {
               try {
                  String syscallClassName = "mars.mips.instructions.syscalls." + file.substring(0, file.indexOf("class") - 1);
                  Class clas = Class.forName(syscallClassName);
                  if (Syscall.class.isAssignableFrom(clas)) {
                     Syscall syscall = (Syscall)clas.newInstance();
                     if (this.findSyscall(syscall.getNumber()) != null) {
                        throw new Exception("Duplicate service number: " + syscall.getNumber() + " already registered to " + this.findSyscall(syscall.getNumber()).getName());
                     }

                     this.syscallList.add(syscall);
                  }
               } catch (Exception var8) {
                  System.out.println("Error instantiating Syscall from file " + file + ": " + var8);
                  System.exit(0);
               }
            }
         }
      }

      this.syscallList = this.processSyscallNumberOverrides(this.syscallList);
   }

   private ArrayList processSyscallNumberOverrides(ArrayList syscallList) {
      ArrayList overrides = (new Globals()).getSyscallOverrides();

      for(int index = 0; index < overrides.size(); ++index) {
         SyscallNumberOverride override = (SyscallNumberOverride)overrides.get(index);
         boolean match = false;

         for(int i = 0; i < syscallList.size(); ++i) {
            Syscall syscall = (Syscall)syscallList.get(i);
            if (override.getName().equals(syscall.getName())) {
               syscall.setNumber(override.getNumber());
               match = true;
            }
         }

         if (!match) {
            System.out.println("Error: syscall name '" + override.getName() + "' in config file does not match any name in syscall list");
            System.exit(0);
         }
      }

      boolean duplicates = false;

      for(int i = 0; i < syscallList.size(); ++i) {
         Syscall syscallA = (Syscall)syscallList.get(i);

         for(int j = i + 1; j < syscallList.size(); ++j) {
            Syscall syscallB = (Syscall)syscallList.get(j);
            if (syscallA.getNumber() == syscallB.getNumber()) {
               System.out.println("Error: syscalls " + syscallA.getName() + " and " + syscallB.getName() + " are both assigned same number " + syscallA.getNumber());
               duplicates = true;
            }
         }
      }

      if (duplicates) {
         System.exit(0);
      }

      return syscallList;
   }

   Syscall findSyscall(int number) {
      Syscall match = null;
      if (this.syscallList == null) {
         this.loadSyscalls();
      }

      for(int index = 0; index < this.syscallList.size(); ++index) {
         Syscall service = (Syscall)this.syscallList.get(index);
         if (service.getNumber() == number) {
            match = service;
         }
      }

      return match;
   }
}
