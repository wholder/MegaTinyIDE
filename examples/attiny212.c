
#pragma chip attiny212

#include <avr/io.h>
#include <stdbool.h>

//            +----------+
//				Vdd | 1   	20 | Gnd					PORTA |7|6|-|-|3|2|1|0|
//    		PA6 | 2   	19 | PA3/CLK
//    		PA7 | 3   	18 | PA0/UPDI/RESET
//    		PA1 | 4   	17 | PA2
//            +----------+

#define SET_BIT(P, B) asm volatile("sbi %0, %1" : : "I" (_SFR_IO_ADDR(P)), "I" (B))
#define CLR_BIT(P, B) asm volatile("cbi %0, %1" : : "I" (_SFR_IO_ADDR(P)), "I" (B))

#define BIT_SET(P, B) (((P) & (1 << (B))) != 0)
#define BIT_CLR(P, B) (((P) & (1 << (B))) == 0)

int main () {
	SET_BIT(VPORTA.DIR, 3);							// Set bit 3 in PORTA (pin 7) to output
	while (true) {
		SET_BIT(VPORTA.OUT, 3);						// Set bit 3 in PORTA (pin 7) HIGH
		CLR_BIT(VPORTA.OUT, 3);						// Clr bit 3 in PORTA (pin 7) LOW
	}
}