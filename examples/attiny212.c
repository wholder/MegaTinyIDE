
#pragma chip attiny212

#include <avr/io.h>
#include <stdbool.h>
#include <util/delay.h>

//            +----------+
//				Vdd | 1   	 8 | Gnd					PORTA |7|6|-|-|3|2|1|0|
//    		PA6 | 2   	 7 | PA3/CLK
//    		PA7 | 3   	 6 | PA0/UPDI/RESET
//    		PA1 | 4   	 5 | PA2
//            +----------+

#define	TOGGLE 1

#define SET_BIT(P, B) asm volatile("sbi %0, %1" : : "I" (_SFR_IO_ADDR(P)), "I" (B))
#define CLR_BIT(P, B) asm volatile("cbi %0, %1" : : "I" (_SFR_IO_ADDR(P)), "I" (B))

#define BIT_SET(P, B) (((P) & (1 << (B))) != 0)
#define BIT_CLR(P, B) (((P) & (1 << (B))) == 0)

int main () {
	SET_BIT(VPORTA.DIR, 3);							// Set bit 3 in PORTA (pin 7) to output
	while (true) {
#if TOGGLE
		SET_BIT(VPORTA.IN, 3);						// Set bit 3 in PORTA (pin 7) HIGH
#else
		SET_BIT(VPORTA.OUT, 3);						// Set bit 3 in PORTA (pin 7) HIGH
		CLR_BIT(VPORTA.OUT, 3);						// Clr bit 3 in PORTA (pin 7) LOW
#endif
    _delay_ms(200);										// Delay 200 ms
	}		
}