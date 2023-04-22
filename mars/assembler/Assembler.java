package mars.assembler;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import mars.ErrorList;
import mars.ErrorMessage;
import mars.Globals;
import mars.MIPSprogram;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.mips.instructions.BasicInstruction;
import mars.mips.instructions.ExtendedInstruction;
import mars.mips.instructions.Instruction;
import mars.util.Binary;
import mars.util.SystemIO;
import mars.venus.NumberDisplayBaseChooser;

public class Assembler {
   private ArrayList machineList;
   private ErrorList errors;
   private boolean inDataSegment;
   private boolean inMacroSegment;
   private int externAddress;
   private boolean autoAlign;
   private Directives currentDirective;
   private Directives dataDirective;
   private MIPSprogram fileCurrentlyBeingAssembled;
   private TokenList globalDeclarationList;
   private UserKernelAddressSpace textAddress;
   private UserKernelAddressSpace dataAddress;
   private DataSegmentForwardReferences currentFileDataSegmentForwardReferences;
   private DataSegmentForwardReferences accumulatedDataSegmentForwardReferences;

   public ArrayList<MIPSprogram> assemble(MIPSprogram p, boolean extendedAssemblerEnabled) throws ProcessingException {
      return this.assemble(p, extendedAssemblerEnabled, false);
   }

   public ArrayList<MIPSprogram> assemble(MIPSprogram p, boolean extendedAssemblerEnabled, boolean warningsAreErrors) throws ProcessingException {
      ArrayList<MIPSprogram> programFiles = new ArrayList<>();
      programFiles.add(p);
      return this.assemble(programFiles, extendedAssemblerEnabled, warningsAreErrors);
   }

   public ErrorList getErrorList() {
      return this.errors;
   }

   public ArrayList<MIPSprogram> assemble(ArrayList tokenizedProgramFiles, boolean extendedAssemblerEnabled) throws ProcessingException {
      return this.assemble(tokenizedProgramFiles, extendedAssemblerEnabled, false);
   }

