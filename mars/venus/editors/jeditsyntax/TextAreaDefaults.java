package mars.venus.editors.jeditsyntax;

import java.awt.Color;
import javax.swing.JPopupMenu;
import mars.Globals;

public class TextAreaDefaults {
   private static TextAreaDefaults DEFAULTS;
   public InputHandler inputHandler;
   public SyntaxDocument document;
   public boolean editable;
   public boolean caretVisible;
   public boolean caretBlinks;
   public boolean blockCaret;
   public int caretBlinkRate;
   public int electricScroll;
   public int tabSize;
   public int cols;
   public int rows;
   public SyntaxStyle[] styles;
   public Color caretColor;
   public Color selectionColor;
   public Color lineHighlightColor;
   public boolean lineHighlight;
   public Color bracketHighlightColor;
   public boolean bracketHighlight;
   public Color eolMarkerColor;
   public boolean eolMarkers;
   public boolean paintInvalid;
   public JPopupMenu popup;

   public static TextAreaDefaults getDefaults() {
      DEFAULTS = new TextAreaDefaults();
      DEFAULTS.inputHandler = new DefaultInputHandler();
      DEFAULTS.inputHandler.addDefaultKeyBindings();
      DEFAULTS.editable = true;
      DEFAULTS.blockCaret = false;
      DEFAULTS.caretVisible = true;
      DEFAULTS.caretBlinks = Globals.getSettings().getCaretBlinkRate() != 0;
      DEFAULTS.caretBlinkRate = Globals.getSettings().getCaretBlinkRate();
      DEFAULTS.tabSize = Globals.getSettings().getEditorTabSize();
      DEFAULTS.electricScroll = 0;
      DEFAULTS.cols = 80;
      DEFAULTS.rows = 25;
      DEFAULTS.styles = SyntaxUtilities.getCurrentSyntaxStyles();
      if(Globals.getSettings().getBooleanSetting(21)) {
         DEFAULTS.selectionColor = new Color(0x214283);
         DEFAULTS.lineHighlightColor = new Color(0x3c3f41);
         DEFAULTS.bracketHighlightColor = Color.white;
         DEFAULTS.caretColor = Color.white;
      }
      else {
         DEFAULTS.selectionColor = new Color(13421823);
         DEFAULTS.lineHighlightColor = new Color(15658734);
         DEFAULTS.bracketHighlightColor = Color.black;
         DEFAULTS.caretColor = Color.black;
      }
      DEFAULTS.lineHighlight = Globals.getSettings().getBooleanSetting(15);
      DEFAULTS.bracketHighlight = false;
      DEFAULTS.eolMarkerColor = new Color(39321);
      DEFAULTS.eolMarkers = false;
      DEFAULTS.paintInvalid = false;
      DEFAULTS.document = new SyntaxDocument();
      return DEFAULTS;
   }
}
