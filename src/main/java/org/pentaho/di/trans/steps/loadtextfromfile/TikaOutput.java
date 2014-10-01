package org.pentaho.di.trans.steps.loadtextfromfile;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.HashMap;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import com.google.gson.Gson;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.PasswordProvider;
import org.apache.tika.parser.html.BoilerpipeContentHandler;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ExpandedTitleContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

public class TikaOutput {
  
  private static TikaOutput instance = null;
  
  
  private TikaOutput() {
    context = new ParseContext();
    detector = new DefaultDetector();
    parser = new AutoDetectParser(detector);
    context.set(Parser.class, parser);
    context.set(PasswordProvider.class, new PasswordProvider() {
        public String getPassword(Metadata metadata) {
            return password;
        }
    });
  }
  
  public static TikaOutput getInstance() {
    if(null == instance) {
      instance = new TikaOutput();
    }
    return instance;
  }
  
  private class OutputType {

    public void process(InputStream input, OutputStream output, Metadata metadata) throws Exception {
        Parser p = parser;
        
        ContentHandler handler = getContentHandler(output, metadata);
        p.parse(input, handler, metadata, context);
        // fix for TIKA-596: if a parser doesn't generate
        // XHTML output, the lack of an output document prevents
        // metadata from being output: this fixes that
        if (handler instanceof NoDocumentMetHandler){
            NoDocumentMetHandler metHandler = (NoDocumentMetHandler)handler;
            if(!metHandler.metOutput()){
                metHandler.endDocument();
            }
        }
    }

    protected ContentHandler getContentHandler(
            OutputStream output, Metadata metadata) throws Exception {
        throw new UnsupportedOperationException();
    }
    
  }

  public final OutputType XML = new OutputType() {
      @Override
      protected ContentHandler getContentHandler(
              OutputStream output, Metadata metadata) throws Exception {
          return getTransformerHandler(output, "xml", encoding, prettyPrint);
      }
  };
  
  public final OutputType HTML = new OutputType() {
      @Override
      protected ContentHandler getContentHandler(
              OutputStream output, Metadata metadata) throws Exception {
          return new ExpandedTitleContentHandler(getTransformerHandler(output, "html", encoding, prettyPrint));
      }
  };
  
  public final OutputType TEXT = new OutputType() {
      @Override
      protected ContentHandler getContentHandler(
              OutputStream output, Metadata metadata) throws Exception {
          return new BodyContentHandler(getOutputWriter(output, encoding));
      }
  };
  
  public final OutputType NO_OUTPUT = new OutputType() {
      @Override
      protected ContentHandler getContentHandler(
              OutputStream output, Metadata metadata) {
          return new DefaultHandler();
      }
  };
  
  public final OutputType TEXT_MAIN = new OutputType() {
      @Override
      protected ContentHandler getContentHandler(
              OutputStream output, Metadata metadata) throws Exception {
          return new BoilerpipeContentHandler(getOutputWriter(output, encoding));
      }
  };
  
  public final OutputType METADATA = new OutputType() {
      @Override
      protected ContentHandler getContentHandler(
              OutputStream output, Metadata metadata) throws Exception {
          final PrintWriter writer =
              new PrintWriter(getOutputWriter(output, encoding));
          return new NoDocumentMetHandler(metadata, writer);
      }
  };
  
  public final OutputType JSON = new OutputType() {
      @Override
      protected ContentHandler getContentHandler(
              OutputStream output, Metadata metadata) throws Exception {
          final PrintWriter writer =
                  new PrintWriter(getOutputWriter(output, encoding));
          return new NoDocumentJSONMetHandler(metadata, writer);
      }
  };
  
  @SuppressWarnings("serial")
  public static final HashMap<String,OutputType> fileOutputTypeCodes = new HashMap<String,OutputType>() {{
    put("Plain text", TikaOutput.getInstance().TEXT);
    put("Main content", TikaOutput.getInstance().TEXT_MAIN);
    put("XML", TikaOutput.getInstance().XML);
    put("HTML", TikaOutput.getInstance().HTML);
    put("JSON", TikaOutput.getInstance().JSON);
  }};
  
  
  private ParseContext context;
  
  private Detector detector;
  
  private Parser parser;

  
  /**
   * Output character encoding, or <code>null</code> for platform default
   */
  private String encoding = null;
  
  /**
   * Password for opening encrypted documents, or <code>null</code>.
   */
  private String password = System.getenv("TIKA_PASSWORD");

  private boolean prettyPrint = false;
  
  
  private class NoDocumentMetHandler extends DefaultHandler {
  
    protected final Metadata metadata;
  
    protected PrintWriter writer;
    
    private boolean metOutput;
  
    public NoDocumentMetHandler(Metadata metadata, PrintWriter writer){
        this.metadata = metadata;
        this.writer = writer;
        this.metOutput = false;
    }
    
