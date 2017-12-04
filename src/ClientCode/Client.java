package ClientCode;

import MiddlewareCode.CommunicationsConfig;
import Transactions.TransactionalMiddleware;

import ResourceManagerCode.ReservableType;
import ResourceManagerCode.*;
import Transactions.TransactionalRequestSender;

import java.rmi.RemoteException;

import java.util.*;
import java.io.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


public class Client {

    private static List<Integer> activeTransactions = new ArrayList<Integer>();

    public static void main(String args[]) {

        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(Level.SEVERE);
        for (Handler h: rootLogger.getHandlers()) {
            h.setLevel(Level.SEVERE);
        }

        new ResourceManagerImpl(CommunicationsConfig.Companion.getCUSTOMER_REQUEST(), CommunicationsConfig.Companion.getCUSTOMER_REPLY());
        new ResourceManagerImpl(CommunicationsConfig.Companion.getFLIGHT_REQUEST(), CommunicationsConfig.Companion.getFLIGHT_REPLY());
        new ResourceManagerImpl(CommunicationsConfig.Companion.getHOTEL_REQUEST(), CommunicationsConfig.Companion.getHOTEL_REPLY());
        new ResourceManagerImpl(CommunicationsConfig.Companion.getCAR_REQUEST(), CommunicationsConfig.Companion.getCAR_REPLY());

        new ResourceManagerImpl(CommunicationsConfig.Companion.getCUSTOMER_REQUEST(), CommunicationsConfig.Companion.getCUSTOMER_REPLY());
        new ResourceManagerImpl(CommunicationsConfig.Companion.getFLIGHT_REQUEST(), CommunicationsConfig.Companion.getFLIGHT_REPLY());
        new ResourceManagerImpl(CommunicationsConfig.Companion.getHOTEL_REQUEST(), CommunicationsConfig.Companion.getHOTEL_REPLY());
        new ResourceManagerImpl(CommunicationsConfig.Companion.getCAR_REQUEST(), CommunicationsConfig.Companion.getCAR_REPLY());

        new TransactionalMiddleware(0);
        new TransactionalMiddleware(0);
        TransactionalRequestSender client = new TransactionalRequestSender(0);


        System.out.println("\n\n\tClient Interface");
        System.out.println("Type \"help\" for list of supported commands");


        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        List<String> arglist = null;
        while (true) {
            System.out.print("\n>");
            try {
                arglist = Arrays.asList(stdin.readLine().split(" "));
                if (arglist.size() == 0)
                    continue;
            } catch (IOException io) {
                System.out.println("Unable to read from stdin.");
                System.exit(1);
            }


            int transaction;
            switch (head(arglist)) {
                case "createresource":
                    transaction = parseTransactionId(tail(arglist));
                    if (transaction != -1) {
                        try {
                            createResource(client, transaction, tail(tail(arglist)));
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                    break;
                case "updateresource":
                    transaction = parseTransactionId(tail(arglist));
                    if (transaction != -1) {
                        try {
                            updateResource(client, transaction, tail(tail(arglist)));
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                    break;
                case "reserveresource":
                    transaction = parseTransactionId(tail(arglist));

                    if (transaction != -1) {
                        try {
                            reserveResource(client, transaction, tail(tail(arglist)));
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                    break;

                case "deleteresource":
                    transaction = parseTransactionId(tail(arglist));
                    if (transaction != -1) {
                        try {
                            deleteResource(client, transaction, tail(tail(arglist)));
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                    break;

                case "queryresource":
                    transaction = parseTransactionId(tail(arglist));
                    if (transaction != -1) {
                        try {
                            queryResource(client, transaction, tail(tail(arglist)));
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                    break;
                case "uniquecustomerid":
                    transaction = parseTransactionId(tail(arglist));
                    if (transaction != -1) {
                        try {
                            uniqueCustomerId(client, transaction, tail(tail(arglist)));
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                    break;
                case "createcustomer":
                    transaction = parseTransactionId(tail(arglist));
                    if (transaction != -1) {
                        try {
                            createCustomer(client, transaction, tail(tail(arglist)));
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                    break;
                case "deletecustomer":
                    transaction = parseTransactionId(tail(arglist));
                    if (transaction != -1) {
                        try {
                            deleteCustomer(client, transaction, tail(tail(arglist)));
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                    break;
                case "customeraddreservation":
                    transaction = parseTransactionId(tail(arglist));
                    if (transaction != -1) {
                        try {
                            customerAddReservation(client, transaction, tail(tail(arglist)));
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                    break;
                case "customerremovereservation":
                    transaction = parseTransactionId(tail(arglist));
                    if (transaction != -1) {
                        try {
                            customerRemoveReservation(client, transaction, tail(tail(arglist)));
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                    break;
                case "querycustomer":
                    transaction = parseTransactionId(tail(arglist));
                    if (transaction != -1) {
                        try {
                            queryCustomer(client, transaction, tail(tail(arglist)));
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                    break;
                case "itinerary":
                    transaction = parseTransactionId(tail(arglist));
                    if (transaction != -1) {
                        try {
                            itinerary(client, transaction, tail(tail(arglist)));
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                    break;

                case "start":
                    try {
                        start(client, activeTransactions, arglist);
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                    break;
                case "commit":
                    commit(client, activeTransactions, tail(arglist));
                    break;
                case "abort":
                    abort(client, activeTransactions, tail(arglist));
                    break;
                case "help":
                    listCommands();
                    break;
                case "quit":
                    System.exit(0);
                default:
                    System.out.println("The interface does not support this command.");
                    //listCommands();
                    break;
            }
        }
    }


    private static void createResource(TransactionalRequestSender client, int transaction, List<String> args) throws RemoteException {
        if (args.size() != 4 || ((typeOf(args.get(0))) == null)) {
            wrongNumber("createResource");
            return;
        }
        boolean success = client.createResource(transaction, typeOf(args.get(0)), args.get(1), Integer.parseInt(args.get(2)), Integer.parseInt(args.get(3)));
        System.out.println(success ? "Resource created." : "Could not create resource.");

    }

    private static void updateResource(TransactionalRequestSender client, int transaction, List<String> args) throws RemoteException {
        if (args.size() != 3) {
            wrongNumber("updateResource");
            return;
        }
        System.out.println(args);
        boolean success = client.updateResource(transaction, args.get(0), Integer.parseInt(args.get(1)), Integer.parseInt(args.get(2)));
        System.out.println(success ? "Resource updated." : "Could not update resource.");

    }

    private static void reserveResource(TransactionalRequestSender client, int transaction, List<String> args) throws RemoteException {
        if (args.size() != 2) {
            wrongNumber("reserveResource");
            return;
        }
        boolean success = client.reserveResource(transaction, args.get(0), Integer.parseInt(args.get(1)));
        System.out.println(success ? "Resource reserved." : "Could not reserve resource.");

    }

    private static void deleteResource(TransactionalRequestSender client, int transaction, List<String> args) throws RemoteException {
        if (args.size() != 1) {
            wrongNumber("deleteResource");
            return;
        }
        boolean success = client.deleteResource(transaction, args.get(0));
        System.out.println(success ? "Resource deleted." : "Could not delete resource.");

    }

    private static void queryResource(TransactionalRequestSender client, int transaction, List<String> args) throws RemoteException {
        if (args.size() != 1) {
            wrongNumber("queryResource");
            return;
        }
        Resource res = client.queryResource(transaction, args.get(0));
        System.out.println("Resource returned:" + res);
    }

    private static void uniqueCustomerId(TransactionalRequestSender client, int transaction, List<String> args) throws RemoteException {
        if (args.size() > 1) {
            wrongNumber("uniqueCustomerId");
            return;
        }
        int ID = client.uniqueCustomerId(transaction);
        System.out.println("Unique customer id returned:" + ID);

    }

    private static void createCustomer(TransactionalRequestSender client, int transaction, List<String> args) throws RemoteException {
        if (args.size() != 1) {
            wrongNumber("createCustomer");
            return;
        }
        boolean success = client.createCustomer(transaction, Integer.parseInt(args.get(0)));
        System.out.println(success ? "Customer created." : "Could not create customer.");

    }

    private static void deleteCustomer(TransactionalRequestSender client, int transaction, List<String> args) throws RemoteException {
        if (args.size() != 1) {
            wrongNumber("deleteCustomer");
            return;
        }
        boolean success = client.deleteCustomer(transaction, Integer.parseInt(args.get(0)));
        System.out.println(success ? "Customer deleted." : "Could not delete customer.");
    }

    private static void customerAddReservation(TransactionalRequestSender client, int transaction, List<String> args) throws RemoteException {
        if (args.size() != 3) {
            wrongNumber("customerAddReservation");
            return;
        }
        boolean success = client.customerAddReservation(transaction, Integer.parseInt(args.get(0)), Integer.parseInt(args.get(1)), args.get(2));
        System.out.println(success ? "Customer reserved resource." : "Could not reserve resource.");
    }

    private static void customerRemoveReservation(TransactionalRequestSender client, int transaction, List<String> args) throws RemoteException {
        if (args.size() != 2) {
            wrongNumber("customerRemoveReservation");
            return;
        }

        boolean success = client.customerRemoveReservation(transaction, Integer.parseInt(args.get(0)), Integer.parseInt(args.get(1)));
        System.out.println(success ? "Customer removed reservation." : "Could not remove reservation.");

    }

    private static void queryCustomer(TransactionalRequestSender client, int transaction, List<String> args) throws RemoteException {

        if (args.size() != 1) {
            wrongNumber("queryCustomer");
            return;
        }

        Customer bill = client.queryCustomer(transaction, Integer.parseInt(args.get(0)));
        System.out.println(bill);
    }

    private static void itinerary(TransactionalRequestSender client, int transaction, List<String> args) throws RemoteException {
        if (args.size() < 2 || args.size() % 2 != 1) {
            wrongNumber("itinerary");
            return;
        }
        Map<Integer, String> map = new HashMap();
        for (int i = 1; i < args.size(); i+=2) {
            map.put(Integer.parseInt(args.get(i)), args.get(i+1));
        }
        boolean success = client.itinerary(transaction, Integer.parseInt(args.get(0)), map);
        System.out.println(success ? "Itinerary created." : "Could not create itinerary.");

    }


    private static void start(TransactionalRequestSender client, List<Integer> activeTransactions, List<String> args) throws RemoteException {

        if (args.size() != 2) {
            wrongNumber("start");
            return;
        }
        int transaction = Integer.parseInt(args.get(1));
        boolean success = client.start(transaction);
        if (success) {
            activeTransactions.add(transaction);
            System.out.println("Started transaction: " + transaction + ".");
        } else {
            System.out.println("Start transaction failed.");
        }
    }

    private static void commit(TransactionalRequestSender client, List<Integer> activeTransactions, List<String> args) {
        if (args.size() != 1) {
            wrongNumber("commit");
            return;
        }
        int transaction = Integer.parseInt(args.get(0));
        activeTransactions.remove(new Integer(transaction));
        boolean exists = client.commit(transaction);
        System.out.println(exists ? "Committed transaction: " + transaction + "." : "Could not commit transaction " + transaction + ". Transaction does not exist.");
    }

    private static void abort(TransactionalRequestSender client, List<Integer> activeTransactions, List<String> args) {
        if (args.size() != 1) {
            wrongNumber("abort");
            return;
        }
        int transaction = Integer.parseInt(args.get(0));
        activeTransactions.remove(new Integer(transaction));
        boolean success = client.abort(transaction);
        if (success) {
            System.out.println("Transaction successfully aborted.");
        }
    }

    private static ReservableType typeOf(String s) {
        switch (s) {
            case "hotel":
                return ReservableType.HOTEL;
            case "flight":
                return ReservableType.FLIGHT;
            case "car":
                return ReservableType.CAR;
            default:
                return null;
        }
    }

    private static String head(List<String> args) {
        if (args.size() > 0)
            return args.get(0).trim().toLowerCase();
        return "";
    }

    private static List<String> tail(List<String> args) {
        if (args.size() > 0)
            return args.subList(1, args.size());
        return Collections.emptyList();
    }

    private static int parseTransactionId(List<String> args) {
        int transaction;
        try {
            transaction = Integer.parseInt(head(args));
            if (!activeTransactions.contains(transaction)) {
                System.out.println("Invalid transaction number. Please start the transaction with <start> command.");
                return -1;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid transaction number. All commands should start with a transaction identifier.");
            return -1;
        }
        return transaction;
    }


    private static void listCommands() {
        System.out.println("Commands accepted by the interface are:\n");
        System.out.println("help\n");
        System.out.println("createresource <transactionId: Int> <type: ReservableType> <id: String> <totalQuantity: Int> <price: Int>\n");
        System.out.println("updateresource <transactionId: Int> <id: String> <newTotalQuantity: Int> <newPrice: Int>\n");
        System.out.println("reserveresource <transactionId: Int> <resourceId: String> <reservationQuantity: Int>\n");
        System.out.println("deleteresource <transactionId: Int> <id: String>\n");
        System.out.println("queryresource <transactionId: Int> <resourceId: String>\n");
        System.out.println("uniquecustomerid <transactionId: Int>\n");
        System.out.println("createcustomer <transactionId: Int> <customerId: Int>\n");
        System.out.println("deletecustomer <transactionId: Int> <customerId: Int>\n");
        System.out.println("customeraddreservation <transactionId: Int> <customerId: Int> <reservationId: Int> <reservableItem: ReservableItem>");
        System.out.println("e.g. customeraddreservation 12 5 7 hotel hotel_1 1 20\n");
        System.out.println("customerremovereservation <transactionId: Int> <customerId: Int> <reservationId: Int>\n");
        System.out.println("querycustomer <transactionId: Int> <customerId: Int>\n");
        System.out.println("itinerary <transactionId: Int> <customerId: Int> <reservationResources: MutableMap<Int, ReservableItem>>\n");
        System.out.println("start <transactionId: Int>\n");
        System.out.println("commit <transactionId: Int>\n");
        System.out.println("abort <transactionId: Int>\n");
        System.out.println("quit");
    }


    private static void wrongNumber(String command) {
        System.out.println("Invalid number of arguments provided to <" + command + "> command.");
        //listCommands();
    }
}

