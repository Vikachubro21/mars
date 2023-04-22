package mars.venus.editors.jeditsyntax.tokenmarker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import javax.swing.text.Segment;
import mars.Globals;
import mars.assembler.Directives;
import mars.assembler.TokenTypes;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.Register;
import mars.mips.hardware.RegisterFile;
import mars.mips.instructions.BasicInstruction;
import mars.mips.instructions.Instruction;
import mars.venus.editors.jeditsyntax.KeywordMap;
import mars.venus.editors.jeditsyntax.PopupHelpItem;

public class MIPSTokenMarker extends TokenMarker {
   private static KeywordMap cKeywords;
   private static String[] tokenLabels;
   private static String[] tokenExamples;
   private KeywordMap keywords;
   private int lastOffset;
   private int lastKeyword;

   public MIPSTokenMarker() {
      this(getKeywords());
   }

   public MIPSTokenMarker(KeywordMap keywords) {
      this.keywords = keywords;
   }

   public static String[] getMIPSTokenLabels() {
      if (tokenLabels == null) {
         tokenLabels = new String[12];
         tokenLabels[1] = "Comment";
         tokenLabels[3] = "String literal";
         tokenLabels[4] = "Character literal";
         tokenLabels[5] = "Label";
         tokenLabels[6] = "MIPS instruction";
         tokenLabels[7] = "Assembler directive";
         tokenLabels[8] = "Register";
         tokenLabels[10] = "In-progress, invalid";
         tokenLabels[11] = "Macro parameter";
      }

      return tokenLabels;
   }

   public static String[] getMIPSTokenExamples() {
      if (tokenExamples == null) {
         tokenExamples = new String[12];
         tokenExamples[1] = "# Load";
         tokenExamples[3] = "\"First\"";
         tokenExamples[4] = "'\\n'";
         tokenExamples[5] = "main:";
         tokenExamples[6] = "lui";
         tokenExamples[7] = ".text";
         tokenExamples[8] = "$zero";
         tokenExamples[10] = "\"Regi";
         tokenExamples[11] = "%arg";
      }

      return tokenExamples;
   }

   public byte markTokensImpl(byte token, Segment line, int lineIndex) {
      char[] array = line.array;
      int offset = line.offset;
      this.lastOffset = offset;
      this.lastKeyword = offset;
      int length = line.count + offset;
      boolean backslash = false;
      int i = offset;

      while(true) {
         label93: {
            if (i < length) {
               int i1 = i + 1;
               char c = array[i];
               if (c == '\\') {
                  backslash = !backslash;
                  break label93;
               }

               label81:
               switch (token) {
                  case 0:
                     switch (c) {
                        case '"':
                           this.doKeyword(line, i, c);
                           if (backslash) {
                              backslash = false;
                           } else {
                              this.addToken(i - this.lastOffset, token);
                              token = 3;
                              this.lastOffset = this.lastKeyword = i;
                           }
                           break label93;
                        case '#':
                           backslash = false;
                           this.doKeyword(line, i, c);
                           if (length - i < 1) {
                              break label93;
                           }

                           this.addToken(i - this.lastOffset, token);
                           this.addToken(length - i, (byte)1);
                           this.lastOffset = this.lastKeyword = length;
                           break label81;
                        case '\'':
                           this.doKeyword(line, i, c);
                           if (backslash) {
                              backslash = false;
                           } else {
                              this.addToken(i - this.lastOffset, token);
                              token = 4;
                              this.lastOffset = this.lastKeyword = i;
                           }
                           break label93;
                        case ':':
                           backslash = false;
                           boolean validIdentifier = false;

                           try {
                              validIdentifier = TokenTypes.isValidIdentifier((new String(array, this.lastOffset, i1 - this.lastOffset - 1)).trim());
                           } catch (StringIndexOutOfBoundsException var13) {
                              validIdentifier = false;
                           }

                           if (validIdentifier) {
                              this.addToken(i1 - this.lastOffset, (byte)5);
                              this.lastOffset = this.lastKeyword = i1;
                           }
                           break label93;
                        default:
                           backslash = false;
                           if (!Character.isLetterOrDigit(c) && c != '_' && c != '.' && c != '$' && c != '%') {
                              this.doKeyword(line, i, c);
                           }
                           break label93;
                     }
                  case 1:
                  case 2:
                  default:
                     throw new InternalError("Invalid state: " + token);
                  case 3:
                     if (backslash) {
                        backslash = false;
                     } else if (c == '"') {
                        this.addToken(i1 - this.lastOffset, token);
                        token = 0;
                        this.lastOffset = this.lastKeyword = i1;
                     }
                     break label93;
                  case 4:
                     if (backslash) {
                        backslash = false;
                     } else if (c == '\'') {
                        this.addToken(i1 - this.lastOffset, (byte)3);
                        token = 0;
                        this.lastOffset = this.lastKeyword = i1;
                     }
                     break label93;
               }
            }

            if (token == 0) {
               this.doKeyword(line, length, '\u0000');
            }

            switch (token) {
               case 3:
               case 4:
                  this.addToken(length - this.lastOffset, (byte)10);
                  token = 0;
                  break;
               case 7:
                  this.addToken(length - this.lastOffset, token);
                  if (!backslash) {
                     token = 0;
                  }
               case 5:
               case 6:
               default:
                  this.addToken(length - this.lastOffset, token);
            }

            return token;
         }

         ++i;
      }
   }

