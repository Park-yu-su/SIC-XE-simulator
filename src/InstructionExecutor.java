import java.io.*;
import java.util.HashMap;

public class InstructionExecutor {

    ResourceManager manager;
    HashMap<Integer, Inst> insts = new HashMap<>(); //instruction table set (opcode : 정보)

    public InstructionExecutor(File instFile) throws FileNotFoundException {
        manager = ResourceManager.getInstance();
        readFile(instFile);

    }

/** 아래는 해당 명령어 실행하기*/
/** 아래는 파일에서 정보 가져오기 */

    public int getFormat(int opcode){ //opcode로 형식 알려주기
        if(insts.containsKey(opcode)){
            return insts.get(opcode).getFormat();
        }
        return -1;
    }

    public String getName(int opcode){ //opcode로 이름 알려주기
        if(insts.containsKey(opcode)){
            return insts.get(opcode).getName();
        }
        return "-1";
    }

    public void makeOpcode(int pc){
        /**
         * memory에서 현재 pc에 해당하는 위치에 1byte메모리를 가져온 뒤
         * 계산을 통해 명렁어인지 확인한다.
         * 명령어인 경우에는 해당 명령어에 해당하는 Inst class (이름/형식/opcode/operand 개수)를 가져오고
         * 해당 형식에 따라 명령어를 실행한다.
         * 해당 명령어 실행 시 3형식/4형식인 경우 nixbpe를 구하고 p=1인 경우,
         * 뒤의 3자리 - pc값 해서 target address 구하기
         * **/

        Integer[] make = manager.getMemory(pc, 1); //일단 1개 사이즈 가져오기
        //해당 10진수 1byte에서 액기스만 빼기
        String tempString = String.format("%02X", make[0]);
        //System.out.println(make[0] + " " + tempString);
        int changeOp = Character.getNumericValue(tempString.charAt(1)) & 0b1100;
        String opString = tempString.charAt(0) + Integer.toString(changeOp, 16);
        int opcode = Integer.parseInt(opString, 16);

        if(!insts.containsKey(opcode)){ //이 경우는 변수이다.
            System.out.println("존재하지 않는 명령어이다.");
            return;
        }
        Inst inst = insts.get(opcode); //여기에는 현재 주소의 명령어가 있다.

        if(inst.getFormat() == 2){ //2형식 명령어 수행
            doInst2(inst, manager.getMemory(pc, 2));
        }
        else{
            make = manager.getMemory(pc, 2);
            int eBit = make[1] & 0b00010000;
            if(eBit == 0){ //3형식 명령어 수행
                doInst3(inst, manager.getMemory(pc, 3));
            }
            else{ //4형식 명령어 수행
                doInst4(inst, manager.getMemory(pc, 4));
            }
        }

    }

    public Integer[] calNIXBPE(Integer[] memcode){
        /** memcode에는 17 20 37 등이 10진수 형태로 저장
         *  이를 16진수 문자열로 바꾼 후에 각가 7 2 에 해당하는 nixbpe을 구해서 뽑아낸다.
         *  그리고 배열에 저장해 return한다.
        **/
        Integer[] answer = {0,0,0,0,0,0};
        String tempString = String.format("%02X", memcode[0]);
        int tempCheck = Character.getNumericValue(tempString.charAt(1));
        answer[0] = (tempCheck & 0b0010) >> 1;
        answer[1] = tempCheck & 0b0001;
        tempString = String.format("%02X", memcode[1]);
        tempCheck = Character.getNumericValue(tempString.charAt(0));
        answer[2] = (tempCheck & 0b1000) >> 3;
        answer[3] = (tempCheck & 0b0100) >> 2;
        answer[4] = (tempCheck & 0b0010) >> 1;
        answer[5] = tempCheck & 0b0001;

        return answer;
    }


