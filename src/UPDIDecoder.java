import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/*
 *  This is experimental code I'm using to learn how to decode the undocumented UPDI protocol
 */

public class UPDIDecoder {
  private static final String[]   ptrs = {"*(ptr)", "*(ptr++)", "ptr", "err"};
  private static final String[]   updi = {
    "STATUSA",            // 0: Status A
    "STATUSB",            // 1; Status B
    "CTRLA",              // 2: Control A
    "CTRLB",              // 3: Control B
    "Reserved_4",         // 4:
    "Reserved_5",         // 5:
    "Reserved_6",         // 6:
    "ASI_KEY_STATUS",     // 7: Asynchronous System Interface Key Status
    "ASI_RESET_REQ",      // 8: Asynchronous System Interface Reset Request (write 0x59 to reset)
    "ASI_CTRLA",          // 9: Asynchronous System Interface Control A
    "ASI_SYS_CTRLA",      // A: Asynchronous System Interface System Control A
    "ASI_SYS_STATUS",     // B: Asynchronous System Interface System Status
    "ASI_CRC_STATUS",     // C: Asynchronous System Interface CRC Status
    "Reserved_D",         // D:
    "Reserved_E",         // E:
    "Reserved_F"          // F:
  };
  /*
   *  UPDI Registers
   *
   *  Off   Register name                 Description
   *  0x00  UPDI.STATUSA
   *          Bits 7-4    UPDIREV         Revision of the current UPDI implementation.
   *          Bits 3-0    Undefined
   *
   *  0x01  UPDI.STATUSB
   *          Bits 7-3    Undefined
   *          Bits 2-0    PESIG           UPDI Error Signature (0 = No error, 1 = Parity error, 2 = Frame error, 3 = Access Layer Time-Out Error
   *                                                            4 = Clock Recovery error, 5 = reserved, 6 = Bus error, 7 = Contention error)
   *
   *  0x02  UPDI.CTRLA
   *          Bit 7       IBDLY           Inter-Byte Delay Enable
   *          Bit 6       Undefined
   *          Bit 5       PARD            Parity Disable
   *          Bit 4       DTD             Disable Time-Out Detection
   *          Bit 3       RSD             Response Signature Disable
   *          Bits 2-0    GTVAL           Guard Time Value (0 = 128 cycles (default), 1 = 54, 2 = 32, 3 = 16, 4 = 8, 5 = 4, 6 = 2, 7 = reserved)
   *
   *  0x03  UPDI.CTRLB
   *          Bits 7-5    Undefined
   *          Bit 4       NACKDIS         Disable NACK Response
   *          Bit 3       CCDETDIS        Collision and Contention Detection Disable
   *          Bit 2       UPDIDIS         UPDI Disable
   *
   *  0x07  UPDI.ASI_KEY_STATUS
   *          Bits 7-6    Undefined
   *          Bit 5       UROWWRITE       User Row Write Key (UROWWRITE) Status (1 = key decoded, else 0)
   *          Bit 4       NVMPROG         NVM Programming Key (NVMPROG) Status (1 = key decoded, else 0)
   *          Bit 3       CHIPERASE       Chip Erase Key (CHIPERASE) Status (1 = key decoded, else 0)
   *          Bits 2-0    Undefined
   *
   *  0x05  Reserved_4                    Used by Attach Debugger and stepTarget()
   *
   *  0x06  Reserved_5                    Used by Attach Debugger, resetTarget and stepTarget()
   *
   *  0x07  Reserved_6
   *
   *  0x08  UPDI.ASI_RESET_REQ
   *          Bits 7-0    RSTREQ          Reset Request (0x00 = RUN, 0x59 = Normal Reset, else Reset condition is cleared)
   *
   *  0x09  UPDI.ASI_CTRLA
   *          Bits 7-2    Undefined
   *          Bits 1-0    UPDICLKDIV      UPDI Clock Divider Select (0 = reserved, 1 = 16 MHz, 2 = 8 MHz, 3 = 4 MHz)
   *
   *  0x0A  UPDI.ASI_SYS_CTRLA
   *          Bits 7-2    Undefined
   *          Bit 1       UROWWRITE_FINAL User Row Programming Done, Writing ‘1’ starts programming the User Row Data to the Flash.
   *          Bit 0       CLKREQ          Request System Clock (write 1 to request system clock, write 0 to lower clock request)
   *
   *  0x0B  UPDI.ASI_SYS_STATUS
   *          Bits 7-6    Undefined
   *          Bit 5       RSTSYS          System Reset Active (read only)
   *          Bit 4       INSLEEP         System Domain in Sleep (read only)
   *          Bit 3       NVMPROG         Start NVM Programming (read only)
   *          Bit 2       UROWPROG        Start User Row Programming (read only)
   *          Bit 1       Undefined
   *          Bit 0       LOCKSTATUS      NVM Lock Status (0 = chip erase is done, 1 = device is locked)
   *
   *  0x0C  UPDI.ASI_CRC_STATUS
   *          Bits 7-3    Undefined
   *          Bits 2-0    CRC_STATUS      CRC Execution Status (0 = not enabled, 1 = busy, 2 = done with OK, 4 = done with FAIL, others reserved)
   *
   *  0x0D  Reserved_D
   *
   *  0x0E  Reserved_E
   *
   *  0x0F  Reserved_F
   *
   *
   *   Timing (based on default, 4 MHz UPDI clock)
   *      BREAK for RESET       - 10 - 200 uS
   *      BREAK end to SYNC     - SYNC must follow in less than 13.5ms
   *      Dir Change Guard Time - CTRLA.GTVAL = 0 (default) is 128 cycles (idle bits) At 500 kHz, this is 36 uS
   *
   *  NVM Control Module
   *      0x1000  - base address
   *
   *    OCD Control Module
   *      0x0F80 - word - base address, w 0x000
   *      0x0F84 - word - w 0x0047 (hardware breakpoint?)
   *      0x0F88 - word - w 0x0002, 0x0004
   *      0x0F8C - word - r 0x0204, 0x0084, 0x0104
   *      0x0F90 - word - w 0x0000
   *      0x0F94 - PC? word - r 0x0048, 0x004E, 0x0002 - w 0x0000
   *      0x0FA0 - Register base
   *
   *    Undocumented regs
   *      rsv4 - undocumented
   *      rsv5 - undocumented
   *   *
   *    KEY Activation Signatures (LSB to MSB)
   *      Chiperase       0x4E564D4572617365
   *      OCD             0x4F43442020202020
   *      NVMPROG         0x4E564D50726F6720
   *      USERROW-Write   0x4E564D5573267465
   *      2-Wire Mode     0x5044493A41325720
   *
   *    ASI_KEY_STATUS (1 = key is active)
   *      bit 0 -
   *      bit 1 - OCD             Key = 0x20, 0x20, 0x20, 0x20, 0x20, 0x44, 0x43, 0x4F - Attach Key active
   *      bit 2 -
   *      bit 3 - CHIPERASE       Key = 0x65, 0x73, 0x61, 0x72, 0x50, 0x4D, 0x56, 0x4E
   *      bit 4 - NVMPROG         Key = 0x20, 0x67, 0x6F, 0x72, 0x50, 0x4D, 0x56, 0x4E
   *      bit 5 - UROWWRITE       Key = 0x65, 0x74, 0x26, 0x73, 0x55, 0x4D, 0x56, 0x4E
   *      bit 6 -
   *      bit 7 -
   *
   *    https://onlinedocs.microchip.com/pr/GUID-F897CF19-8EAC-457A-BE11-86BDAC9B59CF-en-US-10/index.html?GUID-A2AA4352-2351-435F-AA22-E33A1EE68FCA
   *
   *    SYSCFG - System Configuration Registers (used for OCD Messaging)
   *      SYSCFG_REVID  = 0x0F01  Device Revision ID Register (0x00 = A, 0x01 = B, and so on)
   *      SYSCFG_EXTBRK = 0x0F02  External Break Register
   *        bit 0 - Enables external break
   *        bit 1 - Will send a halt command to external break pin. This bit has no Configuration Change Protection
   *        bit 2 - Will send a restart command to external break pin. This bit has no Configuration Change Protection
   *      SYSCFG_OCDM   = 0x0F18  OCD Message Register
   *      SYSCFG_OCDMS  = 0x0F19  OCD Message Status
   *        bit 0 - OCD Message Read bit mask (1 = waiting for debugger to collect message)
   *
   *  System Informtion Block
   *    SIB = 0x74 0x69 0x6E 0x79 0x41 0x56 0x52 0x20 0x50 0x3A 0x30 0x44 0x3A 0x30 0x2D 0x33  -
   *         't'  'i'  'n'  'y'  'A'  'V'  'R'  ' '  'P'  ':'  '0'  'D'  ':'  '0'  '-'  '3'
   *          0    1    2    3    4    5    6    7    8    9   10   11   12   13   14   15
   *    0-6 = Family ID     "tinyAVR"
   *      7 = Reserved      " "
   *   8-10 = NVM Version   "P:0"
   *  11-13 = OCD Version   "D:0"
   *     14 = Reserved      "-"
   *     15 = DBG_OSC_FREQ  "3"
   */

