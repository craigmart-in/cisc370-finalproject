package blackjack.server;

import game.server.GameServer;
import game.server.InputException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cards.Card;
import cards.Shoe;

import communication.Communication;
import communication.Response;
import communication.ResponseException;

public class Blackjack {
	private GameServer _gs;
	
	private List<BlackjackPlayer> _players = new ArrayList<BlackjackPlayer>();
	private List<BlackjackPlayer> _toRemove = new ArrayList<BlackjackPlayer>();
	private List<BlackjackPlayer> _toAdd = new ArrayList<BlackjackPlayer>();
	
	private Dealer _dealer;

	private Shoe _shoe;
	private double _bet;
	
	private static final int maxPlayers = 6;
	
	public Blackjack(GameServer gs, int shoeSize) throws IOException, ClassNotFoundException
	{
		_gs = gs;
		
		if(shoeSize >= 2 && shoeSize <= 8)
			_shoe = new Shoe(shoeSize);
		else
			throw new IllegalArgumentException("Blackjack.constructor: Invalid shoe size");

		_dealer = new Dealer(_shoe);
	}
	
	public void playGame() throws IOException, ClassNotFoundException
	{
		boolean flag;
		boolean allBusted;
		boolean atLeastOneActive;
		Hand dealerHand;
		Card[] dealerCards;
		allBusted = true;
		atLeastOneActive = false;
		
		addToPlayers();
		
		if(_players.size() > 0)
			atLeastOneActive = true;
		
		while(atLeastOneActive)
		{
			atLeastOneActive = false;
			flag = true;
			
			addToPlayers();
			for(BlackjackPlayer player : _players)
				player.resetHand();
				
			System.out.println("\nActive players this round: ");
			for(BlackjackPlayer player : _players){
				System.out.println(player.getName());
				atLeastOneActive = true;
			}
			
			if(atLeastOneActive)
			{
				this._dealer.resetHand();
				for(BlackjackPlayer player : _players){
					for(BlackjackPlayer player2 : _players)
						if(!player.equals(player2))
							//Prints message to other clients that are not currently playing
							Communication.sendWait(player2,"Waiting for other players to make a decision...");
					
					//TODO Parallel betting.
					boolean doneBet = false;
					while(!doneBet){
						try {
							Communication.sendBank(player, player.getMoney() + "");
							Communication.sendBet(player,"Enter an integer value to wager?(min. 0) ");
							String hold= player.getInputWithTimeout(30);	
							if(!hold.equals("quit")){
								_bet = Response.bet(hold);

								if(_bet > player.getMoney() || _bet < 0){
									Communication.sendError(player,"You do not have that much to wager");
								}
								else{
									player.setBet(_bet);
									doneBet = true;
								}
							}
							else{
								_toRemove.add(player);
								_gs.returnToGameSelectionThread(player);
							}
						}catch (ResponseException e) {
							Communication.sendMessage(player, e.getMessage());
						} catch (InputException e) {
							Communication.sendPop(player, "You were kicked for inactivity");
							_toRemove.add(player);
							_gs.logout(player);
						}
					}
				}

				removePlayers();
				
				for(BlackjackPlayer player : _players){
					Communication.sendHands(player, "CLEAR");
					player.setbet21(false);
					player.setPlayerHit(false);
				}
				
				dealFirstRound();

				dealerHand = this._dealer.getHand();
				dealerCards = dealerHand.getCards();

				if(!this._dealer.is21()){	
					updateTableToAllUsers("dealer=0="+dealerCards[0] + "<>back/");
										
					for(BlackjackPlayer player : _players){						
						for(BlackjackPlayer player2 : _players)
							if(!player.equals(player2))
								//Prints message to other clients that are not currently playing
								Communication.sendWait(player2,"Waiting for other players to make a decision...");

						allBusted = true;
						while(flag){
							try {
								flag = player.hitMe();
							} catch (InputException e) {
								flag = false;
								_toRemove.add(player);
								_gs.logout(player);
							}

							if(flag){
								this._dealer.hitPlayer(player);
								player.setPlayerHit(true);
								updateTableToAllUsers("dealer=0="+dealerCards[0] + "<>back/");
							}
								
							if(player.isBusted())
							{
								flag = false;
							}
							else if(player.is21()&& !player.getPlayerHit())
							{
								flag = false;
								allBusted = false;
								player.setbet21(true);
							}
							else
								allBusted = false;
						}
						flag = true;
					}
					
					removePlayers();
					updateTableToAllUsers(this._dealer.toString());
					if(!allBusted){
						while(this._dealer.hitMe()){
							this._dealer.dealSelf();
							updateTableToAllUsers(this._dealer.toString());
						}
					}
				}
				else
					updateTableToAllUsers(this._dealer.toString());
				
				//TODO Update stats after check.
				for(BlackjackPlayer player : _players)
					player.setResult(this._dealer.winLoseOrPush(player));
				
				updateMoneyStats();
				
				for(BlackjackPlayer player : _players)
					Communication.sendResults(player,printResults());
			}
		}
	}

	public String printResults()
	{
		String result = "";
		for(BlackjackPlayer player : _players){
			if(player.getResult() == 0){
				result = result + "Push<>";
			}
				
			else if(player.getResult() > 0){
				if(player.getbet21() == true)
					result = result + "Won $"+(1.5*player.getBet())+"<>";
				else
					result = result + "Won $"+player.getBet()+"<>";
			}
				
			else if(player.getResult() < 0){
				result = result + "Lost $"+player.getBet()+"<>";
			}
		}
		return result;
	}
	
	public void addToPlayers(){
		_players.addAll(_toAdd);
		_toAdd.clear();
	}
	
	public synchronized void addPlayer(BlackjackPlayer player){
		if(_players.size() <= maxPlayers)
			_toAdd.add(player);
		else{
			Communication.sendMessage(player, "The current table is full.");
			_gs.returnToGameSelectionThread(player);
		}
	}

	public void dealFirstRound()
	{
		this._dealer.shuffleCheck(_players.size());// shuffles the shoe if there are not so many as 10 cards per player
		
		for(int j=0; j<2; j++)
		{
			for(BlackjackPlayer player : _players)
				this._dealer.deal(player);

			this._dealer.dealSelf();
		}
	}

	public void removePlayers(){
		_players.removeAll(_toRemove);
		_toRemove.clear();
	}
	
	public void updateTableToAllUsers(String dealer){ //TODO
		for(BlackjackPlayer player : _players){
			String hands = "";
			hands += dealer;
			
			//Prints ALL players' names and cards
			for(BlackjackPlayer player2 : _players){
				hands += player2.toSpecialString();
			}
			Communication.sendHands(player, hands);
		}
	}
	
	public void updateMoneyStats(){
		for(BlackjackPlayer player : _players){
			if(player.getResult() == 0){
				_gs.updatePushes(player);
				_gs.updateMoney(player,0);
			}
			else if(player.getResult() > 0){
				_gs.updateWins(player);
				if(player.getbet21() == true)
					_gs.updateMoney(player,(1.5*player.getBet()));
				else
					_gs.updateMoney(player,(player.getBet()));
			}
				
			else if(player.getResult() < 0){
				_gs.updateLosses(player);
				_gs.updateMoney(player,(-1*player.getBet()));
			}
			_gs.updateTotal(player);
			Communication.sendBank(player, player.getMoney() +"");
			Communication.sendStats(player, player.getStats() +"");
		}
	}
}
