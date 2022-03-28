# Virtual Ports

The TinyMega series supports special registers called [Virtual Ports](documentation/peripherals.md) that occupy the lowest ranges of the I/O address
space.  Because of this, these ports can be controlled by special, one byte `SBI` and `CBI` instructions for setting and clearing a single bit, respectively.
This sample code  shows how to define macros (note 3) called **`SET_BIT()`** and **`CLR_BIT()`** which you can use in C code to control individual I/O pins.
In this example, bit 3 of the virtual port **`VPORTA.DIR`** is set to enable pin 7 as an output pin.  Then, in a loop, this pin is toggled on and off 
(note 1) by setting bit 3 of the virtual port **`VPORTA.IN`**.
 
<table style="font-family:monospaced">
    <tr style="font-weight: bold"><td>Address</td><td>Port</td><td>&nbsp;</td><td>Function</td></tr>
    <tr><td>0x0000</td><td>VPORTA.DIR</td><td>&nbsp;</td><td>Write 1 to set pin as output, else set to input</td></tr>
    <tr><td>0x0001</td><td>VPORTA.OUT</td><td>&nbsp;</td><td>Write 1 to set pin high, or 0 to set pin low</td></tr>
    <tr><td>0x0002</td><td>VPORTA.IN</td><td>&nbsp;</td><td>Read for current state of pin, or write 1 to toggle it state</td></tr>
    <tr><td>0x0003</td><td>VPORTA.INTFLAGS</td><td>&nbsp;</td><td>Interrupt flags (bit is set if I/O pin caused an interrupt)</td></tr>
</table>

*[CODE_BLOCK:
pragma chip 	attiny212        // Select target device
#pragma clock	20000000         // Set clock to 20 MHz

#include <avr/io.h>
#include <stdbool.h>
#include <util/delay.h>

  //  PORTA |PA7|PA6|---|---|PA3|PA2|PA1|PA0|
  //
  //      +----212----+
  //  Vdd | 1       8 | Gnd
  //  PA6 | 2       7 | PA3/CLK
  //  PA7 | 3       6 | PA0/UPDI/RESET
  //  PA1 | 4       5 | PA2
  //      +-----------+

#define SET_BIT(P, B) asm volatile("sbi %0, %1" : : "I" (_SFR_IO_ADDR(P)), "I" (B))
#define CLR_BIT(P, B) asm volatile("cbi %0, %1" : : "I" (_SFR_IO_ADDR(P)), "I" (B))

int main () {
  CPU_CCP = 0xD8;                // Enable Configuration Change Protected register
  CLKCTRL.MCLKCTRLB = 0x00;      // Set Clock to 20 MHz
  SET_BIT(VPORTA.DIR, 3);        // Set bit 3 in PORTA (pin 7) to output
  while (true) {
    SET_BIT(VPORTA.IN, 3);       // Toggle bit 3 in PORTA (pin 7)
    _delay_ms(500);              // Delay 500 ms
  }
}
]*

**Note 1**: **`VPORTA.IN`** is normally used to read the input bits from port A, but it can also be used to toggle the state of an output pin by writing a 1 to the appropriate bit.

**Note 2**: Other features of the I/O pins, such as enabling pullups, can be set by writing to the [**`PORTx`** registers](documentation/peripherals.md#PORTA).
However, because these ports are **not** located in the lower address space, you can only use these macros and the more efficient **`SBI`** and **`CBI`** 
instructions with the virtual ports.

**Note 3**: Alternately, you can also use compiler-defined constants, such as **`PIN3_bm`** to write code such as **`VPORTA.OUT |= PIN3_bm`** to
set bits and code like **`VPORTA.OUT &= ~PIN3_bm`** to clear bits.
