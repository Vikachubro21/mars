package mars;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import javax.swing.*;

import mars.MarsDark1;
import mars.mips.dump.DumpFormat;
import mars.mips.dump.DumpFormatLoader;
import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.InvalidRegisterAccessException;
import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryAccessNotice;
import mars.mips.hardware.MemoryConfiguration;
import mars.mips.hardware.MemoryConfigurations;
import mars.mips.hardware.RegisterFile;
import mars.simulator.ProgramArgumentList;
import mars.util.Binary;
import mars.util.FilenameFinder;
import mars.util.MemoryDump;
import mars.venus.VenusUI;

public class MarsLaunch {
   private boolean simulate;
   private int displayFormat;
   private boolean verbose;
   private boolean assembleProject;
   private boolean pseudo;
   private boolean delayedBranching;
   private boolean warningsAreErrors;
   private boolean startAtMain;
   private boolean countInstructions;
   private boolean selfModifyingCode;
   private static final String rangeSeparator = "-";
   private static final int splashDuration = 2000;
   private static final int memoryWordsPerLine = 4;
   private static final int DECIMAL = 0;
   private static final int HEXADECIMAL = 1;
   private static final int ASCII = 2;
   private ArrayList registerDisplayList;
   private ArrayList memoryDisplayList;
   private ArrayList filenameList;
   private MIPSprogram code;
   private int maxSteps;
   private int instructionCount;
   private PrintStream out;
   private ArrayList dumpTriples = null;
   private ArrayList programArgumentList;
   private int assembleErrorExitCode;
   private int simulateErrorExitCode;

   public MarsLaunch(String[] args) {
      boolean gui = args.length == 0;
      Globals.initialize(gui);
      if (gui) {
         if(Globals.getSettings().getBooleanSetting(21))
            MarsDark1.setup();
         this.launchIDE();
      } else {
         System.setProperty("java.awt.headless", "true");
         this.simulate = true;
         this.displayFormat = 0;
         this.verbose = true;
         this.assembleProject = false;
         this.pseudo = true;
         this.delayedBranching = false;
         this.warningsAreErrors = false;
         this.startAtMain = false;
         this.countInstructions = false;
         this.selfModifyingCode = false;
         this.instructionCount = 0;
         this.assembleErrorExitCode = 0;
         this.simulateErrorExitCode = 0;
         this.registerDisplayList = new ArrayList();
         this.memoryDisplayList = new ArrayList();
         this.filenameList = new ArrayList();
         MemoryConfigurations.setCurrentConfiguration(MemoryConfigurations.getDefaultConfiguration());
         this.code = new MIPSprogram();
         this.maxSteps = -1;
         this.out = System.out;
         if (this.parseCommandArgs(args)) {
            if (this.runCommand()) {
               this.displayMiscellaneousPostMortem();
               this.displayRegistersPostMortem();
               this.displayMemoryPostMortem();
            }

            this.dumpSegments();
         }

         System.exit(Globals.exitCode);
      }

   }

   private void dumpSegments() {
      if (this.dumpTriples != null) {
         for(int i = 0; i < this.dumpTriples.size(); ++i) {
            String[] triple = (String[])this.dumpTriples.get(i);
            File file = new File(triple[2]);
            Integer[] segInfo = MemoryDump.getSegmentBounds(triple[0]);
            if (segInfo == null) {
               try {
                  String[] memoryRange = this.checkMemoryAddressRange(triple[0]);
                  segInfo = new Integer[]{new Integer(Binary.stringToInt(memoryRange[0])), new Integer(Binary.stringToInt(memoryRange[1]))};
               } catch (NumberFormatException var9) {
                  segInfo = null;
               } catch (NullPointerException var10) {
                  segInfo = null;
               }
            }

            if (segInfo == null) {
               this.out.println("Error while attempting to save dump, segment/address-range " + triple[0] + " is invalid!");
            } else {
               DumpFormatLoader loader = new DumpFormatLoader();
               ArrayList dumpFormats = loader.loadDumpFormats();
               DumpFormat format = DumpFormatLoader.findDumpFormatGivenCommandDescriptor(dumpFormats, triple[1]);
               if (format == null) {
                  this.out.println("Error while attempting to save dump, format " + triple[1] + " was not found!");
               } else {
                  try {
                     int highAddress = Globals.memory.getAddressOfFirstNull(segInfo[0], segInfo[1]) - 4;
                     if (highAddress < segInfo[0]) {
                        this.out.println("This segment has not been written to, there is nothing to dump.");
                     } else {
                        format.dumpMemoryRange(file, segInfo[0], highAddress);
                     }
                  } catch (FileNotFoundException var11) {
                     this.out.println("Error while attempting to save dump, file " + file + " was not found!");
                  } catch (AddressErrorException var12) {
                     this.out.println("Error while attempting to save dump, file " + file + "!  Could not access address: " + var12.getAddress() + "!");
                  } catch (IOException var13) {
                     this.out.println("Error while attempting to save dump, file " + file + "!  Disk IO failed!");
                  }
               }
            }
         }

      }
   }

