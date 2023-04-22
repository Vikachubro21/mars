package mars.assembler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import mars.ErrorList;
import mars.ErrorMessage;
import mars.Globals;
import mars.MIPSprogram;
import mars.ProcessingException;

public class Tokenizer {
   private ErrorList errors;
   private MIPSprogram sourceMIPSprogram;
   private HashMap equivalents;
   private static final String escapedCharacters = "'\"\\ntbrf0";
   private static final String[] escapedCharactersValues = new String[]{"39", "34", "92", "10", "9", "8", "13", "12", "0"};

   public Tokenizer() {
      this((MIPSprogram)null);
   }

   public Tokenizer(MIPSprogram program) {
      this.errors = new ErrorList();
      this.sourceMIPSprogram = program;
   }

   public ArrayList tokenize(MIPSprogram p) throws ProcessingException {
      this.sourceMIPSprogram = p;
      this.equivalents = new HashMap();
      ArrayList tokenList = new ArrayList();
      ArrayList source = this.processIncludes(p, new HashMap());
      p.setSourceLineList(source);

      for(int i = 0; i < source.size(); ++i) {
         String sourceLine = ((SourceLine)source.get(i)).getSource();
         TokenList currentLineTokens = this.tokenizeLine(i + 1, sourceLine);
         tokenList.add(currentLineTokens);
         if (sourceLine.length() > 0 && sourceLine != currentLineTokens.getProcessedLine()) {
            source.set(i, new SourceLine(currentLineTokens.getProcessedLine(), ((SourceLine)source.get(i)).getMIPSprogram(), ((SourceLine)source.get(i)).getLineNumber()));
         }
      }

      if (this.errors.errorsOccurred()) {
         throw new ProcessingException(this.errors);
      } else {
         return tokenList;
      }
   }

   private ArrayList processIncludes(MIPSprogram program, Map inclFiles) throws ProcessingException {
      ArrayList source = program.getSourceList();
      ArrayList result = new ArrayList(source.size());

      for(int i = 0; i < source.size(); ++i) {
         String line = (String)source.get(i);
         TokenList tl = this.tokenizeLine(program, i + 1, line, false);
         boolean hasInclude = false;

         for(int ii = 0; ii < tl.size(); ++ii) {
            if (tl.get(ii).getValue().equalsIgnoreCase(Directives.INCLUDE.getName()) && tl.size() > ii + 1 && tl.get(ii + 1).getType() == TokenTypes.QUOTED_STRING) {
               String filename = tl.get(ii + 1).getValue();
               filename = filename.substring(1, filename.length() - 1);
               if (!(new File(filename)).isAbsolute()) {
                  filename = (new File(program.getFilename())).getParent() + File.separator + filename;
               }

               if (inclFiles.containsKey(filename)) {
                  Token t = tl.get(ii + 1);
                  this.errors.add(new ErrorMessage(program, t.getSourceLine(), t.getStartPos(), "Recursive include of file " + filename));
                  throw new ProcessingException(this.errors);
               }

               inclFiles.put(filename, filename);
               MIPSprogram incl = new MIPSprogram();

               try {
                  incl.readSource(filename);
               } catch (ProcessingException var14) {
                  Token t = tl.get(ii + 1);
                  this.errors.add(new ErrorMessage(program, t.getSourceLine(), t.getStartPos(), "Error reading include file " + filename));
                  throw new ProcessingException(this.errors);
               }

               ArrayList allLines = this.processIncludes(incl, inclFiles);
               result.addAll(allLines);
               hasInclude = true;
               break;
            }
         }

         if (!hasInclude) {
            result.add(new SourceLine(line, program, i + 1));
         }
      }

      return result;
   }

   public TokenList tokenizeExampleInstruction(String example) throws ProcessingException {
      new TokenList();
      TokenList result = this.tokenizeLine(this.sourceMIPSprogram, 0, example, false);
      if (this.errors.errorsOccurred()) {
         throw new ProcessingException(this.errors);
      } else {
         return result;
      }
   }

