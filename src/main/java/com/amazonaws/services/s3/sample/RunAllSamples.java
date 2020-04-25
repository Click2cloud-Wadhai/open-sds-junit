package main.java.com.amazonaws.services.s3.sample;

public class RunAllSamples {

    /** Put your access key here **/
    private static final String awsAccessKey = "AKIAWVTLFQG5LXQVDPSM";
    
    /** Put your secret key here **/
    private static final String awsSecretKey = "Wq2W2rIlyTf9IEp9yOtsd3SLtNO+vpCt6mBzewJG";
    
    /** Put your bucket name here **/
    private static final String bucketName = "opensds-c2c";
    
    /** The name of the region where the bucket is created. (e.g. us-west-1) **/
    private static final String regionName = "us-east-1";
    
    
    /**
     * Run all the included samples. Before running the samples, you need to
     * specify the bucket name, region name and your credentials.
     */
    public static void main(String[] args) {
        
        PutS3ObjectSample.putS3Object(bucketName, regionName, awsAccessKey, awsSecretKey);
        
        GetS3ObjectSample.getS3Object(bucketName, regionName, awsAccessKey, awsSecretKey);

        PresignedUrlSample.getPresignedUrlToS3Object(bucketName, regionName, awsAccessKey, awsSecretKey);

        PutS3ObjectChunkedSample.putS3ObjectChunked(bucketName, regionName, awsAccessKey, awsSecretKey);

    }

}
