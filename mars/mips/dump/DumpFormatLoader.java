package mars.mips.dump;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import mars.util.FilenameFinder;

public class DumpFormatLoader {
   private static final String CLASS_PREFIX = "mars.mips.dump.";
   private static final String DUMP_DIRECTORY_PATH = "mars/mips/dump";
   private static final String SYSCALL_INTERFACE = "DumpFormat.class";
   private static final String CLASS_EXTENSION = "class";
   private static ArrayList formatList = null;

   public ArrayList loadDumpFormats() {
      if (formatList == null) {
         formatList = new ArrayList();
         ArrayList candidates = FilenameFinder.getFilenameList(this.getClass().getClassLoader(), "mars/mips/dump", "class");

         for(int i = 0; i < candidates.size(); ++i) {
            String file = (String)candidates.get(i);

            try {
               String formatClassName = "mars.mips.dump." + file.substring(0, file.indexOf("class") - 1);
               Class clas = Class.forName(formatClassName);
               if (DumpFormat.class.isAssignableFrom(clas) && !Modifier.isAbstract(clas.getModifiers()) && !Modifier.isInterface(clas.getModifiers())) {
                  formatList.add(clas.newInstance());
               }
            } catch (Exception var6) {
               System.out.println("Error instantiating DumpFormat from file " + file + ": " + var6);
            }
         }
      }

      return formatList;
   }

   public static DumpFormat findDumpFormatGivenCommandDescriptor(ArrayList formatList, String formatCommandDescriptor) {
      DumpFormat match = null;

      for(int i = 0; i < formatList.size(); ++i) {
         if (((DumpFormat)formatList.get(i)).getCommandDescriptor().equals(formatCommandDescriptor)) {
            match = (DumpFormat)formatList.get(i);
            break;
         }
      }

      return match;
   }
}
