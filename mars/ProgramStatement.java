package mars;

import java.util.ArrayList;
import mars.assembler.Token;
import mars.assembler.TokenList;
import mars.assembler.TokenTypes;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.RegisterFile;
import mars.mips.instructions.BasicInstruction;
import mars.mips.instructions.BasicInstructionFormat;
import mars.mips.instructions.Instruction;
import mars.util.Binary;
import mars.venus.NumberDisplayBaseChooser;

public class ProgramStatement {
   private MIPSprogram sourceMIPSprogram;
   private String source;
   private String basicAssemblyStatement;
   private String machineStatement;
   private TokenList originalTokenList;
   private TokenList strippedTokenList;
   private BasicStatementList basicStatementList;
   private int[] operands;
   private int numOperands;
   private Instruction instruction;
   private int textAddress;
   private int sourceLine;
   private int binaryStatement;
   private boolean altered;
   private static final String invalidOperator = "<INVALID>";

   public ProgramStatement(MIPSprogram sourceMIPSprogram, String source, TokenList origTokenList, TokenList strippedTokenList, Instruction inst, int textAddress, int sourceLine) {
      this.sourceMIPSprogram = sourceMIPSprogram;
      this.source = source;
      this.originalTokenList = origTokenList;
      this.strippedTokenList = strippedTokenList;
      this.operands = new int[4];
      this.numOperands = 0;
      this.instruction = inst;
      this.textAddress = textAddress;
      this.sourceLine = sourceLine;
      this.basicAssemblyStatement = null;
      this.basicStatementList = new BasicStatementList();
      this.machineStatement = null;
      this.binaryStatement = 0;
      this.altered = false;
   }

   public ProgramStatement(int binaryStatement, int textAddress) {
      this.sourceMIPSprogram = null;
      this.binaryStatement = binaryStatement;
      this.textAddress = textAddress;
      this.originalTokenList = this.strippedTokenList = null;
      this.source = "";
      this.machineStatement = this.basicAssemblyStatement = null;
      BasicInstruction instr = Globals.instructionSet.findByBinaryCode(binaryStatement);
      if (instr == null) {
         this.operands = null;
         this.numOperands = 0;
         this.instruction = binaryStatement == 0 ? (Instruction)Globals.instructionSet.matchOperator("nop").get(0) : null;
      } else {
         this.operands = new int[4];
         this.numOperands = 0;
         this.instruction = instr;
         String opandCodes = "fst";
         String fmt = instr.getOperationMask();
         BasicInstructionFormat instrFormat = instr.getInstructionFormat();
         int numOps = 0;

         for(int i = 0; i < opandCodes.length(); ++i) {
            int code = opandCodes.charAt(i);
            int j = fmt.indexOf(code);
            if (j >= 0) {
               int k0 = 31 - fmt.lastIndexOf(code);
               int k1 = 31 - j;
               int opand = binaryStatement >> k0 & (1 << k1 - k0 + 1) - 1;
               if (instrFormat.equals(BasicInstructionFormat.I_BRANCH_FORMAT) && numOps == 2) {
                  opand = opand << 16 >> 16;
               } else if (instrFormat.equals(BasicInstructionFormat.J_FORMAT) && numOps == 0) {
                  opand |= textAddress >> 2 & 1006632960;
               }

               this.operands[numOps] = opand;
               ++numOps;
            }
         }

         this.numOperands = numOps;
      }

      this.altered = false;
      this.basicStatementList = this.buildBasicStatementListFromBinaryCode(binaryStatement, instr, this.operands, this.numOperands);
   }

