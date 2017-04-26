import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Hannah Pedersen on 4/4/17.
 */
public class resourceAllocation {

       /*/
       solveResourceAllocation is a function that handles both bankers and optomistic manager by going through each
       process and calling subsequent functions. the bankersAlgo boolean flag controls whether the isSafe() or the canDo()
       function is called. A detailed description of the protocols for both is below.

        GAMEPLAN:: (for Bankers)
        1. make sure that each initiate doesn't exceed the bankers resourcesAvailable[at that resource]
        2. if the process does initiate more than the banker has, print message and abort that process.
        3. check the readyQueue for any proccesses that can be finished before the other processes
        4. go through each process and check if the process can be granted by checking if it's safe
               -to be safe, make copies and grant the request. If the task could finish, safe. else, not safe.
        5. if the event is safe, grant it, delete the event
        6. if the event is not safe, don't grant it.

            GAME PLAN:: (for Optimistic)
            ** check the readyQueue for any events that can be finished before going through event list
            1. go through first event of each arrayList in events list
            2. if that event can't be granted, add to wait list
            3. if it can be granted, do it, and delete that event.
            4. if all processes are in queue and none can be taken out, deadlock.
            5. if a process has a "delay", set process.delay to delay time -1 (since 1 cycle will
                technically have already been completed)
          */

    public void solveResourceAllocation(ArrayList<ArrayList<Event>> events, int[] resourcesAvailable, int[][] currentHasOfEachProcess, Process[] processes, int[][] initialClaimsOfEachProcess, boolean bankersAlgo) throws InterruptedException {
        int cycle = 0;
        Queue<Process> waitingQueue = new ConcurrentLinkedQueue<Process>();

        //if this is bankers algorithm, check for illegal initial claims
        if (bankersAlgo) {
            for (int p = 0; p < processes.length; p++) {
                for (int r = 0; r < resourcesAvailable.length; r++) {
                    if (initialClaimsOfEachProcess[p][r] > resourcesAvailable[r]) {
                        System.out.println("ERROR: process is claiming more of resource " + r + " than there is available");
                        singleProcessAbort(processes[p], currentHasOfEachProcess, resourcesAvailable);
                    }
                }
            }
        }

        while (!allProcessesDone(processes)) {
            //System.out.println("During cycle " + cycle + " to " + (cycle+1));

            //create a temp array which will hold all the resources being returned during this cycle
            //this is so resources released by one process won't be available for another process during the same cycle
            int[] resourcesToBeReturned = new int[resourcesAvailable.length];

            //if every process is in the waiting queue and no requests can be grated, abort a process.
            //this block should never be reahced in bankersAlgo flag is true
            if (!bankersAlgo) {
                if (allProccessesInQueue(waitingQueue, processes)) {
                    for (Process p : processes) {
                        if (!canDoEvent(events.get(p.processNumber).get(0), p, resourcesAvailable, currentHasOfEachProcess, initialClaimsOfEachProcess, bankersAlgo)) {
                            abortProcess(waitingQueue, events, resourcesAvailable, currentHasOfEachProcess, cycle);
                        }
                    }

                }

            }

            //check all processes for a possible event to be granted or not granted BEFORE looking at the other processes
            for (Process p : waitingQueue) {
                if (p.isDone) {
                    continue;
                } else {
                    checkProcess(p, waitingQueue, events, resourcesAvailable, currentHasOfEachProcess, cycle, resourcesToBeReturned, initialClaimsOfEachProcess, bankersAlgo);

                }

            }

            for (Process p : processes) {
                //skip any process that was checked in the waiting queue or that is already done.
                if (p.inQueue == true || p.isDone) {
                    //if p was in queue but just completed a task, remove from the waiting queue for the next cycle
                    if (p.lastTaskGranted == true) {
                        p.inQueue = false;
                    }
                    continue;
                } else {
                    checkProcess(p, waitingQueue, events, resourcesAvailable, currentHasOfEachProcess, cycle, resourcesToBeReturned, initialClaimsOfEachProcess, bankersAlgo);
                }
            }

            cycle++;

            //for every process in the waiting queue, increase waiting time
            //skip if it's in the waitingQueue due to delay
            for (Process p : waitingQueue) {
                if (p.lastTaskGranted == false && (!p.computing) && (!p.isDone)) {
                    p.waitingTime++;
                }
            }

            //reset the resources to be returned for the next cycle
            for (int r = 0; r < resourcesAvailable.length; r++) {
                resourcesAvailable[r] += resourcesToBeReturned[r];
                resourcesToBeReturned[r] = 0;
            }

        }

        //print all ending information after all processes have completed
        for (Process p : processes) {
            p.printFinishingInfo();
        }
        int total = 0;
        int waitTime = 0;
        for (Process p : processes) {
            if (p.waitingTime == -1) {
                continue;
                //to signify an aborted process, wait time was set to -1. for correct total wait, skip this.
            } else {
                total += p.timeTaken;
                waitTime += p.waitingTime;
            }
        }
        int percentageWaitTime = (waitTime * 100) / total;
        System.out.println("Total time: " + total + ", total wait time: " + waitTime + " average percentage of wait time: " + percentageWaitTime + "%");


    }


