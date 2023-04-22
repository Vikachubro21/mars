package mars.venus.editors.jeditsyntax;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Segment;
import javax.swing.text.Utilities;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import mars.Globals;
import mars.venus.editors.jeditsyntax.tokenmarker.Token;
import mars.venus.editors.jeditsyntax.tokenmarker.TokenMarker;

public class JEditTextArea extends JComponent {
   public static String LEFT_OF_SCROLLBAR = "los";
   public static Color POPUP_HELP_TEXT_COLOR;
   private static final int VERTICAL_SCROLLBAR_UNIT_INCREMENT_IN_LINES = 1;
   private static final int LINES_PER_MOUSE_WHEEL_NOTCH = 3;
   private JScrollBar lineNumbersVertical;
   JPopupMenu popupMenu;
   protected static String CENTER;
   protected static String RIGHT;
   protected static String BOTTOM;
   protected static JEditTextArea focusedComponent;
   protected static Timer caretTimer;
   protected TextAreaPainter painter;
   protected JPopupMenu popup;
   protected EventListenerList listenerList;
   protected MutableCaretEvent caretEvent;
   protected boolean caretBlinks;
   protected boolean caretVisible;
   protected boolean blink;
   protected boolean editable;
   protected int caretBlinkRate;
   protected int firstLine;
   protected int visibleLines;
   protected int electricScroll;
   protected int horizontalOffset;
   protected JScrollBar vertical;
   protected JScrollBar horizontal;
   protected boolean scrollBarsInitialized;
   protected InputHandler inputHandler;
   protected SyntaxDocument document;
   protected DocumentHandler documentHandler;
   protected Segment lineSegment;
   protected int selectionStart;
   protected int selectionStartLine;
   protected int selectionEnd;
   protected int selectionEndLine;
   protected boolean biasLeft;
   protected int bracketPosition;
   protected int bracketLine;
   protected int magicCaret;
   protected boolean overwrite;
   protected boolean rectSelect;
   protected boolean unredoing;

   static {
      POPUP_HELP_TEXT_COLOR = Color.BLACK;
      CENTER = "center";
      RIGHT = "right";
      BOTTOM = "bottom";
      caretTimer = new Timer(500, new CaretBlinker());
      caretTimer.setInitialDelay(500);
      caretTimer.start();
   }

   public JEditTextArea(JComponent lineNumbers) {
      this(TextAreaDefaults.getDefaults(), lineNumbers);
   }

