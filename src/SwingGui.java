import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.io.File;
import javax.swing.*;
import javax.swing.plaf.FileChooserUI;
import java.awt.*;

public class SwingGui extends JFrame{

    private static SwingGui frame;
    
    JTextField fileNameField; //파일 이름 들어가는
    JTextField programNameField, startAddressField, lengthField; //이름, 시작주소, 길이 (H)
    JTextField ADecField, AHexField, XDecField, XHexField, LDecField, LHexField, PCDecField, PCHexField;
    JTextField SWDecField, BDecField, BHexField, SDecField, SHexField, TDecField, THexField, FDecField, FHexField;
    JTextField firstInstructionField; //end 시 시작주소
    JTextField startAddressinMemField, targetAddressField; //메모리 시작주소, target address
    JTextField doingField; //현재 ojbect code 4C0000

    JTextArea operatorArea; //doingfield 모음
    JTextArea logArea; //doingfield -> LDA

    JButton fileOpenButton; //파일 열어주는 버튼
    JButton onestepButton, allstepButton, endButton; //진행 버튼

    File file;
    SicSimulator sicSimulator;
    ResourceManager manager;

    public void update(){ //모든 내용을 update해서 UI에 적용한다
        programNameField.setText(manager.getProgramName());
        String st = String.format("%06X", manager.getStartAddress());
        String lg = String.format("%06X", manager.getLengthofProgram());
        startAddressField.setText(st);
        lengthField.setText(lg);
        firstInstructionField.setText(st);
        ADecField.setText(Integer.toString(manager.getAregister())); AHexField.setText(String.format("%06X", manager.getAregister()));
        XDecField.setText(Integer.toString(manager.getXregister())); XHexField.setText(String.format("%06X", manager.getXregister()));
        LDecField.setText(Integer.toString(manager.getLregister())); LHexField.setText(String.format("%06X", manager.getLregister()));
        PCDecField.setText(Integer.toString(manager.getPCregister())); PCHexField.setText(String.format("%06X", manager.getPCregister()));
        SWDecField.setText(String.format("%06X", manager.getSWregister()));
        BDecField.setText(Integer.toString(manager.getBregister())); BHexField.setText(String.format("%06X", manager.getBregister()));
        SDecField.setText(Integer.toString(manager.getSregister())); SHexField.setText(String.format("%06X", manager.getSregister()));
        TDecField.setText(Integer.toString(manager.getTregister())); THexField.setText(String.format("%06X", manager.getTregister()));
        FDecField.setText(Integer.toString(manager.getFregister())); FHexField.setText(String.format("%06X", manager.getFregister()));
        doingField.setText(manager.getNowInstruction());
        //if(manager.getNowInstruction() != null && manager.getNowInstruction() != "")
        operatorArea.append(manager.getNowInstruction() + "\n");
        //if(manager.getNowInstructionName() != null && manager.getNowInstructionName() != "")
        logArea.append(manager.getNowInstructionName() + "\n");
        targetAddressField.setText(String.format("%06X", manager.getTargerAddress()));
    }

    public SwingGui() throws FileNotFoundException {

        sicSimulator = new SicSimulator(new File("inst_table.txt"));
        manager = ResourceManager.getInstance();


        //GUI 만들기
        makeUI();
        //이제 핸들러 구현
        fileOpenButtonListener();
        setVisible(true);

    }

