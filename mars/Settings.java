package mars;

import java.awt.Color;
import java.awt.Font;
import java.util.Arrays;
import java.util.Observable;
import java.util.StringTokenizer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import mars.util.Binary;
import mars.util.EditorFont;
import mars.venus.editors.jeditsyntax.SyntaxStyle;
import mars.venus.editors.jeditsyntax.SyntaxUtilities;

public class Settings extends Observable {
   private static String settingsFile = "Settings";
   public static final int EXTENDED_ASSEMBLER_ENABLED = 0;
   public static final int BARE_MACHINE_ENABLED = 1;
   public static final int ASSEMBLE_ON_OPEN_ENABLED = 2;
   public static final int ASSEMBLE_ALL_ENABLED = 3;
   public static final int LABEL_WINDOW_VISIBILITY = 4;
   public static final int DISPLAY_ADDRESSES_IN_HEX = 5;
   public static final int DISPLAY_VALUES_IN_HEX = 6;
   public static final int EXCEPTION_HANDLER_ENABLED = 7;
   public static final int DELAYED_BRANCHING_ENABLED = 8;
   public static final int EDITOR_LINE_NUMBERS_DISPLAYED = 9;
   public static final int WARNINGS_ARE_ERRORS = 10;
   public static final int PROGRAM_ARGUMENTS = 11;
   public static final int DATA_SEGMENT_HIGHLIGHTING = 12;
   public static final int REGISTERS_HIGHLIGHTING = 13;
   public static final int START_AT_MAIN = 14;
   public static final int EDITOR_CURRENT_LINE_HIGHLIGHTING = 15;
   public static final int POPUP_INSTRUCTION_GUIDANCE = 16;
   public static final int POPUP_SYSCALL_INPUT = 17;
   public static final int GENERIC_TEXT_EDITOR = 18;
   public static final int AUTO_INDENT = 19;
   public static final int SELF_MODIFYING_CODE_ENABLED = 20;
   public static final int DARK_THEME_ENABLED = 21;
   private static String[] booleanSettingsKeys = new String[]{"ExtendedAssembler", "BareMachine", "AssembleOnOpen", "AssembleAll", "LabelWindowVisibility", "DisplayAddressesInHex", "DisplayValuesInHex", "LoadExceptionHandler", "DelayedBranching", "EditorLineNumbersDisplayed", "WarningsAreErrors", "ProgramArguments", "DataSegmentHighlighting", "RegistersHighlighting", "StartAtMain", "EditorCurrentLineHighlighting", "PopupInstructionGuidance", "PopupSyscallInput", "GenericTextEditor", "AutoIndent", "SelfModifyingCode", "DarkThemeEnabled"};
   public static boolean[] defaultBooleanSettingsValues = new boolean[]{true, false, false, false, false, true, true, false, false, true, false, false, true, true, false, true, true, false, false, true, false, true};
   public static final int EXCEPTION_HANDLER = 0;
   public static final int TEXT_COLUMN_ORDER = 1;
   public static final int LABEL_SORT_STATE = 2;
   public static final int MEMORY_CONFIGURATION = 3;
   public static final int CARET_BLINK_RATE = 4;
   public static final int EDITOR_TAB_SIZE = 5;
   public static final int EDITOR_POPUP_PREFIX_LENGTH = 6;
   private static final String[] stringSettingsKeys = new String[]{"ExceptionHandler", "TextColumnOrder", "LabelSortState", "MemoryConfiguration", "CaretBlinkRate", "EditorTabSize", "EditorPopupPrefixLength"};
   private static String[] defaultStringSettingsValues = new String[]{"", "0 1 2 3 4", "0", "", "500", "8", "2"};
   public static final int EDITOR_FONT = 0;
   public static final int EVEN_ROW_FONT = 1;
   public static final int ODD_ROW_FONT = 2;
   public static final int TEXTSEGMENT_HIGHLIGHT_FONT = 3;
   public static final int TEXTSEGMENT_DELAYSLOT_HIGHLIGHT_FONT = 4;
   public static final int DATASEGMENT_HIGHLIGHT_FONT = 5;
   public static final int REGISTER_HIGHLIGHT_FONT = 6;
   private static final String[] fontFamilySettingsKeys = new String[]{"EditorFontFamily", "EvenRowFontFamily", "OddRowFontFamily", " TextSegmentHighlightFontFamily", "TextSegmentDelayslotHighightFontFamily", "DataSegmentHighlightFontFamily", "RegisterHighlightFontFamily"};
   private static final String[] fontStyleSettingsKeys = new String[]{"EditorFontStyle", "EvenRowFontStyle", "OddRowFontStyle", " TextSegmentHighlightFontStyle", "TextSegmentDelayslotHighightFontStyle", "DataSegmentHighlightFontStyle", "RegisterHighlightFontStyle"};
   private static final String[] fontSizeSettingsKeys = new String[]{"EditorFontSize", "EvenRowFontSize", "OddRowFontSize", " TextSegmentHighlightFontSize", "TextSegmentDelayslotHighightFontSize", "DataSegmentHighlightFontSize", "RegisterHighlightFontSize"};
   private static final String[] defaultFontFamilySettingsValues = new String[]{"Lucida Sans Typewriter", "Lucida Sans Typewriter", "Lucida Sans Typewriter", "Lucida Sans Typewriter", "Lucida Sans Typewriter", "Lucida Sans Typewriter", "Lucida Sans Typewriter"};
   private static final String[] defaultFontStyleSettingsValues = new String[]{"Plain", "Plain", "Plain", "Plain", "Plain", "Plain", "Plain"};
   private static final String[] defaultFontSizeSettingsValues = new String[]{"14", "14", "14", "14", "14", "14", "14"};
   public static final int EVEN_ROW_BACKGROUND = 0;
   public static final int EVEN_ROW_FOREGROUND = 1;
   public static final int ODD_ROW_BACKGROUND = 2;
   public static final int ODD_ROW_FOREGROUND = 3;
   public static final int TEXTSEGMENT_HIGHLIGHT_BACKGROUND = 4;
   public static final int TEXTSEGMENT_HIGHLIGHT_FOREGROUND = 5;
   public static final int TEXTSEGMENT_DELAYSLOT_HIGHLIGHT_BACKGROUND = 6;
   public static final int TEXTSEGMENT_DELAYSLOT_HIGHLIGHT_FOREGROUND = 7;
   public static final int DATASEGMENT_HIGHLIGHT_BACKGROUND = 8;
   public static final int DATASEGMENT_HIGHLIGHT_FOREGROUND = 9;
   public static final int REGISTER_HIGHLIGHT_BACKGROUND = 10;
   public static final int REGISTER_HIGHLIGHT_FOREGROUND = 11;
   private static final String[] colorSettingsKeys = new String[]{"EvenRowBackground", "EvenRowForeground", "OddRowBackground", "OddRowForeground", "TextSegmentHighlightBackground", "TextSegmentHighlightForeground", "TextSegmentDelaySlotHighlightBackground", "TextSegmentDelaySlotHighlightForeground", "DataSegmentHighlightBackground", "DataSegmentHighlightForeground", "RegisterHighlightBackground", "RegisterHighlightForeground"};
   private static String[] defaultColorSettingsValues = new String[]{"0x00e0e0e0", "0", "0x00ffffff", "0", "0x00ffff99", "0", "0x0033ff00", "0", "0x0099ccff", "0", "0x0099cc55", "0"};
   private static String[] defaultDarkThemeColorSettingsValues = new String[]{"0xf0f0f0", "0xf0f0f0", "0xf0f0f0", "0xf0f0f0", "0xf0f0f0", "0xf0f0f0", "0xf0f0f0", "0xf0f0f0", "0xf0f0f0", "0xf0f0f0", "0xf0f0f0", "0xf0f0f0"};
   private boolean[] booleanSettingsValues;
   private String[] stringSettingsValues;
   private String[] fontFamilySettingsValues;
   private String[] fontStyleSettingsValues;
   private String[] fontSizeSettingsValues;
   private String[] colorSettingsValues;
   private Preferences preferences;
   private String[] syntaxStyleColorSettingsValues;
   private boolean[] syntaxStyleBoldSettingsValues;
   private boolean[] syntaxStyleItalicSettingsValues;
   private static final String SYNTAX_STYLE_COLOR_PREFIX = "SyntaxStyleColor_";
   private static final String SYNTAX_STYLE_BOLD_PREFIX = "SyntaxStyleBold_";
   private static final String SYNTAX_STYLE_ITALIC_PREFIX = "SyntaxStyleItalic_";
   private static String[] syntaxStyleColorSettingsKeys;
   private static String[] syntaxStyleBoldSettingsKeys;
   private static String[] syntaxStyleItalicSettingsKeys;
   private static String[] defaultSyntaxStyleColorSettingsValues;
   private static boolean[] defaultSyntaxStyleBoldSettingsValues;
   private static boolean[] defaultSyntaxStyleItalicSettingsValues;

