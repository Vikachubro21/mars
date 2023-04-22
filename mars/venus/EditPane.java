package mars.venus;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import mars.Globals;
import mars.venus.editors.MARSTextEditingArea;
import mars.venus.editors.generic.GenericTextArea;
import mars.venus.editors.jeditsyntax.JEditBasedTextArea;

public class EditPane extends JPanel implements Observer {
   private MARSTextEditingArea sourceCode;
   private VenusUI mainUI;
   private String currentDirectoryPath;
   private JLabel caretPositionLabel;
   private JCheckBox showLineNumbers;
   private JLabel lineNumbers;
   private static int count = 0;
   private boolean isCompoundEdit = false;
   private CompoundEdit compoundEdit;
   private FileStatus fileStatus;
   private static final String spaces = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
   private static final char newline = '\n';

   EditPane editPane;

   ChangeListener cL;

   public EditPane(VenusUI appFrame) {
      super(new BorderLayout());
      this.mainUI = appFrame;
      this.currentDirectoryPath = System.getProperty("user.dir");
      this.editPane = this;
      Globals.getSettings().addObserver(this);
      this.fileStatus = new FileStatus();
      this.lineNumbers = new JLabel();
      if (Globals.getSettings().getBooleanSetting(18)) {
         this.sourceCode = new GenericTextArea(this, this.lineNumbers);
      } else {
         this.sourceCode = new JEditBasedTextArea(this, this.lineNumbers);
      }
      this.add(this.sourceCode.getOuterComponent(), "Center");
      this.sourceCode.getDocument().addDocumentListener(new DocumentListener() {
         public void insertUpdate(DocumentEvent var1) {
            if(editPane.getFileStatus() != 4 && getFileStatus() != 2) {
               editPane.setFileStatus(4);
               if(cL != null) {
                  cL.stateChanged(new ChangeEvent(this));
               }
            }
         }

         public void removeUpdate(DocumentEvent evt) {
            this.insertUpdate(evt);
         }

         public void changedUpdate(DocumentEvent evt) {
            this.insertUpdate(evt);
         }
      });
      this.showLineNumbers = new JCheckBox("Show Line Numbers");
      this.showLineNumbers.setToolTipText("If checked, will display line number for each line of text.");
      this.showLineNumbers.setEnabled(false);
      this.showLineNumbers.setSelected(Globals.getSettings().getBooleanSetting(9));
      this.setSourceCode("", false);
      this.lineNumbers.setFont(this.getLineNumberFont(this.sourceCode.getFont()));
      this.lineNumbers.setVerticalAlignment(1);
      this.lineNumbers.setText("");
      this.lineNumbers.setVisible(true);
      this.showLineNumbers.addItemListener(new ItemListener() {
         public void itemStateChanged(ItemEvent e) {
            if (EditPane.this.showLineNumbers.isSelected()) {
               EditPane.this.lineNumbers.setText(EditPane.this.getLineNumbersList(EditPane.this.sourceCode.getDocument()));
               EditPane.this.lineNumbers.setVisible(true);
            } else {
               EditPane.this.lineNumbers.setText("");
               EditPane.this.lineNumbers.setVisible(false);
            }
            EditPane.this.sourceCode.revalidate();
            Globals.getSettings().setEditorLineNumbersDisplayed(EditPane.this.showLineNumbers.isSelected());
            EditPane.this.sourceCode.setCaretVisible(true);
            EditPane.this.sourceCode.requestFocusInWindow();
         }
      });
      JPanel editInfo = new JPanel(new BorderLayout());
      this.caretPositionLabel = new JLabel();
      this.caretPositionLabel.setToolTipText("Tracks the current position of the text editing cursor.");
      this.displayCaretPosition(new Point());
      editInfo.add(this.caretPositionLabel, "West");
      editInfo.add(this.showLineNumbers, "Center");
      this.add(editInfo, "South");
   }

   public void setChangeListener(ChangeListener x) {
      this.cL = x;
   }

   public void setSourceCode(String s, boolean editable) {
      this.sourceCode.setSourceCode(s, editable);
   }

   public void discardAllUndoableEdits() {
      this.sourceCode.discardAllUndoableEdits();
   }

   public String getLineNumbersList(Document doc) {
      StringBuffer lineNumberList = new StringBuffer("<html>");
      int lineCount = doc.getDefaultRootElement().getElementCount();
      int digits = Integer.toString(lineCount).length();

      for(int i = 1; i <= lineCount; ++i) {
         String lineStr = Integer.toString(i);
         int leadingSpaces = digits - lineStr.length();
         if (leadingSpaces == 0) {
            lineNumberList.append(lineStr + "&nbsp;<br>");
         } else {
            lineNumberList.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;".substring(0, leadingSpaces * 6) + lineStr + "&nbsp;<br>");
         }
      }

      lineNumberList.append("<br></html>");
      return lineNumberList.toString();
   }

   public int getSourceLineCount() {
      BufferedReader bufStringReader = new BufferedReader(new StringReader(this.sourceCode.getText()));
      int lineNums = 0;

      try {
         while(bufStringReader.readLine() != null) {
            ++lineNums;
         }
      } catch (IOException var4) {
      }

      return lineNums;
   }

   public String getSource() {
      return this.sourceCode.getText();
   }

   public void setFileStatus(int fileStatus) {
      this.fileStatus.setFileStatus(fileStatus);
   }

