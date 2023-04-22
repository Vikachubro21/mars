package mars.simulator;

public class SimulatorNotice {
   private int action;
   private int maxSteps;
   private double runSpeed;
   private int programCounter;
   public static final int SIMULATOR_START = 0;
   public static final int SIMULATOR_STOP = 1;

   public SimulatorNotice(int action, int maxSteps, double runSpeed, int programCounter) {
      this.action = action;
      this.maxSteps = maxSteps;
      this.runSpeed = runSpeed;
      this.programCounter = programCounter;
   }

   public int getAction() {
      return this.action;
   }

   public int getMaxSteps() {
      return this.maxSteps;
   }

   public double getRunSpeed() {
      return this.runSpeed;
   }

   public int getProgramCounter() {
      return this.programCounter;
   }

   public String toString() {
      return (this.getAction() == 0 ? "START " : "STOP  ") + "Max Steps " + this.maxSteps + " " + "Speed " + (this.runSpeed == 40.0 ? "unlimited " : "" + this.runSpeed + " inst/sec") + "Prog Ctr " + this.programCounter;
   }
}
