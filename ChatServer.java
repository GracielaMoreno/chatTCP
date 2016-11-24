package ec.edu.epn.redes.broadcast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

/**
 * Servidor de una sala de chat multiprocesos. cuando un cliente se conecta
 * al servidor recibe una respuesta, solicitando que ingrese un nombre o nick,
 * y mantiene esta petición hasta que el nombre que ingrese el cliente sea unico
 * luego el servidor marca este nombre como aceptado.
 * Todos los mensajes que un cliente envia serán enviados y difundidos con los otros
 * clientes. losMensajes difundidos tiene el preijo "MESSAGE"
 * 
 * Debido a que este es solo un ejemplo para enseñanza de un sencillo servidor
 * de chat, hay algunas configuraciones que no se han tomadoo en cuenta
 * Existen dos que osn  muy utiles:
 * 	1. El protocolo debe mejorar para q el cliente pueda enviar mensajes de desconexión 
 * 		y limpiar los mensajes del servidor
 *	2. El servidor deberia hacer algun acceso
 */

public class ChatServer {

    private static final int PORT = 9001; // El puerto que el servidor escucha


    /** Conjunto de nombres de cliente de la sala de chat. Permite que los nuevos
      *  clientes creen nombres de usuarios diferenstes que no esten en uso*/
    private static HashSet<String> names = new HashSet<String>();
    
    /** El conjunto de todos los mensajes que envian los clientes, lo que 
     * facilita el reenvio a los otros cliente*/
    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();
    
    public static void main(String[] args) throws Exception { 	//Aplicación del metodo main que solo escucha el puerto 
    															//y manipula los hilos o threads.
        System.out.println("The chat server is running.");
        
        ServerSocket listener = new ServerSocket(PORT);
        
        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
    }
    
    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for a dealing with a single client
     * and broadcasting its messages.
     */
    private static class Handler extends Thread {
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        /**
         * Constructs a handler thread, squirreling away the socket.
         * All the interesting work is done in the run method.
         */
        public Handler(Socket socket) {
            this.socket = socket;
        }
        /**
         * Services this thread's client by repeatedly requesting a
         * screen name until a unique one has been submitted, then
         * acknowledges the name and registers the output stream for
         * the client in a global set, then repeatedly gets inputs and
         * broadcasts them.
         */
        public void run() {
            try {

                // Create character streams for the socket.
                in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Request a name from this client.  Keep requesting until
                // a name is submitted that is not already used.  Note that
                // checking for the existence of a name and adding the name
                // must be done while locking the set of names.
                while (true) {
                    out.println("SUBMITNAME");
                    name = in.readLine();
                    if (name == null) {
                        return;
                    }
                    synchronized (names) {
                        if (!names.contains(name)) {
                            names.add(name);
                            break;
                        }
                    }
                }

                // Now that a successful name has been chosen, add the
                // socket's print writer to the set of all writers so
                // this client can receive broadcast messages.
                out.println("NAMEACCEPTED");
                writers.add(out);

                // Accept messages from this client and broadcast them.
                // Ignore other clients that cannot be broadcasted to.
                while (true) {
                    String input = in.readLine();
                    if (input == null) {
                        return;
                    }
                    for (PrintWriter writer : writers) {
                        writer.println("MESSAGE " + name + ": " + input);
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                // This client is going down!  Remove its name and its print
                // writer from the sets, and close its socket.
                if (name != null) {
                    names.remove(name);
                }
                if (out != null) {
                    writers.remove(out);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}

