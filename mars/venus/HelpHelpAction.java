package mars.venus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import mars.Globals;
import mars.assembler.Directives;
import mars.mips.instructions.Instruction;

public class HelpHelpAction extends GuiAction {
   static Color altBackgroundColor = new Color(238, 238, 238);
   public static final String descriptionDetailSeparator = ":";

   public HelpHelpAction(String name, Icon icon, String descrip, Integer mnemonic, KeyStroke accel, VenusUI gui) {
      super(name, icon, descrip, mnemonic, accel, gui);
   }

   private Dimension getSize() {
      return new Dimension(800, 600);
   }

   public void actionPerformed(ActionEvent e) {
      JTabbedPane tabbedPane = new JTabbedPane();
      tabbedPane.addTab("MIPS", this.createMipsHelpInfoPanel());
      tabbedPane.addTab("MARS", this.createMarsHelpInfoPanel());
      tabbedPane.addTab("License", this.createCopyrightInfoPanel());
      tabbedPane.addTab("Bugs/Comments", this.createHTMLHelpPanel("BugReportingHelp.html"));
      tabbedPane.addTab("Acknowledgements", this.createHTMLHelpPanel("Acknowledgements.html"));
      tabbedPane.addTab("Instruction Set Song", this.createHTMLHelpPanel("MIPSInstructionSetSong.html"));
      final JDialog dialog = new JDialog(this.mainUI, "MARS 4.5 Help");
      dialog.addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent e) {
            dialog.setVisible(false);
            dialog.dispose();
         }
      });
      JButton closeButton = new JButton("Close");
      closeButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            dialog.setVisible(false);
            dialog.dispose();
         }
      });
      JPanel closePanel = new JPanel();
      closePanel.setLayout(new BoxLayout(closePanel, 2));
      closePanel.add(Box.createHorizontalGlue());
      closePanel.add(closeButton);
      closePanel.add(Box.createHorizontalGlue());
      closePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 5));
      JPanel contentPane = new JPanel();
      contentPane.setLayout(new BoxLayout(contentPane, 3));
      contentPane.add(tabbedPane);
      contentPane.add(Box.createRigidArea(new Dimension(0, 5)));
      contentPane.add(closePanel);
      contentPane.setOpaque(true);
      dialog.setContentPane(contentPane);
      dialog.setSize(this.getSize());
      dialog.setLocationRelativeTo(this.mainUI);
      dialog.setVisible(true);
   }

   private JPanel createHTMLHelpPanel(String filename) {
      JPanel helpPanel = new JPanel(new BorderLayout());

      JScrollPane helpScrollPane;
      try {
         InputStream is = this.getClass().getResourceAsStream("/help/" + filename);
         BufferedReader in = new BufferedReader(new InputStreamReader(is));
         StringBuffer text = new StringBuffer();

         String line;
         while((line = in.readLine()) != null) {
            text.append(line + "\n");
         }

         in.close();
         JEditorPane helpDisplay = new JEditorPane("text/html", text.toString());
         helpDisplay.setEditable(false);
         helpDisplay.setCaretPosition(0);
         helpScrollPane = new JScrollPane(helpDisplay, 22, 30);
         helpDisplay.addHyperlinkListener(new HelpHyperlinkListener((HelpHyperlinkListener)null));
      } catch (Exception var9) {
         helpScrollPane = new JScrollPane(new JLabel("Error (" + var9 + "): " + filename + " contents could not be loaded."));
      }

      helpPanel.add(helpScrollPane);
      return helpPanel;
   }

   private JPanel createCopyrightInfoPanel() {
      JPanel marsCopyrightInfo = new JPanel(new BorderLayout());

      JScrollPane marsCopyrightScrollPane;
      try {
         InputStream is = this.getClass().getResourceAsStream("/MARSlicense.txt");
         BufferedReader in = new BufferedReader(new InputStreamReader(is));
         StringBuffer text = new StringBuffer("<pre>");

         String line;
         while((line = in.readLine()) != null) {
            text.append(line + "\n");
         }

         in.close();
         text.append("</pre>");
         JEditorPane marsCopyrightDisplay = new JEditorPane("text/html", text.toString());
         marsCopyrightDisplay.setEditable(false);
         marsCopyrightDisplay.setCaretPosition(0);
         marsCopyrightScrollPane = new JScrollPane(marsCopyrightDisplay, 22, 30);
      } catch (Exception var8) {
         marsCopyrightScrollPane = new JScrollPane(new JLabel("Error: license contents could not be loaded."));
      }

      marsCopyrightInfo.add(marsCopyrightScrollPane);
      return marsCopyrightInfo;
   }

   private JPanel createMarsHelpInfoPanel() {
      JPanel marsHelpInfo = new JPanel(new BorderLayout());
      JTabbedPane tabbedPane = new JTabbedPane();
      tabbedPane.addTab("Intro", this.createHTMLHelpPanel("MarsHelpIntro.html"));
      tabbedPane.addTab("IDE", this.createHTMLHelpPanel("MarsHelpIDE.html"));
      tabbedPane.addTab("Debugging", this.createHTMLHelpPanel("MarsHelpDebugging.html"));
      tabbedPane.addTab("Settings", this.createHTMLHelpPanel("MarsHelpSettings.html"));
      tabbedPane.addTab("Tools", this.createHTMLHelpPanel("MarsHelpTools.html"));
      tabbedPane.addTab("Command", this.createHTMLHelpPanel("MarsHelpCommand.html"));
      tabbedPane.addTab("Limits", this.createHTMLHelpPanel("MarsHelpLimits.html"));
      tabbedPane.addTab("History", this.createHTMLHelpPanel("MarsHelpHistory.html"));
      marsHelpInfo.add(tabbedPane);
      return marsHelpInfo;
   }

   private JPanel createMipsHelpInfoPanel() {
      JPanel mipsHelpInfo = new JPanel(new BorderLayout());
      String helpRemarksColor = "CCFF99";
      String helpRemarks = "<html><center><table bgcolor=\"#" + helpRemarksColor + "\" border=0 cellpadding=0>" + "<tr>" + "<th colspan=2><b><i><font size=+1>&nbsp;&nbsp;Operand Key for Example Instructions&nbsp;&nbsp;</font></i></b></th>" + "</tr>" + "<tr>" + "<td><tt>label, target</tt></td><td>any textual label</td>" + "</tr><tr>" + "<td><tt>$t1, $t2, $t3</tt></td><td>any integer register</td>" + "</tr><tr>" + "<td><tt>$f2, $f4, $f6</tt></td><td><i>even-numbered</i> floating point register</td>" + "</tr><tr>" + "<td><tt>$f0, $f1, $f3</tt></td><td><i>any</i> floating point register</td>" + "</tr><tr>" + "<td><tt>$8</tt></td><td>any Coprocessor 0 register</td>" + "</tr><tr>" + "<td><tt>1</tt></td><td>condition flag (0 to 7)</td>" + "</tr><tr>" + "<td><tt>10</tt></td><td>unsigned 5-bit integer (0 to 31)</td>" + "</tr><tr>" + "<td><tt>-100</tt></td><td>signed 16-bit integer (-32768 to 32767)</td>" + "</tr><tr>" + "<td><tt>100</tt></td><td>unsigned 16-bit integer (0 to 65535)</td>" + "</tr><tr>" + "<td><tt>100000</tt></td><td>signed 32-bit integer (-2147483648 to 2147483647)</td>" + "</tr><tr>" + "</tr><tr>" + "<td colspan=2><b><i><font size=+1>Load & Store addressing mode, basic instructions</font></i></b></td>" + "</tr><tr>" + "<td><tt>-100($t2)</tt></td><td>sign-extended 16-bit integer added to contents of $t2</td>" + "</tr><tr>" + "</tr><tr>" + "<td colspan=2><b><i><font size=+1>Load & Store addressing modes, pseudo instructions</font></i></b></td>" + "</tr><tr>" + "<td><tt>($t2)</tt></td><td>contents of $t2</td>" + "</tr><tr>" + "<td><tt>-100</tt></td><td>signed 16-bit integer</td>" + "</tr><tr>" + "<td><tt>100</tt></td><td>unsigned 16-bit integer</td>" + "</tr><tr>" + "<td><tt>100000</tt></td><td>signed 32-bit integer</td>" + "</tr><tr>" + "<td><tt>100($t2)</tt></td><td>zero-extended unsigned 16-bit integer added to contents of $t2</td>" + "</tr><tr>" + "<td><tt>100000($t2)</tt></td><td>signed 32-bit integer added to contents of $t2</td>" + "</tr><tr>" + "<td><tt>label</tt></td><td>32-bit address of label</td>" + "</tr><tr>" + "<td><tt>label($t2)</tt></td><td>32-bit address of label added to contents of $t2</td>" + "</tr><tr>" + "<td><tt>label+100000</tt></td><td>32-bit integer added to label's address</td>" + "</tr><tr>" + "<td><tt>label+100000($t2)&nbsp;&nbsp;&nbsp;</tt></td><td>sum of 32-bit integer, label's address, and contents of $t2</td>" + "</tr>" + "</table></center></html>";
      JLabel helpRemarksLabel = new JLabel(helpRemarks, 0);
      helpRemarksLabel.setOpaque(true);
      helpRemarksLabel.setBackground(Color.decode("0x" + helpRemarksColor));
      JScrollPane operandsScrollPane = new JScrollPane(helpRemarksLabel, 22, 32);
      mipsHelpInfo.add(operandsScrollPane, "North");
      JTabbedPane tabbedPane = new JTabbedPane();
      tabbedPane.addTab("Basic Instructions", this.createMipsInstructionHelpPane("mars.mips.instructions.BasicInstruction"));
      tabbedPane.addTab("Extended (pseudo) Instructions", this.createMipsInstructionHelpPane("mars.mips.instructions.ExtendedInstruction"));
      tabbedPane.addTab("Directives", this.createMipsDirectivesHelpPane());
      tabbedPane.addTab("Syscalls", this.createHTMLHelpPanel("SyscallHelp.html"));
      tabbedPane.addTab("Exceptions", this.createHTMLHelpPanel("ExceptionsHelp.html"));
      tabbedPane.addTab("Macros", this.createHTMLHelpPanel("MacrosHelp.html"));
      operandsScrollPane.setPreferredSize(new Dimension((int)this.getSize().getWidth(), (int)(this.getSize().getHeight() * 0.2)));
      operandsScrollPane.getVerticalScrollBar().setUnitIncrement(10);
      tabbedPane.setPreferredSize(new Dimension((int)this.getSize().getWidth(), (int)(this.getSize().getHeight() * 0.6)));
      JSplitPane splitsville = new JSplitPane(0, operandsScrollPane, tabbedPane);
      splitsville.setOneTouchExpandable(true);
      splitsville.resetToPreferredSizes();
      mipsHelpInfo.add(splitsville);
      return mipsHelpInfo;
   }

   private JScrollPane createMipsDirectivesHelpPane() {
      Vector exampleList = new Vector();
      String blanks = "            ";
      Iterator it = Directives.getDirectiveList().iterator();

      while(it.hasNext()) {
         Directives direct = (Directives)it.next();
         exampleList.add(direct.toString() + blanks.substring(0, Math.max(0, blanks.length() - direct.toString().length())) + direct.getDescription());
      }

      Collections.sort(exampleList);
      JList examples = new JList(exampleList);
      JScrollPane mipsScrollPane = new JScrollPane(examples, 22, 30);
      examples.setFont(new Font("Lucida Sans Typewriter", 0, 12));
      return mipsScrollPane;
   }

   private JScrollPane createMipsInstructionHelpPane(String instructionClassName) {
      ArrayList instructionList = Globals.instructionSet.getInstructionList();
      Vector exampleList = new Vector(instructionList.size());
      Iterator it = instructionList.iterator();
      String blanks = "                        ";

      while(it.hasNext()) {
         Instruction instr = (Instruction)it.next();

         try {
            if (Class.forName(instructionClassName).isInstance(instr)) {
               exampleList.add(instr.getExampleFormat() + blanks.substring(0, Math.max(0, blanks.length() - instr.getExampleFormat().length())) + instr.getDescription());
            }
         } catch (ClassNotFoundException var10) {
            System.out.println(var10 + " " + instructionClassName);
         }
      }

      Collections.sort(exampleList);
      JList examples = new JList(exampleList);
      JScrollPane mipsScrollPane = new JScrollPane(examples, 22, 30);
      examples.setFont(new Font("Lucida Sans Typewriter", 0, 12));
      examples.setCellRenderer(new MyCellRenderer((MyCellRenderer)null));
      return mipsScrollPane;
   }

   private class HelpHyperlinkListener implements HyperlinkListener {
      JDialog webpageDisplay;
      JTextField webpageURL;
      private static final String cannotDisplayMessage = "<html><title></title><body><strong>Unable to display requested document.</strong></body></html>";

      private HelpHyperlinkListener() {
      }

      public void hyperlinkUpdate(HyperlinkEvent e) {
         if (e.getEventType() == EventType.ACTIVATED) {
            JEditorPane pane = (JEditorPane)e.getSource();
            if (e instanceof HTMLFrameHyperlinkEvent) {
               HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent)e;
               HTMLDocument doc = (HTMLDocument)pane.getDocument();
               doc.processHTMLFrameHyperlinkEvent(evt);
            } else {
               this.webpageDisplay = new JDialog(HelpHelpAction.this.mainUI, "Primitive HTML Viewer");
               this.webpageDisplay.setLayout(new BorderLayout());
               this.webpageDisplay.setLocation(HelpHelpAction.this.mainUI.getSize().width / 6, HelpHelpAction.this.mainUI.getSize().height / 6);

               JEditorPane webpagePane;
               try {
                  webpagePane = new JEditorPane(e.getURL());
               } catch (Throwable var8) {
                  webpagePane = new JEditorPane("text/html", "<html><title></title><body><strong>Unable to display requested document.</strong></body></html>");
               }

               webpagePane.addHyperlinkListener(new HyperlinkListener() {
                  public void hyperlinkUpdate(HyperlinkEvent e) {
                     if (e.getEventType() == EventType.ACTIVATED) {
                        JEditorPane pane = (JEditorPane)e.getSource();
                        if (e instanceof HTMLFrameHyperlinkEvent) {
                           HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent)e;
                           HTMLDocument doc = (HTMLDocument)pane.getDocument();
                           doc.processHTMLFrameHyperlinkEvent(evt);
                        } else {
                           try {
                              pane.setPage(e.getURL());
                           } catch (Throwable var5) {
                              pane.setText("<html><title></title><body><strong>Unable to display requested document.</strong></body></html>");
                           }

                           HelpHyperlinkListener.this.webpageURL.setText(e.getURL().toString());
                        }
                     }

                  }
               });
               webpagePane.setPreferredSize(new Dimension(HelpHelpAction.this.mainUI.getSize().width * 2 / 3, HelpHelpAction.this.mainUI.getSize().height * 2 / 3));
               webpagePane.setEditable(false);
               webpagePane.setCaretPosition(0);
               JScrollPane webpageScrollPane = new JScrollPane(webpagePane, 20, 30);
               this.webpageURL = new JTextField(e.getURL().toString(), 50);
               this.webpageURL.setEditable(false);
               this.webpageURL.setBackground(Color.WHITE);
               JPanel URLPanel = new JPanel(new FlowLayout(0, 4, 4));
               URLPanel.add(new JLabel("URL: "));
               URLPanel.add(this.webpageURL);
               this.webpageDisplay.add(URLPanel, "North");
               this.webpageDisplay.add(webpageScrollPane);
               JButton closeButton = new JButton("Close");
               closeButton.addActionListener(new ActionListener() {
                  public void actionPerformed(ActionEvent e) {
                     HelpHyperlinkListener.this.webpageDisplay.setVisible(false);
                     HelpHyperlinkListener.this.webpageDisplay.dispose();
                  }
               });
               JPanel closePanel = new JPanel();
               closePanel.setLayout(new BoxLayout(closePanel, 2));
               closePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 5));
               closePanel.add(Box.createHorizontalGlue());
               closePanel.add(closeButton);
               closePanel.add(Box.createHorizontalGlue());
               this.webpageDisplay.add(closePanel, "South");
               this.webpageDisplay.pack();
               this.webpageDisplay.setVisible(true);
            }
         }

      }

      // $FF: synthetic method
      HelpHyperlinkListener(HelpHyperlinkListener var2) {
         this();
      }
   }

   private class MyCellRenderer extends JLabel implements ListCellRenderer {
      private MyCellRenderer() {
      }

      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
         String s = value.toString();
         this.setText(s);
         if (isSelected) {
            this.setBackground(list.getSelectionBackground());
            this.setForeground(list.getSelectionForeground());
         } else {
            this.setBackground(index % 2 == 0 ? HelpHelpAction.altBackgroundColor : list.getBackground());
            this.setForeground(list.getForeground());
         }

         this.setEnabled(list.isEnabled());
         this.setFont(list.getFont());
         this.setOpaque(true);
         return this;
      }

      // $FF: synthetic method
      MyCellRenderer(MyCellRenderer var2) {
         this();
      }
   }
}
