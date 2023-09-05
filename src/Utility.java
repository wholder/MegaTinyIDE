import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.CRC32;

class Utility {
  private static final String   fileSep =  System.getProperty("file.separator");
  private static final Pattern  pat1 = Pattern.compile("(\\*\\[(.*?)]\\*)", Pattern.DOTALL | Pattern.MULTILINE);

  static String createDir (String path) throws IOException {
    File base = (new File(path));
    if (!base.exists() && !base.mkdirs()) {
      throw new IOException("createDir() unable to create directory: " + base.getAbsolutePath());
    }
    return base.getAbsolutePath() + fileSep;
  }

  static String parseClockSpeed (String clock) {
    clock = clock.replaceAll(",", "").toLowerCase().trim();
    if (clock.endsWith("mhz")) {
      return Integer.toString((int) (Double.parseDouble(clock.substring(0, clock.length() - 3).trim()) * 1000000));
    } else if (clock.endsWith("khz")) {
      return Integer.toString((int) (Double.parseDouble(clock.substring(0, clock.length() - 3).trim()) * 1000));
    } else if (clock.endsWith("hz")) {
      return clock.substring(0, clock.length() - 2).trim();
    } else {
      return clock;
    }
  }

  /**
   * Reformat String to remove all whitespace characters
   * @param text Input text
   * @return Reformatted output
   */
  static String removeWhitespace (String text) {
    StringTokenizer tok = new StringTokenizer(text);
    StringBuilder buf = new StringBuilder();
    while (tok.hasMoreTokens()) {
      String line = tok.nextToken();
      buf.append(line);
    }
    return buf.toString().trim();
  }

  /**
   * Reformat String to reduce all whitespace to a single space
   * @param text Input text
   * @return Reformatted output
   */
  static String condenseWhitespace (String text) {
    StringTokenizer tok = new StringTokenizer(text);
    StringBuilder buf = new StringBuilder();
    while (tok.hasMoreTokens()) {
      String line = tok.nextToken();
      buf.append(line);
      buf.append(' ');
    }
    return buf.toString().trim();
  }

  static String condenseTabs (String text) {
    StringBuilder tmp = new StringBuilder();
    boolean lastTab = false;
    for (char cc : text.toCharArray()) {
      boolean isTab = cc == '\t';
      if (isTab && lastTab) {
        continue;
      }
      tmp.append(cc);
      lastTab = isTab;
    }
    return tmp.toString();
  }

  public static String escapeHTML (String s) {
    StringBuilder out = new StringBuilder(Math.max(16, s.length()));
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c > 127 || c == '"' || c == '\'' || c == '<' || c == '>' || c == '&') {
        out.append("&#");
        out.append((int) c);
        out.append(';');
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }

  static void copyFile (File src, String dest) throws IOException {
    FileInputStream fis = new FileInputStream(src);
    byte[] data = new byte[fis.available()];
    if (fis.read(data) != data.length) {
      throw new IOException("copyFile() not all bytes read from file: " + src.getName());
    }
    fis.close();
    FileOutputStream fOut = new FileOutputStream(dest);
    fOut.write(data);
    fOut.close();
  }

