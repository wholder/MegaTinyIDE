import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

import java.io.ByteArrayOutputStream;
import java.util.*;

public class SDBG extends Programmer {
  public static final int     SYNC = 0x55;            // Sync character
  public static final int     ACK = 0x40;             // Ack character
  public static final int     LDS = 0x00;             // Load Data from Data Space Using Direct Addressing
  public static final int     LD = 0x20;              // Load Data from Data Space Using Indirect Addressing
  public static final int     STS = 0x40;             // Store Data to Data Space Using Direct Addressing
  public static final int     ST = 0x60;              // Store Data from Data Space Using Indirect Addressing
  public static final int     LDCS = 0x80;            // Load Data from Control and Status Register Space
  public static final int     REPEAT = 0xA0;          // Set Instruction Repeat Counter
  public static final int     STCS = 0xC0;            // Store Data to Control and Status register space
  public static final int     KEY = 0xE0;             // Set Activation KEY
  // Used by KEY instructions
  public static final int     KEY_64 = 0x00;
  public static final int     KEY_128 = 0x01;
  public static final int     GET_SIB = 0x04;
  // Used by ST and LD instructions
  public static final int     AT_PTR = 0;             // *(ptr)
  public static final int     AT_PTR_PP = 1;          // *(ptr++)
  public static final int     PTR = 2;                // ptr
  // Keys
  public static final byte[]  ChipErase = {0x65, 0x73, 0x61, 0x72, 0x45, 0x4D, 0x56, 0x4E};   // "esarEMVN" - "NVMErase"
  public static final byte[]  NvmProg   = {0x20, 0x67, 0x6F, 0x72, 0x50, 0x4D, 0x56, 0x4E};   // " gorPMVN" - "NVMProg "
  public static final byte[]  UserRow   = {0x65, 0x74, 0x26, 0x73, 0x55, 0x4D, 0x56, 0x4E};   // "et&IUMVN" - "NVMUs&te"
  public static final byte[]  OcdAttach = {0x20, 0x20, 0x20, 0x20, 0x20, 0x44, 0x43, 0x4F};   // "     DCO" - "OCD     "
  // UPDI Registers
  // addresses 0x0-0x3 are the UPDI Physical configuration registers
  public static final int     UPDI_STATUSA = 0x00;      // Status A
  public static final int     UPDI_STATUSB = 0x01;      // Status B
  public static final int     UPDI_CTRLA = 0x02;        //
  public static final int     UPDI_CTRLB = 0x03;        //
  // addresses 0x4-0xC are the ASI level registers
  public static final int     ASI_KEY_STATUS = 0x07;    //
  public static final int     ASI_RESET_REQ = 0x08;     // ASI Reset Request (write 0x59 to reset)
  public static final int     ASI_CTRLA = 0x09;         //
  public static final int     ASI_SYS_CTRLA = 0x0A;     //
  public static final int     ASI_SYS_STATUS = 0x0B;    //
  public static final int     ASI_CRC_STATUS = 0x0C;    //
  // NVM CTRA Commands
  public static final int     NVM_NONE = 0;             // No command
  public static final int     NVM_WP = 1;               // Write page buffer to memory (NVMCTRL.ADDR selects memory)
  public static final int     NVMCER = 2;               // Erase page (NVMCTRL.ADDR selects which memory)
  public static final int     NVM_ERWP = 3;             // Erase and write page (NVMCTRL.ADDR selects which memory)
  public static final int     NVM_PBC = 4;              // Page buffer clear
  public static final int     NVM_CHER = 5;             // Erase Flash and EEPROM (unless EESAVE in FUSE.SYSCFG is '1')
  public static final int     NVM_EEER = 6;             // EEPROM Erase
  public static final int     NVM_WFU = 7;              // Write fuse (only accessible through UPDI)
  // Register Bit Field Masks
  public static final int     UPDIREV = 0xF0;           // STATUSA.UPDIREV (UPDI Revision)
  public static final int     PESIG = 0x0F;             // STATUSB.PESIG (UPDI Error Signature)
  public static final int     IBDLY = 0x80;             // CTRLA.IBDLY (Inter-Byte Delay Enable)
  public static final int     PARD = 0x20;              // CTRLA.PARD (Parity Disable)
  public static final int     DTD = 0x10;               // CTRLA.DTD (Disable Timeout Detection)
  public static final int     RSD = 0x08;               // CTRLA.RSD (Response Signature Disable)
  public static final int     GTVAL = 0x07;             // CTRLA.GTVAL (Guard Time Value)
  public static final int     NACKDIS = 0x10;           // CTRLB.NACKDIS (Disable NACK Response)
  public static final int     CCDETDIS = 0x08;          // CTRLB.CCDETDIS (Collision and Contention Detection Disable)
  public static final int     UPDIDIS = 0x04;           // CTRLB.UPDIDIS (UPDI Disable)
  public static final int     UROWWRITE = 0x20;         // ASI_KEY_STATUS.UROWWRITE (User Row Write Key Status Active)
  public static final int     NVMPROG = 0x10;           // ASI_KEY_STATUS.NVMPROG (NVM Programming Active)
  public static final int     CHIPERASE = 0x08;         // ASI_KEY_STATUS.CHIPERASE (Chip Erase Active)
  public static final int     UPDICLKSEL = 0x03;        // ASI_CTRLA.UPDICLKSEL (UPDI Clock Select)
  public static final int     UROWWRITE_FINAL = 0x02;   // ASI_SYS_CTRLA.UROWWRITE_FINAL (User Row Programming Done)
  public static final int     CLKREQ = 0x01;            // ASI_SYS_CTRLA.CLKREQ (Request System Clock)
  public static final int     RSTSYS = 0x30;            // ASI_SYS_STATUS.RSTSYS (System Reset Active)
  public static final int     INSLEEP = 0x10;           // ASI_SYS_STATUS.INSLEEP (System Domain in Sleep)
  public static final int     NVMPROG2 = 0x08;          // ASI_SYS_STATUS.NVMPROG2 (Start NVM Programming)
  public static final int     UROWPROG = 0x04;          // ASI_SYS_STATUS.UROWPROG (Start User Row Programming)
  public static final int     LOCKSTATUS = 0x01;        // ASI_SYS_STATUS.LOCKSTATUS (NVM Lock Status Active)
  public static final int     CRC_STATUS = 0x07;        // ASI_CRC_STATUS.CRC_STATUS (CRC Execution Status)
  // Direct Addresses
  public static final int     NVMCTRL_BASE = 0x1000;    //
  public static final int     LOCK_BASE = 0x1040;       //
  public static final int     SIGNATURE_BASE = 0x1100;  // (0-3) Device Id, (4-12) Serial num
  public static final int     TEMP_BASE = 0x1120;       // (See  datasheet
  public static final int     OSC16_BASE = 0x1122;      // OSC16 error at 3V (first byte) and 5V (2nd byte)
  public static final int     OSC20_BASE = 0x1124;      // OSC20 error at 3V (first byte) and 5V (2nd byte)
  public static final int     FUSE_BASE = 0x1280;       //
  public static final int     USERROW_BASE = 0x1300;    //
  public static final int     EEPROM_BASE = 0x1400;     //
  public static final int     SRAM_128_BASE = 0x3F80;   //
  public static final int     SRAM__256_BASE = 0x3F00;  //
  public static final int     FLASH_BASE = 0x8000;      //
  // NVMCTRL Registers (Offset from NVMCTRL_BASE)
  public static final int     NVM_CTRLA = 0x00;         // NVMCTRL_BASE.CTRLA
  public static final int     NVM_CTRLB = 0x01;         // NVMCTRL_BASE.CTRLB
  public static final int     NVM_STATUS = 0x02;        // NVMCTRL_BASE.STATUS
  public static final int     NVM_INTCTRL = 0x03;       // NVMCTRL_BASE.INTCTRL
  public static final int     NVM_INTFLAGS = 0x04;      // NVMCTRL_BASE.INTFLAGS
  public static final int     NVM_DATAL = 0x06;         // NVMCTRL_BASE.DATAL
  public static final int     NVM_DATAH = 0x07;         // NVMCTRL_BASE.DATAH
  public static final int     NVM_ADDL = 0x08;          // NVMCTRL_BASE.ADDL
  public static final int     NVM_ADDH = 0x09;          // NVMCTRL_BASE.ADDH
  // Misc constants
  public static final int     BYTE = 0;                 // Byte address or data
  public static final int     WORD = 1;                 // Word address or data
  // Variables
  private final JSSCPort      jPort;
  private Utility.ProgressBar progress;
  private final MegaTinyIDE   ide;

