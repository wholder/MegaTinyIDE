
## VPORTA - Virtual Port A (bit-accessible via SBI & CBI)
*[BEGIN_REGS]*
*[REG_ITEM:0x0000,DIR,-,8|DIR(7-0)[Direction]]*
*[REG_ITEM:0x0001,OUT,-,8|OUT(7-0)[Output]]*
*[REG_ITEM:0x0002,IN,-,8|IN(7-0)[Input]]*
*[REG_ITEM:0x0003,INTFLAGS,-,8|INTFLAGS(7-0)[Interrupt Flags]]*
*[END_REGS]*

## VPORTB - Virtual Port B (bit-accessible via SBI & CBI)
*[BEGIN_REGS]*
*[REG_ITEM:0x0004,DIR,-,8|DIR(7-0)[Direction]]*
*[REG_ITEM:0x0005,OUT,-,8|OUT(7-0)[Output]]*
*[REG_ITEM:0x0006,IN,-,8|IN(7-0)[Input]]*
*[REG_ITEM:0x0007,INTFLAGS,-,8|INTFLAGS(7-0)[Interrupt Flags]]*
*[END_REGS]*

## VPORTC - Virtual Port C (bit-accessible via SBI & CBI)
*[BEGIN_REGS]*
*[REG_ITEM:0x0008,DIR,-,8|DIR(7-0)[Direction]]*
*[REG_ITEM:0x0009,OUT,-,8|OUT(7-0)[Output]]*
*[REG_ITEM:0x000A,IN,-,8|IN(7-0)[Input]]*
*[REG_ITEM:0x000B,INTFLAGS,-,8|INTFLAGS(7-0)[Interrupt Flags]]*
*[END_REGS]*

## GPIO - General Purpose I/O Registers (bit-accessible via SBI & CBI)
*[BEGIN_REGS]*
*[REG_ITEM:0x001C,GPIOR0,-,8|GPIOR(7-0)[General Purpose I/O Register 0]]*
*[REG_ITEM:0x001D,GPIOR1,-,8|GPIOR(7-0)[General Purpose I/O Register 1]]*
*[REG_ITEM:0x001E,GPIOR2,-,8|GPIOR(7-0)[General Purpose I/O Register 2]]*
*[REG_ITEM:0x001F,GPIOR3,-,8|GPIOR(7-0)[General Purpose I/O Register 3]]*
*[END_REGS]*

## CPU - CPU Registers
*[BEGIN_REGS]*
*[REG_ITEM:0x0030-03,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0034,CCP,-,8|CCP(7-0)[Configuration Change Protection]]*
*[REG_ITEM:0x0035-0C,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x003D,SP,-,8|SP(7-0)[Stack Pointer LSB]]*
*[REG_ITEM:0x003E,SP,-,8|SP(15-8)[Stack Pointer MSB]]*
*[REG_ITEM:0x003F,SREG,-,I[Global Interrupt Enable],T[Transfer],H[Half Carry],S[Sign],V[Two’s Complement Overflow],N[Negative],Z[Zero],C[Carry]]*
*[END_REGS]*

## RSTCTRL - Reset Controller
*[BEGIN_REGS]*
*[REG_ITEM:0x0040,RSTFR,-,-,-,UPDIRF[UPDI Reset],SWRF[Software Reset],WDRF[Watchdog Reset],EXTRF[External Reset],BORF[Brown-out Reset],PORF[Power-on Reset]]*
*[REG_ITEM:0x0041,SWRR,-,-,-,-,-,-,-,-,SWRE[Software Reset Enable]]*
*[END_REGS]*

## SLPCTRL - Sleep Controller
*[BEGIN_REGS]*
*[REG_ITEM:0x0050,CTRLA,-,-,-,-,-,-,2|SMODE[Sleep Mode],SEN[Sleep Enable]]*
*[END_REGS]*

## CLKCTRL - Clock Controller
*[BEGIN_REGS]*
*[REG_ITEM:0x0060,MCLKCTRLA,-,CLKOUT[System Clock Out],-,-,-,-,-,2|CLKSEL[Clock Select]]*
*[REG_ITEM:0x0061,MCLKCTRLB,-,-,-,-,4|PDIV[Prescaler Division],PEN[Prescaler Enable]]*
*[REG_ITEM:0x0062,MCLKLOCK,-,-,-,-,-,-,-,-,LOCKEN[Lock Enable]]*
*[REG_ITEM:0x0063,MCLKSTATUS,-,EXTS[External Clock Status],XOSC32KS[XOSC32K Status],OSC32KS[OSCULP32K Status],OSC20MS[OSC20M Status],-,-,-,SOSC[Main Clock Oscillator Changing]]*
*[REG_ITEM:0x0064-6F,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0070,OSC20MCTRLA,-,-,-,-,-,-,-,RUNSTDBY[Run in Standby],-]*
*[REG_ITEM:0x0071,OSC20MCALIBA,-,-,-,-,5|CAL20M[Calibration]]*
*[REG_ITEM:0x0072,OSC20MCALIBB,-,LOCK[Oscillator Calibration Locked by Fuse],-,-,-,4|TEMPCAL20M[Oscillator Temperature Coefficient Calibration]]*
*[REG_ITEM:0x0073-77,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0078,OSC32KCTRLA,-,-,-,-,-,-,-,RUNSTDBY[Run in Standby],-]*
*[REG_ITEM:0x0079-1B,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x007C,XOSC32KCTRLA,-,-,-,2|CSUT[Crystal Start-Up Time],-,SEL[Source Select],RUNSTDBY[Run in Standby],ENABLE[Enable]]*
*[END_REGS]*

## BOD - Brown Out Detetector
*[BEGIN_REGS]*
*[REG_ITEM:0x0080,CTRLA,-,-,-,-,SAMPFREQ[Sample Frequency],2|ACTIVE[Active],2|SLEEP[Sleep]]*
*[REG_ITEM:0x0081,CTRLB,-,-,-,-,-,-,3|LVL[BOD Level]]*
*[REG_ITEM:0x0082-07,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0088,VLMCTRLA,-,-,-,-,-,-,-,2|VLMLVL[VLM Level]]*
*[REG_ITEM:0x0089,INTCTRL,-,-,-,-,-,-,2|VLMCFG[VLM Configuration],VLMIE[VLM Interrupt Enable]]*
*[REG_ITEM:0x008A,INTFLAGS,-,-,-,-,-,-,-,-,VLMIF[VLM Interrupt Flag]]*
*[REG_ITEM:0x008B,STATUS,-,-,-,-,-,-,-,-,VLMS[VLM Status]]*
*[END_REGS]*

## VREF - Voltage Reference
*[BEGIN_REGS]*
*[REG_ITEM:0x00A0,CTRLA,-,-,3|ADC0REFSEL[ADC0 Reference Select],-,3|DAC0REFSEL[DAC0 and AC0 Reference Select]]*
*[REG_ITEM:0x00A1,CTRLB,-,-,-,DAC2REFEN[DAC2 and AC2 Reference Force Enable],ADC1REFEN[ADC1 Reference Force Enable],DAC1REFEN[DAC1 and AC1 Reference Force Enable],-,ADC0REFEN[ADC0 Reference Force Enable],DAC0REFEN[DAC0 and AC0 Reference Force Enable]]*
*[REG_ITEM:0x00A2,CTRLC,-,-,3|ADC1REFSEL[ADC1 Reference Select],-,3|DAC1REFSEL[DAC1 and AC1 Reference Select]]*
*[REG_ITEM:0x00A3,CTRLD,-,-,-,-,-,-,3|DAC2REFSEL[DAC2 and AC2 Reference Select]]*
*[END_REGS]*

## WDT - Watchdog Timer
*[BEGIN_REGS]*
*[REG_ITEM:0x0100,CTRLA,-,4|WINDOW[Window],4|PERIOD[Period]]*
*[REG_ITEM:0x0101,STATUS,-,LOCK[Lock,-,-,-,-,-,-,SYNCBUSY[Synchronization Busy]]]*
*[END_REGS]*

## CPUINT - CPU Interrupt Controller
*[BEGIN_REGS]*
*[REG_ITEM:0x0110,CTRLA,-,-,IVSEL[Interrupt Vector Select],CVT[Compact Vector Table],-,-,-,-,LVL0RR[Round Robin Priority Enable]]*
*[REG_ITEM:0x0111,STATUS,-,NMIEX[Non-Maskable Interrupt Executing],-,-,-,-,-,LVL1EX[Level 1 Interrupt Executing],LVL0EX[Level 0 Interrupt Executing]]*
*[REG_ITEM:0x0112,LVL0PRI,-,8|LVL0PRI(7-0)[Interrupt Priority Level 0]]*
*[REG_ITEM:0x0113,LVL1VEC,-,8|LVL1VEC(7-0)[Interrupt Vector with Priority Level 1]]*
*[END_REGS]*


