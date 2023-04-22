package mars;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.AbstractAction;
import mars.assembler.Assembler;
import mars.assembler.MacroPool;
import mars.assembler.SourceLine;
import mars.assembler.SymbolTable;
import mars.assembler.Tokenizer;
import mars.mips.hardware.RegisterFile;
import mars.simulator.BackStepper;
import mars.simulator.Simulator;

public class MIPSprogram {
   private boolean steppedExecution = false;
   private String filename;
   private ArrayList sourceList;
   private ArrayList tokenList;
   private ArrayList parsedList;
   private ArrayList machineList;
   private BackStepper backStepper;
   private SymbolTable localSymbolTable;
   private MacroPool macroPool;
   private ArrayList sourceLineList;
   private Tokenizer tokenizer;

   public ArrayList getSourceList() {
      return this.sourceList;
   }

   public void setSourceLineList(ArrayList sourceLineList) {
      this.sourceLineList = sourceLineList;
      this.sourceList = new ArrayList();
      Iterator i$ = sourceLineList.iterator();

      while(i$.hasNext()) {
         SourceLine sl = (SourceLine)i$.next();
         this.sourceList.add(sl.getSource());
      }

   }

   public ArrayList getSourceLineList() {
      return this.sourceLineList;
   }

   public String getFilename() {
      return this.filename;
   }

   public ArrayList getTokenList() {
      return this.tokenList;
   }

   public Tokenizer getTokenizer() {
      return this.tokenizer;
   }

   public ArrayList createParsedList() {
      this.parsedList = new ArrayList();
      return this.parsedList;
   }

   public ArrayList getParsedList() {
      return this.parsedList;
   }

   public ArrayList getMachineList() {
      return this.machineList;
   }

   public BackStepper getBackStepper() {
      return this.backStepper;
   }

   public SymbolTable getLocalSymbolTable() {
      return this.localSymbolTable;
   }

   public boolean backSteppingEnabled() {
      return this.backStepper != null && this.backStepper.enabled();
   }

   public String getSourceLine(int i) {
      return i >= 1 && i <= this.sourceList.size() ? (String)this.sourceList.get(i - 1) : null;
   }

   public void readSource(String file) throws ProcessingException {
      this.filename = file;
      this.sourceList = new ArrayList();
      ErrorList errors = null;

      try {
         BufferedReader inputFile = new BufferedReader(new FileReader(file));

         for(String line = inputFile.readLine(); line != null; line = inputFile.readLine()) {
            this.sourceList.add(line);
         }

      } catch (Exception var7) {
         errors = new ErrorList();
         errors.add(new ErrorMessage((MIPSprogram)null, 0, 0, var7.toString()));
         throw new ProcessingException(errors);
      }
   }

   public void tokenize() throws ProcessingException {
      this.tokenizer = new Tokenizer();
      this.tokenList = this.tokenizer.tokenize(this);
      this.localSymbolTable = new SymbolTable(this.filename);
   }

   public ArrayList prepareFilesForAssembly(ArrayList filenames, String leadFilename, String exceptionHandler) throws ProcessingException {
      ArrayList MIPSprogramsToAssemble = new ArrayList();
      int leadFilePosition = 0;
      if (exceptionHandler != null && exceptionHandler.length() > 0) {
         filenames.add(0, exceptionHandler);
         leadFilePosition = 1;
      }

      for(int i = 0; i < filenames.size(); ++i) {
         String filename = (String)filenames.get(i);
         MIPSprogram preparee = filename.equals(leadFilename) ? this : new MIPSprogram();
         preparee.readSource(filename);
         preparee.tokenize();
         if (preparee == this && MIPSprogramsToAssemble.size() > 0) {
            MIPSprogramsToAssemble.add(leadFilePosition, preparee);
         } else {
            MIPSprogramsToAssemble.add(preparee);
         }
      }

      return MIPSprogramsToAssemble;
   }

   public ErrorList assemble(ArrayList MIPSprogramsToAssemble, boolean extendedAssemblerEnabled) throws ProcessingException {
      return this.assemble(MIPSprogramsToAssemble, extendedAssemblerEnabled, false);
   }

   public ErrorList assemble(ArrayList MIPSprogramsToAssemble, boolean extendedAssemblerEnabled, boolean warningsAreErrors) throws ProcessingException {
      this.backStepper = null;
      Assembler asm = new Assembler();
      this.machineList = asm.assemble(MIPSprogramsToAssemble, extendedAssemblerEnabled, warningsAreErrors);
      this.backStepper = new BackStepper();
      return asm.getErrorList();
   }

   public boolean simulate(int[] breakPoints) throws ProcessingException {
      return this.simulateFromPC(breakPoints, -1, (AbstractAction)null);
   }

   public boolean simulate(int maxSteps) throws ProcessingException {
      return this.simulateFromPC((int[])null, maxSteps, (AbstractAction)null);
   }

   public boolean simulateFromPC(int[] breakPoints, int maxSteps, AbstractAction a) throws ProcessingException {
      this.steppedExecution = false;
      Simulator sim = Simulator.getInstance();
      return sim.simulate(this, RegisterFile.getProgramCounter(), maxSteps, breakPoints, a);
   }

   public boolean simulateStepAtPC(AbstractAction a) throws ProcessingException {
      this.steppedExecution = true;
      Simulator sim = Simulator.getInstance();
      boolean done = sim.simulate(this, RegisterFile.getProgramCounter(), 1, (int[])null, a);
      return done;
   }

   public boolean inSteppedExecution() {
      return this.steppedExecution;
   }

   public MacroPool createMacroPool() {
      this.macroPool = new MacroPool(this);
      return this.macroPool;
   }

   public MacroPool getLocalMacroPool() {
      return this.macroPool;
   }

   public void setLocalMacroPool(MacroPool macroPool) {
      this.macroPool = macroPool;
   }
}
