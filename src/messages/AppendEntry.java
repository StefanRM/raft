package messages;

import java.util.List;

import servers.Log;

public class AppendEntry extends Message {
	public int leaderId;
	public boolean fromleader;
	public boolean logReplication;
	public boolean needLog;
	public int prevLogIndex;
	public int prevLogTerm;
	public List<Log> entries;
	public int leaderCommit;

	public AppendEntry(int term, int leaderId, boolean fromleader, int serverId) {
		this.term = term;
		this.leaderId = leaderId;
		this.fromleader = fromleader;
		this.serverId = serverId;
		this.logReplication = false;
	}

	public AppendEntry(int term, int leaderId, boolean fromleader, int serverId, boolean logReplication, int prevLogIndex, int prevLogTerm,
			List<Log> entries, int leaderCommit, boolean needLog) {
		this.term = term;
		this.leaderId = leaderId;
		this.fromleader = fromleader;
		this.serverId = serverId;
		this.logReplication = logReplication;
		this.prevLogIndex = prevLogIndex;
		this.prevLogTerm = prevLogTerm;
		this.entries = entries;
		this.leaderCommit = leaderCommit;
		this.needLog = needLog;
	}

	@Override
	public String toString() {
		if (!logReplication) {
			return "AppendEntry: [term: " + this.term + ", leaderId: " + this.leaderId + ", fromleader: "
					+ this.fromleader + ", serverId: " + this.serverId + "]";
		}

		return "AppendEntry: [term: " + this.term + ", leaderId: " + this.leaderId + ", fromleader: " + this.fromleader
				+ ", serverId: " + this.serverId + ", prevLogIndex: " + this.prevLogIndex + ", prevLogTerm: "
				+ this.prevLogTerm + ", entries: " + this.entries + ", leaderCommit: " + this.leaderCommit + "]";
	}
}
