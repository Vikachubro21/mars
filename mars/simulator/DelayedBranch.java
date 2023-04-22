package mars.simulator;

public class DelayedBranch {
   private static final int CLEARED = 0;
   private static final int REGISTERED = 1;
   private static final int TRIGGERED = 2;
   private static int state = 0;
   private static int branchTargetAddress = 0;

   public static void register(int targetAddress) {
      switch (state) {
         case 0:
            branchTargetAddress = targetAddress;
         case 1:
         case 2:
            state = 1;
         default:
      }
   }

   static void trigger() {
      switch (state) {
         case 1:
         case 2:
            state = 2;
         case 0:
         default:
      }
   }

   static void clear() {
      state = 0;
      branchTargetAddress = 0;
   }

   static boolean isRegistered() {
      return state == 1;
   }

   static boolean isTriggered() {
      return state == 2;
   }

   static int getBranchTargetAddress() {
      return branchTargetAddress;
   }
}