   public Settings() {
      this(true);
   }

   public Settings(boolean gui) {
      this.booleanSettingsValues = new boolean[booleanSettingsKeys.length];
      this.stringSettingsValues = new String[stringSettingsKeys.length];
      this.fontFamilySettingsValues = new String[fontFamilySettingsKeys.length];
      this.fontStyleSettingsValues = new String[fontStyleSettingsKeys.length];
      this.fontSizeSettingsValues = new String[fontSizeSettingsKeys.length];
      this.colorSettingsValues = new String[colorSettingsKeys.length];
      this.preferences = Preferences.userNodeForPackage(this.getClass());
      this.initialize();
   }

   public boolean getBackSteppingEnabled() {
      return Globals.program != null && Globals.program.getBackStepper() != null && Globals.program.getBackStepper().enabled();
   }

   public void reset(boolean gui) {
      this.initialize();
   }

   public void setEditorSyntaxStyleByPosition(int index, SyntaxStyle syntaxStyle) {
      this.syntaxStyleColorSettingsValues[index] = syntaxStyle.getColorAsHexString();
      this.syntaxStyleItalicSettingsValues[index] = syntaxStyle.isItalic();
      this.syntaxStyleBoldSettingsValues[index] = syntaxStyle.isBold();
      this.saveEditorSyntaxStyle(index);
   }

