package org.alittlela;

import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;

public class TestUtil {

	public static void createTmpFile(String path, byte[] content) throws Exception {
		File f = new File(path);
		f.deleteOnExit();
		RandomAccessFile raf = new RandomAccessFile(f, "rw");
		raf.write(content);
		raf.close();
	}

	public static void createTmpFile(String path) throws Exception {
		createTmpFile(path, new byte[0]);
	}

	public static byte[] readBytes(String path) throws Exception {
		FileInputStream fis = new FileInputStream(path);
		var data = fis.readAllBytes();
		fis.close();
		return data;
	}

	public static void deleteFile(String path) {
		File f = new File(path);
		f.delete();
	}
}
