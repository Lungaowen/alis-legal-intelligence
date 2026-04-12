package za.co.alis.util;

import com.google.gson.*;
import okhttp3.*;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class OllamaClient {
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL = "deepseek-r1:1.5b"; // or qwen2.5-coder
    private final OkHttpClient client;
    private final Gson gson;

    public OllamaClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    public String analyzeDocument(String documentText, String lawRulesContext) throws IOException {
        String prompt = buildPrompt(documentText, lawRulesContext);
        
        JsonObject reqBody = new JsonObject();
        reqBody.addProperty("model", MODEL);
        reqBody.addProperty("prompt", prompt);
        reqBody.addProperty("stream", false);
        reqBody.addProperty("format", "json");

        Request request = new Request.Builder()
                .url(OLLAMA_URL)
                .post(RequestBody.create(reqBody.toString(), MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Ollama error: " + response);
            JsonObject jsonResp = gson.fromJson(response.body().string(), JsonObject.class);
            return jsonResp.get("response").getAsString();
        }
    }

    private String buildPrompt(String documentText, String lawRulesContext) {
        return """
            You are a legal AI assistant specialized in South African contract law.
            Analyze the following document and return a JSON object with this exact structure:
            {
              "deal_type": "string",
              "sector": "string",
              "clauses": [
                {
                  "clause_text": "exact text from document",
                  "risk_level": "Low|Medium|High|Critical",
                  "suggestion": "improvement suggestion if any"
                }
              ]
            }

            Relevant South African legislation and compliance rules:
            %s

            Document text:
            %s

            Important: Output only valid JSON. No extra commentary.
            """.formatted(lawRulesContext, documentText);
    }

    public static String computeSHA256(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
