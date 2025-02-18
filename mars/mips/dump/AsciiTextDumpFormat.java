package mars.mips.dump;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import mars.Globals;
import mars.mips.hardware.AddressErrorException;
import mars.util.Binary;

public class AsciiTextDumpFormat extends AbstractDumpFormat {
   public AsciiTextDumpFormat() {
      super("ASCII Text", "AsciiText", "Memory contents interpreted as ASCII characters", (String)null);
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

            out.println(Binary.intToAscii(temp));
         }
      } finally {
         out.close();
      }

   }
}
