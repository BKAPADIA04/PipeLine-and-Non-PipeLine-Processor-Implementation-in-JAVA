import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class NonPipeline{

    //Array to store Instructions and Data in the Memory
    static String[] Inst_Memory = new String[1000];
    static int[] Data_Memory = new int[1000];

    //HashMap to store the value of the respective register
    static HashMap<Integer,Integer> Register_Value = new HashMap<>();
    //HashMap to store the function value of the respective instruction
    static HashMap<Integer,String> Function_Value = new HashMap<>();
    //HashMap to map addresses and names of registers
    static HashMap<Integer,String> Register_Box = new HashMap<>();
    //Program Counter
    static int pc = 0;

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
    static class Rtype{
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

        void Execute_Phase(){
            //In Execute Phase, we find out the given instruction from the function field
            //The HashMap Function_Value has the set of function fields mapped to the respective instruction
            //add instruction
            if(Function_Value.get(this.function).compareTo("add") == 0){
                this.rd = this.rs + this.rt;
            }
            //sub instruction
            else if(Function_Value.get(this.function).compareTo("sub") == 0){
                this.rd = this.rs - this.rt;
            }
            //sll instruction
            else if(Function_Value.get(this.function).compareTo("left_shift") == 0){
                int temp = (int)Math.pow(2, this.shamt);
                this.rd = this.rt * temp;
            }
            //mul instruction
            //mul can be used as R and I Type but we used as R-Type though its opcode is not 000000
            else if(Function_Value.get(this.function).compareTo("mul") == 0){
                this.rd = this.rs * this.rt;
            }
            //slt instruction
            else if(Function_Value.get(this.function).compareTo("slt") == 0){
                if(this.rs < this.rt){
                    this.rd = 1;
                }
                else{
                    this.rd = 0;
                }
            }
        }

        //The above mentioned instruction do not fetch any data from the memory
        //So no need of Memory_Phase here
        void Memory_Phase(){
            @SuppressWarnings("unused")
            String temporary_class = "No-use-in-rType";
        }

        //This Phase works in 3 parts:
        // 1) We fetch the rd field from the instruction
        // 2) Convert this from binary to decimal
        // 3) Map the calculated rd value to the rd field in the HashMap Register_Value
        void Write_Back_Phase(){
            Register_Value.put(Binary_to_Decimal_Loop(this.instruction.substring(16, 21)),this.rd);
        }
    }
    
    //Class focusses on implementation of all the I-Type Instructions used in the program
    static class Itype{
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
        void Decode_Phase(){
            this.opcode = this.instruction.substring(0,6);
            this.rs = Register_Value.get(Binary_to_Decimal_Loop(this.instruction.substring(6,11)));
            this.rt = Register_Value.get(Binary_to_Decimal_Loop(this.instruction.substring(11,16)));
            this.immediate = (Binary_to_Decimal(this.instruction.substring(16)));
        }

        void Execute_Phase(){
            //We detect the operation based on the opcode
            //Load or Store or addi needs addition of rs and immediate field in execute phase
            if((this.opcode.compareTo("100011") == 0) || (this.opcode.compareTo("101011") == 0) || (this.opcode.compareTo("001000") == 0)){
                this.temp = this.rs+this.immediate;   
            }
            //beq instruction
            else if(this.opcode.compareTo("000100") == 0){
                if(this.rs == this.rt){
                    pc = pc+this.immediate*4;
                }
            }
            //bne instruction
            else if(this.opcode.compareTo("000101") == 0){
                if(this.rs != this.rt){
                    pc = pc+this.immediate*4;
                }
            }
        }

        void Memory_Phase(){
            //Load instruction
            if(this.opcode.compareTo("100011") == 0){
                this.temp = Data_Memory[this.temp];
            }
            //Store instruction
            else if(this.opcode.compareTo("101011") == 0){
                Data_Memory[this.temp] = this.rt;
            }
        }

        void Write_Back_Phase(){
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

    //Class focusses on implementation of all the J-Type Instructions used in the program
    static class Jtype{
        String instruction;
        String opcode;
        int immediate;

        public Jtype(String k){
            this.instruction = k;
            this.opcode = "0";
            this.immediate = 0;
        }

        //Jump instruction in decode phase decodes the opcode
        //It even decodes immediate field of 26 bits and adds extra bits to make it 32 bit instruction
        void Decode_Phase(){
            this.opcode = this.instruction.substring(0,6);
            String temp = this.instruction.substring(6);
            temp = temp.concat("00");
            temp = "0000".concat(temp);
            this.immediate = Binary_to_Decimal(temp);
        }
        //Updating pc value
        void Execute_Phase(){
            pc = this.immediate-4;
        }
    }

    public static void main(String[] args){
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
                Register_Value.put(i,0);
            }
        }

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

        //Instruction Fetch here pc = pc+4
        for(;pc<k;pc+=4){
            //R-Type or mul instruction
            if( (Inst_Memory[pc].substring(0,6).compareTo("000000") == 0) || (Inst_Memory[pc].substring(0,6).compareTo("011100") == 0)){
                // form a object of R type and do remaining four stage...
                Rtype r_obj = new Rtype(Inst_Memory[pc]);
                r_obj.Decode_Phase();
                r_obj.Execute_Phase();
                r_obj.Memory_Phase();
                r_obj.Write_Back_Phase();
            }
            //J-Type instructions
            else if(Inst_Memory[pc].substring(0, 6).compareTo("000010") == 0){
                Jtype j_obj = new Jtype(Inst_Memory[pc]);
                j_obj.Decode_Phase();
                j_obj.Execute_Phase();
            }
            //I-Type instructions
            else{
                //Form object of I type and remaining four stage....
                Itype i_obj = new Itype(Inst_Memory[pc]);
                i_obj.Decode_Phase();
                i_obj.Execute_Phase();
                i_obj.Memory_Phase();
                i_obj.Write_Back_Phase();
            }
            clock_cycle += 5;
        }
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