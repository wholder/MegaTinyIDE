# Real Time Counter

Example code for using the Real Counter Clock ([RTC](documentation/peripherals.md#RTC)) Interrupt to toggle **`PORTA`** Bit 3 (I/O pin 7) on and off at
32.768 kHz (Note 1) divided by 32 (resulting in an interrupt rate of 1024 Hz.)  The Real Time counter can be driven directly by the internal, 32.768 kHz,
Ultra-Low Power oscillator (**`OSCULP32K`**), by this same oscillator divided by 32 (approx. 1 KHz), or by an external clock on the **`TOSC1`** pin (see below.)

*[CODE_BLOCK:
#include <avr/io.h>
#include <avr/interrupt.h>
#include <avr/cpufunc.h>

  // RTC Periodic Interrupt Code Example

#pragma chip 	attiny212
#pragma clock	20000000                  // Set clock to 20 MHz

  // RTC Interrupt handler
ISR (RTC_PIT_vect) {
  RTC.PITINTFLAGS = RTC_PI_bm;            // Clear interrupt flag by writing '1'
  VPORTA.IN |= PIN3_bm;                   // Toggle bit 3 of PORTA (pin 7)
}

int main (void) 	{
  VPORTA.DIR |= PIN3_bm;                  // Set bit 3 of PORTA as Output (pin 7)
  // Initialize RTC
  RTC.CLKSEL = RTC_CLKSEL_INT32K_gc;      // 32.768 kHz Internal Crystal Oscillator (XOSC32K)
  RTC.PITINTCTRL = RTC_PI_bm;             // Enable Periodic Interrupt
  RTC.PITCTRLA = RTC_PERIOD_CYC32_gc      // RTC Clock Cycles (divide by 32)
               | RTC_PITEN_bm;            // Enable RTC interrupt
  sei();                                  // Enable Global Interrupts
  while (1) {                             // Loop forever
  }
}
]*

**Note 1**: The accuracy of the **`OSCULP32K`** oscillator is factory calibrated to +/- 3% at 25°C, 3.0V.

**Note 2**: Adapted from Microchip Technical Brief TB3213 "Getting Started with RTC"

## Other Divisor Settiings
<table style="font-family:monospaced;font-size:12">
  <trstyle="font-weight: bold"><td>RTC.PITCTRLA</td><td>Value</td><td>&nbsp;</td><td>Divisor</td><td>&nbsp;</td><td>Rate at 32.768 kHz</td><td>&nbsp;</td><td>Rate at 1024 Hz</td></tr>
  <tr><td>RTC_PERIOD_OFF_gc</td><td>(0x00 &lt;&lt; 3)</td><td></td><td>none</td><td></td><td>No Interrupt</td><td></td><td>No Interrupt</td></tr>
  <tr><td>RTC_PERIOD_CYC4_gc</td><td>(0x01 &lt;&lt; 3)</td><td></td><td>4</td><td></td><td>8192 Hz</td><td></td><td>256 Hz</td></tr>
  <tr><td>RTC_PERIOD_CYC8_gc</td><td>(0x02 &lt;&lt; 3)</td><td></td><td>8</td><td></td><td>4096 Hz</td><td></td><td>128 Hz</td></tr>
  <tr><td>RTC_PERIOD_CYC16_gc</td><td>(0x03 &lt;&lt; 3)</td><td></td><td>16</td><td></td><td>2048 Hz</td><td></td><td>64 Hz</td></tr>
  <tr><td>RTC_PERIOD_CYC32_gc</td><td>(0x04 &lt;&lt; 3)</td><td></td><td>32</td><td></td><td>1024 Hz</td><td></td><td>32 Hz</td></tr>
  <tr><td>RTC_PERIOD_CYC64_gc</td><td>(0x05 &lt;&lt; 3)</td><td></td><td>64</td><td></td><td>512 Hz</td><td></td><td>16 Hz</td></tr>
  <tr><td>RTC_PERIOD_CYC128_gc</td><td>(0x06 &lt;&lt; 3)</td><td></td><td>128</td><td></td><td>256 Hz</td><td></td><td>8 Hz</td></tr>
  <tr><td>RTC_PERIOD_CYC256_gc</td><td>(0x07 &lt;&lt; 3)</td><td></td><td>256</td><td></td><td>128 Hz</td><td></td><td>4 Hz</td></tr>
  <tr><td>RTC_PERIOD_CYC512_gc</td><td>(0x08 &lt;&lt; 3)</td><td></td><td>512</td><td></td><td>64 Hz</td><td></td><td>2 Hz</td></tr>
  <tr><td>RTC_PERIOD_CYC1024_gc</td><td>(0x09 &lt;&lt; 3)</td><td></td><td>1024</td><td></td><td>32 Hz</td><td></td><td>1 Hz</td></tr>
  <tr><td>RTC_PERIOD_CYC2048_gc</td><td>(0x0A &lt;&lt; 3)</td><td></td><td>2048</td><td></td><td>16 Hz</td><td></td><td>0.500</td></tr>
  <tr><td>RTC_PERIOD_CYC4096_gc</td><td>(0x0B &lt;&lt; 3)</td><td></td><td>4096</td><td></td><td>8 Hz</td><td></td><td>0.250</td></tr>
  <tr><td>RTC_PERIOD_CYC8192_gc</td><td>(0x0C &lt;&lt; 3)</td><td></td><td>8192</td><td></td><td>4 Hz</td><td></td><td>0.125</td></tr>
  <tr><td>RTC_PERIOD_CYC16384_gc</td><td>(0x0D &lt;&lt; 3)</td><td></td><td>16384</td><td></td><td>2 Hz</td><td></td><td>0.063</td></tr>
  <tr><td>RTC_PERIOD_CYC32768_gc</td><td>(0x0E &lt;&lt; 3)</td><td></td><td>32768</td><td></td><td>1 Hz</td><td></td><td>0.031</td></tr>
</table>

## Other Clock Sources
<table style="font-family:monospaced;font-size:12">
  <trstyle="font-weight: bold"><td>RTC.CLKSEL</td><td>Value</td><td>&nbsp;</td><td>Source</td></tr>
  <tr><td>RTC_CLKSEL_INT32K_gc</td><td>(0x00)</td><td>&nbsp;</td><td>32 KHz from OSCULP32K</td></tr>
  <tr><td>RTC_CLKSEL_INT1K_gc</td><td>(0x01)</td><td>&nbsp;</td><td>1 KHz from OSCULP32K</td></tr>
  <tr><td>Reserved</td><td>(0x02)</td><td>&nbsp;</td><td>Reserved</td></tr>
  <tr><td>RTC_CLKSEL_EXTCLK_gc</td><td>(0x03)<td>&nbsp;</td><td>External clock from TOSC1 pin</td></tr>
</table>
