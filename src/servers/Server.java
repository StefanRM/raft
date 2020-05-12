package servers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import Main.Main;
import messages.AppendEntry;
import messages.ClientRequest;
import messages.Message;
import messages.RequestVote;
import simulation.Supervisor;

public class Server implements Runnable {
	private final int electionTimeoutMin = 3000; // ms
	private final int electionTimeoutMax = 5000; // ms
	public Supervisor supervisor;
	public Thread supervisorThread;
	public int id;
	public State state;
	public int term;
	public int nrVotes;
	public boolean voted;
	public ConcurrentLinkedQueue<Message> msgQ;
	private final int leaderHeartbeatTimeout = 1400; // ms (aprox half of the minimum election timeout)
	public int pendingAppEnt;
	public List<Log> log;
	public int commitIndex;
	public int lastApplied;
	public ConcurrentLinkedQueue<ClientRequest> clientReqQ;
	public int[] nextIndex;
	private final int shutdownTime = 100000; // ms (long enough to stay in shutdown)
	private boolean replicateLogs;

	public Server(int id) {
		this.id = id;
		state = State.FOLLOWER;
		this.term = 0;
		this.nrVotes = 0;
		this.voted = false;
		this.msgQ = new ConcurrentLinkedQueue<Message>();
		this.pendingAppEnt = 0;
		this.log = new ArrayList<Log>();
		this.clientReqQ = new ConcurrentLinkedQueue<ClientRequest>();
		this.nextIndex = new int[Main.nrServers];
		this.lastApplied = -1;
		this.commitIndex = 0;
		for (int i = 0; i < nextIndex.length; i++) {
			nextIndex[i] = 0;
		}
		this.replicateLogs = false;
	}

	private void resetStats() {
		this.nrVotes = 0;
		this.voted = false;
		this.pendingAppEnt = 0;
	}

	private int getElectionTimeout() {
		return (int) ((Math.random() * (this.electionTimeoutMax - this.electionTimeoutMin)) + this.electionTimeoutMin);
	}

	public void stopServer() {
		System.out.println("[Server " + this.id + "] shutdown");
		this.state = State.SHUTDOWN;
	}

	public void restartServer() {
		System.out.println("[Server " + this.id + "] restarted");
		while (!this.msgQ.isEmpty()) {
			this.msgQ.poll();
		}
		while (!this.clientReqQ.isEmpty()) {
			this.clientReqQ.poll();
		}

		this.state = State.FOLLOWER;
	}

