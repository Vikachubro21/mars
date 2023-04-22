package mars.mips.hardware;

import java.util.Collection;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;
import mars.Globals;
import mars.ProgramStatement;
import mars.util.Binary;

public class Memory extends Observable {
   public static int textBaseAddress = MemoryConfigurations.getDefaultTextBaseAddress();
   public static int dataSegmentBaseAddress = MemoryConfigurations.getDefaultDataSegmentBaseAddress();
   public static int externBaseAddress = MemoryConfigurations.getDefaultExternBaseAddress();
   public static int globalPointer = MemoryConfigurations.getDefaultGlobalPointer();
   public static int dataBaseAddress = MemoryConfigurations.getDefaultDataBaseAddress();
   public static int heapBaseAddress = MemoryConfigurations.getDefaultHeapBaseAddress();
   public static int stackPointer = MemoryConfigurations.getDefaultStackPointer();
   public static int stackBaseAddress = MemoryConfigurations.getDefaultStackBaseAddress();
   public static int userHighAddress = MemoryConfigurations.getDefaultUserHighAddress();
   public static int kernelBaseAddress = MemoryConfigurations.getDefaultKernelBaseAddress();
   public static int kernelTextBaseAddress = MemoryConfigurations.getDefaultKernelTextBaseAddress();
   public static int exceptionHandlerAddress = MemoryConfigurations.getDefaultExceptionHandlerAddress();
   public static int kernelDataBaseAddress = MemoryConfigurations.getDefaultKernelDataBaseAddress();
   public static int memoryMapBaseAddress = MemoryConfigurations.getDefaultMemoryMapBaseAddress();
   public static int kernelHighAddress = MemoryConfigurations.getDefaultKernelHighAddress();
   public static final int WORD_LENGTH_BYTES = 4;
   public static final boolean LITTLE_ENDIAN = true;
   public static final boolean BIG_ENDIAN = false;
   private static boolean byteOrder = true;
   public static int heapAddress;
   Collection observables = this.getNewMemoryObserversCollection();
   private static final int BLOCK_LENGTH_WORDS = 1024;
   private static final int BLOCK_TABLE_LENGTH = 1024;
   private int[][] dataBlockTable;
   private int[][] kernelDataBlockTable;
   private int[][] stackBlockTable;
   private static final int MMIO_TABLE_LENGTH = 16;
   private int[][] memoryMapBlockTable;
   private static final int TEXT_BLOCK_LENGTH_WORDS = 1024;
   private static final int TEXT_BLOCK_TABLE_LENGTH = 1024;
   private ProgramStatement[][] textBlockTable;
   private ProgramStatement[][] kernelTextBlockTable;
   public static int dataSegmentLimitAddress;
   public static int textLimitAddress;
   public static int kernelDataSegmentLimitAddress;
   public static int kernelTextLimitAddress;
   public static int stackLimitAddress;
   public static int memoryMapLimitAddress;
   private static Memory uniqueMemoryInstance;
   private static final boolean STORE = true;
   private static final boolean FETCH = false;

   private Memory() {
      this.initialize();
   }

   public static Memory getInstance() {
      return uniqueMemoryInstance;
   }

   public void clear() {
      setConfiguration();
      this.initialize();
   }

