
public class Operation {
	String id;
	int startTime;
	int spanTime;
	int endTime;
	int robot;
	int job;
	
	public Operation(String id, int span, int robot, int job) {
		this.id = id;
		this.spanTime = span;
		this.robot = robot;
		this.job = job;
	}

	public boolean equals(Operation op) {
		return this.id.equals(op.id);
	}
}