    /*
    this function is used in both bankers and optimistic algorithm. One event for each process is selected, passed in,
    and handled according to what event it is.
     */
    public void checkProcess(Process p, Queue<Process> waitingQueue, ArrayList<ArrayList<Event>> events, int[] resourcesAvailable, int[][] currentHasOfEachProcess, int cycle, int[] resourcesToBeReturned, int[][] initialClaimsOfEachProcess, boolean bankersAlgo) {
        Process currProcess = p;
        Event eventToDo = events.get(p.processNumber).get(0);
        //System.out.print("Process " + (currProcess.processNumber+1) + " event " + eventToDo.type + " ");

        int resourceForEvent = eventToDo.resourceNum;
        int processNumForEvent = eventToDo.processNum;

        /*/
        If the event has a delay... (eventToDo.delay>0) than set the delay time.
        UNLESS:
            the current process is already computing (otherwise the delay time will get re-set)
            the event hasn't changed from the previous event (as in the delay has been served, but it hasn't been granted)

        if the current block time is 0, there is no delay or the delay time has completed. Set the currentEvent
        so if the event cannot be granted, the delay time isn't re-served.
         */
        if (eventToDo.delay > 0 && (!currProcess.computing) && (eventToDo.type != currProcess.currEventType)) {
            currProcess.currBlockTime = eventToDo.delay;
            currProcess.computing = true;
            currProcess.currEventType = eventToDo.type;
        } else if (currProcess.currBlockTime == 0) {
            currProcess.computing = false;
            currProcess.currEventType = eventToDo.type;
        }

        //Terminate event doesn't take a cycle, so mark process as done and log correct cycle finish time
        if (eventToDo.type == 3 && (!currProcess.computing)) {
            currProcess.isDone = true;
            currProcess.timeTaken = cycle;
            return;
        }

        if (canDoEvent(eventToDo, currProcess, resourcesAvailable, currentHasOfEachProcess, initialClaimsOfEachProcess, bankersAlgo)) {
           //if the event was granted, remove from waiting queue
            currProcess.lastTaskGranted = true;
            if (waitingQueue.contains(currProcess)) {
                waitingQueue.remove(currProcess);

            }

            if (eventToDo.type == 2) {
                resourcesToBeReturned[resourceForEvent] += eventToDo.resourceAmount;
                currentHasOfEachProcess[processNumForEvent][resourceForEvent] -= eventToDo.resourceAmount;
            }
            if (eventToDo.type == 1) {
                resourcesAvailable[resourceForEvent] -= eventToDo.resourceAmount;
                currentHasOfEachProcess[processNumForEvent][resourceForEvent] += eventToDo.resourceAmount;
            }

            //delete the event that was just completed
            events.get(currProcess.processNumber).remove(0);

        } else {
            currProcess.lastTaskGranted = false;
            //check if process is already in queue so it's not added again.
            if (waitingQueue.contains(currProcess)) {
                return;
            } else {
                currProcess.inQueue = true;
                waitingQueue.add(currProcess);
                return;
            }

        }
    }

    /*
    finds the smalles process in the waiting queue to abort if there's a deadlock in optimistic manager.
     */
    public void abortProcess(Queue<Process> waitingQueue, ArrayList<ArrayList<Event>> events, int[] resourcesAvailable, int[][] currentHasOfEachProcess, int cycle) {

        int smallestProcessNum = Integer.MAX_VALUE;
        Process processToBeDeleted = null;
        for (Process p : waitingQueue) {
            if (p.processNumber < smallestProcessNum) {
                smallestProcessNum = p.processNumber;
                processToBeDeleted = p;
            }
        }

        waitingQueue.remove(processToBeDeleted);
        processToBeDeleted.isDone = true;
        processToBeDeleted.timeTaken = cycle;

        //-1 signifies that the process was aborted
        processToBeDeleted.waitingTime = -1;

        for (int r = 0; r < currentHasOfEachProcess[smallestProcessNum].length; r++) {
            resourcesAvailable[r] += currentHasOfEachProcess[smallestProcessNum][r];
            currentHasOfEachProcess[smallestProcessNum][r] = 0;
        }

    }

