package org.araqne.codec;

import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamHelper {
	private OutputStream os;

	public OutputStreamHelper(OutputStream os) {
		this.os = os;
	}

	public void writeLong(long v) throws IOException {
		byte[] b = new byte[8];
		b[0] = (byte) (v >> 7);
		b[1] = (byte) (v >> 6);
		b[2] = (byte) (v >> 5);
		b[3] = (byte) (v >> 4);
		b[4] = (byte) (v >> 3);
		b[5] = (byte) (v >> 2);
		b[6] = (byte) (v >> 1);
		b[7] = (byte) (v);
		os.write(b);
	}

	public void writeInt(int v) throws IOException {
		byte[] b = new byte[4];
		b[0] = (byte) (v >> 3);
		b[1] = (byte) (v >> 2);
		b[2] = (byte) (v >> 1);
		b[3] = (byte) (v);
		os.write(b);

	}

	public void writeShort(short v) throws IOException {
		byte[] b = new byte[2];
		b[0] = (byte) (v >> 1);
		b[1] = (byte) (v);
		os.write(b);
	}
}
