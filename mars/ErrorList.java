package mars;

import java.io.File;
import java.util.ArrayList;

public class ErrorList {
   private ArrayList messages = new ArrayList();
   private int errorCount = 0;
   private int warningCount = 0;
   public static final String ERROR_MESSAGE_PREFIX = "Error";
   public static final String WARNING_MESSAGE_PREFIX = "Warning";
   public static final String FILENAME_PREFIX = " in ";
   public static final String LINE_PREFIX = " line ";
   public static final String POSITION_PREFIX = " column ";
   public static final String MESSAGE_SEPARATOR = ": ";

   public ArrayList getErrorMessages() {
      return this.messages;
   }

   public boolean errorsOccurred() {
      return this.errorCount != 0;
   }

   public boolean warningsOccurred() {
      return this.warningCount != 0;
   }

   public void add(ErrorMessage mess) {
      this.add(mess, this.messages.size());
   }

   public void add(ErrorMessage mess, int index) {
      if (this.errorCount <= this.getErrorLimit()) {
         if (this.errorCount == this.getErrorLimit()) {
            this.messages.add(new ErrorMessage((MIPSprogram)null, mess.getLine(), mess.getPosition(), "Error Limit of " + this.getErrorLimit() + " exceeded."));
            ++this.errorCount;
         } else {
            this.messages.add(index, mess);
            if (mess.isWarning()) {
               ++this.warningCount;
            } else {
               ++this.errorCount;
            }

         }
      }
   }

   public int errorCount() {
      return this.errorCount;
   }

   public int warningCount() {
      return this.warningCount;
   }

   public boolean errorLimitExceeded() {
      return this.errorCount > this.getErrorLimit();
   }

   public int getErrorLimit() {
      return Globals.maximumErrorMessages;
   }

   public String generateErrorReport() {
      return this.generateReport(false);
   }

   public String generateWarningReport() {
      return this.generateReport(true);
   }

   public String generateErrorAndWarningReport() {
      return this.generateWarningReport() + this.generateErrorReport();
   }

   private String generateReport(boolean isWarning) {
      StringBuffer report = new StringBuffer("");

      for(int i = 0; i < this.messages.size(); ++i) {
         ErrorMessage m = (ErrorMessage)this.messages.get(i);
         if (isWarning && m.isWarning() || !isWarning && !m.isWarning()) {
            String reportLine = (isWarning ? "Warning" : "Error") + " in ";
            if (m.getFilename().length() > 0) {
               reportLine = reportLine + (new File(m.getFilename())).getPath();
            }

            if (m.getLine() > 0) {
               reportLine = reportLine + " line " + m.getMacroExpansionHistory() + m.getLine();
            }

            if (m.getPosition() > 0) {
               reportLine = reportLine + " column " + m.getPosition();
            }

            reportLine = reportLine + ": " + m.getMessage() + "\n";
            report.append(reportLine);
         }
      }

      return report.toString();
   }
}