   public SyntaxStyle getEditorSyntaxStyleByPosition(int index) {
      return new SyntaxStyle(this.getColorValueByPosition(index, this.syntaxStyleColorSettingsValues), this.syntaxStyleItalicSettingsValues[index], this.syntaxStyleBoldSettingsValues[index]);
   }

   public SyntaxStyle getDefaultEditorSyntaxStyleByPosition(int index) {
      return new SyntaxStyle(this.getColorValueByPosition(index, defaultSyntaxStyleColorSettingsValues), defaultSyntaxStyleItalicSettingsValues[index], defaultSyntaxStyleBoldSettingsValues[index]);
   }

   private void saveEditorSyntaxStyle(int index) {
      try {
         this.preferences.put(syntaxStyleColorSettingsKeys[index], this.syntaxStyleColorSettingsValues[index]);
         this.preferences.putBoolean(syntaxStyleBoldSettingsKeys[index], this.syntaxStyleBoldSettingsValues[index]);
         this.preferences.putBoolean(syntaxStyleItalicSettingsKeys[index], this.syntaxStyleItalicSettingsValues[index]);
         this.preferences.flush();
      } catch (SecurityException var3) {
      } catch (BackingStoreException var4) {
      }

   }

   private void initializeEditorSyntaxStyles() {
      SyntaxStyle[] syntaxStyle = SyntaxUtilities.getDefaultSyntaxStyles(preferences.getBoolean("DarkThemeEnabled", false));
      int tokens = syntaxStyle.length;
      syntaxStyleColorSettingsKeys = new String[tokens];
      syntaxStyleBoldSettingsKeys = new String[tokens];
      syntaxStyleItalicSettingsKeys = new String[tokens];
      defaultSyntaxStyleColorSettingsValues = new String[tokens];
      defaultSyntaxStyleBoldSettingsValues = new boolean[tokens];
      defaultSyntaxStyleItalicSettingsValues = new boolean[tokens];
      this.syntaxStyleColorSettingsValues = new String[tokens];
      this.syntaxStyleBoldSettingsValues = new boolean[tokens];
      this.syntaxStyleItalicSettingsValues = new boolean[tokens];

      for(int i = 0; i < tokens; ++i) {
         syntaxStyleColorSettingsKeys[i] = "SyntaxStyleColor_" + i;
         syntaxStyleBoldSettingsKeys[i] = "SyntaxStyleBold_" + i;
         syntaxStyleItalicSettingsKeys[i] = "SyntaxStyleItalic_" + i;
         this.syntaxStyleColorSettingsValues[i] = defaultSyntaxStyleColorSettingsValues[i] = syntaxStyle[i].getColorAsHexString();
         this.syntaxStyleBoldSettingsValues[i] = defaultSyntaxStyleBoldSettingsValues[i] = syntaxStyle[i].isBold();
         this.syntaxStyleItalicSettingsValues[i] = defaultSyntaxStyleItalicSettingsValues[i] = syntaxStyle[i].isItalic();
      }

   }

