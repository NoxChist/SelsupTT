package ru.selsup;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;

public class CrptApi {
    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final HttpClient client;
    private final Gson gson;
    private final Semaphore semaphore;
    private final int poolSize= 1;
    private final int initDelay = 0;
    private final int callPeriod = 1;


    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.client = HttpClient.newHttpClient();
        this.gson = new Gson();
        this.semaphore = new Semaphore(requestLimit);

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(poolSize);
        executor.scheduleAtFixedRate(() -> {
            int permits = requestLimit - semaphore.availablePermits();
            if (permits > 0) {
                semaphore.release(permits);
            }
        }, initDelay, callPeriod, timeUnit);
    }

    public void createDocument(Document document, String signature) throws IOException, InterruptedException {
        semaphore.acquire();
        String requestBody = gson.toJson(document);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        var response =client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            System.out.println(requestBody);
            System.out.println(response);
        }
    }

   protected static class Document {
       private Description description;
       private String doc_id;
       private String doc_status;
       private final String doc_type = "LP_INTRODUCE_GOODS";
       private boolean importRequest;
       private String owner_inn;
       private String participant_inn;
       private String producer_inn;
       private String production_date;
       private String production_type;
       private Product[] products;
       private String reg_date;
       private String reg_number;
   }

    protected static class Description {
        private String participantInn;
    }

    protected static class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        public String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
    }

    public static void main(String[] args) {
        try {
            CrptApi api = new CrptApi(TimeUnit.MINUTES, 5);

            Document document = new Document();
            document.description = new Description();

            document.description.participantInn = "description";
            document.doc_id = "doc_id";
            document.doc_status = "doc_status";
            document.importRequest = true;
            document.owner_inn = "owner_inn";
            document.participant_inn = "participant_inn";
            document.producer_inn = "producer_inn";
            document.production_date = "2020-01-23";
            document.production_type = "production_type";
            document.reg_date = "2020-01-23";
            document.reg_number = "string";

            Product product = new Product();
            product.certificate_document = "string1";
            product.certificate_document_date = "2020-01-23";
            product.certificate_document_number = "string1";
            product.owner_inn = "owner_inn1";
            product.producer_inn = "producer_inn1";
            product.production_date = "2020-01-23";
            product.tnved_code = "code1";
            product.uit_code = "uit_code1";
            product.uitu_code = "uitu_code1";

            document.products = new Product[]{product};

            String signature = "your-signature";

            api.createDocument(document, signature);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
