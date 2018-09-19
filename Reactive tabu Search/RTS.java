import java.util.ArrayList;
import java.util.LinkedList;

public class RTS {
	
	static final int MAXITERATIONS = 5000;
	int CYCLEMAX = 30;
	
	LinkedList<ArrayList<ArrayList<Operation>>> tabuList = new LinkedList<ArrayList<ArrayList<Operation>>>();
	LinkedList<ArrayList<ArrayList<Operation>>> savingList = new LinkedList<ArrayList<ArrayList<Operation>>>();
	ArrayList<ArrayList<Operation>> optimalSolution;
	int cycleMoveAve = 0;
	int Nrobot;
	int optimalMakespan;
	int TabuMaxLength = 40;
	
	public RTS(ArrayList<LinkedList<Operation>> jobs, int Nr) {
		ArrayList<ArrayList<Operation>> newState;
		ArrayList<ArrayList<Operation>> state;
		int newMakespan, iterations = 0, lastUpdate = 0;
		
		//number of machines
		Nrobot = Nr;
		
		//find initial state and set it as optimal
		state = initialSolution(jobs);
		optimalSolution = state;
		optimalMakespan = calculateMakespan(state);
		
		//save initial state into the tabu list and in saving list
		tabuList.add(state);
		savingList.add(state);
		
		while(iterations < MAXITERATIONS) {
			
			//find best neighbor state not in tabu list
			if((newState = findBestNeighbour(state)) == null) {
				break;
			}
			state = newState;
			
			//if tabu list is at max size delete element as FIFO and add new state
			if(tabuList.size() == TabuMaxLength && tabuList.size() > 0)
				tabuList.removeFirst();
			
			tabuList.add(state);
			
			//calculate makespan of new state
			newMakespan = calculateMakespan(state);
			
			//update optimal state found so far
			if(newMakespan < optimalMakespan) {
				optimalMakespan = newMakespan;
				optimalSolution = state;
			}
			
			//find when state has been visited in the past CYCLEMAX iterations
			int lastiteration;
			lastiteration = checkSavingList(state, iterations);
			
			//if it's less than CYCLEMAX past iterations increase tabu list size
			if(lastiteration <= CYCLEMAX) {
				
				double mean = (double) (cycleMoveAve * (iterations) + lastiteration) / (iterations+1);
				cycleMoveAve = (int) mean;
				
				modifyTLLength(1);
				
				lastUpdate = iterations;
			} else if(iterations - lastUpdate > cycleMoveAve) { //if tabu list hasn't been changed for the past cycleMoveAve iterations decrease tabu list size
				modifyTLLength(-1);
				lastUpdate = iterations;
			}
			
			//if savingList is full make room for new state and add it
			if(savingList.size() == CYCLEMAX)
				savingList.removeFirst();
			savingList.add(state);
			
			iterations++;
		}
	}
	
	/*
	 * takes the list of operations for each job and output an initial solution
	 */
	public ArrayList<ArrayList<Operation>> initialSolution(ArrayList<LinkedList<Operation>> jobs) {
		ArrayList<ArrayList<Operation>> state = new ArrayList<ArrayList<Operation>>();
		ArrayList<Operation> C = new ArrayList<>();
		ArrayList<Operation> D;
		Operation oStar = null;
		Operation oCurrent = null;
		int minTime;
		
		for(int i = 0; i < Nrobot; i++) {
			state.add(new ArrayList<Operation>());
		}
		
		// find set of next operations for each job
		for(LinkedList<Operation> operations : jobs) {
			C.add(operations.removeFirst());
		}
		
		//while there are still operations to be inserted into the schedule
		while(!C.isEmpty()) {
			minTime = Integer.MAX_VALUE;
			
			for(Operation o : C) {	
				o.startTime = 0;
				
				//find minimum start time considering previous operation of same job
				for(int i = 0; i < state.size(); i++) {
					for(int j = 0; j < state.get(i).size(); j++) {
						if(state.get(i).get(j).job == o.job && o.startTime < state.get(i).get(j).endTime)
							o.startTime = state.get(i).get(j).endTime;
					}
				}
				//find minimum start time considering free slots in machine scheduling
				for(int i = 0; i < state.get(o.robot).size(); i++) {
					if(state.get(o.robot).get(i).endTime > o.startTime) {
						if(state.get(o.robot).get(i).startTime - o.startTime < o.spanTime)
							o.startTime = state.get(o.robot).get(i).endTime;
						else
							break;
					}
				}
				o.endTime = o.startTime + o.spanTime;
				
				//find operation with minimum end time -> oStar
				if(o.endTime < minTime) {
					minTime = o.endTime;
					oStar = o;
				}
			}
			
			D = new ArrayList<>();
			
			//find all operations performed by the machine that also performs oStar and add to D
			//if they have a start time smaller that oStar end time
			for(Operation o : C) {
				if(o.robot == oStar.robot && o.startTime < minTime) {
					D.add(o);
				}
			}
			
			//add in solution the first in D updating start time and end time
			oCurrent = D.get(0);
			state.get(oCurrent.robot).add(oCurrent);
			
			// remove operation from set C
			C.remove(oCurrent);
			
			// add in C operation after oCurrent removing it from jobs
			if(jobs.get(oCurrent.job).size() != 0) {
				C.add(jobs.get(oCurrent.job).removeFirst());
			}
		}
		
		return state;
	}
	
