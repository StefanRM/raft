package servers;

public class Log {
	public int term;
	public int commitIndex;
	public int log;

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