   private void getEditorSyntaxStyleSettingsFromPreferences() {
      for(int i = 0; i < syntaxStyleColorSettingsKeys.length; ++i) {
         this.syntaxStyleColorSettingsValues[i] = this.preferences.get(syntaxStyleColorSettingsKeys[i], this.syntaxStyleColorSettingsValues[i]);
         this.syntaxStyleBoldSettingsValues[i] = this.preferences.getBoolean(syntaxStyleBoldSettingsKeys[i], this.syntaxStyleBoldSettingsValues[i]);
         this.syntaxStyleItalicSettingsValues[i] = this.preferences.getBoolean(syntaxStyleItalicSettingsKeys[i], this.syntaxStyleItalicSettingsValues[i]);
      }

   }

   public boolean getBooleanSetting(int id) {
      if (id >= 0 && id < this.booleanSettingsValues.length) {
         return this.booleanSettingsValues[id];
      } else {
         throw new IllegalArgumentException("Invalid boolean setting ID");
      }
   }

   /** @deprecated */
   public boolean getBareMachineEnabled() {
      return this.booleanSettingsValues[1];
   }

   /** @deprecated */
   public boolean getExtendedAssemblerEnabled() {
      return this.booleanSettingsValues[0];
   }

   /** @deprecated */
   public boolean getAssembleOnOpenEnabled() {
      return this.booleanSettingsValues[2];
   }

   /** @deprecated */
   public boolean getDisplayAddressesInHex() {
      return this.booleanSettingsValues[5];
   }

   /** @deprecated */
   public boolean getDisplayValuesInHex() {
      return this.booleanSettingsValues[6];
   }

   /** @deprecated */
   public boolean getAssembleAllEnabled() {
      return this.booleanSettingsValues[3];
   }

   /** @deprecated */
   public boolean getExceptionHandlerEnabled() {
      return this.booleanSettingsValues[7];
   }

   /** @deprecated */
   public boolean getDelayedBranchingEnabled() {
      return this.booleanSettingsValues[8];
   }

   /** @deprecated */
   public boolean getLabelWindowVisibility() {
      return this.booleanSettingsValues[4];
   }

   /** @deprecated */
   public boolean getEditorLineNumbersDisplayed() {
      return this.booleanSettingsValues[9];
   }

   /** @deprecated */
   public boolean getWarningsAreErrors() {
      return this.booleanSettingsValues[10];
   }

   /** @deprecated */
   public boolean getProgramArguments() {
      return this.booleanSettingsValues[11];
   }

   /** @deprecated */
   public boolean getDataSegmentHighlighting() {
      return this.booleanSettingsValues[12];
   }

   /** @deprecated */
   public boolean getRegistersHighlighting() {
      return this.booleanSettingsValues[13];
   }

   /** @deprecated */
   public boolean getStartAtMain() {
      return this.booleanSettingsValues[14];
   }

   public String getExceptionHandler() {
      return this.stringSettingsValues[0];
   }

   public String getMemoryConfiguration() {
      return this.stringSettingsValues[3];
   }

   public Font getEditorFont() {
      return this.getFontByPosition(0);
   }