   public static void setConfiguration() {
      textBaseAddress = MemoryConfigurations.getCurrentConfiguration().getTextBaseAddress();
      dataSegmentBaseAddress = MemoryConfigurations.getCurrentConfiguration().getDataSegmentBaseAddress();
      externBaseAddress = MemoryConfigurations.getCurrentConfiguration().getExternBaseAddress();
      globalPointer = MemoryConfigurations.getCurrentConfiguration().getGlobalPointer();
      dataBaseAddress = MemoryConfigurations.getCurrentConfiguration().getDataBaseAddress();
      heapBaseAddress = MemoryConfigurations.getCurrentConfiguration().getHeapBaseAddress();
      stackPointer = MemoryConfigurations.getCurrentConfiguration().getStackPointer();
      stackBaseAddress = MemoryConfigurations.getCurrentConfiguration().getStackBaseAddress();
      userHighAddress = MemoryConfigurations.getCurrentConfiguration().getUserHighAddress();
      kernelBaseAddress = MemoryConfigurations.getCurrentConfiguration().getKernelBaseAddress();
      kernelTextBaseAddress = MemoryConfigurations.getCurrentConfiguration().getKernelTextBaseAddress();
      exceptionHandlerAddress = MemoryConfigurations.getCurrentConfiguration().getExceptionHandlerAddress();
      kernelDataBaseAddress = MemoryConfigurations.getCurrentConfiguration().getKernelDataBaseAddress();
      memoryMapBaseAddress = MemoryConfigurations.getCurrentConfiguration().getMemoryMapBaseAddress();
      kernelHighAddress = MemoryConfigurations.getCurrentConfiguration().getKernelHighAddress();
      dataSegmentLimitAddress = Math.min(MemoryConfigurations.getCurrentConfiguration().getDataSegmentLimitAddress(), dataSegmentBaseAddress + 4194304);
      textLimitAddress = Math.min(MemoryConfigurations.getCurrentConfiguration().getTextLimitAddress(), textBaseAddress + 4194304);
      kernelDataSegmentLimitAddress = Math.min(MemoryConfigurations.getCurrentConfiguration().getKernelDataSegmentLimitAddress(), kernelDataBaseAddress + 4194304);
      kernelTextLimitAddress = Math.min(MemoryConfigurations.getCurrentConfiguration().getKernelTextLimitAddress(), kernelTextBaseAddress + 4194304);
      stackLimitAddress = Math.max(MemoryConfigurations.getCurrentConfiguration().getStackLimitAddress(), stackBaseAddress - 4194304);
      memoryMapLimitAddress = Math.min(MemoryConfigurations.getCurrentConfiguration().getMemoryMapLimitAddress(), memoryMapBaseAddress + 65536);
   }

   public boolean usingCompactMemoryConfiguration() {
      return (kernelHighAddress & 32767) == kernelHighAddress;
   }

   private void initialize() {
      heapAddress = heapBaseAddress;
      this.textBlockTable = new ProgramStatement[1024][];
      this.dataBlockTable = new int[1024][];
      this.kernelTextBlockTable = new ProgramStatement[1024][];
      this.kernelDataBlockTable = new int[1024][];
      this.stackBlockTable = new int[1024][];
      this.memoryMapBlockTable = new int[16][];
      System.gc();
   }

   public int allocateBytesFromHeap(int numBytes) throws IllegalArgumentException {
      int result = heapAddress;
      if (numBytes < 0) {
         throw new IllegalArgumentException("request (" + numBytes + ") is negative heap amount");
      } else {
         int newHeapAddress = heapAddress + numBytes;
         if (newHeapAddress % 4 != 0) {
            newHeapAddress += 4 - newHeapAddress % 4;
         }

         if (newHeapAddress >= dataSegmentLimitAddress) {
            throw new IllegalArgumentException("request (" + numBytes + ") exceeds available heap storage");
         } else {
            heapAddress = newHeapAddress;
            return result;
         }
      }
   }

   public void setByteOrder(boolean order) {
      byteOrder = order;
   }

   public boolean getByteOrder() {
      return byteOrder;
   }

   public int set(int address, int value, int length) throws AddressErrorException {
      int oldValue = 0;
      if (Globals.debug) {
         System.out.println("memory[" + address + "] set to " + value + "(" + length + " bytes)");
      }

      int relativeByteAddress;
      if (inDataSegment(address)) {
         relativeByteAddress = address - dataSegmentBaseAddress;
         oldValue = this.storeBytesInTable(this.dataBlockTable, relativeByteAddress, length, value);
      } else if (address > stackLimitAddress && address <= stackBaseAddress) {
         relativeByteAddress = stackBaseAddress - address;
         oldValue = this.storeBytesInTable(this.stackBlockTable, relativeByteAddress, length, value);
      } else if (inTextSegment(address)) {
         if (!Globals.getSettings().getBooleanSetting(20)) {
            throw new AddressErrorException("Cannot write directly to text segment!", 5, address);
         }

         ProgramStatement oldStatement = this.getStatementNoNotify(address);
         if (oldStatement != null) {
            oldValue = oldStatement.getBinaryStatement();
         }

         this.setStatement(address, new ProgramStatement(value, address));
      } else if (address >= memoryMapBaseAddress && address < memoryMapLimitAddress) {
         relativeByteAddress = address - memoryMapBaseAddress;
         oldValue = this.storeBytesInTable(this.memoryMapBlockTable, relativeByteAddress, length, value);
      } else {
         if (!inKernelDataSegment(address)) {
            if (inKernelTextSegment(address)) {
               throw new AddressErrorException("DEVELOPER: You must use setStatement() to write to kernel text segment!", 5, address);
            }

            throw new AddressErrorException("address out of range ", 5, address);
         }

         relativeByteAddress = address - kernelDataBaseAddress;
         oldValue = this.storeBytesInTable(this.kernelDataBlockTable, relativeByteAddress, length, value);
      }

      this.notifyAnyObservers(1, address, length, value);
      return oldValue;
   }

