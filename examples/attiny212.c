
#pragma chip 	attiny212
#pragma clock	20000000

#include <avr/io.h>
#include <stdbool.h>
#include <util/delay.h>

	//            +----------+
	//				Vdd | 1   	 8 | Gnd					PORTA |7|6|-|-|3|2|1|0|
	//    		PA6 | 2   	 7 | PA3/CLK
	//    		PA7 | 3   	 6 | PA0/UPDI/RESET
	//    		PA1 | 4   	 5 | PA2
	//            +----------+

	// Blink LED connected to Pin 7 (PA3)

#define SET_BIT(P, B) asm volatile("sbi %0, %1" : : "I" (_SFR_IO_ADDR(P)), "I" (B))
#define CLR_BIT(P, B) asm volatile("cbi %0, %1" : : "I" (_SFR_IO_ADDR(P)), "I" (B))

// Alternate way to set, clear, or toggle bits
//#define BIT_SET(P, B) (((P) & (1 << (B))) != 0)
//#define BIT_CLR(P, B) (((P) & (1 << (B))) == 0)

/*
    MCLKCTRLB values
      0x00  divide by 1   0000 0000   20 MHz
      0x01  divide by 2   0000 0001   10 MHz
      0x03  divide by 4   0000 0011   5 Mhz
      0x11  divide by 6   0001 0001   3.33.. MHz
      0x05  divide by 8   0000 0101   2.5 MHz
      0x13  divide by 10  0001 0011   2 Mhz
      0x15  divide by 12  0001 0101   1.66.. MHz
      0x07  divide by 16  0000 0111   1.25 MHz
      0x17  divide by 24  0001 0111   833.33.. kHz
      0x09  divide by 32  0000 1001   625 kHz
      0x19  divide by 48  0001 1001   416.66.. kHz
      0x0D  divide by 64  0000 1011   312.5 kHz
*/

char var1[32];

int main () {
  CPU_CCP = 0xD8;                 		// Enable Configuration Change Protected register
	CLKCTRL.MCLKCTRLB = 0x00;						// Set Clock to 20 MHz
	SET_BIT(VPORTA.DIR, 3);							// Set bit 3 in PORTA (pin 7) to output
	while (true) {
		SET_BIT(VPORTA.IN, 3);						// Toggle bit 3 in PORTA (pin 7)
    _delay_ms(500);										// Delay 500 ms
	}		
}