	/**
	 * takes the current state and output the neighbor state with the lowest makespan that it's not in the tabu list
	 */
	public ArrayList<ArrayList<Operation>> findBestNeighbour(ArrayList<ArrayList<Operation>> currentState) {
		ArrayList<Operation> criticalPath = new ArrayList<Operation>();
		ArrayList<ArrayList<Operation>> newState;
		ArrayList<ArrayList<Operation>> bestState = null;
		Operation lastop = null;
		int lowestMakespan = Integer.MAX_VALUE;
		int lastendtime = 0;
		
		// set lastop to the operation with the last end time among the last operation of each machine
		for(ArrayList<Operation> r: currentState) { //for each machine
			if(r.get(r.size()-1).endTime > lastendtime) { // last operation for each machine
				lastendtime = r.get(r.size()-1).endTime;
				lastop = r.get(r.size()-1);
			}
		}
		
		// find one critical path
		findCriticalPath(lastop.startTime, currentState, criticalPath, lastop);
		criticalPath.add(lastop);
		
		// if two adjacent operations in critical path have the same machine then critical block -> possible neighbor
		for(int i = 0; i < criticalPath.size()-1; i++) {
			if(criticalPath.get(i).robot == criticalPath.get(i+1).robot && criticalPath.get(i).job != criticalPath.get(i+1).job) {
				//build new state swapping two operations
				newState = replan(cloneState(currentState), cloneOperation(criticalPath.get(i)), cloneOperation(criticalPath.get(i+1)));
				//set bestState to the state with the lowest makespan that is not in tabu list
				if(lowestMakespan > calculateMakespan(newState) && !checkTabuList(newState)) {
					lowestMakespan = calculateMakespan(newState);
					bestState = newState;
				}
			}
		}
		
		return bestState;
	}
	
	/**
	 * return a cloned state of currentState
	 */
	public ArrayList<ArrayList<Operation>> cloneState(ArrayList<ArrayList<Operation>> currentState) {
		ArrayList<ArrayList<Operation>> clonestate = new ArrayList<ArrayList<Operation>>();
		for(int j = 0; j < currentState.size(); j++) {
			clonestate.add(new ArrayList<Operation>());
			for(int k = 0; k < currentState.get(j).size(); k++) {
				Operation newop = new Operation(currentState.get(j).get(k).id, currentState.get(j).get(k).spanTime, currentState.get(j).get(k).robot, currentState.get(j).get(k).job);
				newop.endTime = currentState.get(j).get(k).endTime;
				newop.startTime = currentState.get(j).get(k).startTime;
				clonestate.get(j).add(newop);
			}
		}
		return clonestate;
	}
	
	/**
	 * return a cloned operation of op
	 */
	public Operation cloneOperation(Operation op) {
		Operation newop = new Operation(op.id, op.spanTime, op.robot, op.job);
		newop.startTime = op.startTime;
		newop.endTime = op.endTime;
		return newop;
	}
	
	/**
	 * @param state initial state
	 * @param op1 first operation to swap
	 * @param op2 second operation to swap
	 * @return a neighbor of state with op1 and op2 swapped
	 */
	public ArrayList<ArrayList<Operation>> replan(ArrayList<ArrayList<Operation>> state, Operation op1, Operation op2) {
		ArrayList<ArrayList<Operation>> newState = new ArrayList<ArrayList<Operation>>();
		ArrayList<Operation> lateOperations = new ArrayList<Operation>();
		int i, timeswap = op1.endTime;
		
		for(i = 0; i < state.size(); i++) { // for each machine
			newState.add(new ArrayList<Operation>()); // initialize machine for newState
			for(int j = 0; j < state.get(i).size(); j++) { // for each operation
				// every op ending before swap remains in its previous position, the others are putted into lateOperation list
				if(state.get(i).get(j).endTime <= timeswap && !state.get(i).get(j).equals(op1) && !state.get(i).get(j).equals(op2)) {
					newState.get(i).add(state.get(i).get(j));
				} else if(!state.get(i).get(j).equals(op1) && !state.get(i).get(j).equals(op2)){ // others ops
					lateOperations.add(state.get(i).get(j)); // add every other operations to this list
				}
			}
		}
		
		//swap op1 with op2 and insert them into newState
		op2.startTime = 0;
		// find latest end time for operations already inserted into newState that are of the same job
		for(ArrayList<Operation> r: newState) { // for each machine in newState
			for(Operation o: r) { // for each operation already inserted into newState
				if(o.job == op2.job && o.endTime >= op2.startTime)
					op2.startTime = o.endTime;
			}
		}
		//find minimum start time considering free slots in machine scheduling
		for(i = 0; i < newState.get(op2.robot).size(); i++) {
			
			if(newState.get(op2.robot).get(i).endTime >= op2.startTime) {
				
				if((newState.get(op2.robot).get(i).startTime - op2.startTime < op2.spanTime))
					op2.startTime = newState.get(op2.robot).get(i).endTime;
				else
					break;
				
			}
		}
		op2.endTime = op2.startTime + op2.spanTime;
		op1.startTime = op2.endTime;
		op1.endTime = op1.startTime + op1.spanTime;
		newState.get(op1.robot).add(op2);
		newState.get(op1.robot).add(op1);
		
		// sort operations in lateOperations based on start time
		lateOperations.sort((o1,o2) -> ((Integer) o1.startTime).compareTo(o2.startTime));
		
		// insert into newSolution all operations saved in lateOperations in the best possible way
		for(Operation op: lateOperations) { // for each operation in lateOperations
			op.startTime = 0;
			// find latest end time for operations already inserted into newState that are of the same job
			for(ArrayList<Operation> r: newState) { // for each machine in newState
				for(Operation o: r) { // for each operation already inserted into newState
					if(o.job == op.job && o.endTime >= op.startTime)
						op.startTime = o.endTime;
				}
			}
			//find minimum start time considering free slots in machine scheduling
			for(i = 0; i < newState.get(op.robot).size(); i++) {
				if(newState.get(op.robot).get(i).endTime >= op.startTime) {
					
					if((newState.get(op.robot).get(i).startTime - op.startTime < op.spanTime))
						op.startTime = newState.get(op.robot).get(i).endTime;
					else
						break;
				}
			}
			op.endTime = op.startTime + op.spanTime;
			newState.get(op.robot).add(op);
			newState.get(op.robot).sort((o1,o2) -> ((Integer) o1.startTime).compareTo(o2.startTime));
		}
		
		return newState;
	}
	
