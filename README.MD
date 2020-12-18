<p align="center"><img src="/images/MegaTinyIDE%20Screenshot.png"></p>

## MegaTinyIDE (for 64 bit macOS, Windows & Linux)

MegaTinyIDE is a simple, GNU-based IDE that's derived from another project of mine called [ATTiny10IDE](https://github.com/wholder/ATTiny10IDE), which I originally wrote to simplify writing code for the ATTiny10 Series Microcontrollers using C. C++ or Assembly language.  This new version is intended to support coding for the new MegaTiny chips (tinyAVR® 1-series and 0-series), such as the ATtiny212, which feature a greatly expanded set of features compared to the original, Atmel-designed ATTiny series chips.

### Project Goals

These new tinyAVR® 1-series and 0-series introduce a new, single-pin Unified Program and Debug Interface (UPDI), which makes it possible to do away with the need for a boot loader and still make use of all the I/O pins except RESET, which is the pin used by UPDI.  While UPDI is supported by Microchip's development tools, such a MPLAB, I wanted to develop a lightweight, alternative IDE similar to the Arduino IDE, but streamlined to support just this new tinyAVR architecture.  In addition, I wanted to add other, special features such as those listed below:

  - Now supports include of user files using `#include "file.h"` notation
  - Automatically annotate interrupt vectors in listing file (optional)
  - Interleave code and generated asm in listing file (optional)
  - Pop Up Interfaces for:
      - Viewing and Editing FUSE values (see below)
      - Viewing EEPROM bytes (working on edit)
      - Identifying Target Chip
  - [Hardware Debugging](https://github.com/wholder/MegaTinyIDE/blob/master/resources/documentation/debugging.md) (via the UPDI interface using [compatible programmer/debugger](https://github.com/wholder/MegaTinyIDE/blob/master/resources/documentation/programmers.md)):
      - Set a Hardware Breakpoint by selecting the checkbox on an executable line of code in the Listing View
      - Single Step while the highlighted line in the Listing View shows the next line to execute
      - Right click on Registers and Status Flags in debugger pane to change value (I/O ports not currently supported)
      - Watch the I/O ports, Registers and Status Flags change while stepping through code (changed fields are highlighted)
      - Click on Links in Symbol table to view current state of variables
  - Built-in Pinout Reference Diagrams (with pin-level tooltips)
  - Pop up AVR Instruction Guide tooltips in Listing Pane (hover over instruction name)
  - A [Console Monitor feature called OCD Messaging](https://github.com/wholder/MegaTinyIDE/blob/master/resources/documentation/ocdmsg.md) that can send text message to the debugger over UPDI
  - Preliminary support for Arduino-style coding using the [megaTinyCore](https://github.com/SpenceKonde/megaTinyCore) library by Spence Konde (aka Dr. Azzy) See: `arduino412.c` in `examples` folder
  - Use a [`#pragma`](https://github.com/wholder/MegaTinyIDE/blob/master/resources/documentation/pragmas.md) in code to engage special compile time features, such as pop up dialogs that prompt for `#define` values or select the target chip part number.
  - **Native Versions with an embedded JVM are now available (see "[Releases](https://github.com/wholder/MegaTinyIDE/releases)")**

As this project advances, I hope to add many more features designed to make uisng the tinyAVR 1-series and 0-series as easy as possible, such as:

  - Improving the built-in documentation
  - More example code
  - Support for other UPDI-based programmers (and possibly debugging)
  - Support for additional tabs to allow opening additional source or header files

### Project status

While more testing is needed, I am declaring the code as operational (Beta status) on Mac, Windows 10 and Linux.  However, MegaTinyIDE is still in development and likely has more than a few bugs, or rough edges.  As this is a one man project, I sometimes struggle to do adequate testing (especially on Windows and Linux) when I release a new version. If you a spot a problem, [please post in Issue](https://github.com/wholder/MegaTinyIDE/issues) so I can investigate.  If you have questions, or suggestions, [please post in Discussions](https://github.com/wholder/MegaTinyIDE/discussions)

### Debugging Interface

<p align="center"><img src="/images/MegaTinyIDE%20Debugging.png"></p>

MegaTinyIDE supports on-chip debugging of using the one pin UPDI interface.  Currently, this requires using a UPDI programmer/debugger, such as the [Atmel-ICE](https://www.microchip.com/DevelopmentTools/ProductDetails/ATATMEL-ICE) or [PICKit-4](https://www.microchip.com/Developmenttools/ProductDetails/PG164140), but MegaTinyIDE can also talk to the built-in debugger chips on the [ATTiny3217-Curiosity-Nano](https://www.microchip.com/DevelopmentTools/ProductDetails/PartNO/EV50J96A), [ATTiny416-Xplained-Nano](https://www.microchip.com/DevelopmentTools/ProductDetails/PartNO/ATTINY416-XNANO) and [ATTiny817-XPlained-Mini](https://www.microchip.com/developmenttools/ProductDetails/attiny817-xmini) and the ATTiny3217-Curiosity-Nano board can also be adapted to function as inexpensive programmer/debugger.

**Debugger Limitations:** The debugger can only set a breakpoint on assembly language instructions, not lines of C/C++ language code.  And, because modern compilers perform a lot of optimization on the code to reduce the number of assembly language instructions needed to implement a statement in C/C++, it can sometimes be hard to tell which assembly instructions correspond to a given C statement, especially if you are not familiar with assembly language code generated by the compiler.  There is an option (on by default) that tells the compiler to interleave the C/C++ statements with the generated assembly, but this can sometimes be more confusing than helpful, especially as modern compilers will sometimes reorder the code, such as to move invariant operations outside a loop.

### Pinout Reference Diagrams
  
  <p align="center"><img src="/images/MegaTinyIDE%20Pinouts.png"></p>
  
MegaTinyIDE's built-in documentation pages include a complete set of pinout diagrams along with the basic specifications for each Chip.  _Note: this is a work in prograam, so some diagrams may be incomplete (currently, secondary I/O units are not listed), or incorrect._

### Fuse Editor

  <p align="center"><img src="/images/MegaTinyIDE%20Fuses.png"></p>
  
MegaTinyIDE's built-in Fuse Editor lets you read the current fuse setting from a target device using the UPDI interface, change as needed and then write back the changed values.  _Note: changing the value of these fuses can alter the behavior of the target in unexpected ways and even "[brick](https://en.wikipedia.org/wiki/Brick_(electronics))" it if you are not careful.  Please be sure you consult the appropriate datasheet and know what you are doing before making any changes._

### Universal JAR File supports 64-bit Mac, Windows and Linux

The Native code releases of MegaTinyIDE, which include an OpenJDM JVM are recommended (see "Releases" near the top right of this page.) However, I also maintain a pre-built, executable universal JAR file in the [out/artifacts/MegaTinyIDE_jar](https://github.com/wholder/MegaTinyIDE/blob/master/out/artifacts/MegaTinyIDE_jar/MegaTinyIDE.jar) folder.  This version is designed to run on 64 bit versions of macOs, Windows 10 and Linux as long as you have Java 8, or later installed on your system.

To use the universal version on a Mac, simply double click the MegaTinyIDE.jar file to run it once you've downloaded it, although you'll probably have to right click and select "Open" the  first time you run MegaTinyIDE due to the Mac OS X security check and the fact that I'm currently unable to digitally sign the JAR file.  You should also be able to double click and run using Windows, but some versions of Windows may require you to [enable this ability](https://www.addictivetips.com/windows-tips/run-a-jar-file-on-windows/) first.  You'll need to give the .jar file RUN permission to execute on Linux and using a USB-based device programmer, such as the AVRISP MkII, may also require you to [create an install a rules file](https://stackoverflow.com/questions/5412727/avrisp-mkii-doesnt-work-with-avrdude-on-linux).  The needed GNU-based AVR toolchains are built into MegaTinyIDE and the appropriate toolchain will be automatically installed when you run the MegaTinyIDE JAR file.

### Requirements
I suggest using [Java 8 JRE](https://www.java.com) or JDK, or later for MegaTinyIDE, but the code also seems to run fine under the OpenJDK Java.  Note: I wrote MegaTinyIDE on a Mac Pro using the _Community_ version of [IntelliJ IDEA from JetBrains](https://www.jetbrains.com/idea/) and OS X is the only environment where I have extensively tested and used MegaTinyIDE .  I've included GNU/AVR toolchains for Mac and 64 bit Windows and 64 bit Linux, but have not extensively tested the code under Windows or Linux.  Feel free to report any issues you discover.  I'll do my best, when time permits, to investigate them, but I cannot guarantee fixes, timely or otherwise.

Note: MegaTinyIDE for macOs seems to run fine on the new Mac Mini with Apple Silicon and Rosetta 2, but more testing is needed.

### Using MegaTinyIDE
MegaTinyIDE supports coding for the AvrXMega3 Series of Microcontrollers in C and C++ using the GNU AVR-C++ compiler to generate code.  The file extension you choose for your code file tells MegaTinyIDE how to process the code:

    .c   - Compile as C file using avr-gcc
    .cpp - Compile as C++ file using avr-g++
    .s   - Assemble using avr-as and link with avr-ld
    
So, this means you'll have to save your source file with the appropriate extension before you can compile, or assemble it.  In addition, you can also write [inline assembly code](https://web.stanford.edu/class/ee281/projects/aut2002/yingzong-mouse/media/GCCAVRInlAsmCB.pdf) in a C/C++ (.c) file.  You'll find a few examples of these different approaches to writing code in the [examples folder](https://github.com/wholder/MegaTinyIDE/tree/master/examples) but these a pretty basic at the moment.

### Credit and Thanks

This project would have been much harder and much less cool without help from the following open source projects, or freely available software.

 - [Hid4Java](https://github.com/gary-rowe/hid4java) by Gary Rowe (used to talk to USB-based programmers)
 - [ATTinyCore](https://github.com/SpenceKonde/ATTinyCore) library by Spence Konde (aka Dr. Azzy)
 - [JSyntaxPane](https://github.com/nordfalk/jsyntaxpane) - Now uses [CppSyntaxPane](https://github.com/wholder/CppSyntaxPane), which is based on JSyntaxPane.
 - [Txtmark](https://github.com/rjeschke/txtmark) Java markdown processor by René Jeschke
 - [GraphPaperLayout](http://www.iitk.ac.in/esc101/05Aug/tutorial/uiswing/layout/example-1dot4/GraphPaperLayout.java) by Michael Martak, a grid layout manager that supports row and column spanning.
 - [IntelliJ IDEA from JetBrains](https://www.jetbrains.com/idea/) (my favorite development environment for Java coding. Thanks JetBrains!)
 - [ANTLR version 4.7.1](http://www.antlr.org) - ALTLRv4 was used to generate the parser for the new (experimental) automatic prototype type generation feature.
 - [CPP14 Antlr Grammar](https://github.com/antlr/grammars-v4/blob/master/cpp/CPP14.g4) - CPP14 was used as the base, C++ grammar for the automatic prototype type generation feature.
 - [James' Dodgy Arduino-Oriented Pinout Maker For Microcontrollers](https://github.com/sleemanj/ArduinoOrientedChipPinoutCreator) by James Sleeman for inspirig me to write my take on his pinout diagram generator.
 - [AVR Logo graphic](https://icon-icons.com/icon/Assembly-AVR/132579) by John Gardner from which I adapted the MegaTinyIDE logo.
 - [Packr](https://github.com/libgdx/packr) by libgdx for the ability to package native versions of MegaTinyIDE with an embedded JVM.

### MIT License

Copyright 2014-2020 Wayne Holder

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.