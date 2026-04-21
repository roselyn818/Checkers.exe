import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.function.Consumer;

public class Client extends Thread {

	Socket socketClient;
	ObjectOutputStream out;
	ObjectInputStream in;
	private Consumer<Serializable> callback;

	Client(Consumer<Serializable> call) {
		callback = call;
	}

	@Override
	public void run() {
		try {
			socketClient = new Socket("127.0.0.1", 5555);
			out = new ObjectOutputStream(socketClient.getOutputStream());
			out.flush();
			in = new ObjectInputStream(socketClient.getInputStream());
			socketClient.setTcpNoDelay(true);
		} catch (Exception e) {
			System.out.println("Could not connect to server.");
			return;
		}

		while (!isInterrupted()) {
			try {
				Message message = (Message) in.readObject();
				callback.accept(message);
			} catch (Exception e) {
				if (!isInterrupted()) callback.accept("Disconnected from server.");
				break;
			}
		}

		try { if (socketClient != null) socketClient.close(); } catch (Exception ignored) {}
	}

	public void send(Message data) {
		try {
			out.writeObject(data);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}