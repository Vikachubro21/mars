package mars.simulator;

import javax.swing.SwingUtilities;

public abstract class SwingWorker {
   private Object value;
   private ThreadVar threadVar;

   protected synchronized Object getValue() {
      return this.value;
   }

   private synchronized void setValue(Object x) {
      this.value = x;
   }

   public abstract Object construct();

   public void finished() {
   }

   public void interrupt() {
      Thread t = this.threadVar.get();
      if (t != null) {
         t.interrupt();
      }

      this.threadVar.clear();
   }

   public Object get() {
      while(true) {
         Thread t = this.threadVar.get();
         if (t == null) {
            return this.getValue();
         }

         try {
            t.join();
         } catch (InterruptedException var3) {
            Thread.currentThread().interrupt();
            return null;
         }
      }
   }

   public SwingWorker(final boolean useSwing) {
      final Runnable doFinished = new Runnable() {
         public void run() {
            SwingWorker.this.finished();
         }
      };
      Runnable doConstruct = new Runnable() {
         public void run() {
            try {
               SwingWorker.this.setValue(SwingWorker.this.construct());
               System.out.println(SwingWorker.this.getValue());
            } finally {
               SwingWorker.this.threadVar.clear();
            }

            if (useSwing) {
               SwingUtilities.invokeLater(doFinished);
            } else {
               doFinished.run();
            }

         }
      };
      Thread t = new Thread(doConstruct, "MIPS");
      this.threadVar = new ThreadVar(t);
   }

   public void start() {
      Thread t = this.threadVar.get();
      if (t != null) {
         t.start();
      }

   }

   private static class ThreadVar {
      private Thread thread;

      ThreadVar(Thread t) {
         this.thread = t;
      }

      synchronized Thread get() {
         return this.thread;
      }

      synchronized void clear() {
         this.thread = null;
      }

   }
}
