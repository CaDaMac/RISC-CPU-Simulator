import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Scanner;

public class main {
	public static final boolean DEBUG = false; //Controls verbose terminal output to help with debugging

	//Simulation Variables
	public static int[] registers = new int[32]; //CPU registers
	public static boolean[] regPerm = new boolean[32]; //Register permissions. Locks a register to prevent data hazards. True == locked
	public static int[] memory = new int[249]; //All memory. (Multiply by 4 to get correct address)
	public static ArrayList<String> inst = new ArrayList<String>(); //Dynamic array for storing an unknown number of instructions
	public static int PCInst = 0; //The current instruction the simulator is executing
	public static String[] pipeStage = new String[5]; //Which instruction is at which stage. 0:IF, 1:ID, 2:EXE, 3:MEM, 4:WB
	public static int cycleCount = 1; //Count how many cycles have gone by
	public static int[] instCount = new int[5]; //Keeps track of what number the instruction in each stage is
	public static boolean branchFlush = false; //If we are currently branching, don't IF until the PC is updated

	//File output Variables
	public static PrintWriter outputFile; 
	public static boolean runAgain = true; //Continue running simulations until user types "n" when questioned

	@SuppressWarnings({ "unused", "resource" }) //Ignore this. Its an Eclipse IDE command
	public static void main(String[] args) {
		while(runAgain == true){

			//Initialize the arrays/variables and clear them out
			for(int x = 0; x < registers.length; x++){
				registers[x] = 0;
			}
			for(int x = 0; x < regPerm.length; x++){
				regPerm[x] = false;
			}
			for(int x = 0; x < memory.length; x++){
				memory[x] = 0;
			}
			inst.clear();
			PCInst = 0;
			for(int x = 0; x < pipeStage.length; x++){
				pipeStage[x] = null;
			}
			cycleCount = 1;
			for(int x = 0; x < instCount.length; x++){
				instCount[x] = 0;
			}
			branchFlush = false;

			//Get the name of the file and load it
			String fileName = "";
			String outputFileName = "";
			File inputFile = null;
			outputFile = null;
			Scanner sysin = new Scanner(System.in);

			while(inputFile == null || outputFile == null){ //Keep asking for a file till a valid input file is found

				//Ask the user for the name of the files
				System.out.print("Enter input and output file name (ex: \"input.txt output.txt\"): "); 


				if(DEBUG == false){ //Assume input file name is "input.txt" if in debug mode
					fileName = sysin.next();
					outputFileName = sysin.next();
				}else{
					fileName = "input.txt";
					outputFileName = "test.txt";
				}

				//Attempting to access the named file
				inputFile = new File(fileName);
				try {
					Scanner sc = new Scanner(inputFile);
					inputLoader(sc);
				} 
				catch (FileNotFoundException e) { //If the file isn't found
					inputFile = null;
					System.err.println("Input File Not Found! Try again...");

					//These are needed to sync up the 2 different output streams
					System.out.flush(); 
					System.err.flush();

				}

				//At this point all of the registers, memory, and instructions should be loaded


				//Create the output file
				try {

					outputFile = new PrintWriter(outputFileName, "UTF-8");


					if(DEBUG)
						System.err.println("Output file successfully created/ opened");
				} 
				catch (FileNotFoundException e) {
					outputFile = null;
					System.err.println("Output File Error! Try again...");

					//These are needed to sync up the 2 different output streams
					System.out.flush(); 
					System.err.flush();

					e.printStackTrace();
				} 
				catch (UnsupportedEncodingException e) {
					outputFile = null;
					System.err.println("Output File Error! Try again...");

					//These are needed to sync up the 2 different output streams
					System.out.flush(); 
					System.err.flush();

					e.printStackTrace();
				}
			}

			//Begin executing the instructions

			while(PCInst < inst.size() + 4){ 
				boolean didStall = false;

				outputFile.print("c#" + cycleCount + " ");

				//Execute WB
				if(runStage(4)) //If it stalled set didStall to true. WB won't stall but this is here for consistency
					didStall = true;

				//Execute MEM
				if(runStage(3))
					didStall = true;

				//Execute EXE
				if(runStage(2))
					didStall = true;

				//Execute ID
				if(runStage(1))
					didStall = true;

				//Execute IF
				if(didStall == false)
					runStage(0); //Only fetch a new instruction if the pipeline doesn't have a stall


				if(didStall == false){ //If there are no stalls, its safe to advance the PC counter
					PCInst++;
				}

				outputFile.println(); //Create a new line in the output file to separate the cycles
				cycleCount++; //Increase the cycle count

				if(DEBUG){ //Print out whats currently in the pipeline
					System.err.print("Current Pipeline: ");
					for(String x: pipeStage){
						if(x != null)
							System.err.print(opConverter(instBreaker(x, 'r')[0], instBreaker(x, 'r')[5])[0] + " ");
						else
							System.err.print("null ");
					}
					System.err.println();
				}
			}

			//Print the registers to the output file
			outputFile.println("REGISTERS");
			for(int x = 0; x < registers.length; x++){
				if(registers[x] != 0)
					outputFile.print("R" + x + " " + registers[x] + "\n");
			}

			//Print the memory to the output file
			outputFile.println("MEMORY");
			for(int x = 0; x < memory.length; x++){
				if(memory[x] != 0)
					outputFile.print(x * 4 + " " + memory[x] + "\n");
			}

			outputFile.close(); //Close access to the file before the simulator ends
			System.out.print("Output file has been saved");
			
			String answer = " ";
			if(DEBUG){
				answer = "n";
				runAgain = false;
			}
			while(!answer.equals("n") && !answer.equals("y")){ //Continue asking until user gives n or y as an answer
				System.out.print("\nWould you like to run another simulation? (y/n): ");
				answer = sysin.next();
				if(answer.equals("n")){
					runAgain = false;
					System.out.println("\nExiting simulator");
				}
			}
		}
	} //End main()

