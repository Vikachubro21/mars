package mars.simulator;

import java.util.*;
import javax.swing.*;

import mars.ErrorList;
import mars.ErrorMessage;
import mars.Globals;
import mars.MIPSprogram;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Memory;
import mars.mips.hardware.RegisterFile;
import mars.mips.instructions.BasicInstruction;
import mars.util.Binary;
import mars.util.SystemIO;
import mars.venus.RunBackstepAction;
import mars.venus.RunGoAction;
import mars.venus.RunSpeedPanel;
import mars.venus.RunStepAction;

public class Simulator extends Observable {
   private SimThread simulatorThread = null;
   private static Simulator simulator = null;
   private static Runnable interactiveGUIUpdater = null;
   public static final int NO_DEVICE = 0;
   public static volatile int externalInterruptingDevice = 0;
   public static final int BREAKPOINT = 1;
   public static final int EXCEPTION = 2;
   public static final int MAX_STEPS = 3;
   public static final int NORMAL_TERMINATION = 4;
   public static final int CLIFF_TERMINATION = 5;
   public static final int PAUSE_OR_STOP = 6;
   private ArrayList stopListeners = new ArrayList(1);

   public static Simulator getInstance() {
      if (simulator == null) {
         simulator = new Simulator();
      }

      return simulator;
   }

   private Simulator() {
      //System.out.println(KeyStroke.getKeyStroke("control PAGE_UP"));
      if (Globals.getGui() != null) {
         interactiveGUIUpdater = new UpdateGUI();
      }

   }

   public static boolean inDelaySlot() {
      return DelayedBranch.isTriggered();
   }

   public boolean simulate(MIPSprogram p, int pc, int maxSteps, int[] breakPoints, AbstractAction actor) throws ProcessingException {
      this.simulatorThread = new SimThread(p, pc, maxSteps, breakPoints, actor);
      this.simulatorThread.start();
      if (actor == null) {
         Object dun = this.simulatorThread.get();
         ProcessingException pe = this.simulatorThread.pe;
         boolean done = this.simulatorThread.done;
         if (done) {
            SystemIO.resetFiles();
         }

         this.simulatorThread = null;
         if (pe != null) {
            throw pe;
         } else {
            return done;
         }
      } else {
         return true;
      }
   }

   public void stopExecution(AbstractAction actor) {
      if (this.simulatorThread != null) {
         this.simulatorThread.setStop(actor);
         Iterator i$ = this.stopListeners.iterator();

         while(i$.hasNext()) {
            StopListener l = (StopListener)i$.next();
            l.stopped(this);
         }

         this.simulatorThread = null;
      }

   }

   public void addStopListener(StopListener l) {
      this.stopListeners.add(l);
   }

   public void removeStopListener(StopListener l) {
      this.stopListeners.remove(l);
   }

   private void notifyObserversOfExecutionStart(int maxSteps, int programCounter) {
      this.setChanged();
      this.notifyObservers(new SimulatorNotice(0, maxSteps, RunSpeedPanel.getInstance().getRunSpeed(), programCounter));
   }

   private void notifyObserversOfExecutionStop(int maxSteps, int programCounter) {
      this.setChanged();
      this.notifyObservers(new SimulatorNotice(1, maxSteps, RunSpeedPanel.getInstance().getRunSpeed(), programCounter));
   }

   private class UpdateGUI implements Runnable {
      private UpdateGUI() {
      }

      public void run() {
         if (Globals.getGui().getRegistersPane().getSelectedComponent() == Globals.getGui().getMainPane().getExecutePane().getRegistersWindow()) {
            Globals.getGui().getMainPane().getExecutePane().getRegistersWindow().updateRegisters();
         } else {
            Globals.getGui().getMainPane().getExecutePane().getCoprocessor1Window().updateRegisters();
         }

         Globals.getGui().getMainPane().getExecutePane().getDataSegmentWindow().updateValues();
         Globals.getGui().getMainPane().getExecutePane().getTextSegmentWindow().setCodeHighlighting(true);
         Globals.getGui().getMainPane().getExecutePane().getTextSegmentWindow().highlightStepAtPC();
      }

   }

   class SimThread extends SwingWorker {
      private MIPSprogram p;
      private int pc;
      private int maxSteps;
      private int[] breakPoints;
      private boolean done;
      private ProcessingException pe;
      private volatile boolean stop = false;
      private volatile AbstractAction stopper;
      private AbstractAction starter;
      private int constructReturnReason;

      private TimerThread t = null;