   private void launchIDE() {
      (new MarsSplashScreen(2000)).showSplash();
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            new VenusUI("MARS Plus");
         }
      });
   }

   private boolean parseCommandArgs(String[] args) {
      String noCopyrightSwitch = "nc";
      String displayMessagesToErrSwitch = "me";
      boolean argsOK = true;
      boolean inProgramArgumentList = false;
      this.programArgumentList = null;
      if (args.length == 0) {
         return true;
      } else {
         this.processDisplayMessagesToErrSwitch(args, displayMessagesToErrSwitch);
         this.displayCopyright(args, noCopyrightSwitch);
         if (args.length == 1 && args[0].equals("h")) {
            this.displayHelp();
            return false;
         } else {
            for(int i = 0; i < args.length; ++i) {
               if (inProgramArgumentList) {
                  if (this.programArgumentList == null) {
                     this.programArgumentList = new ArrayList();
                  }

                  this.programArgumentList.add(args[i]);
               } else if (args[i].toLowerCase().equals("pa")) {
                  inProgramArgumentList = true;
               } else if (!args[i].toLowerCase().equals(displayMessagesToErrSwitch) && !args[i].toLowerCase().equals(noCopyrightSwitch)) {
                  if (args[i].toLowerCase().equals("dump")) {
                     if (args.length <= i + 3) {
                        this.out.println("Dump command line argument requires a segment, format and file name.");
                        argsOK = false;
                     } else {
                        if (this.dumpTriples == null) {
                           this.dumpTriples = new ArrayList();
                        }

                        ArrayList var10000 = this.dumpTriples;
                        String[] var10001 = new String[3];
                        ++i;
                        var10001[0] = args[i];
                        ++i;
                        var10001[1] = args[i];
                        ++i;
                        var10001[2] = args[i];
                        var10000.add(var10001);
                     }
                  } else {
                     String s;
                     if (args[i].toLowerCase().equals("mc")) {
                        ++i;
                        s = args[i];
                        MemoryConfiguration config = MemoryConfigurations.getConfigurationByName(s);
                        if (config == null) {
                           this.out.println("Invalid memory configuration: " + s);
                           argsOK = false;
                        } else {
                           MemoryConfigurations.setCurrentConfiguration(config);
                        }
                     } else {
                        if (args[i].toLowerCase().indexOf("ae") == 0) {
                           s = args[i].substring(2);

                           try {
                              this.assembleErrorExitCode = Integer.decode(s);
                              continue;
                           } catch (NumberFormatException var13) {
                           }
                        }

                        if (args[i].toLowerCase().indexOf("se") == 0) {
                           s = args[i].substring(2);

                           try {
                              this.simulateErrorExitCode = Integer.decode(s);
                              continue;
                           } catch (NumberFormatException var12) {
                           }
                        }

                        if (args[i].toLowerCase().equals("d")) {
                           Globals.debug = true;
                        } else if (args[i].toLowerCase().equals("a")) {
                           this.simulate = false;
                        } else if (!args[i].toLowerCase().equals("ad") && !args[i].toLowerCase().equals("da")) {
                           if (args[i].toLowerCase().equals("p")) {
                              this.assembleProject = true;
                           } else if (args[i].toLowerCase().equals("dec")) {
                              this.displayFormat = 0;
                           } else if (args[i].toLowerCase().equals("hex")) {
                              this.displayFormat = 1;
                           } else if (args[i].toLowerCase().equals("ascii")) {
                              this.displayFormat = 2;
                           } else if (args[i].toLowerCase().equals("b")) {
                              this.verbose = false;
                           } else if (args[i].toLowerCase().equals("db")) {
                              this.delayedBranching = true;
                           } else if (!args[i].toLowerCase().equals("np") && !args[i].toLowerCase().equals("ne")) {
                              if (args[i].toLowerCase().equals("we")) {
                                 this.warningsAreErrors = true;
                              } else if (args[i].toLowerCase().equals("sm")) {
                                 this.startAtMain = true;
                              } else if (args[i].toLowerCase().equals("smc")) {
                                 this.selfModifyingCode = true;
                              } else if (args[i].toLowerCase().equals("ic")) {
                                 this.countInstructions = true;
                              } else if (args[i].indexOf("$") == 0) {
                                 if (RegisterFile.getUserRegister(args[i]) == null && Coprocessor1.getRegister(args[i]) == null) {
                                    this.out.println("Invalid Register Name: " + args[i]);
                                 } else {
                                    this.registerDisplayList.add(args[i]);
                                 }
                              } else if (RegisterFile.getUserRegister("$" + args[i]) == null && Coprocessor1.getRegister("$" + args[i]) == null) {
                                 if ((new File(args[i])).exists()) {
                                    this.filenameList.add(args[i]);
                                 } else {
                                    try {
                                       Integer.decode(args[i]);
                                       this.maxSteps = Integer.decode(args[i]);
                                    } catch (NumberFormatException var11) {
                                       try {
                                          String[] memoryRange = this.checkMemoryAddressRange(args[i]);
                                          this.memoryDisplayList.add(memoryRange[0]);
                                          this.memoryDisplayList.add(memoryRange[1]);
                                       } catch (NumberFormatException var9) {
                                          this.out.println("Invalid/unaligned address or invalid range: " + args[i]);
                                          argsOK = false;
                                       } catch (NullPointerException var10) {
                                          this.out.println("Invalid Command Argument: " + args[i]);
                                          argsOK = false;
                                       }
                                    }
                                 }
                              } else {
                                 this.registerDisplayList.add("$" + args[i]);
                              }
                           } else {
                              this.pseudo = false;
                           }
                        } else {
                           Globals.debug = true;
                           this.simulate = false;
                        }
                     }
                  }
               }
            }

            return argsOK;
         }
      }
   }

   private boolean runCommand() {
      boolean programRan = false;
      if (this.filenameList.size() == 0) {
         return programRan;
      } else {
         try {
            Globals.getSettings().setBooleanSettingNonPersistent(8, this.delayedBranching);
            Globals.getSettings().setBooleanSettingNonPersistent(20, this.selfModifyingCode);
            File mainFile = (new File((String)this.filenameList.get(0))).getAbsoluteFile();
            ArrayList filesToAssemble;
            ArrayList moreFilesToAssemble;
            if (!this.assembleProject) {
               filesToAssemble = FilenameFinder.getFilenameList(this.filenameList, FilenameFinder.MATCH_ALL_EXTENSIONS);
            } else {
               filesToAssemble = FilenameFinder.getFilenameList(mainFile.getParent(), Globals.fileExtensions);
               if (this.filenameList.size() > 1) {
                  this.filenameList.remove(0);
                  moreFilesToAssemble = FilenameFinder.getFilenameList(this.filenameList, FilenameFinder.MATCH_ALL_EXTENSIONS);
                  int index2 = 0;

                  while(true) {
                     if (index2 >= moreFilesToAssemble.size()) {
                        filesToAssemble.addAll(moreFilesToAssemble);
                        break;
                     }

                     for(int index1 = 0; index1 < filesToAssemble.size(); ++index1) {
                        if (filesToAssemble.get(index1).equals(moreFilesToAssemble.get(index2))) {
                           moreFilesToAssemble.remove(index2);
                           --index2;
                           break;
                        }
                     }

                     ++index2;
                  }
               }
            }

            if (Globals.debug) {
               this.out.println("--------  TOKENIZING BEGINS  -----------");
            }

            moreFilesToAssemble = this.code.prepareFilesForAssembly(filesToAssemble, mainFile.getAbsolutePath(), (String)null);
            if (Globals.debug) {
               this.out.println("--------  ASSEMBLY BEGINS  -----------");
            }

            ErrorList warnings = this.code.assemble(moreFilesToAssemble, this.pseudo, this.warningsAreErrors);
            if (warnings != null && warnings.warningsOccurred()) {
               this.out.println(warnings.generateWarningReport());
            }

            RegisterFile.initializeProgramCounter(this.startAtMain);
            if (this.simulate) {
               (new ProgramArgumentList(this.programArgumentList)).storeProgramArguments();
               this.establishObserver();
               if (Globals.debug) {
                  this.out.println("--------  SIMULATION BEGINS  -----------");
               }

               programRan = true;
               boolean done = this.code.simulate(this.maxSteps);
               if (!done) {
                  this.out.println("\nProgram terminated when maximum step limit " + this.maxSteps + " reached.");
               }
            }

            if (Globals.debug) {
               this.out.println("\n--------  ALL PROCESSING COMPLETE  -----------");
            }
         } catch (ProcessingException var7) {
            Globals.exitCode = programRan ? this.simulateErrorExitCode : this.assembleErrorExitCode;
            this.out.println(var7.errors().generateErrorAndWarningReport());
            this.out.println("Processing terminated due to errors.");
         }

         return programRan;
      }
   }

   private String[] checkMemoryAddressRange(String arg) throws NumberFormatException {
      String[] memoryRange = null;
      if (arg.indexOf("-") > 0 && arg.indexOf("-") < arg.length() - 1) {
         memoryRange = new String[]{arg.substring(0, arg.indexOf("-")), arg.substring(arg.indexOf("-") + 1)};
         if (Binary.stringToInt(memoryRange[0]) > Binary.stringToInt(memoryRange[1]) || !Memory.wordAligned(Binary.stringToInt(memoryRange[0])) || !Memory.wordAligned(Binary.stringToInt(memoryRange[1]))) {
            throw new NumberFormatException();
         }
      }

      return memoryRange;
   }

   private void establishObserver() {
      if (this.countInstructions) {
         Observer instructionCounter = new Observer() {
            private int lastAddress = 0;

            public void update(Observable o, Object obj) {
               if (obj instanceof AccessNotice) {
                  AccessNotice notice = (AccessNotice)obj;
                  if (!notice.accessIsFromMIPS()) {
                     return;
                  }

                  if (notice.getAccessType() != 0) {
                     return;
                  }

                  MemoryAccessNotice m = (MemoryAccessNotice)notice;
                  int a = m.getAddress();
                  if (a == this.lastAddress) {
                     return;
                  }

                  this.lastAddress = a;
                  MarsLaunch var10000 = MarsLaunch.this;
                  var10000.instructionCount = var10000.instructionCount + 1;
               }

            }
         };

         try {
            Globals.memory.addObserver(instructionCounter, Memory.textBaseAddress, Memory.textLimitAddress);
         } catch (AddressErrorException var3) {
            this.out.println("Internal error: MarsLaunch uses incorrect text segment address for instruction observer");
         }
      }

   }

   private void displayMiscellaneousPostMortem() {
      if (this.countInstructions) {
         this.out.println("\n" + this.instructionCount);
      }

   }

   private void displayRegistersPostMortem() {
      this.out.println();
      Iterator regIter = this.registerDisplayList.iterator();

      while(regIter.hasNext()) {
         String reg = regIter.next().toString();
         if (RegisterFile.getUserRegister(reg) != null) {
            if (this.verbose) {
               this.out.print(reg + "\t");
            }

            int value = RegisterFile.getUserRegister(reg).getValue();
            this.out.println(this.formatIntForDisplay(value));
         } else {
            float fvalue = Coprocessor1.getFloatFromRegister(reg);
            int ivalue = Coprocessor1.getIntFromRegister(reg);
            double dvalue = Double.NaN;
            long lvalue = 0L;
            boolean hasDouble = false;

            try {
               dvalue = Coprocessor1.getDoubleFromRegisterPair(reg);
               lvalue = Coprocessor1.getLongFromRegisterPair(reg);
               hasDouble = true;
            } catch (InvalidRegisterAccessException var13) {
            }

            if (this.verbose) {
               this.out.print(reg + "\t");
            }

            if (this.displayFormat == 1) {
               this.out.print(Binary.binaryStringToHexString(Binary.intToBinaryString(ivalue)));
               if (hasDouble) {
                  this.out.println("\t" + Binary.binaryStringToHexString(Binary.longToBinaryString(lvalue)));
               } else {
                  this.out.println("");
               }
            } else if (this.displayFormat == 0) {
               this.out.print(fvalue);
               if (hasDouble) {
                  this.out.println("\t" + dvalue);
               } else {
                  this.out.println("");
               }
            } else {
               this.out.print(Binary.intToAscii(ivalue));
               if (hasDouble) {
                  this.out.println("\t" + Binary.intToAscii(Binary.highOrderLongToInt(lvalue)) + Binary.intToAscii(Binary.lowOrderLongToInt(lvalue)));
               } else {
                  this.out.println("");
               }
            }
         }
      }

   }

   private String formatIntForDisplay(int value) {
      String strValue;
      switch (this.displayFormat) {
         case 0:
            strValue = "" + value;
            break;
         case 1:
            strValue = Binary.intToHexString(value);
            break;
         case 2:
            strValue = Binary.intToAscii(value);
            break;
         default:
            strValue = Binary.intToHexString(value);
      }

      return strValue;
   }

   private void displayMemoryPostMortem() {
      Iterator memIter = this.memoryDisplayList.iterator();
      int addressStart = 0;
      int addressEnd = 0;

      while(memIter.hasNext()) {
         try {
            addressStart = Binary.stringToInt(memIter.next().toString());
            addressEnd = Binary.stringToInt(memIter.next().toString());
         } catch (NumberFormatException var8) {
         }

         int valuesDisplayed = 0;

         for(int addr = addressStart; addr <= addressEnd && (addr >= 0 || addressEnd <= 0); addr += 4) {
            if (valuesDisplayed % 4 == 0) {
               this.out.print(valuesDisplayed > 0 ? "\n" : "");
               if (this.verbose) {
                  this.out.print("Mem[" + Binary.intToHexString(addr) + "]\t");
               }
            }

            try {
               int value;
               if (!Memory.inTextSegment(addr) && !Memory.inKernelTextSegment(addr)) {
                  value = Globals.memory.getWord(addr);
               } else {
                  Integer iValue = Globals.memory.getRawWordOrNull(addr);
                  value = iValue == null ? 0 : iValue;
               }

               this.out.print(this.formatIntForDisplay(value) + "\t");
            } catch (AddressErrorException var9) {
               this.out.print("Invalid address: " + addr + "\t");
            }

            ++valuesDisplayed;
         }

         this.out.println();
      }

   }

   private void processDisplayMessagesToErrSwitch(String[] args, String displayMessagesToErrSwitch) {
      for(int i = 0; i < args.length; ++i) {
         if (args[i].toLowerCase().equals(displayMessagesToErrSwitch)) {
            this.out = System.err;
            return;
         }
      }

   }

   private void displayCopyright(String[] args, String noCopyrightSwitch) {
      boolean print = true;

      for(int i = 0; i < args.length; ++i) {
         if (args[i].toLowerCase().equals(noCopyrightSwitch)) {
            return;
         }
      }

      this.out.println("MARS 4.5  Copyright " + Globals.copyrightYears + " " + Globals.copyrightHolders + "\n");
   }

   private void displayHelp() {
      String[] segmentNames = MemoryDump.getSegmentNames();
      String segments = "";

      for(int i = 0; i < segmentNames.length; ++i) {
         segments = segments + segmentNames[i];
         if (i < segmentNames.length - 1) {
            segments = segments + ", ";
         }
      }

      ArrayList dumpFormats = (new DumpFormatLoader()).loadDumpFormats();
      String formats = "";

      for(int i = 0; i < dumpFormats.size(); ++i) {
         formats = formats + ((DumpFormat)dumpFormats.get(i)).getCommandDescriptor();
         if (i < dumpFormats.size() - 1) {
            formats = formats + ", ";
         }
      }

      this.out.println("Usage:  Mars  [options] filename [additional filenames]");
      this.out.println("  Valid options (not case sensitive, separate by spaces) are:");
      this.out.println("      a  -- assemble only, do not simulate");
      this.out.println("  ae<n>  -- terminate MARS with integer exit code <n> if an assemble error occurs.");
      this.out.println("  ascii  -- display memory or register contents interpreted as ASCII codes.");
      this.out.println("      b  -- brief - do not display register/memory address along with contents");
      this.out.println("      d  -- display MARS debugging statements");
      this.out.println("     db  -- MIPS delayed branching is enabled");
      this.out.println("    dec  -- display memory or register contents in decimal.");
      this.out.println("   dump <segment> <format> <file> -- memory dump of specified memory segment");
      this.out.println("            in specified format to specified file.  Option may be repeated.");
      this.out.println("            Dump occurs at the end of simulation unless 'a' option is used.");
      this.out.println("            Segment and format are case-sensitive and possible values are:");
      this.out.println("            <segment> = " + segments);
      this.out.println("            <format> = " + formats);
      this.out.println("      h  -- display this help.  Use by itself with no filename.");
      this.out.println("    hex  -- display memory or register contents in hexadecimal (default)");
      this.out.println("     ic  -- display count of MIPS basic instructions 'executed'");
      this.out.println("     mc <config>  -- set memory configuration.  Argument <config> is");
      this.out.println("            case-sensitive and possible values are: Default for the default");
      this.out.println("            32-bit address space, CompactDataAtZero for a 32KB memory with");
      this.out.println("            data segment at address 0, or CompactTextAtZero for a 32KB");
      this.out.println("            memory with text segment at address 0.");
      this.out.println("     me  -- display MARS messages to standard err instead of standard out. ");
      this.out.println("            Can separate messages from program output using redirection");
      this.out.println("     nc  -- do not display copyright notice (for cleaner redirected/piped output).");
      this.out.println("     np  -- use of pseudo instructions and formats not permitted");
      this.out.println("      p  -- Project mode - assemble all files in the same directory as given file.");
      this.out.println("  se<n>  -- terminate MARS with integer exit code <n> if a simulation (run) error occurs.");
      this.out.println("     sm  -- start execution at statement with global label main, if defined");
      this.out.println("    smc  -- Self Modifying Code - Program can write and branch to either text or data segment");
      this.out.println("    <n>  -- where <n> is an integer maximum count of steps to simulate.");
      this.out.println("            If 0, negative or not specified, there is no maximum.");
      this.out.println(" $<reg>  -- where <reg> is number or name (e.g. 5, t3, f10) of register whose ");
      this.out.println("            content to display at end of run.  Option may be repeated.");
      this.out.println("<reg_name>  -- where <reg_name> is name (e.g. t3, f10) of register whose");
      this.out.println("            content to display at end of run.  Option may be repeated. ");
      this.out.println("            The $ is not required.");
      this.out.println("<m>-<n>  -- memory address range from <m> to <n> whose contents to");
      this.out.println("            display at end of run. <m> and <n> may be hex or decimal,");
      this.out.println("            must be on word boundary, <m> <= <n>.  Option may be repeated.");
      this.out.println("     pa  -- Program Arguments follow in a space-separated list.  This");
      this.out.println("            option must be placed AFTER ALL FILE NAMES, because everything");
      this.out.println("            that follows it is interpreted as a program argument to be");
      this.out.println("            made available to the MIPS program at runtime.");
      this.out.println("If more than one filename is listed, the first is assumed to be the main");
      this.out.println("unless the global statement label 'main' is defined in one of the files.");
      this.out.println("Exception handler not automatically assembled.  Add it to the file list.");
      this.out.println("Options used here do not affect MARS Settings menu values and vice versa.");
   }
}