## CRCSCAN - Cyclic Redundancy Check Memory Scan
*[BEGIN_REGS]*
*[REG_ITEM:0x0120,CTRLA,-,RESET[Reset CRCSCAN],-,-,-,-,-,NMIEN[Enable NMI Trigger],ENABLE[Enable CRCSCAN]]*
*[REG_ITEM:0x0121,CTRLB,-,-,-,2|MODE[CRC Flash Access Mode],-,-,2|SRC[CRC Source]]*
*[REG_ITEM:0x0122,STATUS,-,-,-,-,-,-,-,OK[CRC OK],BUSY[CRC Busy]]*
*[END_REGS]*

## RTC - Real-Time Counter
*[BEGIN_REGS]*
*[REG_ITEM:0x0140,CTRLA,-,RUNSTDBY[Run in Standby],4|PRESCALER[Prescaler],-,-,RTCEN[RTC Peripheral Enable]]*
*[REG_ITEM:0x0141,STATUS,-,-,-,-,-,CMPBUSY[Compare Synchronization Busy],PERBUSY[Period Synchronization Busy],CNTBUSY[Counter Synchronization Busy],CTRLABUSY[Control A Synchronization Busy]]*
*[REG_ITEM:0x0142,INTCTRL,-,-,-,-,-,-,-,CMP[Compare Match Interrupt Enable],OVF[Overflow Interrupt Enable]]*
*[REG_ITEM:0x0143,INTFLAGS,-,-,-,-,-,-,-,CMP[Compare Match Interrupt Flag],OVF[Overflow Interrupt Flag]]*
*[REG_ITEM:0x0144,TEMP,-,8|TEMP(7-0)[Temporary Register]]*
*[REG_ITEM:0x0145,DBGCTRL,-,-,-,-,-,-,-,-,DBGRUN[Debug Run]]*
*[REG_ITEM:0x0146,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0147,CLKSEL,-,-,-,-,-,-,-,2|CLKSEL[Clock Select]]*
*[REG_ITEM:0x0148,CNT,-,8|CNT(7-0)[Counter Low Byte]]*
*[REG_ITEM:0x0149,CNT,-,8|CNT(15-8)[Counter High Byte]]*
*[REG_ITEM:0x014B,PER,-,8|PER(7-0)[Period Low Byte]]*
*[REG_ITEM:0x014B,PER,-,8|PER(15-8[Period High Byte]]*
*[REG_ITEM:0x014C,CMP,-,8|CMP(7-0)[Compare Low Byte]]*
*[REG_ITEM:0x014D,CMP,-,8|CMP(15-8)[Compare High Byte]]*
*[REG_ITEM:0x014E-0F,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0150,PITCTRLA,-,-,PERIOD[Period],-,-,PITEN[Periodic Interrupt Timer Enable]]*
*[REG_ITEM:0x0151,PITSTATUS,-,-,-,-,-,-,-,-,CTRLBUSY[PITCTRLA Synchronization Busy]]*
*[REG_ITEM:0x0152,PITINTCTRL,-,-,-,-,-,-,-,-,PI[Periodic Interrupt]]*
*[REG_ITEM:0x0153,PITINTFLAGS,-,-,-,-,-,-,-,-,PI[Periodic Interrupt Flag]]*
*[REG_ITEM:0x0154,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0155,PITDBGCTRL,-,-,-,-,-,-,-,-,DBGRUN[Debug Run]]*
*[END_REGS]*

## EVSYS - Event System
*[BEGIN_REGS]*
*[REG_ITEM:0x0180,ASYNCSTROBE,-,8|ASYNCSTROBE[Asynchronous Channel Strobe]]*
*[REG_ITEM:0x0181,SYNCSTROBE,-,8|SYNCSTROBE[Synchronous Channel Strobe]]*
*[REG_ITEM:0x0182,ASYNCCH0,-,8|ASYNCCH(7-0)[Asynchronous Channel Generator Selection]]*
*[REG_ITEM:0x0183,ASYNCCH1,-,8|ASYNCCH(7-0)[Asynchronous Channel Generator Selection]]*
*[REG_ITEM:0x0184,ASYNCCH2,-,8|ASYNCCH(7-0)[[Asynchronous Channel Generator Selection]]]*
*[REG_ITEM:0x0185,ASYNCCH3,-,8|ASYNCCH(7-0)[[Asynchronous Channel Generator Selection]]]*
*[REG_ITEM:0x0186-09,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x018A,SYNCCH0,-,8|SYNCCH(7-0)[Synchronous Channel Generator Selection]]*
*[REG_ITEM:0x018B,SYNCCH1,-,8|SYNCCH(7-0)[Synchronous Channel Generator Selection]]*
*[REG_ITEM:0x018C-91,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0192,ASYNCUSER0,-,8|ASYNCUSER0(7-0)[Asynchronous User Channel Selection]]*
*[REG_ITEM:0x0193,ASYNCUSER1,-,8|ASYNCUSER(7-0)[Asynchronous User Channel Selection]]*
*[REG_ITEM:0x0194,ASYNCUSER2,-,8|ASYNCUSER(7-0)[Asynchronous User Channel Selection]]*
*[REG_ITEM:0x0195,ASYNCUSER3,-,8|ASYNCUSER(7-0)[Asynchronous User Channel Selection]]*
*[REG_ITEM:0x0196,ASYNCUSER4,-,8|ASYNCUSER(7-0)[Asynchronous User Channel Selection]]*
*[REG_ITEM:0x0196,ASYNCUSER5,-,8|ASYNCUSER(7-0)[Asynchronous User Channel Selection]]*
*[REG_ITEM:0x0197,ASYNCUSER6,-,8|ASYNCUSER(7-0)[Asynchronous User Channel Selection]]*
*[REG_ITEM:0x0198,ASYNCUSER7,-,8|ASYNCUSER(7-0)[Asynchronous User Channel Selection]]*
*[REG_ITEM:0x019A,ASYNCUSER8,-,8|ASYNCUSER(7-0)[Asynchronous User Channel Selection]]*
*[REG_ITEM:0x019B,ASYNCUSER9,-,8|ASYNCUSER(7-0)[Asynchronous User Channel Selection]]*
*[REG_ITEM:0x019C,ASYNCUSER10,-,8|ASYNCUSER(7-0)[Asynchronous User Channel Selection]]*
*[REG_ITEM:0x019D,ASYNCUSER11,-,8|ASYNCUSER(7-0)[Asynchronous User Channel Selection]]*
*[REG_ITEM:0x019E,ASYNCUSER12,-,8|ASYNCUSER(7-0)[Asynchronous User Channel Selection]]*
*[REG_ITEM:0x019F-A1,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x01A2,SYNCUSER0,-,8|SYNCUSER(7-0)[Synchronous User Channel Selection]]*
*[REG_ITEM:0x01A3,SYNCUSER1,-,8|SYNCUSER(7-0)[Synchronous User Channel Selection]]*
*[END_REGS]*

## CCL - Configurable Custom Logic<a name='CCL'></a>
*[BEGIN_REGS]*
*[REG_ITEM:0x01C0,CTRLA,-,-,RUNSTDBY[Run in Standby],-,-,-,-,-,ENABLE[Enable]]*
*[REG_ITEM:0x01C1,SEQCTRL0,-,-,-,-,-,4|SEQSEL[Sequential Selection]]*
*[REG_ITEM:0x01C2-C4,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x01C5,LUT0CTRLA,-,EDGEDET[Edge Detection],CLKSRC[Clock Source Selection],2|FILTSEL[Filter Selection],OUTEN[Output Enable],-,-,ENABLE[LUT Enable]]*
*[REG_ITEM:0x01C6,LUT0CTRLB,-,4|INSEL1[LUT 0 Input 1 Source Selection],4|INSEL0[LUT 0 Input 0 Source Selection]]*
*[REG_ITEM:0x01C7,LUT0CTRLC,-,-,-,-,-,4|INSEL2[LUT 0 Input 2 Source Selection]]*
*[REG_ITEM:0x01C8,TRUTH0,-,8|TRUTH(7-0)[LUT 0 Truth Table]]*
*[REG_ITEM:0x01C9,LUT1CTRLA,-,EDGEDET[Edge Detection],CLKSRC[Clock Source Selection],2|FILTSEL[Filter Selection],OUTEN[Output Enable],-,-,ENABLE[LUT Enable]]*
*[REG_ITEM:0x01CA,LUT1CTRLB,-,4|INSEL1[LUT 1 Input 1 Source Selection],4|INSEL0[LUT 1 Input 0 Source Selection]]*
*[REG_ITEM:0x01CB,LUT1CTRLC,-,-,-,-,-,4|INSEL2[LUT 1 Input 2 Source Selection]]*
*[REG_ITEM:0x01CC,TRUTH1,-,8|TRUTH(7-0)[LUT 1 Truth Table]]*
*[END_REGS]*

## PORTMUX - Port Multiplexer
*[BEGIN_REGS]*
*[REG_ITEM:0x0200,CTRLA,-,-,-,LUT1[CCL LUT 1 Output],LUT0[CCL LUT 0 Output],-,EVOUT2[Event Output 2],EVOUT1[Event Output 1],EVOUT0[Event Output 0]]*
*[REG_ITEM:0x0201,CTRLB,-,-,-,-,TWI0[TWI 0 Communication],-,SPI0[SPI 0 Communication],-,USART0[USART 0 Communication]]*
*[REG_ITEM:0x0202,CTRLC,-,-,-,TCA05[TCA0 Waveform Output 5],TCA04[TCA0 Waveform Output 4],TCA03[TCA0 Waveform Output 3],TCA02[TCA0 Waveform Output 2],TCA01[TCA0 Waveform Output 1],TCA00[TCA0 Waveform Output 0]]*
*[REG_ITEM:0x0203,CTRLD,-,-,-,-,-,-,-,TCB1[TCB1 Output],TCB0[TCB0 Output]]*
*[END_REGS]*

## PORTA - I/O Pin Configuration
*[BEGIN_REGS]*
*[REG_ITEM:0x400,DIR,-,8|DIR(7-0)[Data Direction]]*
*[REG_ITEM:0x401,DIRSET,-,,8|DIRSET(7-0)[Data Direction Set]]*
*[REG_ITEM:0x402,DIRCLR,-,,8|DIRCLR(7-0)[Data Direction Clear]]*
*[REG_ITEM:0x403,DIRTGL,-,,8|DIRTGL(7-0)[Data Direction Toggle]]*
*[REG_ITEM:0x404,OUT,-,,8|OUT(7-0)[Output Value]]*
*[REG_ITEM:0x405,OUTSET,-,,8|OUTSET(7-0)[Output Value Set]]*
*[REG_ITEM:0x406,OUTCLR,-,,8|OUTCLR(7-0)[Output Value Clear]]*
*[REG_ITEM:0x407,OUTTGL,-,,8|OUTTGL(7-0)[Output Value Toggle]]*
*[REG_ITEM:0x408,IN,-,,8|IN(7-0)[Input Value]]*
*[REG_ITEM:0x409,INTFLAGS,-,,8|INTFLAGS(7-0)[Pin Interrupt Flag]]*
*[REG_ITEM:0x40A-0F,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x410,PIN0CTRL,-,INVEN[Inverted I/O Enable],-,-,-,PULLUPEN[Pull-up Enable],3|ISC[Input/Sense Configuration]]*
*[REG_ITEM:0x411,PIN1CTRL,-,INVEN[Inverted I/O Enable],-,-,-,PULLUPEN[Pull-up Enable],3|ISC[Input/Sense Configuration]]*
*[REG_ITEM:0x412,PIN2CTRL,-,INVEN[Inverted I/O Enable],-,-,-,PULLUPEN[Pull-up Enable],3|ISC[Input/Sense Configuration]]*
*[REG_ITEM:0x413,PIN3CTRL,-,INVEN[Inverted I/O Enable],-,-,-,PULLUPEN[Pull-up Enable],3|ISC[Input/Sense Configuration]]*
*[REG_ITEM:0x414,PIN4CTRL,-,INVEN[Inverted I/O Enable],-,-,-,PULLUPEN[Pull-up Enable],3|ISC[Input/Sense Configuration]]*
*[REG_ITEM:0x415,PIN5CTRL,-,INVEN[Inverted I/O Enable],-,-,-,PULLUPEN[Pull-up Enable],3|ISC[Input/Sense Configuration]]*
*[REG_ITEM:0x416,PIN6CTRL,-,INVEN[Inverted I/O Enable],-,-,-,PULLUPEN[Pull-up Enable],3|ISC[Input/Sense Configuration]]*
*[REG_ITEM:0x417,PIN7CTRL,-,INVEN[Inverted I/O Enable],-,-,-,PULLUPEN[Pull-up Enable],3|ISC[Input/Sense Configuration]]*
*[END_REGS]*

## PORTB - I/O Pin Configuration
*[BEGIN_REGS]*
*[REG_ITEM:0x420,DIR,-,8|DIR(7-0)[Data Direction]]*
*[REG_ITEM:0x421,DIRSET,-,,8|DIRSET(7-0)[Data Direction Set]]*
*[REG_ITEM:0x422,DIRCLR,-,,8|DIRCLR(7-0)[Data Direction Clear]]*
*[REG_ITEM:0x423,DIRTGL,-,,8|DIRTGL(7-0)[Data Direction Toggle]]*
*[REG_ITEM:0x424,OUT,-,,8|OUT(7-0)[Output Value]]*
*[REG_ITEM:0x425,OUTSET,-,,8|OUTSET(7-0)[Output Value Set]]*
*[REG_ITEM:0x426,OUTCLR,-,,8|OUTCLR(7-0)[Output Value Clear]]*
*[REG_ITEM:0x427,OUTTGL,-,,8|OUTTGL(7-0)[Output Value Toggle]]*
*[REG_ITEM:0x428,IN,-,,8|IN(7-0)[Input Value]]*
*[REG_ITEM:0x429,INTFLAGS,-,,8|INTFLAGS(7-0)[Pin Interrupt Flag]]*
*[REG_ITEM:0x42A-2F,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x430,PIN0CTRL,-,INVEN[Inverted I/O Enable],-,-,-,PULLUPEN[Pull-up Enable],3|ISC[Input/Sense Configuration]]*
*[REG_ITEM:0x431,PIN1CTRL,-,INVEN[Inverted I/O Enable],-,-,-,PULLUPEN[Pull-up Enable],3|ISC[Input/Sense Configuration]]*
*[REG_ITEM:0x432,PIN2CTRL,-,INVEN[Inverted I/O Enable],-,-,-,PULLUPEN[Pull-up Enable],3|ISC[Input/Sense Configuration]]*
*[REG_ITEM:0x433,PIN3CTRL,-,INVEN[Inverted I/O Enable],-,-,-,PULLUPEN[Pull-up Enable],3|ISC[Input/Sense Configuration]]*
*[REG_ITEM:0x434,PIN4CTRL,-,INVEN[Inverted I/O Enable],-,-,-,PULLUPEN[Pull-up Enable],3|ISC[Input/Sense Configuration]]*
*[REG_ITEM:0x435,PIN5CTRL,-,INVEN[Inverted I/O Enable],-,-,-,PULLUPEN[Pull-up Enable],3|ISC[Input/Sense Configuration]]*
*[REG_ITEM:0x436,PIN6CTRL,-,INVEN[Inverted I/O Enable],-,-,-,PULLUPEN[Pull-up Enable],3|ISC[Input/Sense Configuration]]*
*[REG_ITEM:0x437,PIN7CTRL,-,INVEN[Inverted I/O Enable],-,-,-,PULLUPEN[Pull-up Enable],3|ISC[Input/Sense Configuration]]*
*[END_REGS]*

## PORTC - I/O Pin Configuration
*[BEGIN_REGS]*
*[REG_ITEM:0x440,DIR,-,8|DIR(7-0)[Data Direction]]*
*[REG_ITEM:0x441,DIRSET,-,,8|DIRSET(7-0)[Data Direction Set]]*
*[REG_ITEM:0x442,DIRCLR,-,,8|DIRCLR(7-0)[Data Direction Clear]]*
*[REG_ITEM:0x443,DIRTGL,-,,8|DIRTGL(7-0)[Data Direction Toggle]]*
*[REG_ITEM:0x444,OUT,-,,8|OUT(7-0)[Output Value]]*
*[REG_ITEM:0x445,OUTSET,-,,8|OUTSET(7-0)[Output Value Set]]*
*[REG_ITEM:0x446,OUTCLR,-,,8|OUTCLR(7-0)[Output Value Clear]]*
*[REG_ITEM:0x447,OUTTGL,-,,8|OUTTGL(7-0)[Output Value Toggle]]*
*[REG_ITEM:0x448,IN,-,,8|IN(7-0)[Input Value]]*
*[REG_ITEM:0x449,INTFLAGS,-,,8|INTFLAGS(7-0)[Pin Interrupt Flag]]*
*[REG_ITEM:0x44A-4F,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x450,PIN0CTRL,-,INVEN[Inverted I/O Enable],-,-,-,PULLUPEN[Pull-up Enable],3|ISC[Input/Sense Configuration]]*
*[REG_ITEM:0x451,PIN1CTRL,-,INVEN[Inverted I/O Enable],-,-,-,PULLUPEN[Pull-up Enable],3|ISC[Input/Sense Configuration]]*
*[REG_ITEM:0x452,PIN2CTRL,-,INVEN[Inverted I/O Enable],-,-,-,PULLUPEN[Pull-up Enable],3|ISC[Input/Sense Configuration]]*
*[REG_ITEM:0x453,PIN3CTRL,-,INVEN[Inverted I/O Enable],-,-,-,PULLUPEN[Pull-up Enable],3|ISC[Input/Sense Configuration]]*
*[REG_ITEM:0x454,PIN4CTRL,-,INVEN[Inverted I/O Enable],-,-,-,PULLUPEN[Pull-up Enable],3|ISC[Input/Sense Configuration]]*
*[REG_ITEM:0x455,PIN5CTRL,-,INVEN[Inverted I/O Enable],-,-,-,PULLUPEN[Pull-up Enable],3|ISC[Input/Sense Configuration]]*
*[REG_ITEM:0x456,PIN6CTRL,-,INVEN[Inverted I/O Enable],-,-,-,PULLUPEN[Pull-up Enable],3|ISC[Input/Sense Configuration]]*
*[REG_ITEM:0x457,PIN7CTRL,-,INVEN[Inverted I/O Enable],-,-,-,PULLUPEN[Pull-up Enable],3|ISC[Input/Sense Configuration]]*
*[END_REGS]*

## ADC0 - Analog-to-Digital Converter<a name='ADC0'></a>
*[BEGIN_REGS]*
*[REG_ITEM:0x0600,CTRLA,-,RUNSTBY[Run in Standby],-,-,-,-,RESSEL[Resolution Selection],FREERUN[Free-Running],ENABLE[ADC Enable]]]*
*[REG_ITEM:0x0601,CTRLB,-,-,-,-,-,-,3|SAMPNUM[Sample Accumulation Number Select]]*
*[REG_ITEM:0x0602,CTRLC,-,-,SAMPCAP[Sample Capacitance Selection],2|REFSEL[Reference Selection],-,3|PRESC[Prescaler]]*
*[REG_ITEM:0x0603,CTRLD,-,3|INITDLY[Initialization Delay],ASDV[Automatic Sampling Delay Variation],4|SAMPDLY[Sampling Delay Selection]]*
*[REG_ITEM:0x0604,CTRLE,-,-,-,-,-,-,3|WINCM[Window Comparator Mode]]*
*[REG_ITEM:0x0605,SAMPCTRL,-,-,-,-,5|SAMPLEN[Sample Length]]*
*[REG_ITEM:0x0606,MUXPOS,-,-,-,-,5|MUXPOS[MUXPOS]]*
*[REG_ITEM:0x0607,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0608,COMMAND,-,-,-,-,-,-,-,-,STCONV[Start Conversion]]*
*[REG_ITEM:0x0609,EVCTRL,-,-,-,-,-,-,-,-,STARTEI[Start Event Input]]*
*[REG_ITEM:0x060A,INTCTRL,-,-,-,-,-,-,-,WCMP[Window Comparator Interrupt Enable],RESRDY[Result Ready Interrupt Enable]]*
*[REG_ITEM:0x060B,INTFLAGS,-,-,-,-,-,-,-,WCMP[Window Comparator Interrupt Flag],RESRDY[Result Ready Interrupt Flag]]*
*[REG_ITEM:0x060C,DBGCTRL,-,-,-,-,-,-,-,-,DBGRUN[Debug Run]]*
*[REG_ITEM:0x060D,TEMP,-,8|TEMP(7-0)[Temporary Register]]*
*[REG_ITEM:0x060E-0F,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0610,RES,-,8|RES(7-0)[Result low byte]]*
*[REG_ITEM:0x0611,RES,-,8|RES(15-8)[Result high byte]]*
*[REG_ITEM:0x0612,WINLT,-,8|WINLT(7-0)[Window Comparator Low Threshold Low Byte]]*
*[REG_ITEM:0x0613,WINLT,-,8|WINLT(15-8)[Window Comparator Low Threshold High Byte]]*
*[REG_ITEM:0x0614,WINHT,-,8|WINHT(7-0)[Window Comparator High Threshold Low Byte]]*
*[REG_ITEM:0x0615,WINHT,-,8|WINHT(15-8)[Window Comparator High Threshold High Byte]]*
*[REG_ITEM:0x0616,CALIB,-,-,-,-,-,-,-,-,DUTYCYC[Duty Cycle]]*
*[END_REGS]*

