import org.jetbrains.annotations.NotNull;

import java.util.Scanner;

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
            // todo: args[1] should be server address
            // todo: args[2] should be server port
            int port = Integer.parseInt(args[1]);
            try {
                client.Client client = new client.Client(port);
                Scanner scanner = new Scanner(System.in);

                System.out.println("Enter STOP to stop the client");
                System.out.println("Please enter file pathname, an offset in bytes, the number of bytes to be read separated by ,:");
                String input = scanner.nextLine();
                String response = client.sendData(input);
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