package mars.assembler;

import java.util.ArrayList;
import mars.ErrorList;
import mars.ErrorMessage;
import mars.Globals;
import mars.util.Binary;

public class SymbolTable {
   private static String startLabel = "main";
   private String filename;
   private ArrayList table;
   public static final int NOT_FOUND = -1;

   public SymbolTable(String filename) {
      this.filename = filename;
      this.table = new ArrayList();
   }

   public void addSymbol(Token token, int address, boolean b, ErrorList errors) {
      String label = token.getValue();
      if (this.getSymbol(label) != null) {
         errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "label \"" + label + "\" already defined"));
      } else {
         Symbol s = new Symbol(label, address, b);
         this.table.add(s);
         if (Globals.debug) {
            System.out.println("The symbol " + label + " with address " + address + " has been added to the " + this.filename + " symbol table.");
         }
      }

   }

   public void removeSymbol(Token token) {
      String label = token.getValue();

      for(int i = 0; i < this.table.size(); ++i) {
         if (((Symbol)((Symbol)this.table.get(i))).getName().equals(label)) {
            this.table.remove(i);
            if (Globals.debug) {
               System.out.println("The symbol " + label + " has been removed from the " + this.filename + " symbol table.");
            }
            break;
         }
      }

   }

   public int getAddress(String s) {
      for(int i = 0; i < this.table.size(); ++i) {
         if (((Symbol)((Symbol)this.table.get(i))).getName().equals(s)) {
            return ((Symbol)this.table.get(i)).getAddress();
         }
      }

      return -1;
   }

   public int getAddressLocalOrGlobal(String s) {
      int address = this.getAddress(s);
      return address == -1 ? Globals.symbolTable.getAddress(s) : address;
   }

   public Symbol getSymbol(String s) {
      for(int i = 0; i < this.table.size(); ++i) {
         if (((Symbol)((Symbol)this.table.get(i))).getName().equals(s)) {
            return (Symbol)this.table.get(i);
         }
      }

      return null;
   }

   public Symbol getSymbolGivenAddress(String s) {
      int address;
      try {
         address = Binary.stringToInt(s);
      } catch (NumberFormatException var4) {
         return null;
      }

      for(int i = 0; i < this.table.size(); ++i) {
         if (((Symbol)((Symbol)this.table.get(i))).getAddress() == address) {
            return (Symbol)this.table.get(i);
         }
      }

      return null;
   }

   public Symbol getSymbolGivenAddressLocalOrGlobal(String s) {
      Symbol sym = this.getSymbolGivenAddress(s);
      return sym == null ? Globals.symbolTable.getSymbolGivenAddress(s) : sym;
   }

   public ArrayList getDataSymbols() {
      ArrayList list = new ArrayList();

      for(int i = 0; i < this.table.size(); ++i) {
         if (((Symbol)this.table.get(i)).getType()) {
            list.add(this.table.get(i));
         }
      }

      return list;
   }

   public ArrayList getTextSymbols() {
      ArrayList list = new ArrayList();

      for(int i = 0; i < this.table.size(); ++i) {
         if (!((Symbol)this.table.get(i)).getType()) {
            list.add(this.table.get(i));
         }
      }

      return list;
   }

   public ArrayList getAllSymbols() {
      ArrayList list = new ArrayList();

      for(int i = 0; i < this.table.size(); ++i) {
         list.add(this.table.get(i));
      }

      return list;
   }

   public int getSize() {
      return this.table.size();
   }

   public void clear() {
      this.table = new ArrayList();
   }

   public void fixSymbolTableAddress(int originalAddress, int replacementAddress) {
      for(Symbol label = this.getSymbolGivenAddress(Integer.toString(originalAddress)); label != null; label = this.getSymbolGivenAddress(Integer.toString(originalAddress))) {
         label.setAddress(replacementAddress);
      }

   }

   public static String getStartLabel() {
      return startLabel;
   }
}
