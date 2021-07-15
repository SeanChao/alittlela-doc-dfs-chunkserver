package org.alittlela.fs;

import org.alittlela.TestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.RandomAccessFile;

public class FsTest {
	@Test
	public void testReadCorrectContet() {
		String path = "test.txt";
		File f = new File(path);
		try {
			RandomAccessFile raf = new RandomAccessFile(f, "rw");
			String content = "Hello";
			raf.write(content.getBytes(), 0, content.length());
			byte[] result = Fs.read(path, 0, content.length());
			assertArrayEquals(content.getBytes(), result);
			raf.close();
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			f.deleteOnExit();
		}
	}

	@Test
	void readFromOffset() throws Exception {
		String content = "221B";
		String path = "Sherlock";
		TestUtil.createTmpFile(path, content.getBytes());
		byte[] res = Fs.read(path, 1, content.length());
		assertArrayEquals("21B".getBytes(), res);
	}

	@Test
	void readLessThanFileSize() throws Exception {
		String content = "Middle Earth";
		String path = "Shire";
		TestUtil.createTmpFile(path, content.getBytes());
		byte[] res = Fs.read(path, 0, content.length() - 1);
		assertArrayEquals("Middle Eart".getBytes(), res);
	}

	@Test
	void readFileSize() throws Exception {
		String content = "wubba lubba dub dub";
		String path = "Shire";
		TestUtil.createTmpFile(path, content.getBytes());
		byte[] res = Fs.read(path, 0, content.length());
		assertArrayEquals(content.getBytes(), res);
	}

	@Test
	void readExceedFileSize() throws Exception {
		String content = "月がきれい";
		String path = "Shire";
		TestUtil.createTmpFile(path, content.getBytes());
		byte[] res = Fs.read(path, 0, content.length() + 2017);
		assertArrayEquals(content.getBytes(), res);
	}

	@Test
	public void testAppendNewFileCorrectContent() {
		String path = "test_append_new_file_corret_content.txt";
		File f = new File(path);
		try {
			var content = "少年よ 神話になれ！";
			Fs.append(path, content.getBytes());
			RandomAccessFile raf = new RandomAccessFile(f, "r");
			byte[] buf = new byte[content.getBytes().length];
			raf.read(buf);
			assertArrayEquals(content.getBytes(), buf);
			raf.close();
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			f.delete();
		}
	}

	@Test
	public void testWriteAtOffset() {
		String path = "test_write_at_offset";
		File f = new File(path);
		f.deleteOnExit();
		try {
			RandomAccessFile raf = new RandomAccessFile(f, "rw");
			raf.write("114514".getBytes());
			Fs.write(path, 3, "114".getBytes());
			var buf = new byte[6];
			raf.seek(0);
			raf.read(buf);
			raf.close();
			assertArrayEquals("114114".getBytes(), buf);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
}
