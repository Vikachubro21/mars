package mars.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.filechooser.FileFilter;

public class FilenameFinder {
   private static final String JAR_EXTENSION = ".jar";
   private static final String FILE_URL = "file:";
   private static final String JAR_URI_PREFIX = "jar:";
   private static final boolean NO_DIRECTORIES = false;
   public static String MATCH_ALL_EXTENSIONS = "*";

   public static ArrayList getFilenameList(ClassLoader classLoader, String directoryPath, String fileExtension) {
      fileExtension = checkFileExtension(fileExtension);
      ArrayList filenameList = new ArrayList();

      try {
         Enumeration urls = classLoader.getResources(directoryPath);

         while(true) {
            while(urls.hasMoreElements()) {
               URI uri = new URI(urls.nextElement().toString());
               if (uri.toString().indexOf("jar:") == 0) {
                  uri = new URI(uri.toString().substring("jar:".length()));
               }

               File f = new File(uri.getPath());
               File[] files = f.listFiles();
               if (files == null) {
                  if (f.toString().toLowerCase().indexOf(".jar") > 0) {
                     filenameList.addAll(getListFromJar(extractJarFilename(f.toString()), directoryPath, fileExtension));
                  }
               } else {
                  FileFilter filter = getFileFilter(fileExtension, "", false);

                  for(int i = 0; i < files.length; ++i) {
                     if (filter.accept(files[i])) {
                        filenameList.add(files[i].getName());
                     }
                  }
               }
            }

            return filenameList;
         }
      } catch (URISyntaxException var10) {
         var10.printStackTrace();
         return filenameList;
      } catch (IOException var11) {
         var11.printStackTrace();
         return filenameList;
      }
   }

   public static ArrayList getFilenameList(ClassLoader classLoader, String directoryPath, ArrayList fileExtensions) {
      ArrayList filenameList = new ArrayList();
      if (fileExtensions != null && fileExtensions.size() != 0) {
         for(int i = 0; i < fileExtensions.size(); ++i) {
            String fileExtension = checkFileExtension((String)fileExtensions.get(i));
            filenameList.addAll(getFilenameList(classLoader, directoryPath, fileExtension));
         }
      } else {
         filenameList = getFilenameList(classLoader, directoryPath, "");
      }

      return filenameList;
   }

   public static ArrayList getFilenameList(String directoryPath, String fileExtension) {
      fileExtension = checkFileExtension(fileExtension);
      ArrayList filenameList = new ArrayList();
      File directory = new File(directoryPath);
      if (directory.isDirectory()) {
         File[] allFiles = directory.listFiles();
         FileFilter filter = getFileFilter(fileExtension, "", false);

         for(int i = 0; i < allFiles.length; ++i) {
            if (filter.accept(allFiles[i])) {
               filenameList.add(allFiles[i].getAbsolutePath());
            }
         }
      }

      return filenameList;
   }

   public static ArrayList getFilenameList(String directoryPath, ArrayList fileExtensions) {
      ArrayList filenameList = new ArrayList();
      if (fileExtensions != null && fileExtensions.size() != 0) {
         for(int i = 0; i < fileExtensions.size(); ++i) {
            String fileExtension = checkFileExtension((String)fileExtensions.get(i));
            filenameList.addAll(getFilenameList(directoryPath, fileExtension));
         }
      } else {
         filenameList = getFilenameList(directoryPath, "");
      }

      return filenameList;
   }

   public static ArrayList getFilenameList(ArrayList nameList, String fileExtension) {
      fileExtension = checkFileExtension(fileExtension);
      ArrayList filenameList = new ArrayList();
      FileFilter filter = getFileFilter(fileExtension, "", false);

      for(int i = 0; i < nameList.size(); ++i) {
         File file = new File((String)nameList.get(i));
         if (filter.accept(file)) {
            filenameList.add(file.getAbsolutePath());
         }
      }

      return filenameList;
   }

   public static ArrayList getFilenameList(ArrayList nameList, ArrayList fileExtensions) {
      ArrayList filenameList = new ArrayList();
      if (fileExtensions != null && fileExtensions.size() != 0) {
         for(int i = 0; i < fileExtensions.size(); ++i) {
            String fileExtension = checkFileExtension((String)fileExtensions.get(i));
            filenameList.addAll(getFilenameList(nameList, fileExtension));
         }
      } else {
         filenameList = getFilenameList(nameList, "");
      }

      return filenameList;
   }