   public int setRawWord(int address, int value) throws AddressErrorException {
      int oldValue = 0;
      if (address % 4 != 0) {
         throw new AddressErrorException("store address not aligned on word boundary ", 5, address);
      } else {
         int relative;
         if (inDataSegment(address)) {
            relative = address - dataSegmentBaseAddress >> 2;
            oldValue = this.storeWordInTable(this.dataBlockTable, relative, value);
         } else if (address > stackLimitAddress && address <= stackBaseAddress) {
            relative = stackBaseAddress - address >> 2;
            oldValue = this.storeWordInTable(this.stackBlockTable, relative, value);
         } else if (inTextSegment(address)) {
            if (!Globals.getSettings().getBooleanSetting(20)) {
               throw new AddressErrorException("Cannot write directly to text segment!", 5, address);
            }

            ProgramStatement oldStatement = this.getStatementNoNotify(address);
            if (oldStatement != null) {
               oldValue = oldStatement.getBinaryStatement();
            }

            this.setStatement(address, new ProgramStatement(value, address));
         } else if (address >= memoryMapBaseAddress && address < memoryMapLimitAddress) {
            relative = address - memoryMapBaseAddress >> 2;
            oldValue = this.storeWordInTable(this.memoryMapBlockTable, relative, value);
         } else {
            if (!inKernelDataSegment(address)) {
               if (inKernelTextSegment(address)) {
                  throw new AddressErrorException("DEVELOPER: You must use setStatement() to write to kernel text segment!", 5, address);
               }

               throw new AddressErrorException("store address out of range ", 5, address);
            }

            relative = address - kernelDataBaseAddress >> 2;
            oldValue = this.storeWordInTable(this.kernelDataBlockTable, relative, value);
         }

         this.notifyAnyObservers(1, address, 4, value);
         if (Globals.getSettings().getBackSteppingEnabled()) {
            Globals.program.getBackStepper().addMemoryRestoreRawWord(address, oldValue);
         }

         return oldValue;
      }
   }

   public int setWord(int address, int value) throws AddressErrorException {
      if (address % 4 != 0) {
         throw new AddressErrorException("store address not aligned on word boundary ", 5, address);
      } else {
         return Globals.getSettings().getBackSteppingEnabled() ? Globals.program.getBackStepper().addMemoryRestoreWord(address, this.set(address, value, 4)) : this.set(address, value, 4);
      }
   }

   public int setHalf(int address, int value) throws AddressErrorException {
      if (address % 2 != 0) {
         throw new AddressErrorException("store address not aligned on halfword boundary ", 5, address);
      } else {
         return Globals.getSettings().getBackSteppingEnabled() ? Globals.program.getBackStepper().addMemoryRestoreHalf(address, this.set(address, value, 2)) : this.set(address, value, 2);
      }
   }

   public int setByte(int address, int value) throws AddressErrorException {
      return Globals.getSettings().getBackSteppingEnabled() ? Globals.program.getBackStepper().addMemoryRestoreByte(address, this.set(address, value, 1)) : this.set(address, value, 1);
   }

   public double setDouble(int address, double value) throws AddressErrorException {
      long longValue = Double.doubleToLongBits(value);
      int oldHighOrder = this.set(address + 4, Binary.highOrderLongToInt(longValue), 4);
      int oldLowOrder = this.set(address, Binary.lowOrderLongToInt(longValue), 4);
      return Double.longBitsToDouble(Binary.twoIntsToLong(oldHighOrder, oldLowOrder));
   }

