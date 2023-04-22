package mars.simulator;

import mars.Globals;
import mars.ProgramStatement;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.Memory;
import mars.mips.hardware.RegisterFile;
import mars.venus.Coprocessor0Window;

public class BackStepper {
   private static final int MEMORY_RESTORE_RAW_WORD = 0;
   private static final int MEMORY_RESTORE_WORD = 1;
   private static final int MEMORY_RESTORE_HALF = 2;
   private static final int MEMORY_RESTORE_BYTE = 3;
   private static final int REGISTER_RESTORE = 4;
   private static final int PC_RESTORE = 5;
   private static final int COPROC0_REGISTER_RESTORE = 6;
   private static final int COPROC1_REGISTER_RESTORE = 7;
   private static final int COPROC1_CONDITION_CLEAR = 8;
   private static final int COPROC1_CONDITION_SET = 9;
   private static final int DO_NOTHING = 10;
   private static final int NOT_PC_VALUE = -1;
   private boolean engaged = true;
   private BackstepStack backSteps;

   public BackStepper() {
      this.backSteps = new BackstepStack(Globals.maximumBacksteps);
   }

   public boolean enabled() {
      return this.engaged;
   }

   public void setEnabled(boolean state) {
      this.engaged = state;
   }

   public boolean empty() {
      return this.backSteps.empty();
   }

   public boolean inDelaySlot() {
      return !this.empty() && this.backSteps.peek().inDelaySlot;
   }

   public void backStep() {
      if (engaged && !backSteps.empty()) {
         ProgramStatement statement = ((BackStep)backSteps.peek()).ps;
         engaged = false; // GOTTA DO THIS SO METHOD CALL IN SWITCH WILL NOT RESULT IN NEW ACTION ON STACK!
         do {
            BackStep step = (BackStep) backSteps.pop();
            /*
            	System.out.println("backstep POP: action "+step.action+" pc "+mars.util.Binary.intToHexString(step.pc)+
            	                   " source "+((step.ps==null)? "none":step.ps.getSource())+
            							 " parm1 "+step.param1+" parm2 "+step.param2);
            */
            if (step.pc != NOT_PC_VALUE) {
               RegisterFile.setProgramCounter(step.pc);
            }
            try {
               switch (step.action) {
                  case MEMORY_RESTORE_RAW_WORD :
                     Globals.memory.setRawWord(step.param1, step.param2);
                     break;
                  case MEMORY_RESTORE_WORD :
                     Globals.memory.setWord(step.param1, step.param2);
                     break;
                  case MEMORY_RESTORE_HALF :
                     Globals.memory.setHalf(step.param1, step.param2);
                     break;
                  case MEMORY_RESTORE_BYTE :
                     Globals.memory.setByte(step.param1, step.param2);
                     break;
                  case REGISTER_RESTORE :
                     RegisterFile.updateRegister(step.param1, step.param2);
                     break;
                  case PC_RESTORE :
                     RegisterFile.setProgramCounter(step.param1);
                     break;
                  case COPROC0_REGISTER_RESTORE :
                     Coprocessor0.updateRegister(step.param1, step.param2);
                     break;
                  case COPROC1_REGISTER_RESTORE :
                     Coprocessor1.updateRegister(step.param1, step.param2);
                     break;
                  case COPROC1_CONDITION_CLEAR :
                     Coprocessor1.clearConditionFlag(step.param1);
                     break;
                  case COPROC1_CONDITION_SET :
                     Coprocessor1.setConditionFlag(step.param1);
                     break;
                  case DO_NOTHING :
                     break;
               }
            }
            catch (Exception e) {
               // if the original action did not cause an exception this will not either.
               System.out.println("Internal MARS error: address exception while back-stepping.");
               System.exit(0);
            }
         } while (!backSteps.empty() && statement == ((BackStep)backSteps.peek()).ps);
         engaged = true;  // RESET IT (was disabled at top of loop -- see comment)
      }
   }

   private int pc() {
      return RegisterFile.getProgramCounter() - 4;
   }

   public int addMemoryRestoreRawWord(int address, int value) {
      this.backSteps.push(0, this.pc(), address, value);
      return value;
   }

   public int addMemoryRestoreWord(int address, int value) {
      this.backSteps.push(1, this.pc(), address, value);
      return value;
   }

   public int addMemoryRestoreHalf(int address, int value) {
      this.backSteps.push(2, this.pc(), address, value);
      return value;
   }

   public int addMemoryRestoreByte(int address, int value) {
      this.backSteps.push(3, this.pc(), address, value);
      return value;
   }

   public int addRegisterFileRestore(int register, int value) {
      this.backSteps.push(4, this.pc(), register, value);
      return value;
   }

   public int addPCRestore(int value) {
      value -= 4;
      this.backSteps.push(5, value, value);
      return value;
   }

   public int addCoprocessor0Restore(int register, int value) {
      this.backSteps.push(6, this.pc(), register, value);
      return value;
   }

   public int addCoprocessor1Restore(int register, int value) {
      this.backSteps.push(7, this.pc(), register, value);
      return value;
   }

   public int addConditionFlagSet(int flag) {
      this.backSteps.push(9, this.pc(), flag);
      return flag;
   }

   public int addConditionFlagClear(int flag) {
      this.backSteps.push(8, this.pc(), flag);
      return flag;
   }

   public int addDoNothing(int pc) {
      if (this.backSteps.empty() || this.backSteps.peek().pc != pc) {
         this.backSteps.push(10, pc);
      }

      return 0;
   }

   private class BackstepStack {
      private int capacity;
      private int size;
      private int top;
      private BackStep[] stack;

      private BackstepStack(int capacity) {
         this.capacity = capacity;
         this.size = 0;
         this.top = -1;
         this.stack = new BackStep[capacity];

         for(int i = 0; i < capacity; ++i) {
            this.stack[i] = BackStepper.this.new BackStep();
         }

      }

      private synchronized boolean empty() {
         return this.size == 0;
      }

      private synchronized void push(int act, int programCounter, int parm1, int parm2) {
         if (this.size == 0) {
            this.top = 0;
            ++this.size;
         } else if (this.size < this.capacity) {
            this.top = (this.top + 1) % this.capacity;
            ++this.size;
         } else {
            this.top = (this.top + 1) % this.capacity;
         }

         this.stack[this.top].assign(act, programCounter, parm1, parm2);
      }

      private synchronized void push(int act, int programCounter, int parm1) {
         this.push(act, programCounter, parm1, 0);
      }

      private synchronized void push(int act, int programCounter) {
         this.push(act, programCounter, 0, 0);
      }

      private synchronized BackStep pop() {
         BackStep bs = this.stack[this.top];
         if (this.size == 1) {
            this.top = -1;
         } else {
            this.top = (this.top + this.capacity - 1) % this.capacity;
         }

         --this.size;
         return bs;
      }

      private synchronized BackStep peek() {
         return this.stack[this.top];
      }

   }

   private class BackStep {
      private int action;
      private int pc;
      private ProgramStatement ps;
      private int param1;
      private int param2;
      private boolean inDelaySlot;

      private BackStep() {
      }

      private void assign(int act, int programCounter, int parm1, int parm2) {
         this.action = act;
         this.pc = programCounter;

         try {
            this.ps = Globals.memory.getStatementNoNotify(programCounter);
         } catch (Exception var6) {
            this.ps = null;
            this.pc = -1;
         }

         this.param1 = parm1;
         this.param2 = parm2;
         this.inDelaySlot = Simulator.inDelaySlot();
      }

   }
}
