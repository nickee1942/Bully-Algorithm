package bullyElection;

public class Process {
	int rank;
	boolean isLeader;
	boolean isDown;
	
	public Process(int rank){
		this.rank = rank;
		this.isLeader = false;
		this.isDown = false;
	}
	
	public int getRank() {
		return rank;
	}
	
	public void setRank(int rank) {
		this.rank = rank;
	}
	
	public boolean leaderFlag() {
		return isLeader;
	}
	
	public void setLeader(boolean co) {
		isLeader = co;
	}
	
	public boolean downFlag() {
		return isDown;
	}
	
	public void setDown(boolean down) {
		isDown = down;
	}
	
}
