package mars.assembler;

import java.util.ArrayList;

public class TokenList implements Cloneable {
   private ArrayList tokenList = new ArrayList();
   private String processedLine = "";

   public void setProcessedLine(String line) {
      this.processedLine = line;
   }

   public String getProcessedLine() {
      return this.processedLine;
   }

   public Token get(int pos) {
      return (Token)this.tokenList.get(pos);
   }

   public void set(int pos, Token replacement) {
      this.tokenList.set(pos, replacement);
   }

   public int size() {
      return this.tokenList.size();
   }

   public void add(Token token) {
      this.tokenList.add(token);
   }

   public void remove(int pos) {
      this.tokenList.remove(pos);
   }

   public boolean isEmpty() {
      return this.tokenList.isEmpty();
   }

   public String toString() {
      String stringified = "";

      for(int i = 0; i < this.tokenList.size(); ++i) {
         stringified = stringified + this.tokenList.get(i).toString() + " ";
      }

      return stringified;
   }

   public String toTypeString() {
      String stringified = "";

      for(int i = 0; i < this.tokenList.size(); ++i) {
         stringified = stringified + ((Token)this.tokenList.get(i)).getType().toString() + " ";
      }

      return stringified;
   }

   public Object clone() {
      try {
         TokenList t = (TokenList)super.clone();
         t.tokenList = (ArrayList)this.tokenList.clone();
         return t;
      } catch (CloneNotSupportedException var2) {
         return null;
      }
   }
}
