package mars.util;

import java.util.Arrays;
import mars.Globals;

public class Binary {
   private static char[] chars = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
   private static final long UNSIGNED_BASE = 4294967296L;

   public static String intToBinaryString(int value, int length) {
      char[] result = new char[length];
      int index = length - 1;

      for(int i = 0; i < length; ++i) {
         result[index] = (char)(bitValue(value, i) == 1 ? 49 : 48);
         --index;
      }

      return new String(result);
   }

   public static String intToBinaryString(int value) {
      return intToBinaryString(value, 32);
   }

   public static String longToBinaryString(long value, int length) {
      char[] result = new char[length];
      int index = length - 1;

      for(int i = 0; i < length; ++i) {
         result[index] = (char)(bitValue(value, i) == 1 ? 49 : 48);
         --index;
      }

      return new String(result);
   }

   public static String longToBinaryString(long value) {
      return longToBinaryString(value, 64);
   }

   public static int binaryStringToInt(String value) {
      int result = value.charAt(0) - 48;

      for(int i = 1; i < value.length(); ++i) {
         result = result << 1 | value.charAt(i) - 48;
      }

      return result;
   }

   public static long binaryStringToLong(String value) {
      long result = (long)(value.charAt(0) - 48);

      for(int i = 1; i < value.length(); ++i) {
         result = result << 1 | (long)(value.charAt(i) - 48);
      }

      return result;
   }

   public static String binaryStringToHexString(String value) {
      int digits = (value.length() + 3) / 4;
      char[] hexChars = new char[digits + 2];
      hexChars[0] = '0';
      hexChars[1] = 'x';
      int position = value.length() - 1;

      for(int digs = 0; digs < digits; ++digs) {
         int result = 0;
         int pow = 1;

         for(int rep = 0; rep < 4 && position >= 0; ++rep) {
            if (value.charAt(position) == '1') {
               result += pow;
            }

            pow *= 2;
            --position;
         }

         hexChars[digits - digs + 1] = chars[result];
      }

      return new String(hexChars);
   }

   public static String hexStringToBinaryString(String value) {
      String result = "";
      if (value.indexOf("0x") == 0 || value.indexOf("0X") == 0) {
         value = value.substring(2);
      }

      for(int digs = 0; digs < value.length(); ++digs) {
         switch (value.charAt(digs)) {
            case '0':
               result = result + "0000";
               break;
            case '1':
               result = result + "0001";
               break;
            case '2':
               result = result + "0010";
               break;
            case '3':
               result = result + "0011";
               break;
            case '4':
               result = result + "0100";
               break;
            case '5':
               result = result + "0101";
               break;
            case '6':
               result = result + "0110";
               break;
            case '7':
               result = result + "0111";
               break;
            case '8':
               result = result + "1000";
               break;
            case '9':
               result = result + "1001";
            case ':':
            case ';':
            case '<':
            case '=':
            case '>':
            case '?':
            case '@':
            case 'G':
            case 'H':
            case 'I':
            case 'J':
            case 'K':
            case 'L':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
            case '[':
            case '\\':
            case ']':
            case '^':
            case '_':
            case '`':
            default:
               break;
            case 'A':
            case 'a':
               result = result + "1010";
               break;
            case 'B':
            case 'b':
               result = result + "1011";
               break;
            case 'C':
            case 'c':
               result = result + "1100";
               break;
            case 'D':
            case 'd':
               result = result + "1101";
               break;
            case 'E':
            case 'e':
               result = result + "1110";
               break;
            case 'F':
            case 'f':
               result = result + "1111";
         }
      }

      return result;
   }

   public static char binaryStringToHexDigit(String value) {
      if (value.length() > 4) {
         return '0';
      } else {
         int result = 0;
         int pow = 1;

         for(int i = value.length() - 1; i >= 0; --i) {
            if (value.charAt(i) == '1') {
               result += pow;
            }

            pow *= 2;
         }

         return chars[result];
      }
   }

   public static String intToHexString(int d) {
      String leadingZero = new String("0");
      String leadingX = new String("0x");

      String t;
      for(t = Integer.toHexString(d); t.length() < 8; t = leadingZero.concat(t)) {
      }

      t = leadingX.concat(t);
      return t;
   }

   public static String intToHalfHexString(int d) {
      String leadingZero = new String("0");
      String leadingX = new String("0x");
      String t = Integer.toHexString(d);
      if (t.length() > 4) {
         t = t.substring(t.length() - 4, t.length());
      }

      while(t.length() < 4) {
         t = leadingZero.concat(t);
      }

      t = leadingX.concat(t);
      return t;
   }

