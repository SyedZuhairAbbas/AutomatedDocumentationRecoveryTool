package org.example;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.UUID;

public class ApiService {

    private static final String BACKEND_URL = "http://127.0.0.1:8000/analyze";

    public String analyzeFile(File file) throws IOException, InterruptedException {
        String boundary = UUID.randomUUID().toString();
        byte[] fileBytes = Files.readAllBytes(file.toPath());

        String boundaryLine = "--" + boundary;
        String newLine = "\r\n";

        StringBuilder sb = new StringBuilder();
        sb.append(boundaryLine).append(newLine);
        sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                .append(file.getName()).append("\"").append(newLine);
        sb.append("Content-Type: text/plain").append(newLine);
        sb.append(newLine);

        byte[] header = sb.toString().getBytes();
        byte[] footer = (newLine + boundaryLine + "--" + newLine).getBytes();

        byte[] body = new byte[header.length + fileBytes.length + footer.length];
        System.arraycopy(header, 0, body, 0, header.length);
        System.arraycopy(fileBytes, 0, body, header.length, fileBytes.length);
        System.arraycopy(footer, 0, body, header.length + fileBytes.length, footer.length);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BACKEND_URL))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}