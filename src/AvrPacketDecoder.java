  /*
   *  Test code to support future development - used to decode various AVR command packets and
   *  error code and convert them into human-readable messages.
   */
public class AvrPacketDecoder {
  public static String decode (byte[] cmd) {
    StringBuilder buf = new StringBuilder();
    switch (cmd[0]) {               // AVR Protocol sub-protocol handler ID
    //
    //  DISCOVERY
    //
    case 0x00:                      // DISCOVERY (For discovering the feature set of the tool)
      buf.append("DISCOVERY:");
      switch (cmd[1]) {             // DISCOVERY Command
      case 0x00:                    // QUERY
        // cmd[2] = Command version (always 0x00)
        buf.append("QUERY:");
        switch (cmd[3]) {           // QUERY context (Context of the parameter to get)
        case 0x00:                  // DISCOVERY_COMMAND_HANDLERS
          buf.append("COMMAND_HANDLERS:");
          break;
        case (byte) 0x80:           // DISCOVERY_TOOL_NAME
          buf.append("TOOL_NAME:");
          break;
        case (byte) 0x81:           // DISCOVERY_SERIAL_NUMBER
          buf.append("SERIAL_NUMBER:");
          break;
        case (byte) 0x82:           // DISCOVERY_MNF_DATE
          buf.append("MNF_DATE:");
          break;
        default:
          buf.append(String.format("Err: 0x%02X:", cmd[3]));
        }
        break;
      default:
        buf.append(String.format("Err: 0x%02X:", cmd[1]));
      }
      break;
    //
    //  HOUSEKEEPING
    //
    case 0x01:                      // HOUSEKEEPING (Housekeeping functions)
      buf.append("HOUSEKEEPING:");
      switch (cmd[1]) {             // HOUSEKEEPING Command
      case 0x00:                    // CMD_HOUSEKEEPING_QUERY (Capability discovery)
        // cmd[2] = Command version (always 0x00)
        switch (cmd[3]) {           // Get context (Context of the parameter to get)
        case 0x00:                  // HK_QUERY_COMMANDS (Supported command list)
          buf.append("HK_QUERY_COMMANDS:");
          break;
        default:
          buf.append(String.format("Err: 0x%02X:", cmd[3]));
        }
        break;
      case 0x01:                    // CMD_HOUSEKEEPING_SET (Set parameters)
        switch (cmd[3]) {           // SET context (Context of the parameter to get)
        // cmd[3] = Context of the parameter to set
        // cmd[4] = Address
        // cmd[5] = number of bytes to write
        // cmd[6:n] = data to write
        default:
          buf.append(String.format("Err: 0x%02X:", cmd[3]));
        }
        //break;
      case 0x02:                    // CMD_HOUSEKEEPING_GET (Get parameters)
        buf.append("CMD_HOUSEKEEPING_GET:");
        switch (cmd[3]) {           // GET context (Context of the parameter to get)
        // cmd[3] = Context of the parameter to set
        // cmd[4] = Address
        // cmd[5] = number of bytes to read
        case 0x00:                  // HK_CONTEXT_CONFIG (Config parameter)
          switch (cmd[4]) {         // Housekeeping Config Context Parameters
          case 0x00:                // HOUSEKEEPING_CONFIG_HWREV (Hardware revision)
            buf.append("HOUSEKEEPING_CONFIG_HWREV:");
            // cmd[5] = 1 bytes (Hardware version)
            break;
          case 0x01:                // HOUSEKEEPING_CONFIG_FWREV_MAJ (Firmware revision high)
            buf.append("HOUSEKEEPING_CONFIG_FWREV_MAJ:");
            // cmd[5] = 1 byte (Firmware major version)
            break;
          case 0x02:                // HOUSEKEEPING_CONFIG_FWREV_MIN (Firmware revision low)
            buf.append("HOUSEKEEPING_CONFIG_FWREV_MIN:");
            // cmd[5] = 1 byte (Firmware minor version)
            break;
          case 0x03:                // HOUSEKEEPING_CONFIG_BUILD (Firmware build number)
            buf.append("HOUSEKEEPING_CONFIG_BUILD:");
            // cmd[5] = 2 bytes (Firmware build number)
            break;
          default:
            buf.append(String.format("Err: 0x%02X:", cmd[4]));
          }
          break;
        case 0x01:                  // HK_CONTEXT_ANALOG (Analog parameter)
          switch (cmd[4]) {         // Housekeeping Analog Context Parameters
          case 0x00:                // HOUSEKEEPING_ANALOG_VTREF (Target reference voltage)
            buf.append("HOUSEKEEPING_ANALOG_VTREF:");
            // cmd[5] = 2 bytes (mV value number) LSB, MSB, 0
            break;
          default:
            buf.append(String.format("Err: 0x%02X:", cmd[4]));
          }
          break;
        default:
          buf.append(String.format("Err: 0x%02X:", cmd[3]));
        }
        break;
      case 0x10:                    // CMD_HOUSEKEEPING_START_SESSION (Start a session)
        buf.append("CMD_HOUSEKEEPING_START_SESSION:");
        // cmd[2] = Command version
        break;
      case 0x11:                    // CMD_HOUSEKEEPING_END_SESSION (Terminate a session)
        buf.append("CMD_HOUSEKEEPING_END_SESSION:");
        // cmd[2] = Command version
        // cmd[3] = Use reset (0 - = no reset, 1 = external reset will be applied)
        break;
      case 0x30:                    // CMD_HOUSEKEEPING_JTAG_DETECT (Scan for JTAG devices)
        buf.append("CMD_HOUSEKEEPING_JTAG_DETECT:");
        // cmd[2] = Command version
        // cmd[3] = Use reset (0 - = no reset, 1 = external reset will be applied)
        break;
      case 0x31:                    // CMD_HOUSEKEEPING_CAL_OSC (Oscillator calibration)
        buf.append("CMD_HOUSEKEEPING_CAL_OSC:");
        // cmd[2] = Command version
        break;
      case 0x50:                    // CMD_HOUSEKEEPING_FW_UPGRADE (Enter upgrade mode)
        buf.append("CMD_HOUSEKEEPING_FW_UPGRADE:");
        // cmd[2] = Command version
        // cmd[3;6] = Upgrade key (4 byte key for enabling upgrade mode)
        break;
      default:
        buf.append(String.format("Err: 0x%02X:", cmd[1]));
      }
      break;
    //
    //  AVRISP (not implemented here)
    //
    case 0x11:                      // AVRISP (not implemented here)
      buf.append("AVRISP:Err: Not implemented");
      break;
    //
    //  AVR8GENERIC
    //
    case 0x12:                      // AVR8GENERIC (Programming and debugging of AVR 8 bit devices)
      buf.append("AVR8GENERIC:");
      if (cmd[2] != 0) {
        return "AVR8GENERIC protocol version must be 0x00";
      }
      // cmd[0] = 0x12 = AVR8GENERIC
      // cmd[1] = Command ID
      // cmd[2] = Command version (always 0x00)
      switch (cmd[1]) {             // AVR8 Command ID
        /*
         *    CMD_AVR8_QUERY
         */
      case 0x00:                    // CMD_AVR8_QUERY
        buf.append("CMD_AVR8_QUERY:");
        // cmd[3] = Query context (Type of query to execute)
        switch (cmd[3]) {           //Context Id
        case 0x00:                  // AVR8_QUERY_COMMANDS (List of protocol commands supported)
          buf.append("AVR8_QUERY_COMMANDS:");
          // response: list of n bytes
          break;
        case 0x05:                  // AVR8_QUERY_CONFIGURATION (List of configuration parameters supported)
          buf.append("AVR8_QUERY_CONFIGURATION:");
          // response: list of n bytes
          break;
        case 0x07:                  // AVR8_QUERY_READ_MEMTYPES (List of readable memtypes)
          buf.append("AVR8_QUERY_READ_MEMTYPES:");
          // response: list of n bytes
         break;
        case 0x08:                  // AVR8_QUERY_WRITE_MEMTYPES (List of writeable memtypes)
          buf.append("AVR8_QUERY_WRITE_MEMTYPES:");
          // response: list of n bytes
          break;
        default:
          buf.append(String.format("Err: 0x%02X:", cmd[3]));
        }
        break;
      /*
       *    CMD_AVR8_SET
       */
      case 0x01:                    // CMD_AVR8_SET
        buf.append("CMD_AVR8_SET:");
        // cmd[0] = 0x12 = AVR8GENERIC
        // cmd[1] = Command Id (0x01 = SET)
        // cmd[2] = command version
        // cmd[3] = SET context (Context of the parameter to set)
        // cmd[4] = Address (Parameter ID/start address)
        // cmd[5] = number of bytes
        switch (cmd[3]) {           // SET context
        case 0x00:                  // AVR8_CTXT_CONFIG
          buf.append("AVR8_CTXT_CONFIG:");
          switch (cmd[4]) {
          case 0x00:                // AVR8_CONFIG_VARIANT
            buf.append("AVR8_CONFIG_VARIANT:");
            // Constants
            switch (cmd[6]) {
            case 0x00:              // AVR8_VARIANT_LOOPBACK      (W 1 byte)
              buf.append("AVR8_VARIANT_LOOPBACK = ");
              break;
            case 0x01:              // AVR8_VARIANT_DW            (W 1 byte)
              buf.append("AVR8_VARIANT_DW");
              break;
            case 0x02:              // AVR8_VARIANT_MEGAJTAG      (W 1 byte)
              buf.append("AVR8_VARIANT_MEGAJTAG");
              break;
            case 0x03:              // AVR8_VARIANT_XMEGA         (W 1 byte)
              buf.append("AVR8_VARIANT_XMEGA");
              break;
            case 0x05:              // AVR8_VARIANT_UPDI          (W 1 byte)
              buf.append("AVR8_VARIANT_UPDI");
              break;
            case (byte) 0xFF:       // AVR8_VARIANT_NONE          (W 1 byte)
              buf.append("AVR8_VARIANT_NONE");
              break;
            default:
              buf.append(String.format("Err: 0x%02X:", cmd[6]));
              break;
            }
            break;
          case 0x01:                // AVR8_CONFIG_FUNCTION
            buf.append("AVR8_CONFIG_FUNCTION = ");
            // Constants
            switch (cmd[6]) {
            case 0x00:              // AVR8_FUNC_NONE             (W 1 byte)
              buf.append("AVR8_FUNC_NONE");
              break;
            case 0x01:              // AVR8_FUNC_PROGRAMMING      (W 1 byte)
              buf.append("AVR8_FUNC_PROGRAMMING");
              break;
            case 0x02:              // AVR8_FUNC_DEBUGGING        (W 1 byte)
              buf.append("AVR8_FUNC_DEBUGGING");
              break;
            default:
              buf.append(String.format("Err: 0x%02X:", cmd[6]));
              break;
            }
            break;
          default:
            buf.append(String.format("Err: 0x%02X:", cmd[4]));
            break;
          }
          break;
        case 0x01:                  // AVR8_CTXT_PHYSICAL
          buf.append("AVR8_CTXT_PHYSICAL:");
          switch (cmd[4]) {
          case 0x00:                // AVR8_PHY_INTERFACE         (W 1 byte)
            buf.append("AVR8_PHY_INTERFACE = ");
            // Constants
            switch (cmd[6]) {
            case 0x00:              // AVR8_PHY_INTF_NONE         (W 1 byte)
              buf.append("AVR8_PHY_INTF_NONE");
              break;
            case 0x04:              // AVR8_PHY_INTF_JTAG         (W 1 byte)
              buf.append("AVR8_PHY_INTF_JTAG");
              break;
            case 0x05:              // AVR8_PHY_INTF_DW           (W 1 byte)
              buf.append("AVR8_PHY_INTF_DW");
              break;
            case 0x06:              // AVR8_PHY_INTF_PDI          (W 1 byte)
              buf.append("AVR8_PHY_INTF_PDI");
              break;
            case 0x08:              // AVR8_PHY_INTF_PDI_1W       (W 1 byte)
              buf.append("AVR8_PHY_INTF_PDI_1W");
              break;
            default:
              buf.append(String.format("Err: 0x%02X:", cmd[6]));
              break;
            }
            break;
          case 0x01:                // AVR8_PHY_JTAG_DAISY        (RW 4 bytes)
            buf.append("AVR8_PHY_JTAG_DAISY:");
            // see Table 7-5 "AVR8 SET/GET Parameters"
            if (cmd[5] == 4) {
              buf.append(String.format("DEV-0x%08X:", get4ByteInt(cmd, 6)));
            } else {
              buf.append(String.format("Err: 0x%02X:", cmd[4]));
            }
            break;
          case 0x10:                // AVR8_PHY_DW_CLK_DIV
            buf.append("AVR8_PHY_DW_CLK_DIV = ");
            // debugWIRE clock division factor                    (W 1 byte)
            buf.append(String.format("%d", (int) cmd[6] & 0xFF));
            break;
          case 0x20:                // AVR8_PHY_MEGA_PRG_CLK
            buf.append("AVR8_PHY_MEGA_PRG_CLK = ");
            // JTAG clock prog frequency (kHz)                    (W 2 bytes)
            buf.append(String.format("%d kHz", get2ByteInt(cmd, 6)));
            break;
          case 0x21:                // AVR8_PHY_MEGA_DBG_CLK
            buf.append("AVR8_PHY_MEGA_DBG_CLK = ");
            // JTAG clock debug frequency (kHz)                   (W 2 bytes)
            buf.append(String.format("%d kHz", get2ByteInt(cmd, 6)));
            break;
          case 0x30:                // AVR8_PHY_XM_JTAG_CLK
            buf.append("AVR8_PHY_XM_JTAG_CLK = ");
            // JTAG XMEGA clock frequency (kHz)                   (W 2 bytes)
            buf.append(String.format("%d kHz", get2ByteInt(cmd, 6)));
            break;
          case 0x31:                // AVR8_PHY_XM_PDI_CLK
            buf.append("AVR8_PHY_XM_PDI_CLK = ");
            // PDI clock frequency (kHz)                          (W 2 bytes)
            buf.append(String.format("%d kHz", get2ByteInt(cmd, 6)));
            break;
          default:
            buf.append(String.format("Err: 0x%02X:", cmd[4]));
            break;
          }
          break;
        case 0x02:                  // AVR8_CTXT_DEVICE
          buf.append("AVR8_CTXT_DEVICE:");
          // Parsing options for AVR devices with UPDI
          int len = cmd[5];
          int idx = cmd[4];
          while (len > 0) {
            switch (idx) {
            case 0x00:                // PROG_BASE (Start address of program memory) 2 bytes
              int base = ((int) cmd[6 + idx] & 0xFF) + (((int) cmd[6 + idx + 1] & 0xFF) << 8);
              buf.append(String.format("\n  PROG_BASE = 0x%04X", base));
              len -= 2;
              idx += 2;
              break;
            case 0x02:                // FLASH_PAGE_BYTES (Page size of flash in bytes) 1 byte
              buf.append(String.format("\n  FLASH_PAGE_BYTES = 0x%02X", ((int) cmd[6 + idx] & 0xFF)));
              len--;
              idx++;
              break;
            case 0x03:                // EEPROM_PAGE_BYTES (Page size of EEPROM) 1 byte
              buf.append(String.format("\n  EEPROM_PAGE_BYTES = 0x%02X", ((int) cmd[6 + idx] & 0xFF)));
              len--;
              idx++;
              break;
            case 0x04:                // NVMCTRL_MODULE (Address of NVMCTRL module) 2 bytes
              int nvm = ((int) cmd[6 + idx] & 0xFF) + (((int) cmd[6 + idx + 1] & 0xFF) << 8);
              buf.append(String.format("\n  NVMCTRL_MODULE = 0x%04X", nvm));
              len -= 2;
              idx += 2;
              break;
            case 0x06:                // OCD_MODULE (Address of OCD module) 2 bytes
              int ocd = ((int) cmd[6 + idx] & 0xFF) + (((int) cmd[6 + idx + 1] & 0xFF) << 8);
              buf.append(String.format("\n  OCD_MODULE = 0x%04X", ocd));
              len -= 2;
              idx += 2;
              break;
            default:
              buf.append(String.format("Err: 0x%02X:", cmd[4]));
              break;
            }
          }
          break;
        case 0x03:                  // AVR8_CTXT_OPTIONS
          buf.append("AVR8_CTXT_OPTIONS:");
          switch (cmd[4]) {         // Address/Id
          case 0x00:                // AVR8_OPT_RUN_TIMERS        (W 1 byte)
            buf.append("AVR8_OPT_RUN_TIMERS = ");
            buf.append(String.format("%d", (int) cmd[6] & 0xFF));
            break;
          case 0x01:                // AVR8_OPT_DISABLE_DBP       (W 1 byte)
            buf.append("AVR8_OPT_DISABLE_DBP = ");
            buf.append(String.format("%d", (int) cmd[6] & 0xFF));
            break;
          case 0x03:                // AVR8_OPT_ENABLE_IDR        (W 1 byte)
            buf.append("AVR8_OPT_ENABLE_IDR = ");
            buf.append(String.format("%d", (int) cmd[6] & 0xFF));
            break;
          case 0x04:                // AVR8_OPT_POLL_INT          (W 1 byte" values = 1. 5. 10, 20 50, 100)
            buf.append("AVR8_OPT_POLL_INT = ");
            buf.append(String.format("%d", (int) cmd[6] & 0xFF));
            break;
          default:
            buf.append(String.format("Err: 0x%02X:", cmd[4]));
            break;
          }
          break;
        case 0x04:                  // AVR8_CTXT_SESSION
          buf.append("AVR8_CTXT_SESSION:");
          switch (cmd[4]) {
          case 0x00:                // AVR8_SESS_MAIN_PC          (W 4 bytes) - (deprecated)
            buf.append("AVR8_SESS_MAIN_PC:");
            if (cmd[5] == 4) {
              buf.append(String.format("PC-0x%08X:", get4ByteInt(cmd, 6)));
            } else {
              buf.append(String.format("Err: 0x%02X:", cmd[5]));
            }
            break;
          default:
            buf.append(String.format("Err: 0x%02X:", cmd[4]));
            break;
          }
          break;
        default:
          buf.append(String.format("Err: 0x%02X:", cmd[3]));
          break;
        }
        break;
      /*
       *    CMD_AVR8_GET
       */
      case 0x02:                    // CMD_AVR8_GET
        buf.append("CMD_AVR8_GET:");
        // cmd[0] = 0x12 = AVR8GENERIC
        // cmd[1] = Command Id (0x02 = GET)
        // cmd[2] = command version
        // cmd[3] = SET context (Context of the parameter to set)
        // cmd[4] = Address (Parameter ID/start address)
        switch (cmd[3]) {           // GET context
        case 0x00:                  // AVR8_CTXT_CONFIG
          buf.append("AVR8_CTXT_CONFIG:");
          switch (cmd[4]) {
          case 0x00:                // AVR8_CONFIG_VARIANT
            buf.append("AVR8_CONFIG_VARIANT:");
            // Constants
            break;
          case 0x01:                // AVR8_CONFIG_FUNCTION
            buf.append("AVR8_CONFIG_FUNCTION = ");
            break;
          default:
            buf.append(String.format("Err: 0x%02X:", cmd[4]));
            break;
          }
          break;
        case 0x01:                  // AVR8_CTXT_PHYSICAL
          buf.append("AVR8_CTXT_PHYSICAL:");
          switch (cmd[4]) {
          case 0x00:                // AVR8_PHY_INTERFACE         (W 1 byte)
            buf.append("AVR8_PHY_INTERFACE = ");
            break;
          case 0x01:                // AVR8_PHY_JTAG_DAISY        (RW 4 bytes)
            buf.append("AVR8_PHY_JTAG_DAISY:");
            // see Table 7-5 "AVR8 SET/GET Parameters"
            break;
          case 0x10:                // AVR8_PHY_DW_CLK_DIV
            buf.append("AVR8_PHY_DW_CLK_DIV = ");
            // debugWIRE clock division factor                    (W 1 byte)
            break;
          case 0x20:                // AVR8_PHY_MEGA_PRG_CLK
            buf.append("AVR8_PHY_MEGA_PRG_CLK = ");
            // JTAG clock prog frequency (kHz)                    (W 2 bytes)
            break;
          case 0x21:                // AVR8_PHY_MEGA_DBG_CLK
            buf.append("AVR8_PHY_MEGA_DBG_CLK = ");
            // JTAG clock debug frequency (kHz)                   (W 2 bytes)
            break;
          case 0x30:                // AVR8_PHY_XM_JTAG_CLK
            buf.append("AVR8_PHY_XM_JTAG_CLK = ");
            // JTAG XMEGA clock frequency (kHz)                   (W 2 bytes)
            break;
          case 0x31:                // AVR8_PHY_XM_PDI_CLK
            buf.append("AVR8_PHY_XM_PDI_CLK = ");
            // PDI clock frequency (kHz)                          (W 2 bytes)
            break;
          default:
            buf.append(String.format("Err: 0x%02X:", cmd[4]));
            break;
          }
          break;
        case 0x02:                  // AVR8_CTXT_DEVICE
          buf.append("AVR8_CTXT_DEVICE:");
          // Parsing options for AVR devices with UPDI
          switch (cmd[4]) {
          case 0x00:                // PROG_BASE (Start address of program memory) 2 bytes
            buf.append("PROG_BASE:");
            break;
          case 0x02:                // FLASH_PAGE_BYTES (Page size of flash in bytes) 1 byte
            buf.append("FLASH_PAGE_BYTES:");
            break;
          case 0x03:                // EEPROM_PAGE_BYTES (Page size of EEPROM) 1 byte
            buf.append("EEPROM_PAGE_BYTES:");
            break;
          case 0x04:                // NVMCTRL_MODULE (Address of NVMCTRL module) 2 bytes
            buf.append("NVMCTRL_MODULE:");
            break;
          case 0x06:                // OCD_MODULE (Address of OCD module) 2 bytes
            buf.append("OCD_MODULE:");
            break;
          default:
            buf.append(String.format("Err: 0x%02X:", cmd[4]));
            break;
          }
          break;
        case 0x03:                  // AVR8_CTXT_OPTIONS
          buf.append("AVR8_CTXT_OPTIONS:");
          switch (cmd[4]) {         // Address/Id
          case 0x00:                // AVR8_OPT_RUN_TIMERS        (W 1 byte)
            buf.append("AVR8_OPT_RUN_TIMERS = ");
            break;
          case 0x01:                // AVR8_OPT_DISABLE_DBP       (W 1 byte)
            buf.append("AVR8_OPT_DISABLE_DBP = ");
            break;
          case 0x03:                // AVR8_OPT_ENABLE_IDR        (W 1 byte)
            buf.append("AVR8_OPT_ENABLE_IDR = ");
            break;
          case 0x04:                // AVR8_OPT_POLL_INT          (W 1 byte" values = 1. 5. 10, 20 50, 100)
            buf.append("AVR8_OPT_POLL_INT = ");
            break;
          default:
            buf.append(String.format("Err: 0x%02X:", cmd[4]));
            break;
          }
          break;
        case 0x04:                  // AVR8_CTXT_SESSION
          buf.append("AVR8_CTXT_SESSION:");
          switch (cmd[4]) {
          case 0x00:                // AVR8_SESS_MAIN_PC          (W 4 bytes) - (deprecated)
            buf.append("AVR8_SESS_MAIN_PC:");
            break;
          default:
            buf.append(String.format("Err: 0x%02X:", cmd[4]));
            break;
          }
          break;
        default:
          buf.append(String.format("Err: 0x%02X:", cmd[3]));
          break;
        }
        break;
        /*
         * CMD_AVR8_ACTIVATE_PHYSICAL
         */
      case 0x10:                    // CMD_AVR8_ACTIVATE_PHYSICAL (Connect physically)
        buf.append("CMD_AVR8_ACTIVATE_PHYSICAL:");
        // cmd[3] = Reset (0 = no reset, 1 = apply external reset during activation)
        switch (cmd[3]) {
        case 0x00:
          buf.append("no reset:");
          break;
        case 0x01:
          buf.append("reset:");
          break;
        default:
          buf.append(String.format("Err: 0x%02X:", cmd[3]));
        }
        break;
      /*
       * CMD_AVR8_DEACTIVATE_PHYSICAL
       */
      case 0x11:                    // CMD_AVR8_DEACTIVATE_PHYSICAL (Disconnect physically)
        buf.append("CMD_AVR8_DEACTIVATE_PHYSICAL:");
        // no additional parameters after cmd[2]
        break;
      /*
       * xxCMD_AVR8_GET_IDxx
       */
      case 0x12:                    // CMD_AVR8_GET_ID (Read the ID)
        buf.append("CMD_AVR8_GET_ID:");
        // no additional parameters after cmd[2]
        break;
      /*
       * CMD_AVR8_ATTACH
       */
      case 0x13:                    // CMD_AVR8_ATTACH (Attach to OCD module)
        buf.append("CMD_AVR8_ATTACH:");
        // cmd[3] = Break (0 = continue running, 1 = break after attach)
        switch (cmd[3]) {
        case 0x00:
          buf.append("continue running:");
          break;
        case 0x01:
          buf.append("break after attach:");
          break;
        default:
          buf.append(String.format("Err: 0x%02X:", cmd[3]));
          break;
        }
        break;
      /*
       * CMD_AVR8_DETACH
       */
      case 0x14:                    // CMD_AVR8_DETACH (Detach from OCD module)
        buf.append("CMD_AVR8_DETACH:");
        // no additional parameters after cmd[2]
        break;
      case 0x15:                    // CMD_AVR8_PROG_MODE_ENTER (Enter programming mode)
        buf.append("CMD_AVR8_PROG_MODE_ENTER:");
        // no additional parameters after cmd[2]
        break;
      case 0x16:                    // CMD_AVR8_PROG_MODE_LEAVE (Leave programming mode)
        buf.append("CMD_AVR8_PROG_MODE_LEAVE:");
        // no additional parameters after cmd[2]
        break;
      case 0x17:                    // CMD_AVR8_DISABLE_DEBUGWIRE (Disable debugWIRE interface)
        buf.append("CMD_AVR8_DISABLE_DEBUGWIRE:");
        // no additional parameters after cmd[2]
        break;
      case 0x20:                    // CMD_AVR8_ERASE (Erase the chip)
        buf.append("CMD_AVR8_ERASE:");
        // cmd[3] = Mode (0 = Chip, 1 = Application, 2 = Boot section, 3 = EEPROM,
        //                4 = Application page, 5 = Boot page, 6 = EEPROM page, 7 = User signature)
        switch (cmd[3]) {
        case 0x00:                  // Chip erase
          buf.append("Chip:");
          break;
        case 0x01:                  // Application erase
          buf.append("Application:");
          break;
        case 0x02:                  // Boot section erase
          buf.append("Boot section:");
          break;
        case 0x03:                  // EEPROM erase
          buf.append("EEPROM:");
          break;
        case 0x04:                  // Application page erase
          buf.append("Application page:");
          break;
        case 0x05:                  // Boot page erase
          buf.append("Boot page:");
          break;
        case 0x06:                  // EEPROM page erase
          buf.append("EEPROM page:");
          break;
        case 0x07:                  // User signature erase
          buf.append("User signature:");
          break;
        default:
          buf.append(String.format("Err: 0x%02X:", cmd[3]));
          break;
        }
        // cmd[4:7] = Address (4 byte Start address (byte address) of memory to access)
        buf.append(String.format("Address-0x%08X:", get4ByteInt(cmd, 4)));
        break;
      /*
       * CMD_AVR8_MEMORY_READ
       */
      case 0x21:                    // CMD_AVR8_MEMORY_READ (Read memory)
        buf.append("CMD_AVR8_MEMORY_READ:");
        // cmd[3] = Type (see list in section 7.4 of "EDBG-based Tools Protocols" document)
        buf.append(getMemType(cmd[3]));
        // cmd[4:7] = Start Address (4 byte address of memory to access)
        buf.append(String.format("Start-0x%08X:", get4ByteInt(cmd, 4)));
        // cmd[8:11] = Bytes (4 bytes Number of bytes to access. Payload restrictions apply)
        buf.append(String.format("Bytes-0x%08X:", get4ByteInt(cmd, 8)));
        break;
      /*
       * CMD_AVR8_MEMORY_READ_MASKED
       */
      case 0x22:                    // CMD_AVR8_MEMORY_READ_MASKED (Read memory while via a mask)
        buf.append("CMD_AVR8_MEMORY_READ_MASKED:");
        // cmd[3] = Type (see list in section 7.4 of "EDBG-based Tools Protocols" document)
        buf.append(getMemType(cmd[3]));
        // cmd[4:7] = Start Address (4 byte First address for Read (byte address))
        buf.append(String.format("Start-0x%08X:", get4ByteInt(cmd, 4)));
        // cmd[8:11] = Bytes (4 bytes Number of bytes to access. Payload restrictions apply)
        buf.append(String.format("Bytes-0x%08X:", get4ByteInt(cmd, 8)));
        // cms[12] = Mask (Mask to apply while reading; 1 bit per location byte, 0 = skip location, 1 = read location)
        break;
      /*
       * CMD_AVR8_MEMORY_WRITE
       */
      case 0x23:                    // CMD_AVR8_MEMORY_WRITE (Write memory)
        buf.append("CMD_AVR8_MEMORY_WRITE:");
        // cmd[3] = Type (see list in section 7.4 of "EDBG-based Tools Protocols" document)
        buf.append(getMemType(cmd[3]));
        // cmd[4:7] = Start Address (4 byte First address for Write (byte address))
        buf.append(String.format("Start-0x%08X:", get4ByteInt(cmd, 4)));
        // cmd[8:11] = Bytes (4 bytes Number of bytes to access. Payload restrictions apply)
        buf.append(String.format("Bytes-0x%08X:", get4ByteInt(cmd, 8)));
        // cmd[12] = Asynchronous (0 = write first, then reply, 1 = reply first, then write)
        // cmd[13:n] data to write
        break;
      /*
       * CMD_AVR8_CRC
       */
      case 0x24:                    // CMD_AVR8_CRC (Calculate CRC)
        buf.append("CMD_AVR8_CRC:");
        // cmd[3] = Mode (0 = use address range, 1 = application section, 2 = boot section, 3 = entire flash)
        switch(cmd[3]) {
        case 0x00:                  // address range
          buf.append("address range:");
          break;
        case 0x01:                  // application section
          buf.append("application section:");
          break;
        case 0x02:                  // boot section
          buf.append("boot section:");
          break;
        case 0x03:                  // entire flash
          buf.append("entire flash:");
          break;
        }
        // cmd[4:7] = Start Address (4 byte First address for CRC (byte address))
        buf.append(String.format("Start-0x%08X:", get4ByteInt(cmd, 4)));
        // cmd[8:11] = End Address (4 byte Last address for CRC (byte address))
        buf.append(String.format("End-0x%08X:", get4ByteInt(cmd, 8)));
        break;
      /*
       * CMD_AVR8_RESET
       */
      case 0x30:                    // CMD_AVR8_RESET (Reset the MCU)
        buf.append("CMD_AVR8_RESET:");
        // cmd[3] = Level (1 = stop at boot reset vector, 2 = stop at main address (deprecated))
        if (cmd[3] == 1) {
          buf.append("STOP:");
        } else {
          buf.append(String.format("Err: 0x%02X:", cmd[3]));
        }
        break;
      /*
       * CMD_AVR8_STOP
       */
      case 0x31:                    // CMD_AVR8_STOP (Stop the MCU)
        buf.append("CMD_AVR8_STOP:");
        // cmd[3] = Level (1 = stop immediately, 2 = stop at next symbol (deprecated))
        if (cmd[3] == 1) {
          buf.append("STOP:");
        } else {
          buf.append(String.format("Err: 0x%02X:", cmd[3]));
        }
        break;
      /*
       * CMD_AVR8_RUN
       */
      case 0x32:                    // CMD_AVR8_RUN (Resume execution)
        buf.append("CMD_AVR8_RUN:");
        // no additional parameters after cmd[2]
        break;
      /*
       * CMD_AVR8_RUN_TO_ADDRESS
       */
      case 0x33:                    // CMD_AVR8_RUN_TO_ADDRESS (Resume with breakpoint)
        buf.append("CMD_AVR8_RUN_TO_ADDRESS:");
        // cmd[3:6] = Address (4 byte address to run to (is word address, but show byte address))
        buf.append(String.format("Address-0x%08X:", get4ByteInt(cmd, 3) * 2));
        break;
      /*
       * CMD_AVR8_STEP
       */
      case 0x34:                    // CMD_AVR8_STEP (Single step)
        buf.append("CMD_AVR8_STEP:");
        // cmd[3] = Level (1 = instruction level step, 2 = statement level step (deprecated))
        // cmd[4] = Mode (0 =  step over (deprecated), 1 = step into, 2 = step out (deprecated))
        if (cmd[3] == 1 && cmd[4] == 1) {
          buf.append("STEP:");
        } else {
          buf.append(String.format("Err: 0x%02X:0x%02X:", cmd[3], cmd[4]));
        }
        break;
      /*
       * CMD_AVR8_PC_READ
       */
      case 0x35:                    // CMD_AVR8_PC_READ (Read PC)
        buf.append("CMD_AVR8_PC_READ:");
        // no additional parameters after cmd[2]
        break;
      /*
       * CMD_AVR8_PC_WRITE
       */
      case 0x36:                    // CMD_AVR8_PC_WRITE (Write PC)
        buf.append("CMD_AVR8_PC_WRITE:");
        // cmd[3:6] = Address (4 byte Program Counter value (is word address, but show byte address))
        buf.append(String.format("Address-0x%08X:", get4ByteInt(cmd, 3) * 2));
        break;
      /*
       * CMD_AVR8_HW_BREAK_SET
       */
      case 0x40:                    // CMD_AVR8_HW_BREAK_SET (Set hardware breakpoint)
        buf.append("CMD_AVR8_HW_BREAK_SET:");
        // cmd[3] = Type (1 = program break)
        buf.append("T1:");
        // cmd[4] = Number (Breakpoint number to set (1, 2, or 3))
        buf.append(String.format("BRK%d:",cmd[4] ));
        // cmd[5:8] = Address (4 byte word address / value to use)
        buf.append(String.format("Address-0x%08X:", get4ByteInt(cmd, 5)));
        // cmd[9] = Mode (3 = program break)
        buf.append("program break:");
        break;
      /*
       * CMD_AVR8_HW_BREAK_CLEAR
       */
      case 0x41:                    // CMD_AVR8_HW_BREAK_CLEAR (Clear hardware breakpoint)
        buf.append("CMD_AVR8_HW_BREAK_CLEAR:");
        // cmd[3] = Number (Breakpoint number to clear (1, 2, or 3))
        buf.append(String.format("BRK%d:",cmd[3] ));
        break;
      /*
       * CMD_AVR8_SW_BREAK_SET
       */
      case 0x43:                    // CMD_AVR8_SW_BREAK_SET (Set software breakpoint)
        buf.append("CMD_AVR8_SW_BREAK_SET:");
        // see section 7.1.27 of "EDBG-based Tools Protocols" document
        break;
      /*
       * CMD_AVR8_SW_BREAK_CLEAR
       */
      case 0x44:                    // CMD_AVR8_SW_BREAK_CLEAR (Clear software breakpoint)
        buf.append("CMD_AVR8_SW_BREAK_CLEAR:");
        // see section 7.1.28 of "EDBG-based Tools Protocols" document
        break;
      case 0x45:                    // CMD_AVR8_SW_BREAK_CLEAR_ALL (Clear all software breakpoints)
        buf.append("CMD_AVR8_SW_BREAK_CLEAR_ALL:");
        // see section 7.1.29 of "EDBG-based Tools Protocols" document
        break;
      /*
       * CMD_AVR8_PAGE_ERASE
       */
      case 0x50:                    // CMD_AVR8_PAGE_ERASE (Erase page)
        buf.append("CMD_AVR8_PAGE_ERASE:");
        // cmd[4:7] = Start Address (4 byte Start address (byte address) of flash page to erase)
        buf.append(String.format("Start-0x%08X:", get4ByteInt(cmd, 4)));
        break;
      default:
        buf.append(String.format("Err: 0x%02X:", cmd[1]));
      }
      break;
    //
    //  AVR32GENERIC (not implemented here)
    //
    case 0x13:                      // AVR32GENERIC (not implemented here)
      buf.append("AVR32GENERIC:Err: not implemented");
      break;
    //
    //  TPI (not implemented here)
    //
    case 0x14:                      // TPI (not implemented here)
      buf.append("TPI:Err: not implemented");
      break;
    //
    //  EDBG_CTRL
    //
    case 0x20:                      // EDBG_CTRL
      buf.append("EDBG_CTRL:");
      switch (cmd[1]) {
      case 0x00:                    // CMD_EDBG_QUERY
        buf.append("CMD_EDBG_QUERY:");
        switch (cmd[3]) {
        case 0x00:                  // context EDBG_QUERY_COMMANDS
          buf.append("EDBG_QUERY_COMMANDS:");
          // return LIST of bytes
          break;
        default:
          buf.append(String.format("Err: 0x%02X:", cmd[3]));
          break;
        }
        break;
      case 0x01:                    // CMD_EDBG_SET Command
        buf.append("SET:");
        switch (cmd[3]) {
        case 0x00:                  // EDBG_CONTEXT_CONTROL
          buf.append("EDBG_CONTEXT_CONTROL:");
          switch (cmd[4]) {         // Address
          case 0x00:                // EDBG_CTRL_LED_USAGE (RW 1 byte)
            buf.append("EDBG_CTRL_LED_USAGE:");
            buf.append(String.format("VAL  =%d:", cmd[6]));
           break;
          case 0x01:                // EDBG_CTRL_EXT_PROG  (RW 1 byte)
            buf.append("EDBG_CTRL_EXT_PROG:");
            buf.append(String.format("VAL = %d:", cmd[6]));
            break;
          case 0x10:                // EDBG_CTRL_TARGET_POWER (RW 1 byte)
            buf.append("EDBG_CTRL_TARGET_POWER:");
            buf.append(String.format("VAL = %d:", cmd[6]));
            break;
          default:
            buf.append(String.format("Err: 0x%02X:", cmd[4]));
            break;
          }
          break;
        case 0x10:                  // EDBG_CONTEXT_CONFIG0
          buf.append("EDBG_CONTEXT_CONFIG0:");
          // Note: must be more here, but document doesn't say...
          break;
        case 0x11:                  // EDBG_CONTEXT_CONFIG1
          buf.append("EDBG_CONTEXT_CONFIG1:");
          // Note: must be more here, but document doesn't say...
          break;
        default:
          buf.append(String.format("Err: 0x%02X:", cmd[3]));
          break;
        }
        break;
      case 0x02:                    // CMD_EDBG_GET Command
        buf.append("CMD_EDBG_GET:");
        switch (cmd[3]) {
        case 0x00:                  // EDBG_CONTEXT_CONTROL
          buf.append("EDBG_CONTEXT_CONTROL:");
          switch (cmd[4]) {         // Address
          case 0x00:                // EDBG_CTRL_LED_USAGE (RW 1 byte)
            buf.append("EDBG_CTRL_LED_USAGE:");
            break;
          case 0x01:                // EDBG_CTRL_EXT_PROG  (RW 1 byte)
            buf.append("EDBG_CTRL_EXT_PROG:");
            break;
          case 0x10:                // EDBG_CTRL_TARGET_POWER (RW 1 byte)
            buf.append("EDBG_CTRL_TARGET_POWER:");
            break;
          default:
            buf.append(String.format("Err: 0x%02X:", cmd[4]));
            break;
          }
          break;
        case 0x10:                  // EDBG_CONTEXT_CONFIG0
          buf.append("EDBG_CONTEXT_CONFIG0:");
          buf.append(String.format("Address = 0x%02X", cmd[4]));
          break;
        case 0x11:                  // EDBG_CONTEXT_CONFIG1
          buf.append("EDBG_CONTEXT_CONFIG1:");
          buf.append(String.format("Address = 0x%02X", cmd[4]));
          break;
        default:
          buf.append(String.format("Err: 0x%02X:", cmd[3]));
          break;
        }
        break;
      default:
        buf.append(String.format("Err: 0x%02X:", cmd[1]));
        break;
      }
      break;
    }
    return buf.toString();
  }

