package mars.assembler;

import java.util.ArrayList;

public final class Directives {
   private static ArrayList directiveList = new ArrayList();
   public static final Directives DATA = new Directives(".data", "Subsequent items stored in Data segment at next available address");
   public static final Directives TEXT = new Directives(".text", "Subsequent items (instructions) stored in Text segment at next available address");
   public static final Directives WORD = new Directives(".word", "Store the listed value(s) as 32 bit words on word boundary");
   public static final Directives ASCII = new Directives(".ascii", "Store the string in the Data segment but do not add null terminator");
   public static final Directives ASCIIZ = new Directives(".asciiz", "Store the string in the Data segment and add null terminator");
   public static final Directives BYTE = new Directives(".byte", "Store the listed value(s) as 8 bit bytes");
   public static final Directives ALIGN = new Directives(".align", "Align next data item on specified byte boundary (0=byte, 1=half, 2=word, 3=double)");
   public static final Directives HALF = new Directives(".half", "Store the listed value(s) as 16 bit halfwords on halfword boundary");
   public static final Directives SPACE = new Directives(".space", "Reserve the next specified number of bytes in Data segment");
   public static final Directives DOUBLE = new Directives(".double", "Store the listed value(s) as double precision floating point");
   public static final Directives FLOAT = new Directives(".float", "Store the listed value(s) as single precision floating point");
   public static final Directives EXTERN = new Directives(".extern", "Declare the listed label and byte length to be a global data field");
   public static final Directives KDATA = new Directives(".kdata", "Subsequent items stored in Kernel Data segment at next available address");
   public static final Directives KTEXT = new Directives(".ktext", "Subsequent items (instructions) stored in Kernel Text segment at next available address");
   public static final Directives GLOBL = new Directives(".globl", "Declare the listed label(s) as global to enable referencing from other files");
   public static final Directives SET = new Directives(".set", "Set assembler variables.  Currently ignored but included for SPIM compatability");
   public static final Directives EQV = new Directives(".eqv", "Substitute second operand for first. First operand is symbol, second operand is expression (like #define)");
   public static final Directives MACRO = new Directives(".macro", "Begin macro definition.  See .end_macro");
   public static final Directives END_MACRO = new Directives(".end_macro", "End macro definition.  See .macro");
   public static final Directives INCLUDE = new Directives(".include", "Insert the contents of the specified file.  Put filename in quotes.");
   private String descriptor;
   private String description;

   private Directives() {
      this.descriptor = "generic";
      this.description = "";
      directiveList.add(this);
   }

   private Directives(String name, String description) {
      this.descriptor = name;
      this.description = description;
      directiveList.add(this);
   }

   public static Directives matchDirective(String str) {
      for(int i = 0; i < directiveList.size(); ++i) {
         Directives match = (Directives)directiveList.get(i);
         if (str.equalsIgnoreCase(match.descriptor)) {
            return match;
         }
      }

      return null;
   }

   public static ArrayList prefixMatchDirectives(String str) {
      ArrayList matches = null;

      for(int i = 0; i < directiveList.size(); ++i) {
         if (((Directives)directiveList.get(i)).descriptor.toLowerCase().startsWith(str.toLowerCase())) {
            if (matches == null) {
               matches = new ArrayList();
            }

            matches.add(directiveList.get(i));
         }
      }

      return matches;
   }

   public String toString() {
      return this.descriptor;
   }

   public String getName() {
      return this.descriptor;
   }

   public String getDescription() {
      return this.description;
   }

   public static ArrayList getDirectiveList() {
      return directiveList;
   }

   public static boolean isIntegerDirective(Directives direct) {
      return direct == WORD || direct == HALF || direct == BYTE;
   }

   public static boolean isFloatingDirective(Directives direct) {
      return direct == FLOAT || direct == DOUBLE;
   }
}
