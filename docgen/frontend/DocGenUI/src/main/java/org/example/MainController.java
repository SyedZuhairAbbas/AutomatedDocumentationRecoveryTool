package org.example;

import com.google.gson.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import java.io.File;

public class MainController {

    @FXML private Label fileNameLabel;
    @FXML private TextArea outputArea;
    @FXML private Button analyzeBtn;
    @FXML private Button exportBtn;
    @FXML private HBox summaryBox;
    @FXML private Label totalFunctionsLabel;
    @FXML private Label totalClassesLabel;
    @FXML private Label totalParamsLabel;
    @FXML private Label analysisStatusLabel;

    private File selectedFile;
    private final ApiService apiService = new ApiService();
    private String lastResult = "";

    @FXML
    private void handleFileSelect() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select a Python File");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Python Files", "*.py")
        );
        File file = chooser.showOpenDialog(null);
        if (file != null) {
            selectedFile = file;
            fileNameLabel.setText("Selected: " + file.getName());
            analyzeBtn.setDisable(false);
        }
    }

    @FXML
    private void handleAnalyze() {
        if (selectedFile == null) return;

        outputArea.setText("Analyzing...");
        analyzeBtn.setDisable(true);

        new Thread(() -> {
            try {
                String json = apiService.analyzeFile(selectedFile);
                String formatted = formatResponse(json);
                javafx.application.Platform.runLater(() -> {
                    outputArea.setText(formatted);
                    lastResult = formatted;
                    exportBtn.setDisable(false);
                    analyzeBtn.setDisable(false);

                    // update summary bar
                    JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                    JsonArray data = root.getAsJsonArray("data");

                    int functions = 0, classes = 0, params = 0;
                    for (JsonElement el : data) {
                        JsonObject item = el.getAsJsonObject();
                        if (item.get("type").getAsString().equals("function")) {
                            functions++;
                            params += item.getAsJsonArray("params").size();
                        } else if (item.get("type").getAsString().equals("class")) {
                            classes++;
                            for (JsonElement m : item.getAsJsonArray("methods")) {
                                params += m.getAsJsonObject().getAsJsonArray("params").size();
                            }
                        }
                    }

                    totalFunctionsLabel.setText("⚙️Functions: " + functions);
                    totalClassesLabel.setText("🏛 Classes: " + classes);
                    totalParamsLabel.setText("📥 Parameters: " + params);
                    analysisStatusLabel.setText("✅ Analysis Complete");
                    summaryBox.setVisible(true);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    outputArea.setText("Error: " + e.getMessage());
                    analyzeBtn.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleExport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Documentation");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Markdown File", "*.md")
        );
        chooser.setInitialFileName("documentation.md");
        File file = chooser.showSaveDialog(null);
        if (file != null) {
            try {
                java.nio.file.Files.writeString(file.toPath(), lastResult);
                outputArea.setText(lastResult + "\n\n✅ Exported to: " + file.getAbsolutePath());
            } catch (Exception e) {
                outputArea.setText("Export failed: " + e.getMessage());
            }
        }
    }

    private String formatResponse(String json) {
        StringBuilder sb = new StringBuilder();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        if (!root.get("status").getAsString().equals("success")) {
            return "❌ Error: " + root.get("message").getAsString();
        }

        JsonArray data = root.getAsJsonArray("data");
        sb.append("# Generated Documentation\n\n");

        for (JsonElement el : data) {
            JsonObject item = el.getAsJsonObject();
            String type = item.get("type").getAsString();
            String name = item.get("name").getAsString();
            int line = item.get("line").getAsInt();
            String docstring = item.get("docstring").getAsString();

            if (type.equals("class")) {
                sb.append("## 🏛 Class: ").append(name).append("\n");
                sb.append("📍 Line: ").append(line).append("\n");
                sb.append("📝 ").append(docstring).append("\n\n");

                JsonArray methods = item.getAsJsonArray("methods");
                if (methods.size() > 0) {
                    sb.append("### Methods:\n");
                    for (JsonElement m : methods) {
                        JsonObject method = m.getAsJsonObject();
                        sb.append("  - **").append(method.get("name").getAsString()).append("(");
                        JsonArray params = method.getAsJsonArray("params");
                        for (int i = 0; i < params.size(); i++) {
                            if (i > 0) sb.append(", ");
                            sb.append(params.get(i).getAsString());
                        }
                        sb.append(")**\n");
                        sb.append("    📝 ").append(method.get("docstring").getAsString()).append("\n");
                    }
                }
                sb.append("\n");

            } else if (type.equals("function")) {
                sb.append("## ⚙️ Function: ").append(name).append("\n");
                sb.append("📍 Line: ").append(line).append("\n");
                JsonArray params = item.getAsJsonArray("params");
                sb.append("📥 Parameters: ");
                if (params.size() == 0) {
                    sb.append("none\n");
                } else {
                    for (int i = 0; i < params.size(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(params.get(i).getAsString());
                    }
                    sb.append("\n");
                }
                sb.append("📝 ").append(docstring).append("\n\n");
            }
        }
        return sb.toString();
    }
}