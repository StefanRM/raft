package messages;

public class ClientRequest extends Message {
	public int log;
	
	public ClientRequest(int log) {
		this.log = log;
	}
	
	@Override
	public String toString() {
		return "ClientRequest: [log: " + this.log + "]";
	}
}