    /*/

    if the bankersAlgo flag is true, then the function isSafe will be called.

    -to be able to do an event, there must not be a block, and the request must be
    less than the amount that the manager currently has

    Three types of events:
        -initiate, which can always be done (optimistic doesn't care about claims and bankers has already checked for errors)
        -request, where the amount requested cannot exceed what banker currently has
        -release, which can always be done
     */
    public boolean canDoEvent(Event eventToDo, Process currProcess, int[] resourcesAvailable, int[][] currentHasOfEachProcess, int[][] initialClaims, boolean bankersAlgo) {
        int resourceForEvent = eventToDo.resourceNum;

        //if the process is delayed, decrament block time, but return false (since process is still computing)
        if (currProcess.currBlockTime > 0) {
            currProcess.computing = true;
            currProcess.currBlockTime--;
            return false;
        }

        if (eventToDo.type == 0) {
            return true;
        } else if (eventToDo.type == 1) {
            if (bankersAlgo) {
                return isSafe(eventToDo, currProcess, resourcesAvailable, currentHasOfEachProcess, initialClaims);
            }
            //for optimistic manager:
            if (resourcesAvailable[resourceForEvent] >= eventToDo.resourceAmount) {
                return true;
            } else {
                return false;
            }

        } else {
            return true;
        }
    }

        /*
    simply checks if all processes are in queue (signifies deadlock)
        */
    public boolean allProccessesInQueue(Queue<Process> waitingQueue, Process[] processes) {
        for (Process p : processes) {
            if (p.isDone) {
                continue;
            }
            if (!waitingQueue.contains(p) || p.computing) {
                return false;

            }
        }
        return true;
    }
    /*
    simply checks if all processes are done
     */
    public boolean allProcessesDone(Process[] processes) {
        for (Process p : processes) {
            if (p.isDone == false) {
                return false;
            }
        }
        return true;
    }

    /*/
    check for safe state used by bankers algorithm
     */
    public boolean isSafe(Event eventToDo, Process currProcess, int[] resourcesAvailable, int[][] currentHasOfEachProcess, int[][] initialClaimsOfEachProcess) {
        int resourceNum = eventToDo.resourceNum;
        int requestedAmount = eventToDo.resourceAmount;
        int processNum = currProcess.processNumber;

        int totalNumProccesses = currentHasOfEachProcess.length;
        int totalNumResources = resourcesAvailable.length;


        int possibleGranting = resourcesAvailable[resourceNum];

        //if the process is requesting more than the banker has, it's not safe.
        if (possibleGranting < requestedAmount) {
            return false;
        }

        //calculate how many of each resources each process still needs at this point
        int[][] stillNeeds = new int[totalNumProccesses][totalNumResources];
        for (int p = 0; p < stillNeeds.length; p++) {
            for (int r = 0; r < stillNeeds[0].length; r++) {
                stillNeeds[p][r] = initialClaimsOfEachProcess[p][r] - currentHasOfEachProcess[p][r];
            }
        }


        //create boolean array to mark off if each process can finish and initialize to false
        boolean[] canFinish = new boolean[totalNumProccesses];
        for (int i = 0; i < canFinish.length; i++) {
            canFinish[i] = false;
        }

        //create temporary copy of the available resources
        int[] tempAvailable = new int[resourcesAvailable.length];
        for (int j = 0; j < resourcesAvailable.length; j++) {
            tempAvailable[j] = resourcesAvailable[j];
        }

        //"pretend" to allocate the request by decramenting banker's available amount and decramenting the amount the process still needs
        tempAvailable[resourceNum] -= requestedAmount;
        stillNeeds[processNum][resourceNum] -= requestedAmount;
        currentHasOfEachProcess[processNum][resourceNum] += requestedAmount;

        if (currentHasOfEachProcess[processNum][resourceNum] > initialClaimsOfEachProcess[processNum][resourceNum]) {
            System.out.println("process " + (processNum + 1) + "has claimed more than initial claim. aborting");
            currentHasOfEachProcess[processNum][resourceNum] -= requestedAmount;
            singleProcessAbort(currProcess, currentHasOfEachProcess, resourcesAvailable);
            return false;
        }

        for (int i = 0; i < totalNumProccesses; i++) {
            //try to find any ordering that will complete (hence the double for loops)
            for (int j = 0; j < totalNumProccesses; j++) {
                if (!canFinish[j]) {
                    boolean currCheck = true;

                    for (int k = 0; k < totalNumResources; k++) {
                        if (stillNeeds[j][k] > tempAvailable[k]) {
                            currCheck = false;
                        }
                    }
                    if (currCheck) {
                        canFinish[j] = true;
                        //pretend to finish this process and give back resources
                        for (int r = 0; r < totalNumResources; r++) {
                            tempAvailable[r] += currentHasOfEachProcess[j][r];
                        }
                    }
                }
            }

        }

        //put currentHasOfEachProcess back to how it was before
        currentHasOfEachProcess[processNum][resourceNum] -= requestedAmount;

        for (int i = 0; i < canFinish.length; i++) {
            if (!canFinish[i]) {
                return false;
            }
        }
        return true;

    }
    /*/
    used for bankers algorithm, when a process requests more than is available.
     */
    public void singleProcessAbort(Process processToBeDeleted, int[][] currentHasOfEachProcess, int[] resourcesAvailable) {
        int proccessNum = processToBeDeleted.processNumber;
        processToBeDeleted.isDone = true;
        processToBeDeleted.timeTaken = 0;
        processToBeDeleted.waitingTime = -1;

        //for each resource the process has, release it back to manager
        for (int r = 0; r < currentHasOfEachProcess[proccessNum].length; r++) {
            resourcesAvailable[r] += currentHasOfEachProcess[proccessNum][r];
            currentHasOfEachProcess[proccessNum][r] = 0;
        }


    }


    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        //TODO change to args[0]
        //"/Users/hpedskis/Desktop/resourceAllocation1.txt"
        File inputFile = new File(args[0]);
        Scanner sc = new Scanner(inputFile);