## ADC1 - Analog-to-Digital Converter<a name='ADC1'></a>
*[BEGIN_REGS]*
*[REG_ITEM:0x0640,CTRLA,-,RUNSTBY[Run in Standby],-,-,-,-,RESSEL[Resolution Selection],FREERUN[Free-Running],ENABLE[ADC Enable]]]*
*[REG_ITEM:0x0641,CTRLB,-,-,-,-,-,-,3|SAMPNUM[Sample Accumulation Number Select]]*
*[REG_ITEM:0x0642,CTRLC,-,-,SAMPCAP[Sample Capacitance Selection],2|REFSEL[Reference Selection],-,3|PRESC[Prescaler]]*
*[REG_ITEM:0x0643,CTRLD,-,3|INITDLY[Initialization Delay],ASDV[Automatic Sampling Delay Variation],4|SAMPDLY[Sampling Delay Selection]]*
*[REG_ITEM:0x0644,CTRLE,-,-,-,-,-,-,3|WINCM[Window Comparator Mode]]*
*[REG_ITEM:0x0645,SAMPCTRL,-,-,-,-,5|SAMPLEN[Sample Length]]*
*[REG_ITEM:0x0646,MUXPOS,-,-,-,-,5|MUXPOS[MUXPOS]]*
*[REG_ITEM:0x0647,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0648,COMMAND,-,-,-,-,-,-,-,-,STCONV[Start Conversion]]*
*[REG_ITEM:0x0649,EVCTRL,-,-,-,-,-,-,-,-,STARTEI[Start Event Input]]*
*[REG_ITEM:0x064A,INTCTRL,-,-,-,-,-,-,-,WCMP[Window Comparator Interrupt Enable],RESRDY[Result Ready Interrupt Enable]]*
*[REG_ITEM:0x064B,INTFLAGS,-,-,-,-,-,-,-,WCMP[Window Comparator Interrupt Flag],RESRDY[Result Ready Interrupt Flag]]*
*[REG_ITEM:0x064C,DBGCTRL,-,-,-,-,-,-,-,-,DBGRUN[Debug Run]]*
*[REG_ITEM:0x064D,TEMP,-,8|TEMP(7-0)[Temporary Register]]*
*[REG_ITEM:0x064E-4F,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0650,RES,-,8|RES(7-0)[Result low byte]]*
*[REG_ITEM:0x0651,RES,-,8|RES(15-8)[Result high byte]]*
*[REG_ITEM:0x0652,WINLT,-,8|WINLT(7-0)[Window Comparator Low Threshold Low Byte]]*
*[REG_ITEM:0x0653,WINLT,-,8|WINLT(15-8)[Window Comparator Low Threshold High Byte]]*
*[REG_ITEM:0x0654,WINHT,-,8|WINHT(7-0)[Window Comparator High Threshold Low Byte]]*
*[REG_ITEM:0x0655,WINHT,-,8|WINHT(15-8)[Window Comparator High Threshold High Byte]]*
*[REG_ITEM:0x0656,CALIB,-,-,-,-,-,-,-,-,DUTYCYC[Duty Cycle]]*
*[END_REGS]*