	@Override
	public void run() {
		System.out.println("[Server " + this.id + "] started");
		while (true) {
			if (this.state == State.SHUTDOWN) {
				try {
					Thread.interrupted(); // clear interrupt status
					Thread.sleep(this.shutdownTime);
				} catch (InterruptedException e) {

				}
				continue;
			}

			System.out.println("[Server " + this.id + "] Log: " + this.log);
			if (this.state != State.LEADER) {
				int electionTimeout = getElectionTimeout();
				System.out.println("[Server " + this.id + "] Election Timeout: " + electionTimeout);
				try {
					Thread.interrupted(); // clear interrupt status
					Thread.sleep(electionTimeout);
				} catch (InterruptedException e) {
					while (!this.msgQ.isEmpty()) {
						Message msg = msgQ.poll();

						if (Main.debug) {
							System.out.println("[Server " + this.id + "] received: " + msg);
						}

						if (this.state == State.FOLLOWER) {
							if (msg instanceof RequestVote) {
								followerRequestVote((RequestVote) msg);
							} else if (msg instanceof AppendEntry) {
								followerAppendEntry((AppendEntry) msg);
							}
						} else { // candidate
							if (msg instanceof RequestVote) {
								candidateRequestVote((RequestVote) msg);
							} else if (msg instanceof AppendEntry) {
								candidateAppendEntry((AppendEntry) msg);
							}
						}
					}
					continue;
				}

				// no message until election timeout expired, time to get some votes
				this.term++;

				Message msg = new RequestVote(this.term, this.id, true, this.id, false);
				this.state = State.CANDIDATE; // we have a new candidate
				this.nrVotes++; // votes for himself
				this.voted = true;

				// send the vote requests
				this.supervisor.msgQ.add(msg);
			} else {
				ClientRequest clReq = null;
				if (!this.clientReqQ.isEmpty()) {
					clReq = this.clientReqQ.poll();

					System.out.println("[Server " + this.id + "] received " + clReq + " from client");
				}

				Message msg;
				int prevLogIndex = log.size() - 1;
				int prevLogTerm = log.size() < 1 ? -1 : log.get(log.size() - 1).term;
				if (clReq == null) {
					if (!this.replicateLogs) {
						msg = new AppendEntry(this.term, this.id, true, this.id, -1, prevLogIndex, prevLogTerm);

						// send the vote requests
						this.supervisor.msgQ.add(msg);
					} else {
						for (int i = 0; i < nextIndex.length; i++) {
							if (i != this.id) {
								if (this.nextIndex[i] <= this.lastApplied) {
									List<Log> entries = new ArrayList<Log>();
									int specialPrevLogIndex = this.nextIndex[i] - 1;
									while (nextIndex[i] <= this.lastApplied) {
										entries.add(this.log.get(nextIndex[i]));
										nextIndex[i]++;
									}

									msg = new AppendEntry(this.term, this.id, true, this.id, true, specialPrevLogIndex,
											entries.get(0).term, entries, this.commitIndex, false, i);

								} else {
									msg = new AppendEntry(this.term, this.id, true, this.id, i, prevLogIndex,
											prevLogTerm);
								}

								// send the append entries with logs
								this.supervisor.msgQ.add(msg);
							}
						}
						this.replicateLogs = false;
					}
				} else { // TODO
//					prevLogIndex = log.size() - 1;
//					prevLogTerm = log.size() < 1 ? -1 : log.get(log.size() - 1).term;
					Log newLog = new Log(this.term, this.commitIndex + 1, clReq.log);
					this.log.add(newLog);
					this.commitIndex += 1;
					this.lastApplied = log.size() - 1;

					List<Log> entries = new ArrayList<Log>();
					entries.add(newLog);

					msg = new AppendEntry(this.term, this.id, true, this.id, true, prevLogIndex, prevLogTerm, entries,
							this.commitIndex, false, -1);

					// send the append entries with logs
					this.supervisor.msgQ.add(msg);
				}
				this.pendingAppEnt = Main.nrServers - 1;

				System.out.println("[Server " + this.id + "] sent " + this.pendingAppEnt + " heartbeats");

				// wait for the heartbeats to arrive
				try {
					Thread.interrupted(); // clear interrupt status
					Thread.sleep(this.leaderHeartbeatTimeout);
				} catch (InterruptedException e) {
					System.out.println("[Server " + this.id + "] Something happened to the leader!");
				}

				while (!this.msgQ.isEmpty()) {
					msg = msgQ.poll();

					if (Main.debug) {
						System.out.println("[Server " + this.id + "] received: " + msg);
					}

					if (msg instanceof AppendEntry) {
						leaderAppendEntry((AppendEntry) msg);
					}
				}

				System.out.println("[Server " + this.id + "] " + this.pendingAppEnt + " heartbeats remaining");
			}
		}
	}

	private void followerRequestVote(RequestVote reqVote) {
		RequestVote respReqVote;

		if (reqVote.fromCandidate) {
			if (this.term < reqVote.term) {
				this.term = reqVote.term;
				this.voted = true;

				respReqVote = new RequestVote(this.term, reqVote.candidateId, false, this.id, true);
			} else if ((this.term == reqVote.term) && (!this.voted)) {
				this.voted = true;

				respReqVote = new RequestVote(this.term, reqVote.candidateId, false, this.id, true);
			} else {
				respReqVote = new RequestVote(this.term, reqVote.candidateId, false, this.id, false);
			}

			this.supervisor.msgQ.add(respReqVote);
		} else {
			System.out.println("[Server " + this.id + "] Some lost message: " + reqVote);
		}
	}

	private void candidateRequestVote(RequestVote reqVote) {
		if (!reqVote.fromCandidate) {
			if (this.term == reqVote.term) {
				if (reqVote.voteGranted) {
					this.nrVotes++;
				}
			} else if (this.term < reqVote.term) {
				this.state = State.FOLLOWER;
				this.term = reqVote.term;
				resetStats();
				return;
			} else {
				return;
			}
		} else {
			RequestVote respReqVote;

			if (this.term == reqVote.term) {
				respReqVote = new RequestVote(this.term, reqVote.candidateId, false, this.id, false);
			} else if (this.term < reqVote.term) {
				this.state = State.FOLLOWER;
				this.term = reqVote.term;
				resetStats();
				this.voted = true;

				respReqVote = new RequestVote(this.term, reqVote.candidateId, false, this.id, true);
			} else {
				respReqVote = new RequestVote(this.term, reqVote.candidateId, false, this.id, false);
			}

			this.supervisor.msgQ.add(respReqVote);

			return;
		}

		if (this.nrVotes >= (supervisor.activeServers / 2 + 1)) {
			this.state = State.LEADER;
			this.supervisor.leaderId = this.id;
			System.out.println("[Server " + this.id + "] I am the LEADER, term " + this.term);
			resetStats();
			for (int i = 0; i < nextIndex.length; i++) {
				nextIndex[i] = this.lastApplied + 1;
			}
		}
	}