   public void buildBasicStatementFromBasicInstruction(ErrorList errors) {
      Token token = this.strippedTokenList.get(0);
      String basicStatementElement = token.getValue() + " ";
      String basic = basicStatementElement;
      this.basicStatementList.addString(basicStatementElement);
      this.numOperands = 0;

      for(int i = 1; i < this.strippedTokenList.size(); ++i) {
         token = this.strippedTokenList.get(i);
         TokenTypes tokenType = token.getType();
         String tokenValue = token.getValue();
         int registerNumber;
         if (tokenType == TokenTypes.REGISTER_NUMBER) {
            basic = basic + tokenValue;
            this.basicStatementList.addString(tokenValue);

            try {
               registerNumber = RegisterFile.getUserRegister(tokenValue).getNumber();
            } catch (Exception var13) {
               errors.add(new ErrorMessage(this.sourceMIPSprogram, token.getSourceLine(), token.getStartPos(), "invalid register name"));
               return;
            }

            this.operands[this.numOperands++] = registerNumber;
         } else if (tokenType == TokenTypes.REGISTER_NAME) {
            registerNumber = RegisterFile.getNumber(tokenValue);
            basicStatementElement = "$" + registerNumber;
            basic = basic + basicStatementElement;
            this.basicStatementList.addString(basicStatementElement);
            if (registerNumber < 0) {
               errors.add(new ErrorMessage(this.sourceMIPSprogram, token.getSourceLine(), token.getStartPos(), "invalid register name"));
               return;
            }

            this.operands[this.numOperands++] = registerNumber;
         } else if (tokenType == TokenTypes.FP_REGISTER_NAME) {
            registerNumber = Coprocessor1.getRegisterNumber(tokenValue);
            basicStatementElement = "$f" + registerNumber;
            basic = basic + basicStatementElement;
            this.basicStatementList.addString(basicStatementElement);
            if (registerNumber < 0) {
               errors.add(new ErrorMessage(this.sourceMIPSprogram, token.getSourceLine(), token.getStartPos(), "invalid FPU register name"));
               return;
            }

            this.operands[this.numOperands++] = registerNumber;
         } else {
            int address;
            if (tokenType == TokenTypes.IDENTIFIER) {
               address = this.sourceMIPSprogram.getLocalSymbolTable().getAddressLocalOrGlobal(tokenValue);
               if (address == -1) {
                  errors.add(new ErrorMessage(this.sourceMIPSprogram, token.getSourceLine(), token.getStartPos(), "Symbol \"" + tokenValue + "\" not found in symbol table."));
                  return;
               }

               boolean absoluteAddress = true;
               if (this.instruction instanceof BasicInstruction) {
                  BasicInstructionFormat format = ((BasicInstruction)this.instruction).getInstructionFormat();
                  if (format == BasicInstructionFormat.I_BRANCH_FORMAT) {
                     address = address - (this.textAddress + 4) >> 2;
                     absoluteAddress = false;
                  }
               }

               basic = basic + address;
               if (absoluteAddress) {
                  this.basicStatementList.addAddress(address);
               } else {
                  this.basicStatementList.addValue(address);
               }

               this.operands[this.numOperands++] = address;
            } else if (tokenType != TokenTypes.INTEGER_5 && tokenType != TokenTypes.INTEGER_16 && tokenType != TokenTypes.INTEGER_16U && tokenType != TokenTypes.INTEGER_32) {
               basic = basic + tokenValue;
               this.basicStatementList.addString(tokenValue);
            } else {
               address = Binary.stringToInt(tokenValue);
               basic = basic + address;
               this.basicStatementList.addValue(address);
               this.operands[this.numOperands++] = address;
            }
         }

         if (i < this.strippedTokenList.size() - 1) {
            TokenTypes nextTokenType = this.strippedTokenList.get(i + 1).getType();
            if (tokenType != TokenTypes.LEFT_PAREN && tokenType != TokenTypes.RIGHT_PAREN && nextTokenType != TokenTypes.LEFT_PAREN && nextTokenType != TokenTypes.RIGHT_PAREN) {
               basicStatementElement = ",";
               basic = basic + basicStatementElement;
               this.basicStatementList.addString(basicStatementElement);
            }
         }
      }

      this.basicAssemblyStatement = basic;
   }

