package mars.mips.instructions;

import java.util.ArrayList;
import java.util.StringTokenizer;
import mars.Globals;
import mars.MIPSprogram;
import mars.assembler.Symbol;
import mars.assembler.TokenList;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.RegisterFile;
import mars.util.Binary;

public class ExtendedInstruction extends Instruction {
   private ArrayList translationStrings;
   private ArrayList compactTranslationStrings;

   public ExtendedInstruction(String example, String translation, String compactTranslation, String description) {
      this.exampleFormat = example;
      this.description = description;
      this.mnemonic = this.extractOperator(example);
      this.createExampleTokenList();
      this.translationStrings = this.buildTranslationList(translation);
      this.compactTranslationStrings = this.buildTranslationList(compactTranslation);
   }

   public ExtendedInstruction(String example, String translation, String description) {
      this.exampleFormat = example;
      this.description = description;
      this.mnemonic = this.extractOperator(example);
      this.createExampleTokenList();
      this.translationStrings = this.buildTranslationList(translation);
      this.compactTranslationStrings = null;
   }

   public ExtendedInstruction(String example, String translation) {
      this(example, translation, "");
   }

   public int getInstructionLength() {
      return this.getInstructionLength(this.translationStrings);
   }

   public ArrayList getBasicIntructionTemplateList() {
      return this.translationStrings;
   }

   public int getCompactInstructionLength() {
      return this.getInstructionLength(this.compactTranslationStrings);
   }

   public boolean hasCompactTranslation() {
      return this.compactTranslationStrings != null;
   }

   public ArrayList getCompactBasicIntructionTemplateList() {
      return this.compactTranslationStrings;
   }

