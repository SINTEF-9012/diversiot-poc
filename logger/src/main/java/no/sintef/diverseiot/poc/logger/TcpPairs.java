package no.sintef.diverseiot.poc.logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TcpPairs extends Handler {
	private Selector acceptSelector;
	private Selector readSelector;
	
	public final int startPort;
	public final int numPorts;
	public final String name;
	
	public TcpPairs(int startPort, int numPorts, String name) {
		this.startPort = startPort;
		this.numPorts = numPorts;
		this.name = name;
	}
	
	private void acceptConnections() throws IOException {
		if (acceptSelector.isOpen() && acceptSelector.selectNow() > 0) {
			for (SelectionKey key : acceptSelector.selectedKeys()) {
				if (key.isValid() && key.isAcceptable()) {
					ServerSocketChannel server = (ServerSocketChannel)key.channel();
					SocketChannel client = server.accept();
					client.configureBlocking(false);
					if (client != null) {						
						@SuppressWarnings("unchecked")
						Set<SocketChannel> clients = (Set<SocketChannel>)key.attachment();
						clients.add(client);
						client.register(readSelector, SelectionKey.OP_READ, clients);
					}
				}
			}
			acceptSelector.selectedKeys().clear();
		}
	}
	
	private void forwardData() throws IOException {
		if (readSelector.isOpen() && readSelector.selectNow() > 0) {
			for (SelectionKey key : readSelector.selectedKeys()) {
				if (key.isValid() && key.isReadable()) {
					SocketChannel client = (SocketChannel)key.channel();
					@SuppressWarnings("unchecked")
					Set<SocketChannel> clients = (Set<SocketChannel>)key.attachment();
					ByteBuffer buf = ByteBuffer.allocate(1024);
					
					// Check if the socket is still open and readable
					if (client.isConnected() && client.read(buf) >= 0) {
						buf.flip();
						List<SocketChannel> closed = new ArrayList<SocketChannel>();
						for (SocketChannel other : clients) {
							if (client == other) continue;
							buf.rewind();
							try {
								while (buf.position() < buf.limit()-1) {
									other.write(buf);
								}
							} catch (IOException e) {
								closed.add(other);
								other.socket().close();
							}
						}
						clients.removeAll(closed);
						
						logData(name, client.socket().getInetAddress().getHostAddress(), Integer.toString(client.socket().getLocalPort()), buf);
					} else {
						// Just remove this socket from the set
						clients.remove(client);
						client.close();
					}					
				}
			}
			readSelector.selectedKeys().clear();
		}
	}
	
	public void start() throws IOException {
		acceptSelector = Selector.open();
		readSelector = Selector.open();
		for (int p = startPort; p < startPort+numPorts; p++) {
			// Create listening port
			ServerSocketChannel server = ServerSocketChannel.open();
			server.configureBlocking(false);
			server.bind(new InetSocketAddress(p));
			// Each port needs to keep a list of clients connected
			server.register(acceptSelector, SelectionKey.OP_ACCEPT, new HashSet<SocketChannel>());
		}
	}

	public void handle() throws IOException {
		acceptConnections();
		forwardData();
	}

	public void stop() throws IOException {
		// TODO Auto-generated method stub
		
	}

}