## AC0 - Analog Comparator<a name='AC0'></a>
*[BEGIN_REGS]*
*[REG_ITEM:0x0680,CTRLA,-,RUNSTDBY[Run in Standby Mode],OUTEN[Analog Comparator Output Pad Enable],2|INTMODE[Interrupt Modes],LPMODE[Low-Power Mode],2|HYSMODE[Hysteresis Mode Select],ENABLE[Enable AC]]*
*[REG_ITEM:0x0681,reserved,-,]*
*[REG_ITEM:0x0682,MUXCTRLA,-,INVERT[Invert AC Output],-,-,2|MUXPOS[Positive Input MUX Selection],-,2|MUXNEG[Negative Input MUX Selection]]*
*[REG_ITEM:0x0683-85,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0686,INTCTRL,-,-,-,-,-,-,-,-,CMP[Analog Comparator Interrupt Enable]]*
*[REG_ITEM:0x0687,STATUS,-,-,-,-,STATE[Analog Comparator State],-,-,-,CMP[Analog Comparator Interrupt Flag]]*
*[END_REGS]*

## AC1 - Analog Comparator<a name='AC1'></a>
*[BEGIN_REGS]*
*[REG_ITEM:0x0688,CTRLA,-,RUNSTDBY[Run in Standby Mode],OUTEN[Analog Comparator Output Pad Enable],2|INTMODE[Interrupt Modes],LPMODE[Low-Power Mode],2|HYSMODE[Hysteresis Mode Select],ENABLE[Enable AC]]*
*[REG_ITEM:0x0689,reserved,-,]*
*[REG_ITEM:0x068A,MUXCTRLA,-,INVERT[Invert AC Output],-,-,2|MUXPOS[Positive Input MUX Selection],-,2|MUXNEG[Negative Input MUX Selection]]*
*[REG_ITEM:0x068B-8D,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x068E,INTCTRL,-,-,-,-,-,-,-,-,CMP[Analog Comparator Interrupt Enable]]*
*[REG_ITEM:0x068F,STATUS,-,-,-,-,STATE[Analog Comparator State],-,-,-,CMP[Analog Comparator Interrupt Flag]]*
*[END_REGS]*

