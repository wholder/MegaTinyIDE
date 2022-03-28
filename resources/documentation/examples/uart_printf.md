# DAC0 Sine Wave

This TinyAVR example code shows how to printf() a string to UART0's TXD output (pin 2, PA6)

*[CODE_BLOCK:
#pragma chip 	   attiny212               // Select target device
#pragma clock	   3333333                 // Clock frequency on reset is 3.33 MHz (20 Mhz / 6)

#include <avr/io.h>
#include <util/delay.h>
#include <stdio.h>

static int USART0_printChar (char c, FILE *stream) { 
  while (!(USART0.STATUS & USART_DREIF_bm)) {
    ;
  }
  USART0.TXDATAL = c;
  return 0; 
}

static FILE USART_stream = FDEV_SETUP_STREAM(USART0_printChar, NULL, _FDEV_SETUP_WRITE);

static void USART0_init (const uint16_t baud_rate) {
  PORTA.DIR |= PIN6_bm;
  USART0.BAUD = (uint16_t) ((float)(F_CPU * 64 / (16 * (float) baud_rate)) + 0.5);
  USART0.CTRLB |= USART_TXEN_bm;  
  stdout = &USART_stream;
}

int main (void) {
  uint8_t count = 0;
  USART0_init(9600);
  while (1) {
    printf("Counter value is: %d\n\r", count++);
    _delay_ms(1000);
  }
}
]*

Note 1: Adapted from Microchip Technical Note TB3216 "Getting Started with USART"