  public SDBG (MegaTinyIDE ide, JSSCPort jPort) {
    this.ide = ide;
    this.jPort = jPort;
    int baudRate = ide.prefs.getInt("sdbg_baud", SerialPort.BAUDRATE_57600);
    // setParameters(int baudRate, int dataBits, int stopBits, int parity)
    jPort.setParameters(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_2, SerialPort.PARITY_EVEN);
  }

  // Progress Bar methods

  public void setProgressMessage (String msg) {
    if (progress == null) {
      progress = new Utility.ProgressBar(ide, "");
    }
    progress.setMessage(msg);
  }

  public void setProgressValue (int value) {
    if (progress != null) {
      progress.setValue(value);
    }
  }

  public void closeProgressBar () {
    if (progress != null) {
      progress.close();
    }
    progress = null;
  }

  /**
   * Initiaoize the UPDI interface (in case it's jammed up)
   */
  void init () {
    int code;
    int retry = 5;
    do {
      try {
        jPort.sendDoubleBreak();
        stcs(UPDI_CTRLB, 1 << 3);                                     // CCDETDIS: Collision and Contention Detection Disable
        stcs(UPDI_CTRLA, 1 << 7);                                     // IBDLY: Inter-Byte Delay Enable
        ldcs(UPDI_STATUSA);
        code = ldcs(UPDI_STATUSB) & 0x07;
        if (code > 0) {
          String[] errCodes = {
            "No error",
            "Parity error",
            "Frame error",
            "Access Layer Timeout Error",
            "Clock recovery error",
            "Reserved",
            "Reserved",
            "Contention error",
          };
          System.out.println("init() " + errCodes[code]);
        } else {
          ldcs(UPDI_STATUSA);
          ldcs(UPDI_STATUSB);
          return;
        }
      } catch (Exception ex) {
        System.out.printf("init() " + ex.getMessage() + ", retry = %d\n", retry);
      }
      retry--;
    } while (retry > 0);
    throw new IllegalStateException("init() timeout");
  }

