
#pragma chip attiny3217

#include <avr/io.h>
#include <stdbool.h>

//										 	Reset   LED BTN
// ATTiny3217 Cur Nano  pin 23  PA3 PB7


#define SET_BIT(P, B) asm volatile("sbi %0, %1" : : "I" (_SFR_IO_ADDR(P)), "I" (B))
#define CLR_BIT(P, B) asm volatile("cbi %0, %1" : : "I" (_SFR_IO_ADDR(P)), "I" (B))

#define BIT_SET(P, B) (((P) & (1 << (B))) != 0)
#define BIT_CLR(P, B) (((P) & (1 << (B))) == 0)

int main () {
	SET_BIT(VPORTA.DIR, 3);							// Set bit 3 in PORTA
  PORTB.PIN7CTRL |= PORT_PULLUPEN_bm; // use the internal pullup resistor on PB7
	while (true) {
		SET_BIT(VPORTA.OUT, 3);						// Set bit 3 in PORTA
		CLR_BIT(VPORTA.OUT, 3);						// Clr bit 3 in PORTA 
		if (BIT_CLR(VPORTB.IN, 7)) {			// Check if BTN is pressed (PB7 goes LOW)
			asm volatile("BREAK");
		}
	}
}