package messages;

public class RequestVote extends Message {
	public int candidateId;
	public boolean voteGranted;
	public boolean fromCandidate;

	public RequestVote(int term, int candidateId, boolean fromCandidate, int serverId, boolean voteGranted) {
		this.term = term;
		this.candidateId = candidateId;
		this.fromCandidate = fromCandidate;
		this.voteGranted = voteGranted;
		this.serverId = serverId;
	}

	@Override
	public String toString() {
		return "RequestVote: [term: " + this.term + ", candidateId: " + this.candidateId + ", voteGranted: "
				+ this.voteGranted + ", fromCandidate: " + this.fromCandidate + ", serverId: " + this.serverId + "]";
	}
}
