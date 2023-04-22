package mars.mips.dump;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import mars.Globals;
import mars.mips.hardware.AddressErrorException;

public class IntelHexDumpFormat extends AbstractDumpFormat {
   public IntelHexDumpFormat() {
      super("Intel hex format", "HEX", "Written as Intel Hex Memory File", "hex");
   }

   public void dumpMemoryRange(File file, int firstAddress, int lastAddress) throws AddressErrorException, IOException {
      PrintStream out = new PrintStream(new FileOutputStream(file));
      String string = null;

      try {
         int address = firstAddress;

         while(true) {
            if (address <= lastAddress) {
               Integer temp = Globals.memory.getRawWordOrNull(address);
               if (temp != null) {
                  for(string = Integer.toHexString(temp); string.length() < 8; string = '0' + string) {
                  }

                  String addr;
                  for(addr = Integer.toHexString(address - firstAddress); addr.length() < 4; addr = '0' + addr) {
                  }

                  int tmp_chksum = 0;
                  tmp_chksum += 4;
                  tmp_chksum += 255 & address - firstAddress;
                  tmp_chksum += 255 & address - firstAddress >> 8;
                  tmp_chksum += 255 & temp;
                  tmp_chksum += 255 & temp >> 8;
                  tmp_chksum += 255 & temp >> 16;
                  tmp_chksum += 255 & temp >> 24;
                  tmp_chksum %= 256;
                  tmp_chksum = ~tmp_chksum + 1;
                  String chksum = Integer.toHexString(255 & tmp_chksum);
                  if (chksum.length() == 1) {
                     chksum = '0' + chksum;
                  }

                  String finalstr = ":04" + addr + "00" + string + chksum;
                  out.println(finalstr.toUpperCase());
                  address += 4;
                  continue;
               }
            }

            out.println(":00000001FF");
            return;
         }
      } finally {
         out.close();
      }
   }
}
