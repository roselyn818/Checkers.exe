import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import javafx.application.Platform;
import javafx.scene.control.ListView;
/*
 * Clicker: A: I really get it    B: No idea what you are talking about
 * C: kind of following
 */

public class Server {

	int count = 1;
	ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
	TheServer server;
	private Consumer<Serializable> callback;

	Map<String, ClientThread> userMap = new HashMap<>();
	Map<String, List<String>> groups = new HashMap<>();

	Server(Consumer<Serializable> call) {

		callback = call;
		server = new TheServer();
		server.start();
	}


	public class TheServer extends Thread {

		public void run() {

			try (ServerSocket mysocket = new ServerSocket(5555);) {
				System.out.println("Server is waiting for a client!");


				while (true) {

					ClientThread c = new ClientThread(mysocket.accept(), count);
					callback.accept("client has connected to server: " + "client #" + count);
					clients.add(c);
					c.start();

					count++;

				}
			}//end of try
			catch (Exception e) {
				callback.accept("Server socket did not launch");
			}
		}//end of while
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

		public void broadcastAll(Message message) {
			for (ClientThread t : clients) {
				try {
					t.out.writeObject(message);
					t.out.flush();
				} catch (Exception e) {
				}
			}
		}

		public boolean sendTo(String targetUsername, Message message) {
			ClientThread target = userMap.get(targetUsername);
			if (target == null) return false;
			try {
				target.out.writeObject(message);
				target.out.flush();
				return true;
			} catch (Exception e) {
				return false;
			}
		}

		public void broadcastUserList() {
			List<String> names = new ArrayList<>(userMap.keySet());
			Message ul = Message.userList(names);
			broadcastAll(ul);
		}

		public void run() {
			try {
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());
				connection.setTcpNoDelay(true);
			} catch (Exception e) {
				System.out.println("Streams not open");
			}

			while (true) {
				try {
					Message msg = (Message) in.readObject();
					callback.accept("client #" + count + " sent: " + msg);
					handleMessage(msg);

				} catch (Exception e) {
					callback.accept("Client #" + count + " (" + username + ") disconnected.");
					if (username != null) {
						userMap.remove(username);
						broadcastUserList();
						broadcastAll(Message.serverInfo("[GLOBAL] " + username + " has left"));
					}
					clients.remove(this);
					break;
				}
			}
		} // end of run

		private void handleMessage(Message msg) throws IOException {
			switch (msg.getType()) {

				case set_username: {
					String requested = msg.getSenderUsername();
					synchronized (userMap) {
						if (userMap.containsKey(requested)) {
							out.writeObject(Message.usernameTaken(requested));
							out.flush();
						} else {
							username = requested;
							userMap.put(username, this);
							out.writeObject(Message.usernameAccepted(username));
							out.flush();
							broadcastUserList();
							broadcastAll(Message.serverInfo("[GLOBAL] " + username + " has joined"));
							callback.accept("Username set: " + username);
						}
					}
					break;
				}

				case get_users: {
					List<String> names = new ArrayList<>(userMap.keySet());
					out.writeObject(Message.userList(names));
					out.flush();
					break;
				}

				case create_group: {
					String groupName = msg.getGroupName();
					List<String> members = msg.getGroupMembers();
					synchronized (groups) {
						if (groups.containsKey(groupName)) {
							out.writeObject(Message.groupError("Group '" + groupName + "' already exists."));
							out.flush();
						} else {
							for (String m : members) {
								if (!userMap.containsKey(m)) {
									out.writeObject(Message.groupError("User '" + m + "' is not connected."));
									out.flush();
									return;
								}
							}
							groups.put(groupName, members);
							callback.accept("Group created: " + groupName + " " + members);
							// Notify other members
							Message notify = Message.groupCreated(groupName, members);
							for (String member : members) {
								sendTo(member, notify);
							}
						}
					}
					break;
				}

				case send_all: {
					String content = msg.getContent();
					Message delivery = Message.receiveMessage(username, "ALL", content);
					for (Map.Entry<String, ClientThread> entry : userMap.entrySet()) {
						if (!entry.getKey().equals(username)) {
							sendTo(entry.getKey(), delivery);
						}
					}
					callback.accept("[ALL] " + username + ": " + content);
					break;
				}

				case send_private: {
					String recipient = msg.getRecipientUsername();
					String content = msg.getContent();
					boolean sent = sendTo(recipient, Message.receiveMessage(username, "PRIVATE", content));
					if (!sent) {
						out.writeObject(Message.serverInfo("User '" + recipient + "' is not connected."));
						out.flush();
					} else {
						callback.accept("[PRIVATE] " + username + " to " + recipient + ": " + content);
					}
					break;
				}

				case send_group: {
					String groupName = msg.getGroupName();
					String content = msg.getContent();
					List<String> members = groups.get(groupName);
					if (members == null) {
						out.writeObject(Message.serverInfo("Group '" + groupName + "' does not exist."));
						out.flush();
					} else {
						Message delivery = Message.receiveMessage(username, "GROUP:" + groupName, content);
						for (String member : members) {
							if (!member.equals(username)) {
								sendTo(member, delivery);
							}
						}
						callback.accept("[GROUP:" + groupName + "] " + username + ": " + content);
					}
					break;
				}

				case leave_group: {
					String groupName = msg.getGroupName();
					synchronized (groups) {
						List<String> members = groups.get(groupName);
						if (members == null) {
							out.writeObject(Message.serverInfo("Group '" + groupName + "' does not exist."));
							out.flush();
						} else {
							members.remove(username);
							// only notify remaining group members
							for (String member : members) {
								sendTo(member, Message.serverInfo(username + " has left group '" + groupName + "'"));
							}
							// confirm only to the leaver
							out.writeObject(Message.serverInfo("You have left group '" + groupName + "'"));
							out.flush();
							callback.accept(username + " left group: " + groupName);
							if (members.isEmpty()) {
								groups.remove(groupName);
								callback.accept("Group deleted (empty): " + groupName);
							}
						}
					}
					break;
				}

				case add_to_group: {
					String groupName = msg.getGroupName();
					List<String> newMembers = msg.getGroupMembers();
					synchronized (groups) {
						List<String> members = groups.get(groupName);
						if (members == null) {
							out.writeObject(Message.serverInfo("Group '" + groupName + "' does not exist."));
							out.flush();
						} else {
							for (String m : newMembers) {
								if (!userMap.containsKey(m)) {
									out.writeObject(Message.serverInfo("User '" + m + "' is not connected."));
									out.flush();
									return;
								}
								if (!members.contains(m)) {
									members.add(m);
									// send groupCreated so their recipientBox updates
									sendTo(m, Message.groupCreated(groupName, members));
									// only send "added you" to the new member
									sendTo(m, Message.serverInfo(username + " added you to group '" + groupName + "'"));
								}
							}
							String memberNames = String.join(", ", newMembers);
							for (String member : members) {
								if (!member.equals(username) && !newMembers.contains(member)) {
									sendTo(member, Message.serverInfo(username + " added " + memberNames + " to group '" + groupName + "'"));
								}
							}
							// confirm to the adder only
							out.writeObject(Message.serverInfo("Added " + memberNames + " to group '" + groupName + "'"));
							out.flush();
							callback.accept(username + " added " + memberNames + " to group: " + groupName);
						}
					}
					break;
				}

				default:
					callback.accept("Unknown message type: " + msg.getType());
			}
		}
	} // end of client thread
}


	
	

	
