package chat;

import java.io.InputStreamReader;
import java.util.List;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.text.StringEscapeUtils;

import com.google.gson.Gson;

@Path("/")
public class API {
	
	private static final String indexhtml;
	static {
		System.out.println("Loading index.html");
		StringBuilder sb = new StringBuilder();
		try (Scanner scan = new Scanner(new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("index.html")))) {
			while (scan.hasNext())
				sb.append(scan.nextLine() + System.lineSeparator());
		}
		indexhtml = sb.toString();
		System.out.println("Finished loading index.html");
	}
	
	@GET
	@Produces(MediaType.TEXT_HTML)
	public static Response indexhtml() {
		System.out.println("index");
		return Response.ok(indexhtml).build();
	}
	
	private static class ChatMessage implements Comparable<ChatMessage> {
		public final long time = System.currentTimeMillis();
		public final String chatid;
		public final String username;
		public final String message;
		
		public ChatMessage(String chatid, String username, String message) {
			this.chatid = chatid;
			this.username = username;
			this.message = message;
		}
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((chatid == null) ? 0 : chatid.hashCode());
			result = prime * result + ((message == null) ? 0 : message.hashCode());
			result = prime * result + (int) (time ^ (time >>> 32));
			result = prime * result + ((username == null) ? 0 : username.hashCode());
			return result;
		}
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (!(obj instanceof ChatMessage)) return false;
			ChatMessage other = (ChatMessage) obj;
			if (chatid == null) {
				if (other.chatid != null) return false;
			} else if (!chatid.equals(other.chatid)) return false;
			if (message == null) {
				if (other.message != null) return false;
			} else if (!message.equals(other.message)) return false;
			if (time != other.time) return false;
			if (username == null) {
				if (other.username != null) return false;
			} else if (!username.equals(other.username)) return false;
			return true;
		}
		public int compareTo(ChatMessage that) {
			int comp = Long.compare(this.time, that.time);
			if (comp != 0) return comp;
			comp = this.chatid.compareTo(that.chatid);
			if (comp != 0) return comp;
			comp = this.username.compareTo(that.username);
			if (comp != 0) return comp;
			return this.message.compareTo(that.message);
		}
	}
	
	private static TreeSet<ChatMessage> messages = new TreeSet<>();
	private static ReadWriteLock lock = new ReentrantReadWriteLock();
	
	@POST
	@Path("/submit")
	public static Response submit(@FormParam("chatid") String chatid, @FormParam("username") String username, @FormParam("input") String input) {
		System.out.printf("[%s] %s%n", username, input);
		
		lock.writeLock().lock();
		try {
			messages.add(new ChatMessage(chatid, username, input));
			while (messages.size() > 100) messages.remove(messages.first());
		} finally {
			lock.writeLock().unlock();
		}
		//messages.removeIf(cm -> cm.time < System.currentTimeMillis()-(1000*60*60));
		return Response.ok().build();
	}
	
	@SuppressWarnings("unused")
	private static class NewMessages {
		public final List<NewMessage> messages;
		public final long time;
		
		public NewMessages(List<ChatMessage> inputMessages, long time) {
			this.messages = inputMessages.stream().map(cm->new NewMessage(cm)).collect(Collectors.toList());
			this.time = time;
		}
		
		private static class NewMessage {
			public final long time;
			public final String username;
			public final String message;
			public NewMessage(ChatMessage cm) {
				this.time=cm.time;
				this.username=StringEscapeUtils.escapeHtml4(cm.username);
				this.message=StringEscapeUtils.escapeHtml4(cm.message);
			}
		}
	}
		
	@GET
	@Path("/update")
	@Produces(MediaType.APPLICATION_JSON)
	public static Response update(@QueryParam("chatid") String chatid, @QueryParam("timestamp") long timestamp) {
		lock.readLock().lock();
		try {
			return Response.ok(new Gson().toJson(
				new NewMessages(messages.stream().filter(m->/*m.chatid.compareTo(chatid) != 0 &&*/ m.time > timestamp).collect(Collectors.toList()),System.currentTimeMillis())
			)).build();
		} finally {
			lock.readLock().unlock();
		}
	}
}