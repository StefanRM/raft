package messages;

public class ClientRequest extends Message {
	public int log; // information from client to be applied on servers

	public ClientRequest(int log) {
		this.log = log;
	}

	@Override
	public String toString() {
		return "ClientRequest: [log: " + this.log + "]";
	}
}
