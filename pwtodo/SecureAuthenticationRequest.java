
import com.mojang.authlib.Agent;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import javax.swing.text.Segment;

public class SecureAuthenticationRequest {
    private Agent agent;
    private String username;
    private String password; // Segment
    private String clientToken;
    private boolean requestUser = true;

    public SecureAuthenticationRequest(YggdrasilUserAuthentication authenticationService, String username, String password) {
        this.agent = authenticationService.getAgent();
        this.username = username;
        this.clientToken = authenticationService.getAuthenticationService().getClientToken();
        this.password = authenticationService.getAuthenticationService().getClientToken(); // ??getPassword
    }
}

