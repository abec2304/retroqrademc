import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.util.Arrays;

import javax.swing.JPasswordField;
import javax.swing.text.*;

import java.nio.*;
import java.nio.charset.*;

public class SecureTypeAdapter extends TypeAdapter<Segment> {
    
    private void write(JsonWriter writer, Segment segment, SecureCharArrayWriter out) throws IOException {
        // FIXME: too long in bytecode
        // a-z, A-Z, 0-9 characters
        char[] safe = new char[segment.count];

        // get position
        int pos1 = out.size();
        
        // jsonify blanked string
        writer.value(Bypass.strip(safe, segment));
        
        // get position
        int pos2 = out.size();

        // replace blanked characters
        out.sub(pos1, pos2, safe, 'z');
        
        // blank substitution array
        Arrays.fill(safe, '\0');
        
        // blank offsets
        pos1 = pos2 = 0;
    }
    
    @Override
    public void write(JsonWriter writer, Segment segment) throws IOException {
        if(writer instanceof SecureJsonWriter) {
            write(writer, segment, ((SecureJsonWriter)writer).getWriter());
            return;
        }
        writer.value(segment.toString());
    }
    
    @Override
    public Segment read(JsonReader reader) throws IOException {
        char[] arr = reader.nextString().toCharArray();
        return new Segment(arr, 0, arr.length);
    }
  
}
