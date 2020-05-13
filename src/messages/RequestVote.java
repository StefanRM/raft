package messages;

public class RequestVote extends Message {
	public int candidateId; // candidate who requests the votes
	public boolean voteGranted; // result of a server's voting
	public boolean fromCandidate; // whether the source of message is a candidate or not

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