    public void doInst2(Inst inst, Integer[] memcode){
        /**2형식에 해당하는 명령어 실행**/
        Integer[] nixbpe = {0,0,0,0,0,0};

        int nowpc = manager.getPCregister();
        String instruction = "";
        for(Integer su : memcode){
            String tempString = String.format("%02X", su);
            instruction += tempString;
        }


        /**여기에 경우 target address라고 할 수 있는 것은 뒤에 두 글자
            이것을 target address로 보낸 후에 처리한다.
         **/
        int targetAddress = memcode[1];

        DODO(inst, nixbpe, nowpc, targetAddress);
        manager.setTargerAddress(targetAddress);
        manager.setNowInstruction(instruction);
        manager.setNowInstructionName(inst.getName());
    }
    public void doInst3(Inst inst, Integer[] memcode){
        int nowpc = manager.getPCregister();
        String instruction = "";
        for(Integer su : memcode){
            String tempString = String.format("%02X", su);
            instruction += tempString;
        }

        int n, i, x, b, p, e;
        Integer[] nixbpe = calNIXBPE(memcode);
        n = nixbpe[0];
        i = nixbpe[1];
        x = nixbpe[2];
        b = nixbpe[3];
        p = nixbpe[4];
        e = nixbpe[5];

        int targetAddress = 0;
        int sum = 0;


        if(p == 1) { //pc relative
            /** 027 뿐 아니라 FEC 등의 경우도 해결해야 함
             * 방법 : 문자열을 통째로 받은 다음에 문자열의 첫번째가 F이면 음수처리
             *       아니면 양수처리
             * **/
            String sumString = "";
            sumString += String.format("%02X", memcode[1]).charAt(1);
            sumString += String.format("%02X", memcode[2]);

            if (sumString.charAt(0) == 'F') {
                sum = Integer.parseInt(sumString, 16) - 4096;
            } else {
                sum = Integer.parseInt(sumString, 16);
            }
            targetAddress = sum + (nowpc + 3);
        }

        else{ //immediate or etc...
            if(i == 1){
                String sumString = "";
                sumString += String.format("%02X", memcode[1]).charAt(1);
                sumString += String.format("%02X", memcode[2]);
                targetAddress = Integer.parseInt(sumString, 16);
            }
        }

        DODO(inst, nixbpe, nowpc, targetAddress);

        if(instruction.equals("032016")) {
            try {
                manager.endWD("OUT.txt");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!11");
        }

        manager.setTargerAddress(targetAddress);
        manager.setNowInstruction(instruction);
        manager.setNowInstructionName(inst.getName());
        //manager.setRegister(Register.PC, nowpc + 3); <- 명령어에서 사용하자

    }
    public void doInst4(Inst inst, Integer[] memcode){
        int nowpc = manager.getPCregister();
        Integer[] nixbpe = calNIXBPE(memcode);

        String instruction = "";
        for(Integer su : memcode){
            String tempString = String.format("%02X", su);
            instruction += tempString;
        }

        int targetAddress = 0;

        /**4형식의 경우 그냥 뒤의 5자리를 가져와서 그 주소가 target Address로 쓰인다**/
        String sumString = "";
        sumString += String.format("%02X",memcode[1]).charAt(1);
        sumString += String.format("%02X",memcode[2]);
        sumString += String.format("%02X",memcode[3]);

        targetAddress = Integer.parseInt(sumString, 16);


        manager.setTargerAddress(targetAddress);
        manager.setNowInstruction(instruction);
        manager.setNowInstructionName(inst.getName());

        DODO(inst, nixbpe, nowpc, targetAddress);
    }

    public void DODO(Inst inst, Integer[] nixbpe, int nowpc, int targetAddress){
        String name = inst.getName();
        if(name.equals("STL")){ST('L', inst, nixbpe, nowpc, targetAddress);}
        else if(name.equals("JSUB")){JSUB(inst, nixbpe, nowpc, targetAddress);}
        else if(name.equals("LDA")){LD('A', inst, nixbpe, nowpc, targetAddress);}
        else if(name.equals("COMP")){COMP(inst, nixbpe, nowpc, targetAddress);}
        else if(name.equals("JEQ")){JEQ(inst, nixbpe, nowpc, targetAddress);}
        else if(name.equals("J")){J(inst, nixbpe, nowpc, targetAddress);}
        else if(name.equals("STA")){ST('A', inst, nixbpe, nowpc, targetAddress);}
        else if(name.equals("CLEAR")){CLEAR(inst, nixbpe, nowpc, targetAddress);}
        else if(name.equals("LDT")){LD('T', inst, nixbpe, nowpc, targetAddress);}
        else if(name.equals("TD")){TD(inst, nixbpe, nowpc, targetAddress);}
        else if(name.equals("RD")){RD(inst, nixbpe, nowpc, targetAddress);}
        else if(name.equals("COMPR")){COMPR(inst, nixbpe, nowpc, targetAddress);}
        else if(name.equals("STCH")){STCH(inst, nixbpe, nowpc, targetAddress);}
        else if(name.equals("TIXR")){TIXR(inst, nixbpe, nowpc, targetAddress);}
        else if(name.equals("JLT")){JLT(inst, nixbpe, nowpc, targetAddress);}
        else if(name.equals("STX")){ST('X', inst, nixbpe, nowpc, targetAddress);}
        else if(name.equals("RSUB")){RSUB();}
        else if(name.equals("LDCH")){LDCH(inst, nixbpe, nowpc, targetAddress);}
        else if(name.equals("WD")){WD(inst, nixbpe, nowpc, targetAddress);}
    }

    public void J(Inst inst, Integer[] nixbpe, int nowpc, int targetAddress){
        if(nixbpe[0] == 1 && nixbpe[1] == 0){
            Integer[] indirect = manager.getMemory(targetAddress, 3);
            int go = 0;
            go += (indirect[0] & 0xFF) << 16;
            go += (indirect[1] & 0xFF) << 8;
            go += indirect[2];
            //System.out.println("드디어 끝인가??? " + go);
            manager.setPCregister(go);

            if(go == manager.getStartAddress()){
                manager.setFinishProgram();
            }
            return;
        }

        manager.setPCregister(targetAddress);
    }

    /**WD 수우우우우우우저어어어어엉 == COMP 0 무한반복**/
    public void WD(Inst inst, Integer[] nixbpe, int nowpc, int targetAddress){
//        System.out.println("WD 실행");
        try {
            manager.fileWD("OUT.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        manager.setFinishWriteFile();
        manager.setPCregister(nowpc + 3);
    }

    public void LDCH(Inst inst, Integer[] nixbpe, int nowpc, int targetAddress){
//        System.out.println("LDCH 실행");
        int xReg = 0;
        if(nixbpe[2] == 1){
            xReg = manager.getXregister();
        }

        Integer[] data = manager.getMemory(targetAddress+xReg, 1);
        manager.setAregister(data[0]);

        if(nixbpe[5] == 1)
            manager.setPCregister(nowpc + 4);
        else
            manager.setPCregister(nowpc + 3);

    }

    public void COMP(Inst inst, Integer[] nixbpe, int nowpc, int targetAddress){
 //       System.out.println("COMP 실행");
        /**일단은 COMP immediate만**/
        if(nixbpe[1] == 1){
            if(manager.getAregister() == targetAddress){
                manager.setTestOk(0);
            }
            else{
                manager.setTestOk(1);
            }
            manager.setPCregister(nowpc + 3);
            return;
        }

    }

    public void RSUB(){
  //      System.out.println("RSUB 실행");
        manager.setPCregister(manager.getLregister());
    }

    public void JLT(Inst inst, Integer[] nixbpe, int nowpc, int targetAddress){
  //      System.out.println("JLT 실행");
        int testResult = manager.getTestOk();

        if(testResult < 0)
            manager.setPCregister(targetAddress);
        else
            manager.setPCregister(nowpc + 3);

    }

    public void TIXR(Inst inst, Integer[] nixbpe, int nowpc, int targetAddress){
        /**X register 값 + 1**/
  //      System.out.println("TIXR 실행");
        int xReg = manager.getXregister();
        xReg += 1;
        manager.setXregister(xReg);
        /**그리고 비교한다**/
        // 0=A / 1=X / 2=L / 3=B / 4=S / 5=T / 6=F / 8=PC / 9=SW
        String checkReg = String.format("%02X", targetAddress);
        char whatReg = checkReg.charAt(0);
        int regGab = 0;
        if(whatReg == '0') {regGab = manager.getAregister();}
        else if(whatReg == '2') {regGab = manager.getLregister();}
        else if(whatReg == '3') {regGab = manager.getBregister();}
        else if(whatReg == '4') {regGab = manager.getSregister();}
        else if(whatReg == '5') {regGab = manager.getTregister();}
        else if(whatReg == '6') {regGab = manager.getFregister();}

        if(xReg == regGab) {manager.setTestOk(0);}
        if(xReg > regGab) {manager.setTestOk(1);}
        if(xReg < regGab) {manager.setTestOk(-1);}

        manager.setPCregister(nowpc + 2);
    }

    public void STCH(Inst inst, Integer[] nixbpe, int nowpc, int targetAddress){
   //     System.out.println("STCH 실행");
        /**A register에 있는 값을 메모리에 저장한다**/
        if(nixbpe[5] == 1){ //+STCH인 경우
            int xReg = 0;
            if(nixbpe[2] == 1){xReg = manager.getXregister();}

            Integer[] data = manager.getMemory(targetAddress + xReg, 1);
            data[0] = manager.getAregister();
            manager.modifyMemory(data, targetAddress + xReg, 1);

            manager.setPCregister(nowpc + 4);
            return;
        }

    }

    public void COMPR(Inst inst, Integer[] nixbpe, int nowpc, int targetAddress){
    //    System.out.println("COMPR 실행");
        String checkReg = String.format("%02X", targetAddress);
        char checkRe1 = checkReg.charAt(0);
        char checkRe2 = checkReg.charAt(1);
        int reg1 = 0;
        int reg2 = 0;
        //0=A 1=X 2=L 3=B 4=S 5=T 6=F 8=PC 9=SW
        /**일단은 하나만 구현**/
        if(checkRe1 == '0'){reg1 = manager.getAregister();} if(checkRe2 == '0'){reg2 = manager.getAregister();}
        if(checkRe1 == '1'){reg1 = manager.getXregister();} if(checkRe2 == '1'){reg2 = manager.getXregister();}
        if(checkRe1 == '2'){reg1 = manager.getLregister();} if(checkRe2 == '2'){reg2 = manager.getLregister();}
        if(checkRe1 == '3'){reg1 = manager.getBregister();} if(checkRe2 == '3'){reg2 = manager.getBregister();}
        if(checkRe1 == '4'){reg1 = manager.getSregister();} if(checkRe2 == '4'){reg2 = manager.getSregister();}
        if(checkRe1 == '5'){reg1 = manager.getTregister();} if(checkRe2 == '5'){reg2 = manager.getTregister();}

//        System.out.println("COMPR EOF<<" + manager.getAregister() + " VS " + manager.getSregister() + ">>");
//        System.out.println("COMPR 길이<<" + manager.getXregister() + " VS " + manager.getFileStringLength() + ">>");

        /** ----------------파일에 EOF가 안 넣어져요------------------------**/
        if(reg1 == reg2){
    //        System.out.println(">>>>>COMPR : EOF OK!");
            manager.setTestOk(0);
            manager.setFinishReadFile();
        }
        else if(manager.getXregister() >= manager.getFileStringLength()){
      //      System.out.println(">>>>>COMPR : 길이 넘침!");
            manager.setTestOk(0);
        }

        else{
            manager.setTestOk(1);
        }

        manager.setPCregister(nowpc + 2);
    }


    public void RD(Inst inst, Integer[] nixbpe, int nowpc, int targetAddress){
     //   System.out.println("RD 실행");
        String fileString = manager.getFileString();
        char oneByte = fileString.charAt(manager.getXregister()); //X레지스터 기준으로 한글자씩 받는다 가정
        manager.setAregister(oneByte);
        manager.setPCregister(nowpc + 3);
    }

    public void JEQ(Inst inst, Integer[] nixbpe, int nowpc, int targetAddress){
     //   System.out.println("JEQ 실행");
        int check = manager.getTestOk();
        //testok = 0이면 jump
        if(check == 0){
            manager.setPCregister(targetAddress);
        }

        else
            manager.setPCregister(nowpc + 3);
    }

    public void TD(Inst inst, Integer[] nixbpe, int nowpc, int targetAddress){
        //ResourceManager의 testOk변수 이용
  //      System.out.println("TD 실행");

        Integer[] filename = manager.getMemory(targetAddress, 1);
//        System.out.println("파일 이름: " + String.format("%02X",filename));

        try {
            if(String.format("%02X",filename).equals("F1"))
                manager.fileTD("IN.txt");
            else
                manager.fileTD("OUT.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        manager.setPCregister(nowpc + 3);
    }

    public void LD(char regname, Inst inst, Integer[] nixbpe, int nowpc, int targetAddress){
     //   System.out.println("LD" + regname + " 실행");
        //A, T
        Integer[] data = manager.getMemory(targetAddress, 3);
        int result = 0;
        result += (data[0] & 0xFF) << 16;
        result += (data[1] & 0xFF) << 8;
        result += data[2];

        if(regname == 'A'){
            manager.setAregister(result);
            /**파일을 다 읽은 경우 */
            if(manager.getFinishReadFile() == 1 && manager.getFinishWriteFile() == 1)
                manager.setAregister(0);
        }
        else if(regname == 'T'){manager.setTregister(result);}

        if(nixbpe[5] == 0)
            manager.setPCregister(nowpc + 3);
        else
            manager.setPCregister(nowpc + 4);
    }

    public void CLEAR(Inst inst, Integer[] nixbpe, int nowpc, int targetAddress){
   //     System.out.println("CLEAR 실행");
        // 0=A / 1=X / 2=L / 3=B / 4=S / 5=T / 6=F / 8=PC / 9=SW
        String checkReg = String.format("%02X", targetAddress);
        char go = checkReg.charAt(0);
        if(go == '0'){manager.setAregister(0);} //A
        else if(go == '1'){manager.setXregister(0);} //X
        else if(go == '2'){manager.setLregister(0);} //L
        else if(go == '3'){manager.setBregister(0);} //B
        else if(go == '4'){manager.setSregister(0);} //S
        else if(go == '5'){manager.setTregister(0);} //T
        else if(go == '6'){manager.setFregister(0);} //F
        else if(go == '8'){manager.setPCregister(0);} //PC
        else if(go == '9'){manager.setSWregister(0);} //SW

        manager.setPCregister(nowpc + 2);
    }

    public void ST(char regname, Inst inst, Integer[] nixbpe, int nowpc, int targetAddress){
   //     System.out.println("ST" + regname + " 실행");
        /**L레지스터에 있는 값을 가상 메모리의 targer Address가 있는 곳에 저장한다.
         * 비트 연산을 해야 한다.
         * **/
        int reg = 0;

        if(regname == 'A') {reg = manager.getAregister();}
        else if(regname == 'L') {reg = manager.getLregister();}
        else if(regname == 'X') {reg = manager.getXregister();}


        Integer[] data = new Integer[3];
        data[0] = (reg) >> 16;
        data[1] = (reg >> 8) & 0xFF;
        data[2] = (reg) & 0xFF;

//        System.out.println("ST결과 : " + data[0] +" " +  data[1] +" " + data[2]);

        manager.modifyMemory(data, targetAddress, 3); //메모리 수정
        if(nixbpe[5] == 0)
            manager.setPCregister(nowpc + 3);
        else
            manager.setPCregister(nowpc + 4);
    }

    public void JSUB(Inst inst, Integer[] nixbpe, int nowpc, int targetAddress){
    //    System.out.println("JSUB 실행");
        manager.setLregister(nowpc + 4);
        manager.setPCregister(targetAddress);
//        System.out.println("타겟 주소 : " + String.format("%06X", targetAddress));
    }




    private void readFile(File instfile) throws FileNotFoundException {
        try  {
            BufferedReader buffer = new BufferedReader(new FileReader(instfile));
            String line;
            while((line = buffer.readLine()) != null){
                Inst now = new Inst(line);
                //System.out.println(now.name + " " + now.format + " " + now.opcode + " " + now.numOperand);
                insts.put(now.getOpcode(), now);
            }
        }
        catch (IOException e){

        }

    }

}

/**각 inst에 대한 정보가 담긴 class*/
class Inst{
    String name;
    int opcode;
    int format;
    int numOperand; //operand 개수 = RSUB = 0

    public Inst(String line){
        String[] split = line.split(" ");
        /**split[0] = name, split[1] = format, split[2] = opcode, split[3] = operand개수 */
        name = split[0];
        format = Integer.parseInt(split[1]);
        opcode = Integer.parseInt(split[2], 16);
        numOperand = Integer.parseInt(split[3]);
    }

    public int getOpcode() {return opcode;}
    public String getName() {return name;}
    public int getFormat() {return format;}
    public int getNumOperand() {return numOperand;}
}
