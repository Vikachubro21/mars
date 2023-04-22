package mars.mips.dump;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import mars.Globals;
import mars.ProgramStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.util.Binary;

public class SegmentWindowDumpFormat extends AbstractDumpFormat {
   public SegmentWindowDumpFormat() {
      super("Text/Data Segment Window", "SegmentWindow", " Text Segment Window or Data Segment Window format to text file", (String)null);
   }

   public void dumpMemoryRange(File file, int firstAddress, int lastAddress) throws AddressErrorException, IOException {
      PrintStream out = new PrintStream(new FileOutputStream(file));
      boolean hexAddresses = Globals.getSettings().getDisplayAddressesInHex();
      int address;
      if (Memory.inDataSegment(firstAddress)) {
         boolean hexValues = Globals.getSettings().getDisplayValuesInHex();
         address = 0;
         String string = "";

         try {
            for(address = firstAddress; address <= lastAddress; address += 4) {
               if (address % 8 == 0) {
                  string = (hexAddresses ? Binary.intToHexString(address) : Binary.unsignedIntToIntString(address)) + "    ";
               }

               ++address;
               Integer temp = Globals.memory.getRawWordOrNull(address);
               if (temp == null) {
                  break;
               }

               string = string + (hexValues ? Binary.intToHexString(temp) : ("           " + temp).substring(temp.toString().length())) + " ";
               if (address % 8 == 0) {
                  out.println(string);
                  string = "";
               }
            }
         } finally {
            out.close();
         }

      } else if (Memory.inTextSegment(firstAddress)) {
         out.println(" Address    Code        Basic                     Source");
         out.println();
         String string = null;

         try {
            for(address = firstAddress; address <= lastAddress; address += 4) {
               string = (hexAddresses ? Binary.intToHexString(address) : Binary.unsignedIntToIntString(address)) + "  ";
               Integer temp = Globals.memory.getRawWordOrNull(address);
               if (temp == null) {
                  break;
               }

               string = string + Binary.intToHexString(temp) + "  ";

               try {
                  ProgramStatement ps = Globals.memory.getStatement(address);
                  string = string + (ps.getPrintableBasicAssemblyStatement() + "                      ").substring(0, 22);
                  string = string + ((ps.getSource() == "" ? "" : (new Integer(ps.getSourceLine())).toString()) + "     ").substring(0, 5);
                  string = string + ps.getSource();
               } catch (AddressErrorException var19) {
               }

               out.println(string);
            }
         } finally {
            out.close();
         }

      }
   }
}
