package Main;

import servers.Server;
import simulation.Supervisor;

public class Main {
	public static int nrServers = 3;
	public static boolean debug = false;

	public static void main(String[] args) {
		Thread[] slaves = new Thread[nrServers];
		Server[] servers = new Server[nrServers];

		// initialize the servers
		for (int i = 0; i < slaves.length; i++) {
			servers[i] = new Server(i);
			slaves[i] = new Thread(servers[i]);
		}

		// initialize the supervisor (communication mean between servers)
		Supervisor supervisor = new Supervisor(slaves, servers);
		Thread master = new Thread(supervisor);
		for (int i = 0; i < slaves.length; i++) {
			servers[i].supervisor = supervisor;
			servers[i].supervisorThread = master;
			slaves[i].start();
		}

		// Let's go
		master.start();
		System.out.println("Raft started.");
	}
}