	//This function loads up the registers, memory, and instruction array from the input file
	public static void inputLoader(Scanner sc){ 
		//if(DEBUG)
		//System.err.println("Running inputLoader");

		//Begin loading registers
		String next = sc.next();
		while(!next.equals("REGISTERS")){
			System.err.println(next);
			//Keep scanning until "REGISTERS" is found
			next = sc.next();
		}
		next = sc.next(); //This should load the first register pointer to next or load the "MEMORY" section

		while(!next.equals("MEMORY")){ //Keep running till "MEMORY" is found
			int regNum = Integer.parseInt(next.substring(1));
			next = sc.next(); //Should load the value of the register
			int regVal = Integer.parseInt(next);

			//if(DEBUG)
			//System.err.println("Found register R" + regNum + ", value: " + regVal);

			registers[regNum] = regVal;

			next = sc.next();
		}

		//Begin loading memory
		next = sc.next(); //Should load the first memory location or "CODE"

		while(!next.equals("CODE")){ //Keep running till "CODE" is found
			int memNum = Integer.parseInt(next);
			next = sc.next();
			int memVal = Integer.parseInt(next);

			//if(DEBUG)
			//System.err.println("Found memory " + memNum + ", value: " + memVal);

			memory[memNum / 4] = memVal; //Divide by 4 since the memory array only holds valid memory locations (every 4)
			next = sc.next();
		}

		//Begin loading code
		while(sc.hasNext()){
			next = sc.next();

			inst.add(next);


		}
	}

