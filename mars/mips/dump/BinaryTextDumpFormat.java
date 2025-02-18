package mars.mips.dump;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import mars.Globals;
import mars.mips.hardware.AddressErrorException;

public class BinaryTextDumpFormat extends AbstractDumpFormat {
   public BinaryTextDumpFormat() {
      super("Binary Text", "BinaryText", "Written as '0' and '1' characters to text file", (String)null);
   }

   public void dumpMemoryRange(File file, int firstAddress, int lastAddress) throws AddressErrorException, IOException {
      PrintStream out = new PrintStream(new FileOutputStream(file));
      String string = null;

      try {
         for(int address = firstAddress; address <= lastAddress; address += 4) {
            Integer temp = Globals.memory.getRawWordOrNull(address);
            if (temp == null) {
               break;
            }

            for(string = Integer.toBinaryString(temp); string.length() < 32; string = '0' + string) {
            }

            out.println(string);
         }
      } finally {
         out.close();
      }

   }
}
