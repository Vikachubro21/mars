package mars.venus;

import java.io.File;
import mars.Globals;

public class FileStatus {
   public static final int NO_FILE = 0;
   public static final int NEW_NOT_EDITED = 1;
   public static final int NEW_EDITED = 2;
   public static final int NOT_EDITED = 3;
   public static final int EDITED = 4;
   public static final int RUNNABLE = 5;
   public static final int RUNNING = 6;
   public static final int TERMINATED = 7;
   public static final int OPENING = 8;
   private static int systemStatus;
   private static boolean systemAssembled;
   private static boolean systemSaved;
   private static boolean systemEdited;
   private static String systemName;
   private static File systemFile;
   private int status;
   private File file;

   public static void set(int newStatus) {
      systemStatus = newStatus;
      Globals.getGui().setMenuState(systemStatus);
   }

   public static int get() {
      return systemStatus;
   }

   public static void setAssembled(boolean b) {
      systemAssembled = b;
   }

   public static void setSaved(boolean b) {
      systemSaved = b;
   }

   public static void setEdited(boolean b) {
      systemEdited = b;
   }

   public static void setName(String s) {
      systemName = s;
   }

   public static void setFile(File f) {
      systemFile = f;
   }

   public static File getFile() {
      return systemFile;
   }

   public static String getName() {
      return systemName;
   }

   public static boolean isAssembled() {
      return systemAssembled;
   }

   public static boolean isSaved() {
      return systemSaved;
   }

   public static boolean isEdited() {
      return systemEdited;
   }

   public static void reset() {
      systemStatus = 0;
      systemName = "";
      systemAssembled = false;
      systemSaved = false;
      systemEdited = false;
      systemFile = null;
   }

   public FileStatus() {
      this(0, (String)null);
   }

   public FileStatus(int status, String pathname) {
      this.status = status;
      if (pathname == null) {
         this.file = null;
      } else {
         this.setPathname(pathname);
      }

   }

   public void setFileStatus(int newStatus) {
      this.status = newStatus;
   }

   public int getFileStatus() {
      return this.status;
   }

   public boolean isNew() {
      return this.status == 1 || this.status == 2;
   }

   public boolean hasUnsavedEdits() {
      return this.status == 2 || this.status == 4;
   }

   public void setPathname(String newPath) {
      this.file = new File(newPath);
   }

   public void setPathname(String parent, String name) {
      this.file = new File(parent, name);
   }

   public String getPathname() {
      return this.file == null ? null : this.file.getPath();
   }

   public String getFilename() {
      return this.file == null ? null : this.file.getName();
   }

   public String getParent() {
      return this.file == null ? null : this.file.getParent();
   }

   public void updateStaticFileStatus() {
      systemStatus = this.status;
      systemName = this.file.getPath();
      systemAssembled = false;
      systemSaved = this.status == 3 || this.status == 5 || this.status == 6 || this.status == 7;
      systemEdited = this.status == 2 || this.status == 4;
      systemFile = this.file;
   }
}
