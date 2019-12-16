package bullyElection;
import java.util.Scanner;
public class Main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Scanner input = new Scanner(System.in);
		System.out.println("Please enter the number of processes: ");
		int process = input.nextInt();
		
		Threads[] t = new Threads[process];

		for (int i = 0; i < process; i++)
			t[i] = new Threads(new Process(i+1), process);
		
		//initialize election
		Process temp = new Process(-1);
		for (int i = 0; i < t.length; i++)
			if (temp.getRank() < t[i].getProcess().getRank())
				temp = t[i].getProcess();
		
		t[temp.rank - 1].getProcess().isLeader = true;
		
		for (int i = 0; i < process; i++)
			new Thread(t[i]).start();
	}

}
