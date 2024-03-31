import org.jetbrains.annotations.NotNull;

public class Main {
    public static void main(@org.jetbrains.annotations.NotNull String @NotNull [] args) {
        System.out.println("Hello world!");

        // if args is not of length 2, print error message
        if (args.length != 2) {
            System.out.println("ERROR - Usage: java Main --server <port> or java Main --client <port>");
            return;
        }

        String appType = args[0]; // "--server" or "--client"

        if (appType.equals("--server")) {
            System.out.println("Starting server...");
            int port = Integer.parseInt(args[1]);
            try {
                server.Server server = new server.Server(port);
                server.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (appType.equals("--client")) {
            System.out.println("Starting client...");
            int port = Integer.parseInt(args[1]);
            try {
                client.Client client = new client.Client(port);
                String response = client.sendData("hello");
//                String response = client.sendData("STOP");
//                System.out.println("Response from server: " + response);
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Invalid app type. Use --server or --client");
        }
    }
}