import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/**
 * objectCode를 해석해서 메모리에 코드를 올리는 역할
 * 메모리에 올라간 코드를 파싱해서 명령어 목록을 생성 및 출력
 * 현재 실행되는 명령어를 선택해서 표시하기
 *
 * */

public class ObjectCodeLoader {
    private ResourceManager manager;
    ArrayList<ArrayList<Integer>> textMem; //전체 control section에 1바이트씩 파싱한 결과
    ArrayList<Integer> memList; //한 control section에 대한 코드 1바이트씩 파싱 결과

    public ObjectCodeLoader(){
        manager = ResourceManager.getInstance();
        textMem = new ArrayList<>();
    }

    /**로드를 실행한다**/

    public void load(File objectfile) throws FileNotFoundException {
        /** records 에 objectCode 저장*/
        ArrayList<ControlSection> tableList = new ArrayList<>(); //각 contorl section의 이름과 시작주소 + 길이
        ArrayList<ArrayList<String>> lineList = readFile(objectfile);


        //System.out.println("READ OK");
        
        int startDepth = 0; //2번째, 3번째 control section 진입 시 기존 메모리 길이 추가
        
        for(ArrayList<String> lines : lineList) { //각 control section에 대해 반복
            ControlSection table = null;
            for (String line : lines) { //한 control section에 대한 명령어 반복
                if (line.charAt(0) == 'H') {
                    memList = new ArrayList<>();
                    table = handleHead(line, startDepth);
                    tableList.add(table);
                }
                else if (line.charAt(0) == 'D') {
                    handleDefine(line, table);
                }
                else if (line.charAt(0) == 'T') {
                    handleText(line, table.getStartAdderss());
                }
                else if (line.charAt(0) == 'E') {
                    handleEnd(line);
                    textMem.add(memList);

                    //메모리 올리기 변화
                    startDepth += table.getProgramLength(); //길이 추가
                    memList = null;
                }
            }
        }

        startDepth = 0;

        /**이제 R과 M을 돌면서 Control section의 symbol table update**/
        /**RDREC같은 경우 00002B + 001033 해서 저장해야 한다**/
        for(int i=0; i< lineList.size(); ++i){
            ArrayList<String> lines = lineList.get(i); //한 control section 가져오기
            ControlSection table = tableList.get(i); //이때의 control section의 symbol table
            HashMap<String, Integer> extref = new HashMap<>();

            for(String line : lines){
                if(line.charAt(0) == 'R'){
                    handleRef(line, extref, tableList);
                }
                else if(line.charAt(0) == 'M'){
                    handleModify(line, extref, table.getStartAdderss());
                }
                else if(line.charAt(0) == 'E'){
                    startDepth += table.getProgramLength();
                }
            }
        }

        /**프로그램 text code 삽입**/
        manager.setTextMem(textMem);

        /**Resource에 프로그램 이름/시작주소/길이 삽입**/
        int allLength = 0;
        for(int i=0; i<tableList.size(); i++)
            allLength += tableList.get(i).getProgramLength();
        manager.setProgramName(tableList.get(0).getProgramName());
        manager.setStartAddress(tableList.get(0).getStartAdderss());
        manager.setLengthofProgram(allLength);



    }

    private ArrayList<ArrayList<String>> readFile(File objfile) throws FileNotFoundException {
        BufferedReader buffer = new BufferedReader(new FileReader(objfile));
        ArrayList<ArrayList<String>> lineList = new ArrayList<>(); //각 라인을 배열화

        String line; //한 줄의 명령어
        ArrayList<String> lines = new ArrayList<>(); //lines에는 한 control section이 들어간다 

        try {
            while ((line = buffer.readLine()) != null) {
                if(line.length() == 0)
                    continue;
                if(line.charAt(0) == 'H'){
                    lines = new ArrayList<>();
                }
                lines.add(line);
                if(line.charAt(0) == 'E'){
                    lineList.add(lines);
                    lines = null;
                }
            }
            buffer.close();
            return lineList;
        }
        catch(IOException e){
            throw new FileNotFoundException();
        }
    }

    private ControlSection handleHead(String line, int startDepth) throws FileNotFoundException{
        //HCOPY  000000001033(0-6 / 7-12 / 13-18)
        int startAddress = Integer.parseInt(line.substring(7,13), 16) + startDepth; //다 16진수로
        int programLength = Integer.parseInt(line.substring(13,19), 16); //다 16진수로

        ControlSection result = new ControlSection(line.substring(1,7).trim(), startAddress, programLength);
        return result;
    }

    private void handleDefine(String line, ControlSection table) throws FileNotFoundException{
        try{
            int startAddress = table.getStartAdderss();
            line = line.substring(1); //D제거
            while(!line.isEmpty()){
                String name = line.substring(0,6).trim();
                int address = Integer.parseInt(line.substring(6,12), 16) + startAddress;
                table.putSymbol(name, address);//Control section에 이름 + 시작주소 IN
                line = line.substring(12);
            }
        }
        catch (NullPointerException e){
            throw new FileNotFoundException();
        }
        catch (StringIndexOutOfBoundsException e){
            throw new FileNotFoundException();
        }
    }