   public void setStatement(int address, ProgramStatement statement) throws AddressErrorException {
      if (address % 4 != 0 || !inTextSegment(address) && !inKernelTextSegment(address)) {
         throw new AddressErrorException("store address to text segment out of range or not aligned to word boundary ", 5, address);
      } else {
         if (Globals.debug) {
            System.out.println("memory[" + address + "] set to " + statement.getBinaryStatement());
         }

         if (inTextSegment(address)) {
            this.storeProgramStatement(address, statement, textBaseAddress, this.textBlockTable);
         } else {
            this.storeProgramStatement(address, statement, kernelTextBaseAddress, this.kernelTextBlockTable);
         }

      }
   }

   public int get(int address, int length) throws AddressErrorException {
      return this.get(address, length, true);
   }

   private int get(int address, int length, boolean notify) throws AddressErrorException {
      int relativeByteAddress;
      int value;
      if (inDataSegment(address)) {
         relativeByteAddress = address - dataSegmentBaseAddress;
         value = this.fetchBytesFromTable(this.dataBlockTable, relativeByteAddress, length);
      } else if (address > stackLimitAddress && address <= stackBaseAddress) {
         relativeByteAddress = stackBaseAddress - address;
         value = this.fetchBytesFromTable(this.stackBlockTable, relativeByteAddress, length);
      } else if (address >= memoryMapBaseAddress && address < memoryMapLimitAddress) {
         relativeByteAddress = address - memoryMapBaseAddress;
         value = this.fetchBytesFromTable(this.memoryMapBlockTable, relativeByteAddress, length);
      } else if (inTextSegment(address)) {
         if (!Globals.getSettings().getBooleanSetting(20)) {
            throw new AddressErrorException("Cannot read directly from text segment!", 4, address);
         }

         ProgramStatement stmt = this.getStatementNoNotify(address);
         value = stmt == null ? 0 : stmt.getBinaryStatement();
      } else {
         if (!inKernelDataSegment(address)) {
            if (inKernelTextSegment(address)) {
               throw new AddressErrorException("DEVELOPER: You must use getStatement() to read from kernel text segment!", 4, address);
            }

            throw new AddressErrorException("address out of range ", 4, address);
         }

         relativeByteAddress = address - kernelDataBaseAddress;
         value = this.fetchBytesFromTable(this.kernelDataBlockTable, relativeByteAddress, length);
      }

      if (notify) {
         this.notifyAnyObservers(0, address, length, value);
      }

      return value;
   }

   public int getRawWord(int address) throws AddressErrorException {
      if (address % 4 != 0) {
         throw new AddressErrorException("address for fetch not aligned on word boundary", 4, address);
      } else {
         int relative;
         int value;
         if (inDataSegment(address)) {
            relative = address - dataSegmentBaseAddress >> 2;
            value = this.fetchWordFromTable(this.dataBlockTable, relative);
         } else if (address > stackLimitAddress && address <= stackBaseAddress) {
            relative = stackBaseAddress - address >> 2;
            value = this.fetchWordFromTable(this.stackBlockTable, relative);
         } else if (address >= memoryMapBaseAddress && address < memoryMapLimitAddress) {
            relative = address - memoryMapBaseAddress >> 2;
            value = this.fetchWordFromTable(this.memoryMapBlockTable, relative);
         } else if (inTextSegment(address)) {
            if (!Globals.getSettings().getBooleanSetting(20)) {
               throw new AddressErrorException("Cannot read directly from text segment!", 4, address);
            }

            ProgramStatement stmt = this.getStatementNoNotify(address);
            value = stmt == null ? 0 : stmt.getBinaryStatement();
         } else {
            if (!inKernelDataSegment(address)) {
               if (inKernelTextSegment(address)) {
                  throw new AddressErrorException("DEVELOPER: You must use getStatement() to read from kernel text segment!", 4, address);
               }

               throw new AddressErrorException("address out of range ", 4, address);
            }

            relative = address - kernelDataBaseAddress >> 2;
            value = this.fetchWordFromTable(this.kernelDataBlockTable, relative);
         }

         this.notifyAnyObservers(0, address, 4, value);
         return value;
      }
   }