  private static int get2ByteInt (byte[] cmd, int off) {
    // Convert 2 byte, little endian value to int
    return ((int) cmd[off] & 0xFF) + (((int) cmd[off + 1] & 0xFF) << 8);
  }

    private static int get4ByteInt (byte[] cmd, int off) {
      // Convert 4 byte, little endian value to int
      return ((int) cmd[off] & 0xFF) + (((int) cmd[off + 1] & 0xFF) << 8) + (((int) cmd[off + 2] & 0xFF) << 16) +
             (((int) cmd[off + 3] & 0xFF) << 24);
    }


    private static String getMemType (byte type) {
    switch (type) {
    case 0x20:                      // MEMTYPE_SRAM (SRAM)
      return "MEMTYPE_SRAM:";
    case 0x22:                      // MEMTYPE_EEPROM ()
      return "MEMTYPE_EEPROM:";
    case (byte) 0xA0:               // MEMTYPE_SPM (Flash memory in a debug session)
      return "MEMTYPE_SPM:";
    case  (byte) 0xB0:              // MEMTYPE_FLASH_PAGE (Flash memory programming)
      return "MEMTYPE_FLASH_PAGE:";
    case  (byte) 0xB1:              // MEMTYPE_EEPROM_PAGE (EEPROM memory pages)
      return "MEMTYPE_EEPROM_PAGE:";
    case  (byte) 0xB2:              // MEMTYPE_FUSES (Fuse memory)
      return "MEMTYPE_FUSES:";
    case  (byte) 0xB3:              // MEMTYPE_LOCKBITS (Lock bits)
      return "MEMTYPE_LOCKBITS:";
    case  (byte) 0xB4:              // MEMTYPE_SIGNATURE (Device signature)
      return "MEMTYPE_SIGNATURE:";
    case  (byte) 0xB5:              // MEMTYPE_OSCCAL (Oscillator calibration values)
      return "MEMTYPE_OSCCAL:";
    case  (byte) 0xB8:              // MEMTYPE_REGFILE (Register file)
      return "MEMTYPE_REGFILE:";
    case  (byte) 0xC0:              // MEMTYPE_APPL_FLASH (Application section flash)
      return "MEMTYPE_APPL_FLASH:";
    case  (byte) 0xC1:              // MEMTYPE_BOOT_FLASH (Boot section flash)
      return "MEMTYPE_BOOT_FLASH:";
    case  (byte) 0xC2:              // MEMTYPE_APPL_FLASH_ATOMIC (Application page with auto-erase)
      return "MEMTYPE_APPL_FLASH_ATOMIC:";
    case  (byte) 0xC3:              // MEMTYPE_BOOT_FLASH_ATOMIC (Boot page with auto-erase)
      return "MEMTYPE_BOOT_FLASH_ATOMIC:";
    case  (byte) 0xC4:              // MEMTYPE_EEPROM_ATOMIC (EEPROM page with auto-erase)
      return "MEMTYPE_EEPROM_ATOMIC:";
    case  (byte) 0xC5:              // MEMTYPE_USER_SIGNATURE (User signature section)
      return "MEMTYPE_USER_SIGNATURE:";
    case  (byte) 0xC6:              // MEMTYPE_CALIBRATION_SIGNATURE (Calibration section)
      return "MEMTYPE_CALIBRATION_SIGNATURE:";
    }
    return "unknown";
  }

