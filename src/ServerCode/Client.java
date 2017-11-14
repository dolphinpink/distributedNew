package ServerCode;

import Transactions.TransactionalMiddleware;

import ResourceManagerCode.ReservableType;
import Tcp.*;
import Transactions.TransactionalMiddleware;
import Transactions.TransactionalRequestReceiver;
import Transactions.TransactionalRequestSender;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;

import java.util.*;
import java.io.*;


public class Client {

    private static List<Integer> activeTransactions = new ArrayList<Integer>();

    public static void main(String args[]) throws RemoteException
    {

        TcpRequestReceiver requestReceiverFlight = new TcpRequestReceiver(new ResourceManagerImpl(), 8087);
        requestReceiverFlight.runServer();

        TcpRequestReceiver requestReceiverHotel = new TcpRequestReceiver(new ResourceManagerImpl(), 8088);
        requestReceiverHotel.runServer();

        TcpRequestReceiver requestReceiverCar = new TcpRequestReceiver(new ResourceManagerImpl(), 8089);
        requestReceiverCar.runServer();

        TcpRequestReceiver requestReceiverCustomer = new TcpRequestReceiver(new ResourceManagerImpl(), 8090);
        requestReceiverCustomer.runServer();

        Thread.sleep(500);

        TransactionalRequestReceiver midware = new TransactionalRequestReceiver(new TransactionalMiddleware("127.0.0.1"), 8086);
        midware.runServer();

        Thread.sleep(500);

        TransactionalRequestSender client = new TransactionalRequestSender(8086, "127.0.0.1");



        System.out.println("\n\n\tClient Interface");
        System.out.println("Type \"help\" for list of supported commands");


        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        List<String> arglist = null;
        while(true){
            System.out.print("\n>");
            try
            {
                arglist = Arrays.asList(stdin.readLine().split(" "));
                if (arglist.size() == 0)
                    continue;
            }
            catch (IOException io)
            {
                System.out.println("Unable to read from stdin.");
                System.exit(1);
            }



            int transaction;
            switch (head(arglist))
            {
                case "add":
                    transaction = parseTransactionId(tail(arglist));
                    if (transaction != -1)
                        add(client, transaction, tail(tail(arglist)));
                    break;
                case "delete":
                    transaction = parseTransactionId(tail(arglist));
                    if (transaction != -1)
                        delete(client, transaction, tail(tail(arglist)));
                    break;
                case "query":
                    transaction = parseTransactionId(tail(arglist));
                    if (transaction != -1)
                        query(client, transaction, tail(tail(arglist)));
                    break;
                case "reserve":
                    transaction = parseTransactionId(tail(arglist));
                    if (transaction != -1)
                        reserve(client, transaction, tail(tail(arglist)));
                    break;
                case "start":
                    start(client, activeTransactions, tail(tail(arglist)));
                    break;
                case "commit":
                    commit(client, activeTransactions, tail(tail(arglist)));
                    break;
                case "abort":
                    abort(client, activeTransactions, tail(tail(arglist)));
                    break;
                case "help":
                    printHelp(head(tail(arglist)));
                default:
                    System.out.println("The interface does not support this command.");
                    break;
            }
        }
    }

    private static void add(TransactionalRequestSender client, int transaction, List<String> args) throws RemoteException
    {
        if (args.size() < 1)
        {
            wrongNumber("add");
            return;
        }
        String[] values = (String[]) tail(args).toArray();
        boolean success;
        int id;
        switch (head(args))
        {
            case "customer":
                if (values.length == 0)
                {
                    id = client.newCustomer(transaction);
                    success = true;
                }
                else if (values.length == 1)
                {
                    id = Integer.parseInt(values[0]);
                    success = client.newCustomer(transaction, id);
                }
                else
                {
                    wrongNumber("add");
                    break;
                }
                System.out.println(success ? "Customer " + id +" added." : "Could not add customer.");
                break;
            default:
                if (values.length != 3 || typeOf(head(args)) == ReservableItem.Type.UNKNOWN)
                {
                    wrongNumber("add");
                    break;
                }
                success = client.add(transaction, new ReservableItem(typeOf(head(args)), values[0], Integer.parseInt(values[1]), Integer.parseInt(values[2])));
                System.out.println(success ? "Item added." : "Could not add item.");
                break;
        }
    }

    private static void query(TransactionalRequestSender client, int transaction, List<String> args) throws RemoteException
    {
        if (args.size() < 1)
        {
            wrongNumber("query");
            return;
        }
        String[] values = (String[]) tail(args).toArray();
        int count;
        switch (head(args))
        {
            case "customer":
                if (values.length != 1)
                {
                    wrongNumber("query");
                    return;
                }
                String bill = client.queryCustomerInfo(transaction, Integer.parseInt(values[0]));
                System.out.println(bill);
                break;
            case "count":
                if (values.length != 2|| typeOf(values[0]) == ReservableItem.Type.UNKNOWN)
                {
                    wrongNumber("query");
                    return;
                }
                count = client.queryCount(transaction, typeOf(values[0]), values[1]);
                System.out.println("Count: " + count + ".");
                break;
            case "price":
                if (values.length != 2 || typeOf(values[0]) == ReservableItem.Type.UNKNOWN)
                {
                    wrongNumber("query");
                    return;
                }
                count = client.queryPrice(transaction, typeOf(values[0]), values[1]);
                System.out.println("Price: " + count + ".");
                break;
            default:
                System.out.println("Cannot query property: " + head(args) + ".");
                break;
        }
    }