   public Integer getRawWordOrNull(int address) throws AddressErrorException {
      Integer value = null;
      if (address % 4 != 0) {
         throw new AddressErrorException("address for fetch not aligned on word boundary", 4, address);
      } else {
         int relative;
         if (inDataSegment(address)) {
            relative = address - dataSegmentBaseAddress >> 2;
            value = this.fetchWordOrNullFromTable(this.dataBlockTable, relative);
         } else if (address > stackLimitAddress && address <= stackBaseAddress) {
            relative = stackBaseAddress - address >> 2;
            value = this.fetchWordOrNullFromTable(this.stackBlockTable, relative);
         } else if (!inTextSegment(address) && !inKernelTextSegment(address)) {
            if (!inKernelDataSegment(address)) {
               throw new AddressErrorException("address out of range ", 4, address);
            }

            relative = address - kernelDataBaseAddress >> 2;
            value = this.fetchWordOrNullFromTable(this.kernelDataBlockTable, relative);
         } else {
            try {
               value = this.getStatementNoNotify(address) == null ? null : new Integer(this.getStatementNoNotify(address).getBinaryStatement());
            } catch (AddressErrorException var5) {
               value = null;
            }
         }

         return value;
      }
   }

   public int getAddressOfFirstNull(int baseAddress, int limitAddress) throws AddressErrorException {
      int address;
      for(address = baseAddress; address < limitAddress && this.getRawWordOrNull(address) != null; address += 4) {
      }

      return address;
   }

   public int getWord(int address) throws AddressErrorException {
      if (address % 4 != 0) {
         throw new AddressErrorException("fetch address not aligned on word boundary ", 4, address);
      } else {
         return this.get(address, 4, true);
      }
   }

   public int getWordNoNotify(int address) throws AddressErrorException {
      if (address % 4 != 0) {
         throw new AddressErrorException("fetch address not aligned on word boundary ", 4, address);
      } else {
         return this.get(address, 4, false);
      }
   }

   public int getHalf(int address) throws AddressErrorException {
      if (address % 2 != 0) {
         throw new AddressErrorException("fetch address not aligned on halfword boundary ", 4, address);
      } else {
         return this.get(address, 2);
      }
   }

   public int getByte(int address) throws AddressErrorException {
      return this.get(address, 1);
   }

   public ProgramStatement getStatement(int address) throws AddressErrorException {
      return this.getStatement(address, true);
   }

   public ProgramStatement getStatementNoNotify(int address) throws AddressErrorException {
      return this.getStatement(address, false);
   }

   private ProgramStatement getStatement(int address, boolean notify) throws AddressErrorException {
      if (!wordAligned(address)) {
         throw new AddressErrorException("fetch address for text segment not aligned to word boundary ", 4, address);
      } else if (!Globals.getSettings().getBooleanSetting(20) && !inTextSegment(address) && !inKernelTextSegment(address)) {
         throw new AddressErrorException("fetch address for text segment out of range ", 4, address);
      } else if (inTextSegment(address)) {
         return this.readProgramStatement(address, textBaseAddress, this.textBlockTable, notify);
      } else {
         return inKernelTextSegment(address) ? this.readProgramStatement(address, kernelTextBaseAddress, this.kernelTextBlockTable, notify) : new ProgramStatement(this.get(address, 4), address);
      }
   }

   public static boolean wordAligned(int address) {
      return address % 4 == 0;
   }

   public static boolean doublewordAligned(int address) {
      return address % 8 == 0;
   }

   public static int alignToWordBoundary(int address) {
      if (!wordAligned(address)) {
         if (address > 0) {
            address += 4 - address % 4;
         } else {
            address -= 4 - address % 4;
         }
      }

      return address;
   }

   public static boolean inTextSegment(int address) {
      return address >= textBaseAddress && address < textLimitAddress;
   }

   public static boolean inKernelTextSegment(int address) {
      return address >= kernelTextBaseAddress && address < kernelTextLimitAddress;
   }

   public static boolean inDataSegment(int address) {
      return address >= dataSegmentBaseAddress && address < dataSegmentLimitAddress;
   }

   public static boolean inKernelDataSegment(int address) {
      return address >= kernelDataBaseAddress && address < kernelDataSegmentLimitAddress;
   }