  /**
   * Send byte[] of data to serial poirt and read back echoed values
   * Note: compares the data read back to that send and throws an exception
   * if it does not match
   * @param data byte[] of UPDI command, or data
   * @throws SerialPortException
   */
  void sendBytes (byte[] data) throws SerialPortException {
    jPort.writeBytes(data);
    try {
      byte[] readback = jPort.readBytes(data.length, 500);
      if (!Arrays.equals(data, readback)) {
        throw new IllegalStateException("Readback mismatch");
      }
    } catch (SerialPortTimeoutException ex) {
      throw new IllegalStateException("SDBG.readBytes() Timeout");
    }
  }

  /**
   * Get a byte[] of data from tthe serial port
   * Notes; uses a timeout of 500 ms to recover from errors
   * @param size number of bytes to read
   * @return byte[] of data
   * @throws SerialPortException
   */
  public byte[] getBytes (int size) throws SerialPortException {
    try {
      return jPort.readBytes(size, 100);
    } catch (SerialPortTimeoutException ex) {
      throw new IllegalStateException("SDBG.readBytes() Timeout");
    }
  }

  // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
  //                    Mid Level Functions
  // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

  /**
   * Read and verify receipt of an ACK byte (0x40) from the UPDI interface
   * @throws SerialPortException
   */
  void getAck () throws SerialPortException {
    byte[] data = getBytes(1);
    if (data.length == 1 & data[0] == ACK) {
      return;
    }
    throw new IllegalStateException("Missing ACK");
  }

  /** Load Data from Data Space Using Direct Addressing
   * @param addressType (BYTE or WORD)
   * @param address     direct memory address
   * @param dataSize    (1 or 2)
   * @return
   * @throws SerialPortException
   */
  public byte[] lds (int addressType, int address, int dataSize) throws SerialPortException {
    if (addressType > WORD || dataSize > 2) {
      throw new IllegalStateException("lds invald parameters");
    }
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    buf.write(SYNC);
    buf.write((byte) (LDS | (addressType << 2) | (dataSize > 1 ? WORD : BYTE)));
    buf.write((byte) (address & 0xFF));
    if (addressType == WORD) {
      buf.write((byte) ((address >> 8) & 0xFF));
    }
    byte[] cmd = buf.toByteArray();
    sendBytes(cmd);
    return getBytes(dataSize);
  }

  int ldsWord (int address) throws SerialPortException {
    byte[] data = lds(WORD, address, 2);
    return (data[0] & 0xFF) + ((((int) data[1] & 0xFF) << 8));
  }

  int ldsByte (int address) throws SerialPortException {
    byte[] data = lds(WORD, address, 1);
    return (data[0] & 0xFF);
  }

   /**
    * Store Data to Data Space Using Direct Addressing
    * @param addressType (BYTE or WORD)
    * @param address     direct memory address
    * @param data        data to write (byte[11] or byte[2])
    * @throws SerialPortException
    */
  public void sts (int addressType, int address, byte[] data) throws SerialPortException {
    if (addressType > WORD || data.length > 2) {
      throw new IllegalStateException("lds invald parameters");
    }
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    buf.write(SYNC);
    buf.write((byte) (STS | (addressType << 2) | (data.length > 1 ? WORD : BYTE)));
    buf.write((byte) (address & 0xFF));
    if (addressType == WORD) {
      buf.write((byte) ((address >> 8) & 0xFF));
    }
    byte[] cmd = buf.toByteArray();
    sendBytes(cmd);
    getAck();
    sendBytes(data);
    getAck();
  }

