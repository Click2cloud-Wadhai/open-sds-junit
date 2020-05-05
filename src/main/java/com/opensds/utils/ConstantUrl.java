package main.java.com.opensds.utils;

public class ConstantUrl {
    private static ConstantUrl mConstantUrl;
    private static  String URL = null;

    private ConstantUrl() {
        URL = "http://" + System.getenv("HOST_IP");
    }

    public static ConstantUrl getInstance() {
        if (mConstantUrl == null) {
            mConstantUrl = new ConstantUrl();
        }
        return mConstantUrl;
    }

    /**
     * Get Types
     *
     * @param adminTenantId admin tenant id.
     */
    public String getTypesUrl(String adminTenantId) {
        return URL +"/v1/"+adminTenantId+"/types";
    }

    /**
     * Add Backend
     *
     * @param adminTenantId admin tenant id.
     */
    public String getAddBackendUrl(String adminTenantId) {
        return URL +"/v1/"+ adminTenantId +"/backends";
    }

    /**
     * Get Backend List
     *
     * @param adminTenantId admin tenant id.
     */
    public String getBackendsUrl(String adminTenantId) {
        return URL +"/v1/"+adminTenantId+"/backends";
    }

    /**
     * Get Backend
     *
     * @param adminTenantId admin tenant id.
     * @param id admin tenant id.
     */
    public String getBackendUrl(String adminTenantId, String id) {
        return URL +"/v1/"+adminTenantId+"/backends/"+id;
    }

    /**
     * Delete Backend
     *
     * @param adminTenantId admin tenant id.
     * @param id admin tenant id.
     */
    public String getDeleteBackendUrl(String adminTenantId, String id) {
        return URL +"/v1/"+adminTenantId+"/backends/"+id;
    }

    /**
     * Create Bucket
     *
     * @param bucketName bucket name.
     */
    public String getCreateBucketUrl(String bucketName) {
        return URL +"/v1/s3/"+ bucketName;
    }

    /**
     * Upload object
     *
     * @param bucketName bucket name.
     * @param fileName file name.
     */
    public String getUploadObjectUrl(String bucketName, String fileName) {
        return URL +"/v1/s3/"+ bucketName +"/"+ fileName;
    }

    /**
     * Download object
     *
     * @param bucketName bucket name.
     * @param fileName file name.
     */
    public String getDownloadObjectUrl(String bucketName, String fileName) {
        return URL +"/v1/s3/"+ bucketName +"/"+ fileName;
    }

    /**
     * Delete bucket
     *
     * @param bucketName bucket name.
     */
    public String getDeleteBucketUrl(String bucketName) {
        return URL +"/v1/s3/"+ bucketName;
    }

    /**
     * Delete object
     *
     * @param bucketName bucket name.
     * @param objectName object name.
     */
    public String getDeleteObjectUrl(String bucketName, String objectName) {
        return URL +"/v1/s3/"+bucketName+"/"+objectName;
    }

    /**
     * Bucket List
     */
    public String getListBucketUrl() {
        return URL +"/v1/s3";
    }

    /**
     * Get list of object of a bucket
     *
     * @param bucketName bucket name
     */
    public String getListOfObjectFromBucketUrl(String bucketName) {
        return URL +"/v1/s3/"+bucketName;
    }

    /**
     * Enable encrypt on bucket.
     *
     * @param bucketName bucket name
     */
    public String getEnableEncryptOnBucketUrl(String bucketName) {
        return URL +"/v1/s3/"+bucketName+"/?DefaultEncryption";
    }
}
