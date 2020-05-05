package messages;

public abstract class Message {
	public int serverId;
	public int term;
	public String text;
	
	public Message() {
		this.text = null;
	}
	
	public Message(String text) {
		this.text = text;
	}
}