      SimThread(MIPSprogram p, int pc, int maxSteps, int[] breakPoints, AbstractAction starter) {
         super(Globals.getGui() != null);
         this.p = p;
         this.pc = pc;
         this.maxSteps = maxSteps;
         this.breakPoints = breakPoints;
         this.done = false;
         this.pe = null;
         this.starter = starter;
         this.stopper = null;
      }

      public void setStop(AbstractAction actor) {
         this.stop = true;
         this.stopper = actor;
      }

      public Object construct() {
         Thread.currentThread().setPriority(4);
         Thread.yield();
         if (this.breakPoints != null && this.breakPoints.length != 0) {
            Arrays.sort(this.breakPoints);
         } else {
            this.breakPoints = null;
         }

         Simulator.getInstance().notifyObserversOfExecutionStart(this.maxSteps, this.pc);
         RegisterFile.initializeProgramCounter(this.pc);
         ProgramStatement statement = null;

         try {
            statement = Globals.memory.getStatement(RegisterFile.getProgramCounter());
         } catch (AddressErrorException var12) {
            ErrorList el = new ErrorList();
            el.add(new ErrorMessage((MIPSprogram) null, 0, 0, "invalid program counter value: " + Binary.intToHexString(RegisterFile.getProgramCounter())));
            this.pe = new ProcessingException(el, var12);
            Coprocessor0.updateRegister(14, RegisterFile.getProgramCounter());
            this.constructReturnReason = 2;
            this.done = true;
            SystemIO.resetFiles();
            Simulator.getInstance().notifyObserversOfExecutionStop(this.maxSteps, this.pc);
            return new Boolean(this.done);
         }

         long steps = 0;
         int pc = 0;
         int ipc = 1000;
         long startTime = System.nanoTime();
         while(statement != null) {
            steps++;
            if(steps%10000 == 0) {
               ipc = (int)(10e9/(System.nanoTime() - startTime));
               startTime = System.nanoTime();
            }
            if(steps%ipc == 0)
               Coprocessor0.updateRegister(9, Coprocessor0.getValue(9)+1);
            pc = RegisterFile.getProgramCounter();
            RegisterFile.incrementPC();
            synchronized(Globals.memoryAndRegistersLock) {
               try {
                  if(Coprocessor0.getValue(9) == Coprocessor0.getValue(11) && steps%ipc == 0)
                  {
                     Simulator.externalInterruptingDevice = 8192;
                  }
                  if (Simulator.externalInterruptingDevice != 0) {
                     int deviceInterruptCode = Simulator.externalInterruptingDevice;
                     Simulator.externalInterruptingDevice = 0;
                     throw new ProcessingException(statement, "External Interrupt", deviceInterruptCode);
                  }

                  BasicInstruction instruction = (BasicInstruction)statement.getInstruction();
                  if (instruction == null) {
                     throw new ProcessingException(statement, "undefined instruction (" + Binary.intToHexString(statement.getBinaryStatement()) + ")", 10);
                  }

                  instruction.getSimulationCode().simulate(statement);
                  if (Globals.getSettings().getBackSteppingEnabled()) {
                     Globals.program.getBackStepper().addDoNothing(pc);
                  }
               } catch (ProcessingException var13) {
                  if (var13.errors() == null) {
                     this.constructReturnReason = 4;
                     this.done = true;
                     SystemIO.resetFiles();
                     Simulator.getInstance().notifyObserversOfExecutionStop(this.maxSteps, pc);
                     return new Boolean(this.done);
                  }

                  ProgramStatement exceptionHandler = null;

                  try {
                     exceptionHandler = Globals.memory.getStatement(Memory.exceptionHandlerAddress);
                  } catch (AddressErrorException var11) {
                  }

                  if (exceptionHandler == null) {
                     this.constructReturnReason = 2;
                     this.pe = var13;
                     this.done = true;
                     SystemIO.resetFiles();
                     Simulator.getInstance().notifyObserversOfExecutionStop(this.maxSteps, pc);
                     return new Boolean(this.done);
                  }

                  RegisterFile.setProgramCounter(Memory.exceptionHandlerAddress);
               }
            }

            if (DelayedBranch.isTriggered()) {
               RegisterFile.setProgramCounter(DelayedBranch.getBranchTargetAddress());
               DelayedBranch.clear();
            } else if (DelayedBranch.isRegistered()) {
               DelayedBranch.trigger();
            }

            if (this.stop) {
               this.constructReturnReason = 6;
               this.done = false;
               Simulator.getInstance().notifyObserversOfExecutionStop(this.maxSteps, pc);
               return new Boolean(this.done);
            }

            if (this.breakPoints != null && Arrays.binarySearch(this.breakPoints, RegisterFile.getProgramCounter()) >= 0) {
               this.constructReturnReason = 1;
               this.done = false;
               Simulator.getInstance().notifyObserversOfExecutionStop(this.maxSteps, pc);
               return new Boolean(this.done);
            }

            if (this.maxSteps > 0) {
               ++steps;
               if (steps >= this.maxSteps) {
                  this.constructReturnReason = 3;
                  this.done = false;
                  Simulator.getInstance().notifyObserversOfExecutionStop(this.maxSteps, pc);
                  return new Boolean(this.done);
               }
            }

            if (Simulator.interactiveGUIUpdater != null && this.maxSteps != 1 && RunSpeedPanel.getInstance().getRunSpeed() < 40.0) {
               SwingUtilities.invokeLater(Simulator.interactiveGUIUpdater);
            }

            if ((Globals.getGui() != null || Globals.runSpeedPanelExists) && this.maxSteps != 1 && RunSpeedPanel.getInstance().getRunSpeed() < 40.0) {
               try {
                  Thread.sleep((long)((int)(1000.0 / RunSpeedPanel.getInstance().getRunSpeed())));
               } catch (InterruptedException var10) {
               }
            }

            try {
               statement = Globals.memory.getStatement(RegisterFile.getProgramCounter());
            } catch (AddressErrorException var9) {
               ErrorList elx = new ErrorList();
               elx.add(new ErrorMessage((MIPSprogram)null, 0, 0, "invalid program counter value: " + Binary.intToHexString(RegisterFile.getProgramCounter())));
               this.pe = new ProcessingException(elx, var9);
               Coprocessor0.updateRegister(14, RegisterFile.getProgramCounter());
               this.constructReturnReason = 2;
               this.done = true;
               SystemIO.resetFiles();
               Simulator.getInstance().notifyObserversOfExecutionStop(this.maxSteps, pc);
               return new Boolean(this.done);
            }
         }

         if (DelayedBranch.isTriggered() || DelayedBranch.isRegistered()) {
            DelayedBranch.clear();
         }

         this.constructReturnReason = 5;
         this.done = true;
         SystemIO.resetFiles();
         Simulator.getInstance().notifyObserversOfExecutionStop(this.maxSteps, pc);
         return new Boolean(this.done);
      }

