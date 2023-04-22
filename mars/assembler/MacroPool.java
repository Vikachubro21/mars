package mars.assembler;

import java.util.ArrayList;
import java.util.Iterator;
import mars.MIPSprogram;

public class MacroPool {
   private MIPSprogram program;
   private ArrayList macroList;
   private Macro current;
   private ArrayList callStack;
   private ArrayList callStackOrigLines;
   private int counter;

   public MacroPool(MIPSprogram mipsProgram) {
      this.program = mipsProgram;
      this.macroList = new ArrayList();
      this.callStack = new ArrayList();
      this.callStackOrigLines = new ArrayList();
      this.current = null;
      this.counter = 0;
   }

   public void beginMacro(Token nameToken) {
      this.current = new Macro();
      this.current.setName(nameToken.getValue());
      this.current.setFromLine(nameToken.getSourceLine());
      this.current.setOriginalFromLine(nameToken.getOriginalSourceLine());
      this.current.setProgram(this.program);
   }

   public void commitMacro(Token endToken) {
      this.current.setToLine(endToken.getSourceLine());
      this.current.setOriginalToLine(endToken.getOriginalSourceLine());
      this.current.readyForCommit();
      this.macroList.add(this.current);
      this.current = null;
   }

   public Macro getMatchingMacro(TokenList tokens, int callerLine) {
      if (tokens.size() < 1) {
         return null;
      } else {
         Macro ret = null;
         Token firstToken = tokens.get(0);
         Iterator i$ = this.macroList.iterator();

         while(true) {
            Macro macro;
            do {
               do {
                  do {
                     if (!i$.hasNext()) {
                        return ret;
                     }

                     macro = (Macro)i$.next();
                  } while(!macro.getName().equals(firstToken.getValue()));
               } while(macro.getArgs().size() + 1 != tokens.size());
            } while(ret != null && ret.getFromLine() >= macro.getFromLine());

            ret = macro;
         }
      }
   }

   public boolean matchesAnyMacroName(String value) {
      Iterator i$ = this.macroList.iterator();

      Macro macro;
      do {
         if (!i$.hasNext()) {
            return false;
         }

         macro = (Macro)i$.next();
      } while(!macro.getName().equals(value));

      return true;
   }

   public Macro getCurrent() {
      return this.current;
   }

   public void setCurrent(Macro current) {
      this.current = current;
   }

   public int getNextCounter() {
      return this.counter++;
   }

   public ArrayList getCallStack() {
      return this.callStack;
   }

   public boolean pushOnCallStack(Token token) {
      int sourceLine = token.getSourceLine();
      int origSourceLine = token.getOriginalSourceLine();
      if (this.callStack.contains(sourceLine)) {
         return true;
      } else {
         this.callStack.add(sourceLine);
         this.callStackOrigLines.add(origSourceLine);
         return false;
      }
   }

   public void popFromCallStack() {
      this.callStack.remove(this.callStack.size() - 1);
      this.callStackOrigLines.remove(this.callStackOrigLines.size() - 1);
   }

   public String getExpansionHistory() {
      String ret = "";

      for(int i = 0; i < this.callStackOrigLines.size(); ++i) {
         if (i > 0) {
            ret = ret + "->";
         }

         ret = ret + ((Integer)this.callStackOrigLines.get(i)).toString();
      }

      return ret;
   }
}
