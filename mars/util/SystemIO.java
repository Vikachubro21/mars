package mars.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import mars.Globals;

public class SystemIO {
   public static final int SYSCALL_BUFSIZE = 128;
   public static final int SYSCALL_MAXFILES = 32;
   public static String fileErrorString = new String("File operation OK");
   private static final int O_RDONLY = 0;
   private static final int O_WRONLY = 1;
   private static final int O_RDWR = 2;
   private static final int O_APPEND = 8;
   private static final int O_CREAT = 512;
   private static final int O_TRUNC = 1024;
   private static final int O_EXCL = 2048;
   private static final int STDIN = 0;
   private static final int STDOUT = 1;
   private static final int STDERR = 2;
   private static BufferedReader inputReader = null;

   public static int readInteger(int serviceNumber) {
      String input = "0";
      if (Globals.getGui() == null) {
         try {
            input = getInputReader().readLine();
         } catch (IOException var3) {
         }
      } else if (Globals.getSettings().getBooleanSetting(17)) {
         input = Globals.getGui().getMessagesPane().getInputString("Enter an integer value (syscall " + serviceNumber + ")");
      } else {
         input = Globals.getGui().getMessagesPane().getInputString(-1);
      }

      return new Integer(input.trim());
   }

   public static float readFloat(int serviceNumber) {
      String input = "0";
      if (Globals.getGui() == null) {
         try {
            input = getInputReader().readLine();
         } catch (IOException var3) {
         }
      } else if (Globals.getSettings().getBooleanSetting(17)) {
         input = Globals.getGui().getMessagesPane().getInputString("Enter a float value (syscall " + serviceNumber + ")");
      } else {
         input = Globals.getGui().getMessagesPane().getInputString(-1);
      }

      return new Float(input.trim());
   }

   public static double readDouble(int serviceNumber) {
      String input = "0";
      if (Globals.getGui() == null) {
         try {
            input = getInputReader().readLine();
         } catch (IOException var3) {
         }
      } else if (Globals.getSettings().getBooleanSetting(17)) {
         input = Globals.getGui().getMessagesPane().getInputString("Enter a double value (syscall " + serviceNumber + ")");
      } else {
         input = Globals.getGui().getMessagesPane().getInputString(-1);
      }

      return new Double(input.trim());
   }

   public static void printString(String string) {
      if (Globals.getGui() == null) {
         System.out.print(string);
      } else {
         Globals.getGui().getMessagesPane().postRunMessage(string);
      }

   }

   public static String readString(int serviceNumber, int maxLength) {
      String input = "";
      if (Globals.getGui() == null) {
         try {
            input = getInputReader().readLine();
         } catch (IOException var4) {
         }
      } else if (Globals.getSettings().getBooleanSetting(17)) {
         input = Globals.getGui().getMessagesPane().getInputString("Enter a string of maximum length " + maxLength + " (syscall " + serviceNumber + ")");
      } else {
         input = Globals.getGui().getMessagesPane().getInputString(maxLength);
         if (input.endsWith("\n")) {
            input = input.substring(0, input.length() - 1);
         }
      }

      if (input.length() > maxLength) {
         return maxLength <= 0 ? "" : input.substring(0, maxLength);
      } else {
         return input;
      }
   }

   public static int readChar(int serviceNumber) {
      String input = "0";
      if (Globals.getGui() == null) {
         try {
            input = getInputReader().readLine();
         } catch (IOException var5) {
         }
      } else if (Globals.getSettings().getBooleanSetting(17)) {
         input = Globals.getGui().getMessagesPane().getInputString("Enter a character value (syscall " + serviceNumber + ")");
      } else {
         input = Globals.getGui().getMessagesPane().getInputString(1);
      }

      try {
         int returnValue = input.charAt(0);
         return returnValue;
      } catch (IndexOutOfBoundsException var4) {
         throw var4;
      }
   }

   public static int writeToFile(int fd, byte[] myBuffer, int lengthRequested) {
      if ((fd == 1 || fd == 2) && Globals.getGui() != null) {
         String data = new String(myBuffer);
         Globals.getGui().getMessagesPane().postRunMessage(data);
         return data.length();
      } else if (!SystemIO.FileIOData.fdInUse(fd, 1)) {
         fileErrorString = new String("File descriptor " + fd + " is not open for writing");
         return -1;
      } else {
         OutputStream outputStream = (OutputStream)SystemIO.FileIOData.getStreamInUse(fd);

         try {
            for(int ii = 0; ii < lengthRequested; ++ii) {
               outputStream.write(myBuffer[ii]);
            }

            outputStream.flush();
            return lengthRequested;
         } catch (IOException var5) {
            fileErrorString = new String("IO Exception on write of file with fd " + fd);
            return -1;
         } catch (IndexOutOfBoundsException var6) {
            fileErrorString = new String("IndexOutOfBoundsException on write of file with fd" + fd);
            return -1;
         }
      }
   }

