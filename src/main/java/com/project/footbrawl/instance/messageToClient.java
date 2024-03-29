package com.project.footbrawl.instance;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageToClient extends Message {

	private Integer numberOfDice; // Integers so default to null
	private Integer userToChoose;
	private int[][] attAssists;
	private int[][] defAssists;
	private List<jsonTile> route; // for moving to a specific point
	private List<jsonTile> squares; // for seeing all squares player can move to
	private Integer rollNeeded;
	private Integer secondaryRollNeeded;
	private List<Integer> rolled;
	private String rollOutcome;
	private String rollType;
	private String team1Name;
	private String team2Name;
	private String teamName;
	private String playerName;
	private String opponentName;
	private TeamInGame team1FullDetails;
	private TeamInGame team2FullDetails;
	private List<PlayerInGame> team1;
	private List<PlayerInGame> team2;
	private Integer team1Score;
	private Integer team2Score;
	private List<String> rerollOptions;
	private String end; // if more messages to follow
	private String playerStatus;
	private int[] ballLocation;
	private boolean isReroll;
	private List<String> possibleActions;
	private String phase;
	
	public MessageToClient() {
        route = null;
		squares = null;
	}

	public List<jsonTile> getSquares() {
		return squares;
	}

	public void setSquares(List<jsonTile> squares) {
		this.squares = squares;
	}

	public List<jsonTile> getRoute() {
		return route;
	}

	public void setRoute(List<jsonTile> route) {
		this.route = route;
	}

	public Integer getDice() {
		return numberOfDice;
	}

	public void setDice(int dice) {
		this.numberOfDice = dice;
	}

	public Integer getUserToChoose() {
		return userToChoose;
	}

	public void setUserToChoose(int userToChoose) {
		this.userToChoose = userToChoose;
	}

	public Integer getNumberOfDice() {
		return numberOfDice;
	}

	public void setNumberOfDice(int numberOfDice) {
		this.numberOfDice = numberOfDice;
	}

	public Integer getRollNeeded() {
		return rollNeeded;
	}

	public void setRollNeeded(int rollNeeded) {
		this.rollNeeded = rollNeeded;
	}

	public List<Integer> getRolled() {
		return rolled;
	}

	public String getTeam1Name() {
		return team1Name;
	}

	public void setTeam1Name(String team1Name) {
		this.team1Name = team1Name;
	}

	public String getTeam2Name() {
		return team2Name;
	}

	public void setTeam2Name(String team2Name) {
		this.team2Name = team2Name;
	}

	public List<PlayerInGame> getTeam1() {
		return team1;
	}

	public void setTeam1(List<PlayerInGame> team1) {
		this.team1 = team1;
	}

	public List<PlayerInGame> getTeam2() {
		return team2;
	}

	public void setTeam2(List<PlayerInGame> team2) {
		this.team2 = team2;
	}

	public void setNumberOfDice(Integer numberOfDice) {
		this.numberOfDice = numberOfDice;
	}

	public void setUserToChoose(Integer userToChoose) {
		this.userToChoose = userToChoose;
	}

	public void setRollNeeded(Integer rollNeeded) {
		this.rollNeeded = rollNeeded;
	}

	public void setRolled(List<Integer> rolled) {
		this.rolled = rolled;
	}

	public String getRollOutcome() {
		return rollOutcome;
	}

	public void setRollOutcome(String rollOutcome) {
		this.rollOutcome = rollOutcome;
	}

	public String getRollType() {
		return rollType;
	}

	public void setRollType(String rollType) {
		this.rollType = rollType;
	}

	public String getPlayerName() {
		return playerName;
	}

	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	public List<String> getRerollOptions() {
		return rerollOptions;
	}

	public void setRerollOptions(List<String> rerollOptions) {
		this.rerollOptions = rerollOptions;
	}

	public String getEnd() {
		return end;
	}

	public void setEnd(String end) {
		this.end = end;
	}

	public String getTeamName() {
		return teamName;
	}

	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}

	public String getPlayerStatus() {
		return playerStatus;
	}

	public void setPlayerStatus(String playerStatus) {
		this.playerStatus = playerStatus;
	}

	public TeamInGame getTeam1FullDetails() {
		return team1FullDetails;
	}

	public void setTeam1FullDetails(TeamInGame team1FullDetails) {
		this.team1FullDetails = team1FullDetails;
	}

	public TeamInGame getTeam2FullDetails() {
		return team2FullDetails;
	}

	public void setTeam2FullDetails(TeamInGame team2FullDetails) {
		this.team2FullDetails = team2FullDetails;
	}

	public Integer getTeam1Score() {
		return team1Score;
	}

	public void setTeam1Score(Integer team1Score) {
		this.team1Score = team1Score;
	}

	public Integer getTeam2Score() {
		return team2Score;
	}

	public void setTeam2Score(Integer team2Score) {
		this.team2Score = team2Score;
	}

	public int[] getBallLocation() {
		return ballLocation;
	}

	public void setBallLocation(int[] ballLocation) {
		this.ballLocation = ballLocation;
	}

	public boolean isReroll() {
		return isReroll;
	}

	public void setReroll(boolean isReroll) {
		this.isReroll = isReroll;
	}

	public int[][] getAttAssists() {
		return attAssists;
	}

	public void setAttAssists(int[][] attAssists) {
		this.attAssists = attAssists;
	}

	public int[][] getDefAssists() {
		return defAssists;
	}

	public void setDefAssists(int[][] defAssists) {
		this.defAssists = defAssists;
	}

	public String getOpponentName() {
		return opponentName;
	}

	public void setOpponentName(String opponentName) {
		this.opponentName = opponentName;
	}

	public List<String> getPossibleActions() {
		return possibleActions;
	}

	public void setPossibleActions(List<String> possibleActions) {
		this.possibleActions = possibleActions;
	}

	public Integer getSecondaryRollNeeded() {
		return secondaryRollNeeded;
	}

	public void setSecondaryRollNeeded(Integer secondaryRollNeeded) {
		this.secondaryRollNeeded = secondaryRollNeeded;
	}

	public String getPhase() {
		return phase;
	}

	public void setPhase(String phase) {
		this.phase = phase;
	}
	
}
