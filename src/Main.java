import org.jetbrains.annotations.NotNull;
import utils.RequestType;
import utils.InvocationSemantics;

import java.util.Scanner;


public class Main {



    // print usage
    public static void printUsage() {
        System.out.println("Usage: java Main --server <server-port> <invocation-semantics> or java Main --client <server-address> <server-port> <invocation-semantics> <freshness-interval>");
        System.out.println("Mandatory arguments:");
        System.out.println("<server-address> - IP address of server");
        System.out.println("<server-port> - Port number to run server on");
        System.out.println("Optional arguments:");
        System.out.println("<invocation-semantics> - AT_LEAST_ONCE or AT_MOST_ONCE");
        System.out.println("<freshness-interval> - Time in milliseconds to wait before checking for file changes in server");
        System.out.println();
    }

    public static void main(@org.jetbrains.annotations.NotNull String @NotNull [] args) {
        System.out.println("Hello world!");

        printUsage();


        // if args is not of length 2, print error message
        if ((args.length < 3) || (args.length > 5)) {
            printUsage();
            return;
        }

        String appType = args[0]; // "--server" or "--client"

        if (appType.equals("--server")) {
            System.out.println("Starting server...");
            int serverPort = Integer.parseInt(args[1]);
            InvocationSemantics invocationSemantics = args.length == 3 ? InvocationSemantics.valueOf(args[2]) : InvocationSemantics.AT_LEAST_ONCE;
            try {
                server.Server server = new server.Server(serverPort, invocationSemantics);
                server.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (appType.equals("--client")) {
            System.out.println("Starting client...");
            String serverAddress = args[1];
            int serverPort = Integer.parseInt(args[2]);
            InvocationSemantics invocationSemantics = args.length == 4 ? InvocationSemantics.valueOf(args[3]) : InvocationSemantics.AT_LEAST_ONCE;
            int freshnessInterval = args.length == 5 ? Integer.parseInt(args[4]) : 0;

            try {
                client.Client client = new client.Client(serverAddress, serverPort, invocationSemantics, freshnessInterval);
                client.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Invalid app type. Use --server or --client");
        }
    }
}
