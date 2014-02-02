package org.araqne.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.araqne.confdb.file.FileConfigDatabase;

public class ConfigRepairer {

	private static final String dbName = "araqne-log-api";

	public static void main(String[] args) throws IOException {
		new ConfigRepairer().run();
	}

	public void run() throws IOException {
		FileConfigDatabase db = new FileConfigDatabase(new File("araqne-confdb"), dbName);

		File dir = new File("araqne-confdb", dbName);

		try {
			db.getManifest(null);
			for (String name : db.getCollectionNames())
				System.out.println(name);

		} catch (Throwable t) {
			t.printStackTrace();
			if (t.getMessage().contains("corrupted")) {
				File logFile = new File(dir, "changeset.log");
				repair(dir, logFile);
				System.out.println("repaired " + db);
			}
		}
	}

	private void repair(File dir, File logFile) throws FileNotFoundException, IOException {
		File corrupted = new File(dir, "changeset.log.corrupted");
		logFile.renameTo(corrupted);

		File repaired = new File(dir, "changeset.log");

		FileInputStream src = new FileInputStream(corrupted);
		FileOutputStream dst = new FileOutputStream(repaired);
		FileChannel srcChannel = src.getChannel();
		FileChannel dstChannel = dst.getChannel();
		long size = srcChannel.size();
		srcChannel.transferTo(0, size - 34, dstChannel);
		src.close();
		dst.close();
	}
}
