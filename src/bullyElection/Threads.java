package bullyElection;
import java.util.*;
import java.io.*;
import java.net.*;

public class Threads implements Runnable{
	private Process process;
	private int total_processes;
	private static boolean messageFlag[];
	ServerSocket[] sock;
	Random r;

	public Process getProcess() {
		return process;
	}

	public void setProcess(Process process) {
		this.process = process;
	}

	public Threads(Process process, int total_processes) {
		this.process = process;
		this.total_processes = total_processes;
		this.r = new Random();
		this.sock = new ServerSocket[total_processes];
		Threads.messageFlag = new boolean[total_processes];
		for (int i = 0; i < total_processes; i++)
			Threads.messageFlag[i] = false;
	}

	synchronized private void recovery() {
		while (Election.isElectionFlag()) {
			// wait;
		}
		
		System.out.println("P" + this.process.getRank() + "is recovered");

		try {
			Election.pingLock.lock();
			Election.setPingFlag(false);
			Socket outgoing = new Socket(InetAddress.getLocalHost(), 12345);
			Scanner scan = new Scanner(outgoing.getInputStream());
			PrintWriter out = new PrintWriter(outgoing.getOutputStream(), true);
			System.out.println("P" + this.process.getRank() + ": Who is coordinator?");
			out.println("Who iscoordinator?");
			out.flush();
			
			String rank = scan.nextLine();
			if (this.process.getRank() > Integer.parseInt(rank)) {
				out.println("Resign");
				out.flush();
				System.out.println("P" + this.process.getRank() + ": Resign -> P" + rank);
				String resignStatus = scan.nextLine();
				if (resignStatus.equals("Successfully Resigned")) {
					this.process.setLeader(true);
					sock[this.process.getRank() - 1] = new ServerSocket(10000 + this.process.getRank());
					System.out.println("P" + this.process.getRank()
							+ ": P" + rank + ", now is the coordinator!");
				} 
			} else {
				out.println("Stop Resign");
				out.flush();
			}

			Election.pingLock.unlock();
			return;
			
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}

	}

	synchronized private void pingCoOrdinator() {
		try {
			Election.pingLock.lock();
			if (Election.isPingFlag()) {
				System.out.println("P" + this.process.getRank() + ": Coordinator, are you there?");
				Socket outgoing = new Socket(InetAddress.getLocalHost(), 12345);
				outgoing.close();
			}
		} catch (Exception ex) {
			Election.setPingFlag(false);
			Election.setElectionFlag(true);
			Election.setElectionDetector(this.process);

			System.out.println("P" + this.process.getRank() + ": Coordinator is down, election..");
		} finally {
			Election.pingLock.unlock();
		}
	}

	private void executeJob() {
		int temp = r.nextInt(20);
		for (int i = 0; i <= temp; i++) {
			try {
				Thread.sleep((temp + 1) * 100);
			} catch (InterruptedException e) {
				System.out.println("Error Executing Thread:" + process.getRank());
				System.out.println(e.getMessage());
			}
		}
	}

	@SuppressWarnings({ "static-access" })
	synchronized private boolean sendMessage() {
		boolean response = false;
		try {
			Election.electionLock.lock();
			if (Election.isElectionFlag() && !Threads.isMessageFlag(this.process.getRank() - 1)
					&& this.process.rank >= Election.getElectionDetector().getRank()) {

				for (int i = this.process.getRank() + 1; i <= this.total_processes; i++) {
					try {
						Socket electionMessage = new Socket(InetAddress.getLocalHost(), 10000 + i);
						System.out.println("P" + i + ": Here");
						electionMessage.close();
						response = true;
					} catch (IOException ex) {
						System.out.println("P" + this.process.getRank() + ": P" + i
								+ " didn't respond");
					} catch (Exception ex) {
						System.out.println(ex.getMessage());
					}
				}
				
				this.setMessageFlag(true, this.process.getRank() - 1);
				Election.electionLock.unlock();
				return response;
			} else {
				throw new Exception();
			}
		} catch (Exception ex1) {
			Election.electionLock.unlock();
			return true;
		}
	}

	public static boolean isMessageFlag(int index) {
		return Threads.messageFlag[index];
	}

	public static void setMessageFlag(boolean messageFlag, int index) {
		Threads.messageFlag[index] = messageFlag;
	}

	synchronized private void serve() {
		try {
			boolean done = false;
			Socket incoming = null;
			ServerSocket s = new ServerSocket(12345);
			Election.setPingFlag(true);
			int temp = this.r.nextInt(5) + 5;
			
			for (int counter = 0; counter < temp; counter++) {
				incoming = s.accept();
				if (Election.isPingFlag())
					System.out.println("P" + this.process.getRank() + ": Yes");
				
				Scanner scan = new Scanner(incoming.getInputStream());
				PrintWriter out = new PrintWriter(incoming.getOutputStream(), true);
				while (scan.hasNextLine() && !done) {
					String line = scan.nextLine();
					if (line.equals("Who is the coordinator?")) {
						System.out.println("P" + this.process.getRank() + ": Me");
						out.println(this.process.getRank());
						out.flush();
					} else if (line.equals("Resign")) {
						this.process.setLeader(false);
						out.println("Successfully Resigned");
						out.flush();
						incoming.close();
						s.close();
						System.out.println("P" + this.process.getRank() + ": Successfully Resigned");
						return;
						
					} else if (line.equals("Don't Resign")) {
						done = true;
					}
				}
			}
			
			this.process.setLeader(false);
			this.process.setDown(true);
			try {
				incoming.close();
				s.close();
				sock[this.process.getRank() - 1].close();
				Thread.sleep(15000);
				recovery();
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}

		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
	}

	@Override
	public void run() {
		try {
			sock[this.process.getRank() - 1] = new ServerSocket(10000 + this.process.getRank());
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
		
		while (true) {
			if (process.leaderFlag()) {
				serve();
			} else {
				while (true) {
					executeJob();
					pingCoOrdinator();
					if (Election.isElectionFlag()) {
						if (!sendMessage()) {
							Election.setElectionFlag(false);
							System.out.println("New coordinator is P" + this.process.getRank());
							this.process.setLeader(true);
							for (int i = 0; i < total_processes; i++) {
								Threads.setMessageFlag(false, i);
							}
							break;
						}
					}
				}
			}
		}
	}
}
