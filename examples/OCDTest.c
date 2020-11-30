#include <avr/io.h>
#include <util/delay.h>

#pragma chip 	attiny212
#pragma clock 20 MHz

//            +----------+
//				Vdd | 1   	 8 | Gnd					PORTA |7|6|-|-|3|2|1|0|
//    		PA6 | 2   	 7 | PA3/CLK
//    		PA7 | 3   	 6 | PA0/UPDI/RESET
//    		PA1 | 4   	 5 | PA2
//            +----------+

#define SYSCFG_OCDM     _SFR_IO8(0x0F18)    // Message port
#define SYSCFG_OCDMS    _SFR_IO8(0x0F19)    // Status Port (bit 0 is HIGH while busy)

void ocd_print (char* pmsg) {
  uint8_t timeout = 100;
  // Send the message one byte at a time
  while (*pmsg) {
    while (timeout-- && (SYSCFG_OCDMS & 0x01) != 0) {
      // wait for debugger to read last char sent, or a timeout
    	_delay_ms(1);
    }
    // If the debugger fails to collect and timeout expires, return
    if (timeout == 0) {
      return;
    }
    // Send next byte of message, then delay 50 ms
    SYSCFG_OCDM = *pmsg++;
    _delay_ms(50);
  }
}

int main (void) {
  CPU_CCP = 0xD8;                 			// Enable Configuration Change Protected register
	CLKCTRL.MCLKCTRLB = 0x00;							// Set Clock to 20 MHz
  while (1) {
    ocd_print("Hello World!\n");
    _delay_ms(2000);										// Delay 2 seconds
  }
}
