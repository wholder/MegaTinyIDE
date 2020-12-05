import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

  /*
   *  This is experimental code I'm using to learn how to decode the undocumented UPDI protocol
   */

public class UPDIDecoder {
  private static final String[]   ptrs = {"*(ptr)", "*(ptr++)", "ptr", "err"};
  private static final String[]   regs = {"STATUSA", "STATUSB", "CTRLA", "CTRLB", "Reserved_4", "Reserved_5", "Reserved_6",
                                          "ASI_KEY_STATUS", "ASI_RESET_REQ", "ASI_CTRLA", "ASI_SYS_CTRLA", "ASI_SYS_STATUS",
                                          "ASI_CRC_STATUS", "Reserved_D", "Reserved_E", "Reserved_F"};

  /*
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
   *
   *    ASI_SYS_STATUS
   *      bit 0 - LOCKSTATUS      NVM Lock Status (1 = locked)
   *      bit 1 - undocumented
   *      bit 2 - UROWPROG        Start User Row Programming
   *      bit 3 - NVMPROG         Start NVM Programming
   *      bit 4 - INSLEEP         System Domain in Sleep
   *      bit 5 - RSTSYS          System Reset Active
   *      bit 6 - undocumented
   *      bit 7 - undocumented
   *
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
          int sizeA = (code >> 2) & 0x03;
          int sizeB = code & 0x03;
          int addr = getData(bin, sizeA);
          int data = getData(bin, sizeB);
          if (sizeA == 0) {
            if (sizeB == 0) {
              out.printf("LDS from addr: 0x%02X returns: 0x%02X\n", addr, data);
            } else {
              out.printf("LDS from addr: 0x%02X returns: 0x%04X\n", addr, data);
            }
          } else {
            if (sizeB == 0) {
              out.printf("LDS from addr: 0x%04X returns: 0x%02X\n", addr, data);
            } else {
              out.printf("LDS from addr: 0x%04X returns: 0x%04X\n", addr, data);
            }
          }
        } break;
        case 0x20: {                            // LD  (load)
          int ptr = (code >> 2) & 0x03;
          int sizeAB = code & 0x03;
          int data = getData(bin, sizeAB);
          if (sizeAB == 0) {
            out.printf("LD load via %s returns: 0x%02X\n", ptrs[ptr], data);
          } else {
            out.printf("LD load via %s returns: 0x%04X\n", ptrs[ptr], data);
          }
          while (repeat-- > 0) {
            data = getData(bin, sizeAB);
            if (sizeAB == 0) {
              out.printf("  RPT: LD load via %s returns: 0x%02X\n", ptrs[ptr], data);
            } else {
              out.printf("  RPT: LD load via %s returns: 0x%04X\n", ptrs[ptr], data);
            }
          }
        } break;
        case 0x40: {                          // STS (store)
          int sizeA = (code >> 2) & 0x03;
          int sizeB = code & 0x03;
          int addr = getData(bin, sizeA);
          int ack1 = read(bin);
          if (ack1 == 0x40) {
            int data = getData(bin, sizeB);
            int ack2 = read(bin);
            if (ack2 == 0x40) {
              if (sizeA == 0) {
                if (sizeB == 0) {
                  out.printf("STS store data: 0x%02X into addr: 0x%02X\n", data, addr);
                } else {
                  out.printf("STS store data: 0x%02X into addr: 0x%04X\n", data, addr);
                }
              } else {
                if (sizeB == 0) {
                  out.printf("STS store data: 0x%04X into addr: 0x%02X\n", data, addr);
                } else {
                  out.printf("STS store data: 0x%04X into addr: 0x%04X\n", data, addr);
                }
              }
            }
          }
        } break;
        case 0x60: {                          // ST (store)
          int ptr = (code >> 2) & 0x03;
          int sizeAB = code & 0x03;
          int data = getData(bin, sizeAB);
          int ack1 = read(bin);
          if (ack1 == 0x40) {
            if (ptr == 2) {
              out.printf("ST store data: 0x%04X into %s\n", data, ptrs[ptr]);
            } else {
              if (sizeAB == 0) {
                out.printf("ST store data: 0x%02X via %s\n", data, ptrs[ptr]);
              } else {
                out.printf("ST store data: 0x%04X via %s\n", data, ptrs[ptr]);
              }
            }
          }
          while (repeat-- > 0) {
            data = getData(bin, sizeAB);
            ack1 = read(bin);
            if (ack1 == 0x40) {
              if (sizeAB == 0) {
                out.printf("  RPT: ST store data: 0x%02X via %s\n", data, ptrs[ptr]);
              } else {
                out.printf("  RPT: ST store data: 0x%04X via %s\n", data, ptrs[ptr]);
              }
            }
          }
        } break;
        case 0x80: {                          // LDCS (load)
          int reg = code & 0x0F;
          int data = read(bin);
          out.printf("LDCS load from %s returns: 0x%02X\n", regs[reg], data);
        } break;
        case 0xA0: {                          // REPEAT
          repeat = read(bin);
          out.printf("REPEAT following instruction %d times\n", repeat + 1);
         } break;
        case 0xC0: {                          // STCS (store)
          int reg = code & 0x0F;
          int data = read(bin);
          out.printf("  STCS store data 0x%02X into %s\n", data, regs[reg]);
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
            data[ii] = (byte) read(bin);;
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

  //  SIB = 0x74 0x69 0x6E 0x79 0x41 0x56 0x52 0x20 0x50 0x3A 0x30 0x44 0x3A 0x30 0x2D 0x33  -
  //         't'  'i'  'n'  'y'  'A'  'V'  'R'  ' '  'P'  ':'  '0'  'D'  ':'  '0'  '-'  '3'


  private static int getData (ByteArrayInputStream bin, int size) {
    if (size == 0) {
      return read(bin);
    } else {
      return (read(bin) & 0xFF) + (read(bin) << 8);
    }
  }
}
