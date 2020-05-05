package messages;

public class AppendEntry extends Message {
	public int leaderId;
	public boolean fromleader;

	public AppendEntry(int term, int leaderId, boolean fromleader, int serverId) {
		this.term = term;
		this.leaderId = leaderId;
		this.fromleader = fromleader;
		this.serverId = serverId;
	}

	@Override
	public String toString() {
		return "AppendEntry: [term: " + this.term + ", leaderId: " + this.leaderId + ", fromleader: " + this.fromleader
				+ ", serverId: " + this.serverId + "]";
	}
}
