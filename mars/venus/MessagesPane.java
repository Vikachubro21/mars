package mars.venus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.NavigationFilter;
import javax.swing.text.Position;
import javax.swing.undo.UndoableEdit;
import mars.Globals;
import mars.simulator.Simulator;

public class MessagesPane extends JTabbedPane {
   JTextArea assemble;
   JTextArea run;
   JPanel assembleTab;
   JPanel runTab;
   public static final int MAXIMUM_SCROLLED_CHARACTERS;
   public static final int NUMBER_OF_CHARACTERS_TO_CUT;

   static {
      MAXIMUM_SCROLLED_CHARACTERS = Globals.maximumMessageCharacters;
      NUMBER_OF_CHARACTERS_TO_CUT = Globals.maximumMessageCharacters / 10;
   }

   public MessagesPane() {
      this.setMinimumSize(new Dimension(0, 0));
      this.assemble = new JTextArea();
      this.run = new JTextArea();
      this.assemble.setEditable(false);
      this.run.setEditable(false);
      Font monoFont = new Font("Lucida Sans Typewriter", 0, 12);
      this.assemble.setFont(monoFont);
      this.run.setFont(monoFont);
      JButton assembleTabClearButton = new JButton("Clear");
      assembleTabClearButton.setToolTipText("Clear the Mars Messages area");
      assembleTabClearButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            MessagesPane.this.assemble.setText("");
         }
      });
      this.assembleTab = new JPanel(new BorderLayout());
      this.assembleTab.add(this.createBoxForButton(assembleTabClearButton), "West");
      this.assembleTab.add(new JScrollPane(this.assemble, 20, 30), "Center");
      this.assemble.addMouseListener(new MouseAdapter() {
         public void mouseClicked(MouseEvent e) {
            int lineStart = 0;
            int lineEnd = 0;

            String text;
            int separatorPosition;
            try {
               separatorPosition = MessagesPane.this.assemble.getLineOfOffset(MessagesPane.this.assemble.viewToModel(e.getPoint()));
               lineStart = MessagesPane.this.assemble.getLineStartOffset(separatorPosition);
               lineEnd = MessagesPane.this.assemble.getLineEndOffset(separatorPosition);
               text = MessagesPane.this.assemble.getText(lineStart, lineEnd - lineStart);
            } catch (BadLocationException var18) {
               text = "";
            }

            if (text.length() > 0 && (text.startsWith("Error") || text.startsWith("Warning"))) {
               MessagesPane.this.assemble.select(lineStart, lineEnd);
               MessagesPane.this.assemble.setSelectionColor(Color.YELLOW);
               MessagesPane.this.assemble.repaint();
               separatorPosition = text.indexOf(": ");
               if (separatorPosition >= 0) {
                  text = text.substring(0, separatorPosition);
               }

               String[] stringTokens = text.split("\\s");
               String lineToken = " line ".trim();
               String columnToken = " column ".trim();
               String lineString = "";
               String columnString = "";

               int line;
               for(line = 0; line < stringTokens.length; ++line) {
                  if (stringTokens[line].equals(lineToken) && line < stringTokens.length - 1) {
                     lineString = stringTokens[line + 1];
                  }

                  if (stringTokens[line].equals(columnToken) && line < stringTokens.length - 1) {
                     columnString = stringTokens[line + 1];
                  }
               }

               try {
                  line = Integer.parseInt(lineString);
               } catch (NumberFormatException var17) {
                  line = 0;
               }

               int columnx;
               try {
                  columnx = Integer.parseInt(columnString);
               } catch (NumberFormatException var16) {
                  columnx = 0;
               }

               int fileNameStart = text.indexOf(" in ") + " in ".length();
               int fileNameEnd = text.indexOf(" line ");
               String fileName = "";
               if (fileNameStart < fileNameEnd && fileNameStart >= " in ".length()) {
                  fileName = text.substring(fileNameStart, fileNameEnd).trim();
               }

               if (fileName != null && fileName.length() > 0) {
                  MessagesPane.this.selectEditorTextLine(fileName, line, columnx);
                  MessagesPane.this.selectErrorMessage(fileName, line, columnx);
               }
            }

         }
      });
      JButton runTabClearButton = new JButton("Clear");
      runTabClearButton.setToolTipText("Clear the Run I/O area");
      runTabClearButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            MessagesPane.this.run.setText("");
         }
      });
      this.runTab = new JPanel(new BorderLayout());
      this.runTab.add(this.createBoxForButton(runTabClearButton), "West");
      this.runTab.add(new JScrollPane(this.run, 20, 30), "Center");
      this.addTab("Mars Messages", this.assembleTab);
      this.addTab("Run I/O", this.runTab);
      this.setToolTipTextAt(0, "Messages produced by Run menu. Click on assemble error message to select erroneous line");
      this.setToolTipTextAt(1, "Simulated MIPS console input and output");
   }

   private Box createBoxForButton(JButton button) {
      Box buttonRow = Box.createHorizontalBox();
      buttonRow.add(Box.createHorizontalStrut(6));
      buttonRow.add(button);
      buttonRow.add(Box.createHorizontalStrut(6));
      Box buttonBox = Box.createVerticalBox();
      buttonBox.add(Box.createVerticalGlue());
      buttonBox.add(buttonRow);
      buttonBox.add(Box.createVerticalGlue());
      return buttonBox;
   }

   public void selectErrorMessage(String fileName, int line, int column) {
      String errorReportSubstring = (new File(fileName)).getName() + " line " + line + " column " + column;
      int textPosition = this.assemble.getText().lastIndexOf(errorReportSubstring);
      if (textPosition >= 0) {
         try {
            int textLine = this.assemble.getLineOfOffset(textPosition);
            int lineStart = this.assemble.getLineStartOffset(textLine);
            int lineEnd = this.assemble.getLineEndOffset(textLine);
            this.assemble.setSelectionColor(Color.YELLOW);
            this.assemble.select(lineStart, lineEnd);
            this.assemble.getCaret().setSelectionVisible(true);
            this.assemble.repaint();
         } catch (BadLocationException var10) {
         }
      }

   }

   public void selectEditorTextLine(String fileName, int line, int column) {
      EditTabbedPane editTabbedPane = (EditTabbedPane)Globals.getGui().getMainPane().getEditTabbedPane();
      EditPane currentPane = null;
      EditPane editPane = editTabbedPane.getEditPaneForFile((new File(fileName)).getPath());
      if (editPane != null) {
         if (editPane != editTabbedPane.getCurrentEditTab()) {
            editTabbedPane.setCurrentEditTab(editPane);
         }

         currentPane = editPane;
      } else if (EditTabbedPane.openFile(new File(fileName))) {
         currentPane = editTabbedPane.getCurrentEditTab();
      }

      if (editPane != null && currentPane != null) {
         currentPane.selectLine(line, column);
      }

   }

   public JTextArea getAssembleTextArea() {
      return this.assemble;
   }

   public JTextArea getRunTextArea() {
      return this.run;
   }

   public void postMarsMessage(String message) {
      this.assemble.append(message);
      if (this.assemble.getDocument().getLength() > MAXIMUM_SCROLLED_CHARACTERS) {
         try {
            this.assemble.getDocument().remove(0, NUMBER_OF_CHARACTERS_TO_CUT);
         } catch (BadLocationException var3) {
         }
      }

      this.assemble.setCaretPosition(this.assemble.getDocument().getLength());
      this.setSelectedComponent(this.assembleTab);
   }

   public void postRunMessage(final String message) {
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            MessagesPane.this.setSelectedComponent(MessagesPane.this.runTab);
            MessagesPane.this.run.append(message);
            if (MessagesPane.this.run.getDocument().getLength() > MessagesPane.MAXIMUM_SCROLLED_CHARACTERS) {
               try {
                  MessagesPane.this.run.getDocument().remove(0, MessagesPane.NUMBER_OF_CHARACTERS_TO_CUT);
               } catch (BadLocationException var2) {
               }
            }

         }
      });
   }

   public void selectMarsMessageTab() {
      this.setSelectedComponent(this.assembleTab);
   }

   public void selectRunMessageTab() {
      this.setSelectedComponent(this.runTab);
   }

   public String getInputString(String prompt) {
      JOptionPane pane = new JOptionPane(prompt, 3, -1);
      pane.setWantsInput(true);
      JDialog dialog = pane.createDialog(Globals.getGui(), "MIPS Keyboard Input");
      dialog.setVisible(true);
      String input = (String)pane.getInputValue();
      this.postRunMessage(Globals.userInputAlert + input + "\n");
      return input;
   }

   public String getInputString(int maxLen) {
      Asker asker = new Asker(maxLen);
      return asker.response();
   }

   class Asker implements Runnable {
      ArrayBlockingQueue resultQueue = new ArrayBlockingQueue(1);
      int initialPos;
      int maxLen;
      final DocumentListener listener = new DocumentListener() {
         public void insertUpdate(final DocumentEvent e) {
            EventQueue.invokeLater(new Runnable() {
               public void run() {
                  try {
                     String inserted = e.getDocument().getText(e.getOffset(), e.getLength());
                     int i = inserted.indexOf(10);
                     if (i >= 0) {
                        int offset = e.getOffset() + i;
                        if (offset + 1 == e.getDocument().getLength()) {
                           Asker.this.returnResponse();
                        } else {
                           e.getDocument().remove(offset, 1);
                           e.getDocument().insertString(e.getDocument().getLength(), "\n", (AttributeSet)null);
                        }
                     } else if (Asker.this.maxLen >= 0 && e.getDocument().getLength() - Asker.this.initialPos >= Asker.this.maxLen) {
                        Asker.this.returnResponse();
                     }
                  } catch (BadLocationException var4) {
                     Asker.this.returnResponse();
                  }

               }
            });
         }

         public void removeUpdate(final DocumentEvent e) {
            EventQueue.invokeLater(new Runnable() {
               public void run() {
                  if ((e.getDocument().getLength() < Asker.this.initialPos || e.getOffset() < Asker.this.initialPos) && e instanceof UndoableEdit) {
                     ((UndoableEdit)e).undo();
                     MessagesPane.this.run.setCaretPosition(e.getOffset() + e.getLength());
                  }

               }
            });
         }

         public void changedUpdate(DocumentEvent e) {
         }
      };
      final NavigationFilter navigationFilter = new NavigationFilter() {
         public void moveDot(NavigationFilter.FilterBypass fb, int dot, Position.Bias bias) {
            if (dot < Asker.this.initialPos) {
               dot = Math.min(Asker.this.initialPos, MessagesPane.this.run.getDocument().getLength());
            }

            fb.moveDot(dot, bias);
         }

         public void setDot(NavigationFilter.FilterBypass fb, int dot, Position.Bias bias) {
            if (dot < Asker.this.initialPos) {
               dot = Math.min(Asker.this.initialPos, MessagesPane.this.run.getDocument().getLength());
            }

            fb.setDot(dot, bias);
         }
      };
      final Simulator.StopListener stopListener = new Simulator.StopListener() {
         public void stopped(Simulator s) {
            Asker.this.returnResponse();
         }
      };

      Asker(int maxLen) {
         this.maxLen = maxLen;
      }

      public void run() {
         MessagesPane.this.setSelectedComponent(MessagesPane.this.runTab);
         MessagesPane.this.run.setEditable(true);
         MessagesPane.this.run.requestFocusInWindow();
         MessagesPane.this.run.setCaretPosition(MessagesPane.this.run.getDocument().getLength());
         this.initialPos = MessagesPane.this.run.getCaretPosition();
         MessagesPane.this.run.setNavigationFilter(this.navigationFilter);
         MessagesPane.this.run.getDocument().addDocumentListener(this.listener);
         Simulator.getInstance().addStopListener(this.stopListener);
      }

      void cleanup() {
         EventQueue.invokeLater(new Runnable() {
            public void run() {
               MessagesPane.this.run.getDocument().removeDocumentListener(Asker.this.listener);
               MessagesPane.this.run.setEditable(false);
               MessagesPane.this.run.setNavigationFilter((NavigationFilter)null);
               MessagesPane.this.run.setCaretPosition(MessagesPane.this.run.getDocument().getLength());
               Simulator.getInstance().removeStopListener(Asker.this.stopListener);
            }
         });
      }

      void returnResponse() {
         try {
            int p = Math.min(this.initialPos, MessagesPane.this.run.getDocument().getLength());
            int l = Math.min(MessagesPane.this.run.getDocument().getLength() - p, this.maxLen >= 0 ? this.maxLen : Integer.MAX_VALUE);
            this.resultQueue.offer(MessagesPane.this.run.getText(p, l));
         } catch (BadLocationException var3) {
            this.resultQueue.offer("");
         }

      }

      String response() {
         EventQueue.invokeLater(this);

         try {
            String var3 = (String)this.resultQueue.take();
            return var3;
         } catch (InterruptedException var6) {
         } finally {
            this.cleanup();
         }

         return null;
      }
   }
}