   public ArrayList getTokenExactMatchHelp(Token token, String tokenText) {
      ArrayList matches = null;
      if (token != null && token.id == 6) {
         ArrayList instrMatches = Globals.instructionSet.matchOperator(tokenText);
         if (instrMatches.size() > 0) {
            int realMatches = 0;
            matches = new ArrayList();

            for(int i = 0; i < instrMatches.size(); ++i) {
               Instruction inst = (Instruction)instrMatches.get(i);
               if (Globals.getSettings().getExtendedAssemblerEnabled() || inst instanceof BasicInstruction) {
                  matches.add(new PopupHelpItem(tokenText, inst.getExampleFormat(), inst.getDescription()));
                  ++realMatches;
               }
            }

            if (realMatches == 0) {
               matches.add(new PopupHelpItem(tokenText, tokenText, "(is not a basic instruction)"));
            }
         }
      }

      if (token != null && token.id == 7) {
         Directives dir = Directives.matchDirective(tokenText);
         if (dir != null) {
            matches = new ArrayList();
            matches.add(new PopupHelpItem(tokenText, dir.getName(), dir.getDescription()));
         }
      }

      return matches;
   }

   public ArrayList getTokenPrefixMatchHelp(String line, Token tokenList, Token token, String tokenText) {
      ArrayList matches = null;
      if (tokenList != null && tokenList.id != 127) {
         if (token != null && token.id == 1) {
            return null;
         } else {
            Token tokens = tokenList;
            String keywordTokenText = null;
            byte keywordType = -1;
            int offset = 0;

            boolean moreThanOneKeyword;
            for(moreThanOneKeyword = false; tokens.id != 127; tokens = tokens.next) {
               if (tokens.id == 6 || tokens.id == 7) {
                  if (keywordTokenText != null) {
                     moreThanOneKeyword = true;
                     break;
                  }

                  keywordTokenText = line.substring(offset, offset + tokens.length);
                  keywordType = tokens.id;
               }

               offset += tokens.length;
            }

            if (token != null && token.id == 6) {
               if (moreThanOneKeyword) {
                  return keywordType == 6 ? this.getTextFromInstructionMatch(keywordTokenText, true) : this.getTextFromDirectiveMatch(keywordTokenText, true);
               } else {
                  return this.getTextFromInstructionMatch(tokenText, false);
               }
            } else if (token != null && token.id == 7) {
               if (moreThanOneKeyword) {
                  return keywordType == 6 ? this.getTextFromInstructionMatch(keywordTokenText, true) : this.getTextFromDirectiveMatch(keywordTokenText, true);
               } else {
                  return this.getTextFromDirectiveMatch(tokenText, false);
               }
            } else {
               if (keywordTokenText != null) {
                  if (keywordType == 6) {
                     return this.getTextFromInstructionMatch(keywordTokenText, true);
                  }

                  if (keywordType == 7) {
                     return this.getTextFromDirectiveMatch(keywordTokenText, true);
                  }
               }

               if (token != null && token.id == 0) {
                  String trimmedTokenText = tokenText.trim();
                  if (keywordTokenText == null && trimmedTokenText.length() == 0) {
                     return null;
                  }

                  if (keywordTokenText == null && trimmedTokenText.length() > 0) {
                     if (trimmedTokenText.charAt(0) == '.') {
                        return this.getTextFromDirectiveMatch(trimmedTokenText, false);
                     }

                     if (trimmedTokenText.length() >= Globals.getSettings().getEditorPopupPrefixLength()) {
                        return this.getTextFromInstructionMatch(trimmedTokenText, false);
                     }
                  }
               }

               return null;
            }
         }
      } else {
         return null;
      }
   }

