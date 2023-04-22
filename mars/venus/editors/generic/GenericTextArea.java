package mars.venus.editors.generic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import mars.Globals;
import mars.venus.EditPane;
import mars.venus.editors.MARSTextEditingArea;

public class GenericTextArea extends JTextArea implements MARSTextEditingArea {
   private EditPane editPane;
   private UndoManager undoManager;
   private UndoableEditListener undoableEditListener;
   private JTextArea sourceCode;
   private JScrollPane editAreaScrollPane;
   private boolean isCompoundEdit = false;
   private CompoundEdit compoundEdit;

   public GenericTextArea(EditPane editPain, JComponent lineNumbers) {
      this.editPane = editPain;
      this.sourceCode = this;
      this.setFont(Globals.getSettings().getEditorFont());
      this.setTabSize(Globals.getSettings().getEditorTabSize());
      this.setMargin(new Insets(0, 3, 3, 3));
      this.setCaretBlinkRate(Globals.getSettings().getCaretBlinkRate());
      JPanel source = new JPanel(new BorderLayout());
      source.add(lineNumbers, "West");
      source.add(this, "Center");
      this.editAreaScrollPane = new JScrollPane(source, 22, 32);
      this.editAreaScrollPane.getVerticalScrollBar().setUnitIncrement(this.sourceCode.getFontMetrics(this.sourceCode.getFont()).getHeight());
      this.undoManager = new UndoManager();
      this.getCaret().addChangeListener(new ChangeListener() {
         public void stateChanged(ChangeEvent e) {
            GenericTextArea.this.editPane.displayCaretPosition(GenericTextArea.this.getCaretPosition());
         }
      });
      this.undoableEditListener = new UndoableEditListener() {
         public void undoableEditHappened(UndoableEditEvent e) {
            if (GenericTextArea.this.isCompoundEdit) {
               GenericTextArea.this.compoundEdit.addEdit(e.getEdit());
            } else {
               GenericTextArea.this.undoManager.addEdit(e.getEdit());
               GenericTextArea.this.editPane.updateUndoState();
               GenericTextArea.this.editPane.updateRedoState();
            }

         }
      };
      this.getDocument().addUndoableEditListener(this.undoableEditListener);
   }

   public void setLineHighlightEnabled(boolean highlight) {
   }

   public void updateSyntaxStyles() {
   }

   public void setCaretBlinkRate(int rate) {
      if (rate >= 0) {
         this.getCaret().setBlinkRate(rate);
      }

   }

   public Component getOuterComponent() {
      return this.editAreaScrollPane;
   }

   public void setSourceCode(String s, boolean editable) {
      this.setText(s);
      this.setBackground(editable ? Color.WHITE : Color.GRAY);
      this.setEditable(editable);
      this.setEnabled(editable);
      this.getCaret().setVisible(editable);
      this.setCaretPosition(0);
      if (editable) {
         this.requestFocusInWindow();
      }

   }

   public void discardAllUndoableEdits() {
      this.undoManager.discardAllEdits();
   }

   public void setText(String s) {
      this.getDocument().removeUndoableEditListener(this.undoableEditListener);
      super.setText(s);
      this.getDocument().addUndoableEditListener(this.undoableEditListener);
   }

   public void setCaretVisible(boolean vis) {
      this.getCaret().setVisible(vis);
   }

   public void setSelectionVisible(boolean vis) {
      this.getCaret().setSelectionVisible(vis);
   }

   public UndoManager getUndoManager() {
      return this.undoManager;
   }

   public void undo() {
      try {
         this.undoManager.undo();
      } catch (CannotUndoException var2) {
         System.out.println("Unable to undo: " + var2);
         var2.printStackTrace();
      }

      this.setCaretVisible(true);
   }

   public void redo() {
      try {
         this.undoManager.redo();
      } catch (CannotRedoException var2) {
         System.out.println("Unable to redo: " + var2);
         var2.printStackTrace();
      }

      this.setCaretVisible(true);
   }

   public int doFindText(String find, boolean caseSensitive) {
      int findPosn = this.sourceCode.getCaretPosition();
      int nextPosn = this.nextIndex(this.sourceCode.getText(), find, findPosn, caseSensitive);
      if (nextPosn >= 0) {
         this.sourceCode.requestFocus();
         this.sourceCode.setSelectionStart(nextPosn);
         this.sourceCode.setSelectionEnd(nextPosn + find.length());
         this.sourceCode.setSelectionStart(nextPosn);
         return 1;
      } else {
         return 0;
      }
   }

   public int nextIndex(String input, String find, int start, boolean caseSensitive) {
      int textPosn = -1;
      if (input != null && find != null && start < input.length()) {
         if (caseSensitive) {
            textPosn = input.indexOf(find, start);
            if (start > 0 && textPosn < 0) {
               textPosn = input.indexOf(find);
            }
         } else {
            String lowerCaseText = input.toLowerCase();
            textPosn = lowerCaseText.indexOf(find.toLowerCase(), start);
            if (start > 0 && textPosn < 0) {
               textPosn = lowerCaseText.indexOf(find.toLowerCase());
            }
         }
      }

      return textPosn;
   }

   public int doReplace(String find, String replace, boolean caseSensitive) {
      if (find != null && find.equals(this.sourceCode.getSelectedText()) && this.sourceCode.getSelectionEnd() == this.sourceCode.getCaretPosition()) {
         int nextPosn = this.sourceCode.getSelectionStart();
         this.sourceCode.grabFocus();
         this.sourceCode.setSelectionStart(nextPosn);
         this.sourceCode.setSelectionEnd(nextPosn + find.length());
         this.isCompoundEdit = true;
         this.compoundEdit = new CompoundEdit();
         this.sourceCode.replaceSelection(replace);
         this.compoundEdit.end();
         this.undoManager.addEdit(this.compoundEdit);
         this.editPane.updateUndoState();
         this.editPane.updateRedoState();
         this.isCompoundEdit = false;
         this.sourceCode.setCaretPosition(nextPosn + replace.length());
         return this.doFindText(find, caseSensitive) == 0 ? 3 : 2;
      } else {
         return this.doFindText(find, caseSensitive);
      }
   }

   public int doReplaceAll(String find, String replace, boolean caseSensitive) {
      int nextPosn = 0;
      int findPosn = 0;
      int replaceCount = 0;
      this.compoundEdit = null;
      this.isCompoundEdit = true;

      while(nextPosn >= 0) {
         nextPosn = this.nextIndex(this.sourceCode.getText(), find, findPosn, caseSensitive);
         if (nextPosn >= 0) {
            if (nextPosn < findPosn) {
               break;
            }

            this.sourceCode.grabFocus();
            this.sourceCode.setSelectionStart(nextPosn);
            this.sourceCode.setSelectionEnd(nextPosn + find.length());
            if (this.compoundEdit == null) {
               this.compoundEdit = new CompoundEdit();
            }

            this.sourceCode.replaceSelection(replace);
            findPosn = nextPosn + replace.length();
            ++replaceCount;
         }
      }

      this.isCompoundEdit = false;
      if (this.compoundEdit != null) {
         this.compoundEdit.end();
         this.undoManager.addEdit(this.compoundEdit);
         this.editPane.updateUndoState();
         this.editPane.updateRedoState();
      }

      return replaceCount;
   }
}
