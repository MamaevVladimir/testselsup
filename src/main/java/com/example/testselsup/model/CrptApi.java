package com.example.testselsup.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.*;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class CrptApi {
    private static final Lock lock = new ReentrantLock();
    private static final AtomicInteger requestCount = new AtomicInteger(0);
    private static TimeUnit timeUnit;
    private static int requestLimit;
    private static final String LP_INTRODUCE_GOODS = "doc_type";
    private static final String SIGNATURE = "signature";

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        CrptApi.timeUnit = timeUnit;
        CrptApi.requestLimit = requestLimit;
    }

    public static void main(String[] args) {
        Map<String, Integer> goods = new HashMap<>();
        goods.put(LP_INTRODUCE_GOODS, 109);

        Product product = Product.builder()
                .certificate_document("")
                .certificate_document_date(LocalDate.of(2020, 1, 23))
                .certificate_document_number("")
                .owner_inn("")
                .producer_inn("")
                .tnved_code("")
                .uit_code("")
                .uitu_code("")
                .build();

        ArrayList<Product> products = new ArrayList<>();
        products.add(product);

        Document document = Document.builder()
                .participant_inn("")
                .doc_id("")
                .doc_status("")
                .goods(goods)
                .importRequest(true)
                .owner_inn("")
                .participantInn("")
                .producer_inn("")
                .production_date(LocalDate.of(2020, 1, 23))
                .production_type("")
                .production_date(LocalDate.of(2020, 1, 23))
                .products(products)
                .reg_date(LocalDate.of(2020, 1, 23))
                .reg_number("")
                .build();

        createIntroduceGoodsDocument(document, SIGNATURE);
    }

    private static void createIntroduceGoodsDocument(Document document, String signature) {
        lock.lock();

        Scanner sc = new Scanner(System.in);
        System.out.println("Введите количество секунд для интервала: ");
        int waitSeconds = sc.nextInt();

        System.out.println("Введите максимальное число запросов: ");
        requestLimit = sc.nextInt();

        try {
            if (requestCount.get() < requestLimit) {
                sendRequest();
                requestCount.incrementAndGet();
            } else {
                while (requestCount.get() >= requestLimit) {
                    try {
                        TimeUnit.SECONDS.sleep(waitSeconds);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                sendRequest();
                requestCount.incrementAndGet();
            }
        } finally {
            lock.unlock();
        }
    }

    private static void sendRequest() {
        try {
            URL url = new URL("https://ismp.crpt.ru/api/v3/lk/documents/create");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String inputString = "{\"description\":\n" +
                    "{ \"participantInn\": \"string\" }, \"doc_id\": \"string\", \"doc_status\": \"string\",\n" +
                    "\"doc_type\": \"LP_INTRODUCE_GOODS\", 109 \"importRequest\": true,\n" +
                    "\"owner_inn\": \"string\", \"participant_inn\": \"string\", \"producer_inn\":\n" +
                    "\"string\", \"production_date\": \"2020-01-23\", \"production_type\": \"string\",\n" +
                    "\"products\": [ { \"certificate_document\": \"string\",\n" +
                    "\"certificate_document_date\": \"2020-01-23\",\n" +
                    "\"certificate_document_number\": \"string\", \"owner_inn\": \"string\",\n" +
                    "\"producer_inn\": \"string\", \"production_date\": \"2020-01-23\",\n" +
                    "\"tnved_code\": \"string\", \"uit_code\": \"string\", \"uitu_code\": \"string\" } ],\n" +
                    "\"reg_date\": \"2020-01-23\", \"reg_number\": \"string\"}";

            try (DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream())) {
                dataOutputStream.writeBytes(inputString);
                dataOutputStream.flush();
            }

            int responseCode = connection.getResponseCode();
            System.out.println("Response code: " + responseCode);

            StringBuilder response;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }

            System.out.println("Response body: " + response.toString());
        } catch (Exception ex) {
            throw new RuntimeException("Превышено число запросов");
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @JsonRootName(value = "description")
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static class Document {
        private String participantInn;
        private String doc_id;
        private String doc_status;
        private Map<String, Integer> goods = new HashMap<>();
        private Boolean importRequest = false;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        @JsonFormat
                (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate production_date;
        private String production_type;
        private ArrayList<Product> products = new ArrayList<>();
        @JsonFormat
                (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate reg_date;
        private String reg_number;

        @JsonAnyGetter
        public Map<String, Integer> getGoods() {
            return goods;
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static class Product {
        private String certificate_document;
        @JsonFormat
                (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        @JsonFormat
                (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
    }
}
