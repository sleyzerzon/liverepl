package net.djpowell.liverepl.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Logger;

public class Console {

	private static String NEWLINE = System.getProperty("line.separator");
	
	public static void main(InetAddress host, int port) throws Exception {
		final BufferedReader cin = new BufferedReader(new InputStreamReader(System.in));
		final Writer cout = new OutputStreamWriter(System.out);
		Socket s = new Socket(host, port);
		try {
			final Thread main = Thread.currentThread();
			final Reader sin = new InputStreamReader(s.getInputStream());
			final Writer sout = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));

			Thread ch = new Thread("ConsoleHandler") {
				@Override
				public void run() {
					try {
						for (;;) {
							// use line-based i/o for reading from the keyboard
							String line = cin.readLine();
							if (line == null) break;
							sout.write(line + NEWLINE);
							sout.flush();
						}
					} catch (IOException e) {
						TRC.fine(e.getMessage());
					} finally {
						main.interrupt();
					}
				}
			};
			ch.start();

			Thread sh = new Thread("SocketHandler") {
				@Override
				public void run() {
					try {
						for (;;) {
							// use character based i/o for printing server responses
							int ic = sin.read();
							if (ic == -1) break;
							char c = (char) ic;
							cout.write(c);
							cout.flush();
						}
					} catch (IOException e) {
						TRC.fine(e.getMessage());
					} finally {
						main.interrupt();
					}
				}
			};
			sh.start();
		
			// block until both threads finish
			try {
				sh.join();
				ch.join();
			} catch (InterruptedException e) {
				s.close();
			}
		} finally {
			s.close();
		}
	}
	
	private static final Logger TRC = Logger.getLogger(Console.class.getName()); 

}