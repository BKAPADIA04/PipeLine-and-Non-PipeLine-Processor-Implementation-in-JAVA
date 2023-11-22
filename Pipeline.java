import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Pipeline {
    
    //Pair to store register address,clock cycle,register value and write back of the register
    static class Pair{
        int reg_add;
        int clk_cycle;
        int reg_value;
        int write_back;

        public Pair(int reg_add,int clk_cycle,int reg_value,int write_back){
            this.reg_add = reg_add;
            this.clk_cycle = clk_cycle;
            this.reg_value = reg_value;
            this.write_back = write_back;
        }
    }

    //Global Variables
    //Array to store Instructions and Data in the Memory
    static String[] Inst_Memory = new String[1000];
    static int[] Data_Memory = new int[1000];

    //HashMap to store the value of the respective register
    static HashMap<Integer,Integer> Register_Value = new HashMap<>();
    //HashMap to store the function value of the respective instruction
    static HashMap<Integer,String> Function_Value = new HashMap<>();
    //HashMap to map addresses and names of registers
    static HashMap<Integer,String> Register_Box = new HashMap<>();
    //HashMap to map the address of the register to the details of the register
    static HashMap<Integer,Pair> Pipeline_register = new HashMap<>();
    //Linked List to store R,I and B Type Objects 
    static LinkedList<Object> instr_fetch_list = new LinkedList<Object>();
    //This maps integers to various objects
    static ArrayList<Integer>instr_fetch_type = new ArrayList<Integer>();
    //Program Counter
    static int pc = 0;
    //Current Clock Cycle 
    static int counter = 0;

    //This function contains the loop which basically converts the given string in binary to decimal
    static int Binary_to_Decimal_Loop(String n)
    {
        int sum=0;
        for(int i=0;i<n.length();i++){
                sum += (Integer.parseInt(n.substring(n.length()-i-1, n.length()-i))*Math.pow(2,i));
            }
        return sum;
    }

    //This function converts the given string into decimal 
    //It uses the above function 
    //It uses the given stirng for positive numbers and 2's complement for negative numbers
    static int Binary_to_Decimal(String n){
        int sum = 0;
        if(n.charAt(0) == '0')
        {
            sum = Binary_to_Decimal_Loop(n);
        }
        else if(n.charAt(0) == '1')
        {
            String temp = n;
            StringBuffer tempFinal = new StringBuffer(n);
            for(int i=0;i<temp.length();i++)
            {
                if(temp.charAt(i) == '0')
                {
                    tempFinal.setCharAt(i, '1');
                }
                else if(temp.charAt(i) == '1')
                {   
                    tempFinal.setCharAt(i, '0');
                }
            }
            sum = Binary_to_Decimal_Loop(tempFinal.toString());
            sum += 1;
            sum = sum * -1;
        }
        return sum;
    }


    //Class focusses on implementation of all the R-Type Instructions used in the program
    static class Rtype {
        String instruction;
        int rs;
        int rt;
        int rd;
        int shamt;
        int function;

        //Initially,the registers,shamt and function is set to zero.
        public Rtype(String k){
            this.instruction = k;
            this.rs = 0;
            this.rt = 0;
            this.rd = 0;
            this.shamt = 0;
            this.function = 0;
        }

        void Decode_Phase(){
            //We need not decode the opcode as opcode of R-Type Instruction is 000000
            //The Decode Phase basically works in 3 parts:
            // 1) It separates rs,rt,rd,shamt and function fields of the instruction using substring
            // 2) These separated strings are converted from binary to decimal 
            // 3) Finally,we retrieve the value of the register from the HashMap Register_Value

            this.rs = Register_Value.get(Binary_to_Decimal_Loop(this.instruction.substring(6,11)));
            this.rt = Register_Value.get(Binary_to_Decimal_Loop(this.instruction.substring(11,16)));
            this.rd = Register_Value.get(Binary_to_Decimal_Loop(this.instruction.substring(16,21)));
            this.shamt = (Binary_to_Decimal_Loop(this.instruction.substring(21,26)));
            this.function = (Binary_to_Decimal_Loop(this.instruction.substring(26,32)));
        }

        //Function to return basic arithmetic operations
        static int operation(String op,int a,int b)
        {
            if(op.equals("+"))
                return a+b;
            if(op.equals("-"))
                return a-b;
            if(op.equals("*"))
                return a*b;
            if(op.equals("slt"))
                return a < b ? 1 : 0;
            return 0;
        }

        //Important Function:Performs forwarding
        //Concept:
        //Firstly,we detect if the registers to loaded into the pipeline register
        void Execute_Op(String op,int a,int b)
        {
            Pair add_1 = Pipeline_register.get(Binary_to_Decimal_Loop(this.instruction.substring(6,11)));
            Pair add_2 = Pipeline_register.get(Binary_to_Decimal_Loop(this.instruction.substring(11,16)));
                if(add_1 != null && add_2 != null){
                    //Both registers are forwarded.They are not written back
                    if( (counter+3 <= add_1.write_back) && (counter+3 <= add_2.write_back) ){
                        //Both registers are forwarded
                        if(counter+3 > add_1.clk_cycle && counter+3 > add_2.clk_cycle){
                            this.rs = add_1.reg_value;
                            this.rt = add_2.reg_value;
                            this.rd = operation(op, this.rs,this.rt);
                            Pair p = new Pair(Binary_to_Decimal_Loop(this.instruction.substring(16,21)), counter+3, this.rd, counter+5);
                            //We update the hashmap of registers with updated values of execute phase and write back clk cycle and updated values
                            Pipeline_register.put(Binary_to_Decimal_Loop(this.instruction.substring(16,21)),p);
                        }
                        else{
                            //Implementation of stalls
                            for(int i=1;i<5;i++){
                                if(counter + 3 + i > add_1.clk_cycle && counter + 3 + i > add_2.clk_cycle){
                                    this.rs = add_1.reg_value;
                                    this.rt = add_2.reg_value;
                                    this.rd = operation(op, this.rs,this.rt);
                                    counter += i;
                                    Pair p = new Pair(Binary_to_Decimal_Loop(this.instruction.substring(16,21)), counter+3, this.rd, counter+5);
                                    Pipeline_register.put(Binary_to_Decimal_Loop(this.instruction.substring(16,21)),p);
                                    break;
                                }
                            }
                        }
                    }
                    
                    //One of the registers is written back and the other is forwarded
                    else if( (counter+3 <= add_2.write_back) && (counter + 3 >= add_1.write_back) ){
                        if(counter+3 > add_2.clk_cycle){
                            this.rt = add_2.reg_value;
                            this.rd = operation(op, a, this.rt);
                            Pair p = new Pair(Binary_to_Decimal_Loop(this.instruction.substring(16,21)), counter+3, this.rd, counter+5);
                            Pipeline_register.put(Binary_to_Decimal_Loop(this.instruction.substring(16,21)),p);
                        }
                        else{
                            for(int i=1;i<5;i++){
                                if(counter + 3 + i > add_2.clk_cycle){
                                    this.rt = add_2.reg_value;
                                    this.rd = operation(op, a , this.rt);
                                    counter += i;
                                    Pair p = new Pair(Binary_to_Decimal_Loop(this.instruction.substring(16,21)), counter+3, this.rd, counter+5);
                                    Pipeline_register.put(Binary_to_Decimal_Loop(this.instruction.substring(16,21)),p);
                                    break;
                                }
                            }
                        }
                    }
                    //One of the registers is written back and the other is forwarded
                    else if((counter+3 <= add_1.write_back) && (counter + 3 >= add_2.write_back)){
                        if(counter+3 > add_1.clk_cycle){
                            this.rs = add_1.reg_value;
                            this.rd = operation(op, this.rs ,b);
                            Pair p = new Pair(Binary_to_Decimal_Loop(this.instruction.substring(16,21)), counter+3, this.rd, counter+5);
                            Pipeline_register.put(Binary_to_Decimal_Loop(this.instruction.substring(16,21)),p);
                        }
                        else{
                            for(int i=1;i<5;i++){
                                if(counter + 3 + i > add_1.clk_cycle){
                                    this.rs = add_1.reg_value;
                                    this.rd = operation(op, this.rs,b);
                                    counter += i;
                                    Pair p = new Pair(Binary_to_Decimal_Loop(this.instruction.substring(16,21)), counter+3, this.rd, counter+5);
                                    Pipeline_register.put(Binary_to_Decimal_Loop(this.instruction.substring(16,21)),p);
                                    break;
                                }
                            }
                        }
                    }
                    //Both the registers are written back
                    else{
                        this.rd = operation(op, a, b);
                        Pair p = new Pair(Binary_to_Decimal_Loop(this.instruction.substring(16,21)), counter+3, this.rd, counter+5);
                        Pipeline_register.put(Binary_to_Decimal_Loop(this.instruction.substring(16,21)),p);
                    }
                }
        }
    
        //Calls the Execute_Op instructions
        void Execute_Phase() {
            // add instruction
            if(Function_Value.get(this.function).compareTo("add") == 0) {
                Execute_Op("+",this.rs,this.rt);
            }
            // sub instruction
            else if(Function_Value.get(this.function).compareTo("sub") == 0) {
                Execute_Op("-",this.rs,this.rt);
            }
            // mul instruction
            else if(Function_Value.get(this.function).compareTo("mul") == 0) {
                Execute_Op("*",this.rs,this.rt);
            }
            // slt instruction
            else if(Function_Value.get(this.function).compareTo("slt") == 0) {
                Execute_Op("slt",this.rs,this.rt);
            }
            // sll instruction
            else if(Function_Value.get(this.function).compareTo("left_shift") == 0) {
                Pair shift_1 = Pipeline_register.get(Binary_to_Decimal_Loop(this.instruction.substring(11,16)));
                if(shift_1 != null){
                    if(counter+3 <= shift_1.write_back){
                        if(counter + 3 > shift_1.clk_cycle){
                            this.rt = shift_1.reg_value;
                            int temp = (int)Math.pow(2, this.shamt);
                            this.rd = operation("*", this.rt, temp);
                            Pair p = new Pair(Binary_to_Decimal_Loop(this.instruction.substring(16,21)), counter+3, this.rd, counter+5);
                            Pipeline_register.put(Binary_to_Decimal_Loop(this.instruction.substring(16,21)),p);
                        }
                        else{
                            for(int i=1;i<5;i++){
                                if(counter + 3 + i > shift_1.clk_cycle){
                                    this.rt = shift_1.reg_value;
                                    int temp = (int)Math.pow(2, this.shamt);
                                    this.rd = operation("*", this.rt, temp);
                                    counter += i;
                                    Pair p = new Pair(Binary_to_Decimal_Loop(this.instruction.substring(16,21)), counter+3, this.rd, counter+5);
                                    Pipeline_register.put(Binary_to_Decimal_Loop(this.instruction.substring(16,21)),p);
                                    break;
                                }
                            }
                        }
                    }
                    else{
                        int temp = (int)Math.pow(2, this.shamt);
                        this.rd = operation("*", this.rt, temp);
                        Pair p = new Pair(Binary_to_Decimal_Loop(this.instruction.substring(16,21)), counter+3, this.rd, counter+5);
                        Pipeline_register.put(Binary_to_Decimal_Loop(this.instruction.substring(16,21)),p);
                        }
                    }
                }
            }

        //The above mentioned instruction do not fetch any data from the memory
        //So no need of Memory_Phase here
        void Memory_Phase(){
            @SuppressWarnings("unused")
            String temp = "No use of memory in r-type";
        }

        //This Phase works in 3 parts:
        // 1) We fetch the rd field from the instruction
        // 2) Convert this from binary to decimal
        // 3) Map the calculated rd value to the rd field in the HashMap Register_Value
        void Write_Back_Phase(){
            Register_Value.put(Binary_to_Decimal_Loop(this.instruction.substring(16, 21)),this.rd);
        }
    }    

    //Class focusses on implementation of all I-Type Instructions of beq and bne type used in the program
    //We are detecting the control hazard in the IF stage itself
    static class Btype {
        
        String instruction;
        String opcode;
        int rs;
        int rt;
        int immediate;
        int temp;

        public Btype(String k){
            this.opcode = "0";
            this.instruction = k;
            this.rs = 0;
            this.rt = 0;
            this.immediate = 0;
            this.temp = 0;
        }

        //General Function 
        void checker()
        {
            this.opcode = this.instruction.substring(0,6);
            this.rs = Register_Value.get(Binary_to_Decimal_Loop(this.instruction.substring(6,11)));
            this.rt = Register_Value.get(Binary_to_Decimal_Loop(this.instruction.substring(11,16)));
            this.immediate = (Binary_to_Decimal(this.instruction.substring(16)));
            
            if(this.opcode.compareTo("000100") == 0)
            {
                operation_checker("==", this.rs, this.rt);
            }
            else if(this.opcode.compareTo("000101") == 0) 
            {
                operation_checker("!=", this.rs, this.rt);
            }
        }

        //Function to check logical operations
        static boolean op_check(String op,int a,int b)
        {
            if(op.equals("=="))
                return a == b;
            if(op.equals("!="))
                return a != b;
            return false;
        }

        void Decode_Phase() {
            @SuppressWarnings("unused")
            String temp = "No use in Btype";
        }
        void Execute_Phase() {
            @SuppressWarnings("unused")
            String temp = "No use in Btype";
        }
        void Memory_Phase() {
            @SuppressWarnings("unused")
            String temp = "No use in Btype";
        }
        void Write_Back_Phase() {
            @SuppressWarnings("unused")
            String temp = "No use in Btype";
        }

        //Function to handle data hazard using forwarding
        void operation_checker(String op,int a,int b)
        {
            Pair add_1 = Pipeline_register.get(Binary_to_Decimal_Loop(this.instruction.substring(6,11)));
            Pair add_2 = Pipeline_register.get(Binary_to_Decimal_Loop(this.instruction.substring(11,16)));
            if(add_1 != null && add_2 != null){
                //Both registers are forwarded.They are not written back
                if( (counter+3 <= add_1.write_back) && (counter+3 <= add_2.write_back) ){
                    //Both registers are forwarded
                    if(counter+3 > add_1.clk_cycle && counter+3 > add_2.clk_cycle){
                        this.rs = add_1.reg_value;
                        this.rt = add_2.reg_value;
                        if(op_check(op, this.rs, this.rt)){
                            pc += this.immediate * 4;
                            
                        }
                    }
                    else{
                        //Implementation of stalls
                        for(int i=1;i<5;i++){
                            if(counter + 3 + i > add_1.clk_cycle && counter + 3 + i > add_2.clk_cycle){
                                this.rs = add_1.reg_value;
                                this.rt = add_2.reg_value;
                                if(op_check(op, this.rs, this.rt)){
                                    pc += this.immediate * 4;
                                    counter += i;
                                   
                                }
                                break;
                            }
                        }
                    }
                }
                //One of the registers is written back and the other is forwarded
                else if( (counter+3 <= add_2.write_back) &&  (counter + 3 >= add_1.write_back) ){
                    if(counter+3 > add_2.clk_cycle){
                        this.rt = add_2.reg_value;
                        if(op_check(op, a, this.rt)){
                            pc += this.immediate * 4; 
                        }
                    }
                    else{
                        for(int i=1;i<5;i++){
                            if(counter + 3 + i > add_2.clk_cycle){
                                this.rt = add_2.reg_value;
                                if(op_check(op, a, this.rt)){
                                    pc += this.immediate * 4;
                                    
                                    counter += i; 
                                }
                                break;
                            }
                        }
                    }
                }
                //One of the registers is written back and the other is forwarded
                else if((counter+3 <= add_1.write_back) && (counter + 3 >= add_2.write_back)){
                    if(counter+3 > add_1.clk_cycle){
                        this.rs = add_1.reg_value;
                        if(op_check(op, this.rs,b)){
                            pc += this.immediate * 4;
                               
                        }
                    }
                    else{
                        for(int i=1;i<5;i++){
                            if(counter + 3 + i > add_1.clk_cycle){
                                this.rs = add_1.reg_value;
                                if(op_check(op, this.rs,b)){
                                    pc += this.immediate * 4;
                                    
                                    counter += i; 
                                }
                                break;
                            }
                        }
                    }
                }
                else{
                //Both registers are written back
                    if(op_check(op,a,b)){
                        pc += this.immediate * 4; 
                    }
                }
            }
        }
    }

    //Class focusses on implementation of all the I-Type Instructions used in the program
    static class Itype {

        String instruction;
        String opcode;
        int rs;
        int rt;
        int immediate;
        int temp;

        //Initially,the registers,opcode,immediate and temp is set to zero.
        public Itype(String k){
            this.opcode = "0";
            this.instruction = k;
            this.rs = 0;
            this.rt = 0;
            this.immediate = 0;
            this.temp = 0;
        }

        //We need to decode the opcode as well
        //The Decode Phase basically works in 3 parts:
        // 1) It separates opcode,rs,rt and immediate fields of the instruction using substring
        // 2) These separated strings are converted from binary to decimal 
        // 3) Finally,we retrieve the value of the register from the HashMap Register_Value
        void Decode_Phase() {
            this.opcode = this.instruction.substring(0,6);
            this.rs = Register_Value.get(Binary_to_Decimal_Loop(this.instruction.substring(6,11)));
            this.rt = Register_Value.get(Binary_to_Decimal_Loop(this.instruction.substring(11,16)));
            this.immediate = (Binary_to_Decimal(this.instruction.substring(16)));
        }

        void Execute_Phase() {
            //Load Instruction with forwarding implemented
            if(this.opcode.compareTo("100011") == 0){
                Pair load = Pipeline_register.get(Binary_to_Decimal_Loop(this.instruction.substring(6,11)));
                if(load != null){
                    if(counter+3 <= load.write_back){
                        if(counter+3 > load.clk_cycle){
                        this.rs = load.reg_value;
                        this.temp = this.rs+this.immediate;
                        }
                        else{
                            for(int i=1;i<5;i++){
                                if(counter + 3 + i > load.clk_cycle){
                                    this.rs = load.reg_value;
                                    this.temp = this.rs + this.immediate;
                                    counter += i;
                                    break;
                                }
                            }
                        }
                    }
                    else{
                        this.temp = this.rs+this.immediate;
                    }
                }
            }
            //Store Instruction with forwarding implemented
            else if(this.opcode.compareTo("101011") == 0){
                Pair store = Pipeline_register.get(Binary_to_Decimal_Loop(this.instruction.substring(6,11)));
                if(store != null){
                    if(counter+3 <= store.write_back){
                        if(counter+3 > store.clk_cycle){
                        this.rs = store.reg_value;
                        this.temp = this.rs+this.immediate;
                        }
                        else{
                            for(int i=1;i<5;i++){
                                if(counter + 3 + i > store.clk_cycle){
                                    this.rs = store.reg_value;
                                    this.temp = this.rs + this.immediate;
                                    counter += i;
                                    break;
                                }
                            }
                        }
                    }
                    else{
                        this.temp = this.rs+this.immediate;
                    }
                }
            }
            //Addi with forwarding
            else if(this.opcode.compareTo("001000") == 0){
                Pair add_i = Pipeline_register.get(Binary_to_Decimal_Loop(this.instruction.substring(6,11)));
                if(add_i != null){
                    if(counter+3 <= add_i.write_back){
                        if(counter+3 > add_i.clk_cycle){
                            this.rs = add_i.reg_value;
                            this.temp = this.rs+this.immediate;
                            Pair p = new Pair(Binary_to_Decimal_Loop(this.instruction.substring(11,16)), counter+3, this.temp, counter+5);
                            Pipeline_register.put(Binary_to_Decimal_Loop(this.instruction.substring(11,16)),p);
                        }
                        else{
                            for(int i=1;i<5;i++){
                                if(counter + 3 + i > add_i.clk_cycle){
                                    this.rs = add_i.reg_value;
                                    this.temp = this.rs + this.immediate;
                                    counter += i;
                                    Pair p = new Pair(Binary_to_Decimal_Loop(this.instruction.substring(11,16)), counter+3, this.temp, counter+5);
                                    Pipeline_register.put(Binary_to_Decimal_Loop(this.instruction.substring(11,16)),p);
                                    break;
                                }
                            }
                        }
                    }
                    else{
                        this.temp = this.rs+this.immediate;
                        Pair p = new Pair(Binary_to_Decimal_Loop(this.instruction.substring(11,16)), counter+3, this.temp, counter+5);
                        Pipeline_register.put(Binary_to_Decimal_Loop(this.instruction.substring(11,16)),p);
                    }
                }
            }
        }
        
        void Memory_Phase() {
            //Load instruction
            if(this.opcode.compareTo("100011") == 0){
                this.temp = Data_Memory[this.temp];
                Pair p = new Pair(Binary_to_Decimal_Loop(this.instruction.substring(11,16)), counter+4, this.temp, counter+5);
                Pipeline_register.put(Binary_to_Decimal_Loop(this.instruction.substring(11,16)),p);
            }
            //Store
            else if(this.opcode.compareTo("101011") == 0){
                Pair Store = Pipeline_register.get(Binary_to_Decimal_Loop(this.instruction.substring(11,16)));
                if(Store != null){
                    if(counter + 3 <= Store.write_back){
                        if(counter+4 > Store.clk_cycle){
                            this.rt = Store.reg_value;
                            Data_Memory[this.temp] = this.rt;
                        }
                        else{
                            for(int i=1;i<5;i++){
                                if(counter + 4 + i > Store.clk_cycle){
                                    this.rt = Store.reg_value;
                                    counter += i;
                                    Data_Memory[this.temp] = this.rt;
                                    break;
                                }
                            }
                        }
                    }
                    else{
                        Data_Memory[this.temp] = this.rt;
                    }
                }
            }

        }
        
        void Write_Back_Phase() {
            //Load instruction
            if(this.opcode.compareTo("100011") == 0){
                Register_Value.put(Binary_to_Decimal_Loop(this.instruction.substring(11, 16)),this.temp);
            }
            //Addi instruction
            else if(this.opcode.compareTo("001000") == 0){
                Register_Value.put(Binary_to_Decimal_Loop(this.instruction.substring(11, 16)),this.temp);
            }
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int clock_cycle = 0;

        System.out.println("Enter the program to be run : Factorial (0) or Fibbonaci (1)");
        int program_num = sc.nextInt();
        String Filename = "";
        StringBuffer FileNameFinal = new StringBuffer(Filename);
        if(program_num == 0)
            FileNameFinal.append("factorial");
        else if(program_num == 1)
            FileNameFinal.append("fibbonaci");
        else 
            FileNameFinal.append("factorial");
        
        String File = FileNameFinal.toString();
        int k = 0;

        //File IO
        try {
          File file_read = new File(File.concat(".txt"));
          Scanner file_reader = new Scanner(file_read);
          while (file_reader.hasNextLine()) {
            String data = file_reader.nextLine();
            if(data.equals(null)){
                break;
            }
            Inst_Memory[k] = data;
            k+=4;
          }
          file_reader.close();
        } 
        catch (FileNotFoundException e) {
          System.out.println("An error occurred.");
          e.printStackTrace();
        }

        System.out.println("Enter no. of integers for "+File+" : ");
        int t1 = sc.nextInt();

        System.out.println("Enter base address for input : ");
        int t2 = sc.nextInt();

        System.out.println("Enter base address for output : ");
        int t3 = sc.nextInt();

        //Input the values of data memory
        System.out.println("Enter integers : ");
        for(int i=t2;i<t2+t1*4;i+=4){
            Data_Memory[i] = sc.nextInt();
        }
        sc.close();
        System.out.println();

        //Insert register value and register t1,t2,t3 are fixed for input value.....based on temp.asm
        for(int i=0;i<=25;i++){
            if(i == 9){
                Register_Value.put(i,t1);
            }
            else if(i == 10){
                Register_Value.put(i, t2);
            }
            else if(i == 11){
                Register_Value.put(i, t3);
            }
            else{
                Register_Value.put(i, 0);
            }
        }

        //Initial Pairs made because of no of inputs(t0),input(t1) and output(t2)
        Pair p_0 = new Pair(0, 0, 0, -1);
        Pipeline_register.put(0,p_0);

        Pair p_1 = new Pair(9, 0, t1, -1);
        Pipeline_register.put(9,p_1);

        Pair p_2 = new Pair(10, 0, t2, -1);
        Pipeline_register.put(10,p_2);

        Pair p_3 = new Pair(11, 0, t3, -1);
        Pipeline_register.put(11,p_3);

        Register_Box.put(0, "$zero");
        Register_Box.put(1, "$at");
        Register_Box.put(2, "$v0");
        Register_Box.put(3, "$v1");
        Register_Box.put(4, "$a0");
        Register_Box.put(5, "$a1");
        Register_Box.put(6, "$a2");
        Register_Box.put(7, "$a3");
        Register_Box.put(8, "$t0");
        Register_Box.put(9, "$t1");
        Register_Box.put(10, "$t2");
        Register_Box.put(11, "$t3");
        Register_Box.put(12, "$t4");
        Register_Box.put(13, "$t5");
        Register_Box.put(14, "$t6");
        Register_Box.put(15, "$t7");
        Register_Box.put(16, "$s0");
        Register_Box.put(17, "$s1");
        Register_Box.put(18, "$s2");
        Register_Box.put(19, "$s3");
        Register_Box.put(20, "$s4");
        Register_Box.put(21, "$s5");
        Register_Box.put(22, "$s6");
        Register_Box.put(23, "$s7");        
        Register_Box.put(24, "$t8");
        Register_Box.put(25, "$t9"); 


        System.out.println("Initial registers value after input");
        for(int i=0;i<Register_Value.size();i++){
            System.out.print(Register_Box.get(i) +":" +Register_Value.get(i)+"  ");
        }
        System.out.println();

        //Insert function value
        //Function values give us the operation in R-Type Instruction
        Function_Value.put(32, "add");
        Function_Value.put(34, "sub");
        Function_Value.put(0, "left_shift");
        Function_Value.put(42,"slt");
        Function_Value.put(2, "mul");
        
        //Logic : These counters increment by 1 after every iteration
        //These maintains that various instructions run at a particular time

        int id_counter = -1;//ID Phase Counter
        int ex_counter = -2;//Ex Phase Counter
        int mem_counter = -3;//Memory Phase Counter
        int wb_counter = -4;//WriteBack Phase Counter

        
        for(;pc<4*k;pc+=4)
        {           
        //IF Phase
           if(pc < k)
           {
                //R-Type or mul instruction
                if( (Inst_Memory[pc].substring(0,6).compareTo("000000") == 0) || (Inst_Memory[pc].substring(0,6).compareTo("011100") == 0))
                {
                    // form a object of R type and add in the linkedlist
                    Rtype r_obj = new Rtype(Inst_Memory[pc]);
                    instr_fetch_list.add(r_obj);
                    instr_fetch_type.add(1);
                    counter++;
                }
                //B-Type instructions(beq and bne)
                else if(Inst_Memory[pc].substring(0, 6).compareTo("000100") == 0 || (Inst_Memory[pc].substring(0, 6).compareTo("000101") == 0)) {
                    // form a object of B type and add in the linkedlist
                    Btype b_obj = new Btype(Inst_Memory[pc]);
                    instr_fetch_list.add(b_obj);
                    instr_fetch_type.add(2);
                    b_obj.checker();
                    counter++;
                }
                //I-Type instructions
                else 
                {
                    // form a object of R type and add in the linkedlist
                    Itype i_obj = new Itype(Inst_Memory[pc]);
                    instr_fetch_list.add(i_obj);
                    instr_fetch_type.add(3);
                    counter++;
                }
           } 

           
           //Execute this flag for the given condition
           if(id_counter >= 0 && id_counter < instr_fetch_list.size())
           {
                //Decode the type of instruction
                if(instr_fetch_type.get(id_counter) == 1)
                {
                    Rtype r = (Rtype)(instr_fetch_list.get(id_counter));
                    id_counter++;
                    r.Decode_Phase();
                }
                else if(instr_fetch_type.get(id_counter) == 2)
                {
                    Btype b = (Btype)(instr_fetch_list.get(id_counter));
                    id_counter++;
                    b.Decode_Phase();
                }
                else if(instr_fetch_type.get(id_counter) == 3)
                {
                    Itype i = (Itype)(instr_fetch_list.get(id_counter));
                    id_counter++;
                    i.Decode_Phase();
                }
           }
           else
           {
                id_counter ++;
           }

           //Execute this flag for the given condition
           if(ex_counter >= 0 && ex_counter < instr_fetch_list.size())
           {
                //Decode the type of instruction
                if(instr_fetch_type.get(ex_counter) == 1)
                {
                    Rtype r = (Rtype)(instr_fetch_list.get(ex_counter));
                    ex_counter++;
                    r.Execute_Phase();
                }
                else if(instr_fetch_type.get(ex_counter) == 2)
                {
                    Btype b = (Btype)(instr_fetch_list.get(ex_counter));
                    ex_counter++;
                    b.Execute_Phase();
                }
                else if(instr_fetch_type.get(ex_counter) == 3)
                {
                    Itype i = (Itype)(instr_fetch_list.get(ex_counter));
                    ex_counter++;
                    i.Execute_Phase();
                }
           }
           else
           {
                ex_counter ++;
           }
           //Execute this flag for the given condition
           if(mem_counter >= 0 && mem_counter < instr_fetch_list.size())
           {    
                //Decode the type of instruction
                if(instr_fetch_type.get(mem_counter) == 1)
                {
                    Rtype r = (Rtype)(instr_fetch_list.get(mem_counter));
                    mem_counter++;
                    r.Memory_Phase();
                }
                else if(instr_fetch_type.get(mem_counter) == 2)
                {
                    Btype b = (Btype)(instr_fetch_list.get(mem_counter));
                    mem_counter++;
                    b.Memory_Phase();
                }
                else if(instr_fetch_type.get(mem_counter) == 3)
                {
                    Itype i = (Itype)(instr_fetch_list.get(mem_counter));
                    mem_counter++;
                    i.Memory_Phase();
                }
           }
           else
           {
                mem_counter ++;
           }
           //Execute this flag for the given condition
           if(wb_counter >= 0 && wb_counter < instr_fetch_list.size())
           {
                //Decode the type of instruction
                if(instr_fetch_type.get(wb_counter) == 1)
                {
                    Rtype r = (Rtype)(instr_fetch_list.get(wb_counter));
                    wb_counter++;
                    r.Write_Back_Phase();
                }
                else if(instr_fetch_type.get(wb_counter) == 2)
                {
                    Btype b = (Btype)(instr_fetch_list.get(wb_counter));
                    wb_counter++;
                    b.Write_Back_Phase();
                }
                else if(instr_fetch_type.get(wb_counter) == 3)
                {
                    Itype i = (Itype)(instr_fetch_list.get(wb_counter));
                    wb_counter++;
                    i.Write_Back_Phase();
                }
           }
           else
           {
                wb_counter ++;
           }
        }

        clock_cycle += counter+3;
        System.out.println();
        System.out.println("Register value after working of all instruction");
        for(int i=0;i<Register_Value.size();i++){
            System.out.print(Register_Box.get(i) +":" +Register_Value.get(i)+"  ");
        }
        System.out.println();
        System.out.println();

        System.out.println("Data Memory : ");
        for(int i=t2;i<(t3+t1*4);i+=4){
            System.out.print(Data_Memory[i]+" ");
        }
        System.out.println();
        System.out.println();
        System.out.println("No. of Clock Cycles : "+clock_cycle);
        System.out.println();
    }
}

