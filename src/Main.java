import org.jetbrains.annotations.NotNull;
import utils.RequestType;

import java.util.Scanner;

public class Main {
    public static void main(@org.jetbrains.annotations.NotNull String @NotNull [] args) {
        System.out.println("Hello world!");

        // if args is not of length 2, print error message
        if (!(args.length == 2 || args.length == 3)) {
            System.out.println("ERROR - Usage: java Main --server <port> or java Main --client <server-add> <port>");
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
            String serverAddress = args[1];
            int port = Integer.parseInt(args[2]);
            try {
                client.Client client = new client.Client(serverAddress,port);
                Scanner scanner = new Scanner(System.in);


                while(true) {
                    System.out.println("Which service would you like to perform (READ, INSERT, LISTEN, STOP)?");
                    String requestTypeStr = scanner.nextLine();
                    RequestType requestType = RequestType.valueOf(requestTypeStr.toUpperCase());

                    if (requestType == RequestType.STOP) {
                       client.makeRequest(requestType, "");
                       break;
                    }

                    System.out.println("Please enter file pathname, an offset in bytes, the number of bytes to be read separated by ,:");
                    String input = scanner.nextLine();
                    String response = client.makeRequest(requestType, input);

                }

                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Invalid app type. Use --server or --client");
        }
    }
}