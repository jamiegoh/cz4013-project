import org.jetbrains.annotations.NotNull;
import utils.RequestType;
import utils.InvocationSemantics;

import java.util.Scanner;


public class Main {

    public static boolean validateInput(RequestType requestType, String input) {
        String[] inputArr = input.split(",");
        switch (requestType) {
            case READ, INSERT:
                return inputArr.length == 3;
            case LISTEN:
                return inputArr.length == 2;
            case CREATE, ATTR:
                return inputArr.length == 1;
            default:
                return false;
        }
    }
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
            InvocationSemantics invocationSemantics = args.length == 3 ? InvocationSemantics.valueOf(args[2]) : InvocationSemantics.AT_LEAST_ONCE;
            try {
                server.Server server = new server.Server(port, invocationSemantics);
                server.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (appType.equals("--client")) {
            System.out.println("Starting client...");
            String serverAddress = args[1];
            int port = Integer.parseInt(args[2]);
            InvocationSemantics invocationSemantics = args.length == 4 ? InvocationSemantics.valueOf(args[3]) : InvocationSemantics.AT_LEAST_ONCE;

            try {
                client.Client client = new client.Client(serverAddress, port, invocationSemantics);
                Scanner scanner = new Scanner(System.in);
                boolean running = true;

                while (running) {
                    System.out.println("Which service would you like to perform (READ, INSERT, LISTEN, CREATE, ATTR, STOP)?");
                    String requestTypeStr = scanner.nextLine();
                    RequestType requestType = RequestType.valueOf(requestTypeStr.toUpperCase());

                    String input;
                    switch (requestType) {
                        case READ:
                            System.out.println("Enter the pathname, offset, and readBytes separated by commas:");
                            input = scanner.nextLine();
                            if (!validateInput(requestType, input)) {
                                System.out.println("Invalid input. Please try again.");
                                continue;
                            }
                            break;
                        case INSERT:
                            System.out.println("Enter the pathname, offset, and data separated by commas:");
                            input = scanner.nextLine();
                            if (!validateInput(requestType, input)) {
                                System.out.println("Invalid input. Please try again.");
                                continue;
                            }
                            break;
                        case LISTEN:
                            System.out.println("Enter the pathname and monitor interval separated by commas:");
                            input = scanner.nextLine();
                            if (!validateInput(requestType, input)) {
                                System.out.println("Invalid input. Please try again.");
                                continue;
                            }
                            break;
                        case CREATE:
                            System.out.println("Enter a pathname to create: e.g. /dir1/file1");
                            input = scanner.nextLine();
                            if (!validateInput(requestType, input)) {
                                System.out.println("Invalid input. Please try again.");
                                continue;
                            }
                            break;
                        case ATTR:
                            System.out.println("Enter a pathname to get time last modified e.g. /dir1/file1");
                            input = scanner.nextLine();
                            if (!validateInput(requestType, input)) {
                                System.out.println("Invalid input. Please try again.");
                                continue;
                            }
                            break;
                        case STOP:
                            client.makeRequest(requestType, "");
                            running = false;
                            continue;
                        default:
                            System.out.println("Invalid request type. Use READ, INSERT, LISTEN, or STOP");
                            continue;
                    }

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
