import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.FileWriter;

enum Register{
    A, X, L, PC, SW, B, S, T, F
}

public class ResourceManager {
    private static ResourceManager manager;

    private ArrayList<ControlSection> symbolTableList; //각 control section의 symbol
    int register[] = {0,0,0,0,0,0,0,0,0,0}; //각 A,X,L .. 의 레지스터 값이 들어간 배열
    Integer[] gasangMemory;
    int gasangPointer; //가상 메모리 움직이는 포인터(X레지스터)
    int finishProgram = 0; //프로그램이 종료되었는지를 판단하는 변수

    int finishReadFile = 0; //파일 읽는 작업 끝났는지 판단하는 변수
    int finishWriteFile = 0; //파일 쓰기 작업 끝났는지 판단하는 변수

    //HEADER RECORD을 읽고 나온 결과물(H)
    String programName;
    int startAddress;
    int lengthofProgram;

    //계산할 때 필요
    int targerAddress; //계산할 때의 targer address
    String nowInstruction; //계산할 현재 instruction
    String nowInstructionName; //계산할 현재 instruction 명령어 번역

    //파일에서 읽은 단어 저장
    int testOk = 0;
    String fileString = "";

    //수정 필요(아래 2개는 사용 안함)
    ArrayList<ArrayList<Integer>> textMem;
    int controlMenNow = 0; //textMem.get(!) - 어느 control section
    int textMenNow = 0; //textMem.get(!).get(!) - 어느 번째 코드

    public String getFileString(){return fileString;}
    public int getFileStringLength(){return fileString.length();}

    public void initWriteFile() throws IOException{
        File file = new File("OUT.txt");
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write("");
        writer.close();
    }

    public void fileWD(String filename) throws IOException {
        File writeFile = new File(filename);
        BufferedWriter writer = new BufferedWriter(new FileWriter(writeFile, true));
        String writeData = "";
        char writeChar = (char)register[0];
        writeData += writeChar;

        writer.write(writeData);
        writer.close();
    }

    public void endWD(String filename) throws IOException{
        File writeFile = new File(filename);

        BufferedWriter writer = new BufferedWriter(new FileWriter(writeFile, true));
        String writeData = "EOF";
        for(int i=0; i<3; i++){
            String writeme = "";
            writeme += (char)writeData.charAt(i);
            writer.write(writeme);
        }

        writer.close();

    }

