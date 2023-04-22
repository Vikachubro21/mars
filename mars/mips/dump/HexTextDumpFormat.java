package mars.mips.dump;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import mars.Globals;
import mars.mips.hardware.AddressErrorException;

public class HexTextDumpFormat extends AbstractDumpFormat {
   public HexTextDumpFormat() {
      super("Hexadecimal Text", "HexText", "Written as hex characters to text file", (String)null);
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

            for(string = Integer.toHexString(temp); string.length() < 8; string = '0' + string) {
            }

            out.println(string);
         }
      } finally {
         out.close();
      }

   }
}
