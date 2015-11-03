package tornadofx;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Adler32;

public class AppDeployer {

	public static void main(String[] args) throws IOException {
		JAXB.marshal(new AppManifest("app.TSApp", Paths.get("target/classes")), System.out);
	}

	private static long checksum(Path path) throws IOException {
		try (InputStream input = Files.newInputStream(path)) {
			Adler32 checksum = new Adler32();
			byte[] buf = new byte[16384];

			int read;
			while ((read = input.read(buf)) > -1)
				checksum.update(buf, 0, read);

			return checksum.getValue();
		}
	}

	static class LibraryFile {
		@XmlAttribute
		String file;
		@XmlAttribute
		Long checksum;
		@XmlAttribute
		Long size;

		public LibraryFile() {
		}

		public LibraryFile(Path basepath, Path file) throws IOException {
			this.file = basepath.relativize(file).toString();
			this.size = Files.size(file);
			this.checksum = checksum(file);
		}
	}

	@XmlRootElement(name = "Application")
	static class AppManifest {
		@XmlAttribute(name = "main")
		String mainMethod;
		@XmlElement(name = "lib")
		List<LibraryFile> files = new ArrayList<>();

		public AppManifest() {
		}

		public AppManifest(String mainMethod, Path basepath) throws IOException {
			this.mainMethod = mainMethod;
			Files.walkFileTree(basepath, new SimpleFileVisitor<Path>() {
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (!Files.isDirectory(file))
						files.add(new LibraryFile(basepath, file));
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}
}