    public void fileOpenButtonListener(){
        onestepButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sicSimulator.oneStep();
                update();
                if(manager.getFinishProgram() == 1){
                    logArea.append("프로그램 종료!\n");
                }
            }
        });

        allstepButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                while(manager.getFinishProgram() == 0) {
                    sicSimulator.oneStep();
                    update();
                    if(manager.getFinishProgram() == 1){
                        logArea.append("프로그램 종료!\n");
                        break;
                    }
                }

            }
        });

        endButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                manager.setPCregister(0);
                operatorArea.setText("");
                logArea.setText("");
                manager.setPCregister(0);
                manager.setAregister(0);
                manager.setXregister(0);
                manager.setLregister(0);
                manager.setBregister(0);
                manager.setSregister(0);
                manager.setTregister(0);
                manager.setFregister(0);
                manager.finishReadFile = 0;
                manager.finishProgram = 0;
            }
        });

        fileOpenButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                FileDialog dig = null;
                try {
                    dig = new FileDialog(getInstance(), "Open");
                } catch (FileNotFoundException fileNotFoundException) {
                    //파일 없어
                }
                dig.setVisible(true);

                file = new File(dig.getFile());

                try {
                    fileNameField.setText(dig.getFile());
                    load(new File(dig.getDirectory(), dig.getFile()));
                    //update();
                    programNameField.setText(manager.getProgramName());
                    String st = String.format("%06X", manager.getStartAddress());
                    String lg = String.format("%06X", manager.getLengthofProgram());
                    startAddressField.setText(st);
                    lengthField.setText(lg);
                    firstInstructionField.setText(st);
                    
                } catch (NullPointerException | FileNotFoundException ne) {
                    
                }
            }
        });
    }

    public void load(File file) throws FileNotFoundException { //파일 받아와서 열어주기(SIC simulator 호출)
        sicSimulator.load(file);
    }
    
    public static void main(String[] args) throws FileNotFoundException {

        getInstance();

    }


    public static SwingGui getInstance() throws FileNotFoundException {
        if(frame == null)
            frame = new SwingGui();
        return frame;
    }

    public void makeUI(){
        setTitle("SIC/XE simulator"); //이름 설정
        setSize(500, 650); //크기 설정
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //x 눌러 닫을 때 확실히 닫기
        setLocationRelativeTo(null); //윈도우 실행 위치
        //main 화면(보이는 화면)
        JPanel main = new JPanel(new BorderLayout()); //매인 패널
        getContentPane().add(main);

        //main에 얹을 거 1 (Filename : [ ] [open])
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); //위에 얹을 패널
        main.add(topPanel, BorderLayout.NORTH);

        topPanel.add(new JLabel("<html>"+ "FileName: " + "<html>")); //topPanel에 들어갈 Filename : []
        ///////fileNameField : 파일 이름을 선택할 공간(파일선택창)
        fileNameField = new JTextField(10);
        topPanel.add(fileNameField);
        ///////fileOpenButton : open 기능을 수행할 버튼
        fileOpenButton = new JButton("open");
        topPanel.add(fileOpenButton);

        //1.Header, Record, End Record 등이 얹어질 패널 (grid 세로 비율 / 가로 비율)
        JPanel centerPanel = new JPanel(new GridLayout(1, 2)); //중앙에 얹을 패널
        main.add(centerPanel, BorderLayout.CENTER);

        //1-1.좌측 패널(중심 기준 좌측 - Header Record, Register, Register for XE
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        centerPanel.add(leftPanel);

        //1-1-1.H(Header Record)...
        //BoxLayout 깔고 -> 각 label,textfield를 add한다.
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        headerPanel.setBorder(BorderFactory.createTitledBorder("H(Header Record)"));
        leftPanel.add(headerPanel);
        //1-1-1-1. Program Name
        JPanel programNamePanel = new JPanel(new GridLayout(1,2));;
        programNamePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        headerPanel.add(programNamePanel);
        JLabel programNameLabel = new JLabel("Program Name:");
        programNameField = new JTextField();
        programNameField.setMaximumSize(new Dimension(70, 20)); // JTextField 크기 설정
        programNamePanel.add(programNameLabel);
        programNamePanel.add(programNameField);
        //1-1-1-2. Start Address of Object Program
        JPanel startAddressofObjPanel = new JPanel(new GridLayout(1,2));
        startAddressofObjPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        headerPanel.add(startAddressofObjPanel);
        JLabel startAddressLabel = new JLabel("<html>Start Address of Object Program:</html>");
        startAddressField = new JTextField();
        startAddressField.setMaximumSize(new Dimension(70, 20)); // JTextField 크기 설정
        startAddressofObjPanel.add(startAddressLabel);
        startAddressofObjPanel.add(startAddressField);
        //1-1-1-3. Length of Program
        JPanel lengthofProgramPanel = new JPanel(new GridLayout(1,2));
        lengthofProgramPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        headerPanel.add(lengthofProgramPanel);
        JLabel lengthLabel = new JLabel("<html>Length of Program:</html>");
        lengthField = new JTextField();
        lengthField.setMaximumSize(new Dimension(70, 20)); // JTextField 크기 설정
        lengthofProgramPanel.add(lengthLabel);
        lengthofProgramPanel.add(lengthField);

        //1-1-2. Register (여기에 A,X,L,PC,SW 패널 붙이기)
        //JPanel registerPanel = new JPanel(new GridLayout(5, 3));
        JPanel registerPanel = new JPanel();
        registerPanel.setLayout(new BoxLayout(registerPanel, BoxLayout.Y_AXIS));
        registerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        leftPanel.add(registerPanel);
        registerPanel.setBorder(BorderFactory.createTitledBorder("Register"));
        registerPanel.add(new JLabel("DEC   HEX"));

        //1-1-2-1. A
        JPanel APanel = new JPanel(new FlowLayout());
        APanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        registerPanel.add(APanel);
        APanel.add(new JLabel("A (#0) "));
        ADecField = new JTextField();
        AHexField = new JTextField();
        ADecField.setPreferredSize(new Dimension(70, 20));
        AHexField.setPreferredSize(new Dimension(70, 20));
        APanel.add(ADecField);
        APanel.add(AHexField);
        //1-1-2-2. X
        JPanel XPanel = new JPanel(new FlowLayout());
        XPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        registerPanel.add(XPanel);
        XPanel.add(new JLabel("X (#1) "), BorderLayout.WEST);
        XDecField = new JTextField();
        XHexField = new JTextField();
        XDecField.setPreferredSize(new Dimension(70, 20));
        XHexField.setPreferredSize(new Dimension(70, 20));
        XPanel.add(XDecField);
        XPanel.add(XHexField);
        //1-1-2-3. L
        JPanel LPanel = new JPanel(new FlowLayout());
        LPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        registerPanel.add(LPanel);
        LPanel.add(new JLabel("L (#2) "), BorderLayout.WEST);
        LDecField = new JTextField();
        LHexField = new JTextField();
        LDecField.setPreferredSize(new Dimension(70, 20));
        LHexField.setPreferredSize(new Dimension(70, 20));
        LPanel.add(LDecField);
        LPanel.add(LHexField);
        //1-1-2-4. PC
        JPanel PCPanel = new JPanel(new FlowLayout());
        PCPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        registerPanel.add(PCPanel);
        PCPanel.add(new JLabel("PC (#8) "), BorderLayout.WEST);
        PCDecField = new JTextField();
        PCHexField = new JTextField();
        PCDecField.setPreferredSize(new Dimension(70, 20));
        PCHexField.setPreferredSize(new Dimension(70, 20));
        PCPanel.add(PCDecField);
        PCPanel.add(PCHexField);
        //1-1-2-5. SW
        JPanel SWPanel = new JPanel(new FlowLayout());
        SWPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        registerPanel.add(SWPanel);
        SWPanel.add(new JLabel("SW (#9) "), BorderLayout.WEST);
        SWDecField = new JTextField();
        SWDecField.setPreferredSize(new Dimension(140, 20));
        SWPanel.add(SWDecField);

        //1-1-3. Register(for XE)
        JPanel registerXePanel = new JPanel();
        registerXePanel.setLayout(new BoxLayout(registerXePanel, BoxLayout.Y_AXIS));
        registerXePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        leftPanel.add(registerXePanel);
        registerXePanel.setBorder(BorderFactory.createTitledBorder("Register(for XE)"));
        registerXePanel.add(new JLabel("DEC   HEX"));

        //1-1-3-1. B
        JPanel BPanel = new JPanel(new FlowLayout());
        BPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        registerXePanel.add(BPanel);
        BPanel.add(new JLabel("B (#3) "));
        BDecField = new JTextField();
        BHexField = new JTextField();
        BDecField.setPreferredSize(new Dimension(70, 20));
        BHexField.setPreferredSize(new Dimension(70, 20));
        BPanel.add(BDecField);
        BPanel.add(BHexField);
        //1-1-3-2. S
        JPanel SPanel = new JPanel(new FlowLayout());
        SPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        registerXePanel.add(SPanel);
        SPanel.add(new JLabel("S (#4) "));
        SDecField = new JTextField();
        SHexField = new JTextField();
        SDecField.setPreferredSize(new Dimension(70, 20));
        SHexField.setPreferredSize(new Dimension(70, 20));
        SPanel.add(SDecField);
        SPanel.add(SHexField);
        //1-1-3-3. T
        JPanel TPanel = new JPanel(new FlowLayout());
        TPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        registerXePanel.add(TPanel);
        TPanel.add(new JLabel("T (#5) "));
        TDecField = new JTextField();
        THexField = new JTextField();
        TDecField.setPreferredSize(new Dimension(70, 20));
        THexField.setPreferredSize(new Dimension(70, 20));
        TPanel.add(TDecField);
        TPanel.add(THexField);
        //1-1-3-4. F
        JPanel FPanel = new JPanel(new FlowLayout());
        FPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        registerXePanel.add(FPanel);
        FPanel.add(new JLabel("F (#6) "));
        FDecField = new JTextField();
        FHexField = new JTextField();
        FDecField.setPreferredSize(new Dimension(70, 20));
        FHexField.setPreferredSize(new Dimension(70, 20));
        FPanel.add(FDecField);
        FPanel.add(FHexField);


        //1-2.우측 패널
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        centerPanel.add(rightPanel);

        //1-2-1. E(End Record)
        JPanel endRecordPanel = new JPanel();
        endRecordPanel.setLayout(new BoxLayout(endRecordPanel, BoxLayout.Y_AXIS));
        endRecordPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        endRecordPanel.setBorder(BorderFactory.createTitledBorder("E(End Record)"));
        rightPanel.add(endRecordPanel);
        //1-2-1-1. Address of first instruction
        JPanel firstInstructionPanel = new JPanel(new GridLayout(1,2));;
        firstInstructionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        endRecordPanel.add(firstInstructionPanel);
        JLabel firstInstructionLabel = new JLabel("<html>Address of First instruction:<html>");
        firstInstructionField = new JTextField();
        firstInstructionField.setMaximumSize(new Dimension(70, 20)); // JTextField 크기 설정
        firstInstructionPanel.add(firstInstructionLabel);
        firstInstructionPanel.add(firstInstructionField);

        //1-2-2. Start Address in Memory
