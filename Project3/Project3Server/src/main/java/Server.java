import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;

public class Server {

	int count = 1;
	ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
	TheServer server;
	private Consumer<Serializable> callback;

	// Game state
	ClientThread player1 = null;
	ClientThread player2 = null;
	CheckersGame game = null;

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

	synchronized void tryStartGame() {
		if (player1 != null && player2 != null && game == null) {
			game = new CheckersGame(player1.username, player2.username);
			callback.accept("Game started: " + player1.username + " (RED) vs " + player2.username + " (BLACK)");

			Message startMsg = Message.gameStart(player1.username, player2.username, game.getBoard());
			player1.send(startMsg);
			player2.send(startMsg);
		}
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
			Message gameOverMsg = Message.gameOver(game.getWinner());
			player1.send(gameOverMsg);
			player2.send(gameOverMsg);
			callback.accept("Game over! Winner: " + game.getWinner());
			game = null;
			player1 = null;
			player2 = null;
		} else {
			Message stateMsg = Message.gameState(game.getBoard(), game.getCurrentTurn(), game.getRedPlayer(), game.getBlackPlayer(), game.getMultiJumpRow(), game.getMultiJumpCol());
			player1.send(stateMsg);
			player2.send(stateMsg);
		}
	}

	class ClientThread extends Thread {

		Socket connection;
		int count;
		ObjectInputStream in;
		ObjectOutputStream out;
		String username = null;

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
					// Simple check: make sure no one else has this name
					boolean taken = false;
					for (ClientThread c : clients) {
						if (c != this && requested.equals(c.username)) {
							taken = true;
							break;
						}
					}
					if (taken) {
						send(Message.usernameTaken(requested));
					} else {
						username = requested;
						send(Message.usernameAccepted(username));
						callback.accept("Username set: " + username);
					}
					break;
				}

				case join_game: {
					synchronized (Server.this) {
						if (player1 == null) {
							player1 = this;
							send(Message.waitingForOpponent());
							callback.accept(username + " is waiting for opponent.");
						} else if (player2 == null && player1 != this) {
							player2 = this;
							tryStartGame();
						} else {
							send(Message.invalidMove("Game is already full or you already joined."));
						}
					}
					break;
				}

				case make_move: {
					handleMove(this, msg);
					break;
				}

				case chat: {
					synchronized (Server.this) {
						if (this == player1 && player2 != null) {
							player2.send(msg);
						} else if (this == player2 && player1 != null) {
							player1.send(msg);
						}
					}
					callback.accept("[Chat] " + msg.getSenderUsername() + ": " + msg.getContent());
					break;
				}

				default:
					callback.accept("Unknown message type: " + msg.getType());
			}
		}

		private void handleDisconnect() {
			synchronized (Server.this) {
				if (this == player1 || this == player2) {
					// Notify the other player
					ClientThread other = (this == player1) ? player2 : player1;
					if (other != null) {
						other.send(Message.gameOverDisconnect(other.username != null ? other.username : "You"));
					}
					game = null;
					player1 = null;
					player2 = null;
				}
			}
		}
	}
}