   public static int readFromFile(int fd, byte[] myBuffer, int lengthRequested) {
      if (fd == 0 && Globals.getGui() != null) {
         String input = Globals.getGui().getMessagesPane().getInputString(lengthRequested);
         byte[] bytesRead = input.getBytes();

         for(int i = 0; i < myBuffer.length; ++i) {
            myBuffer[i] = i < bytesRead.length ? bytesRead[i] : 0;
         }

         return Math.min(myBuffer.length, bytesRead.length);
      } else if (!SystemIO.FileIOData.fdInUse(fd, 0)) {
         fileErrorString = new String("File descriptor " + fd + " is not open for reading");
         return -1;
      } else {
         InputStream InputStream = (InputStream)SystemIO.FileIOData.getStreamInUse(fd);

         try {
            int retValue = InputStream.read(myBuffer, 0, lengthRequested);
            if (retValue == -1) {
               retValue = 0;
            }

            return retValue;
         } catch (IOException var7) {
            fileErrorString = new String("IO Exception on read of file with fd " + fd);
            return -1;
         } catch (IndexOutOfBoundsException var8) {
            fileErrorString = new String("IndexOutOfBoundsException on read of file with fd" + fd);
            return -1;
         }
      }
   }

   public static int openFile(String filename, int flags) {
      char[] var10000 = new char[]{' '};
      int fdToUse = SystemIO.FileIOData.nowOpening(filename, flags);
      int retValue = fdToUse;
      if (fdToUse < 0) {
         return -1;
      } else {
         if (flags == 0) {
            try {
               FileInputStream inputStream = new FileInputStream(filename);
               SystemIO.FileIOData.setStreamInUse(fdToUse, inputStream);
            } catch (FileNotFoundException var9) {
               fileErrorString = new String("File " + filename + " not found, open for input.");
               retValue = -1;
            }
         } else if ((flags & 1) != 0) {
            try {
               FileOutputStream outputStream = new FileOutputStream(filename, (flags & 8) != 0);
               SystemIO.FileIOData.setStreamInUse(fdToUse, outputStream);
            } catch (FileNotFoundException var8) {
               fileErrorString = new String("File " + filename + " not found, open for output.");
               retValue = -1;
            }
         }

         return retValue;
      }
   }

   public static void closeFile(int fd) {
      SystemIO.FileIOData.close(fd);
   }

   public static void resetFiles() {
      SystemIO.FileIOData.resetFiles();
   }

   public static String getFileErrorMessage() {
      return fileErrorString;
   }

   private static BufferedReader getInputReader() {
      if (inputReader == null) {
         inputReader = new BufferedReader(new InputStreamReader(System.in));
      }

      return inputReader;
   }

   private static class FileIOData {
      private static String[] fileNames = new String[32];
      private static int[] fileFlags = new int[32];
      private static Object[] streams = new Object[32];

      private static void resetFiles() {
         for(int i = 0; i < 32; ++i) {
            close(i);
         }

         setupStdio();
      }

      private static void setupStdio() {
         fileNames[0] = "STDIN";
         fileNames[1] = "STDOUT";
         fileNames[2] = "STDERR";
         fileFlags[0] = 0;
         fileFlags[1] = 1;
         fileFlags[2] = 1;
         streams[0] = System.in;
         streams[1] = System.out;
         streams[2] = System.err;
         System.out.flush();
         System.err.flush();
      }

      private static void setStreamInUse(int fd, Object s) {
         streams[fd] = s;
      }

      private static Object getStreamInUse(int fd) {
         return streams[fd];
      }

      private static boolean filenameInUse(String requestedFilename) {
         for(int i = 0; i < 32; ++i) {
            if (fileNames[i] != null && fileNames[i].equals(requestedFilename)) {
               return true;
            }
         }

         return false;
      }

      private static boolean fdInUse(int fd, int flag) {
         if (fd >= 0 && fd < 32) {
            if (fileNames[fd] != null && fileFlags[fd] == 0 && flag == 0) {
               return true;
            } else {
               return fileNames[fd] != null && (fileFlags[fd] & flag & 1) == 1;
            }
         } else {
            return false;
         }
      }

      private static void close(int fd) {
         if (fd > 2 && fd < 32) {
            fileNames[fd] = null;
            if (streams[fd] != null) {
               int keepFlag = fileFlags[fd];
               Object keepStream = streams[fd];
               fileFlags[fd] = -1;
               streams[fd] = null;

               try {
                  if (keepFlag == 0) {
                     ((FileInputStream)keepStream).close();
                  } else {
                     ((FileOutputStream)keepStream).close();
                  }
               } catch (IOException var4) {
               }
            } else {
               fileFlags[fd] = -1;
            }

         }
      }

      private static int nowOpening(String filename, int flag) {
         int i = 0;
         if (filenameInUse(filename)) {
            SystemIO.fileErrorString = new String("File name " + filename + " is already open.");
            return -1;
         } else if (flag != 0 && flag != 1 && flag != 9) {
            SystemIO.fileErrorString = new String("File name " + filename + " has unknown requested opening flag");
            return -1;
         } else {
            while(fileNames[i] != null && i < 32) {
               ++i;
            }

            if (i >= 32) {
               SystemIO.fileErrorString = new String("File name " + filename + " exceeds maximum open file limit of " + 32);
               return -1;
            } else {
               fileNames[i] = new String(filename);
               fileFlags[i] = flag;
               SystemIO.fileErrorString = new String("File operation OK");
               return i;
            }
         }
      }
   }
}