    public void fileTD(String filename) throws IOException{
        //파일이 있는지 확인하고 파일이 있으면 고
        File test = new File(filename);

        //JEQ에서 비교하니까 주의
        if(test.exists()) {
            testOk = 1;
        }
        else {
            testOk = 0;
            System.out.println(filename + " 파일 없습니다");
            return;
        }

        /**TD 할때마다 중복으로 실행을 막기*/
        if(fileString.equals("")) {
            StringBuilder bd = new StringBuilder();
            BufferedReader buffer = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = buffer.readLine()) != null) {
                bd.append(line).append(System.lineSeparator());
            }
            fileString = bd.toString();
            fileString += (char)0X00;
//            System.out.println(">>>>>파일 읽기 완료: " + fileString);
        }
    }

    public void setFinishWriteFile(){
        finishWriteFile = 1;
    }
    public int getFinishWriteFile(){
        return finishWriteFile;
    }

    public void setFinishReadFile(){
        finishReadFile = 1;
    }
    public int getFinishReadFile(){
        return finishReadFile;
    }

    public void setTestOk(int in){testOk = in;}
    public int getTestOk(){return testOk;}

    public void setFinishProgram(){finishProgram = 1;}
    public int getFinishProgram(){return finishProgram;}

    public void setTargerAddress(int in){targerAddress = in;}
    public void setNowInstruction(String in){nowInstruction = in;}
    public void setNowInstructionName(String in){nowInstructionName = in;}

    public int getTargerAddress(){return targerAddress;}
    public String getNowInstruction(){return nowInstruction;}
    public String getNowInstructionName(){return nowInstructionName;}

    public ResourceManager(){ //가상 메모리 10000byte 초기화
        gasangMemory = new Integer[10000];
        for(int i=0; i<10000; i++)
            gasangMemory[i] = 0;

        try {
            initWriteFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setRegister(Register register, int value){
        setRegister(registerToInt(register), value);
    }

    public void setRegister(int registernumber, int value){
        if(0<=registernumber && registernumber<=8)
            register[registernumber] = value & 0xFFFFFF;
    }


    public void setMemory(int startAddress, ArrayList<Integer> memList){
        /** startAddress = 현재 text record의 시작주소
         *  startDepth = 새 control section 대비 시작주소 ++
         *  memList = startAdderss부터 1바이트씩 가지고 있는 메모리 값
         * **/

        //System.out.println("메모리 " + (startAddress + startDepth) + "부터 " + (startAddress+startDepth+memList.size() + "까지"));
        for(int i = 0; i< memList.size(); i++){
            gasangMemory[i+startAddress] = memList.get(i);
            //System.out.println(i+startAddress + "위치의 값 : " + gasangMemory[i+startAddress+startDepth]);
        }
        /**gasangMemory에 명령어 올리는데 성공*
         * 이제 program의 시작주소를 받아서 해당 위치에서 1byte읽고
         * bit연산으로 파싱해서 명령어 찾고
         * 해당 명령어가 몇 형식인가에 따라 몇 바이트 읽은 후
         * 해당 명령어 처리
         * */

    }

    public void modifyMemory(Integer[] modify, int startAddress, int size){ //가상 메모리를 수정할 때 사용
        for(int i=0; i<size; i++)
            gasangMemory[startAddress + i] = modify[i];
    }

    public Integer[] getMemory(int address, int size){ //start 부터 size 크기에 해당하는 바이트 return
        Integer[] temp = new Integer[size];
        for(int i=0; i<size; ++i){
            temp[i] = gasangMemory[address + i];
        }

        return temp;
    }

    public static ResourceManager getInstance(){
        if(manager == null)
            manager = new ResourceManager();
        return manager;
    }

    public int registerToInt(Register reg){
        if(reg == Register.A) return 0;
        if(reg == Register.X) return 1;
        if(reg == Register.L) return 2;
        if(reg == Register.PC) return 3;
        if(reg == Register.SW) return 4;
        if(reg == Register.B) return 5;
        if(reg == Register.S) return 6;
        if(reg == Register.T) return 7;
        if(reg == Register.F) return 8;
        return 9; //그 외의 값 handle
    }


    public ArrayList<Integer> getTextMem(int i) {return textMem.get(i);}
    public int getControlMenNow() {return controlMenNow;}
    public int getTextMenNow() {return textMenNow;}
    public String getProgramName(){return programName;}
    public int getStartAddress(){return startAddress;}
    public int getLengthofProgram(){return lengthofProgram;}
    public int getAregister(){return register[0];}
    public int getXregister(){return register[1];}
    public int getLregister(){return register[2];}
    public int getPCregister(){return register[3];}
    public int getSWregister(){return register[4];}
    public int getBregister(){return register[5];}
    public int getSregister(){return register[6];}
    public int getTregister(){return register[7];}
    public int getFregister(){return register[8];}

    public void setTextMem(ArrayList<ArrayList<Integer>> in) {textMem = in;}
    public void setControlMenNow(int in) {controlMenNow = in;}
    public void setTextMenNow(int in) {textMenNow = in;}
    public void setProgramName(String name){programName = name;}

    public void setStartAddress(int name){
        startAddress = name;
        gasangPointer = startAddress;
    }


    public void setLengthofProgram(int name){lengthofProgram = name;}
    public void setAregister(int in) {register[0] = in;}
    public void setXregister(int in) {register[1] = in;}
    public void setLregister(int in) {register[2] = in;}
    public void setPCregister(int in) {register[3] = in;}
    public void setSWregister(int in) {register[4] = in;}
    public void setBregister(int in) {register[5] = in;}
    public void setSregister(int in) {register[6] = in;}
    public void setTregister(int in) {register[7] = in;}
    public void setFregister(int in) {register[8] = in;}

}


