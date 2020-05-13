package servers;

public class Log {
	public int term;
	public int commitIndex; // leader commit index for log replication
	public int log; // information stored

	public Log(int term, int commitIndex, int log) {
		this.term = term;
		this.commitIndex = commitIndex;
		this.log = log;
	}

	@Override
	public String toString() {
		return "Log: [term: " + this.term + ", commitIndex: " + this.commitIndex + ", log: " + this.log + "]";
	}
}
