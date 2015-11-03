package tornadofx.launcher;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
@XmlRootElement(name = "Application")
public class FXManifest {
    @XmlAttribute
    String name;
    @XmlAttribute
    URI uri;
    @XmlAttribute(name = "launch")
    String launchClass;
    @XmlElement(name = "lib")
    List<LibraryFile> files = new ArrayList<>();
}

