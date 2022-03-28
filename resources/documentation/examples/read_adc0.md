# Read from ADC0

This TinyAVR example code shows how to configure ADC0 to read a 10 bit value from channel 6 (pin 2) of an attiny212

*[CODE_BLOCK:
#include <avr/io.h>
#include <avr/interrupt.h>

  // Read 10 bit value from ADC channel 6 (pin 2)

#pragma chip 	attiny212

void      ADC0_init (void);
uint16_t  ADC0_read (void);

void ADC0_init (void) {
  PORTA.PIN6CTRL &= ~PORT_ISC_gm;               // Disable digital input buffer
  PORTA.PIN6CTRL |= PORT_ISC_INPUT_DISABLE_gc;
  PORTA.PIN6CTRL &= ~PORT_PULLUPEN_bm;          // Disable pull-up resistor
  ADC0.CTRLC = ADC_PRESC_DIV4_gc                // CLK_PER divided by 4
             | ADC_REFSEL_INTREF_gc;            // Internal reference
  ADC0.CTRLA = ADC_ENABLE_bm                    // ADC Enable: enabled
             | ADC_RESSEL_10BIT_gc;             // 10-bit mode
  ADC0.MUXPOS  = ADC_MUXPOS_AIN6_gc;            // Select ADC channel 6
}

uint16_t ADC0_read (void) {
  ADC0.COMMAND = ADC_STCONV_bm;                 // Start ADC conversion
  while (!(ADC0.INTFLAGS & ADC_RESRDY_bm)) {    // Wait until ADC conversion done
    ;
  }
  ADC0.INTFLAGS = ADC_RESRDY_bm;                // Clear the interrupt flag
  return ADC0.RES;
}

int main (void) {
  ADC0_init();
  uint16_t adcVal = ADC0_read();
  while (1) {
    ;
  }
}
]*
