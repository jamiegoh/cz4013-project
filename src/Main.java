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

    // print usage
    public static void printUsage() {
        System.out.println("Usage: java Main --server <server-port> <invocation-semantics> or java Main --client <server-address> <server-port> <invocation-semantics> <freshness-interval>");
        System.out.println("Mandatory arguments:");
        System.out.println("<server-address> - IP address of server");
        System.out.println("<server-port> - Port number to run server on");
        System.out.println("Optional arguments:");
        System.out.println("<invocation-semantics> - AT_LEAST_ONCE or AT_MOST_ONCE");
        System.out.println("<freshness-interval> - Time in milliseconds to wait before checking for file changes in server");
    }

    public static void main(@org.jetbrains.annotations.NotNull String @NotNull [] args) {
        System.out.println("Hello world!");

        printUsage();


        // if args is not of length 2, print error message
        if (!(args.length == 2 || args.length == 3)) {
            printUsage();
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
            int freshnessInterval = args.length == 5 ? Integer.parseInt(args[4]) : 0;

            try {
                client.Client client = new client.Client(serverAddress, port, invocationSemantics, freshnessInterval);
                Scanner scanner = new Scanner(System.in);
                boolean running = true;

                while (running) {
                    System.out.println("Which service would you like to perform on server (READ, INSERT, LISTEN, CREATE, ATTR, STOP)?");
                    System.out.println("Enter QUIT to exit local client.");
                    String requestTypeStr = scanner.nextLine();

                    if (requestTypeStr.equals("QUIT")) {
                        running = false;
                        continue;
                    }

                    RequestType requestType;
                    try {
                        requestType = RequestType.valueOf(requestTypeStr.toUpperCase());
                    }
                    catch (IllegalArgumentException e) {
                        System.out.println("Invalid request type. Use READ, INSERT, LISTEN, CREATE, ATTR, or STOP");
                        continue;
                    }

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
                            System.out.println("Enter the pathname and monitor interval (minutes) separated by commas:");
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
                            System.out.println("Invalid request type. Use READ, INSERT, LISTEN, CREATE, ATTR or STOP");
                            continue;
                    }

                    client.makeRequest(requestType, input);
                 
                }
                scanner.close();
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Invalid app type. Use --server or --client");
        }
    }
}
