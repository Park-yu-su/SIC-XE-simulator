import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


/**
 * 각 control section의 시작주소와 이름
 * 그리고 해당 control section의 symbol들을 저장한다.
 * **/

public class ControlSection {
    HashMap<String, Integer> list; //각 ExtDEF의 symbol의 이름과 lc
    String programName;
    int startAdderss;
    int programLength;

    public ControlSection(String programNameIN, int startAdderssIN, int programLengthIN){
        list = new HashMap<>();
        list.put(programNameIN, startAdderssIN);
        programName = programNameIN;
        startAdderss = startAdderssIN;
        programLength = programLengthIN;

    }

    public boolean symbolExist(String keyname){return list.containsKey(keyname);}

    public void printkeySet(){
        System.out.println(list.keySet());
    }

    //프로그램 이름 반환
    public String getProgramName() {return programName;}

    //프로그램 시작주소 반환
    public int getStartAdderss() {return startAdderss;}

    //프로그램 길이 반환
    public int getProgramLength() {return programLength;}

    //심볼 추가
    public void putSymbol(String symbol, int address) throws FileNotFoundException{
        if(list.containsKey(symbol))
            throw new FileNotFoundException();
        list.put(symbol, address);
    }

    //심볼 이름 줄 시 주소 반환
    public int getAddress(String symbol) throws FileNotFoundException{
        if(list.containsKey(symbol) == false)
            throw  new FileNotFoundException();
        return list.get(symbol);
    }




}
