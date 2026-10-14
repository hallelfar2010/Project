import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class MainServer {

    public static void main(String[] args) throws IOException {

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/add", new AddTransactionHandler());

        server.createContext("/list", new ListTransactionsHandler());

        server.createContext("/delete", new DeleteTransactionHandler());

        server.start();

        System.out.println("The Budget Server is running on http://localhost:8080");
    }

    static class AddTransactionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
            BufferedReader br = new BufferedReader(isr);

            String formData = br.readLine();

            String[] pairs = formData.split("&");

            String description = pairs[0].split("=")[1];
            String amount = pairs[1].split("=")[1];
            String type = pairs[2].split("=")[1];

            String notes = "";
            if(pairs.length > 3) {
                String[] notesSplit = pairs[3].split("=");
                if(notesSplit.length > 1) {
                    notes = notesSplit[1];
                }
            }

            description = URLDecoder.decode(description, "UTF-8");
            type = URLDecoder.decode(type, "UTF-8");
            notes = URLDecoder.decode(notes, "UTF-8");

            FileWriter fw = new FileWriter("budget_database.txt", true);
            PrintWriter pw = new PrintWriter(fw);

            pw.println(description + "," + amount + "," + type + "," + notes);

            pw.close();

            String response = "OK";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }


    static class ListTransactionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");

            String allData = "";
            try {
                FileReader fr = new FileReader("budget_database.txt");
                BufferedReader br = new BufferedReader(fr);
                String line;

                while ((line = br.readLine()) != null ) {
                    allData += line + "\n";
                }
                br.close();
            } catch (IOException e) {
                allData = "אין נתונים עדיין.";
            }

            exchange.sendResponseHeaders(200, allData.getBytes("UTF-8").length);
            OutputStream os = exchange.getResponseBody();
            os.write(allData.getBytes("UTF-8"));
            os.close();
        }
    }

    static class DeleteTransactionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
            BufferedReader br = new BufferedReader(isr);
            String formData = br.readLine();

            int indexToDelete = Integer.parseInt(formData.split("=")[1]);

            List<String> lines = new ArrayList<>();
            try {
                FileReader fr = new FileReader("budget_database.txt");
                BufferedReader fileBr = new BufferedReader(fr);
                String line;
                while ((line = fileBr.readLine()) != null) {
                    lines.add(line);
                }
                fileBr.close();
            } catch (IOException e) {}

            // מחיקת השורה הספציפית מתוך הרשימה לפי המספר הסידורי שלה
            if (indexToDelete >= 0 && indexToDelete < lines.size()) {
                lines.remove(indexToDelete);
            }

            // כתיבת כל הרשימה המעודכנת מחדש לתוך הקובץ
            // שים לב: הפעם השתמשנו ב-'false', כלומר אנחנו דורסים את הקובץ הישן לחלוטין וכותבים את הרשימה החדשה בלבד
            FileWriter fw = new FileWriter("budget_database.txt", false);
            PrintWriter pw = new PrintWriter(fw);
            for (String s : lines) {
                pw.println(s); // כותב כל שורה שנשארה ברשימה
            }
            pw.close();

            // החזרת אישור שהמחיקה בוצעה בהצלחה
            String response = "Deleted";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}