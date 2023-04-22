package mars.mips.hardware;

import java.util.Observable;

public class Register extends Observable {
   private String name;
   private int number;
   private int resetValue;
   private volatile int value;

   public Register(String n, int num, int val) {
      this.name = n;
      this.number = num;
      this.value = val;
      this.resetValue = val;
   }

   public String getName() {
      return this.name;
   }

   public synchronized int getValue() {
      this.notifyAnyObservers(0);
      return this.value;
   }

   public synchronized int getValueNoNotify() {
      return this.value;
   }

   public int getResetValue() {
      return this.resetValue;
   }

   public int getNumber() {
      return this.number;
   }

   public synchronized int setValue(int val) {
      int old = this.value;
      this.value = val;
      this.notifyAnyObservers(1);
      return old;
   }

   public synchronized void resetValue() {
      this.value = this.resetValue;
   }

   public synchronized void changeResetValue(int reset) {
      this.resetValue = reset;
   }

   private void notifyAnyObservers(int type) {
      if (this.countObservers() > 0) {
         this.setChanged();
         this.notifyObservers(new RegisterAccessNotice(type, this.name));
      }

   }
}
