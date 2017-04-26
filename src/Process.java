/**
 * Created by Hannah Pedersen on 4/4/17.
 */
public class Process {
    public boolean isDone = false;
    public int processNumber;
    public int beginTime = 0;
    public int currBlockTime = 0;
    public boolean computing = false;
    public int currEventType = 0;

    public int timeTaken;
    public int waitingTime =0;
    public int percentageWait;
    public boolean lastTaskGranted = false;
    public boolean inQueue = false;


    public Process(int processNum){
        this.processNumber = processNum;
    }

    public void printFinishingInfo(){
        System.out.print("Task " + (this.processNumber+1) + ": ");
        if(waitingTime == -1){
            System.out.println("aborted");
        }else{
            int percentageWaitTime = (waitingTime  * 100) / timeTaken;
            System.out.println("time taken: " + timeTaken + ", wait time: " + waitingTime + ", percentage of time waiting: " + percentageWaitTime + "%");
        }


    }
}
