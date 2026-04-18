import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class Message implements Serializable {
    static final long serialVersionUID = 42L;

    public enum MessageType{
        set_username,
        get_users,
        create_group,
        send_all,
        send_private,
        send_group,
        leave_group,
        add_to_group,

        username_accepted,
        username_taken,
        user_list,
        group_created,
        group_error,
        receive_message,
        server_info
    }

    private MessageType type;
    private String senderUsername;
    private String recipientUsername;
    private String groupName;
    private List<String> groupMembers;
    private List<String> userList;
    private String content;

    public Message(MessageType type){
        this.type = type;
        this.groupMembers = new ArrayList<>();
        this.userList = new ArrayList<>();
    }

    public static Message setUsername(String username){
        Message m = new Message(MessageType.set_username);
        m.senderUsername = username;
        return m;
    }

    public static Message usernameAccepted(String username){
        Message m = new Message(MessageType.username_accepted);
        m.content = username;
        return m;
    }

    public static Message usernameTaken(String username){
        Message m = new Message(MessageType.username_taken);
        m.content = username + " is already taken.";
        return m;
    }

    public static Message getUserList(String senderUsername){
        Message m = new Message(MessageType.get_users);
        m.senderUsername = senderUsername;
        return m;
    }

    public static Message userList(List<String> users){
        Message m = new Message(MessageType.user_list);
        m.userList = new ArrayList<>(users);
        return m;
    }

    public static Message createGroup(String senderUsername, String groupName, List<String> members){
        Message m = new Message(MessageType.create_group);
        m.senderUsername = senderUsername;
        m.groupName = groupName;
        m.groupMembers = new ArrayList<>(members);
        return m;
    }

    public static Message groupCreated(String groupName, List<String> members){
        Message m =  new Message(MessageType.group_created);
        m.groupName = groupName;
        m.groupMembers = new ArrayList<>(members);
        m.content = "Your group " + groupName + " has successfully been created! (" + members.size() + ") member(s).";
        return m;
    }

    public static Message groupError(String reason){
        Message m = new Message(MessageType.group_error);
        m.content = reason;
        return m;
    }

    public static Message sendAll(String senderUsername, String content){
        Message m = new Message(MessageType.send_all);
        m.senderUsername = senderUsername;
        m.content = content;
        return m;
    }

    public static Message sendPrivate(String senderUsername, String recipientUsername, String content){
        Message m = new Message(MessageType.send_private);
        m.senderUsername = senderUsername;
        m.recipientUsername = recipientUsername;
        m.content = content;
        return m;
    }

    public static Message sendGroup(String senderUsername, String groupName, String content){
        Message m = new Message(MessageType.send_group);
        m.senderUsername = senderUsername;
        m.groupName = groupName;
        m.content = content;
        return m;
    }

    public static Message leaveGroup(String senderUsername, String groupName) {
        Message m = new Message(MessageType.leave_group);
        m.senderUsername = senderUsername;
        m.groupName = groupName;
        return m;
    }

    public static Message addToGroup(String senderUsername, String groupName, List<String> newMembers) {
        Message m = new Message(MessageType.add_to_group);
        m.senderUsername = senderUsername;
        m.groupName = groupName;
        m.groupMembers = new ArrayList<>(newMembers);
        return m;
    }

    public static Message receiveMessage(String senderUsername, String context, String content){
        Message m = new Message(MessageType.receive_message);
        m.senderUsername = senderUsername;
        m.groupName = context;
        m.content = content;
        return m;
    }

    public static Message serverInfo(String info){
        Message m = new Message(MessageType.server_info);
        m.content = info;
        return m;
    }

    public MessageType getType(){
        return type;
    }

    public String getSenderUsername(){
        return senderUsername;
    }

    public String getRecipientUsername() {
        return recipientUsername;
    }

    public String getGroupName(){
        return groupName;
    }

    public List<String> getGroupMembers() {
        return groupMembers;
    }

    public List<String> getUserList(){
        return userList;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString(){
        return "[Message type=" + type + " sender=" + senderUsername + " recipient=" + recipientUsername + " group=" + groupName + " content=" + content + "]";
    }
}
