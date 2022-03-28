# DAC0 Sine Wave

This TinyAVR example code shows how to generate a 50 Hz Sine Wave to DAC0 (pin 2) of an attiny212

*[CODE_BLOCK:
#pragma chip 	   attiny212                       // Select target device
#pragma clock	   3333333                         // Clock frequency on reset is 3.33 MHz (20 Mhz / 6)

#define VREF_DELAY (25)                            // VREF Startup time (microseconds)
#define STEPS      (100)                           // Number of steps for a sine wave period
#define AMPLITUDE  (127)                           // Sine wave amplitude
#define DC_OFFSET  (128)                           // Sine wave DC offset
#define M_2PI      (2 * M_PI)                      // 2 * PI
#define FREQUENCY  (50)                            // Frequency of the sine wave (approx. 50 Hz)
#define STEP_DELAY ((1000000 / FREQUENCY) / STEPS) // Fixed point math to calculate Step delay (microseconds)

#include <avr/io.h>
#include <util/delay.h>
#include <math.h>

uint8_t sineWave[STEPS];                           // Sine wave samples

void sineWaveInit (void);
void VREF_init    (void);
void DAC0_init    (void);
void DAC0_setVal  (uint8_t val);

void sineWaveInit (void) {
  for (uint16_t ii = 0; ii < STEPS; ii++) {
    sineWave[ii] = DC_OFFSET + AMPLITUDE * sin(ii * M_2PI / STEPS);
  }
}

void VREF_init (void) {
  VREF.CTRLA |= VREF_DAC0REFSEL_4V34_gc;           // Voltage reference at 4.34V
  VREF.CTRLB |= VREF_DAC0REFEN_bm;                 // Enable DAC0/AC0 reference
  _delay_us(VREF_DELAY);                           // Wait VREF start-up time
}

void DAC0_init (void) {
  PORTA.PIN6CTRL &= ~PORT_ISC_gm;                  // Disable digital input buffer
  PORTA.PIN6CTRL |= PORT_ISC_INPUT_DISABLE_gc;
  PORTA.PIN6CTRL &= ~PORT_PULLUPEN_bm;             // Disable pull-up resistor
  DAC0.DATA = DC_OFFSET;                           // default value
  DAC0.CTRLA = DAC_ENABLE_bm                       // Enable DAC, Output Buffer, Run in Standby
             | DAC_OUTEN_bm | DAC_RUNSTDBY_bm;
}

void DAC0_setVal (uint8_t val) {
    DAC0.DATA = val;
}

int main (void) {
  uint16_t ii = 0;
  VREF_init();
  DAC0_init();
  sineWaveInit();
  while (1) {
    DAC0_setVal(sineWave[ii]);    
    ii = (ii + 1) % STEPS;        
    _delay_us(STEP_DELAY);
  }
}
]*`

Note 1: Adapted from Microchip Technical Note TB3210 "Getting Started with DAC"