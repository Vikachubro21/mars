package mars.mips.instructions.syscalls;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

class Tone implements Runnable {
   public static final int TEMPO = 1000;
   public static final int DEFAULT_CHANNEL = 0;
   private byte pitch;
   private int duration;
   private byte instrument;
   private byte volume;
   private static Lock openLock = new ReentrantLock();

   public Tone(byte pitch, int duration, byte instrument, byte volume) {
      this.pitch = pitch;
      this.duration = duration;
      this.instrument = instrument;
      this.volume = volume;
   }

   public void run() {
      this.playTone();
   }

   private void playTone() {
      try {
         Sequencer player = null;
         openLock.lock();

         try {
            player = MidiSystem.getSequencer();
            player.open();
         } finally {
            openLock.unlock();
         }

         Sequence seq = new Sequence(0.0F, 1);
         player.setTempoInMPQ(1000.0F);
         Track t = seq.createTrack();
         ShortMessage inst = new ShortMessage();
         inst.setMessage(192, 0, this.instrument, 0);
         MidiEvent instChange = new MidiEvent(inst, 0L);
         t.add(instChange);
         ShortMessage on = new ShortMessage();
         on.setMessage(144, 0, this.pitch, this.volume);
         MidiEvent noteOn = new MidiEvent(on, 0L);
         t.add(noteOn);
         ShortMessage off = new ShortMessage();
         off.setMessage(128, 0, this.pitch, this.volume);
         MidiEvent noteOff = new MidiEvent(off, (long)this.duration);
         t.add(noteOff);
         player.setSequence(seq);
         EndOfTrackListener eot = new EndOfTrackListener();
         player.addMetaEventListener(eot);
         player.start();

         try {
            eot.awaitEndOfTrack();
         } catch (InterruptedException var23) {
         } finally {
            player.close();
         }
      } catch (MidiUnavailableException var26) {
         var26.printStackTrace();
      } catch (InvalidMidiDataException var27) {
         var27.printStackTrace();
      }

   }
}
