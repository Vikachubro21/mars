package mars.mips.dump;

import java.io.File;
import java.io.IOException;
import mars.mips.hardware.AddressErrorException;

public interface DumpFormat {
   String getFileExtension();

   String getDescription();

   String getCommandDescriptor();

   String toString();

   void dumpMemoryRange(File var1, int var2, int var3) throws AddressErrorException, IOException;
}
