package simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import Main.Main;
import messages.AppendEntry;
import messages.ClientRequest;
import messages.Message;
import messages.RequestVote;
import servers.Server;

public class Supervisor implements Runnable {

	private final Thread[] serversThreads;
	private final Server[] servers;
	public ConcurrentLinkedQueue<Message> msgQ; // message queue
	public int activeServers; // number of currently running servers
	private int nrHeartbeats; // how many heartbeats were sent so far (useful for testing)
	private boolean newHearbeat; // used for enabling only one test per heartbeat
	public int leaderId; // server with leader role
	public List<Integer> shutdownServers = new ArrayList<Integer>(); // list of shutdown servers

	public Supervisor(Thread[] serversThreads, Server[] servers) {
		this.serversThreads = serversThreads;
		this.servers = servers;
		this.activeServers = servers.length;
		this.msgQ = new ConcurrentLinkedQueue<Message>();
		this.nrHeartbeats = 0;
		this.leaderId = -1;
		this.shutdownServers = new ArrayList<Integer>();
		this.newHearbeat = false;
	}

	public void run() {
		System.out.println("[Supervisor] started");
		while (true) {

			while (!this.msgQ.isEmpty()) { // handling new messages

				// test logic --- begin
				// send a new client request at each 7 heartbeats
				if (nrHeartbeats > 0 && nrHeartbeats % 7 == 0 && newHearbeat) {
					ClientRequest clReq = new ClientRequest(nrHeartbeats);
					System.out.println(
							"[Supervisor] received from client: " + clReq + " --> sending to server " + this.leaderId);
					this.servers[this.leaderId].clientReqQ.add(clReq);

					this.newHearbeat = false;
				}

				// stop the leader at heartbeat no. 11
				if (nrHeartbeats == 11 && newHearbeat) {
					this.servers[this.leaderId].stopServer();
					this.shutdownServers.add(this.leaderId);
					this.activeServers--;

					this.newHearbeat = false;
				}

				// restart the shutdown server at heartbeat no. 30
				if (nrHeartbeats == 30 && newHearbeat) {
					int serverId = this.shutdownServers.remove(0);
					this.servers[serverId].restartServer();
					this.activeServers++;
					this.serversThreads[serverId].interrupt();

					this.newHearbeat = false;
				}
				// test logic --- end

				Message msg = this.msgQ.poll();

				if (Main.debug) {
					System.out.println("[Supervisor] received: " + msg);
				}

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
					AppendEntry appEnt = (AppendEntry) msg;
					if (!appEnt.fromleader) {

						servers[appEnt.leaderId].msgQ.add(appEnt);
						// leader server does not need to be interrupted as it waits
						// for heartbeats for a certain amount of time
					} else {
						nrHeartbeats++; // heartbeats
						this.newHearbeat = true;
						System.out.println("[Supervisor] Nr. of Heartbeats so far: " + this.nrHeartbeats);

						if (appEnt.destServerId == -1) { // broadcast the message if -1
							for (int i = 0; i < serversThreads.length; i++) {
								if (i != appEnt.serverId) {
									servers[i].msgQ.add(appEnt);
									serversThreads[i].interrupt();
								}
							}
						} else {
							servers[appEnt.destServerId].msgQ.add(appEnt);
							serversThreads[appEnt.destServerId].interrupt();
						}
					}
				}
			}
		}
	}
}
