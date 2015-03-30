package jp.seraphyware.rmiexample;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {

	private static final long serialVersionUID = 1234L;

	private String message;

	private LocalDateTime time;

	private void writeObject(ObjectOutputStream outstm) throws IOException {
		// シリアライズが行われていることを示す
		System.out.println("★★writeObject★★");
		outstm.defaultWriteObject();
	}

	private void readObject(ObjectInputStream inpstm) throws IOException, ClassNotFoundException {
		inpstm.defaultReadObject();
		// デシリアライズが行われていることを示す
		System.out.println("★★readObject★★");
	}

	public void setTime(LocalDateTime time) {
		this.time = time;
	}

	public LocalDateTime getTime() {
		return time;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("(time=").append(time);
		buf.append(",message=").append(message);
		buf.append(")");
		return buf.toString();
	}
}
