import cpp14.CPP14ProtoGen;

import javax.swing.*;
import java.io.*;

import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/*
   * GNU Toolchain Controller for Compiling and Assembling code for avrxmega3 Series Chips
   * Author: Wayne Holder, 2017
   * License: MIT (https://opensource.org/licenses/MIT)
   *
   * References:
   *    https://www.mankier.com/1/arduino-ctags - used by Arduino IDE to generate function prototypes
   *
   * Notes:
   *    -MD       Generate dependencies -> file.d
   *    -MMD      Generate dependencies (only user header files, not system header files)
   *    -MF file  Dependencies -> file
   */

class MegaTinyCompiler {
  private static final String fileSep = System.getProperty("file.separator");

  private static final String prePro =  "avr-g++ " +                  // https://linux.die.net/man/1/avr-g++
                                        "-w " +                       // Inhibit all warning messages
                                        "-x c++ " +                   // Assume c++ file
                                        "-E " +                       // Preprocess only
                                        "-MMD " +                     // Generate dependencies to Sketch.inc
                                        "-MF *[TDIR]**[BASE]*.inc " + //   " "
                                        "-DF_CPU=*[CLOCK]* " +        // Create #define for F_CPU
                                        "-mmcu=*[CHIP]* " +           // Select CHIP microcontroller type
                                        "-DARDUINO_ARCH_MEGAAVR " +   // #define ARDUINO_ARCH_AVR
                                        "-DMILLIS_USE_TIMERD0 " +     // #define MILLIS_USE_TIMERD0
                                        "-I *[TDIR]* " +              // Also search in temp directory for header files
                                        "-I *[IDIR]* " +              // Also search in user's src directory for header files
                                        "*[SDIR]**[FILE]* ";          // Source file is temp/FILE.x

  private static final String compCpp = "avr-g++ " +                  // https://linux.die.net/man/1/avr-g++
                                        "-c " +                       // Compile but do not link
                                        "-g " +                       // Enable link-time optimization
                                        "-Os " +                      // Optimize for size
                                        "-w " +                       // Inhibit all warning messages
                                        "-std=gnu++11 " +             // Support GNU extensions to C++
                                        "-fpermissive " +             // Downgrade nonconformant code errors to warnings
                                        "-fno-exceptions " +          // Disable exception-handling code
                                        "-ffunction-sections " +      // Separate functions in output file
                                        "-fdata-sections " +          // Separate data in output file
                                        "-fno-threadsafe-statics " +  // No extra code for C++ ABI routines
                                        "-flto " +                    // Run standard link optimizer (requires 5.4.0)
                                        "-DLTO_ENABLED " +            // ??
                                        "-mmcu=*[CHIP]* " +           // Select CHIP microcontroller type
                                        "-DF_CPU=*[CLOCK]* " +        // Create #define for F_CPU
                                        "-DARDUINO_ARCH_MEGAAVR " +   // #define ARDUINO_ARCH_AVR
                                        "-DMILLIS_USE_TIMERD0 " +     // #define MILLIS_USE_TIMERD0
                                        "*[DEFINES]* " +              // Add in conditional #defines, if any
                                        "-I *[TDIR]* " +              // Also search in temp directory for header files
                                        "-I *[IDIR]* " +              // Also search in user directory for header files
                                        "*[SDIR]**[FILE]* " +        // Source file is temp/FILE.x
                                        "-o *[TDIR]**[FILE]*.o ";    // Output to file temp/FILE.x.o

  private static final String compC = "avr-gcc " +                    // https://linux.die.net/man/1/avr-gcc
                                        "-c " +                       // Compile but do not link
                                        "-g " +                       // Enable link-time optimization
                                        "-Os " +                      // Optimize for size
                                        "-w " +                       // Inhibit all warning messages.
                                        "-std=gnu11 " +               // ??
                                        "-ffunction-sections " +      // Separate functions in output file
                                        "-fdata-sections " +          // Separate data in output file
                                        "-flto " +                    // Run standard link optimizer (requires 5.4.0)
                                        "-DLTO_ENABLED " +            // ??
                                        "-DF_CPU=*[CLOCK]* " +        // Create #define for F_CPU
                                        "-mmcu=*[CHIP]* " +           // Select CHIP microcontroller type
                                        "-DARDUINO_ARCH_MEGAAVR " +   // #define ARDUINO_ARCH_AVR
                                        "-DMILLIS_USE_TIMERD0 " +     // #define MILLIS_USE_TIMERD0
                                        "*[DEFINES]* " +              // Add in conditional #defines, if any
                                        "-fno-fat-lto-objects " +     //
                                        "-I *[TDIR]* " +              // Also search in temp directory for header files
                                        "-I *[IDIR]* " +              // Also search in user directory for header files
                                        "*[SDIR]**[FILE]* " +         // Source file is temp/FILE.x
                                        "-o *[TDIR]**[FILE]*.o ";     // Output to file temp/FILE.x.o