   public void buildMachineStatementFromBasicStatement(ErrorList errors) {
      try {
         this.machineStatement = ((BasicInstruction)this.instruction).getOperationMask();
      } catch (ClassCastException var4) {
         errors.add(new ErrorMessage(this.sourceMIPSprogram, this.sourceLine, 0, "INTERNAL ERROR: pseudo-instruction expansion contained a pseudo-instruction"));
         return;
      }

      BasicInstructionFormat format = ((BasicInstruction)this.instruction).getInstructionFormat();
      if (format == BasicInstructionFormat.J_FORMAT) {
         if ((this.textAddress & -268435456) != (this.operands[0] & -268435456)) {
            errors.add(new ErrorMessage(this.sourceMIPSprogram, this.sourceLine, 0, "Jump target word address beyond 26-bit range"));
            return;
         }

         this.operands[0] >>>= 2;
         this.insertBinaryCode(this.operands[0], Instruction.operandMask[0], errors);
      } else {
         int i;
         if (format == BasicInstructionFormat.I_BRANCH_FORMAT) {
            for(i = 0; i < this.numOperands - 1; ++i) {
               this.insertBinaryCode(this.operands[i], Instruction.operandMask[i], errors);
            }

            this.insertBinaryCode(this.operands[this.numOperands - 1], Instruction.operandMask[this.numOperands - 1], errors);
         } else {
            for(i = 0; i < this.numOperands; ++i) {
               this.insertBinaryCode(this.operands[i], Instruction.operandMask[i], errors);
            }
         }
      }

      this.binaryStatement = Binary.binaryStringToInt(this.machineStatement);
   }

   public String toString() {
      String blanks = "                               ";
      String result = "[" + this.textAddress + "]";
      int i;
      if (this.basicAssemblyStatement != null) {
         i = this.basicAssemblyStatement.indexOf(" ");
         result = result + blanks.substring(0, 16 - result.length()) + this.basicAssemblyStatement.substring(0, i);
         result = result + blanks.substring(0, 24 - result.length()) + this.basicAssemblyStatement.substring(i + 1);
      } else {
         result = result + blanks.substring(0, 16 - result.length()) + "0x" + Integer.toString(this.binaryStatement, 16);
      }

      result = result + blanks.substring(0, 40 - result.length()) + ";  ";
      if (this.operands != null) {
         for(i = 0; i < this.numOperands; ++i) {
            result = result + Integer.toString(this.operands[i], 16) + " ";
         }
      }

      if (this.machineStatement != null) {
         result = result + "[" + Binary.binaryStringToHexString(this.machineStatement) + "]";
         result = result + "  " + this.machineStatement.substring(0, 6) + "|" + this.machineStatement.substring(6, 11) + "|" + this.machineStatement.substring(11, 16) + "|" + this.machineStatement.substring(16, 21) + "|" + this.machineStatement.substring(21, 26) + "|" + this.machineStatement.substring(26, 32);
      }

      return result;
   }

   public void setBasicAssemblyStatement(String statement) {
      this.basicAssemblyStatement = statement;
   }

   public void setMachineStatement(String statement) {
      this.machineStatement = statement;
   }

   public void setBinaryStatement(int binaryCode) {
      this.binaryStatement = binaryCode;
   }

   public void setSource(String src) {
      this.source = src;
   }

   public MIPSprogram getSourceMIPSprogram() {
      return this.sourceMIPSprogram;
   }

   public String getSourceFile() {
      return this.sourceMIPSprogram == null ? "" : this.sourceMIPSprogram.getFilename();
   }

   public String getSource() {
      return this.source;
   }

   public int getSourceLine() {
      return this.sourceLine;
   }

   public String getBasicAssemblyStatement() {
      return this.basicAssemblyStatement;
   }

   public String getPrintableBasicAssemblyStatement() {
      return this.basicStatementList.toString();
   }

   public String getMachineStatement() {
      return this.machineStatement;
   }

   public int getBinaryStatement() {
      return this.binaryStatement;
   }

   public TokenList getOriginalTokenList() {
      return this.originalTokenList;
   }

   public TokenList getStrippedTokenList() {
      return this.strippedTokenList;
   }

   public Instruction getInstruction() {
      return this.instruction;
   }

   public int getAddress() {
      return this.textAddress;
   }

   public int[] getOperands() {
      return this.operands;
   }

   public int getOperand(int i) {
      return i >= 0 && i < this.numOperands ? this.operands[i] : -1;
   }