   public ArrayList assemble(ArrayList<MIPSprogram> tokenizedProgramFiles, boolean extendedAssemblerEnabled, boolean warningsAreErrors) throws ProcessingException {
      if (tokenizedProgramFiles != null && tokenizedProgramFiles.size() != 0) {
         this.textAddress = new UserKernelAddressSpace(Memory.textBaseAddress, Memory.kernelTextBaseAddress);
         this.dataAddress = new UserKernelAddressSpace(Memory.dataBaseAddress, Memory.kernelDataBaseAddress);
         this.externAddress = Memory.externBaseAddress;
         this.currentFileDataSegmentForwardReferences = new DataSegmentForwardReferences();
         this.accumulatedDataSegmentForwardReferences = new DataSegmentForwardReferences();
         Globals.symbolTable.clear();
         Globals.memory.clear();
         this.machineList = new ArrayList();
         this.errors = new ErrorList();
         if (Globals.debug) {
            System.out.println("Assembler first pass begins:");
         }

         int fileIndex;
         ArrayList parsedList;
         int sourceLine;
         for(fileIndex = 0; fileIndex < tokenizedProgramFiles.size() && !this.errors.errorLimitExceeded(); ++fileIndex) {
            this.fileCurrentlyBeingAssembled = tokenizedProgramFiles.get(fileIndex);
            this.globalDeclarationList = new TokenList();
            this.inDataSegment = false;
            this.inMacroSegment = false;
            this.autoAlign = true;
            this.dataDirective = Directives.WORD;
            this.fileCurrentlyBeingAssembled.getLocalSymbolTable().clear();
            this.currentFileDataSegmentForwardReferences.clear();
            parsedList = this.fileCurrentlyBeingAssembled.createParsedList();
            ArrayList sourceList = this.fileCurrentlyBeingAssembled.getSourceLineList();
            ArrayList tokenList = this.fileCurrentlyBeingAssembled.getTokenList();
            MacroPool macroPool = this.fileCurrentlyBeingAssembled.createMacroPool();

            for(sourceLine = 0; sourceLine < tokenList.size() && !this.errors.errorLimitExceeded(); ++sourceLine) {
               for(int z = 0; z < ((TokenList)tokenList.get(sourceLine)).size(); ++z) {
                  Token t = ((TokenList)tokenList.get(sourceLine)).get(z);
                  t.setOriginal(((SourceLine)sourceList.get(sourceLine)).getMIPSprogram(), ((SourceLine)sourceList.get(sourceLine)).getLineNumber());
               }

               ArrayList statements = this.parseLine((TokenList)tokenList.get(sourceLine), ((SourceLine)sourceList.get(sourceLine)).getSource(), ((SourceLine)sourceList.get(sourceLine)).getLineNumber(), extendedAssemblerEnabled);
               if (statements != null) {
                  parsedList.addAll(statements);
               }
            }

            if (this.inMacroSegment) {
               this.errors.add(new ErrorMessage(this.fileCurrentlyBeingAssembled, this.fileCurrentlyBeingAssembled.getLocalMacroPool().getCurrent().getFromLine(), 0, "Macro started but not ended (no .end_macro directive)"));
            }

            this.transferGlobals();
            this.currentFileDataSegmentForwardReferences.resolve(this.fileCurrentlyBeingAssembled.getLocalSymbolTable());
            this.accumulatedDataSegmentForwardReferences.add(this.currentFileDataSegmentForwardReferences);
            this.currentFileDataSegmentForwardReferences.clear();
         }

         this.accumulatedDataSegmentForwardReferences.resolve(Globals.symbolTable);
         this.accumulatedDataSegmentForwardReferences.generateErrorMessages(this.errors);
         if (this.errors.errorsOccurred()) {
            throw new ProcessingException(this.errors);
         } else {
            if (Globals.debug) {
               System.out.println("Assembler second pass begins");
            }
            for(fileIndex = 0; fileIndex < tokenizedProgramFiles.size() && !this.errors.errorLimitExceeded(); ++fileIndex) {
               this.fileCurrentlyBeingAssembled = tokenizedProgramFiles.get(fileIndex);
               parsedList = this.fileCurrentlyBeingAssembled.getParsedList();

               for(int i = 0; i < parsedList.size(); ++i) {
                  ProgramStatement statement = (ProgramStatement)parsedList.get(i);
                  statement.buildBasicStatementFromBasicInstruction(this.errors);
                  if (this.errors.errorsOccurred()) {
                     throw new ProcessingException(this.errors);
                  }

                  if (statement.getInstruction() instanceof BasicInstruction) {
                     this.machineList.add(statement);
                  } else {
                     ExtendedInstruction inst = (ExtendedInstruction)statement.getInstruction();
                     String basicAssembly = statement.getBasicAssemblyStatement();
                     sourceLine = statement.getSourceLine();
                     TokenList theTokenList = (new Tokenizer()).tokenizeLine(sourceLine, basicAssembly, this.errors, false);
                     ArrayList templateList;
                     if (this.compactTranslationCanBeApplied(statement)) {
                        templateList = inst.getCompactBasicIntructionTemplateList();
                     } else {
                        templateList = inst.getBasicIntructionTemplateList();
                     }

                     this.textAddress.set(statement.getAddress());

                     for(int instrNumber = 0; instrNumber < templateList.size(); ++instrNumber) {
                        String instruction = ExtendedInstruction.makeTemplateSubstitutions(this.fileCurrentlyBeingAssembled, (String)templateList.get(instrNumber), theTokenList);
                        if (instruction != null && instruction != "") {
                           if (Globals.debug) {
                              System.out.println("PSEUDO generated: " + instruction);
                           }

                           TokenList newTokenList = (new Tokenizer()).tokenizeLine(sourceLine, instruction, this.errors, false);
                           ArrayList instrMatches = this.matchInstruction(newTokenList.get(0));
                           Instruction instr = OperandFormat.bestOperandMatch(newTokenList, instrMatches);
                           ProgramStatement ps = new ProgramStatement(this.fileCurrentlyBeingAssembled, instrNumber == 0 ? statement.getSource() : "", newTokenList, newTokenList, instr, this.textAddress.get(), statement.getSourceLine());
                           this.textAddress.increment(4);
                           ps.buildBasicStatementFromBasicInstruction(this.errors);
                           this.machineList.add(ps);
                        }
                     }
                  }
               }
            }

            if (Globals.debug) {
               System.out.println("Code generation begins");
            }

            for(int i = 0; i < this.machineList.size() && !this.errors.errorLimitExceeded(); ++i) {
               ProgramStatement statement = (ProgramStatement)this.machineList.get(i);
               statement.buildMachineStatementFromBasicStatement(this.errors);
               if (Globals.debug) {
                  System.out.println(statement);
               }

               try {
                  Globals.memory.setStatement(statement.getAddress(), statement);
               } catch (AddressErrorException var19) {
                  Token t = statement.getOriginalTokenList().get(0);
                  this.errors.add(new ErrorMessage(t.getSourceMIPSprogram(), t.getSourceLine(), t.getStartPos(), "Invalid address for text segment: " + var19.getAddress()));
               }
            }

            SystemIO.resetFiles();
            Collections.sort(this.machineList, new ProgramStatementComparator());
            this.catchDuplicateAddresses(this.machineList, this.errors);
            if (!this.errors.errorsOccurred() && (!this.errors.warningsOccurred() || !warningsAreErrors)) {
               return this.machineList;
            } else {
               throw new ProcessingException(this.errors);
            }
         }
      } else {
         return null;
      }
   }

