package pooro.blog.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pooro.blog.exception.post.PostNotFoundException;

import java.io.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AwsS3Service {

    private final AmazonS3Client amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * file을 bucket에 업로드
     */
    public String upload(String key, File file) {
        amazonS3Client.putObject(new PutObjectRequest(bucket, key, file)
                .withCannedAcl(CannedAccessControlList.PublicRead));

        return key;
    }

    /**
     * 버켓에 이미지 업로드
     * @return 버켓의 이미지 객체 url
     */
    public String uploadImage(MultipartFile file) throws IOException {
        UUID uuid = UUID.randomUUID();
        String key = "img/" + uuid.toString() + "-" + file.getOriginalFilename();

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(file.getContentType());
        objectMetadata.setContentLength(file.getSize());

        amazonS3Client.putObject(new PutObjectRequest(bucket, key, file.getInputStream(), objectMetadata)
                .withCannedAcl(CannedAccessControlList.PublicRead));

        return amazonS3Client.getUrl(bucket, key).toString();
    }

    /**
     * bucket에 있는 file의 내용 가져오기
     */
    public String getObjectContent(String key) throws IOException, PostNotFoundException {
        try {
            S3Object object = amazonS3Client.getObject(new GetObjectRequest(bucket, key));
            return getContent(object.getObjectContent());
        } catch (AmazonS3Exception e) {
            throw new PostNotFoundException();
        }
    }

    /**
     * bucket에서 파일 삭제
     */
    public void delete(String key) {
        amazonS3Client.deleteObject(new DeleteObjectRequest(bucket, key));
    }

    /**
     * InputStream을 한 줄씩 합쳐서 String으로 반환
     */
    private static String getContent(InputStream input) throws IOException {
        // Read the text input stream one line at a time
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String content = "";
        String line = null;

        while ((line = reader.readLine()) != null) {
            content += line + "\n";
        }

        return content.trim();
    }
}