	//Handles advancing the pipeline/ stalling
	public static boolean runStage(int stage){ //Stages: 0-4 IF, ID, EXE, MEM, WB. Returns true if there is a stall

		//Move the inst[stage-1] forward. Execute the inst. Null the inst[stage-1] if no stall. Null inst[stage] if there is a stall



		if(stage == 0){ //IF stage
			if(PCInst < inst.size() && branchFlush == false){ //Only fetch more instruction if the PC is pointing to a valid address in this program. Fill with null otherwise
				pipeStage[0] = inst.get(PCInst);
				instCount[0] = instCount[0] + 1; //Give this new instruction an instruction number
				outputFile.print("I" + instCount[0]+ "-IF ");

			}else{
				pipeStage[0] = null;
			}
			return false;
		}

		if(pipeStage[stage - 1] == null){ //If there's no instruction in the previous stage, then there's nothing to advance to the next stage
			if(stage == 4){
				pipeStage[stage] = null; //If there's nothing to execute for the WB stage, clear out the WB stage
			}
			return false;
		}

		if(stage == 1){ //ID stage
			pipeStage[1] = pipeStage[0]; //Move the instruction forward
			instCount[1] = instCount[0];

			if(runInst(pipeStage[1], 1)){ //Decode the instruction. If it stalls, null out the ID stage
				outputFile.print("I" + instCount[1] + "-stall "); //Print out the stall to the output file
				pipeStage[1] = null;
				instCount[1] = 0;

				return true;
			}else{ //If there is no stall
				pipeStage[0] = null;

				outputFile.print("I" + instCount[1] + "-ID "); //Print out the instruction to the output file

				return false;
			}
		}

		if(stage == 2){ //EXE stage
			pipeStage[stage] = pipeStage[stage - 1];
			instCount[stage] = instCount[stage - 1];

			if(runInst(pipeStage[stage], stage)){ //These next stages shouldn't ever stall
				outputFile.print("I" + instCount[stage] + "-stall "); //Print out the stall to the output file
				instCount[stage] = 0;
				pipeStage[stage] = null;
				return true;
			}else{
				pipeStage[stage - 1] = null;
				
				outputFile.print("I" + instCount[stage] + "-EX "); //Print out the instruction to the output file
			}

		}

		if(stage == 3){ //MEM stage
			pipeStage[stage] = pipeStage[stage - 1];
			instCount[stage] = instCount[stage - 1];

			if(runInst(pipeStage[stage], stage)){ //These next stages shouldn't ever stall
				outputFile.print("I" + instCount[stage] + "-stall "); //Print out the stall to the output file
				instCount[stage] = 0;
				pipeStage[stage] = null;
				return true;
			}else{
				pipeStage[stage - 1] = null;
				
				outputFile.print("I" + instCount[stage] + "-MEM "); //Print out the instruction to the output file
			}
		}

		if(stage == 4){ //WB stage
			pipeStage[stage] = pipeStage[stage - 1];
			instCount[stage] = instCount[stage - 1];

			if(runInst(pipeStage[stage], stage)){ //These next stages shouldn't ever stall
				outputFile.print("I" + instCount[stage] + "-stall "); //Print out the stall to the output file
				instCount[stage] = 0;
				pipeStage[stage] = null;
				return true;
			}else{
				pipeStage[stage - 1] = null;
				
				outputFile.print("I" + instCount[stage] + "-WB "); //Print out the instruction to the output file
			}
			registers[0] = 0; //Make sure register 0 never changes
		}


		return false; //If an instruction needed to stall, return true. 
	}

	//Executes specific instructions
	public static boolean runInst(String instruction, int stage){ //Run the specific instruction at stage level "stage"
		String opcode = instBreaker(instruction, 'r')[0]; //Get just the opcode 
		String funct = instBreaker(instruction, 'r')[5]; //Get just the funct. If its not an r type, then the instBreaker will just ignore the funct anyway
		String instName = opConverter(opcode, funct)[0]; //Gets the name of the instruction

		if(instName.equals("LW")){
			return LW(instruction, stage); //Returns true if stalls
		}
		if(instName.equals("SW")){
			return SW(instruction, stage); //Returns true if stalls
		}
		if(instName.equals("ADD")){
			return ADD(instruction, stage); //Returns true if stalls
		}
		if(instName.equals("ADDI")){
			return ADDI(instruction, stage); //Returns true if stalls
		}
		if(instName.equals("SUB")){
			return SUB(instruction, stage); //Returns true if stalls
		}
		if(instName.equals("SLT")){
			return SLT(instruction, stage); //Returns true if stalls
		}
		if(instName.equals("BEQ")){
			return BEQ(instruction, stage); //Returns true if stalls
		}
		if(instName.equals("BNE")){
			return BNE(instruction, stage); //Returns true if stalls
		}

		System.err.println("runInst(): INSTRUCTION NOT RECOGNIZED");
		return false;
	}



