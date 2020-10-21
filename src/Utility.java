import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
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
  private static final String   StartMarker = "//:Begin Embedded Markdown Data (do not edit)";
  private static final String   EndMarker   = "\n//:End Embedded Markdown Data";
  private static final String   fileSep =  System.getProperty("file.separator");
  private static final Pattern  pat1 = Pattern.compile("(\\*\\[(.*?)]\\*)");

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
      return clock.substring(0, clock.length() - 3).trim() + "000000";
    } else if (clock.endsWith("khz")) {
      return clock.substring(0, clock.length() - 3).trim() + "000";
    } else if (clock.endsWith("hz")) {
      return clock.substring(0, clock.length() - 2).trim();
    } else {
      return clock;
    }
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
      fis = new FileInputStream(file);
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
        }
        if (!file.delete()) {
          throw new IOException("removeFiles() unable to delete file: " + file.getName());
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
      String[] parts = tag.split(":");
      if (parts.length > 1) {
        tag = parts[0];
        parm = parts[1];
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
   * Scans input code for a comment block containing encoded markdown text and, if present, extracts and
   * decodes it along with the source code (minus the comment block)
   * @param src input source code with optional encoded and embedded comment block
   * @return String[] array of length 1 of no embedded markdown, else String[] of length 2 where first
   * element in the array if the source code (minus the block comment) and the 2nd element is the extracted
   * and decoded markdown text.
   */
  static String[] decodeMarkdown (String src) {
    try {
      List<String> list = new ArrayList<>();
      int idx1 = src.indexOf(StartMarker);
      int idx2 = src.indexOf(EndMarker);
      if (idx1 >= 0 && idx2 > idx1) {
        list.add(src.substring(0, idx1) + src.substring(idx2 + EndMarker.length()));
        String tmp = src.substring(idx1 + StartMarker.length(), idx2);
        StringTokenizer tok = new StringTokenizer(tmp, "\n");
        StringBuilder buf = new StringBuilder();
        while (tok.hasMoreElements()) {
          String line = (String) tok.nextElement();
          if (line.startsWith("//:") && line.length() == 128 + 3) {
            buf.append(line.substring(3));
          }
        }
        tmp = new String(Base64.getDecoder().decode(buf.toString()), StandardCharsets.UTF_8);
        tmp = URLDecoder.decode(tmp, "utf8");
        list.add(tmp);
      } else {
        list.add(src);
      }
      return list.toArray(new String[0]);
    } catch (UnsupportedEncodingException ex) {
      ex.printStackTrace();
      return new String[] {src};
    }
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

  static int fromHex (char cc) {
    cc = Character.toUpperCase(cc);
    return cc >= 'A' ? cc - 'A' + 10 : cc - '0';
  }

  public static void printHex (byte[] data) {
    printHex(data, 0, data.length);
  }

  public static void printHex (byte[] data, int off, int len) {
    for (int ii = 0; ii < len; ii++) {
      System.out.printf("0x%02X ", data[off + ii]);
      if (((ii + 1) & 0x0F) == 0 || ii == len - 1) {
        System.out.println();
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
      return new Font("Courier", Font.PLAIN, points);
    } else {
      return new Font("Courier", Font.PLAIN, points);
    }
  }
}
