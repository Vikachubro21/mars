package mars.mips.dump;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import mars.Globals;
import mars.mips.hardware.AddressErrorException;

public class BinaryDumpFormat extends AbstractDumpFormat {
   public BinaryDumpFormat() {
      super("Binary", "Binary", "Written as byte stream to binary file", (String)null);
   }

   public void dumpMemoryRange(File file, int firstAddress, int lastAddress) throws AddressErrorException, IOException {
      PrintStream out = new PrintStream(new FileOutputStream(file));

      try {
         for(int address = firstAddress; address <= lastAddress; address += 4) {
            Integer temp = Globals.memory.getRawWordOrNull(address);
            if (temp == null) {
               break;
            }

            int word = temp;

            for(int i = 0; i < 4; ++i) {
               out.write(word >>> (i << 3) & 255);
            }
         }
      } finally {
         out.close();
      }

   }
}