    private void handleRef(String line, HashMap<String, Integer> extref, ArrayList<ControlSection> tableList){
        line = line.substring(1); //RDREC WRREC

        /**
         System.out.println("tableList 크기 : " + tableList.size());
         for(int i=0; i<tableList.size(); i++){
         System.out.println(tableList.get(i).getProgramName());
         System.out.println(tableList.get(i).getStartAdderss());
         System.out.println(tableList.get(i).getProgramLength());
         }
         **/
        while(!line.isEmpty()){
            String ref = line.substring(0, 6).trim();
            for (ControlSection table : tableList){
                if(table.symbolExist(ref)){ //테이블을 순회하면서 해당 ref에 대한 정보가 있을 때
                    if(!extref.containsKey(ref)){ //extref 테이블에 없으면 추가
                        try {
                            extref.put(ref, table.getAddress(ref));
                            //System.out.println(ref + "의 주소: " + table.getAddress(ref));

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            line = line.substring(6);
        }
    }

    private void handleText(String line, int startAddress) throws FileNotFoundException{
        try{
            //T00001D0D0100030F200A4B1000003E2000 예시
            startAddress += Integer.parseInt(line.substring(1,7), 16); //시작주소 00001D
            int length = Integer.parseInt(line.substring(7,9), 16); //길이 0D
            String dataline = line.substring(9, length * 2 + 9); //액기스 부분 0100030F200A4B1000003E2000

            ArrayList<Integer> memOneList = new ArrayList<>(); //Text 한 줄에 대한 리스트

            //System.out.println("---------------------------------------------");

            for(int i=0; i<dataline.length()/2; ++i) {
                memList.add(Integer.parseInt(dataline.substring(i * 2, i * 2 + 2), 16));
                memOneList.add(Integer.parseInt(dataline.substring(i * 2, i * 2 + 2), 16));
                //System.out.println(Integer.toString(Integer.parseInt(dataline.substring(i * 2, i * 2 + 2), 16),16) + " = " + Integer.parseInt(dataline.substring(i * 2, i * 2 + 2), 16));
            }
            //System.out.println("시작주소 + 이전거 길이 = " + startAddress + " + " + startDepth + " = " + (startAddress+startDepth));
            manager.setMemory(startAddress, memOneList); //현재 주소

            /*
            System.out.println("너의 시작 주소 " + Integer.toString(startAddress,16));
            System.out.println("기존 문장: " + dataline);
            for(int i=0; i<memOneList.size(); i++)
                System.out.print(memOneList.get(i) + "(" + Integer.toString(memOneList.get(i), 16) + ") ");
            System.out.println();
            */


        }

        catch (StringIndexOutOfBoundsException e){
            throw new FileNotFoundException();
        }
    }

    public void handleModify(String line, HashMap<String, Integer> extref, int startAddress){
        /**4C000000 -> 4C[RDREC메모리주소] 로 이동시키기위해 가상메모리에 접근한다.
         * M00000405+RDREC --> 000004(nowAddress) 05(length) +(plma)  RDREC(whatRef)
         * **/
        int nowAddress = startAddress + Integer.parseInt(line.substring(1,7),16);
        int length = Integer.parseInt(line.substring(7,9),16);
        char plma = line.charAt(9);
        String whatRef = line.substring(10,16).trim();
        //System.out.println(">>" + String.format("%06X", nowAddress));

        if(!extref.containsKey(whatRef)){
            System.out.println("없다없다없다없다");
            return;
        }
        //length = 5 (4B100000) 3개 / 6 (454F46) 3개
        //4B1 00000에서 00000을 01033으로 바꾼다.
        Integer[] gasang = manager.getMemory(nowAddress, (length+1)/2);

        int temp = 0;
        for(int i=0; i<gasang.length; ++i){
            temp = temp << 8; //1byte에 해당하는 값  << 옮기고
            temp += gasang[i]; //4B100000 --> 10 00 00의 값이 저장
        }

        int offset = extref.get(whatRef); //더하거나 빼야 할 메모리 위치

        if(plma == '+'){ temp += offset;}
        else if(plma == '-'){ temp -= offset;}

        Integer[] result = new Integer[(length+1)/2];
        for(int i = gasang.length-1; i>=0; --i){
            result[i] = (Integer)(temp & 0xFF);
            temp = temp >> 8;
        }

        ArrayList<Integer> resultArray = new ArrayList<>();
        for(int in : result)
            resultArray.add(in);

        //이렇게 하면 result에 modify가 적용된 address가 IN
        manager.setMemory(nowAddress, resultArray);

        /*
        System.out.print("가상메모리 바꿀 값 : " + gasang[0] + " " + gasang[1] + " " + gasang[2] + "(");
        for(int k=0; k<gasang.length; k++)
            System.out.print(String.format("%02X",gasang[k]) + " ");
        System.out.println(")");

        System.out.println("더하거나 빼야 할 값 : " + offset + " " + String.format("%06X",offset));

        System.out.print("가상메모리 바꾼 후 값 : " + result[0] + " " + result[1] + " " + result[2] + "(");
        for(int k=0; k<result.length; k++)
            System.out.print(String.format("%02X",result[k]) + " ");
        System.out.println(")");
        System.out.println("------------------------------");
        */
    }



    private void handleEnd(String line) throws FileNotFoundException{
        try{
            if(line.length() > 1){ //E 가 아닌 경우
                int startAdderss = Integer.parseInt(line.substring(1,7), 16);
                manager.setRegister(Register.PC, startAdderss); //끝났을 때 pc = 시작주소
            }
        }
        catch (StringIndexOutOfBoundsException e){
            throw new FileNotFoundException();
        }
    }

}