  /**
   * Uses the st() method to send a 16 bit wowrd to absolute WORD address
   * @param address address in target's address space
   * @param data byte[] of data to send
   * @throws SerialPortException
   */
  public void stsWord (int address, int data) throws SerialPortException {
    sts(WORD, address, new byte[] {(byte) (data & 0xFF), (byte) ((data >> 8) & 0xFF)});
  }

  /**
   * Uses the st() method to send an 8 bit byte to an absolute WORD address
   * @param address address in target's address space
   * @param data byte[] of data to send
   * @throws SerialPortException
   */
  public void stsByte (int address, int data) throws SerialPortException {
    sts(WORD, address, new byte[] {(byte) (data & 0xFF)});
  }

  /**
   * LD Instruction Operation
   *
   * @param ptr        if 0 = *(ptr), if 1 = *(ptr++), else 3 = ptr
   * @param dataType   0: BYTE or 1: WORD
   * @param dataLength number of data units (byte or word) to read
   * @return
   * @throws SerialPortException
   */
  public byte[] ld (int ptr, int dataType, int dataLength) throws SerialPortException {
    byte[] cmd = new byte[]{SYNC, (byte) (LD | (ptr << 2) | dataType)};
    sendBytes(cmd);
    return getBytes(dataLength);
  }

  /**
   * ST Instruction Operation
   *
   * @param ptr  if 0 = *(ptr), if 1 = *(ptr++), else 3 = ptr
   * @param data 1 or 2 byte array of data
   * @throws SerialPortException
   */
  public void st (int ptr, byte[] data) throws SerialPortException {
    if (data.length > 2) {
      throw new IllegalStateException("st(int, byte[]) data too long");
    }
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    buf.write(SYNC);
    buf.write(((byte) (ST | (ptr << 2) | (data.length > 1 ? WORD : BYTE))));
    buf.write(data[0]);
    if (data.length > 1) {
      buf.write(data[1]);
    }
    sendBytes(buf.toByteArray());
    getAck();
  }

  /**
   * Use the st() method to send a 16 bit WORD of data
   * @param ptr if 0 = *(ptr), if 1 = *(ptr++), else 3 = ptr
   * @param valus 16 bit word value
   * @throws SerialPortException
   */
  public void stWord (int ptr, int valus) throws SerialPortException {
    st(ptr, new byte[] {(byte) (valus & 0xFF), (byte) ((valus >> 8) & 0xFF)});
  }

  /**
   * Use the st() method to send an bit BYTE of data
   * @param ptr if 0 = *(ptr), if 1 = *(ptr++), else 3 = ptr
   * @param value 8 bit word value
   * @throws SerialPortException
   */
  public void stByte (int ptr, int value) throws SerialPortException {
    st(ptr, new byte[] {(byte) (value & 0xFF)});
  }

  /**
   * Load Data from Control and Status Register Space
   *
   * @param reg register's number (0 - 15)
   * @return register's value
   * @throws SerialPortException
   */
  public int ldcs (int reg) throws SerialPortException {
    byte[] cmd = new byte[]{SYNC, (byte) (LDCS | reg)};
    sendBytes(cmd);
    return getBytes(1)[0] & 0xFF;
  }

  /**
   * Store Data to Control and Status Register Space
   * @param reg register number (0 - 15)
   * @param data value to set into register
   * @throws SerialPortException
   */
  public void stcs (int reg, int data) throws SerialPortException {
    byte[] cmd = new byte[] {SYNC, (byte) (STCS | reg), (byte) data};
    sendBytes(cmd);
  }

  /**
   * Set Instruction Repeat Counter
   * @param count numver of times to repeat following command
   * @throws SerialPortException
   */
  public void setRepeat (int count) throws SerialPortException {
    byte[] cmd = new byte[] {SYNC, (byte) REPEAT, (byte) count};
    sendBytes(cmd);
  }

  /**
   * Send key value to unlock selected function function:
   *  ChipErase, NvmProg, UserRow, Attach (debugger)
   * @param key key value
   * @throws SerialPortException
   */
  public void setKey (byte[] key) throws SerialPortException{
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    buf.write(SYNC);
    buf.write(key.length == 8 ? (byte) (KEY | KEY_64) : (byte) (KEY | KEY_128));
    buf.write(key, 0, key.length);
    sendBytes(buf.toByteArray());
  }