## AC2 - Analog Comparator<a name='AC2'></a>
*[BEGIN_REGS]*
*[REG_ITEM:0x0690,CTRLA,-,RUNSTDBY[Run in Standby Mode],OUTEN[Analog Comparator Output Pad Enable],2|INTMODE[Interrupt Modes],LPMODE[Low-Power Mode],2|HYSMODE[Hysteresis Mode Select],ENABLE[Enable AC]]*
*[REG_ITEM:0x0691,reserved,-,]*
*[REG_ITEM:0x0692,MUXCTRLA,-,INVERT[Invert AC Output],-,-,2|MUXPOS[Positive Input MUX Selection],-,2|MUXNEG[Negative Input MUX Selection]]*
*[REG_ITEM:0x0693-95,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0696,INTCTRL,-,-,-,-,-,-,-,-,CMP[Analog Comparator Interrupt Enable]]*
*[REG_ITEM:0x0697,STATUS,-,-,-,-,STATE[Analog Comparator State],-,-,-,CMP[Analog Comparator Interrupt Flag]]*
*[END_REGS]*

## DAC0 - Digital to Analog Converter<a name='DAC0'></a>
*[BEGIN_REGS]*
*[REG_ITEM:0x06A0,CTRLA,-,RUNSTDBY[Run in Standby Mode],OUTEN[Output Buffer Enable],-,-,-,-,-,ENABLE[DAC Enable]]*
*[REG_ITEM:0x06A1,DATA,-,8|DATA(7-0)[Data]]*
*[END_REGS]*

## DAC1 - Digital to Analog Converter<a name='DAC1'></a>
*[BEGIN_REGS]*
*[REG_ITEM:0x06A8,CTRLA,-,RUNSTDBY[Run in Standby Mode],OUTEN[Output Buffer Enable],-,-,-,-,-,ENABLE[DAC Enable]]*
*[REG_ITEM:0x06A9,DATA,-,8|DATA(7-0)[Data]]*
*[END_REGS]*

## DAC2 - Digital to Analog Converter<a name='DAC2'></a>
*[BEGIN_REGS]*
*[REG_ITEM:0x06B0,CTRLA,-,RUNSTDBY[Run in Standby Mode],OUTEN[Output Buffer Enable],-,-,-,-,-,ENABLE[DAC Enable]]*
*[REG_ITEM:0x06B1,DATA,-,8|DATA(7-0)[Data]]*
*[END_REGS]*

## USART0 - (Asynchronous Mode)<a name='USART0'></a>
*[BEGIN_REGS]*
*[REG_ITEM:0x0800,RXDATAL,-,8|DATA(7-0)[Receiver Data Register LSB]]*
*[REG_ITEM:0x0801,RXDATAH,-,RXCIF[Receive Complete Interrupt Flag],BUFOVF[Buffer Overflow],-,-,-,FERR[Frame Error],PERR[Parity Error],DATA(8)[Receiver Data Register High Bit]]*
*[REG_ITEM:0x0802,TXDATAL,-,8|DATA(7-0)[Transmit Data Register LSB]]*
*[REG_ITEM:0x0803,TXDATAH,-,-,-,-,-,-,-,-,DATA(8)[Transmit Data Register High Bit]]*
*[REG_ITEM:0x0804,STATUS,-,RXCIF[Receive Complete Interrupt Flag],TXCIF[Transmit Complete Interrupt Flag],DREIF[Data Register Empty Flag],RXSIF[Receive Start Interrupt Flag],ISFIF[Inconsistent Sync Field Interrupt Flag],-,BDF[Break Detected Flag],WFB[Wait For Break]]*
*[REG_ITEM:0x0805,CTRLA,-,RXCIE[Receive Complete Interrupt Enable],TXCIE[Transmit Complete Interrupt Enable],DREIE[Data Register Empty Interrupt Enable],RXSIE[Receiver Start Frame Interrupt Enable],LBME[Loop-back Mode Enable],ABEIE[Auto-baud Error Interrupt Enable],2|RS485[RS-485 Mode]]*
*[REG_ITEM:0x0806,CTRLB,-,RXEN[Receiver Enable],TXEN[Transmitter Enable],-,SFDEN[Start-of-Frame Detection Enable],ODME[Open Drain Mode Enable],2|RXMODE[Receiver Mode],MPCM[Multi-Processor Communication Mode]]*
*[REG_ITEM:0x0807,CTRLC,*,2|CMODE[Communication Mode],2|PMODE[Parity Mode],SBMODE[Stop Bit Mode],3|CHSIZE[Character Size]]*
*[REG_ITEM:0x0808,BAUD,-,8|BAUD(7-0)[Baud Rate Low Byte]]*
*[REG_ITEM:0x0809,BAUD,-,8|BAUD(15-8)[Baud Rate High Byte]]*
*[REG_ITEM:0x080A,CTRLD,-,2|ABW,-,-,-,-,-,-]*
*[REG_ITEM:0x080B,DBGCTRL,-,-,-,-,-,-,-,-,DBGRUN[Debug Run]]*
*[REG_ITEM:0x080C,EVCTRL,-,-,-,-,-,-,-,-,IREI[IrDA Event Input Enable]]*
*[REG_ITEM:0x080D,TXPLCTRL,-,8|TXPL(7-0)[Transmitter Pulse Length]]*
*[REG_ITEM:0x080E,RXPLCTRL,-,-,7|RXPL(6-0)[Receiver Pulse Length]]*
*[END_REGS]*

## USART0 - (SPI Master Mode)
*[BEGIN_REGS]*
*[REG_ITEM:0x0807,CTRLC,*,2|CMODE[Communication Mode],-,-,-,UDORD[Data Order],UCPHA[Clock Phase],-]*
*[END_REGS]*