   public static String makeTemplateSubstitutions(MIPSprogram program, String template, TokenList theTokenList) {
      String instruction = template;
      if (template.indexOf("DBNOP") >= 0) {
         return Globals.getSettings().getDelayedBranchingEnabled() ? "nop" : "";
      } else {
         int index;
         int op;
         String value;
         int val;
         int add;
         int extra;
         for(op = 1; op < theTokenList.size(); ++op) {
            instruction = substitute(instruction, "RG" + op, theTokenList.get(op).getValue());
            instruction = substitute(instruction, "OP" + op, theTokenList.get(op).getValue());
            if ((index = instruction.indexOf("LH" + op + "P")) >= 0) {
               value = theTokenList.get(op).getValue();
               val = 0;
               add = instruction.charAt(index + 4) - 48;

               try {
                  val = Binary.stringToInt(value) + add;
               } catch (NumberFormatException var27) {
               }

               extra = Binary.bitValue(val, 15);
               instruction = substitute(instruction, "LH" + op + "P" + add, String.valueOf((val >> 16) + extra));
            }

            if (instruction.indexOf("LH" + op) >= 0) {
               value = theTokenList.get(op).getValue();
               val = 0;

               try {
                  val = Binary.stringToInt(value);
               } catch (NumberFormatException var26) {
               }

               add = Binary.bitValue(val, 15);
               instruction = substitute(instruction, "LH" + op, String.valueOf((val >> 16) + add));
            }

            if ((index = instruction.indexOf("LL" + op + "P")) >= 0) {
               value = theTokenList.get(op).getValue();
               val = 0;
               add = instruction.charAt(index + 4) - 48;

               try {
                  val = Binary.stringToInt(value) + add;
               } catch (NumberFormatException var25) {
               }

               instruction = substitute(instruction, "LL" + op + "P" + add, String.valueOf(val << 16 >> 16));
            }

            if ((index = instruction.indexOf("LL" + op)) >= 0) {
               value = theTokenList.get(op).getValue();
               val = 0;

               try {
                  val = Binary.stringToInt(value);
               } catch (NumberFormatException var24) {
               }

               if (instruction.length() > index + 3 && instruction.charAt(index + 3) == 'U') {
                  instruction = substitute(instruction, "LL" + op + "U", String.valueOf(val & '\uffff'));
               } else {
                  instruction = substitute(instruction, "LL" + op, String.valueOf(val << 16 >> 16));
               }
            }

            if ((index = instruction.indexOf("VHL" + op + "P")) >= 0) {
               value = theTokenList.get(op).getValue();
               val = 0;
               add = instruction.charAt(index + 5) - 48;

               try {
                  val = Binary.stringToInt(value) + add;
               } catch (NumberFormatException var23) {
               }

               instruction = substitute(instruction, "VHL" + op + "P" + add, String.valueOf(val >> 16));
            }

            if ((index = instruction.indexOf("VH" + op + "P")) >= 0) {
               value = theTokenList.get(op).getValue();
               val = 0;
               add = instruction.charAt(index + 4) - 48;

               try {
                  val = Binary.stringToInt(value) + add;
               } catch (NumberFormatException var22) {
               }

               extra = Binary.bitValue(val, 15);
               instruction = substitute(instruction, "VH" + op + "P" + add, String.valueOf((val >> 16) + extra));
            }

            if (instruction.indexOf("VH" + op) >= 0) {
               value = theTokenList.get(op).getValue();
               val = 0;

               try {
                  val = Binary.stringToInt(value);
               } catch (NumberFormatException var21) {
               }

               add = Binary.bitValue(val, 15);
               instruction = substitute(instruction, "VH" + op, String.valueOf((val >> 16) + add));
            }

            if ((index = instruction.indexOf("VL" + op + "P")) >= 0) {
               value = theTokenList.get(op).getValue();
               val = 0;
               add = instruction.charAt(index + 4) - 48;

               try {
                  val = Binary.stringToInt(value) + add;
               } catch (NumberFormatException var20) {
               }

               if (instruction.length() > index + 5 && instruction.charAt(index + 5) == 'U') {
                  instruction = substitute(instruction, "VL" + op + "P" + add + "U", String.valueOf(val & '\uffff'));
               } else {
                  instruction = substitute(instruction, "VL" + op + "P" + add, String.valueOf(val << 16 >> 16));
               }
            }

            if ((index = instruction.indexOf("VL" + op)) >= 0) {
               value = theTokenList.get(op).getValue();
               val = 0;

               try {
                  val = Binary.stringToInt(value);
               } catch (NumberFormatException var19) {
               }

               if (instruction.length() > index + 3 && instruction.charAt(index + 3) == 'U') {
                  instruction = substitute(instruction, "VL" + op + "U", String.valueOf(val & '\uffff'));
               } else {
                  instruction = substitute(instruction, "VL" + op, String.valueOf(val << 16 >> 16));
               }
            }

            if (instruction.indexOf("VHL" + op) >= 0) {
               value = theTokenList.get(op).getValue();
               val = 0;

               try {
                  val = Binary.stringToInt(value);
               } catch (NumberFormatException var18) {
               }

               instruction = substitute(instruction, "VHL" + op, String.valueOf(val >> 16));
            }
         }

         String label;
         if (instruction.indexOf("LHL") >= 0) {
            label = theTokenList.get(2).getValue();
            val = 0;

            try {
               val = Binary.stringToInt(label);
            } catch (NumberFormatException var17) {
            }

            instruction = substitute(instruction, "LHL", String.valueOf(val >> 16));
         }

         if ((index = instruction.indexOf("LHPAP")) >= 0) {
            label = theTokenList.get(2).getValue();
            value = theTokenList.get(4).getValue();
            val = 0;
            add = instruction.charAt(index + 5) - 48;

            try {
               val = Binary.stringToInt(label) + Binary.stringToInt(value) + add;
            } catch (NumberFormatException var16) {
            }

            extra = Binary.bitValue(val, 15);
            instruction = substitute(instruction, "LHPAP" + add, String.valueOf((val >> 16) + extra));
         }

         if (instruction.indexOf("LHPA") >= 0) {
            label = theTokenList.get(2).getValue();
            value = theTokenList.get(4).getValue();
            val = 0;

            try {
               val = Binary.stringToInt(label) + Binary.stringToInt(value);
            } catch (NumberFormatException var15) {
            }

            add = Binary.bitValue(val, 15);
            instruction = substitute(instruction, "LHPA", String.valueOf((val >> 16) + add));
         }

         if (instruction.indexOf("LHPN") >= 0) {
            label = theTokenList.get(2).getValue();
            value = theTokenList.get(4).getValue();
            val = 0;

            try {
               val = Binary.stringToInt(label) + Binary.stringToInt(value);
            } catch (NumberFormatException var14) {
            }

            instruction = substitute(instruction, "LHPN", String.valueOf(val >> 16));
         }

         if ((index = instruction.indexOf("LLPP")) >= 0) {
            label = theTokenList.get(2).getValue();
            value = theTokenList.get(4).getValue();
            val = 0;
            add = instruction.charAt(index + 4) - 48;

            try {
               val = Binary.stringToInt(label) + Binary.stringToInt(value) + add;
            } catch (NumberFormatException var13) {
            }

            instruction = substitute(instruction, "LLPP" + add, String.valueOf(val << 16 >> 16));
         }

         if ((index = instruction.indexOf("LLP")) >= 0) {
            label = theTokenList.get(2).getValue();
            value = theTokenList.get(4).getValue();
            val = 0;

            try {
               val = Binary.stringToInt(label) + Binary.stringToInt(value);
            } catch (NumberFormatException var12) {
            }

            if (instruction.length() > index + 3 && instruction.charAt(index + 3) == 'U') {
               instruction = substitute(instruction, "LLPU", String.valueOf(val & '\uffff'));
            } else {
               instruction = substitute(instruction, "LLP", String.valueOf(val << 16 >> 16));
            }
         }

         if ((index = instruction.indexOf("BROFF")) >= 0) {
            try {
               label = instruction.substring(index + 5, index + 6);
               value = instruction.substring(index + 6, index + 7);
               instruction = substitute(instruction, "BROFF" + label + value, Globals.getSettings().getDelayedBranchingEnabled() ? value : label);
            } catch (IndexOutOfBoundsException var11) {
               instruction = substitute(instruction, "BROFF", "BAD_PSEUDO_OP_SPEC");
            }
         }

         if (instruction.indexOf("NR") >= 0) {
            for(op = 1; op < theTokenList.size(); ++op) {
               value = theTokenList.get(op).getValue();

               try {
                  val = RegisterFile.getUserRegister(value).getNumber();
                  if (val >= 0) {
                     instruction = substitute(instruction, "NR" + op, "$" + (val + 1));
                  }
               } catch (NullPointerException var28) {
                  val = Coprocessor1.getRegisterNumber(value);
                  if (val >= 0) {
                     instruction = substitute(instruction, "NR" + op, "$f" + (val + 1));
                  }
               }
            }
         }

         if (instruction.indexOf("S32") >= 0) {
            label = theTokenList.get(theTokenList.size() - 1).getValue();
            val = 0;

            try {
               val = Binary.stringToInt(label);
            } catch (NumberFormatException var10) {
            }

            instruction = substitute(instruction, "S32", Integer.toString(32 - val));
         }

         if (instruction.indexOf("LAB") >= 0) {
            label = theTokenList.get(theTokenList.size() - 1).getValue();
            Symbol sym = program.getLocalSymbolTable().getSymbolGivenAddressLocalOrGlobal(label);
            if (sym != null) {
               instruction = substituteFirst(instruction, "LAB", sym.getName());
            }
         }

         return instruction;
      }
   }

