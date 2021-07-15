package org.alittlela.fs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Fs {
	/**
	 * Read file at path with range [start, end)
	 * 
	 * @param path  path of file
	 * @param start
	 * @param end
	 * @return the content of the file in byte array
	 */
	public static byte[] read(String path, int start, int end) throws FileNotFoundException, IOException {
		RandomAccessFile file = new RandomAccessFile(path, "r");
		byte[] buffer = new byte[(int) (end - start)];
		file.read(buffer, start, end - start);
		file.close();
		return buffer;
	}

	public static void write(String path, int offset, byte[] content) throws FileNotFoundException, IOException {
		createParentDirs(path);
		RandomAccessFile f = new RandomAccessFile(path, "rw");
		f.seek(offset);
		f.write(content);
		f.close();
	}

	public static int append(String path, byte[] content) throws FileNotFoundException, IOException {
		createParentDirs(path);
		RandomAccessFile f = new RandomAccessFile(path, "rw");
		int len = (int) f.length();
		f.seek(len);
		f.write(content);
		f.close();
		return len;
	}

	private static void createParentDirs(String path) {
		File f = new File(path);
		File parent = f.getParentFile();
		if (parent != null && !parent.exists() && !parent.mkdirs()) {
			throw new IllegalStateException("Couldn't create dir: " + parent);
		}
	}
}