## TWI0 - Two Wire Interface / I2C<a name='TWI0'></a>
*[BEGIN_REGS]*
*[REG_ITEM:0x0810,CTRLA,-,-,-,-,SDASETUP[SDA Setup Time],2|SDAHOLD[SDA Hold Time],FMPEN[FM Plus Enable],-]*
*[REG_ITEM:0x0811,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0812,DBGCTRL,-,-,-,-,-,-,-,-,DBGRUN[Debug Run]]*
*[REG_ITEM:0x0813,MCTRLA,-,RIEN[Read Interrupt Enable],WIEN[Write Interrupt Enable],-,QCEN[Quick Command Enable],2|TIMEOUT[Inactive Bus Time-Out],SMEN[Smart Mode Enable],ENABLE[Enable TWI Master]]*
*[REG_ITEM:0x0814,MCTRLB,-,-,-,-,-,FLUSH[Flush],ACKACT[Acknowledge Action],2|MCMD[Command]]*
*[REG_ITEM:0x0815,MSTATUS,-,RIF[Read Interrupt Flag],WIF[Write Interrupt Flag],CLKHOLD[Clock Hold],RXACK[Received Acknowledge],ARBLOST[Arbitration Lost],BUSERR[Bus Error],2|BUSSTATE[Bus State]]*
*[REG_ITEM:0x0816,MBAUD,-,8|BAUD(7-0)[Master Baud Rate]]*
*[REG_ITEM:0x0817,MADDR,-,8|ADDR(7-0)[Master Address]]*
*[REG_ITEM:0x0818,MDATA,-,8|DATA(7-0)[Master Data]]*
*[REG_ITEM:0x0819,SCTRLA,-,DIEN[Data Interrupt Enable],APIEN[Address or Stop Interrupt Enable],PIEN[Stop Interrupt Enable],-,-,PMEN[Address Recognition Mode],SMEN[Smart Mode Enable],ENABLE[Enable TWI Slave]]*
*[REG_ITEM:0x081A,SCTRLB,-,-,-,-,-,-,ACKACT[Acknowledge Action],2|SCMD[Command]]*
*[REG_ITEM:0x081B,SSTATUS,-,DIF[Data Interrupt Flag],APIF[Address or Stop Interrupt Flag],CLKHOLD[Clock Hold],RXACK[Received Acknowledge],COLL[Collision],BUSERR[Bus Error],DIR[Read/Write Direction],AP[Address or Stop]]*
*[REG_ITEM:0x081C,SADDR,-,8|ADDR(7-0)[Slave Address]]*
*[REG_ITEM:0x081D,SDATA,-,8|DATA(7-0)[Slave Data]]*
*[REG_ITEM:0x081E,SADDRMASK,-,7|ADDRMASK(6-0)[Address Mask],ADDREN[Address Mask Enable]]*
*[END_REGS]*

## SPI0 - Serial Peripheral Interface (Normal Mode)<a name='SPI0'></a>
*[BEGIN_REGS]*
*[REG_ITEM:0x0820,CTRLA,-,-,DORD[Data Order],MASTER[Master/Slave Select],CLK2X[Clock Double],-,2|PRESC[Prescaler],ENABLE[SPI Enable]]*
*[REG_ITEM:0x0821,CTRLB,-,BUFEN[Buffer Mode Enable],BUFWR[Buffer Mode Wait for Receive],-,-,-,SSD[Slave Select Disable],2|MODE[Transfer Mode]]*
*[REG_ITEM:0x0822,INTCTRL,-,RXCIE[Receive Complete Interrupt Enable],TXCIE[Transfer Complete Interrupt Enable],DREIE[Data Register Empty Interrupt Enable],SSIE[Slave Select Trigger Interrupt Enable],-,-,-,IE[Interrupt Enable]]*
*[REG_ITEM:0x0823,INTFLAGS,*,IF[Interrupt Flag],WRCOL[Write Collision],-,-,-,-,-,-]*
*[REG_ITEM:0x0824,DATA,-,8|DATA(7-0)[SPI Data]]*
*[END_REGS]*

## SPI0 - Serial Peripheral Interface (Buffer Mode )
*[BEGIN_REGS]*
*[REG_ITEM:0x0823,INTFLAGS,*,RXCIF[Receive Complete Interrupt Flag],TXCIF[Transfer Complete Interrupt Flag],DREIF[Data Register Empty Interrupt Flag],SSIF[Select Trigger Interrupt Flag],-,-,-,BUFOVF[Buffer Overflow]]*
*[END_REGS]*

## TCA0 - 16 Bit Timer Counter Type A (Normal Mode)<a name='TCA0'></a>
*[BEGIN_REGS]*
*[REG_ITEM:0x0A00,CTRLA,-,-,-,-,-,3|CLKSEL[Clock Select],ENABLE[Enable]]*
*[REG_ITEM:0x0A01,CTRLB,-,-,CMP2EN[Compare 2 Enable],CMP1EN[Compare 1 Enable],CMP0EN[Compare 0 Enable],ALUPD[Auto-Lock Update],3|WGMODE[Waveform Generation Mode]]*
*[REG_ITEM:0x0A02,CLRLC,-,-,-,-,-,-,CMP2OV[Compare Output Value 2],CMP1OV[Compare Output Value 1],CMP0OV[Compare Output Value 0]]]*
*[REG_ITEM:0x0A03,CTRLD,-,-,-,-,-,-,-,-,SPLITM[Enable Split Mode]]*
*[REG_ITEM:0x0A04,CTRLCCLR,-,-,-,-,-,2|CMD[Command],LUPD[Lock Update],DIR[Counter Direction]]*
*[REG_ITEM:0x0A05,CTRLESET,-,-,-,-,-,2|CMD[Command],LUPD[Lock Update],DIR[Counter Direction]]*
*[REG_ITEM:0x0A06,CTRLFCLR,-,-,-,-,-,CMP2BV[Compare 2 Buffer Valid],CMP1BV[Compare 1 Buffer Valid],CMP0BV[Compare 0 Buffer Valid],PERBV[Period Buffer Valid]]*
*[REG_ITEM:0x0A07,CTRLFSET,-,-,-,-,-,CMP2BV[Compare 2 Buffer Valid],CMP1BV[Compare 1 Buffer Valid],CMP0B[Compare 0 Buffer Valid],PERBV[Period Buffer Valid]]*
*[REG_ITEM:0x0A08,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0A09,EVCTRL,-,-,-,-,-,3|EVACT[Event Action],CNTEI[Enable Count on Event Input]]*
*[REG_ITEM:0x0A0A,INTCTRL,-,-,CMP2[Compare Channel 2 Interrupt Enable],CMP1[Compare Channel 1 Interrupt Enable],CMP0[Compare Channel 0 Interrupt Enable],-,-,-,OVF[Timer Overflow/Underflow Interrupt Enable]]*
*[REG_ITEM:0x0A0B,INTFLAGS,-,-,CMP2[Compare Channel 2 Interrupt Flag],CMP1[Compare Channel 1 Interrupt Flag],CMP0[Compare Channel 0 Interrupt Flag],-,-,-,OVF[Overflow/Underflow Interrupt Flag]]]*
*[REG_ITEM:0x0A0C-1D,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0A0E,DBGCTRL,-,-,-,-,-,-,-,-,DBGRUN[Run in Debug]]*
*[REG_ITEM:0x0A0G,TEMP,-,8|TEMP(7-0)[Temporary Bits for 16-Bit Access]]*
*[REG_ITEM:0x10-1F,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0A20,CNT,-,8|CNT(7-0)[Counter Low Byte]]*
*[REG_ITEM:0x0A21,CNT,-,8|CNT(15-8)[Counter High Byte]]*
*[REG_ITEM:0x0A22-25,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0A26,PER,-,8|PER(7-0)[Periodic Low Byte]]*
*[REG_ITEM:0x0A27,PER,-,8|PER(15-8)[Periodic High Byte]]*
*[REG_ITEM:0x0A28,CMP0,-,8|CMP0(7-0)[Compare 0 Low Byte]]*
*[REG_ITEM:0x0A29,CMP0,-,8|CMP0(15-8)[Compare 0 High Byte]]*
*[REG_ITEM:0x0A2A,CMP1,-,8|CMP1(7-0)[Compare 1 Low Byte]]*
*[REG_ITEM:0x0A2B,CMP1,-,8|CMP1(15-8)[Compare 1 High Byte]]*
*[REG_ITEM:0x0A2C,CMP2,-,8|CMP2(7-0)[Compare 2 Low Byte]]*
*[REG_ITEM:0x0A2D,CMP2,-,8|CMP2(15-8)[Compare 2 High Byte]]*
*[REG_ITEM:0x0A2E-35,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0A36,PERBUF,-,8|PERBUF(7-0)[Period Buffer Low Byte]]*
*[REG_ITEM:0x0A37,PERBUF,-,8|PERBUF(15-8)[Period Buffer High Byte]]*
*[REG_ITEM:0x0A38,CMP0BUF,-,8|CMP0BUF(7-0)[Compare 0 Low Byte]]*
*[REG_ITEM:0x0A39,CMP0BUF,-,8|CMP0BUF(15-8)[Compare 0 High Byte]]*
*[REG_ITEM:0x0A3A,CMP1BUF,-,8|CMP1BUF(7-0)[Compare 1 Low Byte]]*
*[REG_ITEM:0x0A3B,CMP1BUF,-,8|CMP1BUF(15-8)[Compare 1 High Byte]]*
*[REG_ITEM:0x0A3C,CMP2BUF,-,8|CMP2BUF(7-0)[Compare 2 Low Byte]]*
*[REG_ITEM:0x0A3D,CMP2BUF,-,8|CMP2BUF(15-8)[Compare 2 High Byte]]*
*[END_REGS]*

