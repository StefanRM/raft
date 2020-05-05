package simulation;

import java.util.concurrent.ConcurrentLinkedQueue;

import messages.AppendEntry;
import messages.Message;
import messages.RequestVote;
import servers.Server;

public class Supervisor implements Runnable {

	private final int SLEEP_TIME = 5000; // ms
	private final Thread[] serversThreads;
	private final Server[] servers;
	public ConcurrentLinkedQueue<Message> msgQ;
	public int activeServers;

	public Supervisor(Thread[] serversThreads, Server[] servers) {
		this.serversThreads = serversThreads;
		this.servers = servers;
		this.activeServers = servers.length;
		this.msgQ = new ConcurrentLinkedQueue<Message>();
	}

	public void run() {
		System.out.println("[Supervisor] started");
		while (true) {
//			System.out.println("[Supervisor] Message sent to servers.");
//			for (int i = 0; i < serversThreads.length; i++) {
//				serversThreads[i].interrupt(); // wake up
//			}
//			try {
//				Thread.interrupted(); // clear interrupt status
//				Thread.sleep(SLEEP_TIME);
//			} catch (InterruptedException e) {
//				System.err.println("[Supervisor] abnormal wake up.");
//				System.out.println("am primit: " + this.txt);
//			}

			try {
				Thread.interrupted(); // clear interrupt status
				Thread.sleep(SLEEP_TIME);
			} catch (InterruptedException e) {
				while (!this.msgQ.isEmpty()) {
					Message msg = this.msgQ.poll();
					System.err.println("[Supervisor] received: " + msg);
					if (msg instanceof RequestVote) {
						if (!((RequestVote) msg).fromCandidate) {
							RequestVote reqVote = (RequestVote) msg;
							servers[reqVote.candidateId].msgQ.add(msg);
							serversThreads[reqVote.candidateId].interrupt();
						} else {
							for (int i = 0; i < serversThreads.length; i++) {
								if (i != msg.serverId) {
									servers[i].msgQ.add(msg);
									serversThreads[i].interrupt();
								}
							}
						}
					} else if (msg instanceof AppendEntry) {
						if (!((AppendEntry) msg).fromleader) {
							AppendEntry appEnt = (AppendEntry) msg;
							servers[appEnt.leaderId].msgQ.add(msg);
							serversThreads[appEnt.leaderId].interrupt();
						} else {
							for (int i = 0; i < serversThreads.length; i++) {
								if (i != msg.serverId) {
									servers[i].msgQ.add(msg);
									serversThreads[i].interrupt();
								}
							}
						}
					}
				}
			}
		}
	}
}
