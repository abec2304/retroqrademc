import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;

class SecureJsonWriter extends JsonWriter {
    
    private final SecureCharArrayWriter writer;
    
    public SecureJsonWriter(SecureCharArrayWriter writer) {
        super(writer);
        this.writer = writer;
    }
    
    public SecureCharArrayWriter getWriter() {
        return writer;
    }
    
    public static byte[] toJson(Gson gson, Object o) {
        // FIXME: too long in bytecode
        SecureCharArrayWriter writer = new SecureCharArrayWriter(4096);
        gson.toJson(o, o.getClass(), new SecureJsonWriter(writer));
        
        CharsetEncoder ce = Charset.forName("UTF8").newEncoder();
        int pos = writer.size();
        byte[] bytes = new byte[pos * 3];
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        
        writer.encode(ce, bb);
        ce.flush(bb);
        writer.clear();
        
        return Bypass.trim(bb, bytes);
    }
    
}