  /**
   * Erase NVM memory
   *  Note: works on locked devices
   *
   * Chip Erase steps:
   *  1. Enter the CHIPERASE KEY by using the KEY instruction
   *  2. Read the Chip Erase bit in the AS Key Status register  (CHIPERASE in ASI_KEY_STATUS) to see
   *      that the KEY is successfully activated.
   *  3. Write the Reset signature (0x59) into the ASI_RESET_REQ register. This will issue a System Reset
   *  4. Write 0x00 to the ASI Reset Request register (ASI_RESET_REQ)  clear the System Reset
   *  5. Read the Lock Status bit in the ASI System Status register (LOCKSTATUS in ASI_SYS_STATUS).
   *  6. Chip Erase is done when LOCKSTATUS == 0 in ASI_SYS_STATUS. If LOCKSTATUS == 1, loop to step 5
   *  Note: After a successful Chip Erase, the Lockbits will be cleared, and the UPDI will have full access
   *        to the system.
   *
   */
  public void eraseChip () throws SerialPortException {
    setKey(ChipErase);
    int chipErase = ldcs(ASI_KEY_STATUS);
    if ((chipErase & CHIPERASE) != 0) {
      stcs(ASI_RESET_REQ, 0x59);
      stcs(ASI_RESET_REQ, 0x00);
      do {
        // Wait for LOCKSTATUS to clear (go to 0)
      } while ((ldcs(ASI_SYS_STATUS) & LOCKSTATUS) != 0);
    } else {
      throw new IllegalStateException("Unable to activate chip erase");
    }
  }

  /**
   * Send NvmProg key to enable NVM access on the target
   * @throws SerialPortException
   */
  private void enterNvmProgMode () throws SerialPortException {
    setKey(NvmProg);
    stcs(ASI_RESET_REQ, 0x59);
    stcs(ASI_RESET_REQ, 0x00);
    int rsp;
    int timeout = 100;
    do {
      rsp = ldcs(ASI_SYS_STATUS);
      if (timeout-- <= 0) {
        throw new IllegalStateException("Timeout reafFuses();");
      }
    } while ((rsp & 0x08) == 0);
  }

  private void exitNvmProgMode () throws SerialPortException {
    stcs(ASI_RESET_REQ, 0x59);
    stcs(ASI_RESET_REQ, 0x00);
    ldcs(ASI_SYS_STATUS);                                             // Clears RSTSYS bit
  }

  /**
   * Read memory using the REPEAT command (max of 255 repeats)
   *
   * @param address direct memory address
   * @param size    nuber of bytes to read (max of 256 bytes)
   * @return        bytes read
   * @throws SerialPortException
   */
  public byte[] readMemory (int address, int size) throws SerialPortException {
    if (size > 256) {
      throw new IllegalStateException("readMemory() size > 256");
    }
    stWord(PTR, address);                                             // Write address to ptr
    setRepeat(size - 1);                                              // Set repeat
    return ld(AT_PTR_PP, BYTE, size);                                 // Read data bytes
  }

  /**
   * Write memory using the ld() command
   *
   * @param address direct memory address
   * @param data    byte[] array of sata to write (max of 256
   * @throws SerialPortException
   */
  public void writeMemory (int address, byte[] data) throws SerialPortException {
    if (data.length > 256) {
      throw new IllegalStateException("readMemory() size > 256");
    }
    stWord(PTR, address);                                             // Write address to ptr
    setRepeat(data.length - 1);                                       // Set repeat
    stByte(AT_PTR_PP, data[0] & 0xFF);                                // Write first data byte via *(ptr++)
    for (int ii = 1; ii < data.length; ii++) {
      sendBytes(new byte[] {data[ii]});
      getAck();
    }
  }

  public void waitFlash () throws SerialPortException {
    for (int ii = 0; ii < 10; ii++) {
      int status = lds(WORD, NVMCTRL_BASE + NVM_STATUS, 1)[0];
      if ((status & (1 << 2)) != 0) {                                 // Write error
        throw new IllegalStateException("waitFlash() write errir");
      }
      if ((status & (1 << 1)) != 0 || (status & (1 << 0)) != 0) {     // Flash or EEPROM busy
        continue;
      }
      return;
    }
    throw new IllegalStateException("waitFlashReady() timeout");
  }

  /**
   * Wait for selected bit in register to go to 0
   * @param register selected register
   * @param bit selected bit number
   * @throws SerialPortException
   */
  private void waitRegBitClear (int register, int bit) throws SerialPortException{
    int timeout = 100;
    do {
      if (timeout-- == 0) {
        throw new IllegalStateException("waitRegBitClear() timeout");
      }
    } while ((ldcs(register) & (1 << bit)) != 0);                     // Wait for bit == 0
  }

  /**
   * Wait for selected bit in register to go to 1
   * @param register selected register
   * @param bit selected bit number
   * @throws SerialPortException
   */
  private void waitRegBitSet (int register, int bit) throws SerialPortException{
    int timeout = 100;
    do {
      if (timeout-- == 0) {
        throw new IllegalStateException("waitRegBitSet() timeout");
      }
    } while ((ldcs(register) & (1 << bit)) == 0);                     // Wait for bit != 0
  }