   public JEditTextArea(TextAreaDefaults defaults, JComponent lineNumbers) {
      this.unredoing = false;
      this.enableEvents(8L);
      this.painter = new TextAreaPainter(this, defaults);
      this.documentHandler = new DocumentHandler();
      this.listenerList = new EventListenerList();
      this.caretEvent = new MutableCaretEvent();
      this.lineSegment = new Segment();
      this.bracketLine = this.bracketPosition = -1;
      this.blink = true;
      this.unredoing = false;
      if(Globals.getSettings().getBooleanSetting(21)){
         POPUP_HELP_TEXT_COLOR = new Color(0xf0f0f0);
      }
      JScrollPane lineNumberScroller = new JScrollPane(lineNumbers, 21, 31);
      lineNumberScroller.setBorder(new EmptyBorder(1, 1, 1, 1));
      this.lineNumbersVertical = lineNumberScroller.getVerticalScrollBar();
      JPanel lineNumbersPlusPainter = new JPanel(new BorderLayout());
      lineNumbersPlusPainter.add(this.painter, "Center");
      lineNumbersPlusPainter.add(lineNumberScroller, "West");
      this.setLayout(new ScrollLayout());
      this.add(CENTER, lineNumbersPlusPainter);
      this.add(RIGHT, this.vertical = new JScrollBar(1));
      this.add(BOTTOM, this.horizontal = new JScrollBar(0));
      this.vertical.addAdjustmentListener(new AdjustHandler());
      this.horizontal.addAdjustmentListener(new AdjustHandler());
      this.painter.addComponentListener(new ComponentHandler());
      this.painter.addMouseListener(new MouseHandler());
      this.painter.addMouseMotionListener(new DragHandler());
      this.painter.addMouseWheelListener(new MouseWheelHandler());
      this.addFocusListener(new FocusHandler());
      this.setInputHandler(defaults.inputHandler);
      this.setDocument(defaults.document);
      this.editable = defaults.editable;
      this.caretVisible = defaults.caretVisible;
      this.caretBlinks = defaults.caretBlinks;
      this.caretBlinkRate = defaults.caretBlinkRate;
      this.electricScroll = defaults.electricScroll;
      this.popup = defaults.popup;
      caretTimer.setDelay(this.caretBlinkRate);
      KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
         public boolean dispatchKeyEvent(KeyEvent e) {
            if (JEditTextArea.this.isFocusOwner() && e.getKeyCode() == 9 && e.getModifiers() == 0) {
               JEditTextArea.this.processKeyEvent(e);
               return true;
            } else {
               return false;
            }
         }
      });
      focusedComponent = this;
   }

   public final TextAreaPainter getPainter() {
      return this.painter;
   }

   public final InputHandler getInputHandler() {
      return this.inputHandler;
   }

   public void setInputHandler(InputHandler inputHandler) {
      this.inputHandler = inputHandler;
   }

   public final boolean isCaretBlinkEnabled() {
      return this.caretBlinks;
   }

   public void setCaretBlinkEnabled(boolean caretBlinks) {
      this.caretBlinks = caretBlinks;
      if (!caretBlinks) {
         this.blink = false;
      }

      this.painter.invalidateSelectedLines();
   }

   public final boolean isCaretVisible() {
      return (!this.caretBlinks || this.blink) && this.caretVisible;
   }

   public void setCaretVisible(boolean caretVisible) {
      this.caretVisible = caretVisible;
      this.blink = true;
      this.painter.invalidateSelectedLines();
   }

   public final void blinkCaret() {
      if (this.caretBlinks) {
         this.blink = !this.blink;
         this.painter.invalidateSelectedLines();
      } else {
         this.blink = true;
      }

   }

   public final int getElectricScroll() {
      return this.electricScroll;
   }

   public final void setElectricScroll(int electricScroll) {
      this.electricScroll = electricScroll;
   }

   public void updateScrollBars() {
      int width;
      if (this.vertical != null && this.visibleLines != 0) {
         this.vertical.setValues(this.firstLine, this.visibleLines, 0, this.getLineCount());
         this.vertical.setUnitIncrement(1);
         this.vertical.setBlockIncrement(this.visibleLines);
         width = this.painter.getFontMetrics(this.painter.getFont()).getHeight();
         this.lineNumbersVertical.setValues(this.firstLine * width, this.visibleLines * width, 0, this.getLineCount() * width);
         this.lineNumbersVertical.setUnitIncrement(1 * width);
         this.lineNumbersVertical.setBlockIncrement(this.visibleLines * width);
      }

      width = this.painter.getWidth();
      if (this.horizontal != null && width != 0) {
         this.horizontal.setValues(-this.horizontalOffset, width, 0, width * 5);
         this.horizontal.setUnitIncrement(this.painter.getFontMetrics().charWidth('w'));
         this.horizontal.setBlockIncrement(width / 2);
      }

   }

   public final int getFirstLine() {
      return this.firstLine;
   }

   public void setFirstLine(int firstLine) {
      if (firstLine != this.firstLine) {
         int oldFirstLine = this.firstLine;
         this.firstLine = firstLine;
         this.updateScrollBars();
         this.painter.repaint();
      }
   }

   public final int getVisibleLines() {
      return this.visibleLines;
   }

   public final void recalculateVisibleLines() {
      if (this.painter != null) {
         int height = this.painter.getHeight();
         int lineHeight = this.painter.getFontMetrics().getHeight();
         int oldVisibleLines = this.visibleLines;
         this.visibleLines = height / lineHeight;
         this.updateScrollBars();
      }
   }

   public final int getHorizontalOffset() {
      return this.horizontalOffset;
   }

   public void setHorizontalOffset(int horizontalOffset) {
      if (horizontalOffset != this.horizontalOffset) {
         this.horizontalOffset = horizontalOffset;
         if (horizontalOffset != this.horizontal.getValue()) {
            this.updateScrollBars();
         }

         this.painter.repaint();
      }
   }

   public boolean setOrigin(int firstLine, int horizontalOffset) {
      boolean changed = false;
      int oldFirstLine = this.firstLine;
      if (horizontalOffset != this.horizontalOffset) {
         this.horizontalOffset = horizontalOffset;
         changed = true;
      }

      if (firstLine != this.firstLine) {
         this.firstLine = firstLine;
         changed = true;
      }

      if (changed) {
         this.updateScrollBars();
         this.painter.repaint();
      }

      return changed;
   }

   public boolean scrollToCaret() {
      int line = this.getCaretLine();
      int lineStart = this.getLineStartOffset(line);
      int offset = Math.max(0, Math.min(this.getLineLength(line) - 1, this.getCaretPosition() - lineStart));
      return this.scrollTo(line, offset);
   }

   public boolean scrollTo(int line, int offset) {
      if (this.visibleLines == 0) {
         this.setFirstLine(Math.max(0, line - this.electricScroll));
         return true;
      } else {
         int newFirstLine = this.firstLine;
         int newHorizontalOffset = this.horizontalOffset;
         if (line < this.firstLine + this.electricScroll) {
            newFirstLine = Math.max(0, line - this.electricScroll);
         } else if (line + this.electricScroll >= this.firstLine + this.visibleLines) {
            newFirstLine = line - this.visibleLines + this.electricScroll + 1;
            if (newFirstLine + this.visibleLines >= this.getLineCount()) {
               newFirstLine = this.getLineCount() - this.visibleLines;
            }

            if (newFirstLine < 0) {
               newFirstLine = 0;
            }
         }

         int x = this._offsetToX(line, offset);
         int width = this.painter.getFontMetrics().charWidth('w');
         if (x < 0) {
            newHorizontalOffset = Math.min(0, this.horizontalOffset - x + width + 5);
         } else if (x + width >= this.painter.getWidth()) {
            newHorizontalOffset = this.horizontalOffset + (this.painter.getWidth() - x) - width - 5;
         }

         return this.setOrigin(newFirstLine, newHorizontalOffset);
      }
   }

   public int lineToY(int line) {
      FontMetrics fm = this.painter.getFontMetrics();
      return (line - this.firstLine) * fm.getHeight() - (fm.getLeading() + fm.getMaxDescent());
   }

   public int yToLine(int y) {
      FontMetrics fm = this.painter.getFontMetrics();
      int height = fm.getHeight();
      return Math.max(0, Math.min(this.getLineCount() - 1, y / height + this.firstLine));
   }

   public final int offsetToX(int line, int offset) {
      this.painter.currentLineTokens = null;
      return this._offsetToX(line, offset);
   }

   public int _offsetToX(int line, int offset) {
      TokenMarker tokenMarker = this.getTokenMarker();
      FontMetrics fm = this.painter.getFontMetrics();
      this.getLineText(line, this.lineSegment);
      int segmentOffset = this.lineSegment.offset;
      int x = this.horizontalOffset;
      if (tokenMarker == null) {
         this.lineSegment.count = offset;
         return x + Utilities.getTabbedTextWidth(this.lineSegment, fm, x, this.painter, 0);
      } else {
         Token tokens;
         if (this.painter.currentLineIndex == line && this.painter.currentLineTokens != null) {
            tokens = this.painter.currentLineTokens;
         } else {
            this.painter.currentLineIndex = line;
            tokens = this.painter.currentLineTokens = tokenMarker.markTokens(this.lineSegment, line);
         }

         Toolkit toolkit = this.painter.getToolkit();
         Font defaultFont = this.painter.getFont();
         SyntaxStyle[] styles = this.painter.getStyles();

         while(true) {
            byte id = tokens.id;
            if (id == 127) {
               return x;
            }

            if (id == 0) {
               fm = this.painter.getFontMetrics();
            } else {
               fm = styles[id].getFontMetrics(defaultFont);
            }

            int length = tokens.length;
            if (offset + segmentOffset < this.lineSegment.offset + length) {
               this.lineSegment.count = offset - (this.lineSegment.offset - segmentOffset);
               return x + Utilities.getTabbedTextWidth(this.lineSegment, fm, x, this.painter, 0);
            }

            this.lineSegment.count = length;
            x += Utilities.getTabbedTextWidth(this.lineSegment, fm, x, this.painter, 0);
            Segment var10000 = this.lineSegment;
            var10000.offset += length;
            tokens = tokens.next;
         }
      }
   }

   public int xToOffset(int line, int x) {
      TokenMarker tokenMarker = this.getTokenMarker();
      FontMetrics fm = this.painter.getFontMetrics();
      this.getLineText(line, this.lineSegment);
      char[] segmentArray = this.lineSegment.array;
      int segmentOffset = this.lineSegment.offset;
      int segmentCount = this.lineSegment.count;
      int width = this.horizontalOffset;
      if (tokenMarker == null) {
         for(int i = 0; i < segmentCount; ++i) {
            char c = segmentArray[i + segmentOffset];
            int charWidth;
            if (c == '\t') {
               charWidth = (int)this.painter.nextTabStop((float)width, i) - width;
            } else {
               charWidth = fm.charWidth(c);
            }

            if (this.painter.isBlockCaretEnabled()) {
               if (x - charWidth <= width) {
                  return i;
               }
            } else if (x - charWidth / 2 <= width) {
               return i;
            }

            width += charWidth;
         }

         return segmentCount;
      } else {
         Token tokens;
         if (this.painter.currentLineIndex == line && this.painter.currentLineTokens != null) {
            tokens = this.painter.currentLineTokens;
         } else {
            this.painter.currentLineIndex = line;
            tokens = this.painter.currentLineTokens = tokenMarker.markTokens(this.lineSegment, line);
         }

         int offset = 0;
         Toolkit toolkit = this.painter.getToolkit();
         Font defaultFont = this.painter.getFont();
         SyntaxStyle[] styles = this.painter.getStyles();

         while(true) {
            byte id = tokens.id;
            if (id == 127) {
               return offset;
            }

            if (id == 0) {
               fm = this.painter.getFontMetrics();
            } else {
               fm = styles[id].getFontMetrics(defaultFont);
            }

            int length = tokens.length;

            for(int i = 0; i < length; ++i) {
               char c = segmentArray[segmentOffset + offset + i];
               int charWidth;
               if (c == '\t') {
                  charWidth = (int)this.painter.nextTabStop((float)width, offset + i) - width;
               } else {
                  charWidth = fm.charWidth(c);
               }

               if (this.painter.isBlockCaretEnabled()) {
                  if (x - charWidth <= width) {
                     return offset + i;
                  }
               } else if (x - charWidth / 2 <= width) {
                  return offset + i;
               }

               width += charWidth;
            }

            offset += length;
            tokens = tokens.next;
         }
      }
   }

   public int xyToOffset(int x, int y) {
      int line = this.yToLine(y);
      int start = this.getLineStartOffset(line);
      return start + this.xToOffset(line, x);
   }

   public final Document getDocument() {
      return this.document;
   }

   public void setDocument(SyntaxDocument document) {
      if (this.document != document) {
         if (this.document != null) {
            this.document.removeDocumentListener(this.documentHandler);
         }

         this.document = document;
         document.addDocumentListener(this.documentHandler);
         this.select(0, 0);
         this.updateScrollBars();
         this.painter.repaint();
      }
   }

   public final TokenMarker getTokenMarker() {
      return this.document.getTokenMarker();
   }

   public final void setTokenMarker(TokenMarker tokenMarker) {
      this.document.setTokenMarker(tokenMarker);
   }

   public final int getDocumentLength() {
      return this.document.getLength();
   }

   public final int getLineCount() {
      return this.document.getDefaultRootElement().getElementCount();
   }

   public final int getLineOfOffset(int offset) {
      return this.document.getDefaultRootElement().getElementIndex(offset);
   }

   public int getLineStartOffset(int line) {
      Element lineElement = this.document.getDefaultRootElement().getElement(line);
      return lineElement == null ? -1 : lineElement.getStartOffset();
   }

   public int getLineEndOffset(int line) {
      Element lineElement = this.document.getDefaultRootElement().getElement(line);
      return lineElement == null ? -1 : lineElement.getEndOffset();
   }

   public int getLineLength(int line) {
      Element lineElement = this.document.getDefaultRootElement().getElement(line);
      return lineElement == null ? -1 : lineElement.getEndOffset() - lineElement.getStartOffset() - 1;
   }

   public String getText() {
      try {
         return this.document.getText(0, this.document.getLength());
      } catch (BadLocationException var2) {
         var2.printStackTrace();
         return null;
      }
   }

   public void setText(String text) {
      try {
         this.document.beginCompoundEdit();
         this.document.remove(0, this.document.getLength());
         this.document.insertString(0, text, (AttributeSet)null);
      } catch (BadLocationException var6) {
         var6.printStackTrace();
      } finally {
         this.document.endCompoundEdit();
      }

   }

   public final String getText(int start, int len) {
      try {
         return this.document.getText(start, len);
      } catch (BadLocationException var4) {
         var4.printStackTrace();
         return null;
      }
   }

   public final void getText(int start, int len, Segment segment) {
      try {
         this.document.getText(start, len, segment);
      } catch (BadLocationException var5) {
         var5.printStackTrace();
         segment.offset = segment.count = 0;
      }

   }

   public final String getLineText(int lineIndex) {
      int start = this.getLineStartOffset(lineIndex);
      return this.getText(start, this.getLineEndOffset(lineIndex) - start - 1);
   }

   public final void getLineText(int lineIndex, Segment segment) {
      int start = this.getLineStartOffset(lineIndex);
      this.getText(start, this.getLineEndOffset(lineIndex) - start - 1, segment);
   }

   public final int getSelectionStart() {
      return this.selectionStart;
   }

   public int getSelectionStart(int line) {
      if (line == this.selectionStartLine) {
         return this.selectionStart;
      } else if (this.rectSelect) {
         Element map = this.document.getDefaultRootElement();
         int start = this.selectionStart - map.getElement(this.selectionStartLine).getStartOffset();
         Element lineElement = map.getElement(line);
         int lineStart = lineElement.getStartOffset();
         int lineEnd = lineElement.getEndOffset() - 1;
         return Math.min(lineEnd, lineStart + start);
      } else {
         return this.getLineStartOffset(line);
      }
   }

   public final int getSelectionStartLine() {
      return this.selectionStartLine;
   }

   public final void setSelectionStart(int selectionStart) {
      this.select(selectionStart, this.selectionEnd);
   }

   public final int getSelectionEnd() {
      return this.selectionEnd;
   }

   public int getSelectionEnd(int line) {
      if (line == this.selectionEndLine) {
         return this.selectionEnd;
      } else if (this.rectSelect) {
         Element map = this.document.getDefaultRootElement();
         int end = this.selectionEnd - map.getElement(this.selectionEndLine).getStartOffset();
         Element lineElement = map.getElement(line);
         int lineStart = lineElement.getStartOffset();
         int lineEnd = lineElement.getEndOffset() - 1;
         return Math.min(lineEnd, lineStart + end);
      } else {
         return this.getLineEndOffset(line) - 1;
      }
   }

   public final int getSelectionEndLine() {
      return this.selectionEndLine;
   }

   public final void setSelectionEnd(int selectionEnd) {
      this.select(this.selectionStart, selectionEnd);
   }

   public final int getCaretPosition() {
      return this.biasLeft ? this.selectionStart : this.selectionEnd;
   }

   public final int getCaretLine() {
      return this.biasLeft ? this.selectionStartLine : this.selectionEndLine;
   }

   public final int getMarkPosition() {
      return this.biasLeft ? this.selectionEnd : this.selectionStart;
   }

   public final int getMarkLine() {
      return this.biasLeft ? this.selectionEndLine : this.selectionStartLine;
   }

   public final void setCaretPosition(int caret) {
      this.select(caret, caret);
   }

   public final void selectAll() {
      this.select(0, this.getDocumentLength());
   }

   public final void selectNone() {
      this.select(this.getCaretPosition(), this.getCaretPosition());
   }

   public void select(int start, int end) {
      int newStart;
      int newEnd;
      boolean newBias;
      if (start <= end) {
         newStart = start;
         newEnd = end;
         newBias = false;
      } else {
         newStart = end;
         newEnd = start;
         newBias = true;
      }

      if (newStart >= 0 && newEnd <= this.getDocumentLength()) {
         if (newStart != this.selectionStart || newEnd != this.selectionEnd || newBias != this.biasLeft) {
            int newStartLine = this.getLineOfOffset(newStart);
            int newEndLine = this.getLineOfOffset(newEnd);
            if (this.painter.isBracketHighlightEnabled()) {
               if (this.bracketLine != -1) {
                  this.painter.invalidateLine(this.bracketLine);
               }

               this.updateBracketHighlight(end);
               if (this.bracketLine != -1) {
                  this.painter.invalidateLine(this.bracketLine);
               }
            }

            this.painter.invalidateLineRange(this.selectionStartLine, this.selectionEndLine);
            this.painter.invalidateLineRange(newStartLine, newEndLine);
            this.document.addUndoableEdit(new CaretUndo(this.selectionStart, this.selectionEnd));
            this.selectionStart = newStart;
            this.selectionEnd = newEnd;
            this.selectionStartLine = newStartLine;
            this.selectionEndLine = newEndLine;
            this.biasLeft = newBias;
            this.fireCaretEvent();
         }

         this.blink = true;
         caretTimer.restart();
         if (this.selectionStart == this.selectionEnd) {
            this.rectSelect = false;
         }

         this.magicCaret = -1;
         this.scrollToCaret();
      } else {
         throw new IllegalArgumentException("Bounds out of range: " + newStart + "," + newEnd);
      }
   }

   public final String getSelectedText() {
      if (this.selectionStart == this.selectionEnd) {
         return null;
      } else if (this.rectSelect) {
         Element map = this.document.getDefaultRootElement();
         int start = this.selectionStart - map.getElement(this.selectionStartLine).getStartOffset();
         int end = this.selectionEnd - map.getElement(this.selectionEndLine).getStartOffset();
         if (end < start) {
            int tmp = end;
            end = start;
            start = tmp;
         }

         StringBuffer buf = new StringBuffer();
         Segment seg = new Segment();

         for(int i = this.selectionStartLine; i <= this.selectionEndLine; ++i) {
            Element lineElement = map.getElement(i);
            int lineStart = lineElement.getStartOffset();
            int lineEnd = lineElement.getEndOffset() - 1;
            int var10000 = lineEnd - lineStart;
            lineStart = Math.min(lineStart + start, lineEnd);
            int lineLen = Math.min(end - start, lineEnd - lineStart);
            this.getText(lineStart, lineLen, seg);
            buf.append(seg.array, seg.offset, seg.count);
            if (i != this.selectionEndLine) {
               buf.append('\n');
            }
         }

         return buf.toString();
      } else {
         return this.getText(this.selectionStart, this.selectionEnd - this.selectionStart);
      }
   }

   public void setSelectedText(String selectedText) {
      if (!this.editable) {
         throw new InternalError("Text component read only");
      } else {
         this.document.beginCompoundEdit();

         try {
            if (this.rectSelect) {
               Element map = this.document.getDefaultRootElement();
               int start = this.selectionStart - map.getElement(this.selectionStartLine).getStartOffset();
               int end = this.selectionEnd - map.getElement(this.selectionEndLine).getStartOffset();
               int lastNewline;
               if (end < start) {
                  lastNewline = end;
                  end = start;
                  start = lastNewline;
               }

               lastNewline = 0;
               int currNewline = 0;

               int offset;
               for(offset = this.selectionStartLine; offset <= this.selectionEndLine; ++offset) {
                  Element lineElement = map.getElement(offset);
                  int lineStart = lineElement.getStartOffset();
                  int lineEnd = lineElement.getEndOffset() - 1;
                  int rectStart = Math.min(lineEnd, lineStart + start);
                  this.document.remove(rectStart, Math.min(lineEnd - rectStart, end - start));
                  if (selectedText != null) {
                     currNewline = selectedText.indexOf(10, lastNewline);
                     if (currNewline == -1) {
                        currNewline = selectedText.length();
                     }

                     this.document.insertString(rectStart, selectedText.substring(lastNewline, currNewline), (AttributeSet)null);
                     lastNewline = Math.min(selectedText.length(), currNewline + 1);
                  }
               }

               if (selectedText != null && currNewline != selectedText.length()) {
                  offset = map.getElement(this.selectionEndLine).getEndOffset() - 1;
                  this.document.insertString(offset, "\n", (AttributeSet)null);
                  this.document.insertString(offset + 1, selectedText.substring(currNewline + 1), (AttributeSet)null);
               }
            } else {
               this.document.remove(this.selectionStart, this.selectionEnd - this.selectionStart);
               if (selectedText != null) {
                  this.document.insertString(this.selectionStart, selectedText, (AttributeSet)null);
               }
            }
         } catch (BadLocationException var15) {
            var15.printStackTrace();
            throw new InternalError("Cannot replace selection");
         } finally {
            this.document.endCompoundEdit();
         }

         this.setCaretPosition(this.selectionEnd);
      }
   }

   public final boolean isEditable() {
      return this.editable;
   }

   public final void setEditable(boolean editable) {
      this.editable = editable;
   }

   public final JPopupMenu getRightClickPopup() {
      return this.popup;
   }

   public final void setRightClickPopup(JPopupMenu popup) {
      this.popup = popup;
   }

   public final int getMagicCaretPosition() {
      return this.magicCaret;
   }

   public final void setMagicCaretPosition(int magicCaret) {
      this.magicCaret = magicCaret;
   }

   public void overwriteSetSelectedText(String str) {
      if (this.overwrite && this.selectionStart == this.selectionEnd) {
         int caret = this.getCaretPosition();
         int caretLineEnd = this.getLineEndOffset(this.getCaretLine());
         if (caretLineEnd - caret <= str.length()) {
            this.setSelectedText(str);
            this.applySyntaxSensitiveHelp();
         } else {
            this.document.beginCompoundEdit();

            try {
               this.document.remove(caret, str.length());
               this.document.insertString(caret, str, (AttributeSet)null);
            } catch (BadLocationException var8) {
               var8.printStackTrace();
            } finally {
               this.document.endCompoundEdit();
            }

            this.applySyntaxSensitiveHelp();
         }
      } else {
         this.setSelectedText(str);
         this.applySyntaxSensitiveHelp();
      }
   }

   public final boolean isOverwriteEnabled() {
      return this.overwrite;
   }

   public final void setOverwriteEnabled(boolean overwrite) {
      this.overwrite = overwrite;
      this.painter.invalidateSelectedLines();
   }

   public final boolean isSelectionRectangular() {
      return this.rectSelect;
   }

   public final void setSelectionRectangular(boolean rectSelect) {
      this.rectSelect = rectSelect;
      this.painter.invalidateSelectedLines();
   }

   public final int getBracketPosition() {
      return this.bracketPosition;
   }

   public final int getBracketLine() {
      return this.bracketLine;
   }

   public final void addCaretListener(CaretListener listener) {
      this.listenerList.add(CaretListener.class, listener);
   }

   public final void removeCaretListener(CaretListener listener) {
      this.listenerList.remove(CaretListener.class, listener);
   }

   public void cut() {
      if (this.editable) {
         this.copy();
         this.setSelectedText("");
      }

   }

   public void copy() {
      if (this.selectionStart != this.selectionEnd) {
         Clipboard clipboard = this.getToolkit().getSystemClipboard();
         String selection = this.getSelectedText();
         int repeatCount = this.inputHandler.getRepeatCount();
         StringBuffer buf = new StringBuffer();

         for(int i = 0; i < repeatCount; ++i) {
            buf.append(selection);
         }

         clipboard.setContents(new StringSelection(buf.toString()), (ClipboardOwner)null);
      }

   }

   public void paste() {
      if (this.editable) {
         Clipboard clipboard = this.getToolkit().getSystemClipboard();

         try {
            String selection = ((String)clipboard.getContents(this).getTransferData(DataFlavor.stringFlavor)).replace('\r', '\n');
            int repeatCount = this.inputHandler.getRepeatCount();
            StringBuffer buf = new StringBuffer();

            for(int i = 0; i < repeatCount; ++i) {
               buf.append(selection);
            }

            selection = buf.toString();
            this.setSelectedText(selection);
         } catch (Exception var6) {
            this.getToolkit().beep();
            System.err.println("Clipboard does not contain a string");
         }
      }

   }

   public void removeNotify() {
      super.removeNotify();
      if (focusedComponent == this) {
         focusedComponent = null;
      }

   }

   public void processKeyEvent(KeyEvent evt) {
      if (this.inputHandler != null) {
         switch (evt.getID()) {
            case 400:
               this.inputHandler.keyTyped(evt);
               break;
            case 401:
               if (!this.checkPopupCompletion(evt)) {
                  this.inputHandler.keyPressed(evt);
               }

               this.checkPopupMenu(evt);
               break;
            case 402:
               this.inputHandler.keyReleased(evt);
         }

      }
   }

   protected void fireCaretEvent() {
      Object[] listeners = this.listenerList.getListenerList();

      for(int i = listeners.length - 2; i >= 0; --i) {
         if (listeners[i] == CaretListener.class) {
            ((CaretListener)listeners[i + 1]).caretUpdate(this.caretEvent);
         }
      }

   }

   protected void updateBracketHighlight(int newCaretPosition) {
      if (newCaretPosition == 0) {
         this.bracketPosition = this.bracketLine = -1;
      } else {
         try {
            int offset = TextUtilities.findMatchingBracket(this.document, newCaretPosition - 1);
            if (offset != -1) {
               this.bracketLine = this.getLineOfOffset(offset);
               this.bracketPosition = offset - this.getLineStartOffset(this.bracketLine);
               return;
            }
         } catch (BadLocationException var3) {
            var3.printStackTrace();
         }

         this.bracketLine = this.bracketPosition = -1;
      }
   }

   protected void documentChanged(DocumentEvent evt) {
      DocumentEvent.ElementChange ch = evt.getChange(this.document.getDefaultRootElement());
      int count;
      if (ch == null) {
         count = 0;
      } else {
         count = ch.getChildrenAdded().length - ch.getChildrenRemoved().length;
      }

      int line = this.getLineOfOffset(evt.getOffset());
      if (count == 0) {
         this.painter.invalidateLine(line);
      } else if (line < this.firstLine) {
         this.setFirstLine(this.firstLine + count);
      } else {
         this.painter.invalidateLineRange(line, this.firstLine + this.visibleLines);
         this.updateScrollBars();
      }

   }

   public String getSyntaxSensitiveToolTipText(int x, int y) {
      String result = null;
      int line = this.yToLine(y);
      ArrayList matches = this.getSyntaxSensitiveHelpAtLineOffset(line, this.xToOffset(line, x), true);
      if (matches == null) {
         return null;
      } else {
         int length = PopupHelpItem.maxExampleLength(matches) + 2;
         result = "<html>";

         for(int i = 0; i < matches.size(); ++i) {
            PopupHelpItem match = (PopupHelpItem)matches.get(i);
            result = result + (i == 0 ? "" : "<br>") + "<tt>" + match.getExamplePaddedToLength(length).replaceAll(" ", "&nbsp;") + "</tt>" + match.getDescription();
         }

         return result + "</html>";
      }
   }

   public String getAutoIndent() {
      return Globals.getSettings().getBooleanSetting(19) ? this.getLeadingWhiteSpace() : "";
   }

   public String getLeadingWhiteSpace() {
      int line = this.getCaretLine();
      int lineLength = this.getLineLength(line);
      String indent = "";
      if (lineLength > 0) {
         String text = this.getText(this.getLineStartOffset(line), lineLength);

         for(int position = 0; position < text.length(); ++position) {
            char character = text.charAt(position);
            if (character != '\t' && character != ' ') {
               break;
            }

            indent = indent + character;
         }
      }

      return indent;
   }

   private ArrayList getSyntaxSensitiveHelpAtLineOffset(int line, int offset, boolean exact) {
      ArrayList matches = null;
      TokenMarker tokenMarker = this.getTokenMarker();
      if (tokenMarker != null) {
         Segment lineSegment = new Segment();
         this.getLineText(line, lineSegment);
         Token tokens = tokenMarker.markTokens(lineSegment, line);
         Token tokenList = tokens;
         int tokenOffset = 0;
         Token tokenAtOffset = null;

         for(Token toke = tokens; toke.id != 127; toke = toke.next) {
         }

         while(true) {
            byte id = tokens.id;
            if (id == 127) {
               break;
            }

            int length = tokens.length;
            if (offset > tokenOffset && offset <= tokenOffset + length) {
               tokenAtOffset = tokens;
               break;
            }

            tokenOffset += length;
            tokens = tokens.next;
         }

         if (tokenAtOffset != null) {
            String tokenText = lineSegment.toString().substring(tokenOffset, tokenOffset + tokenAtOffset.length);
            if (exact) {
               matches = tokenMarker.getTokenExactMatchHelp(tokenAtOffset, tokenText);
            } else {
               matches = tokenMarker.getTokenPrefixMatchHelp(lineSegment.toString(), tokenList, tokenAtOffset, tokenText);
            }
         }
      }

      return matches;
   }

   private void applySyntaxSensitiveHelp() {
      if (Globals.getSettings().getBooleanSetting(16)) {
         int line = this.getCaretLine();
         int lineStart = this.getLineStartOffset(line);
         int offset = Math.max(1, Math.min(this.getLineLength(line), this.getCaretPosition() - lineStart));
         ArrayList helpItems = this.getSyntaxSensitiveHelpAtLineOffset(line, offset, false);
         if (helpItems == null && this.popupMenu != null) {
            this.popupMenu.setVisible(false);
            this.popupMenu = null;
         }

         if (helpItems != null) {
            this.popupMenu = new JPopupMenu();
            int length = PopupHelpItem.maxExampleLength(helpItems) + 2;

            int y;
            for(y = 0; y < helpItems.size(); ++y) {
               PopupHelpItem item = (PopupHelpItem)helpItems.get(y);
               JMenuItem menuItem = new JMenuItem("<html><tt>" + item.getExamplePaddedToLength(length).replaceAll(" ", "&nbsp;") + "</tt>" + item.getDescription() + "</html>");
               if (item.getExact()) {
                  menuItem.setSelected(false);
               } else {
                  menuItem.addActionListener(new PopupHelpActionListener(item.getTokenText(), item.getExample()));
               }

               this.popupMenu.add(menuItem);
            }

            this.popupMenu.pack();
            y = this.lineToY(line);
            int x = this.offsetToX(line, offset);
            int height = this.painter.getFontMetrics(this.painter.getFont()).getHeight();
            int width = this.painter.getFontMetrics(this.painter.getFont()).charWidth('w');
            int menuXLoc = x + width + width + width;
            int menuYLoc = y + height + height;
            this.popupMenu.show(this, menuXLoc, menuYLoc);
            this.requestFocusInWindow();
         }

      }
   }

   private void checkAutoIndent(KeyEvent evt) {
      if (evt.getKeyCode() == 10) {
         int line = this.getCaretLine();
         if (line <= 0) {
            return;
         }

         int previousLine = line - 1;
         int previousLineLength = this.getLineLength(previousLine);
         if (previousLineLength <= 0) {
            return;
         }

         String previous = this.getText(this.getLineStartOffset(previousLine), previousLineLength);
         String indent = "";

         for(int position = 0; position < previous.length(); ++position) {
            char character = previous.charAt(position);
            if (character != '\t' && character != ' ') {
               break;
            }

            indent = indent + character;
         }

         this.overwriteSetSelectedText(indent);
      }

   }

   private void checkPopupMenu(KeyEvent evt) {
      if (evt.getKeyCode() == 8 || evt.getKeyCode() == 127) {
         this.applySyntaxSensitiveHelp();
      }

      if ((evt.getKeyCode() == 10 || evt.getKeyCode() == 27) && this.popupMenu != null && this.popupMenu.isVisible()) {
         this.popupMenu.setVisible(false);
      }

   }

   private boolean checkPopupCompletion(KeyEvent evt) {
      MenuElement[] path;
      AbstractButton item;
      if ((evt.getKeyCode() == 38 || evt.getKeyCode() == 40) && this.popupMenu != null && this.popupMenu.isVisible() && this.popupMenu.getComponentCount() > 0) {
         path = MenuSelectionManager.defaultManager().getSelectedPath();
         if (path.length >= 1 && path[path.length - 1] instanceof AbstractButton) {
            item = (AbstractButton)path[path.length - 1].getComponent();
            if (item.isEnabled()) {
               int index = this.popupMenu.getComponentIndex(item);
               if (index < 0) {
                  return false;
               } else {
                  if (evt.getKeyCode() == 38) {
                     index = index == 0 ? this.popupMenu.getComponentCount() - 1 : index - 1;
                  } else {
                     index = index == this.popupMenu.getComponentCount() - 1 ? 0 : index + 1;
                  }

                  final MenuElement[] newPath = new MenuElement[]{path[0], (MenuElement)this.popupMenu.getComponentAtIndex(index)};
                  SwingUtilities.invokeLater(new Runnable() {
                     public void run() {
                        MenuSelectionManager.defaultManager().setSelectedPath(newPath);
                     }
                  });
                  return true;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         if ((evt.getKeyCode() == 9 || evt.getKeyCode() == 10) && this.popupMenu != null && this.popupMenu.isVisible() && this.popupMenu.getComponentCount() > 0) {
            path = MenuSelectionManager.defaultManager().getSelectedPath();
            if (path.length < 1 || !(path[path.length - 1] instanceof AbstractButton)) {
               return false;
            }

            item = (AbstractButton)path[path.length - 1].getComponent();
            if (item.isEnabled()) {
               ActionListener[] listeners = item.getActionListeners();
               if (listeners.length > 0) {
                  listeners[0].actionPerformed(new ActionEvent(item, 1001, evt.getKeyCode() == 9 ? "\t" : " "));
                  return true;
               }
            }
         }

         return false;
      }
   }

   class AdjustHandler implements AdjustmentListener {
      public void adjustmentValueChanged(final AdjustmentEvent evt) {
         if (JEditTextArea.this.scrollBarsInitialized) {
            SwingUtilities.invokeLater(new Runnable() {
               public void run() {
                  if (evt.getAdjustable() == JEditTextArea.this.vertical) {
                     JEditTextArea.this.setFirstLine(JEditTextArea.this.vertical.getValue());
                  } else {
                     JEditTextArea.this.setHorizontalOffset(-JEditTextArea.this.horizontal.getValue());
                  }

               }
            });
         }
      }
   }

   static class CaretBlinker implements ActionListener {
      public void actionPerformed(ActionEvent evt) {
         if (JEditTextArea.focusedComponent != null && JEditTextArea.focusedComponent.hasFocus()) {
            JEditTextArea.focusedComponent.blinkCaret();
         }

      }
   }

   class CaretUndo extends AbstractUndoableEdit {
      private int start;
      private int end;

      CaretUndo(int start, int end) {
         this.start = start;
         this.end = end;
      }

      public boolean isSignificant() {
         return false;
      }

      public String getPresentationName() {
         return "caret move";
      }

      public void undo() throws CannotUndoException {
         super.undo();
         JEditTextArea.this.select(this.start, this.end);
      }

      public void redo() throws CannotRedoException {
         super.redo();
         JEditTextArea.this.select(this.start, this.end);
      }

      public boolean addEdit(UndoableEdit edit) {
         if (edit instanceof CaretUndo) {
            CaretUndo cedit = (CaretUndo)edit;
            this.start = cedit.start;
            this.end = cedit.end;
            cedit.die();
            return true;
         } else {
            return false;
         }
      }
   }

   class ComponentHandler extends ComponentAdapter {
      public void componentResized(ComponentEvent evt) {
         JEditTextArea.this.recalculateVisibleLines();
         JEditTextArea.this.scrollBarsInitialized = true;
      }
   }

   class DocumentHandler implements DocumentListener {
      public void insertUpdate(DocumentEvent evt) {
         JEditTextArea.this.documentChanged(evt);
         int offset = evt.getOffset();
         int length = evt.getLength();
         if (JEditTextArea.this.unredoing) {
            JEditTextArea.this.select(offset, offset + length);
         } else {
            int newStart;
            if (JEditTextArea.this.selectionStart <= offset && (JEditTextArea.this.selectionStart != JEditTextArea.this.selectionEnd || JEditTextArea.this.selectionStart != offset)) {
               newStart = JEditTextArea.this.selectionStart;
            } else {
               newStart = JEditTextArea.this.selectionStart + length;
            }

            int newEnd;
            if (JEditTextArea.this.selectionEnd >= offset) {
               newEnd = JEditTextArea.this.selectionEnd + length;
            } else {
               newEnd = JEditTextArea.this.selectionEnd;
            }

            JEditTextArea.this.select(newStart, newEnd);
         }
      }

      public void removeUpdate(DocumentEvent evt) {
         JEditTextArea.this.documentChanged(evt);
         int offset = evt.getOffset();
         int length = evt.getLength();
         if (JEditTextArea.this.unredoing) {
            JEditTextArea.this.select(offset, offset);
            JEditTextArea.this.setCaretPosition(offset);
         } else {
            int newStart;
            if (JEditTextArea.this.selectionStart > offset) {
               if (JEditTextArea.this.selectionStart > offset + length) {
                  newStart = JEditTextArea.this.selectionStart - length;
               } else {
                  newStart = offset;
               }
            } else {
               newStart = JEditTextArea.this.selectionStart;
            }

            int newEnd;
            if (JEditTextArea.this.selectionEnd > offset) {
               if (JEditTextArea.this.selectionEnd > offset + length) {
                  newEnd = JEditTextArea.this.selectionEnd - length;
               } else {
                  newEnd = offset;
               }
            } else {
               newEnd = JEditTextArea.this.selectionEnd;
            }

            JEditTextArea.this.select(newStart, newEnd);
         }
      }

      public void changedUpdate(DocumentEvent evt) {
      }
   }

   class DragHandler implements MouseMotionListener {
      public void mouseDragged(MouseEvent evt) {
         if (JEditTextArea.this.popup == null || !JEditTextArea.this.popup.isVisible()) {
            JEditTextArea.this.setSelectionRectangular((evt.getModifiers() & 2) != 0);
            JEditTextArea.this.select(JEditTextArea.this.getMarkPosition(), JEditTextArea.this.xyToOffset(evt.getX(), evt.getY()));
         }
      }

      public void mouseMoved(MouseEvent evt) {
      }
   }

   class FocusHandler implements FocusListener {
      public void focusGained(FocusEvent evt) {
         JEditTextArea.this.setCaretVisible(true);
         JEditTextArea.focusedComponent = JEditTextArea.this;
      }

      public void focusLost(FocusEvent evt) {
         JEditTextArea.this.setCaretVisible(false);
         JEditTextArea.focusedComponent = null;
      }
   }

   class MouseHandler extends MouseAdapter {
      public void mousePressed(MouseEvent evt) {
         JEditTextArea.this.requestFocus();
         JEditTextArea.this.setCaretVisible(true);
         JEditTextArea.focusedComponent = JEditTextArea.this;
         if ((evt.getModifiers() & 4) != 0 && JEditTextArea.this.popup != null) {
            JEditTextArea.this.popup.show(JEditTextArea.this.painter, evt.getX(), evt.getY());
         } else {
            int line = JEditTextArea.this.yToLine(evt.getY());
            int offset = JEditTextArea.this.xToOffset(line, evt.getX());
            int dot = JEditTextArea.this.getLineStartOffset(line) + offset;
            switch (evt.getClickCount()) {
               case 1:
                  this.doSingleClick(evt, line, offset, dot);
                  break;
               case 2:
                  try {
                     this.doDoubleClick(evt, line, offset, dot);
                  } catch (BadLocationException var6) {
                     var6.printStackTrace();
                  }
                  break;
               case 3:
                  this.doTripleClick(evt, line, offset, dot);
            }

         }
      }

      private void doSingleClick(MouseEvent evt, int line, int offset, int dot) {
         if ((evt.getModifiers() & 1) != 0) {
            JEditTextArea.this.rectSelect = (evt.getModifiers() & 2) != 0;
            JEditTextArea.this.select(JEditTextArea.this.getMarkPosition(), dot);
         } else {
            JEditTextArea.this.setCaretPosition(dot);
         }

      }

      private void doDoubleClick(MouseEvent evt, int line, int offset, int dot) throws BadLocationException {
         if (JEditTextArea.this.getLineLength(line) != 0) {
            int ch;
            try {
               int bracket = TextUtilities.findMatchingBracket(JEditTextArea.this.document, Math.max(0, dot - 1));
               if (bracket != -1) {
                  ch = JEditTextArea.this.getMarkPosition();
                  if (bracket > ch) {
                     ++bracket;
                     --ch;
                  }

                  JEditTextArea.this.select(ch, bracket);
                  return;
               }
            } catch (BadLocationException var12) {
               var12.printStackTrace();
            }

            String lineText = JEditTextArea.this.getLineText(line);
            ch = lineText.charAt(Math.max(0, offset - 1));
            String noWordSep = (String)JEditTextArea.this.document.getProperty("noWordSep");
            if (noWordSep == null) {
               noWordSep = "";
            }

            boolean selectNoLetter = !Character.isLetterOrDigit((char)ch) && noWordSep.indexOf(ch) == -1;
            int wordStart = 0;

            int wordEnd;
            for(wordEnd = offset - 1; wordEnd >= 0; --wordEnd) {
               ch = lineText.charAt(wordEnd);
               if (selectNoLetter ^ (!Character.isLetterOrDigit((char)ch) && noWordSep.indexOf(ch) == -1)) {
                  wordStart = wordEnd + 1;
                  break;
               }
            }

            wordEnd = lineText.length();

            int i;
            for(i = offset; i < lineText.length(); ++i) {
               ch = lineText.charAt(i);
               if (selectNoLetter ^ (!Character.isLetterOrDigit((char)ch) && noWordSep.indexOf(ch) == -1)) {
                  wordEnd = i;
                  break;
               }
            }

            i = JEditTextArea.this.getLineStartOffset(line);
            JEditTextArea.this.select(i + wordStart, i + wordEnd);
         }
      }

      private void doTripleClick(MouseEvent evt, int line, int offset, int dot) {
         JEditTextArea.this.select(JEditTextArea.this.getLineStartOffset(line), JEditTextArea.this.getLineEndOffset(line) - 1);
      }
   }

   class MouseWheelHandler implements MouseWheelListener {
      public void mouseWheelMoved(MouseWheelEvent e) {
         int maxMotion = Math.abs(e.getWheelRotation()) * 3;
         if (e.getWheelRotation() < 0) {
            JEditTextArea.this.setFirstLine(JEditTextArea.this.getFirstLine() - Math.min(maxMotion, JEditTextArea.this.getFirstLine()));
         } else {
            JEditTextArea.this.setFirstLine(JEditTextArea.this.getFirstLine() + Math.min(maxMotion, Math.max(0, JEditTextArea.this.getLineCount() - (JEditTextArea.this.getFirstLine() + JEditTextArea.this.visibleLines))));
         }

      }
   }

   class MutableCaretEvent extends CaretEvent {
      MutableCaretEvent() {
         super(JEditTextArea.this);
      }

      public int getDot() {
         return JEditTextArea.this.getCaretPosition();
      }

      public int getMark() {
         return JEditTextArea.this.getMarkPosition();
      }
   }

   private class PopupHelpActionListener implements ActionListener {
      private String tokenText;
      private String text;

      public PopupHelpActionListener(String tokenText, String text) {
         this.tokenText = tokenText;
         this.text = text.split(" ")[0];
      }

      public void actionPerformed(ActionEvent e) {
         String insert = e.getActionCommand().charAt(0) == '\t' ? "\t" : " ";
         if (this.tokenText.length() >= this.text.length()) {
            JEditTextArea.this.overwriteSetSelectedText(insert);
         } else {
            JEditTextArea.this.overwriteSetSelectedText(this.text.substring(this.tokenText.length()) + insert);
         }

      }
   }

   class ScrollLayout implements LayoutManager {
      private Component center;
      private Component right;
      private Component bottom;
      private Vector leftOfScrollBar = new Vector();

      public void addLayoutComponent(String name, Component comp) {
         if (name.equals(JEditTextArea.CENTER)) {
            this.center = comp;
         } else if (name.equals(JEditTextArea.RIGHT)) {
            this.right = comp;
         } else if (name.equals(JEditTextArea.BOTTOM)) {
            this.bottom = comp;
         } else if (name.equals(JEditTextArea.LEFT_OF_SCROLLBAR)) {
            this.leftOfScrollBar.addElement(comp);
         }

      }

      public void removeLayoutComponent(Component comp) {
         if (this.center == comp) {
            this.center = null;
         }

         if (this.right == comp) {
            this.right = null;
         }

         if (this.bottom == comp) {
            this.bottom = null;
         } else {
            this.leftOfScrollBar.removeElement(comp);
         }

      }

      public Dimension preferredLayoutSize(Container parent) {
         Dimension dim = new Dimension();
         Insets insets = JEditTextArea.this.getInsets();
         dim.width = insets.left + insets.right;
         dim.height = insets.top + insets.bottom;
         Dimension centerPref = this.center.getPreferredSize();
         dim.width += centerPref.width;
         dim.height += centerPref.height;
         Dimension rightPref = this.right.getPreferredSize();
         dim.width += rightPref.width;
         Dimension bottomPref = this.bottom.getPreferredSize();
         dim.height += bottomPref.height;
         return dim;
      }

      public Dimension minimumLayoutSize(Container parent) {
         Dimension dim = new Dimension();
         Insets insets = JEditTextArea.this.getInsets();
         dim.width = insets.left + insets.right;
         dim.height = insets.top + insets.bottom;
         Dimension centerPref = this.center.getMinimumSize();
         dim.width += centerPref.width;
         dim.height += centerPref.height;
         Dimension rightPref = this.right.getMinimumSize();
         dim.width += rightPref.width;
         Dimension bottomPref = this.bottom.getMinimumSize();
         dim.height += bottomPref.height;
         return dim;
      }

      public void layoutContainer(Container parent) {
         Dimension size = parent.getSize();
         Insets insets = parent.getInsets();
         int itop = insets.top;
         int ileft = insets.left;
         int ibottom = insets.bottom;
         int iright = insets.right;
         int rightWidth = this.right.getPreferredSize().width;
         int bottomHeight = this.bottom.getPreferredSize().height;
         int centerWidth = size.width - rightWidth - ileft - iright;
         int centerHeight = size.height - bottomHeight - itop - ibottom;
         this.center.setBounds(ileft, itop, centerWidth, centerHeight);
         this.right.setBounds(ileft + centerWidth, itop, rightWidth, centerHeight);

         Dimension dim;
         for(Enumeration status = this.leftOfScrollBar.elements(); status.hasMoreElements(); ileft += dim.width) {
            Component comp = (Component)status.nextElement();
            dim = comp.getPreferredSize();
            comp.setBounds(ileft, itop + centerHeight, dim.width, bottomHeight);
         }

         this.bottom.setBounds(ileft, itop + centerHeight, size.width - rightWidth - ileft - iright, bottomHeight);
      }
   }
}