    private static void delete(TransactionalRequestSender client, int transaction, List<String> args) throws RemoteException
    {
        if (args.size() < 1)
        {
            wrongNumber("delete");
            return;
        }
        String[] values = (String[]) tail(args).toArray();
        boolean success;
        switch (head(args))
        {
            case "customer":
                if (values.length == 1)
                {
                    success = client.newCustomer(transaction, Integer.parseInt(values[0]));
                    System.out.println(success ? "Customer deleted." : "Could not delete customer.");
                }
                else
                {
                    wrongNumber("delete");
                    return;
                }
                break;
            default:
                if (values.length != 1 || typeOf(head(args)) == ReservableItem.Type.UNKNOWN)
                {
                    wrongNumber("delete");
                    return;
                }
                success = client.delete(transaction, typeOf(values[0]), values[0]);
                System.out.println(success ? "Item deleted." : "Could not delete item.");
                break;
        }
    }

    private static void reserve(TransactionalRequestSender client, int transaction, List<String> args) throws RemoteException
    {
        if (args.size() != 3)
        {
            wrongNumber("reserve");
            return;
        }
        String[] values = (String[]) tail(args).toArray();
        boolean success = client.reserve(transaction, Integer.parseInt(values[0]), typeOf(values[1]), values[2]);
        System.out.println(success ? "Item reserved." : "Could not reserve item.");
    }

    private static void start(TransactionalRequestSender client, List<Integer> activeTransactions, List<String> args) throws RemoteException
    {
        if (args.size() != 0)
        {
            wrongNumber("start");
            return;
        }
        int transaction = client.start();
        activeTransactions.add(transaction);
        System.out.println("Started transaction: " + transaction + ".");
    }

    private static void commit(TransactionalRequestSender client, List<Integer> activeTransactions, List<String> args) throws RemoteException, InvalidTransactionException, TransactionAbortedException
    {
        if (args.size() != 1)
        {
            wrongNumber("commit");
            return;
        }
        int transaction = Integer.parseInt(args.get(0));
        activeTransactions.remove(transaction);
        boolean exists = client.commit(transaction);
        System.out.println(exists ? "Committed transaction: " + transaction + "." : "Could not commit transaction " + transaction + ". Transaction does not exist.");
    }

    private static void abort(TransactionalRequestSender client, List<Integer> activeTransactions, List<String> args) throws RemoteException, InvalidTransactionException
    {
        if (args.size() != 1)
        {
            wrongNumber("abort");
            return;
        }
        int transaction = Integer.parseInt(args.get(0));
        activeTransactions.remove(transaction);
        client.abort(transaction);
    }

    private static String head(List<String> args)
    {
        if (args.size() > 0)
            return args.get(0).trim().toLowerCase();
        return "";
    }

    private static List<String> tail(List<String> args)
    {
        if (args.size() > 0)
            return args.subList(1, args.size());
        return Collections.emptyList();
    }

    private static ReservableItem.Type typeOf(String type)
    {
        switch (type.trim().toLowerCase())
        {
            case "car":
                return ReservableItem.Type.CAR;
            case "room":
                return ReservableItem.Type.HOTEL;
            case "flight":
                return ReservableItem.Type.FLIGHT;
            default:
                return ReservableItem.Type.UNKNOWN;
        }
    }

    private static int parseTransactionId(List<String> args)
    {
        int transaction;
        try
        {
            transaction = Integer.parseInt(head(args));
            if (!activeTransactions.contains(transaction))
            {
                System.out.println("Invalid transaction number. Please start the transaction with <start> command.");
                return -1;
            }
        }
        catch (NumberFormatException e)
        {
            System.out.println("Invalid transaction number. All commands should start with a transaction identifier.");
            return -1;
        }
        return transaction;
    }


    public void listCommands()
    {
        System.out.println("\nWelcome to the client interface provided to test your project.");
        System.out.println("Commands accepted by the interface are:");
        System.out.println("help");
        System.out.println("newflight\nnewcar\nnewroom\nnewcustomer\nnewcusomterid\ndeleteflight\ndeletecar\ndeleteroom");
        System.out.println("deletecustomer\nqueryflight\nquerycar\nqueryroom\nquerycustomer");
        System.out.println("queryflightprice\nquerycarprice\nqueryroomprice");
        System.out.println("reserveflight\nreservecar\nreserveroom\nitinerary");
        System.out.println("nquit");
        System.out.println("\ntype help, <commandname> for detailed info(NOTE the use of comma).");
    }



    }

    public static void wrongNumber(String command)
    {
        System.out.println("Invalid number of arguments provided to <" + command + "> command.");
        printHelp(command);
    }

    private static void printHelp(String command)
    {
        switch (command.trim().toLowerCase())
        {
            case "":
                break;
            case "add":
                break;
            case "delete":
                break;
            case "query":
                break;
            case "reserve":
                break;
            case "start":
                break;
            case "commit":
                break;
            case "abort":
                break;
            default:
                break;
        }
    }

}