  /** Wait for value in register ANDed with mask to goto zero
   * @param register selected register
   * @param mask selected bits mask
   * @throws SerialPortException
   */
  private void waitRegMaskZero (int register, int mask) throws SerialPortException{
    int timeout = 100;
    do {
      if (timeout-- == 0) {
        throw new IllegalStateException("waitRegMaskZero() timeout");
      }
    } while ((ldcs(register) & mask) != 0);                     // Wait for (value & mask) != 0
  }

  private int[] bytesToWords (byte[] data) {
    int[] words = new int[data.length / 2];
    int idx = 0;
    for (int ii = 0; ii < words.length; ii++) {
      words[ii] = (data[idx] & 0xFF) + (((data[idx + 1] & 0xFF) << 8));
      idx += 2;
    }
    return words;
  }

  /**
   * Helper class for High-Level functions
   */
  class NvmHandler {
    byte[] doAction () {
      try {
        jPort.open(null);
        init();
        enterNvmProgMode();
        byte[] ret = action();
        exitNvmProgMode();
        return ret;
      } catch (Exception ex) {
        throw new EDBGException(("NvmHandler() " + ex.getMessage()));
      } finally {
        try {
          jPort.close();
        } catch (Exception ex) {
          throw new EDBGException(("NvmHandler() " + ex.getMessage()));
        }
      }
    }

    byte[] action () throws SerialPortException {
      // Override
      return null;
    }
  }

  // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
  //                    High-Level Functions
  // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

  /**
   * Get Target's Signature Info (3 bytes)
   * @return byte[] of Signature data
   * @throws EDBGException
   */
  @Override
  public byte[] getDeviceSignature() throws EDBGException {
    return new NvmHandler() {
      @Override
      byte[] action () throws SerialPortException {
        return readMemory(SIGNATURE_BASE, 3);
      }
    }.doAction();
  }

  /**
   * Get Target's Serial Number Info (13 bytes)
   * @return byte[] of serial data
   * @throws EDBGException
   */
  @Override
  public byte[] getDeviceSerialNumber () throws EDBGException {
    return new NvmHandler() {
      @Override
      byte[] action () throws SerialPortException {
        return readMemory(SIGNATURE_BASE + 3, 13);
      }
    }.doAction();
  }

  /**
   * Read from Target's Flash Memory
   * @param address address relative to FLASH_BASE
   * @param len number of bytes to read
   * @return byyte[] of Flash data
   * @throws EDBGException
   */
  @Override
  public byte[] readFlash (int address, int len) throws EDBGException {
    byte[] data = new byte[len];
    return new NvmHandler() {
      @Override
      byte[] action () throws SerialPortException {
        byte[] buf = new byte[256];
        for (int idx = 0; idx < data.length; idx += buf.length) {
          int remain = Math.min(256, data.length - idx);
          buf = readMemory(FLASH_BASE + address + idx, remain);
          setProgressValue((int) ((float) idx / data.length * 100.0));
          System.arraycopy(buf, 0, data, idx, remain);
        }
        return data;
      }
    }.doAction();
  }

  /**
   * Write code to flash in 32 word chunks
   * @param address zero-based address
   * @param data byte[] array of code
   * @throws EDBGException
   */
  @Override
  public void writeFlash (int address, byte[] data) throws EDBGException {
    new NvmHandler() {
      @Override
      byte[] action () throws SerialPortException {
        int pageSize = 64;                                            // 32 words
        waitRegMaskZero(NVMCTRL_BASE + NVM_STATUS, 0x03);             // Wait for FBUSY and EEBUSY == 0
        stsByte(NVMCTRL_BASE + NVM_CTRLA, NVM_CHER);                  // 0x05 -> NVM.NVM.CTRLA (Chip erase)
        waitRegMaskZero(NVMCTRL_BASE + NVM_STATUS, 0x03);             // Wait for FBUSY and EEBUSY == 0
        for (int idx = 0; idx < data.length; idx += pageSize) {
          stsByte(NVMCTRL_BASE + NVM_CTRLA, NVM_PBC);                 // 0x04 -> NVM.NVM.CTRLA (Page buffer clear)
          waitRegMaskZero(NVMCTRL_BASE + NVM_STATUS, 0x03);           // Wait for FBUSY and EEBUSY == 0
          stWord(PTR, FLASH_BASE + address + idx);                    // FLASH_BASE + address + idx -> ptr
          byte[] page = new byte[pageSize];
          int remain = Math.min(pageSize, data.length - idx);         // remaining bytes to write in page
          System.arraycopy(data, idx, page, 0, remain);               // Copy bytes into page[] array
          for (int ii = remain; ii < pageSize; ii++) {
            page[ii] = (byte) 0xFF;                                   // Fill unused bytes with 0xFF
          }
          int[] words = bytesToWords(page);
          setRepeat(pageSize / 2 - 1);                                // Repeat fllowing command pageSize / 2 -1 times
          stWord(AT_PTR_PP, words[0]);                                // Write first word to ptr
          for (int ii = 1; ii < words.length; ii++) {
            int word = words[ii];
            sendBytes(new byte[] {(byte) word, (byte) (word >> 8)});
            getAck();
          }
          stsByte(NVMCTRL_BASE + NVM_CTRLA, NVM_WP);                  // 0x01 -> NVM.NVM.CTRLA (Write page buffer to memory)
          waitRegMaskZero(NVMCTRL_BASE + NVM_STATUS, 0x03);           // Wait for FBUSY and EEBUSY == 0
          setProgressValue((int) ((float) idx / data.length * 100.0));
        }
        return null;
      }
    }.doAction();

  }