   public static String longToHexString(long value) {
      return binaryStringToHexString(longToBinaryString(value));
   }

   public static String unsignedIntToIntString(int d) {
      return d >= 0 ? Integer.toString(d) : Long.toString(4294967296L + (long)d);
   }

   public static String intToAscii(int d) {
      StringBuilder result = new StringBuilder(8);

      for(int i = 3; i >= 0; --i) {
         int byteValue = getByte(d, i);
         result.append(byteValue < Globals.ASCII_TABLE.length ? Globals.ASCII_TABLE[byteValue] : Globals.ASCII_NON_PRINT);
      }

      return result.toString();
   }

   public static int stringToInt(String s) throws NumberFormatException {
      String work = new String(s);
      int result;
      try {
         result = Integer.decode(s);
      } catch (NumberFormatException var7) {
         work = work.toLowerCase();
         int c;
         if (work.length() == 10 && work.startsWith("0x")) {
            String bitString = "";

            for(int i = 2; i < 10; ++i) {
               c = Arrays.binarySearch(chars, work.charAt(i));
               if (c < 0) {
                  throw new NumberFormatException();
               }

               bitString = bitString + intToBinaryString(c, 4);
            }

            result = binaryStringToInt(bitString);
         } else {
            if (work.startsWith("0x")) {
               throw new NumberFormatException();
            }

            result = 0;

            for(int i = 0; i < work.length(); ++i) {
               c = work.charAt(i);
               if (48 > c || c > 57) {
                  throw new NumberFormatException();
               }

               result *= 10;
               result += c - 48;
            }
         }
      }

      return result;
   }

   public static long stringToLong(String s) throws NumberFormatException {
      String work = new String(s);
      long result = 0L;

      try {
         result = Long.decode(s);
      } catch (NumberFormatException var8) {
         work = work.toLowerCase();
         if (work.length() != 18 || !work.startsWith("0x")) {
            throw new NumberFormatException();
         }

         String bitString = "";

         for(int i = 2; i < 18; ++i) {
            int index = Arrays.binarySearch(chars, work.charAt(i));
            if (index < 0) {
               throw new NumberFormatException();
            }

            bitString = bitString + intToBinaryString(index, 4);
         }

         result = binaryStringToLong(bitString);
      }

      return result;
   }

   public static int highOrderLongToInt(long longValue) {
      return (int)(longValue >> 32);
   }

   public static int lowOrderLongToInt(long longValue) {
      return (int)(longValue << 32 >> 32);
   }

   public static long twoIntsToLong(int highOrder, int lowOrder) {
      return (long)highOrder << 32 | (long)lowOrder & 4294967295L;
   }

   public static int bitValue(int value, int bit) {
      return 1 & value >> bit;
   }

   public static int bitValue(long value, int bit) {
      return (int)(1L & value >> bit);
   }

   public static int setBit(int value, int bit) {
      return value | 1 << bit;
   }

   public static int clearBit(int value, int bit) {
      return value & ~(1 << bit);
   }

   public static int setByte(int value, int bite, int replace) {
      return value & ~(255 << (bite << 3)) | (replace & 255) << (bite << 3);
   }

   public static int getByte(int value, int bite) {
      return value << (3 - bite << 3) >>> 24;
   }

   public static boolean isHex(String v) {
      try {
         try {
            stringToInt(v);
         } catch (NumberFormatException var4) {
            try {
               stringToLong(v);
            } catch (NumberFormatException var3) {
               return false;
            }
         }

         if (v.charAt(0) == '-' && v.charAt(1) == '0' && Character.toUpperCase(v.charAt(1)) == 'X') {
            return true;
         } else {
            return v.charAt(0) == '0' && Character.toUpperCase(v.charAt(1)) == 'X';
         }
      } catch (StringIndexOutOfBoundsException var5) {
         return false;
      }
   }

   public static boolean isOctal(String v) {
      try {
         int dontCare = stringToInt(v);
         if (isHex(v)) {
            return false;
         } else if (v.charAt(0) == '-' && v.charAt(1) == '0' && v.length() > 1) {
            return true;
         } else {
            return v.charAt(0) == '0' && v.length() > 1;
         }
      } catch (StringIndexOutOfBoundsException var2) {
         return false;
      } catch (NumberFormatException var3) {
         return false;
      }
   }
}
