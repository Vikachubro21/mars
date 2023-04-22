package mars.mips.hardware;

import java.util.ArrayList;
import java.util.Iterator;
import mars.Globals;

public class MemoryConfigurations {
   private static ArrayList configurations = null;
   private static MemoryConfiguration defaultConfiguration;
   private static MemoryConfiguration currentConfiguration;
   private static final String[] configurationItemNames = new String[]{".text base address", "data segment base address", ".extern base address", "global pointer $gp", ".data base address", "heap base address", "stack pointer $sp", "stack base address", "user space high address", "kernel space base address", ".ktext base address", "exception handler address", ".kdata base address", "MMIO base address", "kernel space high address", "data segment limit address", "text limit address", "kernel data segment limit address", "kernel text limit address", "stack limit address", "memory map limit address"};
   private static int[] defaultConfigurationItemValues = new int[]{4194304, 268435456, 268435456, 268468224, 268500992, 268697600, 2147479548, 2147483644, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, -2147483264, -1879048192, -65536, -1, Integer.MAX_VALUE, 268435452, -65537, -1879048196, 268697600, -1};
   private static int[] dataBasedCompactConfigurationItemValues = new int[]{12288, 0, 4096, 6144, 0, 8192, 12284, 12284, 16383, 16384, 16384, 16768, 20480, 32512, 32767, 12287, 16380, 32511, 20476, 8192, 32767};
   private static int[] textBasedCompactConfigurationItemValues = new int[]{0, 4096, 4096, 6144, 8192, 12288, 16380, 16380, 16383, 16384, 16384, 16768, 20480, 32512, 32767, 16383, 4092, 32511, 20476, 12288, 32767};

   public static void buildConfigurationCollection() {
      if (configurations == null) {
         configurations = new ArrayList();
         configurations.add(new MemoryConfiguration("Default", "Default", configurationItemNames, defaultConfigurationItemValues));
         configurations.add(new MemoryConfiguration("CompactDataAtZero", "Compact, Data at Address 0", configurationItemNames, dataBasedCompactConfigurationItemValues));
         configurations.add(new MemoryConfiguration("CompactTextAtZero", "Compact, Text at Address 0", configurationItemNames, textBasedCompactConfigurationItemValues));
         defaultConfiguration = (MemoryConfiguration)configurations.get(0);
         currentConfiguration = defaultConfiguration;
         setCurrentConfiguration(getConfigurationByName(Globals.getSettings().getMemoryConfiguration()));
      }

   }

   public static Iterator getConfigurationsIterator() {
      if (configurations == null) {
         buildConfigurationCollection();
      }

      return configurations.iterator();
   }

   public static MemoryConfiguration getConfigurationByName(String name) {
      Iterator configurationsIterator = getConfigurationsIterator();

      MemoryConfiguration config;
      do {
         if (!configurationsIterator.hasNext()) {
            return null;
         }

         config = (MemoryConfiguration)configurationsIterator.next();
      } while(!name.equals(config.getConfigurationIdentifier()));

      return config;
   }

   public static MemoryConfiguration getDefaultConfiguration() {
      if (defaultConfiguration == null) {
         buildConfigurationCollection();
      }

      return defaultConfiguration;
   }

   public static MemoryConfiguration getCurrentConfiguration() {
      if (currentConfiguration == null) {
         buildConfigurationCollection();
      }

      return currentConfiguration;
   }

   public static boolean setCurrentConfiguration(MemoryConfiguration config) {
      if (config == null) {
         return false;
      } else if (config != currentConfiguration) {
         currentConfiguration = config;
         Globals.memory.clear();
         RegisterFile.getUserRegister("$gp").changeResetValue(config.getGlobalPointer());
         RegisterFile.getUserRegister("$sp").changeResetValue(config.getStackPointer());
         RegisterFile.getProgramCounterRegister().changeResetValue(config.getTextBaseAddress());
         RegisterFile.initializeProgramCounter(config.getTextBaseAddress());
         RegisterFile.resetRegisters();
         return true;
      } else {
         return false;
      }
   }

   public static int getDefaultTextBaseAddress() {
      return defaultConfigurationItemValues[0];
   }

   public static int getDefaultDataSegmentBaseAddress() {
      return defaultConfigurationItemValues[1];
   }

   public static int getDefaultExternBaseAddress() {
      return defaultConfigurationItemValues[2];
   }

   public static int getDefaultGlobalPointer() {
      return defaultConfigurationItemValues[3];
   }

   public static int getDefaultDataBaseAddress() {
      return defaultConfigurationItemValues[4];
   }

   public static int getDefaultHeapBaseAddress() {
      return defaultConfigurationItemValues[5];
   }

   public static int getDefaultStackPointer() {
      return defaultConfigurationItemValues[6];
   }

   public static int getDefaultStackBaseAddress() {
      return defaultConfigurationItemValues[7];
   }

   public static int getDefaultUserHighAddress() {
      return defaultConfigurationItemValues[8];
   }

   public static int getDefaultKernelBaseAddress() {
      return defaultConfigurationItemValues[9];
   }

   public static int getDefaultKernelTextBaseAddress() {
      return defaultConfigurationItemValues[10];
   }

   public static int getDefaultExceptionHandlerAddress() {
      return defaultConfigurationItemValues[11];
   }

   public static int getDefaultKernelDataBaseAddress() {
      return defaultConfigurationItemValues[12];
   }

   public static int getDefaultMemoryMapBaseAddress() {
      return defaultConfigurationItemValues[13];
   }

   public static int getDefaultKernelHighAddress() {
      return defaultConfigurationItemValues[14];
   }

   public int getDefaultDataSegmentLimitAddress() {
      return defaultConfigurationItemValues[15];
   }

   public int getDefaultTextLimitAddress() {
      return defaultConfigurationItemValues[16];
   }

   public int getDefaultKernelDataSegmentLimitAddress() {
      return defaultConfigurationItemValues[17];
   }

   public int getDefaultKernelTextLimitAddress() {
      return defaultConfigurationItemValues[18];
   }

   public int getDefaultStackLimitAddress() {
      return defaultConfigurationItemValues[19];
   }

   public int getMemoryMapLimitAddress() {
      return defaultConfigurationItemValues[20];
   }
}