    @Override
    public void endDocument() {
        String[] names = metadata.names();
        Arrays.sort(names);
        outputMetadata(names);
        writer.flush();
        this.metOutput = true;
    }
    
    public void outputMetadata(String[] names) {
       for (String name : names) {
          for(String value : metadata.getValues(name)) {
             writer.println(name + ": " + value);
          }
       }
    }
    
    public boolean metOutput(){
        return this.metOutput;
    }
    
  }
  
  /**
  * Uses GSON to do the JSON escaping, but does
  *  the general JSON glueing ourselves.
  */
  private class NoDocumentJSONMetHandler extends NoDocumentMetHandler {
    private NumberFormat formatter;
    private Gson gson;
   
    public NoDocumentJSONMetHandler(Metadata metadata, PrintWriter writer){
        super(metadata, writer);
        
        formatter = NumberFormat.getInstance();
        gson = new Gson();
    }
    
    @Override
    public void outputMetadata(String[] names) {
       writer.print("{ ");
       boolean first = true;
       for (String name : names) {
          if(! first) {
             writer.println(", ");
          } else {
             first = false;
          }
          gson.toJson(name, writer);
          writer.print(":");
          outputValues(metadata.getValues(name));
       }
       writer.print(" }");
    }
    
    public void outputValues(String[] values) {
       if(values.length > 1) {
          writer.print("[");
       }
       for(int i=0; i<values.length; i++) {
          String value = values[i];
          if(i > 0) {
             writer.print(", ");
          }
          
          if(value == null || value.length() == 0) {
             writer.print("null");
          } else {
             // Is it a number?
             ParsePosition pos = new ParsePosition(0);
             formatter.parse(value, pos);
             if(value.length() == pos.getIndex()) {
                // It's a number. Remove leading zeros and output
                value = value.replaceFirst("^0+(\\d)", "$1");
                writer.print(value);
             } else {
                // Not a number, escape it
                gson.toJson(value, writer);
             }
          }
       }
       if(values.length > 1) {
          writer.print("]");
       }
    }
  }
  
  /**
   * Returns a output writer with the given encoding.
   *
   * @see <a href="https://issues.apache.org/jira/browse/TIKA-277">TIKA-277</a>
   * @param output output stream
   * @param encoding output encoding,
   *                 or <code>null</code> for the platform default
   * @return output writer
   * @throws UnsupportedEncodingException
   *         if the given encoding is not supported
   */
  private static Writer getOutputWriter(OutputStream output, String encoding)
          throws UnsupportedEncodingException {
      if (encoding != null) {
          return new OutputStreamWriter(output, encoding);
      } else if (System.getProperty("os.name")
              .toLowerCase().startsWith("mac os x")) {
          // TIKA-324: Override the default encoding on Mac OS X
          return new OutputStreamWriter(output, "UTF-8");
      } else {
          return new OutputStreamWriter(output);
      }
  }
  
  /**
   * Returns a transformer handler that serializes incoming SAX events
   * to XHTML or HTML (depending the given method) using the given output
   * encoding.
   *
   * @see <a href="https://issues.apache.org/jira/browse/TIKA-277">TIKA-277</a>
   * @param output output stream
   * @param method "xml" or "html"
   * @param encoding output encoding,
   *                 or <code>null</code> for the platform default
   * @return {@link System#out} transformer handler
   * @throws TransformerConfigurationException
   *         if the transformer can not be created
   */
  private static TransformerHandler getTransformerHandler(
          OutputStream output, String method, String encoding, boolean prettyPrint)
          throws TransformerConfigurationException {
      SAXTransformerFactory factory = (SAXTransformerFactory)
              SAXTransformerFactory.newInstance();
      TransformerHandler handler = factory.newTransformerHandler();
      handler.getTransformer().setOutputProperty(OutputKeys.METHOD, method);
      handler.getTransformer().setOutputProperty(OutputKeys.INDENT, prettyPrint ? "yes" : "no");
      if (encoding != null) {
          handler.getTransformer().setOutputProperty(
                  OutputKeys.ENCODING, encoding);
      }
      handler.setResult(new StreamResult(output));
      return handler;
  }
  
  private static OutputType getTypeByName(String name) {
    return fileOutputTypeCodes.get(name);
  }
  
  public static void parse(InputStream in, String outputFormat, OutputStream out) throws Exception {
    InputStream input = TikaInputStream.get(in);
    OutputType type = getTypeByName(outputFormat);
    try {
        type.process(input, out, new Metadata());
    } catch(Exception e) {
      e.printStackTrace();
      throw e;
    }
      finally {
      try{
        input.close();
        out.flush();
      }catch(Exception e) {e.printStackTrace();}
    }
  }
}