## TCB0 - 16 Bit Timer Counter Type B<a name='TCB0'></a>
*[BEGIN_REGS]*
*[REG_ITEM:0x0A40,CTRLA,-,-,RUNSTDBY[Run in Standby],-,SYNCUPD[Synchronize Update],-,2|CLKSEL[Clock Select],ENABLE[Enable]]*
*[REG_ITEM:0x0A41,CTRLB,-,-,ASYNC[Asynchronous Enable],CCMPINIT[Compare/Capture Pin Initial Value],CCMPEN[Compare/Capture Output Enable],-,3|CNTMODE[Timer Mode]]*
*[REG_ITEM:0x0A42-43,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0A44,EVCTRL,-,-,FILTER[Input Capture Noise Cancellation Filter],-,EDGE[Event Edge],-,-,-,CAPTEI[Capture Event Input Enable]]*
*[REG_ITEM:0x0A45,INTCTRTL,-,-,-,-,-,-,-,-,CAPT[Capture Interrupt Enable]]*
*[REG_ITEM:0x0A46,INTFLAGS,-,-,-,-,-,-,-,-,CAPT[Capture Interrupt Flag]]*
*[REG_ITEM:0x0A47,STATUS,-,-,-,-,-,-,-,-,RUN[Run]]*
*[REG_ITEM:0x0A48,DBGCTRL,-,-,-,-,-,-,-,-,DBGRUN[Debug Run]]*
*[REG_ITEM:0x0A49,TEMP,-,8|TEMP(7-0)[Temporary Value]]*
*[REG_ITEM:0x0A4A,CNT,-,8|CNT(7-0)[Count Value Low Byte]]*
*[REG_ITEM:0x0A4B,CNT,-,8|CNT(15-8)[Count Value High Byte]]*
*[REG_ITEM:0x0A4C,CCMP,-,8|CCMP(7-0)[Capture/Compare Value Low Byte]]*
*[REG_ITEM:0x0A4D,CCMP,-,8|CCMP(15-8)[Capture/Compare Value High Byte]]*
*[END_REGS]*

 ## TCB1- 16 Bit Timer Counter Type B<a name='TCB1'></a>
*[BEGIN_REGS]*
*[REG_ITEM:0x0A50,CTRLA,-,-,RUNSTDBY[Run in Standby],-,SYNCUPD[Synchronize Update],-,2|CLKSEL[Clock Select],ENABLE[Enable]]*
*[REG_ITEM:0x0A51,CTRLB,-,-,ASYNC[Asynchronous Enable],CCMPINIT[Compare/Capture Pin Initial Value],CCMPEN[Compare/Capture Output Enable],-,3|CNTMODE[Timer Mode]]*
*[REG_ITEM:0x0A52-53,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0A54,EVCTRL,-,-,FILTER[Input Capture Noise Cancellation Filter],-,EDGE[Event Edge],-,-,-,CAPTEI[Capture Event Input Enable]]*
*[REG_ITEM:0x0A55,INTCTRTL,-,-,-,-,-,-,-,-,CAPT[Capture Interrupt Enable]]*
*[REG_ITEM:0x0A56,INTFLAGS,-,-,-,-,-,-,-,-,CAPT[Capture Interrupt Flag]]*
*[REG_ITEM:0x0A57,STATUS,-,-,-,-,-,-,-,-,RUN[Run]]*
*[REG_ITEM:0x0A58,DBGCTRL,-,-,-,-,-,-,-,-,DBGRUN[Debug Run]]*
*[REG_ITEM:0x0A59,TEMP,-,8|TEMP(7-0)[Temporary Value]]*
*[REG_ITEM:0x0A5A,CNT,-,8|CNT(7-0)[Count Value Low Byte]]*
*[REG_ITEM:0x0A5B,CNT,-,8|CNT(15-8)[Count Value High Byte]]*
*[REG_ITEM:0x0A5C,CCMP,-,8|CCMP(7-0)[Capture/Compare Value Low Byte]]*
*[REG_ITEM:0x0A5D,CCMP,-,8|CCMP(15-8)[Capture/Compare Value High Byte]]*
*[END_REGS]*

## TCD0 - 12 Bit Timer Counter Type D<a name='TCD0'></a>
*[BEGIN_REGS]*
*[REG_ITEM:0x0A80,CTRLA,-,-,2|CLKSEL[Clock Select],2|CNTPRES[Counter Prescaler],2|SYNCPRES[Synchronization Prescaler],ENABLE[Enable]]*
*[REG_ITEM:0x0A81,CTRLB,-,-,-,-,-,-,-,2|WGMODE[Waveform Generation Mode]]*
*[REG_ITEM:0x0A82,CTRLC,-,CMPDSEL[Compare D Output Select],CMPCSEL[Compare C Output Select],-,-,FIFTY[Fifty Percent Waveform],-,AUPDATE[Automatically Update],CMPOVR[Compare Output Value Override]]*
*[REG_ITEM:0x0A83,CTRLD,-,4|CMPBVAL[Compare B Value (in Active state)],4|CMPAVAL[Compare A Value (in Active state)]]*
*[REG_ITEM:0x0A84,CTRLE,-,DISEOC[Disable at End of TCD Cycle Strobe],-,-,SCAPTUREB[Software Capture B Strobe],SCAPTUREA[Software Capture A Strobe],RESTART[Restart Strobe],SYNC[Synchronize Strobe],SYNCEOC[Synchronize End of TCD Cycle Strobe]]*
*[REG_ITEM:0x0A85-87,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0A88,EVCTRLA,-,2|CFG[Event Configuration],-,EDGE[Edge Selection],-,ACTION[Event Action],-,TRIGEI[Trigger Event Input Enable]]*
*[REG_ITEM:0x0A89,EVCTRLB,-,2|CFG[Event Configuration],-,EDGE[Edge Selection],-,ACTION[Event Action],-,TRIGEI[Trigger Event Input Enable]]*
*[REG_ITEM:0x0A8A-8B,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0A8C,INTCTRL,-,-,-,-,-,TRIGB[Trigger B Interrupt Enable],TRIGA[Trigger A Interrupt Enable],-,OVF[Counter Overflow]]*
*[REG_ITEM:0x0A8D,INTFLAGS,-,-,-,-,-,TRIGB[Trigger B Interrupt Flag],TRIGA[Trigger A Interrupt Flag],-,OVF[Overflow Interrupt Flag]]*
*[REG_ITEM:0x0A8E,STATUS,-,PWMACTB[PWM Activity on B],PWMACTA[PWM Activity on A],-,-,-,-,CMDRDY[Command Ready],ENRDY[Enable Ready]]*
*[REG_ITEM:0x0A8F,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0A90,INPUTCTRLA,-,-,-,-,-,4|INPUTMODE[Input Mode]]*
*[REG_ITEM:0x0A91,INPUTCTRLB,-,-,-,-,-,4|INPUTMODE[Input Mode]]*
*[REG_ITEM:0x0A92,FAULTCTRL,-,CMPDEN[Compare D Enable],CMPCEN[Compare C Enable],CMPBEN[Compare B Enable],CMPAEN[Compare A Enable],CMPD[Compare D Value],CMPC[Compare C Value],CMPB[Compare B Value],CMPA[Compare A Value]]*
*[REG_ITEM:0x0A93,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0A94,DLYCTRL,-,-,-,2|DLYPRESC[Delay Prescaler],2|DLYTRIG[Delay Trigger],2|DLYSEL[Delay Select]]*
*[REG_ITEM:0x0A95,DLYVAL,-,8|DLYVAL(7-0)[Delay Value]]*
*[REG_ITEM:0x0A96-97,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0A98,DITCTRL,-,-,-,-,-,-,-,2|DITHERSEL[Dither Select]]*
*[REG_ITEM:0x0A99,DITVAL,-,-,-,-,-,4|DITHER[Dither Value]]*
*[REG_ITEM:0x0A9A-9D,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0A9E,DBGCTRL,-,-,-,-,-,-,FAULTDET[Fault Detection],-,DBGRUN[Debug Run]]*
*[REG_ITEM:0x0A9F-A1,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0AA2,CAPTUREA,-,8|CAPTUREA(7-0)[Capture A LSB]]*
*[REG_ITEM:0x0AA3,CAPTUREA,-,-,-,-,-,4|CAPTUREA(11-8)[Capture A Upper Bits]]*
*[REG_ITEM:0x0AA4,CAPTUREB,-,8|CAPTUREB(7-0)[Capture B LSB]]*
*[REG_ITEM:0x0AA5,CAPTUREB,-,-,-,-,-,4|CAPTUREB(11-8)[Capture B Upper Bits]]*
*[REG_ITEM:0x0AA6-A7,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x0AA8,CMPASET,-,8|CMPASET(7-0)[Compare A Set LSB]]*
*[REG_ITEM:0x0AA9,CMPASET,-,-,-,-,-,4|CMPASET(11-8)[Compare A Set Upper Bits]]*
*[REG_ITEM:0x0AAA,CMPACLR,-,8|CMPACLR(7-0)[Compare A Clear LSB]]*
*[REG_ITEM:0x0AAB,CMPACLR,-,-,-,-,-,4|CMPACLR(11-8)[Compare B Clear Lower bits]]*
*[REG_ITEM:0x0AAC,CMPBSET,-,8|CMPBSET(7-0)[Compare B Set LSB]]*
*[REG_ITEM:0x0AAD,CMPBSET,-,-,-,-,-,4|CMPBSET(11-8)[Compare B Set Upper Bits]]*
*[REG_ITEM:0x0AAE,CMPBCLR,-,8|CMPBCLR(7-0)[Compare B Clear LSB]]*
*[REG_ITEM:0x0AAF,CMPBCLR,-,-,-,-,-,4|CMPBCLR(11-8)[Compare B Clear Upper Bits]]*
*[END_REGS]*