   private void insertBinaryCode(int value, char mask, ErrorList errors) {
      int startPos = this.machineStatement.indexOf(mask);
      int endPos = this.machineStatement.lastIndexOf(mask);
      if (startPos != -1 && endPos != -1) {
         String bitString = Binary.intToBinaryString(value, endPos - startPos + 1);
         String state = this.machineStatement.substring(0, startPos) + bitString;
         if (endPos < this.machineStatement.length() - 1) {
            state = state + this.machineStatement.substring(endPos + 1);
         }

         this.machineStatement = state;
      } else {
         errors.add(new ErrorMessage(this.sourceMIPSprogram, this.sourceLine, 0, "INTERNAL ERROR: mismatch in number of operands in statement vs mask"));
      }
   }

   private BasicStatementList buildBasicStatementListFromBinaryCode(int binary, BasicInstruction instr, int[] operands, int numOperands) {
      BasicStatementList statementList = new BasicStatementList();
      int tokenListCounter = 1;
      if (instr == null) {
         statementList.addString("<INVALID>");
         return statementList;
      } else {
         statementList.addString(instr.getName() + " ");

         for(int i = 0; i < numOperands; ++i) {
            if (tokenListCounter > 1 && tokenListCounter < instr.getTokenList().size()) {
               TokenTypes thisTokenType = instr.getTokenList().get(tokenListCounter).getType();
               if (thisTokenType != TokenTypes.LEFT_PAREN && thisTokenType != TokenTypes.RIGHT_PAREN) {
                  statementList.addString(",");
               }
            }

            for(boolean notOperand = true; notOperand && tokenListCounter < instr.getTokenList().size(); ++tokenListCounter) {
               TokenTypes tokenType = instr.getTokenList().get(tokenListCounter).getType();
               if (tokenType.equals(TokenTypes.LEFT_PAREN)) {
                  statementList.addString("(");
               } else if (tokenType.equals(TokenTypes.RIGHT_PAREN)) {
                  statementList.addString(")");
               } else if (tokenType.toString().contains("REGISTER")) {
                  String marker = tokenType.toString().contains("FP_REGISTER") ? "$f" : "$";
                  statementList.addString(marker + operands[i]);
                  notOperand = false;
               } else {
                  statementList.addValue(operands[i]);
                  notOperand = false;
               }
            }
         }

         for(; tokenListCounter < instr.getTokenList().size(); ++tokenListCounter) {
            TokenTypes tokenType = instr.getTokenList().get(tokenListCounter).getType();
            if (tokenType.equals(TokenTypes.LEFT_PAREN)) {
               statementList.addString("(");
            } else if (tokenType.equals(TokenTypes.RIGHT_PAREN)) {
               statementList.addString(")");
            }
         }

         return statementList;
      }
   }

   private class BasicStatementList {
      private ArrayList list = new ArrayList();

      BasicStatementList() {
      }

      void addString(String string) {
         this.list.add(new ListElement(0, string, 0));
      }

      void addAddress(int address) {
         this.list.add(new ListElement(1, (String)null, address));
      }

      void addValue(int value) {
         this.list.add(new ListElement(2, (String)null, value));
      }

      public String toString() {
         int addressBase = Globals.getSettings().getBooleanSetting(5) ? 16 : 10;
         int valueBase = Globals.getSettings().getBooleanSetting(6) ? 16 : 10;
         StringBuffer result = new StringBuffer();

         for(int i = 0; i < this.list.size(); ++i) {
            ListElement e = (ListElement)this.list.get(i);
            switch (e.type) {
               case 0:
                  result.append(e.sValue);
                  break;
               case 1:
                  result.append(NumberDisplayBaseChooser.formatNumber(e.iValue, addressBase));
                  break;
               case 2:
                  if (valueBase == 16) {
                     result.append(Binary.intToHexString(e.iValue));
                  } else {
                     result.append(NumberDisplayBaseChooser.formatNumber(e.iValue, valueBase));
                  }
            }
         }

         return result.toString();
      }

      private class ListElement {
         int type;
         String sValue;
         int iValue;

         ListElement(int type, String sValue, int iValue) {
            this.type = type;
            this.sValue = sValue;
            this.iValue = iValue;
         }
      }
   }
}
