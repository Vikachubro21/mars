package mars.mips.dump;

import java.io.File;
import java.io.IOException;
import mars.mips.hardware.AddressErrorException;

public abstract class AbstractDumpFormat implements DumpFormat {
   private String name;
   private String commandDescriptor;
   private String description;
   private String extension;

   public AbstractDumpFormat(String name, String commandDescriptor, String description, String extension) {
      this.name = name;
      this.commandDescriptor = commandDescriptor == null ? null : commandDescriptor.replaceAll(" ", "");
      this.description = description;
      this.extension = extension;
   }

   public String getFileExtension() {
      return this.extension;
   }

   public String getDescription() {
      return this.description;
   }

   public String toString() {
      return this.name;
   }

   public String getCommandDescriptor() {
      return this.commandDescriptor;
   }

   public abstract void dumpMemoryRange(File var1, int var2, int var3) throws AddressErrorException, IOException;
}
