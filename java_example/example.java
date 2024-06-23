import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class S3Uploader implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String REGION = System.getenv("AWS_REGION");
    private static final String BUCKET_NAME = System.getenv("UploadBucket");
    private static final int URL_EXPIRATION_SECONDS = 300;

    private final AmazonS3 s3Client;

    public S3Uploader() {
        this.s3Client = AmazonS3ClientBuilder.standard().withRegion(REGION).build();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            String uploadURL = getUploadURL();
            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("uploadURL", uploadURL);
            responseBody.put("Key", uploadURL.substring(uploadURL.lastIndexOf("/") + 1));

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(new ObjectMapper().writeValueAsString(responseBody));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Error generating upload URL: " + e.getMessage());
        }
    }

    private String getUploadURL() {
        Random random = new Random();
        int randomID = random.nextInt(10000000);
        String key = randomID + ".jpg";

        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(BUCKET_NAME, key)
                .withMethod(HttpMethod.PUT)
                .withExpiration(new Date(System.currentTimeMillis() + URL_EXPIRATION_SECONDS * 1000))
                .withContentType("image/jpeg");

        URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
        return url.toString();
    }
}
