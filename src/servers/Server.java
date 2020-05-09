package servers;

import java.util.concurrent.ConcurrentLinkedQueue;

import messages.AppendEntry;
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

	public Server(int id) {
		this.id = id;
		state = State.FOLLOWER;
		this.term = 0;
		this.nrVotes = 0;
		this.voted = false;
		this.msgQ = new ConcurrentLinkedQueue<Message>();
		this.pendingAppEnt = 0;
	}

	private void resetStats() {
		this.nrVotes = 0;
		this.voted = false;
		this.pendingAppEnt = 0;
	}

	private int getElectionTimeout() {
		return (int) ((Math.random() * (this.electionTimeoutMax - this.electionTimeoutMin)) + this.electionTimeoutMin);
	}

	@Override
	public void run() {
		System.out.println("[Server " + this.id + "] started");
		while (true) {
			if (this.state != State.LEADER) {
				int electionTimeout = getElectionTimeout();
				System.out.println("[Server " + this.id + "] Election Timeout: " + electionTimeout);
				try {
					Thread.interrupted(); // clear interrupt status
					Thread.sleep(electionTimeout);
				} catch (InterruptedException e) {
					while (!this.msgQ.isEmpty()) {
						Message msg = msgQ.poll();
						System.out.println("[Server " + this.id + "] received: " + msg);
						if (this.state == State.FOLLOWER) {
							if (msg instanceof RequestVote) {
								followerRequestVote((RequestVote) msg);
							} else if (msg instanceof AppendEntry) {
								followerAppendEntry((AppendEntry) msg);
							}
						} else {
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
				Message msg = new AppendEntry(this.term, this.id, true, this.id);
				this.pendingAppEnt = this.supervisor.activeServers - 1;

				// send the vote requests
				this.supervisor.msgQ.add(msg);

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
					System.out.println("[Server " + this.id + "] received: " + msg);
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
			System.out.println("[Server " + this.id + "] I am the LEADER");
			resetStats();
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

			respAppEnt = new AppendEntry(this.term, appEnt.leaderId, false, this.id);
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
			respAppEnt = new AppendEntry(this.term, appEnt.leaderId, false, this.id);
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
		} else {
			// There can be only one leader in a term
			System.out.println("[Server " + this.id + "] Some lost message: " + appEnt);
		}
	}
}
