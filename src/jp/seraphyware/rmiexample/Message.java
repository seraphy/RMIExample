package jp.seraphyware.rmiexample;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {

	private static final long serialVersionUID = -4450957666222313052L;

	private String message;

	private LocalDateTime time;

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