  /**
   * Performs an erase on the target. Note: Functionality varies according to target family.
   *
   * @param address Start address of section to erase (byte address)
   * @param mode    (see table 7-25)
   *                0x00 = Chip erase (use this mode for ATTiny)
   *                0x01 = Application erase
   *                0x02 = Boot section erase
   *                0x03 = EEPROM erase
   *                0x04 = Application page erase
   *                0x05 = Boot page erase
   *                0x06 = EEPROM page erase
   *                0x07 = User signature erase
   */
  @Override
  public void eraseTarget (int address, int mode) throws EDBGException {
    if (address != 0 || mode != 0) {
      throw new EDBGException("eraseTarget() invalid parameters");
    }
    new NvmHandler() {
      @Override
      byte[] action () throws SerialPortException {
        eraseChip();
        return null;
      }
    }.doAction();
  }

  /**
   * Read fuse starting at FUSES_BASE + offsets
   * Note: base offset for fuses starts at 0x1280 and must be in program mode to call
   *
   *  Default fuse values:          attiny416 attiny212 attiny3217
   *    0x00  WDTCFG    = 00000000    0x00      0x00      0x00
   *    0x01  BODCFG    = 00000000    0x00      0x00      0x00
   *    0x02  OSCCFG    = 0-----10    0x02      0x02      0x02
   *    0x03  Reserved                0xFF      0xFF      0xFF
   *    0x04  TCD0CFG   = 00000000    0x00      0x00      0x00
   *    0x05  SYSCFG0   = 11-101-0    0xCD      0xF6      0xF6
   *    0x06  SYSCFG1   = -----111    0x03      0x07      0x07
   *    0x07  APPEND    = 00000000    0x00      0x00      0x00
   *    0x08  BOOTEND   = 00000000    0x02      0x00      0x00
   *    0x09  Reserved                0xFF      0xFF      0xFF
   *    0x0A  LOCKBIT   = 00000000    0xC5      0xC5      0xC5
   *
   *    Note: attiny416  from ATTiny416-Xplained-Nano
   *          attiny3217 from ATTiny3217-Curiosity-Nano
   *          attiny212  from raw attiny212 chip
   *
   * @param offsets array of offsets to FUSE_BASE from which to read fuses
   * @return byte[offsets.length] array containing fuse data
   */
  @Override
  public byte[] readFuses (int[] offsets) throws EDBGException {
    return new NvmHandler() {
      @Override
      byte[] action () throws SerialPortException {
        byte[] data = readMemory(FUSE_BASE, 11);
        // Copy the fuses selected by offsets[]
        byte[] buf = new byte[offsets.length];
        for (int ii = 0; ii < offsets.length; ii++) {
          buf[ii] = data[offsets[ii]];
        }
        return buf;
      }
    }.doAction();
  }

  /**
   * Write to fuse at FUSES_BASE + offsets (debugger can only write one byte at a time)
   * Note: base offset for fuses starts at 0x1280 and must be in program mode to call
   *
   * @param offsets array of offsets to FUSE_BASE from which to write fuses
   * @param fuses   fuse data bytes to write
   * @throws EDBGException
   */
  @Override
  public void writeFuses (int[] offsets, byte[] fuses) throws EDBGException {
    new NvmHandler() {
      byte[] action () throws SerialPortException {
        for (int idx = 0; idx < offsets.length; idx++) {
          waitRegBitSet(ASI_SYS_STATUS, 3);                           // Wait for NVMPROG == 1
          int address = FUSES_BASE + offsets[idx];
          stsByte(NVMCTRL_BASE + NVM_ADDL, address & 0xFF);           // address & 0xFF -> NVM_ADDL
          stsByte(NVMCTRL_BASE + NVM_ADDH, (address >> 8) & 0xFF);    // address >> 8 -> NVM_ADDH
          stsByte(NVMCTRL_BASE + NVM_DATAL, fuses[idx]);              // fuse -> NVM_DATAL
          stsByte(NVMCTRL_BASE + NVM_CTRLA, NVM_WFU);                 // NVM_WFU (0x07) -> NVM_WFU (Write fuse)
          int status;
          do {
            status = ldsByte(NVMCTRL_BASE + NVM_STATUS);
          } while ((status & 0x03) != 0);                             // Wait for EEBUSY & FBUSY == 0
        }
        return null;
      }
    }.doAction();
  }

