import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;

public class Main {
	
	static int Nrobot=0;

	public static void main(String[] args) {
		
		//run reactive tabu search to find best possible solution
		RTS rts = new RTS(readFile(), Nrobot);
		
		//print solution and its makespan
		System.out.println();
		System.out.println("Best solution found:");
		System.out.println(rts.toString());
		System.out.println("with makespan: " + rts.optimalMakespan);

	}
	
	/**
	 * scan the input file from System.in and return a list of jobs containing their operations and the robot that can do it
	 */
	public static ArrayList<LinkedList<Operation>> readFile() {
		ArrayList<LinkedList<Operation>> jobs = new ArrayList<LinkedList<Operation>>();
		int job = 0, r, span;
		
		Scanner s = new Scanner(System.in);
		Nrobot = s.nextInt();
		s.nextLine();
		while(s.hasNextLine()) {
			Scanner sl = new Scanner(s.nextLine());
			jobs.add(new LinkedList<Operation>());
			int nop = 0;
			while(sl.hasNext()) {
				r = sl.nextInt();
				span = sl.nextInt();
				String id = job + "" + nop;
				jobs.get(job).add(new Operation(id, span, r, job));
				nop++;
			}
			job++;
			sl.close();
		}
		s.close();
		
		System.out.println("Input file:");
		for(int i = 0; i < jobs.size(); i++) {
			System.out.print("job " + i + ": ");
			for(int j = 0; j < jobs.get(i).size(); j++) {
				System.out.print(jobs.get(i).get(j).id + " " + jobs.get(i).get(j).robot + " " + jobs.get(i).get(j).spanTime + ", ");
			}
			System.out.println();
		}
		
		return jobs;
	}

}