   public Font getFontByPosition(int fontSettingPosition) {
      return fontSettingPosition >= 0 && fontSettingPosition < this.fontFamilySettingsValues.length ? EditorFont.createFontFromStringValues(this.fontFamilySettingsValues[fontSettingPosition], this.fontStyleSettingsValues[fontSettingPosition], this.fontSizeSettingsValues[fontSettingPosition]) : null;
   }

   public Font getDefaultFontByPosition(int fontSettingPosition) {
      return fontSettingPosition >= 0 && fontSettingPosition < defaultFontFamilySettingsValues.length ? EditorFont.createFontFromStringValues(defaultFontFamilySettingsValues[fontSettingPosition], defaultFontStyleSettingsValues[fontSettingPosition], defaultFontSizeSettingsValues[fontSettingPosition]) : null;
   }

   public int[] getTextColumnOrder() {
      return this.getTextSegmentColumnOrder(this.stringSettingsValues[1]);
   }

   public int getCaretBlinkRate() {
      int rate;
      try {
         rate = Integer.parseInt(this.stringSettingsValues[4]);
      } catch (NumberFormatException var3) {
         rate = Integer.parseInt(defaultStringSettingsValues[4]);
      }

      return rate;
   }

   public int getEditorTabSize() {
      int size;
      try {
         size = Integer.parseInt(this.stringSettingsValues[5]);
      } catch (NumberFormatException var3) {
         size = this.getDefaultEditorTabSize();
      }

      return size;
   }

   public int getEditorPopupPrefixLength() {
      int length = 2;

      try {
         length = Integer.parseInt(this.stringSettingsValues[6]);
      } catch (NumberFormatException var3) {
      }

      return length;
   }

   public int getDefaultEditorTabSize() {
      return Integer.parseInt(defaultStringSettingsValues[5]);
   }

   public String getLabelSortState() {
      return this.stringSettingsValues[2];
   }

   public Color getColorSettingByKey(String key) {
      return this.getColorValueByKey(key, this.colorSettingsValues);
   }

   public Color getDefaultColorSettingByKey(String key) {
      return this.getColorValueByKey(key, defaultColorSettingsValues);
   }

   public Color getColorSettingByPosition(int position) {
      return this.getColorValueByPosition(position, this.colorSettingsValues);
   }

   public Color getDefaultColorSettingByPosition(int position) {
      return this.getColorValueByPosition(position, defaultColorSettingsValues);
   }

   public void setBooleanSetting(int id, boolean value) {
      if (id >= 0 && id < this.booleanSettingsValues.length) {
         this.internalSetBooleanSetting(id, value);
      } else {
         throw new IllegalArgumentException("Invalid boolean setting ID");
      }
   }

   /** @deprecated */
   public void setExtendedAssemblerEnabled(boolean value) {
      this.internalSetBooleanSetting(0, value);
   }

   /** @deprecated */
   public void setAssembleOnOpenEnabled(boolean value) {
      this.internalSetBooleanSetting(2, value);
   }

   /** @deprecated */
   public void setAssembleAllEnabled(boolean value) {
      this.internalSetBooleanSetting(3, value);
   }

   /** @deprecated */
   public void setDisplayAddressesInHex(boolean value) {
      this.internalSetBooleanSetting(5, value);
   }

   /** @deprecated */
   public void setDisplayValuesInHex(boolean value) {
      this.internalSetBooleanSetting(6, value);
   }

   /** @deprecated */
   public void setLabelWindowVisibility(boolean value) {
      this.internalSetBooleanSetting(4, value);
   }

   /** @deprecated */
   public void setExceptionHandlerEnabled(boolean value) {
      this.internalSetBooleanSetting(7, value);
   }

   /** @deprecated */
   public void setDelayedBranchingEnabled(boolean value) {
      this.internalSetBooleanSetting(8, value);
   }

   /** @deprecated */
   public void setEditorLineNumbersDisplayed(boolean value) {
      this.internalSetBooleanSetting(9, value);
   }

   /** @deprecated */
   public void setWarningsAreErrors(boolean value) {
      this.internalSetBooleanSetting(10, value);
   }

   /** @deprecated */
   public void setProgramArguments(boolean value) {
      this.internalSetBooleanSetting(11, value);
   }

