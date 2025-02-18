package mars.mips.instructions;

import java.util.StringTokenizer;
import mars.ProcessingException;
import mars.assembler.TokenList;
import mars.assembler.Tokenizer;

public abstract class Instruction {
   public static final int INSTRUCTION_LENGTH = 4;
   public static final int INSTRUCTION_LENGTH_BITS = 32;
   public static char[] operandMask = new char[]{'f', 's', 't'};
   protected String mnemonic;
   protected String exampleFormat;
   protected String description;
   protected TokenList tokenList;

   public String getName() {
      return this.mnemonic;
   }

   public String getExampleFormat() {
      return this.exampleFormat;
   }

   public String getDescription() {
      return this.description;
   }

   public TokenList getTokenList() {
      return this.tokenList;
   }

   public int getInstructionLength() {
      return 4;
   }

   protected String extractOperator(String example) {
      StringTokenizer st = new StringTokenizer(example, " ,\t");
      return st.nextToken();
   }

   protected void createExampleTokenList() {
      try {
         this.tokenList = (new Tokenizer()).tokenizeExampleInstruction(this.exampleFormat);
      } catch (ProcessingException var2) {
         System.out.println("CONFIGURATION ERROR: Instruction example \"" + this.exampleFormat + "\" contains invalid token(s).");
      }

   }
}