	/**
	 * recursive function to find a critical path in the schedule state
	 */
	public boolean findCriticalPath(int time, ArrayList<ArrayList<Operation>> state, ArrayList<Operation> criticalPath, Operation op) {
		//if time == 0 means that I have found a critical path whose first operation is op
		if(time == 0) {
			return true;
		}
		
		for(ArrayList<Operation> r: state) { // for each machine
			for(Operation o: r) { // for each operation
				if(o.endTime == time) {// if operation is adjacent to op
					if(findCriticalPath(o.startTime, state, criticalPath, o)) { // go down another level
						criticalPath.add(o); // add operation if critical path has been found
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	/**
	 * return how many passed iterations newState has been visited;
	 * if newState is not in savingList return CYCLEMAX+1
	 */
	public int checkSavingList(ArrayList<ArrayList<Operation>> newState, int currentIteration) {
		boolean equal;
		
		for(int k = savingList.size()-1; k >= 0; k--) { //for each state saved in savingList
			equal = true;
			for(int i = 0; i < newState.size(); i++) { //for each machine in newSolution
				for(int j = 0; j < newState.get(i).size(); j++) { //for each operation in a machine
					if(!newState.get(i).get(j).equals(savingList.get(k).get(i).get(j)) 
							|| newState.get(i).get(j).startTime != savingList.get(k).get(i).get(j).startTime) {
						equal = false;
					}
				}
			}
			if(equal) {
				return savingList.size()-k;
			}
		}
			
		return CYCLEMAX+1;
	}
	
	/**
	 * return true if newState is in tabuList else return false
	 */
	public boolean checkTabuList(ArrayList<ArrayList<Operation>> newState) {
		boolean equal;
		
		for(ArrayList<ArrayList<Operation>> solution : tabuList) { //for each state in tabuList
			equal = true;
			for(int i = 0; i < newState.size(); i++) { //for each machine
				for(int j = 0; j < newState.get(i).size(); j++) { //for each operation
					if(!newState.get(i).get(j).equals(solution.get(i).get(j)) 
							|| newState.get(i).get(j).startTime != solution.get(i).get(j).startTime) {
						equal = false;
					}
				}
			}
			if(equal)
				return true;
		}
		
		return false;
	}
	
	/**
	 * return the makespan of newState
	 */
	public int calculateMakespan(ArrayList<ArrayList<Operation>> newState) {
		int solution = 0;
		
		for(ArrayList<Operation> r: newState) {
			if(solution < r.get(r.size()-1).endTime)
				solution = r.get(r.size()-1).endTime;
		}
		
		return solution;
	}
	
	/**
	 * modify the length of the tabu list
	 */
	public void modifyTLLength(double alpha) {
		int oldLength = tabuList.size();
		
		TabuMaxLength += alpha;
		if(TabuMaxLength < 1)
			TabuMaxLength = 1;
		while(oldLength > TabuMaxLength) {
			oldLength--;
			tabuList.removeFirst();
		}
	}
	
	@Override
	public String toString() {
		String result = "";
		
		for(int i = 0; i < optimalSolution.size(); i++) {
			//result += "robot " + i;
			for(int j = 0; j < optimalSolution.get(i).size(); j++) {
				result += " " + optimalSolution.get(i).get(j).id;
				result += " " + optimalSolution.get(i).get(j).startTime;
				result += " " + optimalSolution.get(i).get(j).endTime + " ";
			}
			result += "\n";
		}
		
		return result;
	}
	
}