	public static String[] instBreaker(String instruction, char type){ //This breaks the instruction up into its OP code, shamt, etc based of type
		String[] splitInst = null;

		//Get the OP code first
		String opcode = instruction.substring(0,6);

		if(type == 'r'){
			splitInst = new String[6]; //Create space for opcode, rs, rt ,rd, shamt, funct

			splitInst[0] = opcode;
			splitInst[1] = instruction.substring(6,11); //rs
			splitInst[2] = instruction.substring(11,16); //rt
			splitInst[3] = instruction.substring(16,21); //rd
			splitInst[4] = instruction.substring(21,26); //shamt
			splitInst[5] = instruction.substring(26,32); //funct
		}

		if(type == 'i'){
			splitInst = new String[4]; //Create space for opcode, rs, rt, immediate

			splitInst[0] = opcode;
			splitInst[1] = instruction.substring(6,11); //rs
			splitInst[2] = instruction.substring(11,16); //rt
			splitInst[3] = instruction.substring(16, 32); //immediate
		}

		if(type == 'j'){
			splitInst = new String[2]; //Create space for opcode, address

			splitInst[0] = opcode;
			splitInst[1] = instruction.substring(6,32); //address
		}

		if(splitInst == null){ //If a wrong type is put in, splitInst won't initialize so we know it was a wrong type. 
			System.err.println("instBreaker(): INVALID INSTRUCTION TYPE");
			System.exit(-1); //Terminate the program
			return null;
		}else{
			return splitInst;
		}
	}

	//Converts the numerical opcode to an instruction name and its type ex: (LW, i)
	public static String[] opConverter(String opcode, String funct){ //Converts the numerical opcode to an instruction name and its type ex: (LW, i)
		String[] instruction = new String[2]; //Instruction name, type
		int numOP = -1; //The decimal representation of the binary opcode

		try{
			numOP = Integer.parseInt(opcode, 2); //Convert the binary to decimal
		}
		catch(NumberFormatException e){
			System.err.println("opConverter(): CAN'T CONVERT OPCODE BINARY TO DECIMAL");
		}

		if(numOP > 0){ //If the opcode isn't 000000
			switch (numOP){ //Convert the decimal opcode to an instruction name
			case 35: 
				instruction[0] = "LW";
				instruction[1] = "i";
				break;

			case 43:
				instruction[0] = "SW";
				instruction[1] = "i";
				break;

			case 8:
				instruction[0] = "ADDI";
				instruction[1] = "i";
				break;

			case 4:
				instruction[0] = "BEQ";
				instruction[1] = "i";
				break;

			case 5:
				instruction[0] = "BNE";
				instruction[1] = "i";
				break;

			default:
				instruction[0] = "ERR";
				instruction[1] = "err";
				break;
			}
		}else{ //If the opcode is 000000 then use the funct to determine instruction name
			int numFunct = -1;
			try{
				numFunct = Integer.parseInt(funct, 2);
			}
			catch(NumberFormatException e){
				System.err.println("opConverter(): CAN'T CONVERT FUNCT BINARY TO DECIMAL");
			}

			switch(numFunct){ //Convert the decimal funct to an instruction name
			case 32:
				instruction[0] = "ADD";
				instruction[1] = "r";
				break;
				
			case 34:
				instruction[0] = "SUB";
				instruction[1] = "i";
				break;
				
			case 42:
				instruction[0] = "SLT";
				instruction[1] = "r";
				break;

			default:
				instruction[0] = "ERR";
				instruction[1] = "err";
				break;
			}
		}
		return instruction;
	}


