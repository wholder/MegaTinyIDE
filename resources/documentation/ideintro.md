MegaTinyIDE is a simple, GNU-based IDE that's derived from another project of mine called ATTiny10IDE, which I originally wrote to simplify writing code for the ATTiny10 Series Microcontrollers using C. C++ or Assembly language.  This new version is intended to support coding for the new MegaTiny chips (tinyAVR® 1-series and 0-series), such as the ATtiny212, which feature a greatly expanded set of features compared to the original, Atmel-designed ATTiny series chips.

## Credit and Thanks

This project would have been much harder and much less cool without help from the following open source projects, or freely available software.

 - [Hid4Java](https://github.com/gary-rowe/hid4java) by Gary Rowe (used to talk to USB-based programmers)
 - [ATTinyCore](https://github.com/SpenceKonde/ATTinyCore) library by Spence Konde (aka Dr. Azzy)
 - [JSyntaxPane](https://github.com/nordfalk/jsyntaxpane) - Now uses [CppSyntaxPane](https://github.com/wholder/CppSyntaxPane), which is based on JSyntaxPane.
 - [GraphPaperLayout](http://www.iitk.ac.in/esc101/05Aug/tutorial/uiswing/layout/example-1dot4/GraphPaperLayout.java) by Michael Martak, a grid layout manager that supports row and column spanning
 - [IntelliJ IDEA from JetBrains](https://www.jetbrains.com/idea/) (my favorite development environment for Java coding. Thanks JetBrains!)
 - [ANTLR version 4.7.1](http://www.antlr.org) - ALTLRv4 was used to generate the parser for the new (experimental) automatic prototype type generation feature.
 - [CPP14 Antlr Grammar](https://github.com/antlr/grammars-v4/blob/master/cpp/CPP14.g4) - CPP14 was used as the base, C++ grammar for the automatic prototype type generation feature.
 - [AVR Logo graphic](https://icon-icons.com/icon/Assembly-AVR/132579) by John Gardner

## MIT License

Copyright 2014-2025 Wayne Holder

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.