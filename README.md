# RISC-CPU-Simulator

This program is designed to simulate a MIPS CPU in a realistic fashion. 

This CPU takes assembler instructions and initial register/memory values, decodes them, executes them, modifies memory and registers as necessary, then prints out the final memory/register values and the pipeline stages. 


Features of this simulator: 

-Data/ Control hazard detection and compensation. 

-Pipelining. This simulator executes multiple instructions simultaneously along 5 main stages in the CPU (Instruction Fetch, decode, execution, write back, memory access)

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


Input file explanation: 

Registers are written first. R1,R3,R5 are register labels with their values to the right of them
Memory is written next. 8, 16 are addresses with their values to the right of them
Assembler code is written next. "input-inst decoded.txt" has these instructions converted to assemble


Output file explanation: 

c#1....c#15 are each clock cycle. Each clock cycle shows what’s currently in the pipeline. It shows the instruction number ("I") and the what stage of the pipeline that instruction is in ("IF": Instruction Fetch, "ID": Instruction decode, "EX": Execution, "MEM": Memory access, "WB": Write Back, "stall": Stall for hazard compensation)


The variable in the code called “DEBUG” can be set to true to enable a verbose execution mode, this can give more insight to how the simulator functions