  static void saveFile (File file, String text) {
    try {
      FileOutputStream out = new FileOutputStream(file);
      out.write(text.getBytes(StandardCharsets.UTF_8));
      out.close();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  static void saveFile (String file, String text) throws Exception {
    FileOutputStream fOut = new FileOutputStream(file);
    fOut.write(text.getBytes(StandardCharsets.UTF_8));
    fOut.close();
  }

  static String getFile (File file) throws IOException {
    FileInputStream fis = new FileInputStream(file);
    byte[] data = new byte[fis.available()];
    if (fis.read(data) != data.length) {
      throw new IOException("getFile() not all bytes read from file: " + file.getName());
    }
    fis.close();
    return new String(data, StandardCharsets.UTF_8);
  }

  static String getFile (String file) throws IOException {
    InputStream fis;
    if (file.startsWith("res:")) {
      fis = Utility.class.getClassLoader().getResourceAsStream(file.substring(4));
    } else {
      fis = Files.newInputStream(Paths.get(file));
    }
    if (fis != null) {
      byte[] data = new byte[fis.available()];
      if (fis.read(data) != data.length) {
        throw new IOException("getFile() not all bytes read from file: " + file);
      }
      fis.close();
      return new String(data, StandardCharsets.UTF_8);
    }
    throw new IllegalStateException("getFile() " + file + " not found");
  }

  /**
   * Recursively remove files and directories from directory "dir"
   * @param dir starting directory
   */
  static void removeFiles (File dir) throws IOException {
    final File[] files = dir.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.isDirectory()) {
          removeFiles(file);
          if (!file.delete()) {
            throw new IOException("removeFiles() unable to delete directory: " + file.getName());
          }
        } else {
          if (!file.delete()) {
            throw new IOException("removeFiles() unable to delete file: " + file.getName());
          }
        }
      }
    }
  }

  static Map<String,String> getResourceMap (String file) throws IOException {
    Properties prop = new Properties();
    InputStream fis = Utility.class.getClassLoader().getResourceAsStream(file);
    if (fis != null) {
      prop.load(fis);
      fis.close();
    }
    return propertiesToMap(prop);
  }

  static Map<String,String> getResourceMap (URL url) throws IOException {
    Properties prop = new Properties();
    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
    InputStream fis = conn.getInputStream();
    prop.load(fis);
    fis.close();
    return propertiesToMap(prop);
  }

  private static Map<String,String> propertiesToMap (Properties prop) {
    Map<String,String> map = new TreeMap<>();
    for (String key : prop.stringPropertyNames()) {
      map.put(key, prop.getProperty(key));
    }
    return map;
  }

  static Map<String,String> parseParms (String parms) {
    Map<String,String> map = new HashMap<>();
    if (parms != null) {
      String[] items = parms.split(",");
      for (String item : items) {
        String[] parts = item.trim().split("=");
        if (parts.length == 2) {
          map.put(parts[0].trim(), parts[1].trim());
        }
      }
    }
    return map;
  }

  static String getFontStyle (Font font) {
    String fName = font.getFontName();
    int size = font.getSize();
    return "style=\"font-family:" + fName + ";font-size:" + size + ";margin: 1em 0;display: block;\"";
  }

  static byte[] getResource (String file) throws IOException {
    InputStream fis = MarkupView.class.getClassLoader().getResourceAsStream(file);
    if (fis != null) {
      byte[] data = new byte[fis.available()];
      if (fis.read(data) != data.length) {
        throw new IOException("getResource() not all bytes read from file: " + file);
      }
      fis.close();
      return data;
    }
    throw new IllegalStateException("MarkupView.getResource() " + file + " not found");
  }

  static String getResourceAsString (String file) throws IOException {
    return new String(getResource(file));
  }

  static String[] arrayFromText (String file) {
    try {
      String list = getResourceAsString(file);
      return list.split("\n");
    } catch (IOException ex) {
      ex.printStackTrace();
      return new String[0];
    }
  }

  interface TagCallback {
    String getTag (String name, String parm, Map<String,String> tags);
  }

  static String replaceTags (String src, Map<String,String> tags) {
    return replaceTags(src, tags, null);
  }

  static String replaceTags (String src, Map<String,String> tags, TagCallback callback) {
    Matcher mat1 = pat1.matcher(src);
    StringBuffer buf = new StringBuffer();
    while (mat1.find()) {
      String tag = mat1.group(2);
      if (tag.contains("*{") && tag.contains("}*")) {
        tag = tag.replaceAll("\\*\\{", "*[");
        tag = tag.replaceAll("}\\*", "]*");
        tag = replaceTags(tag, tags, callback);
      }
      String parm = null;
      int idx = tag.indexOf(':');
      if (idx > 0) {
        parm = tag.substring((idx + 1));
        tag = tag.substring(0, idx);
      }
      String rep = tags.get(tag);
      if (rep != null) {
        try {
          mat1.appendReplacement(buf, Matcher.quoteReplacement(rep));
        } catch (Exception ex) {
          throw (new IllegalStateException("tag = '" + tag + "'. rep = '" + rep + "'"));
        }
      } else if (callback != null && (rep = callback.getTag(tag, parm, tags)) != null) {
        mat1.appendReplacement(buf, Matcher.quoteReplacement(rep));
      } else {
        throw new IllegalStateException("Utility.replaceTags() Tag '" + tag + "' not defined");
      }
    }
    mat1.appendTail(buf);
    return buf.toString();
  }

  static String runCmd (Process proc) {
    return Stream.of(proc.getErrorStream(), proc.getInputStream()).parallel().map((InputStream isForOutput) -> {
      StringBuilder output = new StringBuilder();
      try (
        BufferedReader br = new BufferedReader(new InputStreamReader(isForOutput))) {
        String line;
        while ((line = br.readLine()) != null) {
          output.append(line);
          output.append("\n");
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return output;
    }).collect(Collectors.joining());
  }

  private static void copyResourceToDir (String fName, String tmpDir) throws IOException {
    InputStream fis = Utility.class.getClassLoader().getResourceAsStream(fName);
    tmpDir = tmpDir.endsWith("/") ? tmpDir : tmpDir + "/";
    if (fis != null) {
      File path = new File(tmpDir);
      if (!path.exists() && !path.mkdirs()) {
        throw new IOException("copyResourceToDir() unable to create path: " + path.getAbsolutePath());
      }
      byte[] data = new byte[fis.available()];
      if (fis.read(data) != data.length) {
        throw new IOException("getFile() not all bytes read from file: " + path.getPath());
      }
      fis.close();
      File file = new File(tmpDir + (new File(fName)).getName());
      FileOutputStream fOut = new FileOutputStream(file);
      fOut.write(data);
      fOut.close();
    } else {
      throw new IllegalStateException("copyResourceToDir('" + fName + "', '" + tmpDir + "') " + " unable to copy");
    }
  }

  static void copyResourcesToDir (String base, String tmpDir) throws URISyntaxException, IOException {
    java.nio.file.FileSystem fileSystem = null;
    if (base != null) {
      try {
        URL url = Utility.class.getResource(base);
        if (url != null) {
          URI uri = url.toURI();
          Path myPath;
          if (uri.getScheme().equals("jar")) {
            fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
            myPath = fileSystem.getPath("/" + base);
          } else {
            myPath = Paths.get(uri);
          }
          Stream<Path> walk = Files.walk(myPath, FileVisitOption.FOLLOW_LINKS);
          for (Iterator<Path> it = walk.iterator(); it.hasNext(); ) {
            Path item = it.next();
            String fName = item.getFileName().toString();
            if (fName.contains(".")) {
              String srcFile = base + item.toString().substring(myPath.toString().length());
              String dstBase = Paths.get(srcFile).getParent().toString().substring(base.length());
              dstBase = dstBase.startsWith("/") ? dstBase.substring(1) : dstBase;
              String dstPath = tmpDir + dstBase;
              copyResourceToDir(srcFile, dstPath);
            }
          }
        }
      } finally {
        if (fileSystem != null) {
          fileSystem.close();
        }
      }
    }
  }

  static List<String> processIncludes (String srcDir, String dir, String file) throws IOException {
    // Scan generated dependency file for include files that also need to be compiled
    List<String> dependencies = new ArrayList<>();
    String buf = Utility.getFile(dir + file);
    StringTokenizer tok = new StringTokenizer(buf, "\n");
    while (tok.hasMoreElements()) {
      String line = tok.nextToken().trim();
      while (line.endsWith("\\") && tok.hasMoreElements()) {
        line = line.substring(0, line.length() - 1) + tok.nextToken();
      }
      line = line.replace(srcDir, "");
      int idx = line.indexOf(":");
      if (idx > 0) {
        line = line.substring(idx + 1).trim();
      }
      String[] parts = line.split("\\s.");
      for (String part : parts) {
        String[] tmp = part.split("\\.");
        if (tmp.length > 1 && "h".equalsIgnoreCase(tmp[1])) {
          dependencies.add(part);
        }
      }
    }
    return dependencies;
  }

  /**
   * Adds path + filename to CRC32 crc
   */
  private static void crcFilename (Path path, CRC32 crc) {
    String file = path.getFileName().toString();
    if (!".DS_Store".equals(file)) {
      String val = path.toString();
      crc.update(val.getBytes(StandardCharsets.UTF_8));
    }
  }

  /**
   * Computes crc for tree of filenames and paths
   * @param base base of file tree
   */
  static long crcTree (String base) {
    CRC32 crc = new CRC32();
    try {
      Files.walk(Paths.get(base))
          .filter(path -> !Files.isDirectory(path))
          .forEach((path) -> crcFilename(path, crc));
    } catch (Exception ex) {
      return 0;
    }
    return crc.getValue();
  }

  static long crcZipfile (String srcZip) {
    CRC32 crc = new CRC32();
    try {
      InputStream in = Utility.class.getClassLoader().getResourceAsStream(srcZip);
      if (in != null) {
        int size = in.available();
        byte[] data = new byte[size];
        if (in.read(data) != data.length) {
          throw new IOException("crcZipfile() not all bytes read");
        }
        crc.update(data);
      }
    } catch (Exception ex) {
      return 0;
    }
    return crc.getValue();
  }

  /**
   * Create three layer border of outer padding, [border] and inner padding
   * @param border Border between inner and outer
   * @param ohPad left/right padding for outer border
   * @param ovPad top/bottom padding for outer border
   * @param ihPad left/right padding for inner border
   * @param ivPad top/bottom padding for inner border
   * @return constructed compound border
   */
  static Border getBorder (Border border, int ohPad, int ovPad, int ihPad, int ivPad) {
    Border outside = BorderFactory.createEmptyBorder(ovPad, ohPad, ovPad, ohPad);
    Border inner = BorderFactory.createEmptyBorder(ivPad, ihPad, ivPad, ihPad);
    Border temp = BorderFactory.createCompoundBorder(outside, border);
    return BorderFactory.createCompoundBorder(temp, inner);
  }

  /**
   * Create three layer border of outer padding, [border] and inner padding
   * @param border Border between inner and outer
   * @param otPad top padding for outer border
   * @param olPad left padding for outer border
   * @param obPad bottom padding for outer border
   * @param orPad right padding for outer border
   * @param itPad top padding for inner border
   * @param ilPad left padding for inner border
   * @param ibPad bottom padding for inner border
   * @param irPad right padding for inner border
   * @return constructed compound border
   */
  static Border getBorder (Border border, int otPad, int olPad, int obPad, int orPad, int itPad, int ilPad, int ibPad, int irPad) {
    Border outside = BorderFactory.createEmptyBorder(otPad, olPad, obPad, orPad);
    Border inner = BorderFactory.createEmptyBorder(itPad, ilPad, ibPad, irPad);
    Border temp = BorderFactory.createCompoundBorder(outside, border);
    return BorderFactory.createCompoundBorder(temp, inner);
  }
  public static class CodeImage {
    public final byte[]  data;
    public final byte    fuses;

    CodeImage (byte[] data, byte fuses) {
      this.data = data;
      this.fuses = fuses;
    }
  }

  public static CodeImage parseIntelHex (String hex) {
    byte fuses = 0x0F;
    ArrayList<Byte> buf = new ArrayList<>();
    nextLine:
    for (String line : hex.split("\\s")) {
      if (line.startsWith(":")  && line.length() > 11) {
        int state = 0, count = 0, add = 0, chk = 0;
        for (int ii = 1; ii < line.length() - 1; ii += 2) {
          int msn = Utility.fromHex(line.charAt(ii));
          int lsn = Utility.fromHex(line.charAt(ii + 1));
          int val = (msn << 4) + lsn;
          switch (state) {
          case 0:
            count = val;
            chk += val;
            state++;
            break;
          case 1:
          case 2:
            // Collect 2 bytes into a 16 bit address
            add = (add << 8) + val;
            if (add + count > buf.size()) {
              buf.ensureCapacity(add + count);
            }
            chk += val;
            state++;
            break;
          case 3:
            // Check if this is a data record
            chk += val;
            if (val != 0) {
              continue nextLine;
            }
            state++;
            break;
          case 4:
            // Read data bytes
            if (count > 0) {
              buf.add(add++, (byte) val);
              chk += val;
              count--;
            }
            if (count == 0) {
              state++;
            }
            break;
          case 5:
            // Verify checksum
            chk += val;
            if ((chk & 0xFF) != 0) {
              throw new IllegalStateException("Invalid checksum in HEX file");
            }
            continue nextLine;
          }
        }
      } else if (line.startsWith("*")  && line.length() == 2) {
        fuses = (byte) Utility.fromHex(line.charAt(1));
      }
    }
    byte[] code = new byte[buf.size()];
    for (int ii = 0; ii < code.length; ii++) {
      code[ii] = buf.get(ii);
    }
    return new CodeImage(code, fuses);
  }

  public static String toIntelHex (byte[] data) {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    PrintStream pout = new PrintStream(bout);
    for (int address = 0; address < data.length; address += 16) {
      int len = Math.min(data.length - address, 16);
      pout.printf(":%02X%04X00", len, address);
      int check = -(len & 0xFF) -(address & 0xFF) + (-(address >> 8) & 0xFF);
      for (int ii = 0; ii < len; ii++) {
        int cc = data[address + ii] & 0xFF;
        check -= cc;
        pout.printf("%02X", cc);
      }
      pout.printf("%02X\n", check & 0xFF);
    }
    pout.println(":00000001FF");
    return bout.toString();
  }

  public static byte[] trimAvrCode (byte[] data) {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    for (int ii = 0; ii < data.length; ii += 2) {
      int b1 = data[ii] & 0xFF;
      int b2 = data[ii + 1] & 0xFF;
      if (b1 == 0xFF && b2 == 0xFF) {
        break;
      }
      buf.write(b1);
      buf.write(b2);
    }
    return buf.toByteArray();
  }

  /*
  // Test code for printHexAscii()
  public static void main (String[] args) throws Exception {
    byte[] data = new byte[66];
    for (int ii = 0; ii < data.length; ii++) {
      data[ii] = (byte) ii;
    }
    String intelHex = toIntelHex(data);
    System.out.println(intelHex);
  }
  */

  /*
    /Users/wholder/IdeaProjects/MegaTinyIDE/test/blink.hex

    avr-objdump -h /Users/wholder/IdeaProjects/MegaTinyIDE/test/blink.hex
  */

/*
  private static final String toObj = "avr-objdump " +
    "-h " +                       // Disassemble code
    "*[PATH]*";                   // Input file

  private static String decompile (byte[] data) {
    //String intelHex = toIntelHex(data);
    Map<String,String> tags = new HashMap<>();
    tags.put("PATH", "/Users/wholder/IdeaProjects/MegaTinyIDE/test/blink.hex");
    String cmd = Utility.replaceTags(toObj, tags);
    try {
      Process proc = Runtime.getRuntime().exec(cmd);
      String ret = Utility.runCmd(proc);
      int dum = 0;
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    return "";
  }

  public static void main (String[] args) throws Exception {
    String ret = decompile(null);
  }
*/

  static int fromHex (char cc) {
    cc = Character.toUpperCase(cc);
    return cc >= 'A' ? cc - 'A' + 10 : cc - '0';
  }

  public static void printHex (byte[] data) {
    printHex(System.out, "", data);
  }

  public static void printHex (PrintStream out, String indent, byte[] data) {
    for (int ii = 0; ii < data.length; ii++) {
      if ((ii & 0x0F) == 0) {
        out.println(indent);
      }
      out.printf("0x%02X ", data[ii]);
      if (ii == data.length - 1) {
        out.println();
      }
    }
  }

  public static void printHexAscii (byte[] data) {
    int base = 0;
    for (int ii = 0; ii < data.length; ii++) {
      System.out.printf("0x%02X ", data[ii]);
      if (((ii + 1) & 0x0F) == 0 || ii == data.length - 1) {
        System.out.print(" - ");
        for (int jj = 0; jj < 16; jj++) {
          char cc = (char) data[base + jj];
          if (cc >= ' ') {
            System.out.print(cc);
          } else {
            System.out.print('.');
          }
        }
        System.out.println();
        base += 16;
      }
    }
  }

  static Font getCodeFont (int points) {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("win")) {
      return new Font("Consolas", Font.PLAIN, points);
    } else if (os.contains("mac")) {
      return new Font("Menlo", Font.PLAIN, points);
    } else if (os.contains("linux")) {
      return new Font("Monospaced", Font.PLAIN, points);
    } else {
      return new Font("Courier", Font.PLAIN, points);
    }
  }

  private static int getInt16 (byte[] data, int offset) {
    return (data[offset] & 0xFF) + ((data[offset + 1] & 0xFF) << 8);
  }

  private static int getInt32 (byte[] data, int offset) {
    return (data[offset] & 0xFF) + ((data[offset + 1] & 0xFF) << 8) + ((data[offset + 2] & 0xFF) << 16) + ((data[offset + 3] & 0xFF) << 24);
  }

  /**
   * Attempt to parse a SHT_NOTE section to obtain the name of the target processor.
   * Note: this code is based on information at https://refspecs.linuxbase.org/elf/elf.pdf but, as this spec does not detail exactly
   * how this record is used by the gnu toolchain, the way this code extracts the name of the target processor is based on my analysis
   * of the data in the record.
   * @param file ELF File
   * @return name of target, or null
   */
  static String getTargetFromElf (File file) {
    try {
      FileInputStream fis = new FileInputStream(file);
      byte[] data = new byte[fis.available()];
      if (fis.read(data) == data.length) {
        fis.close();
        if (data[0] == 0x7F && data[1] == 'E' && data[2] == 'L' && data[3] == 'F') {
          if (data[5] == 1) {
            if (data[6] == 1) {
              int e_shoff = getInt32(data, 0x20);             // Points to the start of the section header table
              int e_shentsize = getInt16(data, 0x2E);         // Contains the size of a section header table entry
              int e_shnum = getInt16(data, 0x30);             // Contains the number of entries in the section header table
              for (int ii = 0; ii < e_shnum; ii++) {
                int offset = e_shoff + (ii * e_shentsize);
                byte[] temp = new byte[e_shentsize];
                System.arraycopy(data, offset, temp, 0, temp.length);
                int secType = getInt32(temp, 0x04);
                int secSize = getInt32(temp, 0x14);
                int secOff = getInt32(temp, 0x10);
                byte[] secData = new byte[secSize];
                System.arraycopy(data, secOff, secData, 0, secData.length);
                if (secType == 0x07) {                        // Is this a SHT_NOTE section?
                  StringBuilder buf = new StringBuilder();
                  for (int jj = 49; jj < secData.length; jj++) {
                    if (secData[jj] == 0) {
                      break;
                    }
                    buf.append((char) secData[jj]);
                  }
                  return buf.toString();
                }
              }
            }
          }
        }
      }
      return null;
    } catch (IOException ex) {
      return null;
    }
  }

  public static void expand (Rectangle dst, Rectangle2D src) {
    int lft = (int) Math.min(dst.x, src.getX());
    int top = (int) Math.min(dst.y, src.getY());
    int rht = (int) Math.max(dst.x + dst.width, src.getX() + src.getWidth());
    int bot = (int) Math.max(dst.y + dst.height, src.getY() + src.getHeight());
    dst.setBounds(lft, top, bot - top, rht - lft);
  }

/*
  // Test code for getTargetFromElf()
  public static void main (String[] args) throws IOException {
    File file = new File("examples/attiny212.elf");
    String target = getTargetFromElf(file);
    System.out.println(target);
  }
*/

  public static byte lsb (int val) {
    return (byte) (val & 0xFF);
  }

  public static byte msb (int val) {
    return (byte) ((val >> 8) & 0xFF);
  }

  static class ProgressBar extends JFrame {
    private final JDialog       frame;
    private final JProgressBar  progress;

    ProgressBar (JFrame comp, String msg) {
      frame = new JDialog(comp);
      frame.setUndecorated(true);
      JPanel pnl = new JPanel(new BorderLayout());
      pnl.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      frame.add(pnl, BorderLayout.CENTER);
      pnl.add(progress = new JProgressBar(), BorderLayout.NORTH);
      JTextArea txt = new JTextArea(msg);
      txt.setEditable(false);
      pnl.add(txt, BorderLayout.SOUTH);
      Rectangle loc = comp.getBounds();
      frame.pack();
      frame.setLocation(loc.x + loc.width / 2 - 150, loc.y + loc.height / 2 - 150);
      frame.setVisible(true);
    }

    void setValue (int value) {
      SwingUtilities.invokeLater(() -> progress.setValue(value));
    }

    void setMaximum (int value) {
      progress.setMaximum(value);
    }

    void close () {
      frame.setVisible(false);
      frame.dispose();
    }
  }
}