   public TokenList tokenizeLine(int lineNum, String theLine) {
      return this.tokenizeLine(this.sourceMIPSprogram, lineNum, theLine, true);
   }

   public TokenList tokenizeLine(int lineNum, String theLine, ErrorList callerErrorList) {
      ErrorList saveList = this.errors;
      this.errors = callerErrorList;
      TokenList tokens = this.tokenizeLine(lineNum, theLine);
      this.errors = saveList;
      return tokens;
   }

   public TokenList tokenizeLine(int lineNum, String theLine, ErrorList callerErrorList, boolean doEqvSubstitutes) {
      ErrorList saveList = this.errors;
      this.errors = callerErrorList;
      TokenList tokens = this.tokenizeLine(this.sourceMIPSprogram, lineNum, theLine, doEqvSubstitutes);
      this.errors = saveList;
      return tokens;
   }

   public TokenList tokenizeLine(MIPSprogram program, int lineNum, String theLine, boolean doEqvSubstitutes) {
      TokenList result = new TokenList();
      if (theLine.length() == 0) {
         return result;
      } else {
         char[] line = theLine.toCharArray();
         int linePos = 0;
         char[] token = new char[line.length];
         int tokenPos = 0;
         int tokenStartPos = 1;
         boolean insideQuotedString = false;
         if (Globals.debug) {
            System.out.println("source line --->" + theLine + "<---");
         }

         for(; linePos < line.length; ++linePos) {
            char c = line[linePos];
            if (insideQuotedString) {
               token[tokenPos++] = c;
               if (c == '"' && token[tokenPos - 2] != '\\') {
                  this.processCandidateToken(token, program, lineNum, theLine, tokenPos, tokenStartPos, result);
                  tokenPos = 0;
                  insideQuotedString = false;
               }
            } else {
               switch (c) {
                  case '\t':
                  case ' ':
                  case ',':
                     if (tokenPos > 0) {
                        this.processCandidateToken(token, program, lineNum, theLine, tokenPos, tokenStartPos, result);
                        tokenPos = 0;
                     }
                     break;
                  case '"':
                     if (tokenPos > 0) {
                        this.processCandidateToken(token, program, lineNum, theLine, tokenPos, tokenStartPos, result);
                        tokenPos = 0;
                     }

                     tokenStartPos = linePos + 1;
                     token[tokenPos++] = c;
                     insideQuotedString = true;
                     break;
                  case '#':
                     if (tokenPos > 0) {
                        this.processCandidateToken(token, program, lineNum, theLine, tokenPos, tokenStartPos, result);
                        tokenPos = 0;
                     }

                     tokenStartPos = linePos + 1;
                     tokenPos = line.length - linePos;
                     System.arraycopy(line, linePos, token, 0, tokenPos);
                     this.processCandidateToken(token, program, lineNum, theLine, tokenPos, tokenStartPos, result);
                     linePos = line.length;
                     tokenPos = 0;
                     break;
                  case '\'':
                     if (tokenPos > 0) {
                        this.processCandidateToken(token, program, lineNum, theLine, tokenPos, tokenStartPos, result);
                        tokenPos = 0;
                     }

                     tokenStartPos = linePos + 1;
                     token[tokenPos++] = c;
                     int lookaheadChars = line.length - linePos - 1;
                     if (lookaheadChars < 2) {
                        break;
                     }

                     ++linePos;
                     c = line[linePos];
                     token[tokenPos++] = c;
                     if (c == '\'') {
                        break;
                     }

                     ++linePos;
                     c = line[linePos];
                     token[tokenPos++] = c;
                     if ((c != '\'' || token[1] == '\\') && lookaheadChars != 2) {
                        ++linePos;
                        c = line[linePos];
                        token[tokenPos++] = c;
                        if (c != '\'' && lookaheadChars != 3) {
                           if (lookaheadChars >= 5) {
                              ++linePos;
                              c = line[linePos];
                              token[tokenPos++] = c;
                              if (c != '\'') {
                                 ++linePos;
                                 c = line[linePos];
                                 token[tokenPos++] = c;
                              }
                           }

                           this.processCandidateToken(token, program, lineNum, theLine, tokenPos, tokenStartPos, result);
                           tokenPos = 0;
                           tokenStartPos = linePos + 1;
                           break;
                        }

                        this.processCandidateToken(token, program, lineNum, theLine, tokenPos, tokenStartPos, result);
                        tokenPos = 0;
                        tokenStartPos = linePos + 1;
                        break;
                     }

                     this.processCandidateToken(token, program, lineNum, theLine, tokenPos, tokenStartPos, result);
                     tokenPos = 0;
                     tokenStartPos = linePos + 1;
                     break;
                  case '(':
                  case ')':
                  case ':':
                     if (tokenPos > 0) {
                        this.processCandidateToken(token, program, lineNum, theLine, tokenPos, tokenStartPos, result);
                        tokenPos = 0;
                     }

                     tokenStartPos = linePos + 1;
                     token[tokenPos++] = c;
                     this.processCandidateToken(token, program, lineNum, theLine, tokenPos, tokenStartPos, result);
                     tokenPos = 0;
                     break;
                  case '+':
                  case '-':
                     if (tokenPos <= 0 || line.length < linePos + 2 || !Character.isDigit(line[linePos + 1]) || line[linePos - 1] != 'e' && line[linePos - 1] != 'E') {
                        if (tokenPos > 0) {
                           this.processCandidateToken(token, program, lineNum, theLine, tokenPos, tokenStartPos, result);
                           tokenPos = 0;
                        }

                        tokenStartPos = linePos + 1;
                        token[tokenPos++] = c;
                        if (!result.isEmpty() && result.get(result.size() - 1).getType() == TokenTypes.IDENTIFIER || line.length < linePos + 2 || !Character.isDigit(line[linePos + 1])) {
                           this.processCandidateToken(token, program, lineNum, theLine, tokenPos, tokenStartPos, result);
                           tokenPos = 0;
                        }
                        break;
                     }

                     token[tokenPos++] = c;
                     break;
                  default:
                     if (tokenPos == 0) {
                        tokenStartPos = linePos + 1;
                     }

                     token[tokenPos++] = c;
               }
            }
         }

         if (tokenPos > 0) {
            this.processCandidateToken(token, program, lineNum, theLine, tokenPos, tokenStartPos, result);
            tokenPos = 0;
         }

         if (doEqvSubstitutes) {
            result = this.processEqv(program, lineNum, theLine, result);
         }

         return result;
      }
   }