  /**
   * Read EEPROM
   *
   * @param address offset from BASE (Note: assumes 128 bytes of EEPROM for ATTiny412)
   * @param size    number of bytes to read
   * @return byte[] array of EEPROM data read
   * @throws EDBGException
   */
  @Override
  public byte[] readEeprom (int address, int size) throws EDBGException {
    return new NvmHandler() {
      @Override
      byte[] action () throws SerialPortException {
        waitFlash();
        return readMemory(EEPROM_BASE + address, size);
      }
    }.doAction();
  }

  /**
   * @param address offset from BASE (Note: assumes 128 bytes of EEPROM for ATTiny412)
   * @param data    byte[] array of EEPROM data to write
   * @throws EDBGException
   *
   * Write 0x13 to UPDI_NVMCTRL.CTRLA (NVMCTRL_BASE (0x1000 )+ 0x00)
   */
  @Override
  public void writeEeprom (int address, byte[] data) throws EDBGException {
    new NvmHandler() {
      @Override
      byte[] action () throws SerialPortException {
        waitFlash();
        stsByte(NVMCTRL_BASE + NVM_CTRLA, NVM_EEER);                  // 0x06 (Erase EEPROM)
        for (int idx = 0; idx < data.length; idx += 32) {
          int remain = Math.min(32, data.length - idx);
          byte[] buf = new byte[remain];
          System.arraycopy(data, idx, buf, 0, buf.length);
          waitFlash();
          stsByte(NVMCTRL_BASE + NVM_CTRLA, NVM_PBC);                 // 0x04 (Page buffer clear)
          writeMemory(EEPROM_BASE + address + idx, buf);
          stsByte(NVMCTRL_BASE + NVM_CTRLA, NVM_WP);                  // 0x01 (Write page buffer to memory)
        }
        waitFlash();
        return null;
      }
    }.doAction();
  }

  /**
   * Read the USERROW data
   * @param address (relative to USERROW_BASE)
   * @param len byte[] array of data (typically 16 bytes)
   * @return byte[] of USERROW data
   * @throws EDBGException
   */
  @Override
  public byte[] readUserRow (int address, int len) throws EDBGException {
    return new NvmHandler() {
      byte[] action () throws SerialPortException {
        return readMemory(USERROW_BASE + address, len);
      }
    }.doAction();
  }

  /**
   *  Write the USERROW data
   * @param address (relative to USERROW_BASE)
   * @param data byte[] array of data (typically 16 bytes)
   * @throws EDBGException
   */
  @Override
  public void writeUserRow (int address, byte[] data) throws EDBGException {
    new NvmHandler() {
      byte[] action () throws SerialPortException {
        setKey(UserRow);
        waitRegBitSet(ASI_KEY_STATUS, 5);                             // Wait for UROWPROG (bit 5) of ASI_KEY_STATUS == 1
        stcs(ASI_RESET_REQ, 0x59);                                    // Reset resuest (0x59)
        ldcs(ASI_SYS_STATUS);                                         // Read tp clear RSTSYS (bit 5)
        stcs(ASI_RESET_REQ, 0x00);                                    // Clear Reset request (0x00)
        waitRegBitSet(ASI_SYS_STATUS, 2);                             // Wait for UROWPROG (bit 2) of ASI_SYS_STATUS == 1
        writeMemory(USERROW_BASE + address, data);
        stcs(ASI_SYS_CTRLA, (1 << 1));                                // UROWWRITE_FINAL (bit 1) = 1
        waitRegBitClear(ASI_SYS_STATUS, 2);                           // Wait for UROWPROG (bit 2) == 0
        stcs(ASI_KEY_STATUS, (1 << 5));                               // Set UROWWRITE (bit = 5) of ASI_KEY_STATUS = 1
        stcs(ASI_RESET_REQ, 0x59);                                    // Reset resuest
        ldcs(ASI_SYS_STATUS);                                         // Read tp clear RSTSYS (bit 5)
        stcs(ASI_RESET_REQ, 0x00);                                    // Clear Reset request
        waitRegBitClear(ASI_SYS_STATUS, 5);                           // Wait for RSTSYS (bit 5) of ASI_SYS_STATUS == 0
        return null;
      }
    }.doAction();
  }

  @Override
  public void close () {
  }
}