	public static boolean LW(String instruction, int stage){ //stage to execute. Returns if it stalls (true) or not (false)
		//R(rt) = M(R(rs) + imm);
		String[] decodInst = instBreaker(instruction, 'i'); //op, rs, rt, imm
		int rs = Integer.parseInt(decodInst[1], 2); //Convert the binary rs to integer format
		int rt = Integer.parseInt(decodInst[2], 2); //Convert the binary rt to integer format
		int imm = binToDec(decodInst[3]); //Convert the binary imm to integer format

		if(stage == 1){//ID. Permission lock the registers. Stall if the needed registers are locked. 
			if(regPerm[rs] == true){ //If rs is about to be written to by another instruction, stall
				return true; //Do nothing an just stall till next cycle
			}
			regPerm[rt] = true; //Lock the (to be written to) register to prevent data hazards
		}

		if(stage == 2){ //EXE. No actual calculations are done since there's no place to write them to yet. 
			regPerm[rt] = true; //Re-lock the register for robustness. 
		}
		if(stage == 3){ //MEM. No memory is loaded from here since memory cannot be changed before the WB of the next cycle, so temporary storage is unnecessary. 
			regPerm[rt] = true; //Re-lock the register for robustness. 
		}
		if(stage == 4){ //WB. Write the memory to address
			registers[rt] = memory[(registers[rs] + imm) / 4]; //Divid R(rs) + imm by 4 since the memory array address are divided by 4
			regPerm[rt] = false; //Remove the permission lock on the register
		}

		return false; //Does this instruction need to stall?
	}

	public static boolean SW(String instruction, int stage){
		//M(R(rs) + IMM) = R(rt)

		String[] decodInst = instBreaker(instruction, 'i'); //op, rs, rt, imm
		int rs = Integer.parseInt(decodInst[1], 2); //Convert the binary rs to integer format
		int rt = Integer.parseInt(decodInst[2], 2); //Convert the binary rt to integer format
		int imm = binToDec(decodInst[3]); //Convert the binary imm to integer format

		if(stage == 1){//ID. Permission lock the registers. Stall if the needed registers are locked. 
			if(regPerm[rt] == true){ //If rt is about to be written to by another instruction, stall
				return true; //Do nothing an just stall till next cycle
			}
		}
		if(stage == 2){//EXE
			return false;
		}
		if(stage == 3){//MEM
			memory[(registers[rs] + imm) / 4] = registers[rt]; //Divide memory address by 4 since memory array is divided by 4
			return false;
		}
		if(stage == 4){//WB
			return false; //SW doesn't do anything during the WB stage
		}

		return false;
	}

	public static boolean ADD(String instruction, int stage){
		//R(rd) = R(rs) + R(rt)

		String[] decodInst = instBreaker(instruction, 'r'); //op, rs, rt, rd, shamt, funct
		int rs = Integer.parseInt(decodInst[1], 2); //Convert the binary rs to integer format
		int rt = Integer.parseInt(decodInst[2], 2); //Convert the binary rt to integer format
		int rd = Integer.parseInt(decodInst[3], 2); //Convert the binary rd to integer format

		if(stage == 1){//ID. Permission lock the registers. Stall if the needed registers are locked. 
			if(regPerm[rs] == true || regPerm[rt] == true){ //If rs||rt are about to be written to by another instruction, stall
				return true; //Do nothing an just stall till next cycle
			}
			regPerm[rd] = true; //Lock the (to be written to) register to prevent data hazards
		}

		if(stage == 2){//EXE
			regPerm[rd] = true;
			return false;
		}
		if(stage == 3){//MEM. ADD doesn't use the MEM stage
			regPerm[rd] = true;
			return false;
		}
		if(stage == 4){//WB
			registers[rd] = registers[rs] + registers[rt]; //Store the result of the addition
			regPerm[rd] = false; //Release the lock on register rd
			return false;
		}

		return false;
	}