/*        JPanel startAddressinMemPanel = new JPanel();
        startAddressinMemPanel.setLayout(new BoxLayout(startAddressinMemPanel, BoxLayout.Y_AXIS));
        rightPanel.add(startAddressinMemPanel);
        startAddressinMemPanel.add(new JLabel("Start Address in Memory"));
        startAddressinMemField = new JTextField();
        startAddressinMemField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        startAddressinMemPanel.add(startAddressinMemField);
*/
        //1-2-3. target Address 표현
        JPanel targetAddressPanel = new JPanel();
        targetAddressPanel.setLayout(new BoxLayout(targetAddressPanel, BoxLayout.Y_AXIS));
        rightPanel.add(targetAddressPanel);
        targetAddressPanel.add(new JLabel("Target Address: "));
        targetAddressField = new JTextField();
        targetAddressField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        targetAddressPanel.add(targetAddressField);

        //1-2-4. Instruction(명령어 가상 메모리 보이기)
        JPanel instructionPanel = new JPanel(new GridLayout(1,2));
        instructionPanel.setBorder(BorderFactory.createTitledBorder("Instructions"));
        rightPanel.add(instructionPanel);

        //1-2-4-1. 파싱된 명령어들이 적히는 곳
        JPanel operatorPanel = new JPanel(new BorderLayout());
        instructionPanel.add(operatorPanel);
        operatorArea = new JTextArea(10, 5);
        operatorPanel.add(new JScrollPane(operatorArea),BorderLayout.CENTER);

        //1-2-4-2. 현재 명령어 표시 및 버튼들
        JPanel doingPanel = new JPanel();
        doingPanel.setLayout(new BoxLayout(doingPanel, BoxLayout.Y_AXIS));
        instructionPanel.add(doingPanel);
        doingPanel.add(Box.createVerticalGlue());

        doingPanel.add(new JLabel("Now Instruction"));
        doingField = new JTextField();
        doingField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        doingPanel.add(doingField);

        onestepButton = new JButton("실행 (1step)");
        onestepButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        allstepButton = new JButton("실행 (all)");
        allstepButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        endButton = new JButton("종료");
        endButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        doingPanel.add(Box.createRigidArea(new Dimension(0, 40)));
        doingPanel.add(onestepButton);
        doingPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        doingPanel.add(allstepButton);
        doingPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        doingPanel.add(endButton);
        doingPanel.add(Box.createVerticalGlue());

        //2. Log(명령어 수행 관련)
        JPanel logPanel = new JPanel(new BorderLayout());
        main.add(logPanel, BorderLayout.SOUTH);
        logPanel.setBorder(BorderFactory.createTitledBorder("Log(명령어 수행 관련): "));
        logArea = new JTextArea(6, 50); //여러 줄 입력
        logPanel.add(new JScrollPane(logArea),BorderLayout.CENTER); //스크롤바
    }
    
    
}