   public static boolean inMemoryMapSegment(int address) {
      return address >= memoryMapBaseAddress && address < kernelHighAddress;
   }

   public void addObserver(Observer obs) {
      try {
         this.addObserver(obs, 0, 2147483644);
         this.addObserver(obs, Integer.MIN_VALUE, -4);
      } catch (AddressErrorException var3) {
         System.out.println("Internal Error in Memory.addObserver: " + var3);
      }

   }

   public void addObserver(Observer obs, int addr) throws AddressErrorException {
      this.addObserver(obs, addr, addr);
   }

   public void addObserver(Observer obs, int startAddr, int endAddr) throws AddressErrorException {
      if (startAddr % 4 != 0) {
         throw new AddressErrorException("address not aligned on word boundary ", 4, startAddr);
      } else if (endAddr != startAddr && endAddr % 4 != 0) {
         throw new AddressErrorException("address not aligned on word boundary ", 4, startAddr);
      } else if (startAddr >= 0 && endAddr < 0) {
         throw new AddressErrorException("range cannot cross 0x8000000; please split it up", 4, startAddr);
      } else if (endAddr < startAddr) {
         throw new AddressErrorException("end address of range < start address of range ", 4, startAddr);
      } else {
         this.observables.add(new MemoryObservable(obs, startAddr, endAddr));
      }
   }

   public int countObservers() {
      return this.observables.size();
   }

   public void deleteObserver(Observer obs) {
      Iterator it = this.observables.iterator();

      while(it.hasNext()) {
         ((MemoryObservable)it.next()).deleteObserver(obs);
      }

   }

   public void deleteObservers() {
      this.observables = this.getNewMemoryObserversCollection();
   }

   public void notifyObservers() {
      throw new UnsupportedOperationException();
   }

   public void notifyObservers(Object obj) {
      throw new UnsupportedOperationException();
   }

   private Collection getNewMemoryObserversCollection() {
      return new Vector();
   }

   private void notifyAnyObservers(int type, int address, int length, int value) {
      if ((Globals.program != null || Globals.getGui() == null) && this.observables.size() > 0) {
         Iterator it = this.observables.iterator();

         while(it.hasNext()) {
            MemoryObservable mo = (MemoryObservable)it.next();
            if (mo.match(address)) {
               mo.notifyObserver(new MemoryAccessNotice(type, address, length, value));
            }
         }
      }

   }

   private int storeBytesInTable(int[][] blockTable, int relativeByteAddress, int length, int value) {
      return this.storeOrFetchBytesInTable(blockTable, relativeByteAddress, length, value, true);
   }

   private int fetchBytesFromTable(int[][] blockTable, int relativeByteAddress, int length) {
      return this.storeOrFetchBytesInTable(blockTable, relativeByteAddress, length, 0, false);
   }

   private synchronized int storeOrFetchBytesInTable(int[][] blockTable, int relativeByteAddress, int length, int value, boolean op) {
      int oldValue = 0;
      int loopStopper = 3 - length;
      if (blockTable == this.stackBlockTable) {
         int delta = relativeByteAddress % 4;
         if (delta != 0) {
            relativeByteAddress += 4 - delta << 1;
         }
      }

      for(int bytePositionInValue = 3; bytePositionInValue > loopStopper; --bytePositionInValue) {
         int bytePositionInMemory = relativeByteAddress % 4;
         int relativeWordAddress = relativeByteAddress >> 2;
         int block = relativeWordAddress / 1024;
         int offset = relativeWordAddress % 1024;
         if (blockTable[block] == null) {
            if (!op) {
               return 0;
            }

            blockTable[block] = new int[1024];
         }

         if (byteOrder) {
            bytePositionInMemory = 3 - bytePositionInMemory;
         }

         if (op) {
            oldValue = this.replaceByte(blockTable[block][offset], bytePositionInMemory, oldValue, bytePositionInValue);
            blockTable[block][offset] = this.replaceByte(value, bytePositionInValue, blockTable[block][offset], bytePositionInMemory);
         } else {
            value = this.replaceByte(blockTable[block][offset], bytePositionInMemory, value, bytePositionInValue);
         }

         ++relativeByteAddress;
      }

      return op ? oldValue : value;
   }

