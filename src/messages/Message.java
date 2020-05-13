package messages;

public abstract class Message {
	public int serverId; // source of message
	public int term;
	public String text; // useful information (used initially for debugging)

	public Message() {
		this.text = null;
	}

	public Message(String text) {
		this.text = text;
	}
}
