package mars.venus.editors.jeditsyntax;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JComponent;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import mars.Globals;
import mars.venus.EditPane;
import mars.venus.editors.MARSTextEditingArea;
import mars.venus.editors.jeditsyntax.tokenmarker.MIPSTokenMarker;

public class JEditBasedTextArea extends JEditTextArea implements MARSTextEditingArea, CaretListener {
   private EditPane editPane;
   private UndoManager undoManager;
   private UndoableEditListener undoableEditListener;
   private boolean isCompoundEdit = false;
   private CompoundEdit compoundEdit;
   private JEditBasedTextArea sourceCode;

   public JEditBasedTextArea(EditPane editPain, JComponent lineNumbers) {
      super(lineNumbers);
      this.editPane = editPain;
      this.undoManager = new UndoManager();
      this.compoundEdit = new CompoundEdit();
      this.sourceCode = this;
      this.undoableEditListener = new UndoableEditListener() {
         public void undoableEditHappened(UndoableEditEvent e) {
            if (JEditBasedTextArea.this.isCompoundEdit) {
               JEditBasedTextArea.this.compoundEdit.addEdit(e.getEdit());
            } else {
               JEditBasedTextArea.this.undoManager.addEdit(e.getEdit());
               JEditBasedTextArea.this.editPane.updateUndoState();
               JEditBasedTextArea.this.editPane.updateRedoState();
            }

         }
      };
      this.getDocument().addUndoableEditListener(this.undoableEditListener);
      this.setFont(Globals.getSettings().getEditorFont());
      this.setTokenMarker(new MIPSTokenMarker());
      this.addCaretListener(this);
   }

   public void setFont(Font f) {
      this.getPainter().setFont(f);
   }

   public Font getFont() {
      return this.getPainter().getFont();
   }

   public void setLineHighlightEnabled(boolean highlight) {
      this.getPainter().setLineHighlightEnabled(highlight);
   }

   public void setCaretBlinkRate(int rate) {
      if (rate == 0) {
         this.caretBlinks = false;
      }

      if (rate > 0) {
         this.caretBlinks = true;
         this.caretBlinkRate = rate;
         caretTimer.setDelay(rate);
         caretTimer.setInitialDelay(rate);
         caretTimer.restart();
      }

   }

   public void setTabSize(int chars) {
      this.painter.setTabSize(chars);
   }

   public void updateSyntaxStyles() {
      this.painter.setStyles(SyntaxUtilities.getCurrentSyntaxStyles());
   }

   public Component getOuterComponent() {
      return this;
   }

   public void discardAllUndoableEdits() {
      this.undoManager.discardAllEdits();
   }

   public void caretUpdate(CaretEvent e) {
      this.editPane.displayCaretPosition(((JEditTextArea.MutableCaretEvent)e).getDot());
   }

   public void replaceSelection(String replacementText) {
      this.setSelectedText(replacementText);
   }

   public void setSelectionVisible(boolean vis) {
   }

   public void setSourceCode(String s, boolean editable) {
      this.setText(s);
      this.setBackground(editable ? Color.WHITE : Color.GRAY);
      this.setEditable(editable);
      this.setEnabled(editable);
      this.setCaretPosition(0);
      if (editable) {
         this.requestFocusInWindow();
      }

   }

   public UndoManager getUndoManager() {
      return this.undoManager;
   }

   public void undo() {
      this.unredoing = true;

      try {
         this.undoManager.undo();
      } catch (CannotUndoException var2) {
         System.out.println("Unable to undo: " + var2);
         var2.printStackTrace();
      }

      this.unredoing = false;
      this.setCaretVisible(true);
   }

   public void redo() {
      this.unredoing = true;

      try {
         this.undoManager.redo();
      } catch (CannotRedoException var2) {
         System.out.println("Unable to redo: " + var2);
         var2.printStackTrace();
      }

      this.unredoing = false;
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
         this.sourceCode.setSelectionStart(nextPosn);
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
            this.sourceCode.setSelectionStart(nextPosn);
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