  public static String getFailMessage (byte context, byte code) {
    switch (context) {
    // - - - - - - - - - - - - - - - - - - - - - - -
    case 0x00:                        // DISCOVERY
      return "DISCOVERY:code=" + code;
    // - - - - - - - - - - - - - - - - - - - - - - -
    case 0x01:                        // HOUSEKEEPING
      return "HOUSEKEEPING:code=" + code;
    // - - - - - - - - - - - - - - - - - - - - - - -
    case 0x11:                        // AVRISP (not implemented here)
      return "AVRISP (not implemented here):";
    // - - - - - - - - - - - - - - - - - - - - - - -
    case 0x12:                        // AVR8GENERIC
      switch (code) {
      case 0x00:                      // AVR8_FAILURE_OK (All OK)
        return "AVR8_FAILURE_OK:";
      case 0x10:                      // AVR8_FAILURE_DW_PHY_ERROR (debugWIRE physical error)
        return "AVR8_FAILURE_DW_PHY_ERROR:";
      case 0x11:                      // AVR8_FAILURE_JTAGM_INIT_ERROR (JTAGM failed to initialise)
        return "AVR8_FAILURE_JTAGM_INIT_ERROR:";
      case 0x12:                      // AVR8_FAILURE_JTAGM_ERROR (JTAGM did something strange)
        return "AVR8_FAILURE_JTAGM_ERROR:";
      case 0x13:                      // AVR8_FAILURE_JTAG_ERROR (JTAG low level error)
        return "AVR8_FAILURE_JTAG_ERROR:";
      case 0x14:                      // AVR8_FAILURE_JTAGM_VERSION (Unsupported version of JTAGM)
        return "AVR8_FAILURE_JTAGM_VERSION:";
      case 0x15:                      // AVR8_FAILURE_JTAGM_TIMEOUT (JTAG master timed out)
        return "AVR8_FAILURE_JTAGM_TIMEOUT:";
      case 0x16:                      // AVR8_FAILURE_JTAG_BIT_BANGER_TIMEOUT (JTAG bit banger timed out)
        return "AVR8_FAILURE_JTAG_BIT_BANGER_TIMEOUT:";
      case 0x17:                      // AVR8_FAILURE_PARITY_ERROR (Parity error in received data)
        return "AVR8_FAILURE_PARITY_ERROR:";
      case 0x18:                      // AVR8_FAILURE_EB_ERROR (Did not receive EMPTY byte)
        return "AVR8_FAILURE_EB_ERROR:";
      case 0x19:                      // AVR8_FAILURE_PDI_TIMEOUT (PDI physical timed out)
        return "AVR8_FAILURE_PDI_TIMEOUT:";
      case 0x1A:                      // AVR8_FAILURE_COLLISION (Collision on physical level)
        return "AVR8_FAILURE_COLLISION:";
      case 0x1B:                      // AVR8_FAILURE_PDI_ENABLE (PDI enable failed)
        return "AVR8_FAILURE_PDI_ENABLE:";
      case 0x20:                      // AVR8_FAILURE_NO_DEVICE_FOUND (devices == 0!)
        return "AVR8_FAILURE_NO_DEVICE_FOUND:";
      case 0x21:                      // AVR8_FAILURE_CLOCK_ERROR (Failure when increasing baud)
        return "AVR8_FAILURE_CLOCK_ERROR:";
      case 0x22:                      // AVR8_FAILURE_NO_TARGET_POWER (Target power not detected)
        return "AVR8_FAILURE_NO_TARGET_POWER:";
      case 0x23:                      // AVR8_FAILURE_NOT_ATTACHED (Must run attach command first)
        return "AVR8_FAILURE_NOT_ATTACHED:";
      case 0x24:                      // AVR8_FAILURE_DAISY_CHAIN_TOO_LONG (Devices > 31)
        return "AVR8_FAILURE_DAISY_CHAIN_TOO_LONG:";
      case 0x25:                      // AVR8_FAILURE_DAISY_CHAIN_CONFIG (Configured device bits do not add
                                      // up to detected bits)
        return "AVR8_FAILURE_DAISY_CHAIN_CONFIG:";
      case 0x31:                      // AVR8_FAILURE_INVALID_PHYSICAL_STATE (Physical not activated)
        return "AVR8_FAILURE_INVALID_PHYSICAL_STATE:";
      case 0x32:                      // AVR8_FAILURE_ILLEGAL_STATE (Illegal run / stopped state)
        return "AVR8_FAILURE_ILLEGAL_STATE:";
      case 0x33:                      // AVR8_FAILURE_INVALID_CONFIG (Invalid config for activate phy)
        return "AVR8_FAILURE_INVALID_CONFIG:";
      case 0x34:                      // AVR8_FAILURE_INVALID_MEMTYPE (Not a valid memtype)
        return "AVR8_FAILURE_INVALID_MEMTYPE:";
      case 0x35:                      // AVR8_FAILURE_INVALID_SIZE (Too many or too few bytes)
        return "AVR8_FAILURE_INVALID_SIZE:";
      case 0x36:                      // AVR8_FAILURE_INVALID_ADDRESS (Asked for a bad address)
        return "AVR8_FAILURE_INVALID_ADDRESS:";
      case 0x37:                      // AVR8_FAILURE_INVALID_ALIGNMENT (Asked for badly aligned data)
        return "AVR8_FAILURE_INVALID_ALIGNMENT:";
      case 0x38:                      // AVR8_FAILURE_ILLEGAL_MEMORY_RANGE (Address not within legal range)
        return "AVR8_FAILURE_ILLEGAL_MEMORY_RANGE:";
      case 0x39:                      // AVR8_FAILURE_ILLEGAL_VALUE (Illegal value given)
        return "AVR8_FAILURE_ILLEGAL_VALUE:";
      case 0x3A:                      // AVR8_FAILURE_ILLEGAL_ID (Illegal target ID)
        return "AVR8_FAILURE_ILLEGAL_ID:";
      case 0x3B:                      // AVR8_FAILURE_INVALID_CLOCK_SPEED (Clock value out of range)
        return "AVR8_FAILURE_INVALID_CLOCK_SPEED:";
      case 0x3C:                      // AVR8_FAILURE_TIMEOUT (q timeout occurred)
        return "AVR8_FAILURE_TIMEOUT:";
      case 0x3D:                      // AVR8_FAILURE_ILLEGAL_OCD_STATUS (Read an illegal OCD status)
        return "AVR8_FAILURE_ILLEGAL_OCD_STATUS:";
      case 0x40:                      // AVR8_FAILURE_NVM_ENABLE (NVM failed to be enabled)
        return "AVR8_FAILURE_NVM_ENABLE:";
      case 0x41:                      // AVR8_FAILURE_NVM_DISABLE (NVM failed to be disabled)
        return "AVR8_FAILURE_NVM_DISABLE:";
      case 0x42:                      // AVR8_FAILURE_CS_ERROR (Illegal control/status bits)
        return "AVR8_FAILURE_CS_ERROR:";
      case 0x43:                      // AVR8_FAILURE_CRC_FAILURE (CRC mismatch)
        return "AVR8_FAILURE_CRC_FAILURE:";
      case 0x44:                      // AVR8_FAILURE_OCD_LOCKED (Failed to enable OCD)
        return "AVR8_FAILURE_OCD_LOCKED:";
      case 0x50:                      // AVR8_FAILURE_NO_OCD_CONTROL (Device is not under control)
        return "AVR8_FAILURE_NO_OCD_CONTROL:";
      case 0x60:                      // AVR8_FAILURE_PC_READ_FAILED (Error when reading PC)
        return "AVR8_FAILURE_PC_READ_FAILED:";
      case 0x61:                      // AVR8_FAILURE_REGISTER_READ_FAILED (Error when reading register)
        return "AVR8_FAILURE_REGISTER_READ_FAILED:";
      case 0x70:                      // AVR8_FAILURE_READ_ERROR (Error while reading)
        return "AVR8_FAILURE_READ_ERROR:";
      case 0x71:                      // AVR8_FAILURE_WRITE_ERROR (Error while writing)
        return "AVR8_FAILURE_WRITE_ERROR:";
      case 0x72:                      // AVR8_FAILURE_WRITE_TIMEOUT (Timeout while reading)
        return "AVR8_FAILURE_WRITE_TIMEOUT:";
      case (byte) 0x80:               // AVR8_FAILURE_ILLEGAL_BREAKPOINT (Invalid breakpoint configuration)
        return "AVR8_FAILURE_ILLEGAL_BREAKPOINT:";
      case (byte) 0x81:               // AVR8_FAILURE_TOO_MANY_BREAKPOINTS (Not enough available resources)
        return "AVR8_FAILURE_TOO_MANY_BREAKPOINTS:";
      case (byte) 0x90:               // AVR8_FAILURE_NOT_SUPPORTED (This feature is not available)
        return "AVR8_FAILURE_NOT_SUPPORTED:";
      case (byte) 0x91:               // AVR8_FAILURE_NOT_IMPLEMENTED (Command has not been implemented)
        return "AVR8_FAILURE_NOT_IMPLEMENTED:";
      case (byte) 0xFF:               // AVR8_FAILURE_UNKNOWN (Disaster)
        return "AVR8_FAILURE_UNKNOWN:";
      }
      break;
    // - - - - - - - - - - - - - - - - - - - - - - -
    case 0x13:                        // AVR32GENERIC (not implemented here)
      return "AVR32GENERIC (not implemented here):";
    // - - - - - - - - - - - - - - - - - - - - - - -
    case 0x14:                        // TPI (not implemented here)
      return "TPI (not implemented here):";
    // - - - - - - - - - - - - - - - - - - - - - - -
    case 0x20:                        // EDBG_CTRL (in progress)
      switch (code) {
      case 0x00:                      // EDBG_FAILED_OK
        return "EDBG_FAILED_OK:";
      case 0x01:                      // EDBG_FAILED_NOT_SUPPORTED
        return "EDBG_FAILED_NOT_SUPPORTED:";
      case 0x10:                      // EDBG_FAILED_ILLEGAL_GPIO_PIN
        return "EDBG_FAILED_ILLEGAL_GPIO_PIN:";
      case 0x11:                      // EDBG_FAILED_ILLEGAL_GPIO_MODE
        return "EDBG_FAILED_ILLEGAL_GPIO_MODE:";
      case 0x12:                      // EDBG_FAILED_ILLEGAL_VOLTAGE_RANGE
        return "EDBG_FAILED_ILLEGAL_VOLTAGE_RANGE:";
      case 0x13:                      // EDBG_FAILED_ILLEGAL_INTERVAL
        return "EDBG_FAILED_ILLEGAL_INTERVAL:";
      case 0x14:                      // EDBG_FAILED_ILLEGAL_MAX_THRESHOLD
        return "EDBG_FAILED_ILLEGAL_MAX_THRESHOLD:";
      case 0x15:                      // EDBG_FAILED_ILLEGAL_MIN_THRESHOLD
        return "EDBG_FAILED_ILLEGAL_MIN_THRESHOLD:";
      case 0x16:                      // EDBG_FAILED_ILLEGAL_ACTION
        return "EDBG_FAILED_ILLEGAL_ACTION:";
      case 0x17:                      // EDBG_FAILED_ILLEGAL_FREQUENCY
        return "EDBG_FAILED_ILLEGAL_FREQUENCY:";
      case 0x18:                      // EDBG_FAILED_ILLEGAL_MODE
        return "EDBG_FAILED_ILLEGAL_MODE:";
      case 0x19:                      // EDBG_FAILED_ILLEGAL_FLAGS
        return "EDBG_FAILED_ILLEGAL_FLAGS:";
      case 0x20:                      // EDBG_FAILED_FLASH_WRITE
        return "EDBG_FAILED_FLASH_WRITE:";
      case 0x30:                      // EDBG_FAILED_OVERFLOW
        return "EDBG_FAILED_OVERFLOW:";
      case (byte) 0xFF:               // EDBG_FAILED_UNKNOWN
        return "EDBG_FAILED_UNKNOWN:";
      }
      break;
    }
    return "error";
  }

