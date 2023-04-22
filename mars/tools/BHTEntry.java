package mars.tools;

public class BHTEntry {
   private boolean[] m_history;
   private boolean m_prediction;
   private int m_incorrect;
   private int m_correct;

   public BHTEntry(int historySize, boolean initVal) {
      this.m_prediction = initVal;
      this.m_history = new boolean[historySize];

      for(int i = 0; i < historySize; ++i) {
         this.m_history[i] = initVal;
      }

      this.m_correct = this.m_incorrect = 0;
   }

   public boolean getPrediction() {
      return this.m_prediction;
   }

   public void updatePrediction(boolean branchTaken) {
      for(int i = 0; i < this.m_history.length - 1; ++i) {
         this.m_history[i] = this.m_history[i + 1];
      }

      this.m_history[this.m_history.length - 1] = branchTaken;
      if (branchTaken == this.m_prediction) {
         ++this.m_correct;
      } else {
         ++this.m_incorrect;
         boolean changePrediction = true;

         for(int i = 0; i < this.m_history.length; ++i) {
            if (this.m_history[i] != branchTaken) {
               changePrediction = false;
            }
         }

         if (changePrediction) {
            this.m_prediction = !this.m_prediction;
         }
      }

   }

   public int getStatsPredIncorrect() {
      return this.m_incorrect;
   }

   public int getStatsPredCorrect() {
      return this.m_correct;
   }

   public double getStatsPredPrecision() {
      int sum = this.m_incorrect + this.m_correct;
      return sum == 0 ? 0.0 : (double)this.m_correct * 100.0 / (double)sum;
   }

   public String getHistoryAsStr() {
      String result = "";

      for(int i = 0; i < this.m_history.length; ++i) {
         if (i > 0) {
            result = result + ", ";
         }

         result = result + (this.m_history[i] ? "T" : "NT");
      }

      return result;
   }

   public String getPredictionAsStr() {
      return this.m_prediction ? "TAKE" : "NOT TAKE";
   }
}