   private synchronized int storeWordInTable(int[][] blockTable, int relative, int value) {
      int block = relative / 1024;
      int offset = relative % 1024;
      if (blockTable[block] == null) {
         blockTable[block] = new int[1024];
      }

      int oldValue = blockTable[block][offset];
      blockTable[block][offset] = value;
      return oldValue;
   }

   private synchronized int fetchWordFromTable(int[][] blockTable, int relative) {
      int block = relative / 1024;
      int offset = relative % 1024;
      int value;
      if (blockTable[block] == null) {
         value = 0;
      } else {
         value = blockTable[block][offset];
      }

      return value;
   }

   private synchronized Integer fetchWordOrNullFromTable(int[][] blockTable, int relative) {
      int block = relative / 1024;
      int offset = relative % 1024;
      if (blockTable[block] == null) {
         return null;
      } else {
         int value = blockTable[block][offset];
         return new Integer(value);
      }
   }

   private int replaceByte(int sourceValue, int bytePosInSource, int destValue, int bytePosInDest) {
      return (sourceValue >> 24 - (bytePosInSource << 3) & 255) << 24 - (bytePosInDest << 3) | destValue & ~(255 << 24 - (bytePosInDest << 3));
   }

   private int reverseBytes(int source) {
      return source >> 24 & 255 | source >> 8 & '\uff00' | source << 8 & 16711680 | source << 24;
   }

   private void storeProgramStatement(int address, ProgramStatement statement, int baseAddress, ProgramStatement[][] blockTable) {
      int relative = address - baseAddress >> 2;
      int block = relative / 1024;
      int offset = relative % 1024;
      if (block < 1024) {
         if (blockTable[block] == null) {
            blockTable[block] = new ProgramStatement[1024];
         }

         blockTable[block][offset] = statement;
      }

   }

   private ProgramStatement readProgramStatement(int address, int baseAddress, ProgramStatement[][] blockTable, boolean notify) {
      int relative = address - baseAddress >> 2;
      int block = relative / 1024;
      int offset = relative % 1024;
      if (block < 1024) {
         if (blockTable[block] != null && blockTable[block][offset] != null) {
            if (notify) {
               this.notifyAnyObservers(0, address, 4, blockTable[block][offset].getBinaryStatement());
            }

            return blockTable[block][offset];
         } else {
            if (notify) {
               this.notifyAnyObservers(0, address, 4, 0);
            }

            return null;
         }
      } else {
         if (notify) {
            this.notifyAnyObservers(0, address, 4, 0);
         }

         return null;
      }
   }

   static {
      dataSegmentLimitAddress = dataSegmentBaseAddress + 4194304;
      textLimitAddress = textBaseAddress + 4194304;
      kernelDataSegmentLimitAddress = kernelDataBaseAddress + 4194304;
      kernelTextLimitAddress = kernelTextBaseAddress + 4194304;
      stackLimitAddress = stackBaseAddress - 4194304;
      memoryMapLimitAddress = memoryMapBaseAddress + 65536;
      uniqueMemoryInstance = new Memory();
   }

   private class MemoryObservable extends Observable implements Comparable {
      private int lowAddress;
      private int highAddress;

      public MemoryObservable(Observer obs, int startAddr, int endAddr) {
         this.lowAddress = startAddr;
         this.highAddress = endAddr;
         this.addObserver(obs);
      }

      public boolean match(int address) {
         return address >= this.lowAddress && address <= this.highAddress - 1 + 4;
      }

      public void notifyObserver(MemoryAccessNotice notice) {
         this.setChanged();
         this.notifyObservers(notice);
      }

      public int compareTo(Object obj) {
         if (!(obj instanceof MemoryObservable)) {
            throw new ClassCastException();
         } else {
            MemoryObservable mo = (MemoryObservable)obj;
            if (this.lowAddress >= mo.lowAddress && (this.lowAddress != mo.lowAddress || this.highAddress >= mo.highAddress)) {
               return this.lowAddress <= mo.lowAddress && (this.lowAddress != mo.lowAddress || this.highAddress <= mo.highAddress) ? 0 : -1;
            } else {
               return -1;
            }
         }
      }
   }
}