  public static ByteArrayOutputStream bbin = new ByteArrayOutputStream();

  static int read (ByteArrayInputStream bin) {
    int data = bin.read() & 0xFF;
    bbin.write(data);
    return data;
  }

  public static String decode (byte[] inp) {
    int state = 0;
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(bout);
    ByteArrayInputStream bin = new ByteArrayInputStream(inp);
    int repeat = 0;
    while (bin.available() > 0) {
      int code = read(bin);
      switch (state) {
        case 0:                                   // Waiting for SYNC (0x55)
          if (code == 0x55) {
            state = 1;
          }
          break;
        case 1:
          out.printf("  0x55 0x%02X: ", code);
          switch (code & 0xE0) {
            case 0x00: {                            // LDS  (load)
              int sizeA = (code >> 2) & 0x03;       // Size of address (0 = byte, 1 = word)
              int sizeB = code & 0x03;              // Size of data (0 = byte[1], 1 = byte[2])
              int addr = getData(bin, sizeA);
              int data = getData(bin, sizeB);
              if (sizeA == 1) {
                // Address is 2 bytes (one word)
                if (sizeB == 1) {
                  out.printf("LDS from addr: 0x%04X returns: 0x%04X\n", addr, data);          // data is 2 bytes
                } else {
                  out.printf("LDS from addr: 0x%04X returns: 0x%02X\n", addr, data);          // data is 1 byte
                }
              } else {
                // Address is one byte
                if (sizeB == 1) {
                  out.printf("LDS from addr: 0x%02X returns: 0x%04X\n", addr, data);          // data is 2 bytes
                } else {
                  out.printf("LDS from addr: 0x%02X returns: 0x%02X\n", addr, data);          // data is 1 byte
                }
              }
            } break;
            case 0x20: {                            // LD  (load)
              int ptr = (code >> 2) & 0x03;         // 0 = *(ptr), if 1 = *(ptr++), else 3 = ptr
              int sizeB = code & 0x03;              // Size of data (0 = byte[1], 1 = byte[2])
              int data = getData(bin, sizeB);
              if (sizeB == 1) {
                out.printf("LD load via %s returns: 0x%04X\n", ptrs[ptr], data);              // data is 2 bytes
              } else {
                out.printf("LD load via %s returns: 0x%02X\n", ptrs[ptr], data);              // data is 1 byte
              }
              while (repeat-- > 0) {
                data = getData(bin, sizeB);
                if (sizeB == 1) {
                  out.printf("  RPT: LD load via %s returns: 0x%04X\n", ptrs[ptr], data);       // data is 2 bytes
                } else {
                  out.printf("  RPT: LD load via %s returns: 0x%02X\n", ptrs[ptr], data);       // data is 1 byte
                }
              }
            } break;
            case 0x40: {                          // STS (store)
              int sizeA = (code >> 2) & 0x03;     // Size of address (0 = byte, 1 = word)
              int sizeB = code & 0x03;            // Size of data (0 = byte[1], 1 = byte[2])
              int addr = getData(bin, sizeA);
              int ack1 = read(bin);
              if (ack1 == 0x40) {
                int data = getData(bin, sizeB);
                int ack2 = read(bin);
                if (ack2 == 0x40) {
                  if (sizeA == 1) {
                    // Address is 2 bytes (one word)
                    if (sizeB == 1) {
                      out.printf("STS store data: 0x%04X into addr: 0x%04X\n", data, addr);   // data is 2 bytes
                    } else {
                      out.printf("STS store data: 0x%02X into addr: 0x%04X\n", data, addr);   // data is 1 byte
                    }
                  } else {
                    // Address is one byte
                    if (sizeB == 1) {
                      out.printf("STS store data: 0x%04X into addr: 0x%02X\n", data, addr);   // data is 2 bytes
                    } else {
                      out.printf("STS store data: 0x%02X into addr: 0x%02X\n", data, addr);   // data is 1 byte
                    }
                  }
                }
              }
            } break;
            case 0x60: {                          // ST (store)
              int ptr = (code >> 2) & 0x03;       // 0 = *(ptr), if 1 = *(ptr++), else 3 = ptr
              int sizeB = code & 0x03;            // Size of data (0 = byte[1], 1 = byte[2])
              int data = getData(bin, sizeB);
              int ack1 = read(bin);
              if (ack1 == 0x40) {
                if (ptr == 2) {
                  out.printf("ST store data: 0x%04X into %s\n", data, ptrs[ptr]);
                } else {
                  if (sizeB == 1) {
                    out.printf("ST store data: 0x%04X via %s\n", data, ptrs[ptr]);            // data is 2 bytes
                  } else {
                    out.printf("ST store data: 0x%02X via %s\n", data, ptrs[ptr]);            // data is 1 byte
                  }
                }
              }
              while (repeat-- > 0) {
                data = getData(bin, sizeB);
                ack1 = read(bin);
                if (ack1 == 0x40) {
                  if (sizeB == 1) {
                    out.printf("  RPT: ST store data: 0x%04X via %s\n", data, ptrs[ptr]);       // data is 2 bytes
                  } else {
                    out.printf("  RPT: ST store data: 0x%02X via %s\n", data, ptrs[ptr]);       // data is 1 byte
                  }
                }
              }
            } break;
            case 0x80: {                          // LDCS (load)
              int reg = code & 0x0F;              // Register (0-15)
              int data = read(bin);
              out.printf("LDCS load from %s returns: 0x%02X\n", updi[reg], data);
            } break;
            case 0xA0: {                          // REPEAT
              repeat = read(bin);
              out.printf("REPEAT following instruction %d times\n", repeat + 1);
            } break;
            case 0xC0: {                          // STCS (store)
              int reg = code & 0x0F;              // Register (0-15)
              int data = read(bin);
              out.printf("STCS store data 0x%02X into %s\n", data, updi[reg]);
            } break;
            case 0xE0: {                          // KEY
              boolean sib = (code & 0x04) != 0;
              byte[] data;
              if (sib) {
                out.print("SIB = ");
                data = new byte[16];
              } else {
                out.print("KEY = ");
                data = new byte[8];
              }
              for (int ii = 0; ii < data.length; ii++) {
                data[ii] = (byte) read(bin);
              }
              for (byte cc : data) {
                out.printf("0x%02X ", ((int) cc & 0xFF));
              }
              out.print("\n                    ");
              for (byte cc : data) {
                out.printf("'%c'  ", (char) cc);
              }
              out.println();
            } break;
            default:
              out.println("error");
              break;
          }
          state = 0;
      }
    }
    return bout.toString();
  }


  private static int getData (ByteArrayInputStream bin, int size) {
    if (size == 0) {
      return read(bin);
    } else {
      // GET 16 BIT, LSB-ORDER DATA
      return (read(bin) & 0xFF) + (read(bin) << 8);
    }
  }
}
