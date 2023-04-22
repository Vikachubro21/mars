package mars.util;

import java.util.ArrayList;
import java.util.HashMap;
import mars.Globals;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;

public class MemoryDump {
   public static ArrayList dumpTriples = null;
   private static final HashMap segmentBoundMap = new HashMap();
   private static final String[] segmentNames = new String[]{".text", ".data"};
   private static int[] baseAddresses = new int[2];
   private static int[] limitAddresses = new int[2];

   public static Integer[] getSegmentBounds(String segment) {
      for(int i = 0; i < segmentNames.length; ++i) {
         if (segmentNames[i].equals(segment)) {
            Integer[] bounds = new Integer[]{new Integer(getBaseAddresses(segmentNames)[i]), new Integer(getLimitAddresses(segmentNames)[i])};
            return bounds;
         }
      }

      return null;
   }

   public static String[] getSegmentNames() {
      return segmentNames;
   }

   public static int[] getBaseAddresses(String[] segments) {
      baseAddresses[0] = Memory.textBaseAddress;
      baseAddresses[1] = Memory.dataBaseAddress;
      return baseAddresses;
   }

   public static int[] getLimitAddresses(String[] segments) {
      limitAddresses[0] = Memory.textLimitAddress;
      limitAddresses[1] = Memory.dataSegmentLimitAddress;
      return limitAddresses;
   }

   public static int getAddressOfFirstNull(int baseAddress, int limitAddress) throws AddressErrorException {
      int address;
      for(address = baseAddress; address < limitAddress && Globals.memory.getRawWordOrNull(address) != null; address += 4) {
      }

      return address;
   }
}