   private void catchDuplicateAddresses(ArrayList instructions, ErrorList errors) {
      for(int i = 0; i < instructions.size() - 1; ++i) {
         ProgramStatement ps1 = (ProgramStatement)instructions.get(i);
         ProgramStatement ps2 = (ProgramStatement)instructions.get(i + 1);
         if (ps1.getAddress() == ps2.getAddress()) {
            errors.add(new ErrorMessage(ps2.getSourceMIPSprogram(), ps2.getSourceLine(), 0, "Duplicate text segment address: " + NumberDisplayBaseChooser.formatUnsignedInteger(ps2.getAddress(), Globals.getSettings().getDisplayAddressesInHex() ? 16 : 10) + " already occupied by " + ps1.getSourceFile() + " line " + ps1.getSourceLine() + " (caused by use of " + (Memory.inTextSegment(ps2.getAddress()) ? ".text" : ".ktext") + " operand)"));
         }
      }

   }

   private ArrayList parseLine(TokenList tokenList, String source, int sourceLineNumber, boolean extendedAssemblerEnabled) {
      ArrayList ret = new ArrayList();
      TokenList tokens = this.stripComment(tokenList);
      MacroPool macroPool = this.fileCurrentlyBeingAssembled.getLocalMacroPool();
      if (this.inMacroSegment) {
         this.detectLabels(tokens, macroPool.getCurrent());
      } else {
         this.stripLabels(tokens);
      }

      if (tokens.isEmpty()) {
         return null;
      } else {
         Token token = tokens.get(0);
         TokenTypes tokenType = token.getType();
         if (tokenType == TokenTypes.DIRECTIVE) {
            this.executeDirective(tokens);
            return null;
         } else if (this.inMacroSegment) {
            return null;
         } else {
            TokenList parenFreeTokens = tokens;
            if (tokens.size() > 2 && tokens.get(1).getType() == TokenTypes.LEFT_PAREN && tokens.get(tokens.size() - 1).getType() == TokenTypes.RIGHT_PAREN) {
               parenFreeTokens = (TokenList)tokens.clone();
               parenFreeTokens.remove(tokens.size() - 1);
               parenFreeTokens.remove(1);
            }

            Macro macro = macroPool.getMatchingMacro(parenFreeTokens, sourceLineNumber);
            if (macro != null) {
               tokens = parenFreeTokens;
               int counter = macroPool.getNextCounter();
               if (macroPool.pushOnCallStack(token)) {
                  this.errors.add(new ErrorMessage(this.fileCurrentlyBeingAssembled, parenFreeTokens.get(0).getSourceLine(), 0, "Detected a macro expansion loop (recursive reference). "));
               } else {
                  for(int i = macro.getFromLine() + 1; i < macro.getToLine(); ++i) {
                     String substituted = macro.getSubstitutedLine(i, tokens, (long)counter, this.errors);
                     TokenList tokenList2 = this.fileCurrentlyBeingAssembled.getTokenizer().tokenizeLine(i, substituted, this.errors);
                     if (tokenList2.getProcessedLine().length() > 0) {
                        substituted = tokenList2.getProcessedLine();
                     }

                     ArrayList statements = this.parseLine(tokenList2, "<" + (i - macro.getFromLine() + macro.getOriginalFromLine()) + "> " + substituted.trim(), sourceLineNumber, extendedAssemblerEnabled);
                     if (statements != null) {
                        ret.addAll(statements);
                     }
                  }

                  macroPool.popFromCallStack();
               }

               return ret;
            } else if (tokenType == TokenTypes.IDENTIFIER && token.getValue().charAt(0) == '.') {
               this.errors.add(new ErrorMessage(true, token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "MARS does not recognize the " + token.getValue() + " directive.  Ignored."));
               return null;
            } else if (this.inDataSegment && (tokenType == TokenTypes.PLUS || tokenType == TokenTypes.MINUS || tokenType == TokenTypes.QUOTED_STRING || tokenType == TokenTypes.IDENTIFIER || TokenTypes.isIntegerTokenType(tokenType) || TokenTypes.isFloatingTokenType(tokenType))) {
               this.executeDirectiveContinuation(tokens);
               return null;
            } else {
               if (!this.inDataSegment) {
                  ArrayList instrMatches = this.matchInstruction(token);
                  if (instrMatches == null) {
                     return ret;
                  }

                  Instruction inst = OperandFormat.bestOperandMatch(tokens, instrMatches);
                  if (inst instanceof ExtendedInstruction && !extendedAssemblerEnabled) {
                     this.errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "Extended (pseudo) instruction or format not permitted.  See Settings."));
                  }

                  if (OperandFormat.tokenOperandMatch(tokens, inst, this.errors)) {
                     ProgramStatement programStatement = new ProgramStatement(this.fileCurrentlyBeingAssembled, source, tokenList, tokens, inst, this.textAddress.get(), sourceLineNumber);
                     int instLength = inst.getInstructionLength();
                     if (this.compactTranslationCanBeApplied(programStatement)) {
                        instLength = ((ExtendedInstruction)inst).getCompactInstructionLength();
                     }

                     this.textAddress.increment(instLength);
                     ret.add(programStatement);
                     return ret;
                  }
               }

               return null;
            }
         }
      }
   }

   private void detectLabels(TokenList tokens, Macro current) {
      if (this.tokenListBeginsWithLabel(tokens)) {
         current.addLabel(tokens.get(0).getValue());
      }

   }

   private boolean compactTranslationCanBeApplied(ProgramStatement statement) {
      return statement.getInstruction() instanceof ExtendedInstruction && Globals.memory.usingCompactMemoryConfiguration() && ((ExtendedInstruction)statement.getInstruction()).hasCompactTranslation();
   }

   private TokenList stripComment(TokenList tokenList) {
      if (tokenList.isEmpty()) {
         return tokenList;
      } else {
         TokenList tokens = (TokenList)tokenList.clone();
         int last = tokens.size() - 1;
         if (tokens.get(last).getType() == TokenTypes.COMMENT) {
            tokens.remove(last);
         }

         return tokens;
      }
   }

   private void stripLabels(TokenList tokens) {
      boolean thereWasLabel = this.parseAndRecordLabel(tokens);
      if (thereWasLabel) {
         tokens.remove(0);
         tokens.remove(0);
      }

   }

   private boolean parseAndRecordLabel(TokenList tokens) {
      if (tokens.size() < 2) {
         return false;
      } else {
         Token token = tokens.get(0);
         if (this.tokenListBeginsWithLabel(tokens)) {
            if (token.getType() == TokenTypes.OPERATOR) {
               token.setType(TokenTypes.IDENTIFIER);
            }

            this.fileCurrentlyBeingAssembled.getLocalSymbolTable().addSymbol(token, this.inDataSegment ? this.dataAddress.get() : this.textAddress.get(), this.inDataSegment, this.errors);
            return true;
         } else {
            return false;
         }
      }
   }

   private boolean tokenListBeginsWithLabel(TokenList tokens) {
      if (tokens.size() < 2) {
         return false;
      } else {
         return (tokens.get(0).getType() == TokenTypes.IDENTIFIER || tokens.get(0).getType() == TokenTypes.OPERATOR) && tokens.get(1).getType() == TokenTypes.COLON;
      }
   }

   private void executeDirective(TokenList tokens) {
      Token token = tokens.get(0);
      Directives direct = Directives.matchDirective(token.getValue());
      if (Globals.debug) {
         System.out.println("line " + token.getSourceLine() + " is directive " + direct);
      }

      if (direct == null) {
         this.errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + token.getValue() + "\" directive is invalid or not implemented in MARS"));
      } else {
         if (direct != Directives.EQV) {
            if (direct == Directives.MACRO) {
               if (tokens.size() < 2) {
                  this.errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + token.getValue() + "\" directive requires at least one argument."));
                  return;
               }

               if (tokens.get(1).getType() != TokenTypes.IDENTIFIER) {
                  this.errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), tokens.get(1).getStartPos(), "Invalid Macro name \"" + tokens.get(1).getValue() + "\""));
                  return;
               }

               if (this.inMacroSegment) {
                  this.errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "Nested macros are not allowed"));
                  return;
               }

               this.inMacroSegment = true;
               MacroPool pool = this.fileCurrentlyBeingAssembled.getLocalMacroPool();
               pool.beginMacro(tokens.get(1));

               for(int i = 2; i < tokens.size(); ++i) {
                  Token arg = tokens.get(i);
                  if (arg.getType() != TokenTypes.RIGHT_PAREN && arg.getType() != TokenTypes.LEFT_PAREN) {
                     if (!Macro.tokenIsMacroParameter(arg.getValue(), true)) {
                        this.errors.add(new ErrorMessage(arg.getSourceMIPSprogram(), arg.getSourceLine(), arg.getStartPos(), "Invalid macro argument '" + arg.getValue() + "'"));
                        return;
                     }

                     pool.getCurrent().addArg(arg.getValue());
                  }
               }
            } else if (direct == Directives.END_MACRO) {
               if (tokens.size() > 1) {
                  this.errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "invalid text after .END_MACRO"));
                  return;
               }

               if (!this.inMacroSegment) {
                  this.errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), ".END_MACRO without .MACRO"));
                  return;
               }

               this.inMacroSegment = false;
               this.fileCurrentlyBeingAssembled.getLocalMacroPool().commitMacro(token);
            } else {
               if (this.inMacroSegment) {
                  return;
               }

               if (direct != Directives.DATA && direct != Directives.KDATA) {
                  if (direct != Directives.TEXT && direct != Directives.KTEXT) {
                     if (direct != Directives.WORD && direct != Directives.HALF && direct != Directives.BYTE && direct != Directives.FLOAT && direct != Directives.DOUBLE) {
                        if (direct != Directives.ASCII && direct != Directives.ASCIIZ) {
                           int i;
                           if (direct == Directives.ALIGN) {
                              if (this.passesDataSegmentCheck(token)) {
                                 if (tokens.size() != 2) {
                                    this.errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + token.getValue() + "\" requires one operand"));
                                    return;
                                 }

                                 if (!TokenTypes.isIntegerTokenType(tokens.get(1).getType()) || Binary.stringToInt(tokens.get(1).getValue()) < 0) {
                                    this.errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + token.getValue() + "\" requires a non-negative integer"));
                                    return;
                                 }

                                 i = Binary.stringToInt(tokens.get(1).getValue());
                                 if (i == 0) {
                                    this.autoAlign = false;
                                 } else {
                                    this.dataAddress.set(this.alignToBoundary(this.dataAddress.get(), (int)Math.pow(2.0, (double)i)));
                                 }
                              }
                           } else if (direct == Directives.SPACE) {
                              if (this.passesDataSegmentCheck(token)) {
                                 if (tokens.size() != 2) {
                                    this.errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + token.getValue() + "\" requires one operand"));
                                    return;
                                 }

                                 if (!TokenTypes.isIntegerTokenType(tokens.get(1).getType()) || Binary.stringToInt(tokens.get(1).getValue()) < 0) {
                                    this.errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + token.getValue() + "\" requires a non-negative integer"));
                                    return;
                                 }

                                 i = Binary.stringToInt(tokens.get(1).getValue());
                                 this.dataAddress.increment(i);
                              }
                           } else if (direct == Directives.EXTERN) {
                              if (tokens.size() != 3) {
                                 this.errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + token.getValue() + "\" directive requires two operands (label and size)."));
                                 return;
                              }

                              if (!TokenTypes.isIntegerTokenType(tokens.get(2).getType()) || Binary.stringToInt(tokens.get(2).getValue()) < 0) {
                                 this.errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + token.getValue() + "\" requires a non-negative integer size"));
                                 return;
                              }

                              i = Binary.stringToInt(tokens.get(2).getValue());
                              if (Globals.symbolTable.getAddress(tokens.get(1).getValue()) == -1) {
                                 Globals.symbolTable.addSymbol(tokens.get(1), this.externAddress, true, this.errors);
                                 this.externAddress += i;
                              }
                           } else if (direct == Directives.SET) {
                              this.errors.add(new ErrorMessage(true, token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "MARS currently ignores the .set directive."));
                           } else {
                              if (direct != Directives.GLOBL) {
                                 this.errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + token.getValue() + "\" directive recognized but not yet implemented."));
                                 return;
                              }

                              if (tokens.size() < 2) {
                                 this.errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + token.getValue() + "\" directive requires at least one argument."));
                                 return;
                              }

                              for(i = 1; i < tokens.size(); ++i) {
                                 Token label = tokens.get(i);
                                 if (label.getType() != TokenTypes.IDENTIFIER) {
                                    this.errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + token.getValue() + "\" directive argument must be label."));
                                    return;
                                 }

                                 this.globalDeclarationList.add(label);
                              }
                           }
                        } else {
                           this.dataDirective = direct;
                           if (this.passesDataSegmentCheck(token)) {
                              this.storeStrings(tokens, direct, this.errors);
                           }
                        }
                     } else {
                        this.dataDirective = direct;
                        if (this.passesDataSegmentCheck(token) && tokens.size() > 1) {
                           this.storeNumeric(tokens, direct, this.errors);
                        }
                     }
                  } else {
                     this.inDataSegment = false;
                     this.textAddress.setAddressSpace(direct == Directives.TEXT ? 0 : 1);
                     if (tokens.size() > 1 && TokenTypes.isIntegerTokenType(tokens.get(1).getType())) {
                        this.textAddress.set(Binary.stringToInt(tokens.get(1).getValue()));
                     }
                  }
               } else {
                  this.inDataSegment = true;
                  this.autoAlign = true;
                  this.dataAddress.setAddressSpace(direct == Directives.DATA ? 0 : 1);
                  if (tokens.size() > 1 && TokenTypes.isIntegerTokenType(tokens.get(1).getType())) {
                     this.dataAddress.set(Binary.stringToInt(tokens.get(1).getValue()));
                  }
               }
            }
         }

      }
   }

   private void transferGlobals() {
      for(int i = 0; i < this.globalDeclarationList.size(); ++i) {
         Token label = this.globalDeclarationList.get(i);
         Symbol symtabEntry = this.fileCurrentlyBeingAssembled.getLocalSymbolTable().getSymbol(label.getValue());
         if (symtabEntry == null) {
            this.errors.add(new ErrorMessage(this.fileCurrentlyBeingAssembled, label.getSourceLine(), label.getStartPos(), "\"" + label.getValue() + "\" declared global label but not defined."));
         } else if (Globals.symbolTable.getAddress(label.getValue()) != -1) {
            this.errors.add(new ErrorMessage(this.fileCurrentlyBeingAssembled, label.getSourceLine(), label.getStartPos(), "\"" + label.getValue() + "\" already defined as global in a different file."));
         } else {
            this.fileCurrentlyBeingAssembled.getLocalSymbolTable().removeSymbol(label);
            Globals.symbolTable.addSymbol(label, symtabEntry.getAddress(), symtabEntry.getType(), this.errors);
         }
      }

   }

   private void executeDirectiveContinuation(TokenList tokens) {
      Directives direct = this.dataDirective;
      if (direct != Directives.WORD && direct != Directives.HALF && direct != Directives.BYTE && direct != Directives.FLOAT && direct != Directives.DOUBLE) {
         if ((direct == Directives.ASCII || direct == Directives.ASCIIZ) && this.passesDataSegmentCheck(tokens.get(0))) {
            this.storeStrings(tokens, direct, this.errors);
         }
      } else if (tokens.size() > 0) {
         this.storeNumeric(tokens, direct, this.errors);
      }

   }

   private ArrayList matchInstruction(Token token) {
      if (token.getType() != TokenTypes.OPERATOR) {
         if (token.getSourceMIPSprogram().getLocalMacroPool().matchesAnyMacroName(token.getValue())) {
            this.errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "forward reference or invalid parameters for macro \"" + token.getValue() + "\""));
         } else {
            this.errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + token.getValue() + "\" is not a recognized operator"));
         }

         return null;
      } else {
         ArrayList inst = Globals.instructionSet.matchOperator(token.getValue());
         if (inst == null) {
            this.errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "Internal Assembler error: \"" + token.getValue() + "\" tokenized OPERATOR then not recognized"));
         }

         return inst;
      }
   }

   private void storeNumeric(TokenList tokens, Directives directive, ErrorList errors) {
      Token token = tokens.get(0);
      if (this.passesDataSegmentCheck(token)) {
         int tokenStart = 0;
         if (token.getType() == TokenTypes.DIRECTIVE) {
            tokenStart = 1;
         }

         int lengthInBytes = DataTypes.getLengthInBytes(directive);
         if (tokens.size() == 4 && tokens.get(2).getType() == TokenTypes.COLON) {
            Token valueToken = tokens.get(1);
            Token repetitionsToken = tokens.get(3);
            if ((Directives.isIntegerDirective(directive) && TokenTypes.isIntegerTokenType(valueToken.getType()) || Directives.isFloatingDirective(directive) && (TokenTypes.isIntegerTokenType(valueToken.getType()) || TokenTypes.isFloatingTokenType(valueToken.getType()))) && TokenTypes.isIntegerTokenType(repetitionsToken.getType())) {
               int repetitions = Binary.stringToInt(repetitionsToken.getValue());
               if (repetitions <= 0) {
                  errors.add(new ErrorMessage(this.fileCurrentlyBeingAssembled, repetitionsToken.getSourceLine(), repetitionsToken.getStartPos(), "repetition factor must be positive"));
               } else {
                  if (this.inDataSegment) {
                     if (this.autoAlign) {
                        this.dataAddress.set(this.alignToBoundary(this.dataAddress.get(), lengthInBytes));
                     }

                     for(int i = 0; i < repetitions; ++i) {
                        if (Directives.isIntegerDirective(directive)) {
                           this.storeInteger(valueToken, directive, errors);
                        } else {
                           this.storeRealNumber(valueToken, directive, errors);
                        }
                     }
                  }

               }
            } else {
               errors.add(new ErrorMessage(this.fileCurrentlyBeingAssembled, valueToken.getSourceLine(), valueToken.getStartPos(), "malformed expression"));
            }
         } else {
            for(int i = tokenStart; i < tokens.size(); ++i) {
               token = tokens.get(i);
               if (Directives.isIntegerDirective(directive)) {
                  this.storeInteger(token, directive, errors);
               }

               if (Directives.isFloatingDirective(directive)) {
                  this.storeRealNumber(token, directive, errors);
               }
            }

         }
      }
   }

   private void storeInteger(Token token, Directives directive, ErrorList errors) {
      int lengthInBytes = DataTypes.getLengthInBytes(directive);
      int value;
      if (TokenTypes.isIntegerTokenType(token.getType())) {
         value = Binary.stringToInt(token.getValue());
         if (directive == Directives.BYTE) {
            value &= 255;
         } else if (directive == Directives.HALF) {
            value &= 65535;
         }

         if (DataTypes.outOfRange(directive, value)) {
            errors.add(new ErrorMessage(true, token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + token.getValue() + "\" is out-of-range for a signed value and possibly truncated"));
         }

         if (this.inDataSegment) {
            this.writeToDataSegment(value, lengthInBytes, token, errors);
         } else {
            try {
               Globals.memory.set(this.textAddress.get(), value, lengthInBytes);
            } catch (AddressErrorException var8) {
               errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + this.textAddress.get() + "\" is not a valid text segment address"));
               return;
            }

            this.textAddress.increment(lengthInBytes);
         }
      } else if (token.getType() == TokenTypes.IDENTIFIER) {
         if (this.inDataSegment) {
            value = this.fileCurrentlyBeingAssembled.getLocalSymbolTable().getAddressLocalOrGlobal(token.getValue());
            if (value == -1) {
               int dataAddress = this.writeToDataSegment(0, lengthInBytes, token, errors);
               this.currentFileDataSegmentForwardReferences.add(dataAddress, lengthInBytes, token);
            } else {
               this.writeToDataSegment(value, lengthInBytes, token, errors);
            }
         } else {
            errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + token.getValue() + "\" label as directive operand not permitted in text segment"));
         }
      } else {
         errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + token.getValue() + "\" is not a valid integer constant or label"));
      }

   }

   private void storeRealNumber(Token token, Directives directive, ErrorList errors) {
      int lengthInBytes = DataTypes.getLengthInBytes(directive);
      if (!TokenTypes.isIntegerTokenType(token.getType()) && !TokenTypes.isFloatingTokenType(token.getType())) {
         errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + token.getValue() + "\" is not a valid floating point constant"));
      } else {
         double value;
         try {
            value = Double.parseDouble(token.getValue());
         } catch (NumberFormatException var8) {
            errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + token.getValue() + "\" is not a valid floating point constant"));
            return;
         }

         if (DataTypes.outOfRange(directive, value)) {
            errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + token.getValue() + "\" is an out-of-range value"));
         } else {
            if (directive == Directives.FLOAT) {
               this.writeToDataSegment(Float.floatToIntBits((float)value), lengthInBytes, token, errors);
            }

            if (directive == Directives.DOUBLE) {
               this.writeDoubleToDataSegment(value, token, errors);
            }

         }
      }
   }

   private void storeStrings(TokenList tokens, Directives direct, ErrorList errors) {
      int tokenStart = 0;
      if (tokens.get(0).getType() == TokenTypes.DIRECTIVE) {
         tokenStart = 1;
      }

      for(int i = tokenStart; i < tokens.size(); ++i) {
         Token token = tokens.get(i);
         if (token.getType() != TokenTypes.QUOTED_STRING) {
            errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + token.getValue() + "\" is not a valid character string"));
         } else {
            String quote = token.getValue();

            for(int j = 1; j < quote.length() - 1; ++j) {
               char theChar = quote.charAt(j);
               if (theChar == '\\') {
                  ++j;
                  theChar = quote.charAt(j);
                  switch (theChar) {
                     case '"':
                        theChar = '"';
                        break;
                     case '\'':
                        theChar = '\'';
                        break;
                     case '0':
                        theChar = 0;
                        break;
                     case '\\':
                        theChar = '\\';
                        break;
                     case 'b':
                        theChar = '\b';
                        break;
                     case 'f':
                        theChar = '\f';
                        break;
                     case 'n':
                        theChar = '\n';
                        break;
                     case 'r':
                        theChar = '\r';
                        break;
                     case 't':
                        theChar = '\t';
                  }
               }

               try {
                  Globals.memory.set(this.dataAddress.get(), theChar, 1);
               } catch (AddressErrorException var12) {
                  errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + this.dataAddress.get() + "\" is not a valid data segment address"));
               }

               this.dataAddress.increment(1);
            }

            if (direct == Directives.ASCIIZ) {
               try {
                  Globals.memory.set(this.dataAddress.get(), 0, 1);
               } catch (AddressErrorException var11) {
                  errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + this.dataAddress.get() + "\" is not a valid data segment address"));
               }

               this.dataAddress.increment(1);
            }
         }
      }

   }

   private boolean passesDataSegmentCheck(Token token) {
      if (!this.inDataSegment) {
         this.errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + token.getValue() + "\" directive cannot appear in text segment"));
         return false;
      } else {
         return true;
      }
   }

   private int writeToDataSegment(int value, int lengthInBytes, Token token, ErrorList errors) {
      if (this.autoAlign) {
         this.dataAddress.set(this.alignToBoundary(this.dataAddress.get(), lengthInBytes));
      }

      try {
         Globals.memory.set(this.dataAddress.get(), value, lengthInBytes);
      } catch (AddressErrorException var6) {
         errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + this.dataAddress.get() + "\" is not a valid data segment address"));
         return this.dataAddress.get();
      }

      int address = this.dataAddress.get();
      this.dataAddress.increment(lengthInBytes);
      return address;
   }

   private void writeDoubleToDataSegment(double value, Token token, ErrorList errors) {
      int lengthInBytes = 8;
      if (this.autoAlign) {
         this.dataAddress.set(this.alignToBoundary(this.dataAddress.get(), lengthInBytes));
      }

      try {
         Globals.memory.setDouble(this.dataAddress.get(), value);
      } catch (AddressErrorException var7) {
         errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + this.dataAddress.get() + "\" is not a valid data segment address"));
         return;
      }

      this.dataAddress.increment(lengthInBytes);
   }

   private int alignToBoundary(int address, int byteBoundary) {
      int remainder = address % byteBoundary;
      if (remainder == 0) {
         return address;
      } else {
         int alignedAddress = address + byteBoundary - remainder;
         this.fileCurrentlyBeingAssembled.getLocalSymbolTable().fixSymbolTableAddress(address, alignedAddress);
         return alignedAddress;
      }
   }

   private class DataSegmentForwardReferences {
      private ArrayList forwardReferenceList;

      private DataSegmentForwardReferences() {
         this.forwardReferenceList = new ArrayList();
      }

      private int size() {
         return this.forwardReferenceList.size();
      }

      private void add(int patchAddress, int length, Token token) {
         this.forwardReferenceList.add(new DataSegmentForwardReference(patchAddress, length, token));
      }

      private void add(DataSegmentForwardReferences another) {
         this.forwardReferenceList.addAll(another.forwardReferenceList);
      }

      private void clear() {
         this.forwardReferenceList.clear();
      }

      private int resolve(SymbolTable localSymtab) {
         int count = 0;

         for(int i = 0; i < this.forwardReferenceList.size(); ++i) {
            DataSegmentForwardReference entry = (DataSegmentForwardReference)this.forwardReferenceList.get(i);
            int labelAddress = localSymtab.getAddressLocalOrGlobal(entry.token.getValue());
            if (labelAddress != -1) {
               try {
                  Globals.memory.set(entry.patchAddress, labelAddress, entry.length);
               } catch (AddressErrorException var7) {
               }

               this.forwardReferenceList.remove(i);
               --i;
               ++count;
            }
         }

         return count;
      }

      private void generateErrorMessages(ErrorList errors) {
         for(int i = 0; i < this.forwardReferenceList.size(); ++i) {
            DataSegmentForwardReference entry = (DataSegmentForwardReference)this.forwardReferenceList.get(i);
            errors.add(new ErrorMessage(entry.token.getSourceMIPSprogram(), entry.token.getSourceLine(), entry.token.getStartPos(), "Symbol \"" + entry.token.getValue() + "\" not found in symbol table."));
         }

      }

      // $FF: synthetic method
      DataSegmentForwardReferences(Object x1) {
         this();
      }

      private class DataSegmentForwardReference {
         int patchAddress;
         int length;
         Token token;

         DataSegmentForwardReference(int patchAddress, int length, Token token) {
            this.patchAddress = patchAddress;
            this.length = length;
            this.token = token;
         }
      }
   }

   private class UserKernelAddressSpace {
      int[] address;
      int currentAddressSpace;
      private final int USER;
      private final int KERNEL;

      private UserKernelAddressSpace(int userBase, int kernelBase) {
         this.USER = 0;
         this.KERNEL = 1;
         this.address = new int[2];
         this.address[0] = userBase;
         this.address[1] = kernelBase;
         this.currentAddressSpace = 0;
      }

      private int get() {
         return this.address[this.currentAddressSpace];
      }

      private void set(int value) {
         this.address[this.currentAddressSpace] = value;
      }

      private void increment(int increment) {
         int[] var10000 = this.address;
         int var10001 = this.currentAddressSpace;
         var10000[var10001] += increment;
      }

      private void setAddressSpace(int addressSpace) {
         if (addressSpace != 0 && addressSpace != 1) {
            throw new IllegalArgumentException();
         } else {
            this.currentAddressSpace = addressSpace;
         }
      }

      // $FF: synthetic method
      UserKernelAddressSpace(int x1, int x2, Object x3) {
         this(x1, x2);
      }
   }

   private class ProgramStatementComparator implements Comparator {
      private ProgramStatementComparator() {
      }

      public int compare(Object obj1, Object obj2) {
         if (obj1 instanceof ProgramStatement && obj2 instanceof ProgramStatement) {
            int addr1 = ((ProgramStatement)obj1).getAddress();
            int addr2 = ((ProgramStatement)obj2).getAddress();
            return (addr1 >= 0 || addr2 < 0) && (addr1 < 0 || addr2 >= 0) ? addr1 - addr2 : addr2;
         } else {
            throw new ClassCastException();
         }
      }

      public boolean equals(Object obj) {
         return this == obj;
      }

      // $FF: synthetic method
      ProgramStatementComparator(Object x1) {
         this();
      }
   }
}
