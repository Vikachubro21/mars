package mars;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import mars.assembler.SymbolTable;
import mars.mips.hardware.Memory;
import mars.mips.instructions.InstructionSet;
import mars.mips.instructions.syscalls.SyscallNumberOverride;
import mars.util.PropertiesFile;
import mars.venus.VenusUI;

public class Globals {
   private static String configPropertiesFile = "Config";
   private static String syscallPropertiesFile = "Syscall";
   public static InstructionSet instructionSet;
   public static MIPSprogram program;
   public static SymbolTable symbolTable;
   public static Memory memory;
   public static Object memoryAndRegistersLock = new Object();
   public static boolean debug = false;
   static Settings settings;
   public static String userInputAlert = "**** user input : ";
   public static final String imagesPath = "/images/";
   public static final String helpPath = "/help/";
   private static boolean initialized = false;
   static VenusUI gui = null;
   public static final String version = "4.5";
   public static final ArrayList fileExtensions = getFileExtensions();
   public static final int maximumMessageCharacters = getMessageLimit();
   public static final int maximumErrorMessages = getErrorLimit();
   public static final int maximumBacksteps = getBackstepLimit();
   public static final String copyrightYears = getCopyrightYears();
   public static final String copyrightHolders = getCopyrightHolders();
   public static final String ASCII_NON_PRINT = getAsciiNonPrint();
   public static final String[] ASCII_TABLE = getAsciiStrings();
   public static int exitCode = 0;
   public static Color background = new Color(89, 89, 89);
   public static boolean runSpeedPanelExists = false;

   private static String getCopyrightYears() {
      return "2003-2014";
   }

   private static String getCopyrightHolders() {
      return "Pete Sanderson and Kenneth Vollmar";
   }

   public static void setGui(VenusUI g) {
      gui = g;
   }

   public static VenusUI getGui() {
      return gui;
   }

   public static Settings getSettings() {
      return settings;
   }

   public static void initialize(boolean gui) {
      if (!initialized) {
         memory = Memory.getInstance();
         instructionSet = new InstructionSet();
         instructionSet.populate();
         symbolTable = new SymbolTable("global");
         settings = new Settings(gui);
         initialized = true;
         debug = false;
         memory.clear();
      }

   }

   private static int getMessageLimit() {
      return getIntegerProperty(configPropertiesFile, "MessageLimit", 1000000);
   }

   private static int getErrorLimit() {
      return getIntegerProperty(configPropertiesFile, "ErrorLimit", 200);
   }

   private static int getBackstepLimit() {
      return getIntegerProperty(configPropertiesFile, "BackstepLimit", 1000);
   }

   public static String getAsciiNonPrint() {
      String anp = getPropertyEntry(configPropertiesFile, "AsciiNonPrint");
      return anp == null ? "." : (anp.equals("space") ? " " : anp);
   }

   public static String[] getAsciiStrings() {
      String let = getPropertyEntry(configPropertiesFile, "AsciiTable");
      String placeHolder = getAsciiNonPrint();
      String[] lets = let.split(" +");
      int maxLength = 0;

      for(int i = 0; i < lets.length; ++i) {
         if (lets[i].equals("null")) {
            lets[i] = placeHolder;
         }

         if (lets[i].equals("space")) {
            lets[i] = " ";
         }

         if (lets[i].length() > maxLength) {
            maxLength = lets[i].length();
         }
      }

      String padding = "        ";
      ++maxLength;

      for(int i = 0; i < lets.length; ++i) {
         lets[i] = padding.substring(0, maxLength - lets[i].length()) + lets[i];
      }

      return lets;
   }

   private static int getIntegerProperty(String propertiesFile, String propertyName, int defaultValue) {
      int limit = defaultValue;
      Properties properties = PropertiesFile.loadPropertiesFromFile(propertiesFile);

      try {
         limit = Integer.parseInt(properties.getProperty(propertyName, Integer.toString(defaultValue)));
      } catch (NumberFormatException var6) {
      }

      return limit;
   }

   private static ArrayList getFileExtensions() {
      ArrayList extensionsList = new ArrayList();
      String extensions = getPropertyEntry(configPropertiesFile, "Extensions");
      if (extensions != null) {
         StringTokenizer st = new StringTokenizer(extensions);

         while(st.hasMoreTokens()) {
            extensionsList.add(st.nextToken());
         }
      }

      return extensionsList;
   }

   public static ArrayList getExternalTools() {
      ArrayList toolsList = new ArrayList();
      String delimiter = ";";
      String tools = getPropertyEntry(configPropertiesFile, "ExternalTools");
      if (tools != null) {
         StringTokenizer st = new StringTokenizer(tools, delimiter);

         while(st.hasMoreTokens()) {
            toolsList.add(st.nextToken());
         }
      }

      return toolsList;
   }

   public static String getPropertyEntry(String propertiesFile, String propertyName) {
      return PropertiesFile.loadPropertiesFromFile(propertiesFile).getProperty(propertyName);
   }

   public ArrayList getSyscallOverrides() {
      ArrayList overrides = new ArrayList();
      Properties properties = PropertiesFile.loadPropertiesFromFile(syscallPropertiesFile);
      Enumeration keys = properties.keys();

      while(keys.hasMoreElements()) {
         String key = (String)keys.nextElement();
         overrides.add(new SyscallNumberOverride(key, properties.getProperty(key)));
      }

      return overrides;
   }
}