## SYSCFG - System Configuration
*[BEGIN_REGS]*
*[REG_ITEM:0x0F00,REVID,-,8|REVID(7-0)[Revision ID]]*
*[END_REGS]*

## NVMCTRL - Nonvolatile Memory Controller
*[BEGIN_REGS]*
*[REG_ITEM:0x1000,CTRLA,-,-,-,-,-,-,3|CMD[Command]]*
*[REG_ITEM:0x1001,CTRLB,-,-,-,-,-,-,-,BOOTLOCK[Boot Section Lock],APCWP[Application Code Section Write Protection]]*
*[REG_ITEM:0x1002,STATUS,-,-,-,-,-,-,WRERROR[Write Error],EEBUSY[EEPROM Busy],FBUSY[Flash Busy]]*
*[REG_ITEM:0x1003,INTCTRL,-,-,-,-,-,-,-,-,EEREADY[EEPROM Ready Interrupt Enable]]*
*[REG_ITEM:0x1004,INTFLAGS,-,-,-,-,-,-,-,-,EEREADY[EEPROM Ready Interrupt Flag]]*
*[REG_ITEM:0x1005,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x1006,DATA,-,8|DATA(7-0)[Data Register LSB]]*
*[REG_ITEM:0x1007,DATA,-,8|DATA(15-8)[Data Register MSB]]*
*[REG_ITEM:0x1008,ADDR,-,8|ADDR(7-0)[Address Register LSB]]*
*[REG_ITEM:0x1009,ADDR,-,8|ADDR(15-8)[Address Register MSB]]*
*[END_REGS]*

## SIGROW - Signature Row (Read Only)
*[BEGIN_REGS]*
*[REG_ITEM:0x1100,DEVICEID0,-,8|DEVICEID(7-0)[Device ID 0]]*
*[REG_ITEM:0x1101,DEVICEID1,-,8|DEVICEID(7-0)[Device ID 1]]*
*[REG_ITEM:0x1102,DEVICEID2,-,8|DEVICEID(7-0)[Device ID 2]]*
*[REG_ITEM:0x1103,SERNUM0,-,8|SERNUM(7-0)[Serial Number 0]]*
*[REG_ITEM:0x1104,SERNUM1,-,8|SERNUM(7-0)[Serial Number 1]]*
*[REG_ITEM:0x1105,SERNUM2,-,8|SERNUM(7-0)[Serial Number 2]]*
*[REG_ITEM:0x1106,SERNUM3,-,8|SERNUM(7-0)[Serial Number 3]]*
*[REG_ITEM:0x1107,SERNUM4,-,8|SERNUM(7-0)[Serial Number 4]]*
*[REG_ITEM:0x1108,SERNUM5,-,8|SERNUM(7-0)[Serial Number 5]]*
*[REG_ITEM:0x1109,SERNUM6,-,8|SERNUM(7-0)[Serial Number 6]]*
*[REG_ITEM:0x110A,SERNUM7,-,8|SERNUM(7-0)[Serial Number 7]]*
*[REG_ITEM:0x110B,SERNUM8,-,8|SERNUM(7-0)[Serial Number 8]]*
*[REG_ITEM:0x110V,SERNUM9,-,8|SERNUM(7-0)[Serial Number 9]]*
*[REG_ITEM:0x110D-1F,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x1120,TEMPSENSE0,-,8|TEMPSENSE(7-0)[Temperature Sensor Calibration Byte 0]]*
*[REG_ITEM:0x1121,TEMPSENSE1,-,8|TEMPSENSE(7-0)[Temperature Sensor Calibration Byte 1]]*
*[REG_ITEM:0x1122,OSC16ERR3V,-,8|OSC16ERR3V(7-0)[OSC16 Error at 3V]]*
*[REG_ITEM:0x1123,OSC16ERR5V,-,8|OSC16ERR5V(7-0)[OSC16 Error at 5V]]*
*[REG_ITEM:0x1124,OSC20ERR3V,-,8|OSC20ERR3V(7-0)[OSC20 Error at 3V]]*
*[REG_ITEM:0x1125,OSC20ERR5V,-,8|OSC20ERR5V(7-0)[OSC20 Error at 5V]]*
*[END_REGS]*

## FUSES (Program via UPDI, Otherwise Read Only)
*[BEGIN_REGS]*
*[REG_ITEM:0x1280,WDTCFG,-,4|WINDOW[Watchdog Window Time-Out Period],4|PERIOD[Watchdog Time-Out Period]]*
*[REG_ITEM:0x1281,BODCFG,-,3|LVL[BOD Level],SAMPFREQ[BOD Sample Frequency],2|ACTIVE[BOD Operation Mode in Active and Idle],2|SLEEP[BOD Operation Mode in Sleep]]*
*[REG_ITEM:0x1282,OSCCFG,-,OSCLOCK[Oscillator Lock],-,-,-,-,-,2|FREQSEL[Frequency Select]]*
*[REG_ITEM:0x1283,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x1284,TCD0CFG,-,CMPDEN[Compare D Enable],CMPCEN[Compare C Enable],CMPBEN[Compare B Enable],CMPAEN[Compare A Enable],CMPD[Compare D],CMPC[Compare C],CMPB[Compare B],CMPA[Compare A]]*
*[REG_ITEM:0x1285,SYSCFG0,-,2|CRCSRC[CRC Source],-,TOUTDIS[Time-Out Disable],2|RSTPINCFG[Reset Pin Configuration],-,EESAVE[EEPROM Save During Chip Erase]]*
*[REG_ITEM:0x1286,SYSCFG1,-,-,-,-,-,-,3|SUT[Start-Up Time Setting]]*
*[REG_ITEM:0x1287,APPEND,-,8|APPEND(7-0)[Application Code Section End]]*
*[REG_ITEM:0x1288,BOOTEND,-,8|BOOTEND(7-0)[Boot Section End]]*
*[REG_ITEM:0x1289,reserved,-,-,-,-,-,-,-,-,-]*
*[REG_ITEM:0x128A,LOCKBIT,-,8|LOCKBIT[Lockbits]]*
*[END_REGS]*

## USERROW - (Extra 64 Bytes of EEPROM)
*[BEGIN_REGS]*
*[REG_ITEM:0x1300,USERROW0,-,8|DATA(7-0)[USERROW (Byte 0)]]*
*[REG_ITEM:...,...,-,8|...[USERROW (Bytes 1-62)]]*
*[REG_ITEM:0x133F,USERROW63,-,8|DATA(7-0)[USERROW (Byte 63)]]*
*[END_REGS]*


## EEPROM (64 - 256 Bytes, depending on device)
*[BEGIN_REGS]*
*[REG_ITEM:0x1400,EEPROM0,-,8|DATA(7-0)[EEPROM (Byte 0)]]*
*[REG_ITEM:...,...,-,8|...[EEPROM (Bytes 1-254)]]*
*[REG_ITEM:0x14FF,EEPROM255,-,8|DATA(7-0)[EEPROM (Byte 255)]]*
*[END_REGS]*
