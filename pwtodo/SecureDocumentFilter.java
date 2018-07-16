import javax.swing.*;
import javax.swing.text.*;

public class SecureDocumentFilter extends DocumentFilter {
    
    @Override
    public void insertString(FilterBypass fb, int off, String str, AttributeSet as) throws BadLocationException {
        if(Bypass.isAllowed(fb, str, 0, 0))
            super.insertString(fb, off, str, as);
    }
    
    @Override
    public void replace(FilterBypass fb, int off, int len, String str, AttributeSet as) throws BadLocationException {
        if(Bypass.isAllowed(fb, str, off, len))
            super.replace(fb, off, len, str, as);
    }
    
    public static JPasswordField newPasswordField(PlainDocument document) {
        document.setDocumentFilter(new SecureDocumentFilter());
        return new JPasswordField(document, null, 0);
    }

}
