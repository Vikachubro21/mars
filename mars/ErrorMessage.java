package mars;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mars.assembler.SourceLine;

public class ErrorMessage {
   private boolean isWarning;
   private String filename;
   private int line;
   private int position;
   private String message;
   private String macroExpansionHistory;
   public static final boolean WARNING = true;
   public static final boolean ERROR = false;

   /** @deprecated */
   @Deprecated
   public ErrorMessage(String filename, int line, int position, String message) {
      this(false, filename, line, position, message, "");
   }

   /** @deprecated */
   @Deprecated
   public ErrorMessage(String filename, int line, int position, String message, String macroExpansionHistory) {
      this(false, filename, line, position, message, macroExpansionHistory);
   }

   /** @deprecated */
   @Deprecated
   public ErrorMessage(boolean isWarning, String filename, int line, int position, String message, String macroExpansionHistory) {
      this.isWarning = isWarning;
      this.filename = filename;
      this.line = line;
      this.position = position;
      this.message = message;
      this.macroExpansionHistory = macroExpansionHistory;
   }

   public ErrorMessage(MIPSprogram sourceMIPSprogram, int line, int position, String message) {
      this(false, sourceMIPSprogram, line, position, message);
   }

   public ErrorMessage(boolean isWarning, MIPSprogram sourceMIPSprogram, int line, int position, String message) {
      this.isWarning = isWarning;
      if (sourceMIPSprogram == null) {
         this.filename = "";
         this.line = line;
      } else if (sourceMIPSprogram.getSourceLineList() == null) {
         this.filename = sourceMIPSprogram.getFilename();
         this.line = line;
      } else {
         SourceLine sourceLine = (SourceLine)sourceMIPSprogram.getSourceLineList().get(line - 1);
         this.filename = sourceLine.getFilename();
         this.line = sourceLine.getLineNumber();
      }

      this.position = position;
      this.message = message;
      this.macroExpansionHistory = getExpansionHistory(sourceMIPSprogram);
   }

   public ErrorMessage(ProgramStatement statement, String message) {
      this.isWarning = false;
      this.filename = statement.getSourceMIPSprogram() == null ? "" : statement.getSourceMIPSprogram().getFilename();
      this.position = 0;
      this.message = message;
      ArrayList defineLine = this.parseMacroHistory(statement.getSource());
      if (defineLine.size() == 0) {
         this.line = statement.getSourceLine();
         this.macroExpansionHistory = "";
      } else {
         this.line = (Integer)defineLine.get(0);
         this.macroExpansionHistory = "" + statement.getSourceLine();
      }

   }

   private ArrayList parseMacroHistory(String string) {
      Pattern pattern = Pattern.compile("<\\d+>");
      Matcher matcher = pattern.matcher(string);
      String verify = (new String(string)).trim();

      ArrayList macroHistory;
      String match;
      for(macroHistory = new ArrayList(); matcher.find(); verify = verify.substring(match.length()).trim()) {
         match = matcher.group();
         if (verify.indexOf(match) != 0) {
            break;
         }

         try {
            int line = Integer.parseInt(match.substring(1, match.length() - 1));
            macroHistory.add(line);
         } catch (NumberFormatException var8) {
            break;
         }
      }

      return macroHistory;
   }

   public String getFilename() {
      return this.filename;
   }

   public int getLine() {
      return this.line;
   }

   public int getPosition() {
      return this.position;
   }

   public String getMessage() {
      return this.message;
   }

   public boolean isWarning() {
      return this.isWarning;
   }

   public String getMacroExpansionHistory() {
      return this.macroExpansionHistory != null && this.macroExpansionHistory.length() != 0 ? this.macroExpansionHistory + "->" : "";
   }

   private static String getExpansionHistory(MIPSprogram sourceMIPSprogram) {
      return sourceMIPSprogram != null && sourceMIPSprogram.getLocalMacroPool() != null ? sourceMIPSprogram.getLocalMacroPool().getExpansionHistory() : "";
   }
}