  private static final String compAsm = "avr-gcc " +                  // https://linux.die.net/man/1/avr-gcc
                                        "-c " +                       // Compile but do not link
                                        "-g " +                       // Enable link-time optimization
                                        "-x assembler-with-cpp " +    //
                                        "-flto " +                    // Run standard link optimizer (requires 5.4.0)
                                        "-DLTO_ENABLED " +            // ??
                                        "-DF_CPU=*[CLOCK]* " +        // Create #define for F_CPU
                                        "-mmcu=*[CHIP]* " +           // Select CHIP microcontroller type
                                        "-DARDUINO_ARCH_MEGAAVR " +   // #define ARDUINO_ARCH_AVR
                                        "-DMILLIS_USE_TIMERD0 " +     // #define MILLIS_USE_TIMERD0
                                        "*[DEFINES]* " +              // Add in conditional #defines, if any
                                        "-I *[TDIR]* " +              // Also search in temp directory for header files
                                        "-I *[IDIR]* " +              // Also search in user directory for header files
                                        "*[SDIR]**[FILE]* " +         // Source file is temp/FILE.x
                                        "-o *[TDIR]**[FILE]*.o ";     // Output to file temp/FILE.x.o

  private static final String link = "avr-gcc " +                     // https://linux.die.net/man/1/avr-g++
                                        "-w " +                       // Inhibit all warning messages.
                                        "-Os " +                      // Optimize for size
                                        "-g " +                       // Enable link-time optimization
                                        "-flto " +                    // Run standard link optimizer (requires 5.4.0)
                                        "-DLTO_ENABLED " +            // ??
                                        "-fuse-linker-plugin " +      // Enable link-time optimization (requires 5.4.0)
                                        "-Wl,--gc-sections " +        // Eliminate unused code
                                        "-Wl,--print-gc-sections " +  // Print dead code removed
                                        "-DF_CPU=*[CLOCK]* " +        // Create #define for F_CPU
                                        "-mmcu=*[CHIP]* " +           // Select CHIP microcontroller type
                                        "-o *[TDIR]**[OFILE]* " +     // Output to file temp/OFILE
                                        "*[LIST]*" +                  // List of files to link (each prefixed by temp/)
                                        "-L*[TDIR]* " +               // Also search in temp dir for -l option
                                        "-lm ";                       // Link Math library (??)

  private static final String list = "avr-objdump " +                 // https://linux.die.net/man/1/avr-objdump
                                        "-d " +                       // Disassemble code
                                        "*[INTLV]* " +                // If enabled, source code intermixed with disassembly
                                        "*[SYMT]* " +                 // If enabled, print the symbol table entries
                                        "*[TDIR]**[BASE]*.elf";       // Input file

  private static final String tohex = "avr-objcopy " +                // https://linux.die.net/man/1/avr-objcopy
                                        "-O ihex " +                  // Output format is Intel HEX
                                        "-R .eeprom " +               // Remove .eeprom section (!!!)
                                        "*[TDIR]**[BASE]*.elf " +     // Input file
                                        "*[TDIR]**[BASE]*.hex";       // Output file

  private static final String size = "avr-size " +                    // https://linux.die.net/man/1/avr-size
                                        "-A " +                       //
                                        "*[TDIR]**[BASE]*.elf";       // Input file

  private static final String[][] build = {
      {"TOHEX", tohex},
      {"LST", list},   // Note add "-l' for source path and line numbers (Warning: large lines!)
      {"SIZE", size},
  };

  static class CompFile {
    String srcDir, dstDir, file;

    CompFile (String srcDir, String dstDir, String file) {
      this.srcDir = srcDir;
      this.dstDir = dstDir;
      this.file =file;
    }
  }

