package org.araqne.console;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.araqne.ansicode.CursorPosCode;
import org.araqne.ansicode.EraseLineCode;
import org.araqne.ansicode.EraseLineCode.Option;
import org.araqne.ansicode.MoveCode;
import org.araqne.api.FunctionKeyEvent;
import org.araqne.api.FunctionKeyEvent.KeyCode;
import org.araqne.api.FunctionKeyEventListener;
import org.araqne.api.ScriptContext;
import org.araqne.api.ScriptOutputStream;

class ReadLineHandler implements FunctionKeyEventListener {
	private ScriptContext context;
	private BlockingQueue<Character> buffer;

	private String screen = "";
	private String input = "";

	// character unit
	private int screenCursor = 0;
	private int inputCursor = 0;

	// history
	private LinkedList<String> history;
	private int historyIndex;

	public ReadLineHandler(ScriptContext context) {
		this.context = context;
		this.buffer = new LinkedBlockingQueue<Character>();
		this.history = new LinkedList<String>();
		resetIndex();
	}

	public String getLine() throws InterruptedException {
		ScriptOutputStream out = context.getOutputStream();

		try {
			while (true) {
				char c = read();

				synchronized (this) {
					if (c == '\r' || c == '\n') {
						// insert at the front
						if (history.size() > 1 && history.get(0).length() == 0)
							history.removeFirst();
						if (history.size() < 1 || !history.get(0).equals(screen)) {
							history.addFirst(screen);
						}
						resetIndex();

						out.println("");
						return screen;
					}

					String left = input.substring(0, screenCursor);
					String right = input.substring(screenCursor);

					input = left + c + right;
					inputCursor++;

					sync();
				}
			}
		} finally {
			screen = "";
			input = "";
			screenCursor = 0;
			inputCursor = 0;
		}
	}

	private void sync() {
		try {
			ScriptOutputStream os = context.getOutputStream();
			int boundary = 0;

			for (; boundary < Math.min(screen.length(), input.length()); boundary++)
				if (screen.charAt(boundary) != input.charAt(boundary))
					break;

			// delete partial screen output

			// new cursor physical position
			int diffPos = getPhysicalPoint(screen, 0, boundary);
			int oldPos = getPhysicalPoint(screen, 0, screen.length());

			int oldCursorPos = getPhysicalPoint(screen, 0, screenCursor);
			int newCursorPos = getPhysicalPoint(input, 0, inputCursor);

			os.print(new CursorPosCode(CursorPosCode.Option.Save));

			// clear different region
			if (oldPos - diffPos != 0) {
				moveCursor((oldPos - oldCursorPos) - (oldPos - diffPos));
				os.print(new EraseLineCode(Option.CursorToEnd));
			}

			// write new input
			os.print(input.substring(boundary));

			// move cursor to final position
			os.print(new CursorPosCode(CursorPosCode.Option.Restore));
			moveCursor(newCursorPos - oldCursorPos);

			// sync
			screen = input;
			screenCursor = inputCursor;
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	// relative move
	private void moveCursor(int move) {
		ScriptOutputStream os = context.getOutputStream();
		if (move > 0)
			os.print(new MoveCode(MoveCode.Direction.Right, move));
		else if (move < 0)
			os.print(new MoveCode(MoveCode.Direction.Left, -move));
	}

	private int getPhysicalPoint(String s, int begin, int end) {
		int pos = 0;
		for (int i = begin; i < end; i++)
			pos += isHalfWidth(s.charAt(i)) ? 1 : 2;
		return pos;
	}

	boolean isHalfWidth(char c) {
		return '\u0000' <= c && c <= '\u00FF' || '\uFF61' <= c && c <= '\uFFDC' || '\uFFE8' <= c && c <= '\uFFEE';
	}

	public char read() throws InterruptedException {
		Character character = buffer.take();
		if (character.charValue() == 27) {
			throw new InterruptedException();
		}

		return character;
	}

	public void offer(char c) {
		buffer.offer(c);
	}

	public void flush() {
		buffer.clear();
	}

	public void flush(Collection<Character> drain) {
		buffer.drainTo(drain);
	}

	@Override
	public void keyPressed(FunctionKeyEvent e) {
		if (e.getKeyCode() == KeyCode.LEFT) {
			synchronized (this) {
				if (inputCursor == 0)
					return;

				inputCursor--;
				sync();
			}
		} else if (e.getKeyCode() == KeyCode.RIGHT) {
			synchronized (this) {
				if (inputCursor == screen.length())
					return;

				inputCursor++;
				sync();
			}
		} else if (e.getKeyCode() == KeyCode.BACKSPACE) {
			synchronized (this) {
				if (screenCursor > 0) {
					String left = input.substring(0, screenCursor - 1);
					String right = input.substring(screenCursor);
					input = left + right;
					inputCursor--;
					sync();
				}
			}
		} else if (e.getKeyCode() == KeyCode.DELETE) {
		} else if (e.getKeyCode() == KeyCode.CTRL_C || e.getKeyCode() == KeyCode.CTRL_D) {
			buffer.offer((char) 27);
		} else if (e.getKeyCode() == KeyCode.UP) {
			synchronized (this) {
				boolean hasBeenEditing = historyIndex == -1;
				String line = previousLine();
				if (line == null) {
					return;
				}

				if (hasBeenEditing) {
					history.push(screen);
					historyIndex = 1;
				}

				input = line;
				inputCursor = input.length();
				sync();
			}
		} else if (e.getKeyCode() == KeyCode.DOWN) {
			synchronized (this) {
				boolean hasBeenEditing = historyIndex == -1;
				if (hasBeenEditing)
					return;

				String line = nextLine();
				if (line == null)
					return;

				input = line;
				inputCursor = input.length();
				sync();
			}
		}
	}

	private void resetIndex() {
		historyIndex = -1;
	}

	private String previousLine() {
		if (history.size() == 0)
			return null;

		historyIndex++;
		if (historyIndex >= history.size())
			historyIndex = history.size() - 1;

		return stripCRLF(history.get(historyIndex));
	}

	private String nextLine() {
		if (history.size() == 0)
			return null;

		historyIndex--;
		if (historyIndex < 0)
			historyIndex = 0;

		return stripCRLF(history.get(historyIndex));
	}

	private String stripCRLF(String line) {
		if (line.endsWith("\r\n"))
			return line.substring(0, line.length() - 2);
		else if (line.endsWith("\r") || line.endsWith("\n"))
			return line.substring(0, line.length() - 1);
		else
			return line;
	}

}