	public static boolean ADDI(String instruction, int stage){
		//R(rt) = R(rs) + imm
		String[] decodInst = instBreaker(instruction, 'i'); //op, rs, rt, imm
		int rs = Integer.parseInt(decodInst[1], 2); //Convert the binary rs to integer format
		int rt = Integer.parseInt(decodInst[2], 2); //Convert the binary rt to integer format
		int imm = binToDec(decodInst[3]); //Convert the binary imm to integer format

		if(stage == 1){//ID. Permission lock the registers. Stall if the needed registers are locked. 
			if(regPerm[rs] == true){ //If rs||rt are about to be written to by another instruction, stall
				return true; //Do nothing an just stall till next cycle
			}
			regPerm[rt] = true; //Lock the (to be written to) register to prevent data hazards
		}

		if(stage == 2){//EXE
			regPerm[rt] = true;
			return false;
		}
		if(stage == 3){//MEM. ADDI doesn't use the MEM stage
			regPerm[rt] = true;
			return false;
		}
		if(stage == 4){//WB
			registers[rt] = registers[rs] + imm; //Store the result of the addition
			regPerm[rt] = false; //Release the lock on register rd
			return false;
		}

		return false;
	}

	public static boolean SUB(String instruction, int stage){
		//R(rd) = R(rs) - R(rt)

		String[] decodInst = instBreaker(instruction, 'r'); //op, rs, rt, rd, shamt, funct
		int rs = Integer.parseInt(decodInst[1], 2); //Convert the binary rs to integer format
		int rt = Integer.parseInt(decodInst[2], 2); //Convert the binary rt to integer format
		int rd = Integer.parseInt(decodInst[3], 2); //Convert the binary rd to integer format

		if(stage == 1){//ID. Permission lock the registers. Stall if the needed registers are locked. 
			if(regPerm[rs] == true || regPerm[rt] == true){ //If rs||rt are about to be written to by another instruction, stall
				return true; //Do nothing an just stall till next cycle
			}
			regPerm[rd] = true; //Lock the (to be written to) register to prevent data hazards
		}

		if(stage == 2){//EXE
			regPerm[rd] = true;
			return false;
		}
		if(stage == 3){//MEM. SUB doesn't use the MEM stage
			regPerm[rd] = true;
			return false;
		}
		if(stage == 4){//WB
			registers[rd] = registers[rs] - registers[rt]; //Store the result of the subtraction
			regPerm[rd] = false; //Release the lock on register rd
			return false;
		}

		return false;
	}