  static Map<String, String> compile (String src, Map<String, String> tags, Preferences prefs, JFrame tinyIde) throws Exception {
    String srcFile = tags.get("EFILE");
    String srcDir = srcFile != null ? (new File(srcFile)).getParent() + "/" : null;
    String srcName = tags.get("FNAME");
    String[] srcParts = srcName.split("\\.");
    String srcBase = srcParts[0];
    String srcExt = srcParts[1];
    boolean isCCode = srcExt.equalsIgnoreCase(".c") || srcExt.equalsIgnoreCase(".cpp") || srcExt.equalsIgnoreCase(".ino");
    String tmpDir = tags.get("TDIR");
    String tmpExe = tags.get("TEXE");
    Utility.removeFiles(new File(tmpDir));
    boolean preOnly = isCCode && "PREONLY".equals(tags.get("PREPROCESS"));
    boolean genProto = isCCode && "GENPROTOS".equals(tags.get("PREPROCESS"));
    String clock = null;
    String chip = prefs.get("programmer.target", "attiny212");
    StringBuilder defines = new StringBuilder();
    Map<String, String> out = new HashMap<>();
    List<String> warnings = new ArrayList<>();
    Map<String, Integer[]> exports = new LinkedHashMap<>();
    // Process #pragma and #include directives
    int lineNum = 0;
    int LastIncludeLine = 1;
    for (String line : src.split("\n")) {
      lineNum++;
      int idx = line.indexOf("//");
      if (idx >= 0) {
        line = line.substring(0, idx).trim();
      }
      if (line.startsWith("#pragma")) {
        line = line.substring(7).trim();
        String[] parts = Utility.parse(line);
        if (parts.length > 1) {
          tags.put("PRAGMA." + parts[0].toUpperCase(), parts[1]);
          switch (parts[0]) {
            case "clock":                                         // Sets F_CPU #define
              clock = parts[1];
              break;
            case "chip":                                          // Sets -mmcu compile option
              chip = parts[1];
              break;
            case "define":                                        // Sets -D compile option to parts[1]
              defines.append("-D").append(parts[1]).append(" ");
              break;
            case "xparm":                                         // Defines exported parameter
              exports.put(parts[1], new Integer[0]);
              break;
            default:
              warnings.add("Unknown pragma: " + line + " (ignored)");
              break;
          }
        } else {
          warnings.add("Invalid pragma: " + line + " (ignored)");
        }
      } else if (line.startsWith("#include")) {
        LastIncludeLine = Math.max(LastIncludeLine, lineNum);
      }
    }
    tags.put("CHIP", chip);
    tags.put("INTLV", prefs.getBoolean("interleave", true) ? "-S" : "");
    tags.put("SYMT", prefs.getBoolean("symbol_table", true) ? "-t" : "");
    tags.put("CLOCK", clock != null ? clock : "8000000");
    tags.put("DEFINES", defines.toString());
    // Build list of files we need to compile and link
    MegaTinyIDE.ChipInfo chipInfo = MegaTinyIDE.chipTypes.get(chip.toLowerCase());
    if (chipInfo == null) {
      throw new IllegalStateException("Unknown chip type: " + chip);
    }
    out.put("INFO", "chip: " + chip + ", clock: " + tags.get("CLOCK"));
    MegaTinyIDE.ProgressBar progress = null;
    try {
      // Copy contents of "source" pane to source file with appropriate extension for code type
      Utility.saveFile(tmpDir + srcName, src);
      // Copy "variant" files into tmpDir so compiler can reference them
      Utility.copyResourcesToDir(chipInfo.variant, tmpDir);
      // If Arduino project, copy needed files
      boolean ardiuno = isArduino(src);
      if (ardiuno) {
        System.out.println("Compile Arduino project");
        Utility.copyResourcesToDir("arduino", tmpDir);
      }
      List<CompFile> compFiles = new ArrayList<>();
      try {
        // Preprocess .cpp source code using GNU c++ compiler
        tags.put("IDIR", srcDir);     // User #include directory
        tags.put("TDIR", tmpDir);     // Temp directory
        tags.put("SDIR", tmpDir);     // User file directory
        tags.put("FILE", srcName);    // Source file name
        tags.put("BASE", srcBase);    // Source file name
        String cmd = Utility.replaceTags(tmpExe + "bin" + fileSep + prePro, tags);
        System.out.println("Preprocess: " + cmd);
        Process proc = Runtime.getRuntime().exec(cmd);
        String ret = Utility.runCmd(proc);
        int retVal = proc.waitFor();
        if (retVal != 0) {
          tags.put("ERR", ret);
          return tags;
        }
        List<String> includes = Utility.processIncludes(srcDir, tmpDir, srcBase + ".inc");
        // Build list if user files to be compiled
        for (String include : includes) {
          String[] parts = include.split("\\.");
          if (parts.length > 1 && "h".equalsIgnoreCase(parts[1])) {
            // Does a matching .c of .cpp file exist in same directory
            for (String ext : new String[] {".c", ".cpp"}) {
              String fName = parts[0] + ext;
              if ((new File(srcDir + fName)).exists()) {
                compFiles.add(new CompFile(srcDir, tmpDir, fName));
              }
            }
          }
        }
        if (preOnly || genProto) {
          // Scan preprocess output for line markers and extract section for generated prototypes
          StringTokenizer lines = new StringTokenizer(ret, "\n");
          StringBuilder buf = new StringBuilder();
          boolean inSketch = false;
          boolean lineMarkerFound = false;
          Pattern lMatch = Pattern.compile("#\\s\\d+\\s\"(.*?)\"");
          String pathPat = tmpDir + srcName;
          String osName = System.getProperty("os.name").toLowerCase();
          if (osName.contains("win")) {
            pathPat = pathPat.replace("\\", "\\\\");
          }
          while (lines.hasMoreElements()) {
            String line = lines.nextToken();
            Matcher mat = lMatch.matcher(line);
            if (mat.find()) {
              String seq = mat.group(1);
              inSketch = seq.equals(pathPat);
              if (!lineMarkerFound) {
                buf.append(line).append("\n");
                lineMarkerFound = true;
              }
            } else if (inSketch) {
              buf.append(line);
              buf.append("\n");
            }
          }
          // Generate prototypes
          if (genProto) {
            // Copy protos into source and continue build
            // todo: if arduino, need to exclude setup() and loop()
            String protos = CPP14ProtoGen.getPrototypes(buf.toString());
            lineNum = 0;
            buf = new StringBuilder();
            for (String line : src.split("\n")) {
              lineNum++;
              buf.append(line).append("\n");
              if (lineNum == LastIncludeLine) {
                buf.append(protos).append("#line ").append(lineNum + 1).append("\n");
              }
            }
            Utility.saveFile(tmpDir + srcName, buf.toString());
          } else {
            // Return just the preprocessed source
            out.put("PRE", ret);
            return out;
          }
        }
        // Build list of all files to be compiled
        File file = new File(tmpDir);
        Path path = file.toPath();
        Stream<Path> walk = Files.walk(path, FileVisitOption.FOLLOW_LINKS);
        for (Iterator<Path> it = walk.iterator(); it.hasNext(); ) {
          Path item = it.next();
          String fName = item.getFileName().toString();
          String[] parts = fName.split("\\.");
          if (parts.length > 1 && (parts[1].equalsIgnoreCase("c") || parts[1].equalsIgnoreCase("cpp"))) {
            String compFile = item.toString().substring(path.toString().length());
            compFile = compFile.startsWith("/") ? compFile.substring(1) : compFile;
            if (!compFile.contains("deprecated")) {
              compFiles.add(new CompFile(tmpDir, tmpDir, compFile));
            }
          }
        }
      } catch (Exception ex) {
        ex.printStackTrace();
        tags.put("ERR", ex.getMessage());
        return tags;
      }
      // Compile source code and add in included code files as they are discovered
      progress = new MegaTinyIDE.ProgressBar(tinyIde, "Compiling and Building");
      StringBuilder linkList = new StringBuilder();
      progress.setMaximum(compFiles.size());
      int progCount = 0;
      progress.setValue(progCount);
      for (CompFile compFile : compFiles) {
        linkList.append(tmpDir).append(compFile.file).append(".o ");
        String tmpExe1 = tags.get("TEXE");
        String suffix = compFile.file.substring(compFile.file.indexOf("."));
        tags.put("IDIR", srcDir);           // User #include directory
        tags.put("SDIR", compFile.srcDir);
        tags.put("TDIR", compFile.dstDir);
        tags.put("FILE", compFile.file);
        String cmd;
        switch (suffix.toLowerCase()) {
          case ".c":
            cmd = Utility.replaceTags(tmpExe1 + "bin" + fileSep + compC, tags);
            break;
          case ".cpp":
            cmd = Utility.replaceTags(tmpExe1 + "bin" + fileSep + compCpp, tags);
            break;
          case ".s":
            cmd = Utility.replaceTags(tmpExe1 + "bin" + fileSep + compAsm, tags);
            break;
          default:
            throw new IllegalStateException("Unknown file type: " + suffix);
        }
        System.out.println("Compile: " + cmd);
        Process proc = Runtime.getRuntime().exec(cmd);
        String ret = Utility.runCmd(proc);
        int retVal = proc.waitFor();
        if (retVal != 0) {
          String msg = "While Compiling: " + compFile.file + "\n" + ret;
          System.out.println(msg);
          tags.put("ERR", msg);
          return tags;
        }
        progress.setValue(++progCount);
      }
      // Link all object files
      tags.put("LIST", linkList.toString());
      tags.put("OFILE", srcBase + ".elf");
      String cmd = Utility.replaceTags(tmpExe + "bin" + fileSep + link, tags);
      System.out.println("Link: " + cmd);
      Process proc = Runtime.getRuntime().exec(cmd);
      String ret = Utility.runCmd(proc);
      int retVal = proc.waitFor();
      if (retVal != 0) {
        String msg = "While Linking\n" + ret;
        System.out.println(msg);
        tags.put("ERR", msg);
        return tags;
      }
      // Generate Arduino-like sketch hex output, listing and code/data size info
      for (String[] seq : build) {
        cmd = Utility.replaceTags(tmpExe + "bin" + fileSep + seq[1], tags);
        System.out.println("Run: " + cmd);
        proc = Runtime.getRuntime().exec(cmd);
        ret = Utility.runCmd(proc);
        retVal = proc.waitFor();
        if (retVal != 0) {
          String msg = "While Building\n" + ret;
          System.out.println(msg);
          tags.put("ERR", msg);
          return tags;
        }
        out.put(seq[0], ret);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      tags.put("ERR", ex.getMessage());
      return tags;
    } finally {
      if (progress != null) {
        progress.close();
      }
    }
    // Copy pragma values into "out" Map
    for (String key : tags.keySet()) {
      String val = tags.get(key);
      if (key.startsWith("PRAGMA.")) {
        out.put(key, val);
      }
    }
    String buf = Utility.getFile(tmpDir + srcBase + ".hex");
    out.put("HEX", buf);
    out.put("CHIP", chip);
    // Check if any variables were exported
    if (exports.size() > 0) {
      try {
        String listing = out.get("LST");
        int idx = listing.indexOf("SYMBOL TABLE:\n");
        int loadStart = 0, dataStart = 0;
        if (idx >= 0) {
          int end = listing.indexOf("\n\n", idx);
          if (end >= 0 && end > idx) {
            String symbols = listing.substring(idx + 14, end);
            StringTokenizer tok = new StringTokenizer(symbols, "\n");
            while (tok.hasMoreElements()) {
              String line = Utility.condenseWhitespace(tok.nextToken());
              String[] parts = line.split("\\s");
              if (parts.length == 6) {
                if ("O".equals(parts[2]) && exports.containsKey(parts[5])) {
                  String tmp = parts[0];
                  int add = Integer.parseInt(tmp.substring(tmp.length() - 4), 16);
                  int size = Integer.parseInt(parts[4]);
                  exports.put(parts[5], new Integer[]{add, size});
                }
              } else if (parts.length == 5) {
                int add = Integer.parseInt(parts[0].toUpperCase().substring(parts[0].length() - 4), 16);
                switch (parts[4]) {
                  case "__data_load_start":
                    loadStart = add;
                    break;
                  case "__data_start":
                    dataStart = add;
                    break;
                }
              }
            }
          }
        }
        StringBuilder exVars = new StringBuilder();
        for (String name : exports.keySet()) {
          Integer[] parts = exports.get(name);
          if (parts != null && parts.length == 2) {
            int add = parts[0] - dataStart + loadStart;
            int size = parts[1];
            exVars.append(name).append(":").append(Integer.toHexString(add)).append(":").append(size).append("\n");
          } else {
            warnings.add("Data for #pragma xparm: " + name + " not found in .data section - " +
                "declare with __attribute__ ((section (\".data\")))");
          }
        }
        out.put("XPARMS", exVars.toString());
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
    // Check if any warnings were generated
    if (warnings.size() > 0) {
      StringBuilder tmp = new StringBuilder("Warnings:\n");
      for (String warn : warnings) {
        tmp.append("  ").append(warn).append("\n");
      }
      out.put("WARN", tmp.toString());
    }
    return out;
  }

  private static boolean isArduino (String src) {
    // Crude, placeholder test (need something better)
    return src.contains("setup") && src.contains("loop");
  }
}
