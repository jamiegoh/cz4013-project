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
            String serverAddress = args[1];
            int port = Integer.parseInt(args[2]);
            try {
                client.Client client = new client.Client(serverAddress,port);
                Scanner scanner = new Scanner(System.in);
                boolean running = true;

                while(running) {
                    System.out.println("Which service would you like to perform (READ, INSERT, LISTEN, STOP)?");
                    String requestTypeStr = scanner.nextLine();
                    RequestType requestType = RequestType.valueOf(requestTypeStr.toUpperCase());

                    switch (requestType) {
                        case READ:
                            System.out.println("Enter the pathname, offset, and readBytes separated by commas:");
                            break;
                        case INSERT:
                            System.out.println("Enter the pathname, offset, and data separated by commas:");
                            break;
                        case LISTEN:
                            System.out.println("Enter the pathname and monitor interval separated by commas:");
                            break;
                        case STOP:
                            client.makeRequest(requestType, "");
                            running = false;
                            continue;
                        default:
                            System.out.println("Invalid request type. Use READ, INSERT, LISTEN, or STOP");
                            continue;
                    }
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