      public void finished() {
         if (Globals.getGui() != null) {
            String starterName = (String)this.starter.getValue("Name");
            if (starterName.equals("Step")) {
               ((RunStepAction)this.starter).stepped(this.done, this.constructReturnReason, this.pe);
            }
            if(starterName.equals("BackStep")) {
               (this.starter).actionPerformed(null);
            }
            if (starterName.equals("Go")) {
               if (this.done) {
                  ((RunGoAction)this.starter).stopped(this.pe, this.constructReturnReason);
               } else if (this.constructReturnReason == 1) {
                  ((RunGoAction)this.starter).paused(this.done, this.constructReturnReason, this.pe);
               } else {
                  String stopperName = (String)this.stopper.getValue("Name");
                  if ("Pause".equals(stopperName)) {
                     ((RunGoAction)this.starter).paused(this.done, this.constructReturnReason, this.pe);
                  } else if ("Stop".equals(stopperName)) {
                     ((RunGoAction)this.starter).stopped(this.pe, this.constructReturnReason);
                  }
               }
            }

         }
      }
   }

   class TimerThread extends Thread {
      private boolean done;
      private int currentTime;
      private long prevTime;
      private long startTime;
      TimerThread() {
         this.done = false;
         currentTime = Coprocessor0.getValue(9);
         prevTime = System.currentTimeMillis();
      }

      public void run() {
         this.done = false;
         currentTime = 0;
         prevTime = startTime = System.currentTimeMillis();
         Thread.currentThread().setPriority(3);
         Thread.yield();

         while (!simulatorThread.stop && !simulatorThread.done && !this.done) {
            synchronized (Globals.memoryAndRegistersLock) {
               currentTime = (int) (System.currentTimeMillis() - prevTime);
               prevTime = System.currentTimeMillis();
               Coprocessor0.updateRegister(9, Coprocessor0.getValue(9) + currentTime);
            }
         }
      }
   }

   public interface StopListener {
      void stopped(Simulator var1);
   }
}
