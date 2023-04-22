package mars.venus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

public class RepeatButton extends JButton implements ActionListener, MouseListener {
   private boolean pressed = false;
   private boolean repeatEnabled = true;
   private Timer timer = null;
   private int initialDelay = 300;
   private int delay = 60;
   private int modifiers = 0;
   private static boolean testing = false;

   public RepeatButton() {
      this.init();
   }

   public RepeatButton(Action a) {
      super(a);
      this.init();
   }

   public RepeatButton(Icon icon) {
      super(icon);
      this.init();
   }

   public RepeatButton(String text) {
      super(text);
      this.init();
   }

   public RepeatButton(String text, Icon icon) {
      super(text, icon);
      this.init();
   }

   private void init() {
      this.addMouseListener(this);
      this.timer = new Timer(this.delay, this);
      this.timer.setRepeats(true);
   }

   public int getDelay() {
      return this.delay;
   }

   public void setDelay(int d) {
      this.delay = d;
   }

   public int getInitialDelay() {
      return this.initialDelay;
   }

   public void setInitialDelay(int d) {
      this.initialDelay = d;
   }

   public boolean isRepeatEnabled() {
      return this.repeatEnabled;
   }

   public void setRepeatEnabled(boolean en) {
      if (!en) {
         this.pressed = false;
         if (this.timer.isRunning()) {
            this.timer.stop();
         }
      }

      this.repeatEnabled = en;
   }

   public void setEnabled(boolean en) {
      if (en != super.isEnabled()) {
         this.pressed = false;
         if (this.timer.isRunning()) {
            this.timer.stop();
         }
      }

      super.setEnabled(en);
   }

   public void actionPerformed(ActionEvent ae) {
      if (ae.getSource() == this.timer) {
         ActionEvent event = new ActionEvent(this, 1001, super.getActionCommand(), this.modifiers);
         super.fireActionPerformed(event);
      } else if (testing && ae.getSource() == this) {
         System.out.println(ae.getActionCommand());
      }

   }

   public void mouseClicked(MouseEvent me) {
      if (me.getSource() == this) {
         this.pressed = false;
         if (this.timer.isRunning()) {
            this.timer.stop();
         }
      }

   }

   public void mousePressed(MouseEvent me) {
      if (me.getSource() == this && this.isEnabled() && this.isRepeatEnabled()) {
         this.pressed = true;
         if (!this.timer.isRunning()) {
            this.modifiers = me.getModifiers();
            this.timer.setInitialDelay(this.initialDelay);
            this.timer.start();
         }
      }

   }

   public void mouseReleased(MouseEvent me) {
      if (me.getSource() == this) {
         this.pressed = false;
         if (this.timer.isRunning()) {
            this.timer.stop();
         }
      }

   }

   public void mouseEntered(MouseEvent me) {
      if (me.getSource() == this && this.isEnabled() && this.isRepeatEnabled() && this.pressed && !this.timer.isRunning()) {
         this.modifiers = me.getModifiers();
         this.timer.setInitialDelay(this.delay);
         this.timer.start();
      }

   }

   public void mouseExited(MouseEvent me) {
      if (me.getSource() == this && this.timer.isRunning()) {
         this.timer.stop();
      }

   }

   public static void main(String[] args) {
      testing = true;
      JFrame f = new JFrame("RepeatButton Test");
      f.setDefaultCloseOperation(3);
      JPanel p = new JPanel();
      RepeatButton b = new RepeatButton("hold me");
      b.setActionCommand("test");
      b.addActionListener(b);
      p.add(b);
      f.getContentPane().add(p);
      f.pack();
      f.setVisible(true);
   }
}