   /** @deprecated */
   public void setDataSegmentHighlighting(boolean value) {
      this.internalSetBooleanSetting(12, value);
   }

   /** @deprecated */
   public void setRegistersHighlighting(boolean value) {
      this.internalSetBooleanSetting(13, value);
   }

   /** @deprecated */
   public void setStartAtMain(boolean value) {
      this.internalSetBooleanSetting(14, value);
   }

   public void setBooleanSettingNonPersistent(int id, boolean value) {
      if (id >= 0 && id < this.booleanSettingsValues.length) {
         this.booleanSettingsValues[id] = value;
      } else {
         throw new IllegalArgumentException("Invalid boolean setting ID");
      }
   }

   /** @deprecated */
   public void setDelayedBranchingEnabledNonPersistent(boolean value) {
      this.booleanSettingsValues[8] = value;
   }

   public void setExceptionHandler(String newFilename) {
      this.setStringSetting(0, newFilename);
   }

   public void setMemoryConfiguration(String config) {
      this.setStringSetting(3, config);
   }

   public void setCaretBlinkRate(int rate) {
      this.setStringSetting(4, "" + rate);
   }

   public void setEditorTabSize(int size) {
      this.setStringSetting(5, "" + size);
   }

   public void setEditorPopupPrefixLength(int length) {
      this.setStringSetting(6, "" + length);
   }

   public void setEditorFont(Font font) {
      this.setFontByPosition(0, font);
   }

   public void setFontByPosition(int fontSettingPosition, Font font) {
      if (fontSettingPosition >= 0 && fontSettingPosition < this.fontFamilySettingsValues.length) {
         this.fontFamilySettingsValues[fontSettingPosition] = font.getFamily();
         this.fontStyleSettingsValues[fontSettingPosition] = EditorFont.styleIntToStyleString(font.getStyle());
         this.fontSizeSettingsValues[fontSettingPosition] = EditorFont.sizeIntToSizeString(font.getSize());
         this.saveFontSetting(fontSettingPosition, fontFamilySettingsKeys, this.fontFamilySettingsValues);
         this.saveFontSetting(fontSettingPosition, fontStyleSettingsKeys, this.fontStyleSettingsValues);
         this.saveFontSetting(fontSettingPosition, fontSizeSettingsKeys, this.fontSizeSettingsValues);
      }

      if (fontSettingPosition == 0) {
         this.setChanged();
         this.notifyObservers();
      }

   }

   public void setTextColumnOrder(int[] columnOrder) {
      String stringifiedOrder = new String();

      for(int i = 0; i < columnOrder.length; ++i) {
         stringifiedOrder = stringifiedOrder + Integer.toString(columnOrder[i]) + " ";
      }

      this.setStringSetting(1, stringifiedOrder);
   }

   public void setLabelSortState(String state) {
      this.setStringSetting(2, state);
   }

   public void setColorSettingByKey(String key, Color color) {
      for(int i = 0; i < colorSettingsKeys.length; ++i) {
         if (key.equals(colorSettingsKeys[i])) {
            this.setColorSettingByPosition(i, color);
            return;
         }
      }

   }

   public void setColorSettingByPosition(int position, Color color) {
      if (position >= 0 && position < colorSettingsKeys.length) {
         this.setColorSetting(position, color);
      }

   }

   private void initialize() {
      this.applyDefaultSettings();
      if (!this.readSettingsFromPropertiesFile(settingsFile)) {
         System.out.println("MARS System error: unable to read Settings.properties defaults. Using built-in defaults.");
      }
      this.getSettingsFromPreferences();
   }

