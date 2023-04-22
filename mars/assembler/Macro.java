package mars.assembler;

import java.util.ArrayList;
import java.util.Collections;
import mars.ErrorList;
import mars.ErrorMessage;
import mars.MIPSprogram;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.RegisterFile;

public class Macro {
   private String name = "";
   private MIPSprogram program = null;
   private ArrayList labels;
   private int fromLine;
   private int toLine;
   private int origFromLine;
   private int origToLine;
   private ArrayList args;

   public Macro() {
      this.fromLine = this.toLine = 0;
      this.origFromLine = this.origToLine = 0;
      this.args = new ArrayList();
      this.labels = new ArrayList();
   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public MIPSprogram getProgram() {
      return this.program;
   }

   public void setProgram(MIPSprogram program) {
      this.program = program;
   }

   public int getFromLine() {
      return this.fromLine;
   }

   public int getOriginalFromLine() {
      return this.origFromLine;
   }

   public void setFromLine(int fromLine) {
      this.fromLine = fromLine;
   }

   public void setOriginalFromLine(int origFromLine) {
      this.origFromLine = origFromLine;
   }

   public int getToLine() {
      return this.toLine;
   }

   public int getOriginalToLine() {
      return this.origToLine;
   }

   public void setToLine(int toLine) {
      this.toLine = toLine;
   }

   public void setOriginalToLine(int origToLine) {
      this.origToLine = origToLine;
   }

   public ArrayList getArgs() {
      return this.args;
   }

   public void setArgs(ArrayList args) {
      this.args = args;
   }

   public boolean equals(Object obj) {
      if (!(obj instanceof Macro)) {
         return super.equals(obj);
      } else {
         Macro macro = (Macro)obj;
         return macro.getName().equals(this.name) && macro.args.size() == this.args.size();
      }
   }

   public void addArg(String value) {
      this.args.add(value);
   }

   public String getSubstitutedLine(int line, TokenList args, long counter, ErrorList errors) {
      TokenList tokens = (TokenList)this.program.getTokenList().get(line - 1);
      String s = this.program.getSourceLine(line);

      for(int i = tokens.size() - 1; i >= 0; --i) {
         Token token = tokens.get(i);
         if (!tokenIsMacroParameter(token.getValue(), true)) {
            if (this.tokenIsMacroLabel(token.getValue())) {
               String substitute = token.getValue() + "_M" + counter;
               s = this.replaceToken(s, token, substitute);
            }
         } else {
            int repl = -1;

            for(int j = 0; j < this.args.size(); ++j) {
               if (((String)this.args.get(j)).equals(token.getValue())) {
                  repl = j;
                  break;
               }
            }

            String substitute = token.getValue();
            if (repl != -1) {
               substitute = args.get(repl + 1).toString();
            } else {
               errors.add(new ErrorMessage(this.program, token.getSourceLine(), token.getStartPos(), "Unknown macro parameter"));
            }

            s = this.replaceToken(s, token, substitute);
         }
      }

      return s;
   }

   private boolean tokenIsMacroLabel(String value) {
      return Collections.binarySearch(this.labels, value) >= 0;
   }

   private String replaceToken(String source, Token tokenToBeReplaced, String substitute) {
      String stringToBeReplaced = tokenToBeReplaced.getValue();
      int pos = source.indexOf(stringToBeReplaced);
      return pos < 0 ? source : source.substring(0, pos) + substitute + source.substring(pos + stringToBeReplaced.length());
   }

   public static boolean tokenIsMacroParameter(String tokenValue, boolean acceptSpimStyleParameters) {
      if (acceptSpimStyleParameters && tokenValue.length() > 0 && tokenValue.charAt(0) == '$' && RegisterFile.getUserRegister(tokenValue) == null && Coprocessor0.getRegister(tokenValue) == null && Coprocessor1.getRegister(tokenValue) == null) {
         return true;
      } else {
         return tokenValue.length() > 1 && tokenValue.charAt(0) == '%';
      }
   }

   public void addLabel(String value) {
      this.labels.add(value);
   }

   public void readyForCommit() {
      Collections.sort(this.labels);
   }
}
