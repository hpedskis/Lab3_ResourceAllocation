/**
 * Created by hpedskis on 4/4/17.
 */
public class Event {

    public int processNum;
    public int resourceNum;
    public int resourceAmount;
    public int delay;
    public int type; //0= initiate, 1=request, 2= release, 3=terminate


     public Event(int type, int processNum, int resourceNum, int resourceAmount, int delay){
         this.type = type;
         this.processNum = processNum;
         this.resourceNum = resourceNum;
         this.resourceAmount = resourceAmount;
         this.delay = delay;


    }

    public void printInfo(){
         System.out.printf("Process Num: %d, type: %d, resourceNum: %d, resourceAmount: %d, delay: %d", processNum, type, resourceNum, resourceAmount, delay);
         System.out.println("");
    }

}