   private void applyDefaultSettings() {
      int i;
      for(i = 0; i < this.booleanSettingsValues.length; ++i) {
         this.booleanSettingsValues[i] = defaultBooleanSettingsValues[i];
      }

      for(i = 0; i < this.stringSettingsValues.length; ++i) {
         this.stringSettingsValues[i] = defaultStringSettingsValues[i];
      }

      for(i = 0; i < this.fontFamilySettingsValues.length; ++i) {
         this.fontFamilySettingsValues[i] = defaultFontFamilySettingsValues[i];
         this.fontStyleSettingsValues[i] = defaultFontStyleSettingsValues[i];
         this.fontSizeSettingsValues[i] = defaultFontSizeSettingsValues[i];
      }
      if(booleanSettingsValues[DARK_THEME_ENABLED]) {
         for (i = 0; i < this.colorSettingsValues.length; ++i) {
            this.colorSettingsValues[i] = defaultDarkThemeColorSettingsValues[i];
         }
      }else {
         for (i = 0; i < this.colorSettingsValues.length; ++i) {
            this.colorSettingsValues[i] = defaultColorSettingsValues[i];
         }
      }
      this.initializeEditorSyntaxStyles();
   }

   private void internalSetBooleanSetting(int settingIndex, boolean value) {
      if (value != this.booleanSettingsValues[settingIndex]) {
         this.booleanSettingsValues[settingIndex] = value;
         this.saveBooleanSetting(settingIndex);
         this.setChanged();
         this.notifyObservers();
      }

   }

   private void setStringSetting(int settingIndex, String value) {
      this.stringSettingsValues[settingIndex] = value;
      this.saveStringSetting(settingIndex);
   }

   private void setColorSetting(int settingIndex, Color color) {
      this.colorSettingsValues[settingIndex] = Binary.intToHexString(color.getRed() << 16 | color.getGreen() << 8 | color.getBlue());
      this.saveColorSetting(settingIndex);
   }

   private Color getColorValueByKey(String key, String[] values) {
      Color color = null;

      for(int i = 0; i < colorSettingsKeys.length; ++i) {
         if (key.equals(colorSettingsKeys[i])) {
            return this.getColorValueByPosition(i, values);
         }
      }

      return null;
   }

   private Color getColorValueByPosition(int position, String[] values) {
      Color color = null;
      if (position >= 0 && position < colorSettingsKeys.length) {
         try {
            color = Color.decode(values[position]);
         } catch (NumberFormatException var5) {
            color = null;
         }
      }

      return color;
   }

   private int getIndexOfKey(String key, String[] array) {
      int index = -1;

      for(int i = 0; i < array.length; ++i) {
         if (array[i].equals(key)) {
            index = i;
            break;
         }
      }

      return index;
   }

   private boolean readSettingsFromPropertiesFile(String filename) {
      try {
         String settingValue;
         int i;
         for(i = 0; i < booleanSettingsKeys.length; ++i) {
            settingValue = Globals.getPropertyEntry(filename, booleanSettingsKeys[i]);
            if (settingValue != null) {
               this.booleanSettingsValues[i] = defaultBooleanSettingsValues[i] = Boolean.valueOf(settingValue);
            }
         }
         this.booleanSettingsValues[21] = preferences.getBoolean("DarkThemeEnabled", false);
         for(i = 0; i < stringSettingsKeys.length; ++i) {
            settingValue = Globals.getPropertyEntry(filename, stringSettingsKeys[i]);
            if (settingValue != null) {
               this.stringSettingsValues[i] = defaultStringSettingsValues[i] = settingValue;
            }
         }

         for(i = 0; i < this.fontFamilySettingsValues.length; ++i) {
            settingValue = Globals.getPropertyEntry(filename, fontFamilySettingsKeys[i]);
            if (settingValue != null) {
               this.fontFamilySettingsValues[i] = defaultFontFamilySettingsValues[i] = settingValue;
            }

            settingValue = Globals.getPropertyEntry(filename, fontStyleSettingsKeys[i]);
            if (settingValue != null) {
               this.fontStyleSettingsValues[i] = defaultFontStyleSettingsValues[i] = settingValue;
            }

            settingValue = Globals.getPropertyEntry(filename, fontSizeSettingsKeys[i]);
            if (settingValue != null) {
               this.fontSizeSettingsValues[i] = defaultFontSizeSettingsValues[i] = settingValue;
            }
         }
         if(booleanSettingsValues[DARK_THEME_ENABLED]) {
            for (i = 0; i < colorSettingsKeys.length; ++i) {
               settingValue = Globals.getPropertyEntry(filename, "DarkTheme"+colorSettingsKeys[i]);
               if (settingValue != null) {
                  this.colorSettingsValues[i]  = settingValue;
               }
               else {
                  this.colorSettingsValues[i] = defaultDarkThemeColorSettingsValues[i];
               }
            }
         }
         else {
            for (i = 0; i < colorSettingsKeys.length; ++i) {
               settingValue = Globals.getPropertyEntry(filename, colorSettingsKeys[i]);
               if (settingValue != null) {
                  this.colorSettingsValues[i] = settingValue;
               }
               else {
                  this.colorSettingsValues[i] = defaultColorSettingsValues[i];
               }
            }
         }

         return true;
      } catch (Exception var4) {
         return false;
      }
   }