   private TokenList processEqv(MIPSprogram program, int lineNum, String theLine, TokenList tokens) {
      int tokenPosLastOperand;
      int endExpression;
      if (tokens.size() > 2 && (tokens.get(0).getType() == TokenTypes.DIRECTIVE || tokens.get(2).getType() == TokenTypes.DIRECTIVE)) {
         int dirPos = tokens.get(0).getType() == TokenTypes.DIRECTIVE ? 0 : 2;
         if (Directives.matchDirective(tokens.get(dirPos).getValue()) == Directives.EQV) {
            tokenPosLastOperand = tokens.size() - (tokens.get(tokens.size() - 1).getType() == TokenTypes.COMMENT ? 2 : 1);
            if (tokenPosLastOperand < dirPos + 2) {
               this.errors.add(new ErrorMessage(program, lineNum, tokens.get(dirPos).getStartPos(), "Too few operands for " + Directives.EQV.getName() + " directive"));
               return tokens;
            }

            if (tokens.get(dirPos + 1).getType() != TokenTypes.IDENTIFIER) {
               this.errors.add(new ErrorMessage(program, lineNum, tokens.get(dirPos).getStartPos(), "Malformed " + Directives.EQV.getName() + " directive"));
               return tokens;
            }

            String symbol = tokens.get(dirPos + 1).getValue();

            int i;
            for(i = dirPos + 2; i < tokens.size(); ++i) {
               if (tokens.get(i).getValue().equals(symbol)) {
                  this.errors.add(new ErrorMessage(program, lineNum, tokens.get(dirPos).getStartPos(), "Cannot substitute " + symbol + " for itself in " + Directives.EQV.getName() + " directive"));
                  return tokens;
               }
            }

            i = tokens.get(dirPos + 2).getStartPos();
            endExpression = tokens.get(tokenPosLastOperand).getStartPos() + tokens.get(tokenPosLastOperand).getValue().length();
            String expression = theLine.substring(i - 1, endExpression - 1);
            if (this.equivalents.containsKey(symbol) && !((String)this.equivalents.get(symbol)).equals(expression)) {
               this.errors.add(new ErrorMessage(program, lineNum, tokens.get(dirPos + 1).getStartPos(), "\"" + symbol + "\" is already defined"));
               return tokens;
            }

            this.equivalents.put(symbol, expression);
            return tokens;
         }
      }

      boolean substitutionMade = false;

      for(tokenPosLastOperand = 0; tokenPosLastOperand < tokens.size(); ++tokenPosLastOperand) {
         Token token = tokens.get(tokenPosLastOperand);
         if (token.getType() == TokenTypes.IDENTIFIER && this.equivalents != null && this.equivalents.containsKey(token.getValue())) {
            String sub = (String)this.equivalents.get(token.getValue());
            endExpression = token.getStartPos();
            theLine = theLine.substring(0, endExpression - 1) + sub + theLine.substring(endExpression + token.getValue().length() - 1);
            substitutionMade = true;
            break;
         }
      }

      tokens.setProcessedLine(theLine);
      return substitutionMade ? this.tokenizeLine(lineNum, theLine) : tokens;
   }