    static void printEdbgCommands (byte[] response) {
      for (byte b : response) {
        switch (b) {
        case 0x00:
          System.out.printf("  0x%02X = CMD_EDBG_QUERY\n", b);
          break;
        case 0x01:
          System.out.printf("  0x%02X = CMD_EDBG_SET\n", b);
          break;
        case 0x02:
          System.out.printf("  0x%02X = CMD_EDBG_GET\n", b);
          break;
        default:
          System.out.printf("  0x%02X = Unknown\n", b);
          break;
        }
      }
    }

    static void printAvr8Commands (byte[] response) {
      for (byte b : response) {
        switch (b) {
        case 0x00:
          System.out.printf("  0x%02X = CMD_AVR8_QUERY\n", b);
          break;
        case 0x01:
          System.out.printf("  0x%02X = CMD_AVR8_SET\n", b);
          break;
        case 0x02:
          System.out.printf("  0x%02X = CMD_AVR8_GET\n", b);
          break;
        case 0x10:
          System.out.printf("  0x%02X = CMD_AVR8_ACTIVATE_PHYSICAL\n", b);
          break;
        case 0x11:
          System.out.printf("  0x%02X = CMD_AVR8_DEACTIVATE_PHYSICAL\n", b);
          break;
        case 0x12:
          System.out.printf("  0x%02X = CMD_AVR8_GET_ID\n", b);
          break;
        case 0x13:
          System.out.printf("  0x%02X = CMD_AVR8_ATTACH\n", b);
          break;
        case 0x14:
          System.out.printf("  0x%02X = CMD_AVR8_DETACH\n", b);
          break;
        case 0x15:
          System.out.printf("  0x%02X = CMD_AVR8_PROG_MODE_ENTER\n", b);
          break;
        case 0x16:
          System.out.printf("  0x%02X = CMD_AVR8_PROG_MODE_LEAVE\n", b);
          break;
        case 0x17:
          System.out.printf("  0x%02X = CMD_AVR8_DISABLE_DEBUGWIRE\n", b);
          break;
        case 0x20:
          System.out.printf("  0x%02X = CMD_AVR8_ERASE\n", b);
          break;
        case 0x21:
          System.out.printf("  0x%02X = CMD_AVR8_MEMORY_READ\n", b);
          break;
        case 0x22:
          System.out.printf("  0x%02X = CMD_AVR8_MEMORY_READ_MASKED\n", b);
          break;
        case 0x23:
          System.out.printf("  0x%02X = CMD_AVR8_MEMORY_WRITE\n", b);
          break;
        case 0x24:
          System.out.printf("  0x%02X = CMD_AVR8_CRC\n", b);
          break;
        case 0x30:
          System.out.printf("  0x%02X = CMD_AVR8_RESET\n", b);
          break;
        case 0x31:
          System.out.printf("  0x%02X = CMD_AVR8_STOP\n", b);
          break;
        case 0x32:
          System.out.printf("  0x%02X = CMD_AVR8_RUN\n", b);
          break;
        case 0x33:
          System.out.printf("  0x%02X = CMD_AVR8_RUN_TO_ADDRESS\n", b);
          break;
        case 0x34:
          System.out.printf("  0x%02X = CMD_AVR8_STEP\n", b);
          break;
        case 0x35:
          System.out.printf("  0x%02X = CMD_AVR8_PC_READ\n", b);
          break;
        case 0x36:
          System.out.printf("  0x%02X = CMD_AVR8_PC_WRITE\n", b);
          break;
        case 0x40:
          System.out.printf("  0x%02X = CMD_AVR8_HW_BREAK_SET\n", b);
          break;
        case 0x41:
          System.out.printf("  0x%02X = CMD_AVR8_HW_BREAK_CLEAR\n", b);
          break;
        case 0x43:
          System.out.printf("  0x%02X = CMD_AVR8_SW_BREAK_SET\n", b);
          break;
        case 0x44:
          System.out.printf("  0x%02X = CMD_AVR8_SW_BREAK_CLEAR\n", b);
          break;
        case 0x45:
          System.out.printf("  0x%02X = CMD_AVR8_SW_BREAK_CLEAR_ALL\n", b);
          break;
        case 0x50:
          System.out.printf("  0x%02X = CMD_AVR8_PAGE_ERASE\n", b);
          break;
        default:
          System.out.printf("  0x%02X = Unknown\n", b);
          break;
        }
      }
    }
  }
