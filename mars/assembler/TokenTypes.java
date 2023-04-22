package mars.assembler;

import mars.Globals;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.Register;
import mars.mips.hardware.RegisterFile;
import mars.util.Binary;

public final class TokenTypes {
   public static final String TOKEN_DELIMITERS = "\t ,()";
   public static final TokenTypes COMMENT = new TokenTypes("COMMENT");
   public static final TokenTypes DIRECTIVE = new TokenTypes("DIRECTIVE");
   public static final TokenTypes OPERATOR = new TokenTypes("OPERATOR");
   public static final TokenTypes DELIMITER = new TokenTypes("DELIMITER");
   public static final TokenTypes REGISTER_NAME = new TokenTypes("REGISTER_NAME");
   public static final TokenTypes REGISTER_NUMBER = new TokenTypes("REGISTER_NUMBER");
   public static final TokenTypes FP_REGISTER_NAME = new TokenTypes("FP_REGISTER_NAME");
   public static final TokenTypes IDENTIFIER = new TokenTypes("IDENTIFIER");
   public static final TokenTypes LEFT_PAREN = new TokenTypes("LEFT_PAREN");
   public static final TokenTypes RIGHT_PAREN = new TokenTypes("RIGHT_PAREN");
   public static final TokenTypes INTEGER_5 = new TokenTypes("INTEGER_5");
   public static final TokenTypes INTEGER_16 = new TokenTypes("INTEGER_16");
   public static final TokenTypes INTEGER_16U = new TokenTypes("INTEGER_16U");
   public static final TokenTypes INTEGER_32 = new TokenTypes("INTEGER_32");
   public static final TokenTypes REAL_NUMBER = new TokenTypes("REAL_NUMBER");
   public static final TokenTypes QUOTED_STRING = new TokenTypes("QUOTED_STRING");
   public static final TokenTypes PLUS = new TokenTypes("PLUS");
   public static final TokenTypes MINUS = new TokenTypes("MINUS");
   public static final TokenTypes COLON = new TokenTypes("COLON");
   public static final TokenTypes ERROR = new TokenTypes("ERROR");
   public static final TokenTypes MACRO_PARAMETER = new TokenTypes("MACRO_PARAMETER");
   private String descriptor;

   private TokenTypes() {
      this.descriptor = "generic";
   }

   private TokenTypes(String name) {
      this.descriptor = name;
   }

   public String toString() {
      return this.descriptor;
   }

   public static TokenTypes matchTokenType(String value) {
      TokenTypes type = null;
      if (value.charAt(0) == '\'') {
         return ERROR;
      } else if (value.charAt(0) == '#') {
         return COMMENT;
      } else {
         if (value.length() == 1) {
            switch (value.charAt(0)) {
               case '(':
                  return LEFT_PAREN;
               case ')':
                  return RIGHT_PAREN;
               case '+':
                  return PLUS;
               case '-':
                  return MINUS;
               case ':':
                  return COLON;
            }
         }

         if (Macro.tokenIsMacroParameter(value, false)) {
            return MACRO_PARAMETER;
         } else {
            Register reg = RegisterFile.getUserRegister(value);
            if (reg != null) {
               return reg.getName().equals(value) ? REGISTER_NAME : REGISTER_NUMBER;
            } else {
               reg = Coprocessor1.getRegister(value);
               if (reg != null) {
                  return FP_REGISTER_NAME;
               } else {
                  try {
                     int i = Binary.stringToInt(value);
                     if (i >= 0 && i <= 31) {
                        return INTEGER_5;
                     } else if (i >= 0 && i <= 65535) {
                        return INTEGER_16U;
                     } else {
                        return i >= -32768 && i <= 32767 ? INTEGER_16 : INTEGER_32;
                     }
                  } catch (NumberFormatException var5) {
                     try {
                        Double.parseDouble(value);
                        return REAL_NUMBER;
                     } catch (NumberFormatException var4) {
                        if (Globals.instructionSet.matchOperator(value) != null) {
                           return OPERATOR;
                        } else if (value.charAt(0) == '.' && Directives.matchDirective(value) != null) {
                           return DIRECTIVE;
                        } else if (value.charAt(0) == '"') {
                           return QUOTED_STRING;
                        } else {
                           return isValidIdentifier(value) ? IDENTIFIER : ERROR;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static boolean isIntegerTokenType(TokenTypes type) {
      return type == INTEGER_5 || type == INTEGER_16 || type == INTEGER_16U || type == INTEGER_32;
   }

   public static boolean isFloatingTokenType(TokenTypes type) {
      return type == REAL_NUMBER;
   }

   public static boolean isValidIdentifier(String value) {
      boolean result = Character.isLetter(value.charAt(0)) || value.charAt(0) == '_' || value.charAt(0) == '.' || value.charAt(0) == '$';

      for(int index = 1; result && index < value.length(); ++index) {
         if (!Character.isLetterOrDigit(value.charAt(index)) && value.charAt(index) != '_' && value.charAt(index) != '.' && value.charAt(index) != '$') {
            result = false;
         }
      }

      return result;
   }
}
