import java.io.File;
import java.io.FileNotFoundException;

public class SicSimulator {

    private ObjectCodeLoader loader;
    private InstructionExecutor executor;
    private ResourceManager manager;

    public SicSimulator(File instfile) throws FileNotFoundException {
        loader = new ObjectCodeLoader();
        executor = new InstructionExecutor(instfile);
        manager = ResourceManager.getInstance();

    }

    public void load(File file) throws FileNotFoundException{ //코드 줘 = 로더 요청
        loader.load(file);
    }

    public void oneStep(){

        Integer[] gasang = manager.getMemory(0, 10000);

        /**
        for(int i=0; i<10000; i++){
            if(i%10 == 0) {
                System.out.println();
                System.out.print(String.format("%04X", i) + ": ");
            }
            System.out.print(gasang[i] + " ");
        }
        **/

        if(manager.getFinishProgram() == 0) {
            int pc = manager.getPCregister();
            executor.makeOpcode(pc);
        }
    }

    public void allStep(){
        for(int i=0; i<100000; i++){
            oneStep();
            if(manager.getFinishProgram() == 1)
                break;
        }

    }

}
