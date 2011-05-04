package game.server;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

import dal.DataAccessLayer;

public class User {
	private Socket _client;
	private PrintWriter _out;
	private BufferedReader _in;
	private String _userName;
	private double _money;
	
	private DataAccessLayer _dal;

	public User(String userName, Socket client, PrintWriter out, BufferedReader in)
	{
		_dal = new DataAccessLayer();
		_client = client;
		_out = out;
		_in = in;
		_userName = userName;
		try {
			_money = _dal.getMoney(_userName);
		} catch (Exception e) {
			//Should never get here.
		}
	}

	public Socket getSocket()  { return _client; }

	public PrintWriter getOutput()  { return _out; }

	public BufferedReader getInput()  { return _in; }
	
	public String getName()  { return _userName; }
	
	public double getMoney() { return _money; }
	
	public boolean addMoney(double add)
	{
		_money += add;
		try {
			_dal.setMoney(_userName, _money);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	//Should we allow -money? probably not Handle elsewhere?
	public boolean subtractMoney(double subtract)
	{
		_money -= subtract;
		try {
			_dal.setMoney(_userName, _money);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}
