package mars.mips.instructions.syscalls;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

class ToneGenerator {
   public static final byte DEFAULT_PITCH = 60;
   public static final int DEFAULT_DURATION = 1000;
   public static final byte DEFAULT_INSTRUMENT = 0;
   public static final byte DEFAULT_VOLUME = 100;
   private static Executor threadPool = Executors.newCachedThreadPool();

   public void generateTone(byte pitch, int duration, byte instrument, byte volume) {
      Runnable tone = new Tone(pitch, duration, instrument, volume);
      threadPool.execute(tone);
   }

   public void generateToneSynchronously(byte pitch, int duration, byte instrument, byte volume) {
      Runnable tone = new Tone(pitch, duration, instrument, volume);
      tone.run();
   }
}
