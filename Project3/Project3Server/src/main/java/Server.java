import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Server {

	int count = 1;
	ArrayList<ClientThread> clients = new ArrayList<>();
	TheServer server;
	private Consumer<Serializable> callback;

	// Active multiplayer game
	ClientThread player1 = null;
	ClientThread player2 = null;
	CheckersGame game = null;

	// Rematch tracking
	boolean player1WantsRematch = false;
	boolean player2WantsRematch = false;

	Server(Consumer<Serializable> call) {
		callback = call;
		server = new TheServer();
		server.start();
	}

	public class TheServer extends Thread {
		public void run() {
			try (ServerSocket mysocket = new ServerSocket(5555)) {
				System.out.println("Server is waiting for a client!");
				while (true) {
					ClientThread c = new ClientThread(mysocket.accept(), count);
					callback.accept("Client #" + count + " connected.");
					clients.add(c);
					c.start();
					count++;
				}
			} catch (Exception e) {
				callback.accept("Server socket did not launch");
			}
		}
	}

	// Broadcasts the list of lobby players (anyone not currently in a game)
	synchronized void broadcastLobbyUpdate() {
		List<String> lobbyPlayers = new ArrayList<>();
		for (ClientThread c : clients) {
			// Exclude players in multiplayer games AND players in AI games
			if (c.username != null && c != player1 && c != player2 && !c.inAiGame) {
				lobbyPlayers.add(c.username);
			}
		}
		Message update = Message.lobbyUpdate(lobbyPlayers);
		for (ClientThread c : clients) {
			if (c.username != null && c != player1 && c != player2 && !c.inAiGame) {
				c.send(update);
			}
		}
		callback.accept("Lobby updated: " + lobbyPlayers);
	}

	// Starts a multiplayer game between two specific players
	synchronized void startGame(ClientThread p1, ClientThread p2) {
		player1 = p1;
		player2 = p2;
		player1WantsRematch = false;
		player2WantsRematch = false;
		game = new CheckersGame(player1.username, player2.username);
		callback.accept("Game started: " + player1.username + " (RED) vs " + player2.username + " (BLACK)");
		Message startMsg = Message.gameStart(player1.username, player2.username, game.getBoard());
		player1.send(startMsg);
		player2.send(startMsg);
		checkAndSendGameOverIfNoMoves();
	}

	// Starts an AI game for a single client
	synchronized void startAiGame(ClientThread client, CheckersAI.Difficulty difficulty) {
		client.aiGame = new CheckersGame(client.username, "A.I.");
		client.ai = new CheckersAI(difficulty);
		client.inAiGame = true;
		callback.accept("AI game started: " + client.username + " (RED) vs A.I. [" + difficulty + "]");
		Message startMsg = Message.gameStart(client.username, "A.I.", client.aiGame.getBoard());
		client.send(startMsg);
		broadcastLobbyUpdate();
	}

	synchronized void handleMove(ClientThread sender, Message msg) {
		if (game == null) {
			sender.send(Message.invalidMove("Game has not started yet."));
			return;
		}
		if (game.isGameOver()) {
			sender.send(Message.invalidMove("Game is already over."));
			return;
		}

		checkAndSendGameOverIfNoMoves();
		if (game == null) return;

		boolean valid = game.makeMove(
				msg.getSenderUsername(),
				msg.getFromRow(), msg.getFromCol(),
				msg.getToRow(), msg.getToCol()
		);

		if (!valid) {
			sender.send(Message.invalidMove("That move is not valid."));
			return;
		}

		callback.accept(msg.getSenderUsername() + " moved: (" + msg.getFromRow() + "," + msg.getFromCol() + ") -> (" + msg.getToRow() + "," + msg.getToCol() + ")");

		if (game.isGameOver()) {
			String winner = game.getWinner();
			Message gameOverMsg = (winner == null) ? Message.gameDraw() : Message.gameOver(winner);
			player1.send(gameOverMsg);
			player2.send(gameOverMsg);
			callback.accept("Game over! Result: " + (winner != null ? winner + " wins" : "Draw"));
			game = null;
			return;
		}

		Message stateMsg = Message.gameState(
				game.getBoard(), game.getCurrentTurn(),
				game.getRedPlayer(), game.getBlackPlayer(),
				game.getMultiJumpRow(), game.getMultiJumpCol()
		);
		player1.send(stateMsg);
		player2.send(stateMsg);

		checkAndSendGameOverIfNoMoves();
	}

	// Handles a human move in an AI game, then triggers the AI response
	synchronized void handleAiMove(ClientThread sender, Message msg) {
		CheckersGame aiGame = sender.aiGame;
		if (aiGame == null || aiGame.isGameOver()) {
			sender.send(Message.invalidMove("AI game is not active."));
			return;
		}

		boolean valid = aiGame.makeMove(
				msg.getSenderUsername(),
				msg.getFromRow(), msg.getFromCol(),
				msg.getToRow(), msg.getToCol()
		);

		if (!valid) {
			sender.send(Message.invalidMove("That move is not valid."));
			return;
		}

		callback.accept("[AI Game] " + msg.getSenderUsername() + " moved: (" + msg.getFromRow() + "," + msg.getFromCol() + ") -> (" + msg.getToRow() + "," + msg.getToCol() + ")");

		if (aiGame.isGameOver()) {
			sendAiGameOver(sender);
			return;
		}

		// If human is in a multi-jump, send state back and wait for next human move
		if (aiGame.getMultiJumpRow() != -1) {
			sender.send(Message.gameState(
					aiGame.getBoard(), aiGame.getCurrentTurn(),
					aiGame.getRedPlayer(), aiGame.getBlackPlayer(),
					aiGame.getMultiJumpRow(), aiGame.getMultiJumpCol()
			));
			return;
		}

		// Now it's the AI's turn — run minimax (possibly multiple times for multi-jump)
		runAiTurn(sender);
	}

	// Runs AI move(s) until AI's turn is done or the game ends
	private void runAiTurn(ClientThread sender) {
		CheckersGame aiGame = sender.aiGame;
		CheckersAI ai = sender.ai;

		while (aiGame.getCurrentTurn() == CheckersGame.BLACK && !aiGame.isGameOver()) {
			int[] move = ai.getBestMove(aiGame.getBoard(), CheckersGame.BLACK);
			if (move == null) break; // AI has no moves

			boolean ok = aiGame.makeMove("A.I.", move[0], move[1], move[2], move[3]);
			if (!ok) break;

			callback.accept("[AI Game] A.I. moved: (" + move[0] + "," + move[1] + ") -> (" + move[2] + "," + move[3] + ")");

			if (aiGame.isGameOver()) {
				sendAiGameOver(sender);
				return;
			}

			// If AI can multi-jump, loop continues — it keeps going until done
			if (aiGame.getMultiJumpRow() == -1) break;
		}

		// Send updated state back to human player
		sender.send(Message.gameState(
				aiGame.getBoard(), aiGame.getCurrentTurn(),
				aiGame.getRedPlayer(), aiGame.getBlackPlayer(),
				aiGame.getMultiJumpRow(), aiGame.getMultiJumpCol()
		));
	}

	private void sendAiGameOver(ClientThread sender) {
		String winner = sender.aiGame.getWinner();
		Message gameOverMsg = (winner == null) ? Message.gameDraw() : Message.gameOver(winner);
		sender.send(gameOverMsg);
		callback.accept("[AI Game] Game over! Result: " + (winner != null ? winner + " wins" : "Draw"));
		sender.aiGame = null;
		sender.ai = null;
		sender.inAiGame = false;
		broadcastLobbyUpdate();
	}

	private void checkAndSendGameOverIfNoMoves() {
		if (game == null) return;
		int current = game.getCurrentTurn();
		if (!game.hasAnyMove(current)) {
			int other = (current == CheckersGame.RED) ? CheckersGame.BLACK : CheckersGame.RED;
			Message gameOverMsg;
			if (!game.hasAnyMove(other)) {
				gameOverMsg = Message.gameDraw();
				callback.accept("Game over! Draw — neither player has moves.");
			} else {
				String winner = (current == CheckersGame.RED) ? game.getBlackPlayer() : game.getRedPlayer();
				gameOverMsg = Message.gameOver(winner);
				callback.accept("Game over! " + winner + " wins — opponent has no moves.");
			}
			player1.send(gameOverMsg);
			player2.send(gameOverMsg);
			game = null;
		}
	}

	synchronized void handleRematch(ClientThread sender) {
		if (player1 == null || player2 == null) return;
		if (sender == player1) player1WantsRematch = true;
		if (sender == player2) player2WantsRematch = true;

		ClientThread other = (sender == player1) ? player2 : player1;
		other.send(Message.rematchOffer(sender.username));
		callback.accept(sender.username + " wants a rematch.");

		if (player1WantsRematch && player2WantsRematch) {
			callback.accept("Both players agreed to rematch — starting new game.");
			startGame(player1, player2);
		}
	}

	synchronized void handleRematchDecline(ClientThread sender) {
		ClientThread other = (sender == player1) ? player2 : player1;
		if (other != null) other.send(Message.challengeDeclined(sender.username));
		player1 = null;
		player2 = null;
		game = null;
		player1WantsRematch = false;
		player2WantsRematch = false;
		broadcastLobbyUpdate();
		callback.accept(sender.username + " declined rematch. Both players returned to lobby.");
	}

	class ClientThread extends Thread {

		Socket connection;
		int count;
		ObjectInputStream in;
		ObjectOutputStream out;
		String username = null;

		// AI game state — per-client, not shared
		CheckersGame aiGame = null;
		CheckersAI ai = null;
		boolean inAiGame = false;

		ClientThread(Socket s, int count) {
			this.connection = s;
			this.count = count;
		}

		public void send(Message msg) {
			try {
				out.writeObject(msg);
				out.flush();
			} catch (Exception e) {
				callback.accept("Failed to send message to client #" + count);
			}
		}

		public void run() {
			try {
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());
				connection.setTcpNoDelay(true);
			} catch (Exception e) {
				System.out.println("Streams not open");
				return;
			}

			while (true) {
				try {
					Message msg = (Message) in.readObject();
					callback.accept("Client #" + count + " sent: " + msg);
					handleMessage(msg);
				} catch (Exception e) {
					callback.accept("Client #" + count + " (" + username + ") disconnected.");
					handleDisconnect();
					clients.remove(this);
					break;
				}
			}
		}

		private void handleMessage(Message msg) {
			switch (msg.getType()) {

				case set_username: {
					String requested = msg.getSenderUsername();
					boolean taken = false;
					for (ClientThread c : clients) {
						if (c != this && requested.equals(c.username)) { taken = true; break; }
					}
					if (taken) {
						send(Message.usernameTaken(requested));
					} else {
						username = requested;
						send(Message.usernameAccepted(username));
						callback.accept("Username set: " + username);
						broadcastLobbyUpdate();
					}
					break;
				}

				case start_ai_game: {
					String diffStr = msg.getContent();
					CheckersAI.Difficulty diff;
					try {
						diff = CheckersAI.Difficulty.valueOf(diffStr);
					} catch (Exception e) {
						diff = CheckersAI.Difficulty.MEDIUM;
					}
					startAiGame(this, diff);
					break;
				}

				case make_move: {
					// Route to AI handler or multiplayer handler
					if (inAiGame) {
						handleAiMove(this, msg);
					} else {
						handleMove(this, msg);
					}
					break;
				}

				case rematch_request: {
					// If they were in an AI game, restart it
					if (inAiGame || aiGame != null) {
						CheckersAI.Difficulty diff = (ai != null) ? ai.getDifficulty() : CheckersAI.Difficulty.MEDIUM;
						startAiGame(this, diff);
					} else {
						handleRematch(this);
					}
					break;
				}

				case rematch_decline: {
					if (inAiGame || aiGame != null) {
						// Clean up AI game and return to lobby
						aiGame = null;
						ai = null;
						inAiGame = false;
						broadcastLobbyUpdate();
					} else {
						handleRematchDecline(this);
					}
					break;
				}

				case challenge_send: {
					String targetName = msg.getRecipientUsername();
					ClientThread target = findClientByUsername(targetName);
					if (target == null) {
						send(Message.invalidMove("Player " + targetName + " not found."));
					} else {
						target.send(Message.challengeReceive(username));
						callback.accept(username + " challenged " + targetName);
					}
					break;
				}

				case challenge_accept: {
					String challengerName = msg.getRecipientUsername();
					ClientThread challenger = findClientByUsername(challengerName);
					if (challenger != null) {
						synchronized (Server.this) {
							startGame(challenger, this);
							broadcastLobbyUpdate();
						}
					}
					break;
				}

				case challenge_decline: {
					String challengerName = msg.getRecipientUsername();
					ClientThread challenger = findClientByUsername(challengerName);
					if (challenger != null) challenger.send(Message.challengeDeclined(username));
					callback.accept(username + " declined challenge from " + challengerName);
					break;
				}

				case chat: {
					synchronized (Server.this) {
						if (this == player1 && player2 != null) player2.send(msg);
						else if (this == player2 && player1 != null) player1.send(msg);
					}
					callback.accept("[Chat] " + msg.getSenderUsername() + ": " + msg.getContent());
					break;
				}

				default:
					callback.accept("Unknown message type: " + msg.getType());
			}
		}

		private ClientThread findClientByUsername(String name) {
			for (ClientThread c : clients) {
				if (name.equals(c.username)) return c;
			}
			return null;
		}

		private void handleDisconnect() {
			synchronized (Server.this) {
				// Clean up AI game
				if (inAiGame) {
					aiGame = null;
					ai = null;
					inAiGame = false;
				}
				// Clean up multiplayer game
				if (this == player1 || this == player2) {
					ClientThread other = (this == player1) ? player2 : player1;
					if (other != null) {
						other.send(Message.gameOverDisconnect(other.username != null ? other.username : "You"));
					}
					game = null;
					player1 = null;
					player2 = null;
					player1WantsRematch = false;
					player2WantsRematch = false;
				}
				broadcastLobbyUpdate();
			}
		}
	}
}