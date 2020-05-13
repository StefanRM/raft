package messages;

import java.util.List;

import servers.Log;

public class AppendEntry extends Message {
	public int leaderId; // current term's leader
	public boolean fromleader; // whether the source of message is the leader or not
	public boolean logReplication; // whether log replication is needed or not
	public boolean needLog; // inconsistency found, need logs to repair
	public int prevLogIndex; // index of log entry immediately preceding new ones
	public int prevLogTerm; // term of prevLogIndex entry
	public List<Log> entries; // entries to be replicated
	public int leaderCommit; // leader's commit for current entries
	public int destServerId; // destination of message

	public AppendEntry(int term, int leaderId, boolean fromleader, int serverId, int destServerId, int prevLogIndex,
			int prevLogTerm) {
		this.term = term;
		this.leaderId = leaderId;
		this.fromleader = fromleader;
		this.serverId = serverId;
		this.logReplication = false;
		this.destServerId = destServerId;
		this.prevLogIndex = prevLogIndex;
		this.prevLogTerm = prevLogTerm;
	}

	public AppendEntry(int term, int leaderId, boolean fromleader, int serverId, boolean logReplication,
			int prevLogIndex, int prevLogTerm, List<Log> entries, int leaderCommit, boolean needLog, int destServerId) {
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
		this.destServerId = destServerId;
	}

	@Override
	public String toString() {
		if (!logReplication) {
			return "AppendEntry: [term: " + this.term + ", leaderId: " + this.leaderId + ", fromleader: "
					+ this.fromleader + ", serverId: " + this.serverId + ", destServerId: " + this.destServerId
					+ ", prevLogIndex: " + this.prevLogIndex + ", prevLogTerm: " + this.prevLogTerm + "]";
		}

		return "AppendEntry: [term: " + this.term + ", leaderId: " + this.leaderId + ", fromleader: " + this.fromleader
				+ ", serverId: " + this.serverId + ", destServerId: " + this.destServerId + ", prevLogIndex: "
				+ this.prevLogIndex + ", prevLogTerm: " + this.prevLogTerm + ", entries: " + this.entries
				+ ", leaderCommit: " + this.leaderCommit + ", needLog: " + this.needLog + "]";
	}
}
