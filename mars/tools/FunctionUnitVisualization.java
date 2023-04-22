package mars.tools;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class FunctionUnitVisualization extends JFrame {
   private JPanel contentPane;
   private String instruction;
   private int register = 1;
   private int control = 2;
   private int aluControl = 3;
   private int alu = 4;
   private int currentUnit;

   public FunctionUnitVisualization(String instruction, int functionalUnit) {
      this.instruction = instruction;
      this.setBounds(100, 100, 840, 575);
      this.contentPane = new JPanel();
      this.contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
      this.contentPane.setLayout(new BorderLayout(0, 0));
      this.setContentPane(this.contentPane);
      UnitAnimation reg;
      if (functionalUnit == this.register) {
         this.currentUnit = this.register;
         reg = new UnitAnimation(instruction, this.register);
         this.contentPane.add(reg);
         reg.startAnimation(instruction);
      } else if (functionalUnit == this.control) {
         this.currentUnit = this.control;
         reg = new UnitAnimation(instruction, this.control);
         this.contentPane.add(reg);
         reg.startAnimation(instruction);
      } else if (functionalUnit == this.aluControl) {
         this.currentUnit = this.aluControl;
         reg = new UnitAnimation(instruction, this.aluControl);
         this.contentPane.add(reg);
         reg.startAnimation(instruction);
      }

   }

   public void run() {
      try {
         FunctionUnitVisualization frame = new FunctionUnitVisualization(this.instruction, this.currentUnit);
         frame.setVisible(true);
      } catch (Exception var2) {
         var2.printStackTrace();
      }

   }
}
