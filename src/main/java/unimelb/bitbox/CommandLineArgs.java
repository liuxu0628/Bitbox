package unimelb.bitbox;
import org.kohsuke.args4j.Option;
import unimelb.bitbox.util.HostPort;

public class CommandLineArgs {

    @Option(required = true, name = "-c", usage = "Command")
    private String command;

    @Option(required = true, name = "-i", usage = "Identity")
    private String identity;

    @Option(required = true, name = "-s", usage = "Server Hostport")
    private String server;

    @Option(required = false, name = "-p", usage = "Peer Hostport")
    private String peer;

    public String getCommand() {
        return command;
    }

    public String getIdentity() {
        return identity;
    }

    public HostPort getServer() {
        return new HostPort(server);
    }

    public HostPort getPeer() {
        return new HostPort(peer);
    }

}
