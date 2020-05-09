package utils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;
import java.io.InputStream;

public class XMLParser extends StreamReaderDelegate {
    private String sourceName = "";

    public XMLParser(String resourceName) {
        this(XMLParser.class.getClassLoader().getResourceAsStream(resourceName));
        this.sourceName = resourceName;
    }

    public XMLParser(InputStream inputStream) {
        super(createXMLStreamReader(inputStream));
    }

    public static XMLStreamReader createXMLStreamReader(InputStream input) {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLStreamReader xmlStreamReader = null;
        try {
            xmlStreamReader = inputFactory.createXMLStreamReader(input);
        } catch (XMLStreamException e) {
            SLF4J.logException("Cannot attach XMLStreamReader to file stream content", e);
        }
        return xmlStreamReader;
    }

    public boolean tryNext() throws XMLStreamException {
        if (this.hasNext()) {
            this.next();
            return true;
        }
        return false;
    }

    public boolean nextBeginTag(String tag) throws XMLStreamException {

        int fromEvent = this.getEventType();
        int skipCount = 0;
        while (!this.isStartElement() && !this.isEndElement() && this.hasNext()) {
            if (!this.isWhiteSpace()) {
                skipCount++;
            }
            this.next();
        }
        int toEvent = this.getEventType();
        String hit = (this.isStartElement() ? this.getLocalName() : "/");

        if (skipCount > 0 && !hit.equals(tag)) {
            SLF4J.LOGGER.debug("nextBeginTag(" + tag + "): skipped " + skipCount +
                    " from event" + fromEvent + " to event" + toEvent + " hit <" + hit + ">");
        }
        return hit.equals(tag);
    }

    public String nextEndTag() throws XMLStreamException {
        while (!this.isEndElement() && this.hasNext()) {
            this.next();
        }
        String hit = (this.isEndElement() ? this.getLocalName() : "/");
        return hit;
    }

    public boolean nextEndTag(String tag) throws XMLStreamException {
        int fromEvent = this.getEventType();
        int skipCount = 0;
        while (!this.isEndElement() && this.hasNext()) {
            if (!this.isWhiteSpace()) {
                skipCount++;
            }
            this.next();

        }
        int toEvent = this.getEventType();
        String hit = (this.isEndElement() ? this.getLocalName() : "/");
        if (skipCount > 0 && !tag.equals(hit)) {
            SLF4J.LOGGER.debug("nextEndTag(" + tag + "): skipped " + skipCount +
                    " from event" + fromEvent + " to event" + toEvent + " hit </" + hit + ">");
        }
        return tag.equals(hit);
    }

    public boolean findBeginTag(String tag) throws XMLStreamException {
        boolean hit = this.nextBeginTag(tag);
        while (!hit && this.hasNext()) {
            this.next();
            hit = this.nextBeginTag(tag);
        }
        return hit;
    }

    public boolean findAndAcceptEndTag(String tag) throws XMLStreamException {
        boolean hit = this.nextEndTag(tag);
        while (!hit && this.hasNext()) {
            this.next();
            hit = this.nextEndTag(tag);
        }
        if (hit) {
            this.next();
        }
        return hit;
    }

    public boolean skipMandatoryElement(String tag) throws XMLStreamException {
        if (this.findBeginTag(tag)) {
            this.findAndAcceptEndTag(tag);
            return true;
        }
        return false;
    }

    public double getDoubleAttributeValue(String ns, String name, double dVal) {
        String value = this.getAttributeValue(ns, name);
        return (value != null ? Double.valueOf(value) : dVal);
    }

    public int getIntegerAttributeValue(String ns, String name, int iVal) {
        String value = this.getAttributeValue(ns, name);
        return (value != null ? Integer.valueOf(value) : iVal);
    }

    public void logStatus() {
        SLF4J.LOGGER.debug("start=" + this.isStartElement() +
                " end=" + this.isEndElement() +
                " chars=" + this.isCharacters() +
                " whitespace=" + this.isWhiteSpace() +
                " name=" + this.getLocalName());
    }
}