	public static boolean SLT(String instruction, int stage){ //Set less than
		//R(rd) = (R(rs) < R(rt))? 1:0

		String[] decodInst = instBreaker(instruction, 'r'); //op, rs, rt, rd, shamt, funct
		int rs = Integer.parseInt(decodInst[1], 2); //Convert the binary rs to integer format
		int rt = Integer.parseInt(decodInst[2], 2); //Convert the binary rt to integer format
		int rd = Integer.parseInt(decodInst[3], 2); //Convert the binary rd to integer format

		if(stage == 1){ //ID. Permission lock the registers. Stall if the needed registers are locked. 
			if(regPerm[rs] == true || regPerm[rt] == true){ //If rs/rt is about to be written to by another instruction, stall
				return true; //Do nothing an just stall till next cycle
			}
			regPerm[rd] = true; //Lock the (to be written to) register to prevent data hazards
		}

		if(stage == 2){ //EXE
			return false;
		}

		if(stage == 3){ //MEM
			return false;
		}

		if(stage == 4){ //WB
			if(registers[rs] < registers[rt]){ //If rs < rt then set rd to 1
				registers[rd] = 1;

			}else{
				registers[rd] = 0;
			}
			regPerm[rd] = false; //Release lock on rd

			return false;
		}

		return false;
	}
	public static boolean BEQ(String instruction, int stage){
		//if(R(rs) == R(rt)) PC = PC + 4 + BranchAddr
		String[] decodInst = instBreaker(instruction, 'i'); //op, rs, rt, imm
		int rs = Integer.parseInt(decodInst[1], 2); //Convert the binary rs to integer format
		int rt = Integer.parseInt(decodInst[2], 2); //Convert the binary rt to integer format
		int imm = binToDec(decodInst[3]); //Convert the binary imm to integer format

		if(stage == 1){ //ID. No registers are getting written to so don't lock any registers
			if(regPerm[rs] == true || regPerm[rt] == true){
				return true; //Stall out if the rs/ rt registers are locked up
			}
			return false;
		}

		if(stage == 2){ //EXE
			if(registers[rs] == registers[rt]){ //If condition is true
				pipeStage[0] = null; //Flush IF
				pipeStage[1] = null; //Flush ID 
				branchFlush = true; //Don't IF until the PC is changed in the MEM stage of branch
			}
			return false;
		}

		if(stage == 3){ //MEM. Verify if condition is true then flush and move PC or do nothing if condition is not meet
			if(registers[rs] == registers[rt]){ //If condition is true
				pipeStage[2] = null; //Flush EXE
				PCInst = PCInst - 3 + 1 + imm; //Reduce by 3 for the 3 pipeline stages needed to execute BNE, + 1 + imm
				branchFlush = false; //PC has been updated, safe to IF now

				//Unlock all the register permissions since the current instructions in ID/ EXE are now gone and won't need to write to the registers
				for(int x = 0; x < regPerm.length; x++){
					regPerm[x] = false;
				}
			}
			//If condition is not true, do nothing
			return false;
		}

		if(stage == 4){ //WB. This stage is unused by the BEQ instruction
			return false;
		}

		return false;
	}
	public static boolean BNE(String instruction, int stage){
		//if(R(rs) != R(rt)) PC = PC + 4 + BranchAddr


		String[] decodInst = instBreaker(instruction, 'i'); //op, rs, rt, imm
		int rs = Integer.parseInt(decodInst[1], 2); //Convert the binary rs to integer format
		int rt = Integer.parseInt(decodInst[2], 2); //Convert the binary rt to integer format
		int imm = binToDec(decodInst[3]); //Convert the signed binary imm to integer format

		if(stage == 1){ //ID. No registers are getting written to so don't lock any registers
			if(regPerm[rs] == true || regPerm[rt] == true){
				return true; //Stall out if the rs/ rt registers are locked up
			}
			return false;
		}

		if(stage == 2){ //EXE
			if(registers[rs] != registers[rt]){ //If condition is true
				pipeStage[0] = null; //Flush IF
				pipeStage[1] = null; //Flush ID 
				branchFlush = true; //Don't IF until the PC is changed in the MEM stage of branch
			}
			return false;
		}

		if(stage == 3){ //MEM. Verify if condition is true then flush and move PC or do nothing if condition is not meet
			if(registers[rs] != registers[rt]){ //If condition is true
				pipeStage[2] = null; //Flush EXE 
				PCInst = PCInst - 3 + 1 + imm; //Reduce by 3 for the 3 pipeline stages needed to execute BNE, + 1 + imm
				branchFlush = false; //PC has been updated, safe to IF now

				//Unlock all the register permissions since the current instructions in ID/ EXE are now gone and won't need to write to the registers
				for(int x = 0; x < regPerm.length; x++){
					regPerm[x] = false;
				}
			}
			//If condition is not true, do nothing
			return false;
		}

		if(stage == 4){ //WB. This stage is unused by the BNE instruction
			return false;
		}

		return false;
	}

	//Converts binary numbers to decimal (takes negative binaries into account)
	public static int binToDec(String binary){
		char[] number = binary.toCharArray();

		if(number[0] == '0'){ //If the number is a positive binary number
			return Integer.parseInt(binary, 2);
		}

		if(number[0] == '1'){ //If the number is a negative binary number
			//Flip the bits and add 1
			for(int x = 0; x < number.length; x++){//Flip the bits
				if(number[x] == '1')
					number[x] = '0';
				else
					number[x] = '1';
			}
			binary = new String(number);
			return -1 * (Integer.parseInt(binary, 2) + 1);
		}
		System.err.println("binToDec(): COULD NOT CONVERT " + binary + " TO DECIMAL");
		return 0;
	}
}