   public ErrorList getErrors() {
      return this.errors;
   }

   private void processCandidateToken(char[] token, MIPSprogram program, int line, String theLine, int tokenPos, int tokenStartPos, TokenList tokenList) {
      String value = new String(token, 0, tokenPos);
      if (value.length() > 0 && value.charAt(0) == '\'') {
         value = this.preprocessCharacterLiteral(value);
      }

      TokenTypes type = TokenTypes.matchTokenType(value);
      if (type == TokenTypes.ERROR) {
         this.errors.add(new ErrorMessage(program, line, tokenStartPos, theLine + "\nInvalid language element: " + value));
      }

      Token toke = new Token(type, value, program, line, tokenStartPos);
      tokenList.add(toke);
   }

   private String preprocessCharacterLiteral(String value) {
      if (value.length() >= 3 && value.charAt(0) == '\'' && value.charAt(value.length() - 1) == '\'') {
         String quotesRemoved = value.substring(1, value.length() - 1);
         if (quotesRemoved.charAt(0) != '\\') {
            return quotesRemoved.length() == 1 ? Integer.toString(quotesRemoved.charAt(0)) : value;
         } else {
            int intValue;
            if (quotesRemoved.length() == 2) {
               intValue = "'\"\\ntbrf0".indexOf(quotesRemoved.charAt(1));
               return intValue >= 0 ? escapedCharactersValues[intValue] : value;
            } else {
               if (quotesRemoved.length() == 4) {
                  try {
                     intValue = Integer.parseInt(quotesRemoved.substring(1), 8);
                     if (intValue >= 0 && intValue <= 255) {
                        return Integer.toString(intValue);
                     }
                  } catch (NumberFormatException var4) {
                  }
               }

               return value;
            }
         }
      } else {
         return value;
      }
   }
}