        int numProcesses = sc.nextInt();
        int numResources = sc.nextInt();
        //keep track of all processes
        Process[] processes = new Process[numProcesses];
        Process[] processesClone = new Process[numProcesses];

        //keep track of total allocated resources available
        int[] resourcesAvailable = new int[numResources];
        int[] resourcesAvailableClone = new int[numResources];

        //keep track of each process and the amount claimed
        int[][] claimsOfEachProcess = new int[numProcesses][numResources];
        int[][] claimsOfEachProccessClone = new int[numProcesses][numResources];

        int[][] currentHasOfEachProcess = new int[numProcesses][numResources];
        int[][] currentHasOfEachProcessClone = new int[numProcesses][numResources];


        //initialize the array of processes
        for (int p = 0; p < numProcesses; p++) {
            processes[p] = new Process(p);
            processesClone[p] = new Process(p);
        }
        //initialize amount of that resource that's available
        for (int i = 0; i < numResources; i++) {
            int num = sc.nextInt();
            resourcesAvailable[i] = num; //sc.nextInt();
            resourcesAvailableClone[i] = num;

        }
        //initialize the array of events for each process
        ArrayList<ArrayList<Event>> eventsForProcesses = new ArrayList<ArrayList<Event>>();
        ArrayList<ArrayList<Event>> eventsForProcessesClone = new ArrayList<ArrayList<Event>>();

        for (int j = 0; j < numProcesses; j++) {
            ArrayList<Event> currEvents = new ArrayList<Event>();
            ArrayList<Event> currEventsClone = new ArrayList<Event>();
            eventsForProcesses.add(currEvents);
            eventsForProcessesClone.add(currEventsClone);
        }
        //initialize the amount of each resource that the process has
        for (int process = 0; process < numProcesses; process++) {
            for (int resource = 0; resource < numResources; resource++) {
                currentHasOfEachProcess[process][resource] = 0;
                currentHasOfEachProcessClone[process][resource] = 0;
            }

        }

        while (sc.hasNext()) {
            String typeLine = sc.next();

            //subtracting one accounts for the indexing of the arrays
            int processNum = (sc.nextInt() - 1);
            int delay = sc.nextInt();
            int resourceType = (sc.nextInt() - 1);
            int resourceAmount = sc.nextInt();

            int type;
            if (typeLine.equals("initiate")) {
                type = 0;
                claimsOfEachProcess[processNum][resourceType] = resourceAmount;
                claimsOfEachProccessClone[processNum][resourceType] = resourceAmount;
            } else if (typeLine.equals("request")) {
                type = 1;
            } else if (typeLine.equals("release")) {
                type = 2;
            } else { //terminate
                type = 3;
            }
            Event newEvent = new Event(type, processNum, resourceType, resourceAmount, delay);
            Event newEventClone = new Event(type, processNum, resourceType, resourceAmount, delay);


            eventsForProcesses.get(newEvent.processNum).add(newEvent);

            eventsForProcessesClone.get(newEventClone.processNum).add(newEventClone);

        }

        resourceAllocation test = new resourceAllocation();
        System.out.println("FIFO:");
        test.solveResourceAllocation(eventsForProcesses, resourcesAvailable, currentHasOfEachProcess, processes, claimsOfEachProcess, false);
        System.out.println("");
        System.out.println("BANKERS: ");
        test.solveResourceAllocation(eventsForProcessesClone, resourcesAvailableClone, currentHasOfEachProcessClone, processesClone, claimsOfEachProccessClone, true);


    }
}
