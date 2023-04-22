package mars.venus.editors;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import javax.swing.text.Document;
import javax.swing.undo.UndoManager;

public interface MARSTextEditingArea {
   int TEXT_NOT_FOUND = 0;
   int TEXT_FOUND = 1;
   int TEXT_REPLACED_FOUND_NEXT = 2;
   int TEXT_REPLACED_NOT_FOUND_NEXT = 3;

   void copy();

   void cut();

   int doFindText(String var1, boolean var2);

   int doReplace(String var1, String var2, boolean var3);

   int doReplaceAll(String var1, String var2, boolean var3);

   int getCaretPosition();

   Document getDocument();

   String getSelectedText();

   int getSelectionEnd();

   int getSelectionStart();

   void select(int var1, int var2);

   void selectAll();

   String getText();

   UndoManager getUndoManager();

   void paste();

   void replaceSelection(String var1);

   void setCaretPosition(int var1);

   void setEditable(boolean var1);

   void setSelectionEnd(int var1);

   void setSelectionStart(int var1);

   void setText(String var1);

   void setFont(Font var1);

   Font getFont();

   boolean requestFocusInWindow();

   FontMetrics getFontMetrics(Font var1);

   void setBackground(Color var1);

   void setEnabled(boolean var1);

   void grabFocus();

   void redo();

   void revalidate();

   void setSourceCode(String var1, boolean var2);

   void setCaretVisible(boolean var1);

   void setSelectionVisible(boolean var1);

   void undo();

   void discardAllUndoableEdits();

   void setLineHighlightEnabled(boolean var1);

   void setCaretBlinkRate(int var1);

   void setTabSize(int var1);

   void updateSyntaxStyles();

   Component getOuterComponent();
}
