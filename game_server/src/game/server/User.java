package game.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Calendar;

//import dal.DataAccessLayer;

public class User{
	private Socket _client;
	private PrintWriter _out;
	private BufferedReader _in;
	private String _userName;
	private double _money;
	private int _wins;
	private int _losses;
	private int _pushes;
	private int _total;
	private double _bet;

	public User(Socket client, PrintWriter out, BufferedReader in)
	{
		_client = client;
		_out = out;
		_in = in;
		_money = 0;
		_wins = 0;
		_losses = 0;
		_pushes = 0;
		_total = 0;
		_bet = 0;
	}

	public Socket getSocket()  { return _client; }

	public PrintWriter getOutput()  { return _out; }

	public BufferedReader getInput()  { return _in; }
	
	public String getUserInput() throws InputException{
		String input = "";
		try {
			input = _in.readLine();
			if (input.equals("*L0gM30ut*"))
				throw new InputException("*L0gM30ut*");
			return input;
		} catch (IOException e) {
			throw new InputException("quit");
		}
	}
	
	public String getInputWithTimeout(int timeLimit) throws InputException
	{
		String input = "quit";
		long startTime = Calendar.getInstance().getTime().getTime();	
		
		try {
			while(!_in.ready() && !timeUp(startTime, timeLimit)){;}
			
			if(timeUp(startTime, timeLimit))
				input = "quit";
			else
				input=_in.readLine();
			
			if(input.equals("*L0gM30ut*"))
				throw new InputException("*L0gM30ut*");
		} catch (IOException e) {
			System.out.println("Time ran out to bet, leaving table...");
		}

		return input;
	}
	
	private boolean timeUp(long startTime, long timeLimit)
	{
		if(Calendar.getInstance().getTime().getTime() - startTime >= timeLimit*1000)
			return true;
		else
			return false;
	}
	
	public String getName()  { return _userName; }
	
	public void setName(String name){
		_userName = name;
	}
	
	public double getMoney() { return _money; }
	
	public void setMoney(double money){
		_money = money;
	}
	
	public double getBet() { return _bet; }
	
	public void setBet(double bet){
		_bet = bet;
	}
	
	public int getWins() { return _wins; }
	
	public void setWins(int wins){
		_wins = wins;
	}
	
	public int getLosses() { return _losses; }
	
	public void setLosses(int losses){
		_losses = losses;
	}
	
	public int getPushes() { return _pushes; }
	
	public void setPushes(int pushes){
		_pushes = pushes;
	}
	
	public String getStats() {
		return getWins() + "-" + getLosses() + "-" + getPushes();
	}
	
	public int getTotal() { return _total; }
	
	public void setTotal(int total){
		_total = total;
	}
}