   public int getFileStatus() {
      return this.fileStatus.getFileStatus();
   }

   public String getFilename() {
      return this.fileStatus.getFilename();
   }

   public String getPathname() {
      return this.fileStatus.getPathname();
   }

   public void setPathname(String pathname) {
      this.fileStatus.setPathname(pathname);
   }

   public boolean hasUnsavedEdits() {
      return this.fileStatus.hasUnsavedEdits();
   }

   public boolean isNew() {
      return this.fileStatus.isNew();
   }

   public void tellEditingComponentToRequestFocusInWindow() {
      this.sourceCode.requestFocusInWindow();
   }

   public void updateStaticFileStatus() {
      this.fileStatus.updateStaticFileStatus();
   }

   public UndoManager getUndoManager() {
      return this.sourceCode.getUndoManager();
   }

   public void copyText() {
      this.sourceCode.copy();
      this.sourceCode.setCaretVisible(true);
      this.sourceCode.setSelectionVisible(true);
   }

   public void cutText() {
      this.sourceCode.cut();
      this.sourceCode.setCaretVisible(true);
   }

   public void pasteText() {
      this.sourceCode.paste();
      this.sourceCode.setCaretVisible(true);
   }

   public void selectAllText() {
      this.sourceCode.selectAll();
      this.sourceCode.setCaretVisible(true);
      this.sourceCode.setSelectionVisible(true);
   }

   public void undo() {
      this.sourceCode.undo();
   }

   public void redo() {
      this.sourceCode.redo();
   }

   public void updateUndoState() {
      this.mainUI.editUndoAction.updateUndoState();
   }

   public void updateRedoState() {
      this.mainUI.editRedoAction.updateRedoState();
   }

   public boolean showingLineNumbers() {
      return this.showLineNumbers.isSelected();
   }

   public void setShowLineNumbersEnabled(boolean enabled) {
      this.showLineNumbers.setEnabled(enabled);
   }

   public void displayCaretPosition(int pos) {
      this.displayCaretPosition(this.convertStreamPositionToLineColumn(pos));
   }

   public void displayCaretPosition(Point p) {
      this.caretPositionLabel.setText("Line: " + p.y + " Column: " + p.x);
   }

   public Point convertStreamPositionToLineColumn(int position) {
      String textStream = this.sourceCode.getText();
      int line = 1;
      int column = 1;

      for(int i = 0; i < position; ++i) {
         if (textStream.charAt(i) == '\n') {
            ++line;
            column = 1;
         } else {
            ++column;
         }
      }

      return new Point(column, line);
   }

   public int convertLineColumnToStreamPosition(int line, int column) {
      String textStream = this.sourceCode.getText();
      int textLength = textStream.length();
      int textLine = 1;
      int textColumn = 1;

      for(int i = 0; i < textLength; ++i) {
         if (textLine == line && textColumn == column) {
            return i;
         }

         if (textStream.charAt(i) == '\n') {
            ++textLine;
            textColumn = 1;
         } else {
            ++textColumn;
         }
      }

      return -1;
   }

   public void selectLine(int line) {
      if (line > 0) {
         int lineStartPosition = this.convertLineColumnToStreamPosition(line, 1);
         int lineEndPosition = this.convertLineColumnToStreamPosition(line + 1, 1) - 1;
         if (lineEndPosition < 0) {
            lineEndPosition = this.sourceCode.getText().length() - 1;
         }

         if (lineStartPosition >= 0) {
            this.sourceCode.select(lineStartPosition, lineEndPosition);
            this.sourceCode.setSelectionVisible(true);
         }
      }

   }

   public void selectLine(int line, int column) {
      this.selectLine(line);
   }

   public int doFindText(String find, boolean caseSensitive) {
      return this.sourceCode.doFindText(find, caseSensitive);
   }

   public int doReplace(String find, String replace, boolean caseSensitive) {
      return this.sourceCode.doReplace(find, replace, caseSensitive);
   }

   public int doReplaceAll(String find, String replace, boolean caseSensitive) {
      return this.sourceCode.doReplaceAll(find, replace, caseSensitive);
   }

   public void update(Observable fontChanger, Object arg) {
      this.sourceCode.setFont(Globals.getSettings().getEditorFont());
      this.sourceCode.setLineHighlightEnabled(Globals.getSettings().getBooleanSetting(15));
      this.sourceCode.setCaretBlinkRate(Globals.getSettings().getCaretBlinkRate());
      this.sourceCode.setTabSize(Globals.getSettings().getEditorTabSize());
      this.sourceCode.updateSyntaxStyles();
      this.sourceCode.revalidate();
      this.lineNumbers.setFont(this.getLineNumberFont(this.sourceCode.getFont()));
      this.lineNumbers.revalidate();
   }

   public void onFileOpenSetLineNumbersActive() {
      if (EditPane.this.showLineNumbers.isSelected()) {
         EditPane.this.lineNumbers.setText(EditPane.this.getLineNumbersList(EditPane.this.sourceCode.getDocument()));
         EditPane.this.lineNumbers.setVisible(true);
      }
   }

   private Font getLineNumberFont(Font sourceFont) {
      return this.sourceCode.getFont().getStyle() == 0 ? sourceFont : new Font(sourceFont.getFamily(), 0, sourceFont.getSize());
   }

}