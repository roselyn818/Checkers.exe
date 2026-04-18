
import java.util.HashMap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class GuiClient extends Application{

	java.util.List<String> allMessages = new ArrayList<>();
	TextField messageField;
	Button sendBtn;
	Button leaveBtn;
	Button addUserBtn;
	HashMap<String, Scene> sceneMap;
	VBox clientBox;
	Client clientConnection;
	
	ListView<String> listItems2;
	ListView<String> userListView;
	ComboBox<String> recipientBox;
	String username = null;
	
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {

		listItems2 = new ListView<String>();
		userListView = new ListView<String>();
		recipientBox = new ComboBox<String>();
		recipientBox.getItems().add("ALL");
		recipientBox.setValue("ALL");

		clientConnection = new Client(data -> {
			Platform.runLater(() -> {
				handleIncoming((Message) data);
			});
		});

		clientConnection.start();

		Platform.runLater(() -> askForUsername(primaryStage));

		messageField = new TextField();
		sendBtn = new Button("Send");
		sendBtn.setOnAction(e -> sendMessage());
		messageField.setOnAction(e -> sendMessage());

		sceneMap = new HashMap<String, Scene>();
		sceneMap.put("client", createClientGui());

		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent t) {
				Platform.exit();
				System.exit(0);
			}
		});

		primaryStage.setScene(sceneMap.get("client"));
		primaryStage.setTitle("Client");
		primaryStage.show();
	}


	private void askForUsername(Stage owner) {
		askForUsername(owner, null);
	}

	private void askForUsername(Stage owner, String errorMsg) {
		Stage dialog = new Stage();
		dialog.setTitle("Choose Username");
		dialog.initOwner(owner);

		TextField nameField = new TextField();
		nameField.setPromptText("Enter username");
		Button okBtn = new Button("Join");
		Label errLabel = new Label(errorMsg != null ? errorMsg : "");
		errLabel.setStyle("-fx-text-fill: red;");

		okBtn.setOnAction(e -> {
			String name = nameField.getText().trim();
			if (name.isEmpty()) {
				errLabel.setText("Username cannot be empty.");
				return;
			}
			okBtn.setDisable(true);
			clientConnection.send(Message.setUsername(name));
			dialog.setUserData("pending");
		});

		nameField.setOnAction(e -> okBtn.fire());

		VBox box = new VBox(10,
				new Label("Choose a username:"),
				nameField, okBtn, errLabel);
		box.setPadding(new Insets(20));

		dialog.setScene(new Scene(box, 300, 150));
		dialog.setUserData(dialog);
		owner.setUserData(dialog);
		dialog.show();
	}

	private void handleIncoming(Message msg) {
		switch (msg.getType()) {

			case username_accepted: {
				username = msg.getContent();
				Stage owner = (Stage) sceneMap.get("client")
						.getWindow();
				Object ud = owner.getUserData();
				if (ud instanceof Stage) ((Stage) ud).close();
				owner.setTitle("Client — " + username);
				clientConnection.send(Message.getUserList(username));
				break;
			}

			case username_taken: {
				Stage owner = (Stage) sceneMap.get("client").getWindow();
				Object ud = owner.getUserData();
				if (ud instanceof Stage) ((Stage) ud).close();
				askForUsername(owner, msg.getContent());
				break;
			}

			case user_list: {
				userListView.getItems().setAll(msg.getUserList());
				String current = recipientBox.getValue();
				List<String> existingGroups = new ArrayList<>();
				for (String item : recipientBox.getItems()) {
					if (item.startsWith("GROUP:")) existingGroups.add(item);
				}

				recipientBox.getItems().clear();
				recipientBox.getItems().add("ALL");
				for (String u : msg.getUserList()) {
					if (!u.equals(username)) recipientBox.getItems().add(u);
				}

				for (String g : existingGroups) {
					recipientBox.getItems().add(g);
				}

				recipientBox.setValue(
						recipientBox.getItems().contains(current) ? current : "ALL");
				break;
			}

			case group_created: {
				String gKey = "GROUP:" + msg.getGroupName();
				if (!recipientBox.getItems().contains(gKey))
					recipientBox.getItems().add(gKey);

				if (msg.getGroupMembers().size() > 0 &&
						msg.getGroupMembers().get(0).equals(username)) {
					addMessage("[Server] " + msg.getContent());
					Alert alert = new Alert(Alert.AlertType.INFORMATION);
					alert.setTitle("Group Created");
					alert.setHeaderText(null);
					alert.setContentText("Group '" + msg.getGroupName() + "' created!");
					alert.show();
				}
				break;
			}

			case group_error:
			case server_info: {
				String content = msg.getContent();
				addMessage("[Server] " + content);

				if (content.contains("added you to group")) {
					Alert alert = new Alert(Alert.AlertType.INFORMATION);
					alert.setTitle("Added to Group");
					alert.setHeaderText(null);
					alert.setContentText(content);
					alert.show();
				}
				break;
			}

			case receive_message: {
				String context = msg.getGroupName();
				String from    = msg.getSenderUsername();
				String body    = msg.getContent();
				String tag;

				if ("ALL".equals(context)) {
					tag = "[ALL]";
				} else if ("PRIVATE".equals(context)) {
					tag = "[PRIVATE from " + from + "]";
					if (!recipientBox.getItems().contains(from)) {
						recipientBox.getItems().add(from);
					}
				} else if (context != null && context.startsWith("PRIVATE→")) {
					tag = "[PRIVATE to " + context.substring(8) + "]";
				} else if (context != null && context.startsWith("GROUP:")) {
					tag = "[GROUP:" + context.substring(6) + "]";
				} else {
					tag = "[?]";
				}
				addMessage(tag + " " + from + ": " + body);
				break;
			}

			default:
				addMessage("[?] " + msg);
		}
	}


	private void sendMessage() {
		if (username == null) return;
		String text = messageField.getText().trim();
		if (text.isEmpty()) return;
		String recipient = recipientBox.getValue();
		if (recipient == null || recipient.equals("ALL")) {
			clientConnection.send(Message.sendAll(username, text));
			addMessage("[ALL] " + username + ": " + text);
		} else if (recipient.startsWith("GROUP:")) {
			String groupName = recipient.substring(6);
			clientConnection.send(Message.sendGroup(username, groupName, text));
			addMessage("[GROUP:" + groupName + "] " + username + ": " + text);
		} else {
			clientConnection.send(Message.sendPrivate(username, recipient, text));
			addMessage("[PRIVATE to " + recipient + "] " + username + ": " + text);
		}
		messageField.clear();
	}


	private void showCreateGroupDialog(Stage owner) {
		Stage dialog = new Stage();
		dialog.setTitle("Create Group");
		dialog.initOwner(owner);

		TextField groupNameField = new TextField();
		groupNameField.setPromptText("Group name");
		TextArea membersArea = new TextArea();
		membersArea.setPromptText("One username per line");
		membersArea.setPrefRowCount(4);
		Button createBtn = new Button("Create");
		Label err = new Label("");
		err.setStyle("-fx-text-fill: red;");

		createBtn.setOnAction(e -> {
			String gName = groupNameField.getText().trim();
			if (gName.isEmpty()) { err.setText("Group name required."); return; }
			java.util.List<String> members = new java.util.ArrayList<>();
			for (String line : membersArea.getText().split("\\n")) {
				String m = line.trim();
				if (!m.isEmpty()) members.add(m);
			}
			if (!members.contains(username)) members.add(0, username);
			clientConnection.send(Message.createGroup(username, gName, members));
			dialog.close();
		});

		VBox box = new VBox(8,
				new Label("Group name:"), groupNameField,
				new Label("Members:"), membersArea,
				createBtn, err);
		box.setPadding(new Insets(16));

		dialog.setScene(new Scene(box, 280, 260));
		dialog.show();
	}

	private void addMessage(String msg) {
		allMessages.add(msg);
		String selected = recipientBox.getValue();
		if (selected != null) selected = selected.replace(" (!)", "");
		String msgWindow = null;
		if (msg.startsWith("[ALL] "))                    msgWindow = "ALL";
		else if (msg.startsWith("[PRIVATE from ")) {
			String rest = msg.substring("[PRIVATE from ".length());
			msgWindow = rest.substring(0, rest.indexOf("]"));
		} else if (msg.startsWith("[PRIVATE to ")) {
			String rest = msg.substring("[PRIVATE to ".length());
			msgWindow = rest.substring(0, rest.indexOf("]"));
		} else if (msg.startsWith("[GROUP:")) {
			String rest = msg.substring("[GROUP:".length());
			msgWindow = "GROUP:" + rest.substring(0, rest.indexOf("]"));
		}

		if (msgWindow != null && !msgWindow.equals(selected)) {
			markUnread(msgWindow);
		}

		if (selected == null || selected.equals("ALL")) {
			filterMessages("ALL");
		} else if (selected.startsWith("GROUP:")) {
			filterMessages("GROUP:" + selected.substring(6));
		} else {
			filterMessages(selected);
		}
	}

	private void filterMessages(String filter) {
		listItems2.getItems().clear();
		if (filter == null || filter.equals("ALL MESSAGES")) {
			listItems2.getItems().addAll(allMessages);
		} else if (filter.equals("ALL")) {
			for (String m : allMessages) {
				if (m.startsWith("[ALL] ")) {
					listItems2.getItems().add(m.substring("[ALL] ".length()));
				} else if (m.startsWith("[Server]") && m.contains("[GLOBAL]")) {
					listItems2.getItems().add(m);
				}
			}
		} else if (filter.startsWith("GROUP:")) {
			String groupName = filter.substring(6);
			String prefix = "[GROUP:" + groupName + "] ";
			for (String m : allMessages) {
				if (m.startsWith(prefix)) {
					listItems2.getItems().add(m.substring(prefix.length()));
				} else if (m.startsWith("[Server]")
						&& !m.contains("[GLOBAL]")
						&& m.contains(groupName)) {
					listItems2.getItems().add(m);
				}
			}
		} else {
			String toPrefix   = "[PRIVATE to " + filter + "] ";
			String fromPrefix = "[PRIVATE from " + filter + "] ";
			for (String m : allMessages) {
				if (m.startsWith(toPrefix)) {
					listItems2.getItems().add(m.substring(toPrefix.length()));
				} else if (m.startsWith(fromPrefix)) {
					listItems2.getItems().add(m.substring(fromPrefix.length()));
				}
			}
		}
	}

	private void showAddUserDialog(Stage owner, String groupName) {
		Stage dialog = new Stage();
		dialog.setTitle("Add User to " + groupName);
		dialog.initOwner(owner);

		TextArea usersArea = new TextArea();
		usersArea.setPromptText("One username per line");
		usersArea.setPrefRowCount(4);
		Button addBtn = new Button("Add");
		Label err = new Label("");
		err.setStyle("-fx-text-fill: red;");

		addBtn.setOnAction(e -> {
			List<String> newMembers = new ArrayList<>();
			for (String line : usersArea.getText().split("\\n")) {
				String m = line.trim();
				if (!m.isEmpty()) newMembers.add(m);
			}
			if (newMembers.isEmpty()) { err.setText("Enter at least one username."); return; }
			clientConnection.send(Message.addToGroup(username, groupName, newMembers));
			dialog.close();
		});

		VBox box = new VBox(8,
				new Label("Add users to group '" + groupName + "':"),
				usersArea, addBtn, err);
		box.setPadding(new Insets(16));
		dialog.setScene(new Scene(box, 280, 220));
		dialog.show();
	}

	private void markUnread(String window) {
		for (int i = 0; i < recipientBox.getItems().size(); i++) {
			String item = recipientBox.getItems().get(i);
			String base = item.replace(" (!)", "");
			if (base.equals(window) && !item.contains("(!)")) {
				recipientBox.getItems().set(i, item + " (!)");
				break;
			}
		}
	}

	public Scene createClientGui() {
		HBox sendRow = new HBox(6, recipientBox, messageField, sendBtn);
		HBox.setHgrow(messageField, Priority.ALWAYS);

		Button refreshBtn = new Button("Refresh Users");
		Button groupBtn   = new Button("Create Group");
		leaveBtn = new Button("Leave Group");
		addUserBtn = new Button("Add User to Group");

		leaveBtn.setMaxWidth(Double.MAX_VALUE);
		addUserBtn.setMaxWidth(Double.MAX_VALUE);

		leaveBtn.setVisible(false);
		addUserBtn.setVisible(false);
		leaveBtn.setManaged(false);
		addUserBtn.setManaged(false);

		refreshBtn.setMaxWidth(Double.MAX_VALUE);
		groupBtn.setMaxWidth(Double.MAX_VALUE);
		leaveBtn.setMaxWidth(Double.MAX_VALUE);

		VBox rightPane = new VBox(6,
				new Label("Online Users:"),
				userListView,
				refreshBtn,
				groupBtn,
				leaveBtn,
				addUserBtn);
		rightPane.setPrefWidth(160);
		rightPane.setPadding(new Insets(6));

		refreshBtn.setOnAction(e -> {
			if (username != null)
				clientConnection.send(Message.getUserList(username));
		});
		groupBtn.setOnAction(e -> {
			Stage owner = (Stage) sceneMap.get("client").getWindow();
			showCreateGroupDialog(owner);
		});
		leaveBtn.setOnAction(e -> {
			String selected = recipientBox.getValue();
			if (selected == null || !selected.startsWith("GROUP:")) {
				addMessage("[!] Select a group first.");
				return;
			}
			String groupName = selected.substring(6);
			clientConnection.send(Message.leaveGroup(username, groupName));
			recipientBox.getItems().remove(selected);
			recipientBox.setValue("ALL");
		});

		recipientBox.setOnAction(e -> {
			String selected = recipientBox.getValue();
			if (selected == null) return;

			if (selected.contains("(!)")) {
				String clean = selected.replace(" (!)", "");
				int idx = recipientBox.getItems().indexOf(selected);
				recipientBox.getItems().set(idx, clean);
				recipientBox.setValue(clean);
				selected = clean;
			}

			boolean isGroup = selected.startsWith("GROUP:");
			leaveBtn.setVisible(isGroup);
			leaveBtn.setManaged(isGroup);
			addUserBtn.setVisible(isGroup);
			addUserBtn.setManaged(isGroup);

			if (selected.equals("ALL")) {
				filterMessages("ALL");
			} else if (isGroup) {
				filterMessages("GROUP:" + selected.substring(6));
			} else {
				filterMessages(selected);
			}
		});

		addUserBtn.setOnAction(e -> {
			String selected = recipientBox.getValue();
			if (selected == null || !selected.startsWith("GROUP:")) return;
			showAddUserDialog((Stage) sceneMap.get("client").getWindow(), selected.substring(6));
		});

		VBox centerPane = new VBox(6, listItems2, sendRow);
		VBox.setVgrow(listItems2, Priority.ALWAYS);
		centerPane.setPadding(new Insets(6));

		BorderPane root = new BorderPane();
		root.setCenter(centerPane);
		root.setRight(rightPane);
		root.setStyle("-fx-background-color: blue; -fx-font-family: 'serif';");

		return new Scene(root, 600, 400);
	}
}
