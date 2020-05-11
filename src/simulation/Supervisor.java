package simulation;

import java.sql.ClientInfoStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import messages.AppendEntry;
import messages.ClientRequest;
import messages.Message;
import messages.RequestVote;
import servers.Server;
import servers.State;

public class Supervisor implements Runnable {

	private final Thread[] serversThreads;
	private final Server[] servers;
	public ConcurrentLinkedQueue<Message> msgQ;
	public int activeServers;
	private int nrMessages;
	public int leaderId;
	public List<Integer> shutdownServers = new ArrayList<Integer>();

	public Supervisor(Thread[] serversThreads, Server[] servers) {
		this.serversThreads = serversThreads;
		this.servers = servers;
		this.activeServers = servers.length;
		this.msgQ = new ConcurrentLinkedQueue<Message>();
		this.nrMessages = 0;
		this.leaderId = -1;
		this.shutdownServers = new ArrayList<Integer>();
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

			while (!this.msgQ.isEmpty()) {
				nrMessages++;
				
				// test logic
//				if (nrMessages % 5 == 0) {
//					ClientRequest clReq = new ClientRequest(nrMessages);
//					System.out.println(
//							"[Supervisor] received from client: " + clReq + " --> sending to server " + this.leaderId);
//					this.servers[this.leaderId].clientReqQ.add(clReq);
//				}
//				
//				if (nrMessages == 10) {
//					this.servers[this.leaderId].stopServer();
//					this.shutdownServers.add(this.leaderId);
//					this.activeServers--;
//				}
//				if (nrMessages == 20) {
//					int serverId = this.shutdownServers.remove(0);
//					this.servers[serverId].restartServer();
//					this.activeServers++;
//					this.serversThreads[serverId].interrupt();
//				}
				
				System.out.println("[Supervisor] Message no. " + this.nrMessages);

				Message msg = this.msgQ.poll();
				System.out.println("[Supervisor] received: " + msg);
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
						// leader server does not need to be interrupted as it waits
						// for heartbeats for a certain amount of time
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
