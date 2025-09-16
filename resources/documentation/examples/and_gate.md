# 3 Input AND Gate using CCL

This TinyAVR example code shows how to program the Configurable Custom Logic (CCL) to implement a 3-input AND gate (Notes 1 & 2) where pins 4, 5 and 6 are
the inputs and pin 2 is the output.

*[CODE_BLOCK:
#pragma chip 	   attiny212           // Select target device
#pragma clock	   3333333             // Clock frequency on reset is 3.33 MHz

#include <avr/io.h>

void PORT0_init (void);
void CCL0_init  (void);

void PORT0_init (void) {
  PORTA.DIR &= ~PIN0_bm;               // PA0 - LUT0 IN[0]  (pin 6)
  PORTA.DIR &= ~PIN1_bm;               // PA1 - LUT0 IN[1]  (pin 4)
  PORTA.DIR &= ~PIN2_bm;               // PA2 - LUT0 IN[2]  (pin 5)
  PORTA.DIR |=  PIN6_bm;		       // PA3 - LUT0 OUT    (pin 2)
}

void CCL0_init (void) {
  CCL.LUT0CTRLB = CCL_INSEL0_IO_gc     // IO pin LUT0-IN0 input source
                | CCL_INSEL1_IO_gc;    // IO pin LUT0-IN1 input source
  CCL.LUT0CTRLC = CCL_INSEL2_IO_gc;    // IO pin LUT0-IN2 input source
  CCL.TRUTH0 = 0x80;                   // Configure Truth Table for LUT0
  CCL.LUT0CTRLA = CCL_OUTEN_bm         // Enable LUT0 output on IO pin
                | CCL_ENABLE_bm;       // Enable LUT0
  CCL.CTRLA = CCL_ENABLE_bm;           // Enable CCL module 
}

int main (void) {
  PORT0_init();
  CCL0_init();
  while (1) {
    ;		
  }
}
]*

Note 1: Adapted from Microchip Technical Note TB3218 "Getting Started with CCL"

Note 3: Pin 6 is also the UPDI pin, so using this will require disabling the UPDI interface.