   private ArrayList getTextFromDirectiveMatch(String tokenText, boolean exact) {
      ArrayList matches = null;
      ArrayList directiveMatches = null;
      if (exact) {
         Object dir = Directives.matchDirective(tokenText);
         if (dir != null) {
            directiveMatches = new ArrayList();
            directiveMatches.add(dir);
         }
      } else {
         directiveMatches = Directives.prefixMatchDirectives(tokenText);
      }

      if (directiveMatches != null) {
         matches = new ArrayList();

         for(int i = 0; i < directiveMatches.size(); ++i) {
            Directives direct = (Directives)directiveMatches.get(i);
            matches.add(new PopupHelpItem(tokenText, direct.getName(), direct.getDescription(), exact));
         }
      }

      return matches;
   }

   private ArrayList getTextFromInstructionMatch(String tokenText, boolean exact) {
      String text = null;
      ArrayList matches = null;
      ArrayList results = new ArrayList();
      if (exact) {
         matches = Globals.instructionSet.matchOperator(tokenText);
      } else {
         matches = Globals.instructionSet.prefixMatchOperator(tokenText);
      }

      if (matches == null) {
         return null;
      } else {
         int realMatches = 0;
         HashMap insts = new HashMap();
         TreeSet mnemonics = new TreeSet();

         String info;
         for(int i = 0; i < matches.size(); ++i) {
            Instruction inst = (Instruction)matches.get(i);
            if (Globals.getSettings().getExtendedAssemblerEnabled() || inst instanceof BasicInstruction) {
               if (exact) {
                  results.add(new PopupHelpItem(tokenText, inst.getExampleFormat(), inst.getDescription(), exact));
               } else {
                  info = inst.getExampleFormat().split(" ")[0];
                  if (!insts.containsKey(info)) {
                     mnemonics.add(info);
                     insts.put(info, inst.getDescription());
                  }
               }

               ++realMatches;
            }
         }

         if (realMatches == 0) {
            if (!exact) {
               return null;
            }

            results.add(new PopupHelpItem(tokenText, tokenText, "(not a basic instruction)", exact));
         } else if (!exact) {
            Iterator mnemonicList = mnemonics.iterator();

            while(mnemonicList.hasNext()) {
               String mnemonic = (String)mnemonicList.next();
               info = (String)insts.get(mnemonic);
               results.add(new PopupHelpItem(tokenText, mnemonic, info, exact));
            }
         }

         return results;
      }
   }

   public static KeywordMap getKeywords() {
      if (cKeywords == null) {
         cKeywords = new KeywordMap(false);
         ArrayList instructionSet = Globals.instructionSet.getInstructionList();

         for(int i = 0; i < instructionSet.size(); ++i) {
            cKeywords.add(((Instruction)instructionSet.get(i)).getName(), (byte)6);
         }

         ArrayList directiveSet = Directives.getDirectiveList();

         for(int i = 0; i < directiveSet.size(); ++i) {
            cKeywords.add(((Directives)directiveSet.get(i)).getName(), (byte)7);
         }

         Register[] registerFile = RegisterFile.getRegisters();

         for(int i = 0; i < registerFile.length; ++i) {
            cKeywords.add(registerFile[i].getName(), (byte)8);
            cKeywords.add("$" + i, (byte)8);
         }

         Register[] coprocessor1RegisterFile = Coprocessor1.getRegisters();

         for(int i = 0; i < coprocessor1RegisterFile.length; ++i) {
            cKeywords.add(coprocessor1RegisterFile[i].getName(), (byte)8);
         }
      }

      return cKeywords;
   }

   private boolean doKeyword(Segment line, int i, char c) {
      int i1 = i + 1;
      int len = i - this.lastKeyword;
      byte id = this.keywords.lookup(line, this.lastKeyword, len);
      if (id != 0) {
         if (this.lastKeyword != this.lastOffset) {
            this.addToken(this.lastKeyword - this.lastOffset, (byte)0);
         }

         this.addToken(len, id);
         this.lastOffset = i;
      }

      this.lastKeyword = i1;
      return false;
   }

   private boolean tokenListContainsKeyword() {
      Token token = this.firstToken;
      boolean result = false;

      String str;
      for(str = ""; token != null; token = token.next) {
         str = str + token.id + "(" + token.length + ") ";
         if (token.id == 6 || token.id == 7 || token.id == 8) {
            result = true;
         }
      }

      System.out.println(result + " " + str);
      return result;
   }
}
