# RISC-CPU-Simulator

This program is designed to simulate a MIPS CPU in a realistic fashion. 
This CPU takes assembeler instructions and initial register/memory values, decodes them, excecutes them, modifies memory and registers as necessary, then prints out the final memory/register values and the pipeline stages. 

Features of this simulator: 
-Data/ Control hazard dection and compensation. 
-Pipelining. This simulator executes multiple instructions similtaniously along 5 main stages in the CPU (Instruction Fetch, decode, execution, write back, memory access)
-Branch prediction.

Supported MIPS instructions: 
-load word (e.g., LW R1, 0(R2))
-store word (e.g., SW R1, 8(R2))
-add (e.g., ADD R1, R2, R3)
-add immediate (e.g., ADDI R1, R2, 6)
-subtract (e.g., SUB R1, R2, R3)
-set less than (e.g., SLT R1,R2, R3)
-branch if equal (e.g., BEQ R1, R2, Loop)
-branch not equal (e.g., BNE R1, R2, Loop). 

Example Input File: 
REGISTERS
R1 16
R3 42
R5 8
MEMORY
8 40
16 60
CODE
10001100001000100000000000000000
00000000010000110010000000100000
10101100001001000000000000000000
00010100000001000000000000000001
00100000010000011111111111110111
00000000001000110000100000100000

Example Output File: 
c#1 I1-IF
c#2 I1-ID I2-IF
c#3 I1-EX I2-stall 
c#4 I1-MEM I2-stall
c#5 I1-WB I2-ID I3-IF
c#6 I2-EX I3-stall 
c#7 I2-MEM I3-stall
c#8 I2-WB I3-ID I4-IF
c#9 I3-EX I4-ID I5-IF
c#10 I3-MEM I4-EX  
c#11 I3-WB I4-MEM I6-IF 
c#12 I4-WB I6-ID 
c#13 I6-EX 
c#14 I6-MEM 
c#15 I6-WB
REGISTERS
R1 58
R2 60
R3 42
R4 102
R5 8
MEMORY
8 40
16 102


The variable in the code called “DEBUG” can be set to true to enable a verbose execution mode, this can give more insight to how the simulator functions