   public static String getExtension(File file) {
      String ext = null;
      String s = file.getName();
      int i = s.lastIndexOf(46);
      if (i > 0 && i < s.length() - 1) {
         ext = s.substring(i + 1).toLowerCase();
      }

      return ext;
   }

   public static FileFilter getFileFilter(ArrayList extensions, String description, boolean acceptDirectories) {
      return new MarsFileFilter(extensions, description, acceptDirectories);
   }

   public static FileFilter getFileFilter(ArrayList extensions, String description) {
      return getFileFilter(extensions, description, true);
   }

   public static FileFilter getFileFilter(String extension, String description, boolean acceptDirectories) {
      ArrayList extensions = new ArrayList();
      extensions.add(extension);
      return new MarsFileFilter(extensions, description, acceptDirectories);
   }

   public static FileFilter getFileFilter(String extension, String description) {
      ArrayList extensions = new ArrayList();
      extensions.add(extension);
      return getFileFilter(extensions, description, true);
   }

   public static boolean fileExtensionMatch(String name, String extension) {
      return extension == null || extension.length() == 0 || name.endsWith((extension.startsWith(".") ? "" : ".") + extension);
   }

   private static ArrayList getListFromJar(String jarName, String directoryPath, String fileExtension) {
      fileExtension = checkFileExtension(fileExtension);
      ArrayList nameList = new ArrayList();
      if (jarName == null) {
         return nameList;
      } else {
         try {
            ZipFile zf = new ZipFile(new File(jarName));
            Enumeration list = zf.entries();

            while(list.hasMoreElements()) {
               ZipEntry ze = (ZipEntry)list.nextElement();
               if (ze.getName().startsWith(directoryPath + "/") && fileExtensionMatch(ze.getName(), fileExtension)) {
                  nameList.add(ze.getName().substring(ze.getName().lastIndexOf(47) + 1));
               }
            }
         } catch (Exception var7) {
            System.out.println("Exception occurred reading MarsTool list from JAR: " + var7);
         }

         return nameList;
      }
   }

   private static String extractJarFilename(String path) {
      new StringTokenizer(path, "\\/");
      if (path.toLowerCase().startsWith("file:")) {
         path = path.substring("file:".length());
      }

      int jarPosition = path.toLowerCase().indexOf(".jar");
      return jarPosition >= 0 ? path.substring(0, jarPosition + ".jar".length()) : path;
   }

   private static String checkFileExtension(String fileExtension) {
      return fileExtension != null && fileExtension.length() != 0 && fileExtension.startsWith(".") ? fileExtension.substring(1) : fileExtension;
   }

   private static class MarsFileFilter extends FileFilter {
      private ArrayList extensions;
      private String fullDescription;
      private boolean acceptDirectories;

      private MarsFileFilter(ArrayList extensions, String description, boolean acceptDirectories) {
         this.extensions = extensions;
         this.fullDescription = this.buildFullDescription(description, extensions);
         this.acceptDirectories = acceptDirectories;
      }

      private String buildFullDescription(String description, ArrayList extensions) {
         String result = description == null ? "" : description;
         if (extensions.size() > 0) {
            result = result + "  (";
         }

         for(int i = 0; i < extensions.size(); ++i) {
            String extension = (String)extensions.get(i);
            if (extension != null && extension.length() > 0) {
               result = result + (i == 0 ? "" : "; ") + "*" + (extension.charAt(0) == '.' ? "" : ".") + extension;
            }
         }

         if (extensions.size() > 0) {
            result = result + ")";
         }

         return result;
      }

      public String getDescription() {
         return this.fullDescription;
      }

      public boolean accept(File file) {
         if (file.isDirectory()) {
            return this.acceptDirectories;
         } else {
            String fileExtension = FilenameFinder.getExtension(file);
            if (fileExtension != null) {
               for(int i = 0; i < this.extensions.size(); ++i) {
                  String extension = FilenameFinder.checkFileExtension((String)this.extensions.get(i));
                  if (extension.equals(FilenameFinder.MATCH_ALL_EXTENSIONS) || fileExtension.equals(extension)) {
                     return true;
                  }
               }
            }

            return false;
         }
      }

      // $FF: synthetic method
      MarsFileFilter(ArrayList x0, String x1, boolean x2, Object x3) {
         this(x0, x1, x2);
      }
   }
}