   private void getSettingsFromPreferences() {
      int i;
      try {
      } catch (Exception ignored) {}
      for(i = 0; i < booleanSettingsKeys.length; ++i) {
         this.booleanSettingsValues[i] = this.preferences.getBoolean(booleanSettingsKeys[i], this.booleanSettingsValues[i]);
      }

      for(i = 0; i < stringSettingsKeys.length; ++i) {
         this.stringSettingsValues[i] = this.preferences.get(stringSettingsKeys[i], this.stringSettingsValues[i]);
      }

      for(i = 0; i < fontFamilySettingsKeys.length; ++i) {
         this.fontFamilySettingsValues[i] = this.preferences.get(fontFamilySettingsKeys[i], this.fontFamilySettingsValues[i]);
         this.fontStyleSettingsValues[i] = this.preferences.get(fontStyleSettingsKeys[i], this.fontStyleSettingsValues[i]);
         this.fontSizeSettingsValues[i] = this.preferences.get(fontSizeSettingsKeys[i], this.fontSizeSettingsValues[i]);
      }
      if(this.booleanSettingsValues[DARK_THEME_ENABLED]) {
         for(i = 0; i < colorSettingsKeys.length; ++i) {
            this.colorSettingsValues[i] = this.preferences.get(("DarkTheme" + colorSettingsKeys[i]), this.colorSettingsValues[i]);
         }
      }
      else {
         for (i = 0; i < colorSettingsKeys.length; ++i) {
            this.colorSettingsValues[i] = this.preferences.get(colorSettingsKeys[i], this.colorSettingsValues[i]);
         }
      }
      this.getEditorSyntaxStyleSettingsFromPreferences();
   }

   private void saveBooleanSetting(int index) {
      try {
         this.preferences.putBoolean(booleanSettingsKeys[index], this.booleanSettingsValues[index]);
         this.preferences.flush();
      } catch (SecurityException var3) {
      } catch (BackingStoreException var4) {
      }

   }

   private void saveStringSetting(int index) {
      try {
         this.preferences.put(stringSettingsKeys[index], this.stringSettingsValues[index]);
         this.preferences.flush();
      } catch (SecurityException var3) {
      } catch (BackingStoreException var4) {
      }

   }

   private void saveFontSetting(int index, String[] settingsKeys, String[] settingsValues) {
      try {
         this.preferences.put(settingsKeys[index], settingsValues[index]);
         this.preferences.flush();
      } catch (SecurityException var5) {
      } catch (BackingStoreException var6) {
      }

   }

   private void saveColorSetting(int index) {
      try {
         this.preferences.put(colorSettingsKeys[index], this.colorSettingsValues[index]);
         this.preferences.flush();
      } catch (SecurityException var3) {
      } catch (BackingStoreException var4) {
      }

   }

   private int[] getTextSegmentColumnOrder(String stringOfColumnIndexes) {
      StringTokenizer st = new StringTokenizer(stringOfColumnIndexes);
      int[] list = new int[st.countTokens()];
      int index = 0;

      int value;
      boolean valuesOK;
      for(valuesOK = true; st.hasMoreTokens(); list[index++] = value) {
         try {
            value = Integer.parseInt(st.nextToken());
         } catch (Exception var8) {
            valuesOK = false;
            break;
         }
      }

      return !valuesOK && !stringOfColumnIndexes.equals(defaultStringSettingsValues[1]) ? this.getTextSegmentColumnOrder(defaultStringSettingsValues[1]) : list;
   }
}
