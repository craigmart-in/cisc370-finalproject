package blackjack.server;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import cards.Card;

import communication.Communication;
import communication.Response;
import communication.ResponseException;

import game.server.InputException;
import game.server.User;

public class BlackjackPlayer extends User{

	private Hand _hand;
	private boolean _isActive;
	private int _result;
	private boolean _bet21;
	private boolean _playerHit;
	
	public BlackjackPlayer(Socket client, PrintWriter out, BufferedReader in) {
		super(client, out, in);
		_hand = new Hand();
		_isActive = false;
		_result = -1;
	}
	
	public Hand getHand() { return _hand; }

	public void addCard(Card card){
		_hand.addCard(card);
	}
	
	public void resetHand(){
		_hand.resetHand();
	}
	
	public boolean isActive() { return _isActive; }
	
	public void setIsActive(boolean isActive){
		_isActive = isActive;
	}
	
	public int getResult(){ return _result; }
	
	public void setResult(int result){
		_result = result;
	}
	
	public boolean is21()
	{
		boolean flag;
		int[] score = _hand.getValues();
		flag = false;
		for(int i=0; i<score.length; i++)
			if(score[i] == 21)
				flag = true;

		return flag;
	}
	
	public boolean isBusted()
	{
		boolean flag;
		int[] score = _hand.getValues();
		flag = true;
		for(int i=0; i<score.length; i++)
			if(score[i] < 22)
				flag = false;

		return flag;
	}

	public boolean hitMe() throws IOException, ClassNotFoundException, InputException
	{
		boolean flag;
		boolean done;

		done = false;
		flag = true;

		if(this.isBusted() || this.is21())
			flag = false;
		if(flag)
		{
			while(!done)
			{
				Communication.sendMessage(this,"\n\nYou have: " + getHand());
				Communication.sendQuestion(this,"\nWould you like to hit (y/n)?");
				try{
					/*if(Response.binaryEval(getInput().readLine()))
					{
						flag = true;
						done = true;
					}
					else
					{
						flag = false;
						done = true;
					}*/
					
					switch(Response.trinaryEval(getInputWithTimeout(30)))
					{
					case -1:
						flag = false;
						done = true;
						break;
					case 0:
						flag = false;
						done = true;
						break;
					case 1:
						flag = true;
						done = true;
						break;
					}
				}catch (ResponseException ex){
					Communication.sendMessage(this, ex.getMessage());
					done = false;
				}
			}
		}

		return flag;
	}
	
	public boolean getbet21(){return _bet21;}
	public void setbet21(boolean flag){
		_bet21 = flag;
	}
	
	public boolean getPlayerHit(){return _playerHit;}
	public void setPlayerHit(boolean flag){
		_playerHit = flag;
	}
	
	public String toString()
	{
		String result = "";
		result = getName() + " " + _hand;
		return result;
	}
}