	private void followerAppendEntry(AppendEntry appEnt) {
		AppendEntry respAppEnt;

		if (appEnt.fromleader) {

			if (this.term < appEnt.term) {
				this.term = appEnt.term;
			} else if ((this.term == appEnt.term)) {
				// normal case (everything up-to-date)
			} else {
				return;
			}

			int recPrevLogIndex = appEnt.prevLogIndex;
			int prevLogIndex = log.size() - 1;
			int prevLogTerm = log.size() < 1 ? -1 : log.get(log.size() - 1).term;

			if (!appEnt.logReplication) {
				if (recPrevLogIndex == prevLogIndex) {
					respAppEnt = new AppendEntry(this.term, appEnt.leaderId, false, this.id, appEnt.leaderId,
							appEnt.prevLogIndex, appEnt.prevLogTerm);
				} else {
					System.out.println("[Server " + this.id + "] inconsistency in logs");
					respAppEnt = new AppendEntry(this.term, appEnt.leaderId, false, this.id, appEnt.leaderId,
							prevLogIndex, prevLogTerm);
					respAppEnt.logReplication = true;
					respAppEnt.needLog = true;
				}
			} else { // TODO

//				int recPrevLogIndex = appEnt.prevLogIndex;
//				int recPrevLogTerm = appEnt.prevLogTerm;
//				int prevLogIndex = log.size() - 1;
//				int prevLogTerm = log.size() < 1 ? -1 : log.get(log.size() - 1).term;

				if (recPrevLogIndex == prevLogIndex) {
					this.commitIndex = appEnt.leaderCommit;

					log.addAll(appEnt.entries);

					respAppEnt = new AppendEntry(this.term, appEnt.leaderId, false, this.id, true, appEnt.prevLogIndex,
							appEnt.prevLogTerm, null, this.commitIndex, false, appEnt.leaderId);
				} else {
					System.out.println("[Server " + this.id + "] inconsistency in logs");
					respAppEnt = new AppendEntry(this.term, appEnt.leaderId, false, this.id, true, prevLogIndex,
							prevLogTerm, null, this.commitIndex, true, appEnt.leaderId);
				}
			}

			this.supervisor.msgQ.add(respAppEnt);
		} else {
			System.out.println("[Server " + this.id + "] !!!Some lost message: " + appEnt);
		}
	}

	private void candidateAppendEntry(AppendEntry appEnt) {
		AppendEntry respAppEnt;

		if (appEnt.fromleader) {
			if (this.term < appEnt.term) {
				this.term = appEnt.term;
			} else if ((this.term == appEnt.term)) {
				// normal case (everything up-to-date)
			} else {
				return;
			}

			this.state = State.FOLLOWER;
			respAppEnt = new AppendEntry(this.term, appEnt.leaderId, false, this.id, appEnt.leaderId,
					appEnt.prevLogIndex, appEnt.prevLogTerm);
			this.supervisor.msgQ.add(respAppEnt);
		} else {
			System.out.println("[Server " + this.id + "] !!!Some lost message: " + appEnt);
		}
	}

	private void leaderAppendEntry(AppendEntry appEnt) {
		if (!appEnt.fromleader) {
			if (this.term < appEnt.term) {
				this.term = appEnt.term;
				this.state = State.FOLLOWER;
			} else if ((this.term == appEnt.term)) {
				this.pendingAppEnt--;
			} else {
				return;
			}

			if (appEnt.logReplication) {
				if (appEnt.needLog) {
					this.nextIndex[appEnt.serverId] = appEnt.prevLogIndex + 1;
					this.replicateLogs = true;
				} else {
					this.nextIndex[appEnt.serverId] = this.lastApplied + 1;
				}
			}

		} else {
			// There can be only one leader in a term
			System.out.println("[Server " + this.id + "] Some lost message: " + appEnt);
		}
	}
}
