package mars.venus;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import mars.Globals;

public class RunSpeedPanel extends JPanel {
   public static final double UNLIMITED_SPEED = 40.0;
   private static final int SPEED_INDEX_MIN = 0;
   private static final int SPEED_INDEX_MAX = 40;
   private static final int SPEED_INDEX_INIT = 40;
   private static final int SPEED_INDEX_INTERACTION_LIMIT = 35;
   private double[] speedTable = new double[]{0.05, 0.1, 0.2, 0.3, 0.4, 0.5, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0, 20.0, 21.0, 22.0, 23.0, 24.0, 25.0, 26.0, 27.0, 28.0, 29.0, 30.0, 40.0, 40.0, 40.0, 40.0, 40.0};
   private JLabel sliderLabel = null;
   private JSlider runSpeedSlider = null;
   private static RunSpeedPanel runSpeedPanel = null;
   private volatile int runSpeedIndex = 40;

   public static RunSpeedPanel getInstance() {
      if (runSpeedPanel == null) {
         runSpeedPanel = new RunSpeedPanel();
         Globals.runSpeedPanelExists = true;
      }

      return runSpeedPanel;
   }

   private RunSpeedPanel() {
      super(new BorderLayout());
      this.runSpeedSlider = new JSlider(0, 0, 40, 40);
      this.runSpeedSlider.setSize(new Dimension(100, (int)this.runSpeedSlider.getSize().getHeight()));
      this.runSpeedSlider.setMaximumSize(this.runSpeedSlider.getSize());
      this.runSpeedSlider.setMajorTickSpacing(5);
      this.runSpeedSlider.setPaintTicks(true);
      this.runSpeedSlider.addChangeListener(new RunSpeedListener((RunSpeedListener)null));
      this.sliderLabel = new JLabel(this.setLabel(this.runSpeedIndex));
      this.sliderLabel.setHorizontalAlignment(0);
      this.sliderLabel.setAlignmentX(0.5F);
      this.add(this.sliderLabel, "North");
      this.add(this.runSpeedSlider, "Center");
      this.setToolTipText("Simulation speed for \"Go\".  At " + (int)this.speedTable[35] + " inst/sec or less, tables updated " + "after each instruction.");
   }

   public double getRunSpeed() {
      return this.speedTable[this.runSpeedIndex];
   }

   private String setLabel(int index) {
      String result = "Run speed ";
      if (index <= 35) {
         if (this.speedTable[index] < 1.0) {
            result = result + this.speedTable[index];
         } else {
            result = result + (int)this.speedTable[index];
         }

         result = result + " inst/sec";
      } else {
         result = result + "at max (no interaction)";
      }

      return result;
   }

   private class RunSpeedListener implements ChangeListener {
      private RunSpeedListener() {
      }

      public void stateChanged(ChangeEvent e) {
         JSlider source = (JSlider)e.getSource();
         if (!source.getValueIsAdjusting()) {
            RunSpeedPanel.this.runSpeedIndex = source.getValue();
         } else {
            RunSpeedPanel.this.sliderLabel.setText(RunSpeedPanel.this.setLabel(source.getValue()));
         }

      }

      // $FF: synthetic method
      RunSpeedListener(RunSpeedListener var2) {
         this();
      }
   }
}
