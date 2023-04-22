package mars.tools;

class CaptureModel {
   private boolean enabled;

   public CaptureModel(boolean set) {
      this.enabled = set;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean set) {
      this.enabled = set;
   }
}
