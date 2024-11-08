import com.google.api.gax.rpc.ApiException;
import com.google.cloud.dialogflow.v2beta1.AgentName;
import com.google.cloud.dialogflow.v2beta1.Intent;
import com.google.cloud.dialogflow.v2beta1.Intent.TrainingPhrase;
import com.google.cloud.dialogflow.v2beta1.Intent.TrainingPhrase.Part;
import com.google.cloud.dialogflow.v2beta1.IntentsClient;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DialogflowTraining {

    public static void main(String[] args) {
        List<String> trainingPhrasesParts = new ArrayList<>();
        List<String> responses = new ArrayList<>();

        try {
            // Đọc câu hỏi
            BufferedReader questionReader = new BufferedReader(new FileReader("C:\\Users\\LAPTOP\\Downloads\\file_trainingcauhoi.txt"));
            String questionLine;
            while ((questionLine = questionReader.readLine()) != null) {
                trainingPhrasesParts.add(questionLine);
            }
            questionReader.close();

            // Đọc câu trả lời
            BufferedReader answerReader = new BufferedReader(new FileReader("C:\\Users\\LAPTOP\\Downloads\\file_trainingtraloi.txt"));
            String answerLine;
            while ((answerLine = answerReader.readLine()) != null) {
                responses.add(answerLine);
            }
            answerReader.close();

            // Kiểm tra xem số câu hỏi và câu trả lời có tương ứng không
            if (trainingPhrasesParts.size() != responses.size()) {
                throw new IllegalArgumentException("Number of questions and answers must match.");
            }

            createIntents("stacklifo-sjpo", trainingPhrasesParts, responses);
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        } catch (ApiException e) {
            System.err.println("Error creating intent: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createIntents(String projectId, List<String> trainingPhrasesParts, List<String> responses) throws ApiException, IOException {
        try (IntentsClient intentsClient = IntentsClient.create()) {
            AgentName parent = AgentName.of(projectId);

            // Lấy danh sách các Intent hiện có
            List<Intent> existingIntents = new ArrayList<>();
            intentsClient.listIntents(parent).iterateAll().forEach(existingIntents::add);

            int maxIntentIndex = 0;

            // Tìm chỉ số tối đa của các Intent hiện tại
            for (Intent intent : existingIntents) {
                String displayName = intent.getDisplayName();
                if (displayName.startsWith("Intent_")) {
                    String[] parts = displayName.split("_");
                    if (parts.length > 1) {
                        try {
                            int index = Integer.parseInt(parts[1]);
                            if (index > maxIntentIndex) {
                                maxIntentIndex = index;
                            }
                        } catch (NumberFormatException e) {
                            // Bỏ qua nếu không phải số
                        }
                    }
                }
            }

            // Tạo Intent cho từng câu hỏi và câu trả lời
            for (int i = 0; i < trainingPhrasesParts.size(); i++) {
                Intent intent = Intent.newBuilder()
                        .setDisplayName("Intent_" + (maxIntentIndex + 1 + i)) // Tạo tên mới dựa trên maxIntentIndex
                        .addTrainingPhrases(
                                TrainingPhrase.newBuilder()
                                        .addParts(Part.newBuilder().setText(trainingPhrasesParts.get(i)).build())
                                        .build())
                        .addMessages(
                                Intent.Message.newBuilder()
                                        .setText(Intent.Message.Text.newBuilder()
                                                .addText(responses.get(i)) // Lấy câu trả lời tương ứng
                                                .build())
                                        .build())
                        .build();

                Intent response = intentsClient.createIntent(parent, intent);
                System.out.format("Intent created: %s\n", response.getDisplayName());
            }
        }
    }
}
