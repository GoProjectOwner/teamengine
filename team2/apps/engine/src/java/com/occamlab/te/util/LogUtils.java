package com.occamlab.te.util;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.occamlab.te.Engine;
import com.occamlab.te.Globals;
import com.occamlab.te.TECore;

public class LogUtils {

    public static PrintWriter createLog(File logDir, String callpath) throws Exception {
      if (logDir != null) {
          File dir = new File(logDir, callpath);
          dir.mkdir();
          File f = new File(dir, "log.xml");
          f.delete();
          BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                  new FileOutputStream(f), "UTF-8"));
          return new PrintWriter(writer);
      }
      return null;
  }
    
    // Reads a log from disk
    public static Document readLog(File logDir, String callpath) throws Exception {
        File dir = new File(logDir, callpath);
        File f = new File(dir, "log.xml");
        if (f.exists()) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.newDocument();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            t.setErrorListener(new com.occamlab.te.NullErrorListener());
            try {
                t.transform(new StreamSource(f), new DOMResult(doc));
            } catch (Exception e) {
                // The log may not have been closed properly.
                // Try again with a closing </log> tag
                RandomAccessFile raf = new RandomAccessFile(f, "r");
                int l = new Long(raf.length()).intValue();
                byte[] buf = new byte[l + 8];
                raf.read(buf);
                raf.close();
                buf[l] = '\n';
                buf[l + 1] = '<';
                buf[l + 2] = '/';
                buf[l + 3] = 'l';
                buf[l + 4] = 'o';
                buf[l + 5] = 'g';
                buf[l + 6] = '>';
                buf[l + 7] = '\n';
                doc = db.newDocument();
                tf.newTransformer().transform(
                        new StreamSource(new ByteArrayInputStream(buf)),
                        new DOMResult(doc));
            }
            return doc;
        } else {
            return null;
        }
    }

    // Returns the id of a test from its log document
    public static String getTestIdFromLog(Document log) throws Exception {
        Element starttest = DomUtils.getElementByTagName(log, "starttest");
        String namespace = starttest.getAttribute("namespace-uri");
        String localName = starttest.getAttribute("local-name");
        return "{" + namespace + "}" + localName;
    }

    public static int getResultFromLog(Document log) throws Exception {
        if (log != null) {
            Element endtest = DomUtils.getElementByTagName(log, "endtest");
            if (endtest != null) {
                return Integer.parseInt(endtest.getAttribute("result"));
            }
        }
        return -1;
    }

    // Returns the parameters to a test from its log document
    public static List<String> getParamListFromLog(net.sf.saxon.s9api.DocumentBuilder builder, Document log) throws Exception {
        List<String> list = new ArrayList<String>(); 
        Element starttest = (Element) log.getElementsByTagName("starttest").item(0);
        for (Element param : DomUtils.getElementsByTagName(starttest, "param")) {
            String value = DomUtils.getElementByTagName(param, "value").getTextContent();
            list.add(param.getAttribute("local-name") + "=" + value);
        }
        return list;
    }
    
    // Returns the parameters to a test from its log document
    public static XdmNode getParamsFromLog(net.sf.saxon.s9api.DocumentBuilder builder, Document log) throws Exception {
        Element starttest = (Element) log.getElementsByTagName("starttest").item(0);
        NodeList nl = starttest.getElementsByTagName("params");
        if (nl == null || nl.getLength() == 0) {
            return null;
        } else {
            Document doc = DomUtils.createDocument(nl.item(0));
            return builder.build(new DOMSource(doc));
        }
    }

    // Returns the context node for a test from its log document
    public static XdmNode getContextFromLog(net.sf.saxon.s9api.DocumentBuilder builder, Document log) throws Exception {
        Element starttest = (Element) log.getElementsByTagName("starttest").item(0);
        NodeList nl = starttest.getElementsByTagName("context");
        if (nl == null || nl.getLength() == 0) {
            return null;
        } else {
            Element context = (Element)nl.item(0);
            Element value = (Element)context.getElementsByTagName("value").item(0);
            nl = value.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node n = nl.item(i);
                if (n.getNodeType() == Node.ATTRIBUTE_NODE) {
                    String s = DomUtils.serializeNode(value);
                    XdmNode xn = builder.build(new StreamSource(new CharArrayReader(s.toCharArray())));
                    return (XdmNode)xn.axisIterator(Axis.ATTRIBUTE).next();
                } else if (n.getNodeType() == Node.ELEMENT_NODE) {
                    Document doc = DomUtils.createDocument(n);
                    return builder.build(new DOMSource(doc));
                }
            }
        }
        return null;
    }

    private static Element makeTestListElement(DocumentBuilder db, Document owner, File logdir,
            String path, List<QName> pathQName, List<List<QName>> excludes) throws Exception {
        File log = new File(new File(logdir, path), "log.xml");
        Document logdoc = LogUtils.readLog(log.getParentFile(), ".");
        if (logdoc == null) {
            return null;
        }
        Element log_e = DomUtils.getElementByTagName(logdoc, "log");
        if (log_e == null) {
            return null;
        }
        Element test = owner.createElement("test");
        List<QName> testQName = new ArrayList<QName>();
        testQName.addAll(pathQName);
        int result = TECore.PASS;
        boolean complete = false;
        boolean childrenFailed = false;
        for (Element e : DomUtils.getChildElements(log_e)) {
            if (e.getNodeName().equals("starttest")) {
                NamedNodeMap atts = e.getAttributes();
                for (int j = 0; j < atts.getLength(); j++) {
                    test.setAttribute(atts.item(j).getNodeName(), atts.item(j).getNodeValue());
                }
                String namespaceURI = test.getAttribute("namespace-uri");
                String localPart = test.getAttribute("local-name");
                String prefix = test.getAttribute("prefix");
                QName qname = new QName(namespaceURI, localPart, prefix);
                testQName.add(qname);
                if (excludes.contains(testQName)) {
                    return null;
                }
            } else if (e.getNodeName().equals("endtest")) {
                complete = true;
                int code = Integer.parseInt(e.getAttribute("result"));
                if (code == TECore.FAIL) {
                    result = TECore.FAIL;
                } else if (childrenFailed) {
                    result = TECore.INHERITED_FAILURE;
                } else if (code == TECore.WARNING) {
                    result = TECore.WARNING;
                }
            } else if (e.getNodeName().equals("testcall")) {
                String newpath = e.getAttribute("path");
                Element child = makeTestListElement(db, owner, logdir, newpath, testQName, excludes);
                if (child != null) {
                    child.setAttribute("path", newpath);
                    int code = Integer.parseInt(child.getAttribute("result"));
                    if (code == TECore.FAIL || code == TECore.INHERITED_FAILURE) {
                        childrenFailed = true;
                    }
                    test.appendChild(child);
                }
            }
        }
        test.setAttribute("result", Integer.toString(result));
        test.setAttribute("complete", complete ? "yes" : "no");
        return test;
    }

    public static Document makeTestList(File logdir, String path, List<List<QName>> excludes) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();
        Element test = makeTestListElement(db, doc, logdir, path, new ArrayList<QName>(), excludes);
        if (test != null) {
            doc.appendChild(test);
            doc.getDocumentElement().setAttribute("path", path);
        }
        return doc;
    }

    public static Document makeTestList(File logdir, String path) throws Exception {
        List<List<QName>> excludes = new ArrayList<List<QName>>();
        excludes.add(new ArrayList<QName>());
        return makeTestList(logdir, path, excludes);
    }

    /**
     * Generates a session identifier. The value corresponds to the name of a
     * sub-directory (session) in the root test log directory.
     * 
     * @return a session id string ("s0001" by default, unless the session
     *         sub-directory already exists).
     */
    public static String generateSessionId(File logDir) {
        int i = 1;
        String session = "s0001";
        while (new File(logDir, session).exists() && i < 10000) {
            i++;
            session = "s" + Integer.toString(10000 + i).substring(1);
        }
        return session;
    }
}