   private static String substitute(String original, String find, String replacement) {
      if (original.indexOf(find) >= 0 && !find.equals(replacement)) {
         int i;
         String modified;
         for(modified = original; (i = modified.indexOf(find)) >= 0; modified = modified.substring(0, i) + replacement + modified.substring(i + find.length())) {
         }

         return modified;
      } else {
         return original;
      }
   }

   private static String substituteFirst(String original, String find, String replacement) {
      if (original.indexOf(find) >= 0 && !find.equals(replacement)) {
         String modified = original;
         int i;
         if ((i = original.indexOf(find)) >= 0) {
            modified = original.substring(0, i) + replacement + original.substring(i + find.length());
         }

         return modified;
      } else {
         return original;
      }
   }

   private ArrayList buildTranslationList(String translation) {
      if (translation != null && translation.length() != 0) {
         ArrayList translationList = new ArrayList();
         StringTokenizer st = new StringTokenizer(translation, "\n");

         while(st.hasMoreTokens()) {
            translationList.add(st.nextToken());
         }

         return translationList;
      } else {
         return null;
      }
   }

   private int getInstructionLength(ArrayList translationList) {
      if (translationList != null && translationList.size() != 0) {
         int instructionCount = 0;

         for(int i = 0; i < translationList.size(); ++i) {
            if (((String)translationList.get(i)).indexOf("DBNOP") < 0 || Globals.getSettings().getDelayedBranchingEnabled()) {
               ++instructionCount;
            }
         }

         return 4 * instructionCount;
      } else {
         return 0;
      }
   }
}
